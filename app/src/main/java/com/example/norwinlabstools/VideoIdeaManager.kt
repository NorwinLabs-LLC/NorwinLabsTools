package com.example.norwinlabstools

import com.google.ai.client.generativeai.GenerativeModel
import com.google.ai.client.generativeai.type.content
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class VideoIdeaManager(private val apiKey: String) {

    private val model = GenerativeModel(
        modelName = "gemini-1.5-flash",
        apiKey = apiKey
    )

    interface VideoIdeaCallback {
        fun onSuccess(idea: String)
        fun onError(error: String)
    }

    suspend fun generateIdea(isShort: Boolean, category: String = "General", callback: VideoIdeaCallback) {
        if (apiKey.isEmpty() || apiKey == "YOUR_GEMINI_API_KEY_HERE") {
            callback.onError("API Key is missing. Please set it in Settings.")
            return
        }

        val prompt = when (category) {
            "Windhelm" -> if (isShort) {
                "Generate a highly engaging YouTube Short idea for a PC game called 'Windhelm' built in Unreal Engine 5.7. Focus on a quick hook, a technical hack, or a visually stunning moment. Keep the response under 30 words."
            } else {
                "Generate a detailed YouTube long-form video idea (Devlog or Tutorial) for a PC game called 'Windhelm' built in Unreal Engine 5.7. Include a catchy title and a brief 1-sentence description of the content."
            }
            else -> if (isShort) {
                "Generate a highly engaging YouTube Short idea about Unreal Engine 5 or general game development. Focus on a quick tip, a hidden feature, or a technical trick. Keep the response under 30 words."
            } else {
                "Generate a detailed YouTube long-form video idea (Tutorial, Devlog, or Industry Analysis) about Unreal Engine 5 or general game development. Include a catchy title and a brief 1-sentence description of the content."
            }
        }

        try {
            val response = withContext(Dispatchers.IO) {
                model.generateContent(content { text(prompt) })
            }
            val ideaText = response.text ?: "Could not generate an idea at this time."
            callback.onSuccess(ideaText)
        } catch (e: Exception) {
            callback.onError(e.message ?: "Unknown AI error")
        }
    }
}