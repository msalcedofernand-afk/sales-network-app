package com.salesnetwork.avon.app.data

import android.content.Context
import com.salesnetwork.avon.app.domain.model.User
import com.salesnetwork.avon.app.domain.model.UserRole
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.UUID
import java.security.MessageDigest
import java.net.HttpURLConnection
import java.net.URL
import org.json.JSONObject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking

class LeaderNetworkRepository private constructor(context: Context) {

    private val prefs = context.getSharedPreferences("leader_network_prefs", Context.MODE_PRIVATE)

    private val usersMap = mutableMapOf<String, User>()
    private val leaderCodesSet = mutableSetOf<String>()
    private val passwordHashes = mutableMapOf<String, String>()

    private val _currentUser = MutableStateFlow<User?>(null)
    val currentUser: StateFlow<User?> = _currentUser.asStateFlow()

    init {
        loadAllUsersFromPrefs()
        _currentUser.value = loadCurrentUserFromPrefs()
    }

    fun registerLeader(name: String, email: String, password: String): Result<User> {
        val cleanEmail = email.trim().lowercase()
        if (usersMap.values.any { it.email.equals(cleanEmail, ignoreCase = true) }) {
            return Result.failure(Exception("El correo '$cleanEmail' ya se encuentra registrado."))
        }
        val referralCode = "AVON-${(1000..9999).random()}"
        val leader = User(
            id = UUID.randomUUID().toString(),
            name = name.trim(),
            email = cleanEmail,
            role = UserRole.LIDER,
            referralCode = referralCode,
            leaderCode = null
        )
        usersMap[leader.id] = leader
        passwordHashes[leader.id] = hashPassword(password)
        leaderCodesSet.add(referralCode)
        persistUser(leader)
        saveActiveUserSession(leader)
        _currentUser.value = leader
        return Result.success(leader)
    }

    fun registerMember(name: String, email: String, password: String, leaderCode: String): Result<User> {
        val cleanLeaderCode = leaderCode.trim().uppercase()
        if (!leaderCodesSet.contains(cleanLeaderCode)) {
            return Result.failure(Exception("El código de Líder '$cleanLeaderCode' no es válido o no existe."))
        }
        val cleanEmail = email.trim().lowercase()
        if (usersMap.values.any { it.email.equals(cleanEmail, ignoreCase = true) }) {
            return Result.failure(Exception("El correo '$cleanEmail' ya se encuentra registrado."))
        }
        val member = User(
            id = UUID.randomUUID().toString(),
            name = name.trim(),
            email = cleanEmail,
            role = UserRole.MIEMBRO,
            referralCode = "MBR-${(1000..9999).random()}",
            leaderCode = cleanLeaderCode
        )
        usersMap[member.id] = member
        passwordHashes[member.id] = hashPassword(password)
        persistUser(member)
        saveActiveUserSession(member)
        _currentUser.value = member
        return Result.success(member)
    }

    fun login(email: String, password: String): Result<User> {
        val cleanEmail = email.trim().lowercase()
        val remote = runBlocking(Dispatchers.IO) { loginSupabase(cleanEmail, password) }
        if (remote.isSuccess) {
            val user = remote.getOrThrow()
            usersMap[user.id] = user
            persistUser(user)
            saveActiveUserSession(user)
            _currentUser.value = user
            return Result.success(user)
        }
        val user = usersMap.values.firstOrNull { it.email.equals(cleanEmail, ignoreCase = true) }
        return if (user != null && passwordHashes[user.id] == hashPassword(password)) {
            saveActiveUserSession(user)
            _currentUser.value = user
            Result.success(user)
        } else {
            Result.failure(Exception("Correo o contraseña incorrectos."))
        }
    }

    private fun loginSupabase(email: String, password: String): Result<User> {
        return try {
        val connection = (URL("https://xceqwexdufdgnmctsxcg.supabase.co/auth/v1/token?grant_type=password").openConnection() as HttpURLConnection).apply {
            requestMethod = "POST"
            doOutput = true
            connectTimeout = 8000
            readTimeout = 8000
            setRequestProperty("apikey", SUPABASE_ANON_KEY)
            setRequestProperty("Content-Type", "application/json")
        }
        connection.outputStream.use { it.write(JSONObject().put("email", email).put("password", password).toString().toByteArray()) }
        val body = (if (connection.responseCode in 200..299) connection.inputStream else connection.errorStream)
            ?.bufferedReader()?.use { it.readText() }.orEmpty()
        if (connection.responseCode !in 200..299) return Result.failure(Exception("Correo o contraseña incorrectos."))
        val json = JSONObject(body)
        val authUser = json.getJSONObject("user")
        val metadata = authUser.optJSONObject("user_metadata")
        Result.success(User(
            id = authUser.getString("id"),
            name = metadata?.optString("name").orEmpty().ifBlank { email.substringBefore("@").replaceFirstChar { it.uppercase() } },
            email = authUser.optString("email", email),
            role = UserRole.LIDER,
            referralCode = "AVON-2026"
        ))
    } catch (_: Exception) {
        Result.failure(Exception("No se pudo conectar. Revisa tu conexión e inténtalo de nuevo."))
        }
    }

    private fun hashPassword(password: String): String = MessageDigest.getInstance("SHA-256")
        .digest(password.toByteArray()).joinToString("") { "%02x".format(it) }

    fun logout() {
        prefs.edit().remove("active_user_id").apply()
        _currentUser.value = null
    }

    fun getMembersForLeader(referralCode: String): List<User> {
        return usersMap.values.filter { it.leaderCode.equals(referralCode.trim(), ignoreCase = true) }
    }

    private fun persistUser(user: User) {
        val userIds = prefs.getStringSet("all_user_ids", mutableSetOf())?.toMutableSet() ?: mutableSetOf()
        userIds.add(user.id)
        prefs.edit()
            .putStringSet("all_user_ids", userIds)
            .putString("user_${user.id}_name", user.name)
            .putString("user_${user.id}_email", user.email)
            .putString("user_${user.id}_role", user.role.name)
            .putString("user_${user.id}_refCode", user.referralCode)
            .putString("user_${user.id}_leaderCode", user.leaderCode)
            .putString("user_${user.id}_passwordHash", passwordHashes[user.id])
            .apply()
    }

    private fun saveActiveUserSession(user: User) {
        prefs.edit().putString("active_user_id", user.id).apply()
    }

    private fun loadAllUsersFromPrefs() {
        val defaultLeader = User(
            id = "leader-demo-01",
            name = "Líder Avon Chiclayo",
            email = "lider.chiclayo@avon.com",
            role = UserRole.LIDER,
            referralCode = "AVON-2026",
            leaderCode = null
        )
        usersMap[defaultLeader.id] = defaultLeader
        leaderCodesSet.add(defaultLeader.referralCode)
        val demoUser = User(
            id = "074fa307-28b6-4ec3-bc11-849b66c97675",
            name = "Usuario demo",
            email = "demo@salesnetwork.test",
            role = UserRole.LIDER,
            referralCode = "AVON-DEMO"
        )
        usersMap[demoUser.id] = demoUser
        passwordHashes[demoUser.id] = hashPassword("ViveDemo-2026!")
        leaderCodesSet.add(demoUser.referralCode)

        val userIds = prefs.getStringSet("all_user_ids", emptySet()) ?: emptySet()
        for (id in userIds) {
            val name = prefs.getString("user_${id}_name", null) ?: continue
            val email = prefs.getString("user_${id}_email", null) ?: continue
            val roleStr = prefs.getString("user_${id}_role", UserRole.LIDER.name)
            val refCode = prefs.getString("user_${id}_refCode", "AVON-2026") ?: "AVON-2026"
            val leaderCode = prefs.getString("user_${id}_leaderCode", null)
            val role = try { UserRole.valueOf(roleStr!!) } catch (e: Exception) { UserRole.LIDER }
            val user = User(id = id, name = name, email = email, role = role, referralCode = refCode, leaderCode = leaderCode)
            usersMap[id] = user
            prefs.getString("user_${id}_passwordHash", null)?.let { passwordHashes[id] = it }
            if (role == UserRole.LIDER) {
                leaderCodesSet.add(refCode)
            }
        }
    }

    private fun loadCurrentUserFromPrefs(): User? {
        val activeId = prefs.getString("active_user_id", null) ?: return null
        return usersMap[activeId]
    }

    companion object {
        private const val SUPABASE_ANON_KEY = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6InhjZXE3ZXhkdWZkZ25tY3RzeGNnIiwicm9sZSI6ImFub24iLCJpYXQiOjE3ODg4MzgxMjgsImV4cCI6MjEwNDQxNDEyOH0.LqPTMoS3Q1-zdsOX9CMOahMynB5XAl-AxsjrVxSlse8"
        @Volatile
        private var INSTANCE: LeaderNetworkRepository? = null

        fun getInstance(context: Context): LeaderNetworkRepository {
            return INSTANCE ?: synchronized(this) {
                val instance = LeaderNetworkRepository(context.applicationContext)
                INSTANCE = instance
                instance
            }
        }
    }
}
