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
 * Ollama local AI commit message generator
 * Based on the Ollama API implementation
 */
class OllamaGenerator(
    apiKey: String?, 
    variant: String?,
    private val baseUrl: String = "http://localhost:11434"
) : CommitGenerator(apiKey, variant) {

    private val client = OkHttpClient.Builder()
        .connectTimeout(getTimeoutConfig(), java.util.concurrent.TimeUnit.MILLISECONDS)
        .readTimeout(getTimeoutConfig(), java.util.concurrent.TimeUnit.MILLISECONDS)
        .writeTimeout(getTimeoutConfig(), java.util.concurrent.TimeUnit.MILLISECONDS)
        .build()
    private val gson = Gson()

    override val modelName: String = "ollama"
    override val defaultVariant: String = ModelVariants.getDefaultVariant("ollama")

    override fun validateConfiguration() {
        // Ollama doesn't require API key, but we should check if the service is available
        if (actualVariant.isBlank()) {
            throw IllegalStateException("No model variant specified for Ollama")
        }
    }

    override fun generateCommitMessage(
        diff: String,
        language: Language,
        prefix: String?
    ): String {
        validateConfiguration()
        
        val prompt = CommitUtils.getCommitPrompt(diff, language, prefix)
        
        val requestBody = JsonObject().apply {
            addProperty("model", actualVariant)
            addProperty("prompt", prompt)
            addProperty("stream", false)
            add("options", gson.toJsonTree(mapOf(
                "temperature" to 0.1,
                "top_p" to 0.9,
                "num_predict" to maxTokens
            )))
        }

        val request = Request.Builder()
            .url("$baseUrl/api/generate")
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
            
            // Check for errors
            if (jsonResponse.has("error")) {
                val error = jsonResponse.get("error").asString
                throw IOException("Ollama error: $error")
            }
            
            val responseText = jsonResponse.get("response")?.asString
                ?: throw IOException("No response text from Ollama")
            
            val cleanedMessage = cleanResponse(responseText)
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
            addProperty("prompt", prompt)
            addProperty("stream", false)
            add("options", gson.toJsonTree(mapOf(
                "temperature" to 0.2,
                "top_p" to 0.9,
                "num_predict" to maxAnalysisTokens
            )))
        }

        val request = Request.Builder()
            .url("$baseUrl/api/generate")
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
            
            // Check for errors
            if (jsonResponse.has("error")) {
                val error = jsonResponse.get("error").asString
                throw IOException("Ollama error: $error")
            }
            
            val responseText = jsonResponse.get("response")?.asString
                ?: throw IOException("No response text from Ollama")
            
            return responseText.trim()
            
        } catch (e: Exception) {
            handleApiError(e, "analyzing changes")
        }
    }

    /**
     * Check if Ollama service is available
     */
    fun isServiceAvailable(): Boolean {
        return try {
            val request = Request.Builder()
                .url("$baseUrl/api/tags")
                .get()
                .build()

            val response = client.newCall(request).execute()
            response.isSuccessful
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Get available models from Ollama
     */
    fun getAvailableModels(): List<String> {
        return try {
            val request = Request.Builder()
                .url("$baseUrl/api/tags")
                .get()
                .build()

            val response = client.newCall(request).execute()

            if (!response.isSuccessful) {
                return emptyList()
            }

            val responseBody = response.body?.string() ?: return emptyList()
            val jsonResponse = gson.fromJson(responseBody, JsonObject::class.java)
            val models = jsonResponse.getAsJsonArray("models")

            models?.map {
                it.asJsonObject.get("name")?.asString ?: ""
            }?.filter { it.isNotBlank() } ?: emptyList()

        } catch (e: Exception) {
            emptyList()
        }
    }
}
