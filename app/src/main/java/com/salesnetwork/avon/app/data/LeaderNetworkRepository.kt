package com.salesnetwork.avon.app.data

import android.content.Context
import com.salesnetwork.avon.app.BuildConfig
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
        val referralCode = "VV-${(1000..9999).random()}"
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
        if (cleanLeaderCode.isBlank() || !leaderCodesSet.contains(cleanLeaderCode)) {
            return Result.failure(Exception("El codigo de red '$cleanLeaderCode' no es valido o no existe. Para registrarte como vendedor debes solicitar el codigo a tu Lider."))
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

        // Production always authenticates against Supabase. Local demo accounts
        // remain available only for debug builds used during UI validation.
        val remote = runBlocking(Dispatchers.IO) { loginSupabase(cleanEmail, password) }
        if (remote.isSuccess) {
            val remoteUser = remote.getOrThrow()
            usersMap[remoteUser.id] = remoteUser
            persistUser(remoteUser)
            saveActiveUserSession(remoteUser)
            _currentUser.value = remoteUser
            return Result.success(remoteUser)
        }

        if (!BuildConfig.DEBUG) return Result.failure(Exception("Correo o contrasena incorrectos."))

        // Verificacion especial para Usuario Root Admin Total y Lideres
        val user = usersMap.values.firstOrNull { it.email.equals(cleanEmail, ignoreCase = true) }
        if (user != null) {
            val isRootAdminMatch = user.role == UserRole.ROOT_ADMIN && (password == "RootAdmin2026!" || password == "RootAdmin2026" || password == "123456")
            val isLeaderMatch = user.role == UserRole.LIDER && (password == "LiderVV2026!" || password == "123456")
            val isHashMatch = passwordHashes[user.id] == hashPassword(password)
            if (isHashMatch || isRootAdminMatch || isLeaderMatch) {
                saveActiveUserSession(user)
                _currentUser.value = user
                return Result.success(user)
            }
        }

        return Result.failure(Exception("Correo o contrasena incorrectos."))
    }

    fun resetPassword(email: String, newPassword: String): Result<Boolean> {
        val cleanEmail = email.trim().lowercase()
        val user = usersMap.values.firstOrNull { it.email.equals(cleanEmail, ignoreCase = true) }
            ?: return Result.failure(Exception("No existe ninguna cuenta con el correo '$cleanEmail'."))
        if (newPassword.length < 6) {
            return Result.failure(Exception("La nueva contrasena debe tener al menos 6 caracteres."))
        }
        val newHash = hashPassword(newPassword)
        passwordHashes[user.id] = newHash
        persistUser(user)
        return Result.success(true)
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
            if (connection.responseCode !in 200..299) return Result.failure(Exception("Correo o contrasena incorrectos."))
            val json = JSONObject(body)
            val authUser = json.getJSONObject("user")
            val metadata = authUser.optJSONObject("user_metadata")
            val accessToken = json.optString("access_token")
            if (accessToken.isNotBlank()) prefs.edit().putString("supabase_access_token", accessToken).apply()
            val role = fetchRemoteRole(authUser.getString("id"), accessToken)
            Result.success(User(
                id = authUser.getString("id"),
                name = metadata?.optString("name").orEmpty().ifBlank { email.substringBefore("@").replaceFirstChar { it.uppercase() } },
                email = authUser.optString("email", email),
                role = role,
                referralCode = "VV-2026"
            ))
        } catch (_: Exception) {
            Result.failure(Exception("No se pudo conectar."))
        }
    }

    private fun fetchRemoteRole(userId: String, accessToken: String): UserRole {
        if (accessToken.isBlank()) return UserRole.MIEMBRO
        return try {
            val endpoint = URL("https://xceqwexdufdgnmctsxcg.supabase.co/rest/v1/team_members?user_id=eq.$userId&select=role&limit=1")
            val connection = (endpoint.openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                connectTimeout = 5000
                readTimeout = 5000
                setRequestProperty("apikey", SUPABASE_ANON_KEY)
                setRequestProperty("Authorization", "Bearer $accessToken")
                setRequestProperty("Accept", "application/json")
            }
            val body = connection.inputStream.bufferedReader().use { it.readText() }
            connection.disconnect()
            val rows = org.json.JSONArray(body)
            if (rows.length() == 0) UserRole.MIEMBRO else when (rows.getJSONObject(0).optString("role")) {
                "LIDER" -> UserRole.LIDER
                "ROOT_ADMIN" -> UserRole.ROOT_ADMIN
                else -> UserRole.MIEMBRO
            }
        } catch (_: Exception) { UserRole.MIEMBRO }
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

    fun getAllUsers(): List<User> {
        return usersMap.values.toList()
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
        // 1. Root Admin Total
        val rootAdmin = User(
            id = "root-admin-01",
            name = "Administrador Central",
            email = "root@vv.com",
            role = UserRole.ROOT_ADMIN,
            referralCode = "VV-ROOT",
            leaderCode = null
        )
        usersMap[rootAdmin.id] = rootAdmin
        passwordHashes[rootAdmin.id] = hashPassword("RootAdmin2026!")
        leaderCodesSet.add(rootAdmin.referralCode)

        // 2. Lider Demo VV
        val defaultLeader = User(
            id = "leader-demo-01",
            name = "Lider VV Chiclayo",
            email = "lider.chiclayo@vv.com",
            role = UserRole.LIDER,
            referralCode = "VV-2026",
            leaderCode = null
        )
        usersMap[defaultLeader.id] = defaultLeader
        passwordHashes[defaultLeader.id] = hashPassword("LiderVV2026!")
        leaderCodesSet.add(defaultLeader.referralCode)

        // 3. Usuario Demo
        val demoUser = User(
            id = "074fa307-28b6-4ec3-bc11-849b66c97675",
            name = "Usuario Demo VV",
            email = "demo@salesnetwork.test",
            role = UserRole.LIDER,
            referralCode = "VV-DEMO"
        )
        usersMap[demoUser.id] = demoUser
        passwordHashes[demoUser.id] = hashPassword("ViveDemo-2026!")
        leaderCodesSet.add(demoUser.referralCode)

        // 4. Vendedoras Iniciales bajo Lider VV Chiclayo (VV-2026)
        val initialMembers = listOf(
            User(id = "mbr-01", name = "Rosa Benites", email = "rosa.benites@vv.com", role = UserRole.MIEMBRO, referralCode = "MBR-1001", leaderCode = "VV-2026", isActiveInCampaign = true),
            User(id = "mbr-02", name = "Carmen Huaman", email = "carmen.huaman@vv.com", role = UserRole.MIEMBRO, referralCode = "MBR-1002", leaderCode = "VV-2026", isActiveInCampaign = true),
            User(id = "mbr-03", name = "Lucia Sanchez", email = "lucia.sanchez@vv.com", role = UserRole.MIEMBRO, referralCode = "MBR-1003", leaderCode = "VV-2026", isActiveInCampaign = false),
            User(id = "mbr-04", name = "Patricia Delgado", email = "patricia.delgado@vv.com", role = UserRole.MIEMBRO, referralCode = "MBR-1004", leaderCode = "VV-2026", isActiveInCampaign = true)
        )
        for (m in initialMembers) {
            usersMap[m.id] = m
            passwordHashes[m.id] = hashPassword("123456")
        }

        // Carga de usuarios guardados
        val userIds = prefs.getStringSet("all_user_ids", emptySet()) ?: emptySet()
        for (id in userIds) {
            val name = prefs.getString("user_${id}_name", null) ?: continue
            val email = prefs.getString("user_${id}_email", null) ?: continue
            val roleStr = prefs.getString("user_${id}_role", UserRole.LIDER.name)
            val refCode = prefs.getString("user_${id}_refCode", "VV-2026") ?: "VV-2026"
            val leaderCode = prefs.getString("user_${id}_leaderCode", null)
            val role = try { UserRole.valueOf(roleStr!!) } catch (e: Exception) { UserRole.LIDER }
            val user = User(id = id, name = name, email = email, role = role, referralCode = refCode, leaderCode = leaderCode)
            usersMap[id] = user
            prefs.getString("user_${id}_passwordHash", null)?.let { passwordHashes[id] = it }
            if (role == UserRole.LIDER || role == UserRole.ROOT_ADMIN) {
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
