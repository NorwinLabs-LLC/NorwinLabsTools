package com.example.norwinlabstools

import android.app.DownloadManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.Uri
import android.os.Build
import android.os.Environment
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.io.File

class UpdateManager(private val context: Context) {

    private val client = OkHttpClient()
    
    private val GITHUB_OWNER = "NorwinLabs"
    private val GITHUB_REPO = "NorwinLabsTools"
    private val GITHUB_API_URL = "https://api.github.com/repos/$GITHUB_OWNER/$GITHUB_REPO/releases/latest"

    interface UpdateCallback {
        fun onUpdateAvailable(latestVersion: String, downloadUrl: String)
        fun onNoUpdate()
        fun onError(error: String, url: String)
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
                        callback.onUpdateAvailable(latestVersion, downloadUrl)
                    } else {
                        callback.onNoUpdate()
                    }
                } else {
                    callback.onError("GitHub returned ${response.code}", GITHUB_API_URL)
                }
            } catch (e: Exception) {
                callback.onError("Network error: ${e.message}", GITHUB_API_URL)
            }
        }.start()
    }

    fun downloadAndInstallApk(url: String, fileName: String) {
        val destination = File(context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS), fileName)
        if (destination.exists()) destination.delete()

        val request = DownloadManager.Request(Uri.parse(url))
            .setTitle("Downloading NorwinLabsTools Update")
            .setDescription("Preparing to install version...")
            .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
            .setDestinationUri(Uri.fromFile(destination))
            .setAllowedOverMetered(true)
            .setAllowedOverRoaming(true)

        val downloadManager = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
        val downloadId = downloadManager.enqueue(request)

        val onComplete = object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                val id = intent.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1)
                if (id == downloadId) {
                    installApk(destination)
                    context.unregisterReceiver(this)
                }
            }
        }
        
        val filter = IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE)
        // Use ContextCompat to handle RECEIVER_EXPORTED requirement on API 34+
        ContextCompat.registerReceiver(context, onComplete, filter, ContextCompat.RECEIVER_EXPORTED)
    }

    private fun installApk(file: File) {
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, "application/vnd.android.package-archive")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
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