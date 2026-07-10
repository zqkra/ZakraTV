package com.zakratv.app.data.remote

import com.zakratv.app.data.model.RdDeviceCode
import com.zakratv.app.data.model.RdTokenResponse
import com.zakratv.app.data.model.RdTorrentAdded
import com.zakratv.app.data.model.RdTorrentInfo
import com.zakratv.app.data.model.RdUnrestrict
import com.zakratv.app.data.model.RdUser
import kotlinx.serialization.json.Json
import okhttp3.FormBody
import okhttp3.Request

/**
 * Real-Debrid REST client.
 * Prefer pre-baked API token (no interactive OAuth). Device code flow is optional fallback.
 */
class RealDebridApi(
    private var token: String = "",
) {
    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        coerceInputValues = true
    }

    private val api = "https://api.real-debrid.com/rest/1.0"
    private val oauth = "https://api.real-debrid.com/oauth/v2"

    // Public OAuth client id used by many open TV clients for device flow
    private val clientId = "X245A4XAIBGVM"

    fun setToken(value: String) {
        token = value.trim()
    }

    fun hasToken(): Boolean = token.isNotBlank()

    fun getToken(): String = token

    fun user(): RdUser {
        val body = get("$api/user")
        return json.decodeFromString(RdUser.serializer(), body)
    }

    fun unrestrict(link: String): RdUnrestrict {
        val form = FormBody.Builder().add("link", link).build()
        val body = post("$api/unrestrict/link", form)
        return json.decodeFromString(RdUnrestrict.serializer(), body)
    }

    fun addMagnet(magnet: String): RdTorrentAdded {
        val form = FormBody.Builder().add("magnet", magnet).build()
        val body = post("$api/torrents/addMagnet", form)
        return json.decodeFromString(RdTorrentAdded.serializer(), body)
    }

    fun selectFiles(id: String, files: String = "all") {
        val form = FormBody.Builder().add("files", files).build()
        post("$api/torrents/selectFiles/$id", form)
    }

    fun torrentInfo(id: String): RdTorrentInfo {
        val body = get("$api/torrents/info/$id")
        return json.decodeFromString(RdTorrentInfo.serializer(), body)
    }

    fun deleteTorrent(id: String) {
        val request = Request.Builder()
            .url("$api/torrents/delete/$id")
            .header("Authorization", "Bearer $token")
            .delete()
            .build()
        HttpClientFactory.rdClient.newCall(request).execute().use { /* ignore */ }
    }

    /**
     * Resolve magnet → RD direct download URL (cached preferred).
     * Returns first streamable unrestricted link or null.
     */
    fun resolveMagnetToDirect(magnet: String, maxWaitMs: Long = 25_000L): String? {
        val added = addMagnet(magnet)
        if (added.id.isBlank()) return null
        try {
            selectFiles(added.id, "all")
            val start = System.currentTimeMillis()
            while (System.currentTimeMillis() - start < maxWaitMs) {
                val info = torrentInfo(added.id)
                when (info.status) {
                    "downloaded", "uploading" -> {
                        val link = info.links.firstOrNull() ?: return null
                        val unrestricted = unrestrict(link)
                        return unrestricted.download.ifBlank { unrestricted.link }
                            .takeIf { it.isNotBlank() }
                    }
                    "error", "virus", "dead" -> return null
                    else -> Thread.sleep(1500)
                }
            }
        } catch (_: Exception) {
            return null
        }
        return null
    }

    // --- Optional device OAuth (only if no token) ---

    fun deviceCode(): RdDeviceCode {
        val url = "$oauth/device/code?client_id=$clientId&new_credentials=yes"
        val body = getPublic(url)
        return json.decodeFromString(RdDeviceCode.serializer(), body)
    }

    fun pollDeviceToken(deviceCode: String, clientId: String = this.clientId, clientSecret: String = ""): RdTokenResponse {
        val form = FormBody.Builder()
            .add("client_id", clientId)
            .add("client_secret", clientSecret)
            .add("code", deviceCode)
            .add("grant_type", "http://oauth.net/grant_type/device/1.0")
            .build()
        val request = Request.Builder()
            .url("$oauth/token")
            .post(form)
            .build()
        HttpClientFactory.rdClient.newCall(request).execute().use { response ->
            val body = response.body?.string().orEmpty()
            return json.decodeFromString(RdTokenResponse.serializer(), body)
        }
    }

    private fun get(url: String): String {
        require(hasToken()) { "Real-Debrid sin token" }
        val request = Request.Builder()
            .url(url)
            .header("Authorization", "Bearer $token")
            .header("Accept", "application/json")
            .get()
            .build()
        HttpClientFactory.rdClient.newCall(request).execute().use { response ->
            val body = response.body?.string().orEmpty()
            if (!response.isSuccessful) {
                throw IllegalStateException("RD HTTP ${response.code}: ${body.take(200)}")
            }
            return body
        }
    }

    private fun post(url: String, form: FormBody): String {
        require(hasToken()) { "Real-Debrid sin token" }
        val request = Request.Builder()
            .url(url)
            .header("Authorization", "Bearer $token")
            .header("Accept", "application/json")
            .post(form)
            .build()
        HttpClientFactory.rdClient.newCall(request).execute().use { response ->
            val body = response.body?.string().orEmpty()
            if (!response.isSuccessful) {
                throw IllegalStateException("RD HTTP ${response.code}: ${body.take(200)}")
            }
            return body
        }
    }

    private fun getPublic(url: String): String {
        val request = Request.Builder().url(url).get().build()
        HttpClientFactory.rdClient.newCall(request).execute().use { response ->
            val body = response.body?.string().orEmpty()
            if (!response.isSuccessful) {
                throw IllegalStateException("RD OAuth HTTP ${response.code}")
            }
            return body
        }
    }
}
