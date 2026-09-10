package com.salesnetwork.avon.app.data

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import java.nio.charset.StandardCharsets
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec
import org.json.JSONObject

data class SecureSession(
    val accessToken: String,
    val refreshToken: String,
    val expiresAtEpochMs: Long,
    val userId: String
)

/** Keeps the Supabase access token out of plain SharedPreferences. */
class SecureTokenStore(context: Context) {
    private val preferences = context.getSharedPreferences(PREFERENCES, Context.MODE_PRIVATE)
    private val keyStore = KeyStore.getInstance(ANDROID_KEYSTORE).apply { load(null) }

    fun saveSession(session: SecureSession) {
        require(session.accessToken.isNotBlank() && session.refreshToken.isNotBlank() && session.userId.isNotBlank())
        val value = JSONObject().apply {
            put("access_token", session.accessToken)
            put("refresh_token", session.refreshToken)
            put("expires_at_ms", session.expiresAtEpochMs)
            put("user_id", session.userId)
        }.toString()
        val cipher = Cipher.getInstance(TRANSFORMATION).apply { init(Cipher.ENCRYPT_MODE, secretKey()) }
        val encrypted = cipher.doFinal(value.toByteArray(StandardCharsets.UTF_8))
        preferences.edit().putString(TOKEN_KEY, Base64.encodeToString(encrypted, Base64.NO_WRAP))
            .putString(IV_KEY, Base64.encodeToString(cipher.iv, Base64.NO_WRAP)).apply()
    }

    fun getSession(): SecureSession? {
        val encoded = preferences.getString(TOKEN_KEY, null) ?: return null
        val encodedIv = preferences.getString(IV_KEY, null) ?: return null
        return runCatching {
            val cipher = Cipher.getInstance(TRANSFORMATION).apply {
                init(Cipher.DECRYPT_MODE, secretKey(), GCMParameterSpec(128, Base64.decode(encodedIv, Base64.NO_WRAP)))
            }
            val json = JSONObject(String(cipher.doFinal(Base64.decode(encoded, Base64.NO_WRAP)), StandardCharsets.UTF_8))
            SecureSession(
                accessToken = json.getString("access_token"),
                refreshToken = json.getString("refresh_token"),
                expiresAtEpochMs = json.getLong("expires_at_ms"),
                userId = json.getString("user_id")
            )
        }.getOrNull()
    }

    fun get(): String? = getSession()?.accessToken

    fun savePendingCrash(payload: String) = saveEncrypted(PENDING_CRASH_KEY, PENDING_CRASH_IV_KEY, payload)

    fun getPendingCrash(): String? = getEncrypted(PENDING_CRASH_KEY, PENDING_CRASH_IV_KEY)

    fun clearPendingCrash() = preferences.edit().remove(PENDING_CRASH_KEY).remove(PENDING_CRASH_IV_KEY).apply()

    fun clear() = preferences.edit().clear().apply()

    private fun saveEncrypted(valueKey: String, ivKey: String, value: String) {
        val cipher = Cipher.getInstance(TRANSFORMATION).apply { init(Cipher.ENCRYPT_MODE, secretKey()) }
        preferences.edit().putString(valueKey, Base64.encodeToString(cipher.doFinal(value.toByteArray(StandardCharsets.UTF_8)), Base64.NO_WRAP))
            .putString(ivKey, Base64.encodeToString(cipher.iv, Base64.NO_WRAP)).apply()
    }

    private fun getEncrypted(valueKey: String, ivKey: String): String? {
        val encoded = preferences.getString(valueKey, null) ?: return null
        val encodedIv = preferences.getString(ivKey, null) ?: return null
        return runCatching {
            val cipher = Cipher.getInstance(TRANSFORMATION).apply {
                init(Cipher.DECRYPT_MODE, secretKey(), GCMParameterSpec(128, Base64.decode(encodedIv, Base64.NO_WRAP)))
            }
            String(cipher.doFinal(Base64.decode(encoded, Base64.NO_WRAP)), StandardCharsets.UTF_8)
        }.getOrNull()
    }

    private fun secretKey(): SecretKey {
        val existing = keyStore.getKey(KEY_ALIAS, null)
        if (existing is SecretKey) return existing
        val generator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, ANDROID_KEYSTORE)
        generator.init(KeyGenParameterSpec.Builder(KEY_ALIAS, KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT)
            .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
            .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
            .setRandomizedEncryptionRequired(true).build())
        return generator.generateKey()
    }

    companion object {
        private const val ANDROID_KEYSTORE = "AndroidKeyStore"
        private const val KEY_ALIAS = "sales_network_supabase_token"
        private const val TRANSFORMATION = "AES/GCM/NoPadding"
        private const val PREFERENCES = "supabase_secure_session"
        private const val TOKEN_KEY = "access_token"
        private const val IV_KEY = "access_token_iv"
        private const val PENDING_CRASH_KEY = "pending_crash"
        private const val PENDING_CRASH_IV_KEY = "pending_crash_iv"
    }
}
