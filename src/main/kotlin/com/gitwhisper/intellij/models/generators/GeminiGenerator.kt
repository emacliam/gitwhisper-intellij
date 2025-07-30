package com.gitwhisper.intellij.models.generators

import com.gitwhisper.intellij.models.CommitGenerator
import com.gitwhisper.intellij.models.Language
import com.gitwhisper.intellij.models.ModelVariants
import com.gitwhisper.intellij.utils.CommitUtils
import com.google.gson.Gson
import com.google.gson.JsonObject
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.IOException

/**
 * Google Gemini commit message generator
 * Based on the Gemini API implementation
 */
class GeminiGenerator(apiKey: String?, variant: String?) : CommitGenerator(apiKey, variant) {

    private val baseUrl = "https://generativelanguage.googleapis.com/v1beta"
    private val client = OkHttpClient.Builder()
        .connectTimeout(getTimeoutConfig(), java.util.concurrent.TimeUnit.MILLISECONDS)
        .readTimeout(getTimeoutConfig(), java.util.concurrent.TimeUnit.MILLISECONDS)
        .writeTimeout(getTimeoutConfig(), java.util.concurrent.TimeUnit.MILLISECONDS)
        .build()
    private val gson = Gson()

    override val modelName: String = "gemini"
    override val defaultVariant: String = ModelVariants.getDefaultVariant("gemini")

    override fun generateCommitMessage(
        diff: String,
        language: Language,
        prefix: String?
    ): String {
        validateConfiguration()
        
        val prompt = CommitUtils.getCommitPrompt(diff, language, prefix)
        
        val requestBody = JsonObject().apply {
            add("contents", gson.toJsonTree(listOf(
                mapOf(
                    "parts" to listOf(
                        mapOf("text" to prompt)
                    )
                )
            )))
            add("generationConfig", gson.toJsonTree(mapOf(
                "maxOutputTokens" to maxTokens,
                "temperature" to 0.1,
                "topP" to 0.9
            )))
        }

        val request = Request.Builder()
            .url("$baseUrl/models/$actualVariant:generateContent?key=$apiKey")
            .post(requestBody.toString().toRequestBody("application/json".toMediaType()))
            .apply {
                getCommonHeaders().forEach { (key, value) ->
                    header(key, value)
                }
            }
            .build()

        try {
            val response = client.newCall(request).execute()
            
            if (!response.isSuccessful) {
                throw IOException("HTTP ${response.code}: ${response.message}")
            }
            
            val responseBody = response.body?.string()
                ?: throw IOException("Empty response body")
            
            val jsonResponse = gson.fromJson(responseBody, JsonObject::class.java)
            val candidates = jsonResponse.getAsJsonArray("candidates")
            
            if (candidates == null || candidates.size() == 0) {
                throw IOException("No candidates in response")
            }
            
            val content = candidates[0].asJsonObject
                .getAsJsonObject("content")
                .getAsJsonArray("parts")[0].asJsonObject
                .get("text")?.asString
                ?: throw IOException("No text in response")
            
            val cleanedMessage = cleanResponse(content)
            validateCommitMessage(cleanedMessage)
            return cleanedMessage
            
        } catch (e: Exception) {
            handleApiError(e, "generating commit message")
        }
    }

    override fun analyzeChanges(diff: String, language: Language): String {
        validateConfiguration()
        
        val prompt = CommitUtils.getAnalysisPrompt(diff, language)
        
        val requestBody = JsonObject().apply {
            add("contents", gson.toJsonTree(listOf(
                mapOf(
                    "parts" to listOf(
                        mapOf("text" to prompt)
                    )
                )
            )))
            add("generationConfig", gson.toJsonTree(mapOf(
                "maxOutputTokens" to maxAnalysisTokens,
                "temperature" to 0.2,
                "topP" to 0.9
            )))
        }

        val request = Request.Builder()
            .url("$baseUrl/models/$actualVariant:generateContent?key=$apiKey")
            .post(requestBody.toString().toRequestBody("application/json".toMediaType()))
            .apply {
                getCommonHeaders().forEach { (key, value) ->
                    header(key, value)
                }
            }
            .build()

        try {
            val response = client.newCall(request).execute()
            
            if (!response.isSuccessful) {
                throw IOException("HTTP ${response.code}: ${response.message}")
            }
            
            val responseBody = response.body?.string()
                ?: throw IOException("Empty response body")
            
            val jsonResponse = gson.fromJson(responseBody, JsonObject::class.java)
            val candidates = jsonResponse.getAsJsonArray("candidates")
            
            if (candidates == null || candidates.size() == 0) {
                throw IOException("No candidates in response")
            }
            
            val content = candidates[0].asJsonObject
                .getAsJsonObject("content")
                .getAsJsonArray("parts")[0].asJsonObject
                .get("text")?.asString
                ?: throw IOException("No text in response")
            
            return content.trim()
            
        } catch (e: Exception) {
            handleApiError(e, "analyzing changes")
        }
    }
}
