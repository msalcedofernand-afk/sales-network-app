package com.salesnetwork.avon.app.data

import android.content.Context
import android.util.Base64
import com.salesnetwork.avon.app.data.local.AppDatabase
import com.salesnetwork.avon.app.domain.model.User
import com.salesnetwork.avon.app.domain.model.UserRole
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.net.HttpURLConnection
import java.net.URL
import org.json.JSONObject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

class LeaderNetworkRepository private constructor(private val context: Context) {

    private val prefs = context.getSharedPreferences("leader_network_prefs", Context.MODE_PRIVATE)
    private val secureTokenStore = SecureTokenStore(context)
    private val repositoryScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val refreshMutex = Mutex()

    // This map only holds the current process view. Credentials always belong to
    // Supabase Auth and are never persisted by this repository.
    private val usersMap = mutableMapOf<String, User>()

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
        repositoryScope.launch { restoreSession() }
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
            com.salesnetwork.avon.app.update.CrashLogger.getInstance(context).flushPending()
            return Result.success(remoteUser)
        }

        val attempts = prefs.getInt("login_attempts", 0) + 1
        prefs.edit().putInt("login_attempts", attempts).apply()

        if (attempts >= MAX_LOGIN_ATTEMPTS) {
            prefs.edit().putLong("lockout_until", System.currentTimeMillis() + LOCKOUT_DURATION_MS).apply()
            prefs.edit().putInt("login_attempts", 0).apply()
            return Result.failure(Exception("Demasiados intentos fallidos. Cuenta bloqueada 5 minutos."))
        }

        return Result.failure(Exception("Correo o contrasena incorrectos."))
    }

    fun resetPassword(email: String, newPassword: String): Result<Boolean> {
        return Result.failure(Exception("Usa la recuperación de contraseña desde la web para recibir un enlace seguro por correo."))
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
            if (connection.responseCode !in 200..299) {
                return Result.failure(Exception("Correo o contrasena incorrectos."))
            }
            val json = JSONObject(body)
            val authUser = json.getJSONObject("user")
            val metadata = authUser.optJSONObject("user_metadata")
            val accessToken = json.optString("access_token")
            val refreshToken = json.optString("refresh_token")
            val userId = authUser.getString("id")
            if (accessToken.isBlank() || refreshToken.isBlank()) return Result.failure(Exception("Sesión incompleta."))
            val expiresAt = System.currentTimeMillis() + json.optLong("expires_in", 3600L) * 1000L
            secureTokenStore.saveSession(SecureSession(accessToken, refreshToken, expiresAt, userId))
            prefs.edit().remove("refresh_token").remove("supabase_access_token").apply()
            val role = fetchRemoteRole(userId, accessToken).getOrElse {
                secureTokenStore.clear()
                return Result.failure(Exception("No pudimos validar tu rol y equipo."))
            }
            val referralCode = fetchActiveInvitation(userId, accessToken).orEmpty()
            Result.success(User(
                id = userId,
                name = metadata?.optString("name").orEmpty().ifBlank { email.substringBefore("@").replaceFirstChar { it.uppercase() } },
                email = authUser.optString("email", email),
                role = role,
                referralCode = referralCode,
                referralCodeExpiresAt = null
            ))
        } catch (_: Exception) {
            Result.failure(Exception("No se pudo conectar."))
        }
    }

    private fun fetchRemoteRole(userId: String, accessToken: String): Result<UserRole> {
        if (accessToken.isBlank()) return Result.failure(Exception("Sesión inválida."))
        return runCatching {
            val endpoint = URL("https://xceqwexdufdgnmctsxcg.supabase.co/rest/v1/team_members?user_id=eq.$userId&select=role&limit=1")
            val connection = (endpoint.openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                connectTimeout = 5000
                readTimeout = 5000
                setRequestProperty("apikey", SUPABASE_ANON_KEY)
                setRequestProperty("Authorization", "Bearer $accessToken")
                setRequestProperty("Accept", "application/json")
            }
            check(connection.responseCode in 200..299)
            val body = connection.inputStream.bufferedReader().use { it.readText() }
            connection.disconnect()
            val rows = org.json.JSONArray(body)
            check(rows.length() > 0) { "membership_required" }
            when (rows.getJSONObject(0).optString("role")) {
                "LIDER" -> UserRole.LIDER
                "ROOT_ADMIN" -> UserRole.ROOT_ADMIN
                "MIEMBRO" -> UserRole.MIEMBRO
                else -> error("unknown_role")
            }
        }
    }

    private fun fetchActiveInvitation(userId: String, accessToken: String): String? = runCatching {
        // A leader only sees an invitation that they created. Never expose another
        // member's code as this user's team code.
        val endpoint = URL("https://xceqwexdufdgnmctsxcg.supabase.co/rest/v1/invitations?created_by=eq.$userId&status=eq.ACTIVE&select=code&order=created_at.desc&limit=1")
        val connection = (endpoint.openConnection() as HttpURLConnection).apply {
            requestMethod = "GET"; connectTimeout = 5000; readTimeout = 5000
            setRequestProperty("apikey", SUPABASE_ANON_KEY); setRequestProperty("Authorization", "Bearer $accessToken")
        }
        check(connection.responseCode in 200..299)
        val rows = org.json.JSONArray(connection.inputStream.bufferedReader().use { it.readText() })
        connection.disconnect()
        if (rows.length() == 0) null else rows.getJSONObject(0).getString("code")
    }.getOrNull()

    fun logout() {
        prefs.edit()
            .remove("active_user_id")
            .remove("refresh_token")
            .remove("login_attempts")
            .remove("lockout_until")
            .apply()
        secureTokenStore.clear()
        AppDatabase.clearForLogout(context)
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
            .apply()
    }

    private fun saveActiveUserSession(user: User) {
        prefs.edit().putString("active_user_id", user.id).apply()
    }


    private fun loadCurrentUserFromPrefs(): User? {
        val activeId = prefs.getString("active_user_id", null) ?: return null
        return usersMap[activeId]
    }

    fun isTokenExpired(): Boolean {
        val session = secureTokenStore.getSession() ?: return true
        return System.currentTimeMillis() + TOKEN_REFRESH_BUFFER_MS >= session.expiresAtEpochMs
    }

    fun getTokenExpirationTime(): Long {
        return secureTokenStore.getSession()?.expiresAtEpochMs ?: 0L
    }

    suspend fun refreshAccessToken(): Result<String> {
        return refreshMutex.withLock {
            val current = secureTokenStore.getSession() ?: return@withLock Result.failure(Exception("No hay sesión."))
            if (System.currentTimeMillis() + TOKEN_REFRESH_BUFFER_MS < current.expiresAtEpochMs) {
                return@withLock Result.success(current.accessToken)
            }
            withContext(Dispatchers.IO) {
            try {
                val connection = (URL("https://xceqwexdufdgnmctsxcg.supabase.co/auth/v1/token?grant_type=refresh_token").openConnection() as HttpURLConnection).apply {
                    requestMethod = "POST"
                    doOutput = true
                    connectTimeout = 8000
                    readTimeout = 8000
                    setRequestProperty("apikey", SUPABASE_ANON_KEY)
                    setRequestProperty("Content-Type", "application/json")
                }
                connection.outputStream.use { it.write(JSONObject().put("refresh_token", current.refreshToken).toString().toByteArray()) }
                val body = (if (connection.responseCode in 200..299) connection.inputStream else connection.errorStream)
                    ?.bufferedReader()?.use { it.readText() }.orEmpty()
                if (connection.responseCode !in 200..299) {
                    return@withContext Result.failure(Exception("Refresh failed."))
                }
                val json = JSONObject(body)
                val newAccessToken = json.optString("access_token")
                val newRefreshToken = json.optString("refresh_token")
                if (newAccessToken.isBlank()) return@withContext Result.failure(Exception("Sesión inválida."))
                secureTokenStore.saveSession(SecureSession(
                    accessToken = newAccessToken,
                    refreshToken = newRefreshToken.ifBlank { current.refreshToken },
                    expiresAtEpochMs = System.currentTimeMillis() + json.optLong("expires_in", 3600L) * 1000L,
                    userId = current.userId
                ))
                Result.success(newAccessToken)
            } catch (_: Exception) {
                Result.failure(Exception("No se pudo refrescar el token."))
            }
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

    private suspend fun restoreSession() {
        val session = secureTokenStore.getSession() ?: return
        if (!ensureValidToken()) return
        val active = secureTokenStore.getSession() ?: return
        val role = fetchRemoteRole(active.userId, active.accessToken).getOrElse { logout(); return }
        val storedName = prefs.getString("user_${active.userId}_name", null) ?: "Usuario"
        val storedEmail = prefs.getString("user_${active.userId}_email", null) ?: ""
        val restored = User(active.userId, storedName, storedEmail, role, fetchActiveInvitation(active.userId, active.accessToken).orEmpty())
        usersMap[restored.id] = restored
        _currentUser.value = restored
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
