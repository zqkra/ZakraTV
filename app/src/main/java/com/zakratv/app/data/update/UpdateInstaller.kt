package com.zakratv.app.data.update

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import androidx.core.content.FileProvider
import com.zakratv.app.data.remote.HttpClientFactory
import okhttp3.Request
import java.io.File

object UpdateInstaller {

    /**
     * Downloads APK to cache and launches the system installer.
     * User still confirms install (Android security). Returns error message or null on success.
     */
    fun downloadAndPromptInstall(context: Context, apkUrl: String): String? {
        return try {
            val dir = File(context.cacheDir, "updates").apply { mkdirs() }
            val out = File(dir, "ZakraTV-update.apk")
            if (out.exists()) out.delete()

            val request = Request.Builder()
                .url(apkUrl)
                .header("User-Agent", "ZakraTV-Updater")
                .get()
                .build()
            // mediaClient has NO callTimeout: a 4-5 MB APK on slow Fire Stick Wi-Fi can take
            // longer than 45s, and a call timeout would leave the APK half-written → won't install.
            HttpClientFactory.mediaClient.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    return "Descarga fallida HTTP ${response.code}"
                }
                val body = response.body ?: return "Respuesta vacía"
                out.outputStream().use { body.byteStream().copyTo(it) }
            }
            val expected = out.length()
            if (!out.exists() || expected < 1_000_000) {
                return "APK incompleto (${expected} bytes). Reintenta con mejor conexión."
            }
            openInstaller(context, out)
            null
        } catch (e: Exception) {
            e.message ?: "Error al actualizar"
        }
    }

    private fun openInstaller(context: Context, apk: File) {
        val uri: Uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            apk,
        )
        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, "application/vnd.android.package-archive")
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(intent)
    }

    fun canRequestPackageInstalls(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.packageManager.canRequestPackageInstalls()
        } else {
            true
        }
    }

    fun openUnknownSourcesSettings(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val intent = Intent(
                android.provider.Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES,
                Uri.parse("package:${context.packageName}"),
            ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(intent)
        }
    }
}
