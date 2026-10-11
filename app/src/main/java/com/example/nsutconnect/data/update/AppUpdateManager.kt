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
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicLong

import okhttp3.ConnectionPool
import okhttp3.Dispatcher

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

    private val dispatcher = Dispatcher().apply {
        maxRequests = 32
        maxRequestsPerHost = 16
    }

    private val httpClient = OkHttpClient.Builder()
        .dispatcher(dispatcher)
        .connectionPool(ConnectionPool(16, 5, TimeUnit.MINUTES))
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(25, TimeUnit.SECONDS)
        .build()

    private const val DIRECT_EDGE_APK_URL = "https://nsync-app.vercel.app/downloads/NSync.apk"
    private const val DIRECT_EDGE_VERSION_URL = "https://nsync-app.vercel.app/version.json"

    /**
     * Resolves high-speed CDN and Cloudflare edge mirror candidates for release assets.
     * Directs traffic to Vercel and Cloudflare edges in Delhi/Mumbai for line-rate 5G speed,
     * with transparent fallback to backup CDN and original URL.
     */
    fun getCandidateUrls(sourceUrl: String): List<String> {
        val candidates = mutableListOf<String>()
        candidates.add(DIRECT_EDGE_APK_URL)
        if (sourceUrl.isNotBlank() && sourceUrl != DIRECT_EDGE_APK_URL) {
            if (sourceUrl.contains("github.com", ignoreCase = true)) {
                candidates.add("https://gh-proxy.com/$sourceUrl")
                candidates.add("https://ghproxy.net/$sourceUrl")
            }
            candidates.add(sourceUrl)
        }
        return candidates
    }

    /**
     * Checks if a newer version of NSync is available.
     * Prioritizes direct Vercel edge endpoint, with fallback to GitHub Releases.
     */
    suspend fun checkForUpdates(context: Context): AppUpdateInfo = withContext(Dispatchers.IO) {
        try {
            var tagName = ""
            var releaseTitle = "Update Available"
            var releaseNotes = "Performance improvements, automated sync, and bug fixes"
            var publishedAt = ""
            var apkUrl = ""

            // Strategy 1: Check high-speed direct edge version endpoint
            try {
                val edgeReq = Request.Builder()
                    .url(DIRECT_EDGE_VERSION_URL)
                    .header("Accept", "application/json")
                    .header("User-Agent", "NSync-Android-App/${BuildConfig.VERSION_NAME}")
                    .build()
                val edgeResp = httpClient.newCall(edgeReq).execute()
                if (edgeResp.isSuccessful) {
                    val body = edgeResp.body?.string()
                    if (!body.isNullOrBlank()) {
                        val json = JSONObject(body)
                        tagName = json.optString("version", "").removePrefix("v").trim()
                        releaseTitle = json.optString("name", "Update Available")
                        releaseNotes = json.optString("notes", releaseNotes)
                        publishedAt = json.optString("publishedAt", "")
                        apkUrl = json.optString("downloadUrl", DIRECT_EDGE_APK_URL)
                    }
                }
            } catch (e: Exception) {
                // Ignore edge check failure and proceed to GitHub fallback
            }

            // Strategy 2: Try GitHub REST API (if edge endpoint was unreachable or blank)
            if (tagName.isBlank()) {
                val request = Request.Builder()
                    .url(GITHUB_LATEST_RELEASE_URL)
                    .header("Accept", "application/vnd.github.v3+json")
                    .header("User-Agent", "NSync-Android-App/${BuildConfig.VERSION_NAME}")
                    .build()

                val response = try { httpClient.newCall(request).execute() } catch (e: Exception) { null }
                if (response != null && response.isSuccessful) {
                    val body = response.body?.string()
                    if (!body.isNullOrBlank()) {
                        val json = JSONObject(body)
                        tagName = json.optString("tag_name", "").removePrefix("v").trim()
                        releaseTitle = json.optString("name", "Update Available")
                        releaseNotes = json.optString("body", releaseNotes)
                        publishedAt = json.optString("published_at", "")

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
                    }
                }
            }

            // Strategy 3: Fallback to GitHub Web redirect if API was rate-limited (403) or failed
            if (tagName.isBlank()) {
                val noRedirectClient = httpClient.newBuilder().followRedirects(false).build()
                val webRequest = Request.Builder()
                    .url("https://github.com/$GITHUB_REPO/releases/latest")
                    .header("User-Agent", "Mozilla/5.0 (Android; Mobile)")
                    .build()

                val webResponse = try { noRedirectClient.newCall(webRequest).execute() } catch (e: Exception) { null }
                if (webResponse != null) {
                    val redirectUrl = webResponse.header("Location") ?: ""
                    if (redirectUrl.contains("/tag/")) {
                        tagName = redirectUrl.substringAfterLast("/tag/").removePrefix("v").trim()
                    }
                }
            }

            if (apkUrl.isBlank() && tagName.isNotBlank()) {
                apkUrl = "https://github.com/$GITHUB_REPO/releases/download/v$tagName/NSync-latest.apk"
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
     * Uses Cloudflare edge mirrors, 8 concurrent parallel streams, 128KB buffers,
     * segment auto-retry, and throttled UI dispatch to achieve line-rate 5G download speed.
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

            val cacheFile = File(context.cacheDir, "nsync_update.apk")
            if (cacheFile.exists()) {
                cacheFile.delete()
            }

            val candidates = getCandidateUrls(apkUrl)
            var downloadSuccess = false
            var lastError: String? = null

            for (candidateUrl in candidates) {
                try {
                    // Probe candidate URL and Range capabilities with bytes=0-0
                    val probeReq = Request.Builder()
                        .url(candidateUrl)
                        .header("User-Agent", "NSync-Android-App/${BuildConfig.VERSION_NAME}")
                        .header("Range", "bytes=0-0")
                        .build()

                    val probeResp = try {
                        httpClient.newCall(probeReq).execute()
                    } catch (e: Exception) {
                        null
                    }

                    if (probeResp == null || (!probeResp.isSuccessful && probeResp.code != 206)) {
                        probeResp?.close()
                        continue
                    }

                    val finalUrl = probeResp.request.url.toString()
                    val contentRange = probeResp.header("Content-Range")
                    val totalBytes = if (contentRange != null && contentRange.contains("/")) {
                        contentRange.substringAfterLast("/").trim().toLongOrNull() ?: -1L
                    } else {
                        probeResp.header("Content-Length")?.toLongOrNull() ?: -1L
                    }
                    val supportsRange = (probeResp.code == 206 || probeResp.header("Accept-Ranges")?.contains("bytes", true) == true) && totalBytes > 1024 * 1024
                    probeResp.close()

                    if (supportsRange) {
                        // High-Speed Multi-Threaded Parallel Segmented Download (8 concurrent streams)
                        val numSegments = 8
                        val segmentSize = (totalBytes + numSegments - 1) / numSegments
                        val downloadedBytes = AtomicLong(0L)
                        var lastReportedProgress = 0f
                        var lastReportedTime = 0L

                        val partFiles = (0 until numSegments).map { i ->
                            File(context.cacheDir, "nsync_part_$i.tmp").also { if (it.exists()) it.delete() }
                        }

                        coroutineScope {
                            val deferreds = (0 until numSegments).map { i ->
                                async(Dispatchers.IO) {
                                    val start = i * segmentSize
                                    val end = minOf(totalBytes - 1, (i + 1) * segmentSize - 1)
                                    if (start > end) return@async

                                    val partFile = partFiles[i]
                                    var segmentDownloaded = false
                                    var lastSegEx: Exception? = null

                                    // Up to 2 retries per segment in case of transient socket hiccup
                                    for (attempt in 1..2) {
                                        try {
                                            val partReq = Request.Builder()
                                                .url(finalUrl)
                                                .header("User-Agent", "NSync-Android-App/${BuildConfig.VERSION_NAME}")
                                                .header("Range", "bytes=$start-$end")
                                                .build()

                                            val partResp = httpClient.newCall(partReq).execute()
                                            if (!partResp.isSuccessful && partResp.code != 206) {
                                                partResp.close()
                                                throw java.io.IOException("Segment $i failed: HTTP ${partResp.code}")
                                            }

                                            val partBody = partResp.body ?: throw java.io.IOException("Empty segment body")
                                            java.io.BufferedInputStream(partBody.byteStream(), 128 * 1024).use { input ->
                                                java.io.BufferedOutputStream(FileOutputStream(partFile), 128 * 1024).use { output ->
                                                    val buf = ByteArray(64 * 1024)
                                                    var read: Int
                                                    while (input.read(buf).also { read = it } != -1) {
                                                        output.write(buf, 0, read)
                                                        val currentTotal = downloadedBytes.addAndGet(read.toLong())
                                                        val progress = currentTotal.toFloat() / totalBytes
                                                        val now = System.currentTimeMillis()
                                                        if (progress - lastReportedProgress >= 0.01f || now - lastReportedTime >= 50L) {
                                                            lastReportedProgress = progress
                                                            lastReportedTime = now
                                                            withContext(Dispatchers.Main) {
                                                                onProgress(progress)
                                                            }
                                                        }
                                                    }
                                                    output.flush()
                                                }
                                            }
                                            segmentDownloaded = true
                                            break
                                        } catch (e: Exception) {
                                            lastSegEx = e
                                            if (partFile.exists()) partFile.delete()
                                        }
                                    }

                                    if (!segmentDownloaded) {
                                        throw lastSegEx ?: java.io.IOException("Segment $i failed after retries")
                                    }
                                }
                            }
                            deferreds.awaitAll()
                        }

                        // Concatenate all 8 segments into final APK using 256KB stream buffer
                        java.io.BufferedOutputStream(FileOutputStream(cacheFile), 256 * 1024).use { mergedOut ->
                            for (partFile in partFiles) {
                                if (partFile.exists()) {
                                    java.io.BufferedInputStream(FileInputStream(partFile), 256 * 1024).use { partIn ->
                                        partIn.copyTo(mergedOut)
                                    }
                                    partFile.delete()
                                }
                            }
                            mergedOut.flush()
                        }

                        if (cacheFile.length() > 0) {
                            downloadSuccess = true
                            break
                        }
                    } else {
                        // High-Speed Single-Stream Download fallback with 128KB buffer
                        val request = Request.Builder()
                            .url(finalUrl)
                            .header("User-Agent", "NSync-Android-App/${BuildConfig.VERSION_NAME}")
                            .build()

                        val response = httpClient.newCall(request).execute()
                        if (!response.isSuccessful) {
                            response.close()
                            continue
                        }

                        val responseBody = response.body ?: continue
                        val streamLength = if (totalBytes > 0) totalBytes else responseBody.contentLength()
                        var singleDownloaded = 0L
                        var lastReportedProgress = 0f
                        var lastReportedTime = 0L

                        java.io.BufferedInputStream(responseBody.byteStream(), 128 * 1024).use { input ->
                            java.io.BufferedOutputStream(FileOutputStream(cacheFile), 128 * 1024).use { output ->
                                val buffer = ByteArray(64 * 1024)
                                var read: Int
                                while (input.read(buffer).also { read = it } != -1) {
                                    output.write(buffer, 0, read)
                                    singleDownloaded += read

                                    if (streamLength > 0) {
                                        val progress = singleDownloaded.toFloat() / streamLength
                                        val now = System.currentTimeMillis()
                                        if (progress - lastReportedProgress >= 0.01f || now - lastReportedTime >= 50L || singleDownloaded == streamLength) {
                                            lastReportedProgress = progress
                                            lastReportedTime = now
                                            withContext(Dispatchers.Main) {
                                                onProgress(progress)
                                            }
                                        }
                                    }
                                }
                                output.flush()
                            }
                        }

                        if (cacheFile.length() > 0) {
                            downloadSuccess = true
                            break
                        }
                    }
                } catch (e: Exception) {
                    lastError = e.message
                    if (cacheFile.exists()) cacheFile.delete()
                }
            }

            if (!downloadSuccess || !cacheFile.exists() || cacheFile.length() == 0L) {
                onError("Failed to download update: ${lastError ?: "Network connection error"}")
                return@withContext
            }

            // Ensure 100% progress is posted
            withContext(Dispatchers.Main) {
                onProgress(1f)
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
