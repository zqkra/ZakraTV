package com.zakratv.app.data.update

import com.zakratv.app.BuildConfig
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.concurrent.TimeUnit

@Serializable
data class GhRelease(
    @SerialName("tag_name") val tagName: String = "",
    val name: String = "",
    val body: String = "",
    val assets: List<GhAsset> = emptyList(),
    @SerialName("html_url") val htmlUrl: String = "",
)

@Serializable
data class GhAsset(
    val name: String = "",
    @SerialName("browser_download_url") val browserDownloadUrl: String = "",
    val size: Long = 0,
)

// Re-export for UI
typealias AvailableUpdate = UpdateVersionLogic.AvailableUpdate

/**
 * Auto-update via **public** GitHub Releases API.
 * No private key / no GitHub token needed for public repos.
 */
object AppUpdateChecker {

    val LATEST_DOWNLOAD_URL: String get() = UpdateVersionLogic.LATEST_DOWNLOAD_URL

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        coerceInputValues = true
    }

    private val client = OkHttpClient.Builder()
        .connectTimeout(12, TimeUnit.SECONDS)
        .readTimeout(20, TimeUnit.SECONDS)
        .build()

    fun currentVersionName(): String = BuildConfig.VERSION_NAME
    fun currentVersionCode(): Int = BuildConfig.VERSION_CODE

    fun checkForUpdate(): AvailableUpdate? {
        val url =
            "https://api.github.com/repos/${UpdateVersionLogic.GITHUB_OWNER}/${UpdateVersionLogic.GITHUB_REPO}/releases/latest"
        val request = Request.Builder()
            .url(url)
            .header("Accept", "application/vnd.github+json")
            .header("User-Agent", "ZakraTV/${BuildConfig.VERSION_NAME}")
            .get()
            .build()
        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) return null
            val body = response.body?.string().orEmpty()
            if (body.isBlank()) return null
            val release = json.decodeFromString(GhRelease.serializer(), body)
            return evaluateRelease(release, BuildConfig.VERSION_CODE, BuildConfig.VERSION_NAME)
        }
    }

    fun evaluateRelease(
        release: GhRelease,
        installedCode: Int,
        installedName: String,
    ): AvailableUpdate? = UpdateVersionLogic.evaluateRelease(
        UpdateVersionLogic.ReleaseInfo(
            tagName = release.tagName,
            name = release.name,
            body = release.body,
            assets = release.assets.map {
                UpdateVersionLogic.AssetInfo(it.name, it.browserDownloadUrl)
            },
        ),
        installedCode,
        installedName,
    )

    fun parseVersionName(tag: String, name: String) = UpdateVersionLogic.parseVersionName(tag, name)
    fun versionNameToCode(versionName: String) = UpdateVersionLogic.versionNameToCode(versionName)
}
