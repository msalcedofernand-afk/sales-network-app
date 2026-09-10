package com.salesnetwork.avon.app.update

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.FileProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import java.security.MessageDigest

class AppUpdateInstaller(private val context: Context) {
    suspend fun downloadVerifyAndOpen(info: AppUpdateInfo): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            require(AppUpdateChecker.isApprovedDownload(info.apkUrl, info.channel))
            val directory = File(context.cacheDir, "updates").apply { mkdirs() }
            val temporary = File(directory, "download.tmp")
            val apk = File(directory, "sales-network-${info.channel}.apk")
            temporary.delete()

            val connection = (URL(info.apkUrl).openConnection() as HttpURLConnection).apply {
                connectTimeout = 15_000
                readTimeout = 60_000
                instanceFollowRedirects = true
            }
            try {
                check(connection.responseCode in 200..299) { "No se pudo descargar la actualización" }
                connection.inputStream.use { input -> temporary.outputStream().use { output -> input.copyTo(output) } }
            } finally {
                connection.disconnect()
            }
            check(sha256(temporary) == info.sha256) { "La firma SHA-256 del APK no coincide" }
            verifyPackageAndSigner(temporary)
            if (apk.exists()) apk.delete()
            check(temporary.renameTo(apk)) { "No se pudo preparar el APK" }

            val uri = FileProvider.getUriForFile(context, "${context.packageName}.updates", apk)
            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(uri, "application/vnd.android.package-archive")
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_GRANT_READ_URI_PERMISSION)
                clipData = android.content.ClipData.newRawUri("actualización", uri)
            }
            context.startActivity(intent)
        }
    }

    @Suppress("DEPRECATION")
    private fun verifyPackageAndSigner(apk: File) {
        val flags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) PackageManager.GET_SIGNING_CERTIFICATES else PackageManager.GET_SIGNATURES
        val archive = checkNotNull(context.packageManager.getPackageArchiveInfo(apk.absolutePath, flags))
        check(archive.packageName == context.packageName) { "El APK pertenece a otra aplicación" }
        val installed = context.packageManager.getPackageInfo(context.packageName, flags)
        val archiveSigners = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) archive.signingInfo?.apkContentsSigners?.map { fingerprint(it.toByteArray()) } else archive.signatures?.map { fingerprint(it.toByteArray()) }
        val installedSigners = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) installed.signingInfo?.apkContentsSigners?.map { fingerprint(it.toByteArray()) } else installed.signatures?.map { fingerprint(it.toByteArray()) }
        check(!archiveSigners.isNullOrEmpty() && archiveSigners == installedSigners) { "La firma del APK no coincide con la instalada" }
    }

    private fun fingerprint(value: ByteArray): String = MessageDigest.getInstance("SHA-256")
        .digest(value).joinToString("") { "%02x".format(it) }

    private fun sha256(file: File): String {
        val digest = MessageDigest.getInstance("SHA-256")
        file.inputStream().use { input ->
            val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
            while (true) {
                val count = input.read(buffer)
                if (count < 0) break
                digest.update(buffer, 0, count)
            }
        }
        return digest.digest().joinToString("") { "%02x".format(it) }
    }
}
