package com.example.norwinlabstools

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Handler
import android.os.Looper
import androidx.core.content.FileProvider
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream

class UpdateManager(private val context: Context) {

    private val client = OkHttpClient()
    private val mainHandler = Handler(Looper.getMainLooper())
    
    private val GITHUB_OWNER = "NorwinLabs"
    private val GITHUB_REPO = "NorwinLabsTools"
    private val GITHUB_API_URL = "https://api.github.com/repos/$GITHUB_OWNER/$GITHUB_REPO/releases/latest"

    interface UpdateCallback {
        fun onUpdateAvailable(latestVersion: String, downloadUrl: String)
        fun onNoUpdate()
        fun onError(error: String, url: String)
        fun onDownloadProgress(progress: Int)
    }

    fun checkForUpdates(callback: UpdateCallback) {
        val request = Request.Builder()
            .url(GITHUB_API_URL)
            .header("User-Agent", "NorwinLabsTools-App")
            .header("Accept", "application/vnd.github+json")
            .build()

        Thread {
            try {
                val response = client.newCall(request).execute()
                val responseBody = response.body?.string()

                if (response.isSuccessful && responseBody != null) {
                    val jsonObject = JSONObject(responseBody)
                    val latestVersion = jsonObject.getString("tag_name")
                    val assets = jsonObject.getJSONArray("assets")
                    
                    var downloadUrl = ""
                    for (i in 0 until assets.length()) {
                        val asset = assets.getJSONObject(i)
                        if (asset.getString("name").endsWith(".apk")) {
                            downloadUrl = asset.getString("browser_download_url")
                            break
                        }
                    }

                    val currentVersion = getCurrentVersion()
                    if (isNewerVersion(latestVersion, currentVersion)) {
                        mainHandler.post { callback.onUpdateAvailable(latestVersion, downloadUrl) }
                    } else {
                        mainHandler.post { callback.onNoUpdate() }
                    }
                } else {
                    mainHandler.post { callback.onError("GitHub returned ${response.code}", GITHUB_API_URL) }
                }
            } catch (e: Exception) {
                mainHandler.post { callback.onError("Network error: ${e.message}", GITHUB_API_URL) }
            }
        }.start()
    }

    fun downloadAndInstallApk(url: String, fileName: String, callback: UpdateCallback) {
        // Use internal cache directory for faster access and better security
        val destination = File(context.cacheDir, fileName)
        if (destination.exists()) destination.delete()

        Thread {
            try {
                val request = Request.Builder().url(url).build()
                val response = client.newCall(request).execute()
                
                if (!response.isSuccessful) {
                    mainHandler.post { callback.onError("Download failed: ${response.code}", url) }
                    return@Thread
                }

                val body = response.body
                val contentLength = body?.contentLength() ?: -1
                val inputStream: InputStream? = body?.byteStream()
                val outputStream = FileOutputStream(destination)

                val buffer = ByteArray(8192)
                var bytesRead: Int
                var totalBytesRead: Long = 0

                inputStream?.use { input ->
                    outputStream.use { output ->
                        while (input.read(buffer).also { bytesRead = it } != -1) {
                            output.write(buffer, 0, bytesRead)
                            totalBytesRead += bytesRead
                            if (contentLength > 0) {
                                val progress = ((totalBytesRead * 100) / contentLength).toInt()
                                mainHandler.post { callback.onDownloadProgress(progress) }
                            }
                        }
                    }
                }

                mainHandler.post { installApk(destination) }
            } catch (e: Exception) {
                mainHandler.post { callback.onError("Download error: ${e.message}", url) }
            }
        }.start()
    }

    private fun installApk(file: File) {
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, "application/vnd.android.package-archive")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            // Hint for the installer to be faster if possible
            putExtra(Intent.EXTRA_NOT_UNKNOWN_SOURCE, true)
        }
        context.startActivity(intent)
    }

    private fun getCurrentVersion(): String {
        return try {
            val pInfo = context.packageManager.getPackageInfo(context.packageName, 0)
            pInfo.versionName ?: "1.0.0"
        } catch (e: Exception) {
            "1.0.0"
        }
    }

    private fun isNewerVersion(latest: String, current: String): Boolean {
        val latestClean = latest.replace(Regex("[^0-9.]"), "")
        val currentClean = current.replace(Regex("[^0-9.]"), "")
        
        val latestParts = latestClean.split(".").filter { it.isNotEmpty() }.map { it.toIntOrNull() ?: 0 }
        val currentParts = currentClean.split(".").filter { it.isNotEmpty() }.map { it.toIntOrNull() ?: 0 }
        
        val length = maxOf(latestParts.size, currentParts.size)
        for (i in 0 until length) {
            val l = latestParts.getOrElse(i) { 0 }
            val c = currentParts.getOrElse(i) { 0 }
            if (l > c) return true
            if (l < c) return false
        }
        return false
    }
}
