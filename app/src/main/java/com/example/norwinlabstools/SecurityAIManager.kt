package com.example.norwinlabstools

import com.google.ai.client.generativeai.GenerativeModel
import com.google.ai.client.generativeai.type.content
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class SecurityAIManager(private val apiKey: String) {

    private val model = GenerativeModel(
        modelName = "gemini-1.5-flash",
        apiKey = apiKey
    )

    interface SecurityCallback {
        fun onSuccess(analysis: String)
        fun onError(error: String)
    }

    suspend fun analyzeVulnerabilities(ip: String, openPorts: List<String>, callback: SecurityCallback) {
        if (apiKey.isEmpty() || apiKey == "YOUR_GEMINI_API_KEY_HERE") {
            callback.onError("API Key is missing. Please set it in Settings.")
            return
        }

        if (openPorts.isEmpty()) {
            callback.onSuccess("No common vulnerabilities detected on this device.")
            return
        }

        val prompt = """
            Analyze the following network scan results for a device at $ip.
            Open Ports/Services: ${openPorts.joinToString(", ")}
            
            Provide a concise (max 50 words) security assessment. 
            Identify the biggest risk and suggest one immediate action to secure the device.
        """.trimIndent()

        try {
            val response = withContext(Dispatchers.IO) {
                model.generateContent(content { text(prompt) })
            }
            val analysisText = response.text ?: "No analysis available."
            callback.onSuccess(analysisText)
        } catch (e: Exception) {
            callback.onError(e.message ?: "AI Analysis failed")
        }
    }
}