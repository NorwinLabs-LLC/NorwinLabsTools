package com.example.norwinlabstools

import android.content.Context
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject

class UpdateManager(private val context: Context) {

    private val client = OkHttpClient()
    
    // Check if these match your GitHub URL exactly: https://github.com/OWNER/REPO
    private val GITHUB_OWNER = "NorwinLabs"

    private val GITHUB_ORG = ""
    private val GITHUB_REPO = "NorwinLabsTools"
    
    private val GITHUB_API_URL = "https://api.github.com/repos/$GITHUB_ORG/$GITHUB_REPO/releases/latest"

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
                val responseCode = response.code
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
                    callback.onError("GitHub returned $responseCode", GITHUB_API_URL)
                }
            } catch (e: Exception) {
                callback.onError("Network error: ${e.message}", GITHUB_API_URL)
            }
        }.start()
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