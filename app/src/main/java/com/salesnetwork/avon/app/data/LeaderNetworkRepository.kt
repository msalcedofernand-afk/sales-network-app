package com.salesnetwork.avon.app.data

import android.content.Context
import android.util.Base64
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
import kotlinx.coroutines.withContext

class LeaderNetworkRepository private constructor(context: Context) {

    private val prefs = context.getSharedPreferences("leader_network_prefs", Context.MODE_PRIVATE)
    private val secureTokenStore = SecureTokenStore(context)

    private val usersMap = mutableMapOf<String, User>()
    private val leaderCodesSet = mutableSetOf<String>()
    private val passwordHashes = mutableMapOf<String, String>()

    private val _currentUser = MutableStateFlow<User?>(null)
    val currentUser: StateFlow<User?> = _currentUser.asStateFlow()

    companion object {
        private const val SUPABASE_ANON_KEY = "sb_publishable_5lm6ZqlAg7Is_DlrJ5mNnA_0DKcDmGp"
        private const val MAX_LOGIN_ATTEMPTS = 5
        private const val LOCKOUT_DURATION_MS = 5 * 60 * 1000L // 5 minutes
        private const val TOKEN_REFRESH_BUFFER_MS = 5 * 60 * 1000L // 5 min before expiry

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

    init {
        _currentUser.value = null
    }

    fun registerLeader(name: String, email: String, password: String): Result<User> {
        return Result.failure(Exception("Crea tu cuenta desde la web para confirmar el correo y crear el equipo de forma segura."))
    }

    fun registerMember(name: String, email: String, password: String, leaderCode: String): Result<User> {
        return Result.failure(Exception("Acepta la invitación desde la web después de confirmar tu correo."))
    }

    suspend fun login(email: String, password: String): Result<User> {
        val cleanEmail = email.trim().lowercase()

        val lockoutUntil = prefs.getLong("lockout_until", 0L)
        if (System.currentTimeMillis() < lockoutUntil) {
            val remaining = (lockoutUntil - System.currentTimeMillis()) / 1000
            return Result.failure(Exception("Demasiados intentos. Espera ${remaining} segundos."))
        }

        val remote = withContext(Dispatchers.IO) { loginSupabase(cleanEmail, password) }
        if (remote.isSuccess) {
            prefs.edit().putInt("login_attempts", 0).apply()
            val remoteUser = remote.getOrThrow()
            usersMap[remoteUser.id] = remoteUser
            persistUser(remoteUser)
            saveActiveUserSession(remoteUser)
            _currentUser.value = remoteUser
            return Result.success(remoteUser)
        }

        val attempts = prefs.getInt("login_attempts", 0) + 1
        prefs.edit().putInt("login_attempts", attempts).apply()

        if (attempts >= MAX_LOGIN_ATTEMPTS) {
            prefs.edit().putLong("lockout_until", System.currentTimeMillis() + LOCKOUT_DURATION_MS).apply()
            prefs.edit().putInt("login_attempts", 0).apply()
            return Result.failure(Exception("Demasiados intentos fallidos. Cuenta bloqueada 5 minutos."))
        }

        val errorMsg = remote.exceptionOrNull()?.message ?: "Unknown error"
        android.util.Log.e("Auth", "Login failed: $errorMsg")
        return Result.failure(Exception("Correo o contrasena incorrectos."))
    }

    fun resetPassword(email: String, newPassword: String): Result<Boolean> {
        return Result.failure(Exception("Usa la recuperación de contraseña desde la web para recibir un enlace seguro por correo."))
    }

    private fun loginSupabase(email: String, password: String): Result<User> {
        return try {
            android.util.Log.d("Auth", "Attempting Supabase login for: $email")
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
            android.util.Log.d("Auth", "Supabase response code: ${connection.responseCode}")
            if (connection.responseCode !in 200..299) {
                android.util.Log.e("Auth", "Supabase auth failed: $body")
                return Result.failure(Exception("Correo o contrasena incorrectos."))
            }
            val json = JSONObject(body)
            val authUser = json.getJSONObject("user")
            val metadata = authUser.optJSONObject("user_metadata")
            val accessToken = json.optString("access_token")
            val refreshToken = json.optString("refresh_token")
            if (accessToken.isNotBlank()) {
                secureTokenStore.save(accessToken)
                if (refreshToken.isNotBlank()) {
                    prefs.edit().putString("refresh_token", refreshToken).apply()
                }
                prefs.edit().remove("supabase_access_token").apply()
            }
            val userId = authUser.getString("id")
            val role = fetchRemoteRole(userId, accessToken)
            val referralCode = generateReferralCode(userId)
            val expiresAt = System.currentTimeMillis() + (90L * 24 * 60 * 60 * 1000) // 90 days
            Result.success(User(
                id = userId,
                name = metadata?.optString("name").orEmpty().ifBlank { email.substringBefore("@").replaceFirstChar { it.uppercase() } },
                email = authUser.optString("email", email),
                role = role,
                referralCode = referralCode,
                referralCodeExpiresAt = expiresAt
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
        prefs.edit()
            .remove("active_user_id")
            .remove("refresh_token")
            .remove("login_attempts")
            .remove("lockout_until")
            .apply()
        secureTokenStore.clear()
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
            .putLong("user_${user.id}_refCodeExpiresAt", user.referralCodeExpiresAt ?: 0L)
            .putString("user_${user.id}_leaderCode", user.leaderCode)
            .putString("user_${user.id}_passwordHash", passwordHashes[user.id])
            .apply()
    }

    private fun saveActiveUserSession(user: User) {
        prefs.edit().putString("active_user_id", user.id).apply()
    }


    private fun loadCurrentUserFromPrefs(): User? {
        val activeId = prefs.getString("active_user_id", null) ?: return null
        return usersMap[activeId]
    }

    private fun generateReferralCode(userId: String): String {
        val hash = MessageDigest.getInstance("SHA-256")
            .digest(userId.toByteArray())
            .take(3)
            .joinToString("") { "%02X".format(it) }
        return "VV-$hash"
    }

    fun isReferralCodeExpired(user: User): Boolean {
        val expiresAt = user.referralCodeExpiresAt ?: return false
        return System.currentTimeMillis() > expiresAt
    }

    fun isTokenExpired(): Boolean {
        val token = secureTokenStore.get() ?: return true
        return try {
            val parts = token.split(".")
            if (parts.size != 3) return true
            val payload = String(Base64.decode(parts[1], Base64.URL_SAFE))
            val json = JSONObject(payload)
            val exp = json.getLong("exp") * 1000
            System.currentTimeMillis() >= exp
        } catch (_: Exception) {
            true
        }
    }

    fun getTokenExpirationTime(): Long {
        val token = secureTokenStore.get() ?: return 0L
        return try {
            val parts = token.split(".")
            if (parts.size != 3) return 0L
            val payload = String(Base64.decode(parts[1], Base64.URL_SAFE))
            val json = JSONObject(payload)
            json.getLong("exp") * 1000
        } catch (_: Exception) {
            0L
        }
    }

    suspend fun refreshAccessToken(): Result<String> {
        val refreshToken = prefs.getString("refresh_token", null)
            ?: return Result.failure(Exception("No hay refresh token."))
        return withContext(Dispatchers.IO) {
            try {
                val connection = (URL("https://xceqwexdufdgnmctsxcg.supabase.co/auth/v1/token?grant_type=refresh_token").openConnection() as HttpURLConnection).apply {
                    requestMethod = "POST"
                    doOutput = true
                    connectTimeout = 8000
                    readTimeout = 8000
                    setRequestProperty("apikey", SUPABASE_ANON_KEY)
                    setRequestProperty("Content-Type", "application/json")
                }
                connection.outputStream.use { it.write(JSONObject().put("refresh_token", refreshToken).toString().toByteArray()) }
                val body = (if (connection.responseCode in 200..299) connection.inputStream else connection.errorStream)
                    ?.bufferedReader()?.use { it.readText() }.orEmpty()
                if (connection.responseCode !in 200..299) {
                    return@withContext Result.failure(Exception("Refresh failed."))
                }
                val json = JSONObject(body)
                val newAccessToken = json.optString("access_token")
                val newRefreshToken = json.optString("refresh_token")
                if (newAccessToken.isNotBlank()) {
                    secureTokenStore.save(newAccessToken)
                }
                if (newRefreshToken.isNotBlank()) {
                    prefs.edit().putString("refresh_token", newRefreshToken).apply()
                }
                Result.success(newAccessToken)
            } catch (_: Exception) {
                Result.failure(Exception("No se pudo refrescar el token."))
            }
        }
    }

    suspend fun ensureValidToken(): Boolean {
        if (isTokenExpired()) {
            val refresh = refreshAccessToken()
            if (refresh.isFailure) {
                logout()
                return false
            }
        }
        return true
    }

    suspend fun generateInvitationCode(teamId: String, maxUses: Int = 1): Result<String> {
        val token = secureTokenStore.get() ?: return Result.failure(Exception("No hay sesión activa."))
        return withContext(Dispatchers.IO) {
            try {
                val expiresAt = java.time.Instant.now().plusSeconds(7 * 24 * 60 * 60).toString()
                val body = JSONObject().apply {
                    put("team_id", teamId)
                    put("expires_at", expiresAt)
                    put("max_uses", maxUses)
                }
                val connection = (URL("https://xceqwexdufdgnmctsxcg.supabase.co/functions/v1/create-invitation").openConnection() as HttpURLConnection).apply {
                    requestMethod = "POST"
                    doOutput = true
                    connectTimeout = 8000
                    readTimeout = 8000
                    setRequestProperty("apikey", SUPABASE_ANON_KEY)
                    setRequestProperty("Authorization", "Bearer $token")
                    setRequestProperty("Content-Type", "application/json")
                }
                connection.outputStream.use { it.write(body.toString().toByteArray()) }
                val response = (if (connection.responseCode in 200..299) connection.inputStream else connection.errorStream)
                    ?.bufferedReader()?.use { it.readText() }.orEmpty()
                if (connection.responseCode !in 200..299) {
                    return@withContext Result.failure(Exception("Error al generar código."))
                }
                val json = JSONObject(response)
                val code = json.getJSONObject("invitation").getString("code")
                Result.success(code)
            } catch (_: Exception) {
                Result.failure(Exception("No se pudo generar el código."))
            }
        }
    }
}
