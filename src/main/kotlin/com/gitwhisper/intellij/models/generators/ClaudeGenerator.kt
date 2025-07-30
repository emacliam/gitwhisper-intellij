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
 * Claude (Anthropic) commit message generator
 * Based on the ClaudeGenerator from the reference implementation
 */
class ClaudeGenerator(apiKey: String?, variant: String?) : CommitGenerator(apiKey, variant) {

    private val baseUrl = "https://api.anthropic.com/v1"
    private val client = OkHttpClient.Builder()
        .connectTimeout(getTimeoutConfig(), java.util.concurrent.TimeUnit.MILLISECONDS)
        .readTimeout(getTimeoutConfig(), java.util.concurrent.TimeUnit.MILLISECONDS)
        .writeTimeout(getTimeoutConfig(), java.util.concurrent.TimeUnit.MILLISECONDS)
        .build()
    private val gson = Gson()

    override val modelName: String = "claude"
    override val defaultVariant: String = ModelVariants.getDefaultVariant("claude")

    override fun generateCommitMessage(
        diff: String,
        language: Language,
        prefix: String?
    ): String {
        validateConfiguration()
        
        val prompt = CommitUtils.getCommitPrompt(diff, language, prefix)
        
        val requestBody = JsonObject().apply {
            addProperty("model", actualVariant)
            addProperty("max_tokens", maxTokens)
            addProperty("temperature", 0.1)
            add("messages", gson.toJsonTree(listOf(
                mapOf("role" to "user", "content" to prompt)
            )))
        }

        val request = Request.Builder()
            .url("$baseUrl/messages")
            .post(requestBody.toString().toRequestBody("application/json".toMediaType()))
            .apply {
                getCommonHeaders().forEach { (key, value) ->
                    header(key, value)
                }
                // Clean API key to avoid special characters
                val cleanApiKey = apiKey?.trim()?.replace(Regex("[^\\x20-\\x7E]"), "")
                header("x-api-key", cleanApiKey!!)
                header("anthropic-version", "2023-06-01")
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
            val content = jsonResponse.getAsJsonArray("content")
            
            if (content == null || content.size() == 0) {
                throw IOException("No content in response")
            }
            
            val message = content[0].asJsonObject
                .get("text")?.asString
                ?: throw IOException("No text in response")
            
            val cleanedMessage = cleanResponse(message)
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
            addProperty("model", actualVariant)
            addProperty("max_tokens", maxAnalysisTokens)
            addProperty("temperature", 0.2)
            add("messages", gson.toJsonTree(listOf(
                mapOf("role" to "user", "content" to prompt)
            )))
        }

        val request = Request.Builder()
            .url("$baseUrl/messages")
            .post(requestBody.toString().toRequestBody("application/json".toMediaType()))
            .apply {
                getCommonHeaders().forEach { (key, value) ->
                    header(key, value)
                }
                // Clean API key to avoid special characters
                val cleanApiKey = apiKey?.trim()?.replace(Regex("[^\\x20-\\x7E]"), "")
                header("x-api-key", cleanApiKey!!)
                header("anthropic-version", "2023-06-01")
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
            val content = jsonResponse.getAsJsonArray("content")
            
            if (content == null || content.size() == 0) {
                throw IOException("No content in response")
            }
            
            val message = content[0].asJsonObject
                .get("text")?.asString
                ?: throw IOException("No text in response")
            
            return message.trim()
            
        } catch (e: Exception) {
            handleApiError(e, "analyzing changes")
        }
    }
}
