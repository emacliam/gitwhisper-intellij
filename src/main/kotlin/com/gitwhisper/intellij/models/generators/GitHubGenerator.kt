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
 * GitHub Models commit message generator
 * Based on the GitHub Models API implementation
 */
class GitHubGenerator(apiKey: String?, variant: String?) : CommitGenerator(apiKey, variant) {

    private val baseUrl = "https://models.inference.ai.azure.com"
    private val client = OkHttpClient.Builder()
        .connectTimeout(getTimeoutConfig(), java.util.concurrent.TimeUnit.MILLISECONDS)
        .readTimeout(getTimeoutConfig(), java.util.concurrent.TimeUnit.MILLISECONDS)
        .writeTimeout(getTimeoutConfig(), java.util.concurrent.TimeUnit.MILLISECONDS)
        .build()
    private val gson = Gson()

    override val modelName: String = "github"
    override val defaultVariant: String = ModelVariants.getDefaultVariant("github")

    override fun generateCommitMessage(
        diff: String,
        language: Language,
        prefix: String?
    ): String {
        validateConfiguration()
        
        val prompt = CommitUtils.getCommitPrompt(diff, language, prefix)
        
        val requestBody = JsonObject().apply {
            add("messages", gson.toJsonTree(listOf(
                mapOf("role" to "user", "content" to prompt)
            )))
            addProperty("max_tokens", maxTokens)
            addProperty("temperature", 0.1)
            addProperty("top_p", 0.9)
        }

        val request = Request.Builder()
            .url("$baseUrl/chat/completions")
            .post(requestBody.toString().toRequestBody("application/json".toMediaType()))
            .apply {
                getCommonHeaders().forEach { (key, value) ->
                    header(key, value)
                }
                // Clean API key to avoid special characters
                val cleanApiKey = apiKey?.trim()?.replace(Regex("[^\\x20-\\x7E]"), "")
                header("Authorization", "Bearer $cleanApiKey")
                header("azureml-model-deployment", actualVariant)
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
            val choices = jsonResponse.getAsJsonArray("choices")
            
            if (choices == null || choices.size() == 0) {
                throw IOException("No choices in response")
            }
            
            val message = choices[0].asJsonObject
                .getAsJsonObject("message")
                .get("content")?.asString
                ?: throw IOException("No content in response")
            
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
            add("messages", gson.toJsonTree(listOf(
                mapOf("role" to "user", "content" to prompt)
            )))
            addProperty("max_tokens", maxAnalysisTokens)
            addProperty("temperature", 0.2)
            addProperty("top_p", 0.9)
        }

        val request = Request.Builder()
            .url("$baseUrl/chat/completions")
            .post(requestBody.toString().toRequestBody("application/json".toMediaType()))
            .apply {
                getCommonHeaders().forEach { (key, value) ->
                    header(key, value)
                }
                // Clean API key to avoid special characters
                val cleanApiKey = apiKey?.trim()?.replace(Regex("[^\\x20-\\x7E]"), "")
                header("Authorization", "Bearer $cleanApiKey")
                header("azureml-model-deployment", actualVariant)
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
            val choices = jsonResponse.getAsJsonArray("choices")
            
            if (choices == null || choices.size() == 0) {
                throw IOException("No choices in response")
            }
            
            val message = choices[0].asJsonObject
                .getAsJsonObject("message")
                .get("content")?.asString
                ?: throw IOException("No content in response")
            
            return message.trim()
            
        } catch (e: Exception) {
            handleApiError(e, "analyzing changes")
        }
    }
}
