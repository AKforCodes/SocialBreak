package com.socialbreak.android.data

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.core.content.FileProvider
import com.socialbreak.android.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.net.HttpURLConnection
import java.net.URL

data class UpdateInfo(
    val hasUpdate: Boolean,
    val currentVersion: String,
    val latestVersion: String,
    val releaseName: String,
    val releaseNotes: String,
    val downloadUrl: String,
    val apkSize: Long,
    val publishedAt: String
)

sealed class UpdateCheckResult {
    data class UpdateAvailable(val info: UpdateInfo) : UpdateCheckResult()
    data class UpToDate(val currentVersion: String) : UpdateCheckResult()
    data class Error(val message: String) : UpdateCheckResult()
}

sealed class DownloadState {
    object Idle : DownloadState()
    data class Downloading(val progress: Float, val downloadedBytes: Long, val totalBytes: Long) : DownloadState()
    data class ReadyToInstall(val apkFile: File, val apkUri: Uri) : DownloadState()
    data class Failed(val error: String) : DownloadState()
}

class AppUpdateManager(private val context: Context) {

    companion object {
        private const val GITHUB_OWNER = "AKforCodes"
        private const val GITHUB_REPO = "SocialBreak"
        private const val API_LATEST_RELEASE = "https://api.github.com/repos/$GITHUB_OWNER/$GITHUB_REPO/releases/latest"
        const val GITHUB_RELEASES_PAGE = "https://github.com/$GITHUB_OWNER/$GITHUB_REPO/releases"
    }

    /**
     * Checks the GitHub Releases API for a new version of SocialBreak.
     */
    suspend fun checkForUpdate(): UpdateCheckResult = withContext(Dispatchers.IO) {
        try {
            val url = URL(API_LATEST_RELEASE)
            val connection = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                connectTimeout = 10000
                readTimeout = 10000
                setRequestProperty("Accept", "application/vnd.github.v3+json")
                setRequestProperty("User-Agent", "SocialBreak-Android/${BuildConfig.VERSION_NAME}")
            }

            val responseCode = connection.responseCode
            if (responseCode == HttpURLConnection.HTTP_NOT_FOUND) {
                return@withContext UpdateCheckResult.Error("No releases found on GitHub repository.")
            }
            if (responseCode != HttpURLConnection.HTTP_OK) {
                return@withContext UpdateCheckResult.Error("GitHub API returned status $responseCode")
            }

            val responseBody = connection.inputStream.bufferedReader().use { it.readText() }
            val json = JSONObject(responseBody)

            val tagName = json.optString("tag_name", "").trim()
            val releaseName = json.optString("name", tagName)
            val body = json.optString("body", "Bug fixes and performance enhancements.")
            val publishedAt = json.optString("published_at", "")

            // Parse APK download URL from assets
            var apkDownloadUrl = ""
            var apkSize = 0L
            val assetsArray = json.optJSONArray("assets")
            if (assetsArray != null) {
                for (i in 0 until assetsArray.length()) {
                    val asset = assetsArray.getJSONObject(i)
                    val assetName = asset.optString("name", "")
                    if (assetName.endsWith(".apk", ignoreCase = true)) {
                        apkDownloadUrl = asset.optString("browser_download_url", "")
                        apkSize = asset.optLong("size", 0L)
                        break
                    }
                }
            }

            val cleanLatestVersion = tagName.removePrefix("v").trim()
            val currentVersion = BuildConfig.VERSION_NAME.removePrefix("v").trim()

            val hasUpdate = isVersionGreater(cleanLatestVersion, currentVersion) && apkDownloadUrl.isNotEmpty()

            val updateInfo = UpdateInfo(
                hasUpdate = hasUpdate,
                currentVersion = BuildConfig.VERSION_NAME,
                latestVersion = if (tagName.startsWith("v")) tagName else "v$tagName",
                releaseName = releaseName,
                releaseNotes = body,
                downloadUrl = apkDownloadUrl,
                apkSize = apkSize,
                publishedAt = publishedAt
            )

            if (hasUpdate) {
                UpdateCheckResult.UpdateAvailable(updateInfo)
            } else {
                UpdateCheckResult.UpToDate(BuildConfig.VERSION_NAME)
            }
        } catch (e: Exception) {
            UpdateCheckResult.Error("Update check failed: ${e.localizedMessage ?: "Network error"}")
        }
    }

    /**
     * Downloads the APK file to cache directory with progress reporting.
     */
    suspend fun downloadApk(
        updateInfo: UpdateInfo,
        onProgress: (Float, Long, Long) -> Unit
    ): File = withContext(Dispatchers.IO) {
        val updatesDir = File(context.cacheDir, "updates").apply { mkdirs() }
        val apkFile = File(updatesDir, "SocialBreak-${updateInfo.latestVersion}.apk")

        if (apkFile.exists() && apkFile.length() == updateInfo.apkSize && updateInfo.apkSize > 0) {
            onProgress(1f, updateInfo.apkSize, updateInfo.apkSize)
            return@withContext apkFile
        }

        // Temporary download file
        val tempFile = File(updatesDir, "download_temp.apk")
        if (tempFile.exists()) tempFile.delete()

        val url = URL(updateInfo.downloadUrl)
        var connection = url.openConnection() as HttpURLConnection
        connection.connectTimeout = 15000
        connection.readTimeout = 30000
        connection.instanceFollowRedirects = true
        connection.setRequestProperty("User-Agent", "SocialBreak-Android/${BuildConfig.VERSION_NAME}")

        var redirect = false
        val status = connection.responseCode
        if (status == HttpURLConnection.HTTP_MOVED_TEMP || status == HttpURLConnection.HTTP_MOVED_PERM || status == HttpURLConnection.HTTP_SEE_OTHER) {
            redirect = true
        }

        if (redirect) {
            val newUrl = connection.getHeaderField("Location")
            connection.disconnect()
            connection = URL(newUrl).openConnection() as HttpURLConnection
            connection.connectTimeout = 15000
            connection.readTimeout = 30000
            connection.setRequestProperty("User-Agent", "SocialBreak-Android/${BuildConfig.VERSION_NAME}")
        }

        val totalLength = connection.contentLength.takeIf { it > 0 } ?: updateInfo.apkSize.toInt()

        connection.inputStream.use { input ->
            FileOutputStream(tempFile).use { output ->
                val buffer = ByteArray(8192)
                var bytesRead: Int
                var totalRead = 0L

                while (input.read(buffer).also { bytesRead = it } != -1) {
                    output.write(buffer, 0, bytesRead)
                    totalRead += bytesRead
                    if (totalLength > 0) {
                        val progress = (totalRead.toFloat() / totalLength).coerceIn(0f, 1f)
                        onProgress(progress, totalRead, totalLength.toLong())
                    }
                }
                output.flush()
            }
        }

        if (apkFile.exists()) apkFile.delete()
        tempFile.renameTo(apkFile)
        onProgress(1f, totalLength.toLong(), totalLength.toLong())
        apkFile
    }

    /**
     * Installs the downloaded APK via Android PackageInstaller / FileProvider Intent.
     */
    fun installApk(apkFile: File): Boolean {
        return try {
            val apkUri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                apkFile
            )

            // For Android 8.0+, check if unknown app sources are allowed
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                if (!context.packageManager.canRequestPackageInstalls()) {
                    val settingsIntent = Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES).apply {
                        data = Uri.parse("package:${context.packageName}")
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK
                    }
                    context.startActivity(settingsIntent)
                    return false
                }
            }

            val installIntent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(apkUri, "application/vnd.android.package-archive")
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_GRANT_READ_URI_PERMISSION
            }
            context.startActivity(installIntent)
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    /**
     * Compares semantic versions (e.g. "1.3.0" vs "1.2.0")
     */
    private fun isVersionGreater(v1: String, v2: String): Boolean {
        try {
            val parts1 = v1.split(".").map { it.filter { char -> char.isDigit() }.toIntOrNull() ?: 0 }
            val parts2 = v2.split(".").map { it.filter { char -> char.isDigit() }.toIntOrNull() ?: 0 }

            val maxLen = maxOf(parts1.size, parts2.size)
            for (i in 0 until maxLen) {
                val p1 = parts1.getOrElse(i) { 0 }
                val p2 = parts2.getOrElse(i) { 0 }
                if (p1 > p2) return true
                if (p1 < p2) return false
            }
            return false
        } catch (_: Exception) {
            return v1 != v2
        }
    }
}

