package com.zakratv.app.data.update

/**
 * Pure version/update decision logic (no Android, no network) — JVM-testable.
 */
object UpdateVersionLogic {

    const val APK_ASSET_NAME = "ZakraTV.apk"
    const val GITHUB_OWNER = "zqkra"
    const val GITHUB_REPO = "ZakraTV"

    const val LATEST_DOWNLOAD_URL =
        "https://github.com/$GITHUB_OWNER/$GITHUB_REPO/releases/latest/download/$APK_ASSET_NAME"

    data class ReleaseInfo(
        val tagName: String,
        val name: String = "",
        val body: String = "",
        val assets: List<AssetInfo> = emptyList(),
    )

    data class AssetInfo(
        val name: String,
        val browserDownloadUrl: String,
    )

    data class AvailableUpdate(
        val versionName: String,
        val versionCode: Int,
        val apkUrl: String,
        val notes: String,
        val tag: String,
    )

    fun evaluateRelease(
        release: ReleaseInfo,
        installedCode: Int,
        installedName: String,
    ): AvailableUpdate? {
        val apk = release.assets.firstOrNull {
            it.name.equals(APK_ASSET_NAME, ignoreCase = true) ||
                it.name.endsWith(".apk", ignoreCase = true)
        } ?: return null
        if (apk.browserDownloadUrl.isBlank()) return null

        val remoteCode = parseVersionCode(release) ?: 0
        val remoteName = parseVersionName(release.tagName, release.name)

        val newerByCode = remoteCode > 0 && remoteCode > installedCode
        val newerByName = remoteCode == 0 && isVersionNameNewer(remoteName, installedName)
        if (!newerByCode && !newerByName) return null

        return AvailableUpdate(
            versionName = remoteName.ifBlank { release.tagName },
            versionCode = if (remoteCode > 0) remoteCode else installedCode + 1,
            apkUrl = apk.browserDownloadUrl,
            notes = release.body.take(400),
            tag = release.tagName,
        )
    }

    fun parseVersionName(tag: String, name: String): String {
        val fromTag = tag.trim().removePrefix("v").removePrefix("V")
        if (fromTag.matches(Regex("""\d+(\.\d+)*"""))) return fromTag
        return Regex("""(\d+\.\d+(\.\d+)?)""").find(name)?.value ?: fromTag
    }

    fun parseVersionCode(release: ReleaseInfo): Int? {
        Regex("""versionCode\s*[:=]\s*(\d+)""", RegexOption.IGNORE_CASE)
            .find(release.body)
            ?.groupValues?.getOrNull(1)
            ?.toIntOrNull()
            ?.let { return it }
        return versionNameToCode(parseVersionName(release.tagName, release.name))
    }

    fun versionNameToCode(versionName: String): Int? {
        val parts = versionName.trim().removePrefix("v").split(".")
            .mapNotNull { it.toIntOrNull() }
        if (parts.isEmpty()) return null
        val major = parts.getOrElse(0) { 0 }
        val minor = parts.getOrElse(1) { 0 }
        val patch = parts.getOrElse(2) { 0 }
        return major * 10000 + minor * 100 + patch
    }

    fun isVersionNameNewer(remote: String, installed: String): Boolean {
        val r = versionNameToCode(remote) ?: return false
        val i = versionNameToCode(installed) ?: return remote != installed && remote.isNotBlank()
        return r > i
    }
}
