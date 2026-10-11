package com.example.nsutconnect.data.update

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.core.content.FileProvider
import com.example.nsutconnect.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.util.concurrent.TimeUnit

data class AppUpdateInfo(
    val hasUpdate: Boolean,
    val latestVersionName: String,
    val currentVersionName: String = BuildConfig.VERSION_NAME,
    val releaseTitle: String = "",
    val releaseNotes: String = "",
    val apkDownloadUrl: String = "",
    val publishedAt: String = ""
)

object AppUpdateManager {

    private const val GITHUB_REPO = "senkuthegreat/NSync"
    private const val GITHUB_LATEST_RELEASE_URL = "https://api.github.com/repos/$GITHUB_REPO/releases/latest"

    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .build()

    /**
     * Checks if a newer version of NSync is available on GitHub Releases.
     */
    suspend fun checkForUpdates(context: Context): AppUpdateInfo = withContext(Dispatchers.IO) {
        try {
            val request = Request.Builder()
                .url(GITHUB_LATEST_RELEASE_URL)
                .header("Accept", "application/vnd.github.v3+json")
                .header("User-Agent", "NSync-Android-App/${BuildConfig.VERSION_NAME}")
                .build()

            val response = httpClient.newCall(request).execute()
            if (!response.isSuccessful) {
                return@withContext AppUpdateInfo(
                    hasUpdate = false,
                    latestVersionName = BuildConfig.VERSION_NAME
                )
            }

            val body = response.body?.string() ?: return@withContext AppUpdateInfo(
                hasUpdate = false,
                latestVersionName = BuildConfig.VERSION_NAME
            )

            val json = JSONObject(body)
            val tagName = json.optString("tag_name", "").removePrefix("v").trim()
            val releaseTitle = json.optString("name", "Update Available")
            val releaseNotes = json.optString("body", "Bug fixes and improvements")
            val publishedAt = json.optString("published_at", "")

            // Look for .apk asset
            var apkUrl = ""
            val assets = json.optJSONArray("assets")
            if (assets != null) {
                for (i in 0 until assets.length()) {
                    val asset = assets.getJSONObject(i)
                    val name = asset.optString("name", "")
                    if (name.endsWith(".apk", ignoreCase = true)) {
                        apkUrl = asset.optString("browser_download_url", "")
                        break
                    }
                }
            }

            val currentVersion = BuildConfig.VERSION_NAME
            val isNewer = isVersionNewer(remoteVersion = tagName, currentVersion = currentVersion)

            AppUpdateInfo(
                hasUpdate = isNewer && apkUrl.isNotBlank(),
                latestVersionName = tagName.ifEmpty { currentVersion },
                currentVersionName = currentVersion,
                releaseTitle = releaseTitle,
                releaseNotes = releaseNotes,
                apkDownloadUrl = apkUrl,
                publishedAt = publishedAt
            )
        } catch (e: Exception) {
            e.printStackTrace()
            AppUpdateInfo(
                hasUpdate = false,
                latestVersionName = BuildConfig.VERSION_NAME
            )
        }
    }

    /**
     * Downloads the APK file and triggers the native package installer.
     */
    suspend fun downloadAndInstallApk(
        context: Context,
        apkUrl: String,
        onProgress: (Float) -> Unit,
        onError: (String) -> Unit
    ) = withContext(Dispatchers.IO) {
        try {
            // Check Android 8.0+ Unknown App Sources Permission
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                if (!context.packageManager.canRequestPackageInstalls()) {
                    withContext(Dispatchers.Main) {
                        val intent = Intent(
                            Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES,
                            Uri.parse("package:${context.packageName}")
                        ).apply {
                            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        }
                        context.startActivity(intent)
                    }
                    onError("Please allow installing apps from this source, then tap Update again.")
                    return@withContext
                }
            }

            val request = Request.Builder()
                .url(apkUrl)
                .header("User-Agent", "NSync-Android-App/${BuildConfig.VERSION_NAME}")
                .build()

            val response = httpClient.newCall(request).execute()
            if (!response.isSuccessful) {
                onError("Failed to download update: Server returned ${response.code}")
                return@withContext
            }

            val responseBody = response.body ?: run {
                onError("Empty response from update server")
                return@withContext
            }

            val totalBytes = responseBody.contentLength()
            val cacheFile = File(context.cacheDir, "nsync_update.apk")
            if (cacheFile.exists()) {
                cacheFile.delete()
            }

            var downloadedBytes = 0L
            responseBody.byteStream().use { input ->
                FileOutputStream(cacheFile).use { output ->
                    val buffer = ByteArray(8 * 1024)
                    var read: Int
                    while (input.read(buffer).also { read = it } != -1) {
                        output.write(buffer, 0, read)
                        downloadedBytes += read
                        if (totalBytes > 0) {
                            val progress = downloadedBytes.toFloat() / totalBytes
                            withContext(Dispatchers.Main) {
                                onProgress(progress)
                            }
                        }
                    }
                    output.flush()
                }
            }

            // Launch package installer
            withContext(Dispatchers.Main) {
                val apkUri: Uri = FileProvider.getUriForFile(
                    context,
                    "${context.packageName}.fileprovider",
                    cacheFile
                )

                val installIntent = Intent(Intent.ACTION_VIEW).apply {
                    setDataAndType(apkUri, "application/vnd.android.package-archive")
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }

                context.startActivity(installIntent)
            }
        } catch (e: Exception) {
            e.printStackTrace()
            withContext(Dispatchers.Main) {
                onError(e.message ?: "Failed to download update")
            }
        }
    }

    /**
     * Compares two semantic version strings (e.g. "1.1.0" vs "1.0.0").
     */
    fun isVersionNewer(remoteVersion: String, currentVersion: String): Boolean {
        if (remoteVersion.isBlank()) return false
        val remoteParts = remoteVersion.split(".", "-").mapNotNull { it.toIntOrNull() }
        val currentParts = currentVersion.split(".", "-").mapNotNull { it.toIntOrNull() }

        val length = maxOf(remoteParts.size, currentParts.size)
        for (i in 0 until length) {
            val r = remoteParts.getOrElse(i) { 0 }
            val c = currentParts.getOrElse(i) { 0 }
            if (r > c) return true
            if (r < c) return false
        }
        return false
    }
}
