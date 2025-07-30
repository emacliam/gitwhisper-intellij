package com.gitwhisper.intellij.models

import com.gitwhisper.intellij.models.generators.*

/**
 * Factory for creating commit generators based on model name
 * Based on the CommitGeneratorFactory from the reference implementation
 */
object CommitGeneratorFactory {

    data class ModelRequirements(
        val requiresApiKey: Boolean,
        val supportsCustomBaseUrl: Boolean = false
    )

    /**
     * Create a commit generator for the specified model
     */
    fun create(
        modelName: String,
        apiKey: String?,
        options: Map<String, Any> = emptyMap()
    ): CommitGenerator {
        if (!ModelVariants.isModelSupported(modelName)) {
            throw IllegalArgumentException("Unsupported model: $modelName")
        }

        // Check if API key is required for this model
        val requirements = getModelRequirements(modelName)
        if (requirements.requiresApiKey && apiKey.isNullOrBlank()) {
            throw IllegalArgumentException("API key is required for $modelName model")
        }

        val variant = options["variant"] as? String
        val baseUrl = options["baseUrl"] as? String

        return when (modelName.lowercase()) {
            "openai" -> OpenAIGenerator(apiKey, variant)
            "claude" -> ClaudeGenerator(apiKey, variant)
            "gemini" -> GeminiGenerator(apiKey, variant)
            "grok" -> GrokGenerator(apiKey, variant)
            "llama" -> LlamaGenerator(apiKey, variant)
            "deepseek" -> DeepSeekGenerator(apiKey, variant)
            "github" -> GitHubGenerator(apiKey, variant)
            "ollama" -> {
                val ollamaBaseUrl = baseUrl ?: "http://localhost:11434"
                OllamaGenerator(apiKey, variant, ollamaBaseUrl)
            }

            else -> throw IllegalArgumentException("Unknown model: $modelName")
        }
    }

    /**
     * Get model requirements
     */
    fun getModelRequirements(modelName: String): ModelRequirements {
        return when (modelName.lowercase()) {
            "openai" -> ModelRequirements(requiresApiKey = true)
            "claude" -> ModelRequirements(requiresApiKey = true)
            "gemini" -> ModelRequirements(requiresApiKey = true)
            "grok" -> ModelRequirements(requiresApiKey = true)
            "llama" -> ModelRequirements(requiresApiKey = true)
            "deepseek" -> ModelRequirements(requiresApiKey = true)
            "github" -> ModelRequirements(requiresApiKey = true)
            "ollama" -> ModelRequirements(requiresApiKey = false, supportsCustomBaseUrl = true)
            else -> ModelRequirements(requiresApiKey = true)
        }
    }

    /**
     * Get all available models
     */
    fun getAvailableModels(): List<String> {
        return ModelVariants.getAvailableModels()
    }

    /**
     * Get implemented models (models that have actual implementations)
     */
    fun getImplementedModels(): List<String> {
        return listOf("openai", "claude", "gemini", "grok", "llama", "deepseek", "github", "ollama")
    }

    /**
     * Check if a model is implemented
     */
    fun isModelImplemented(modelName: String): Boolean {
        return getImplementedModels().contains(modelName.lowercase())
    }

    /**
     * Get model display information
     */
    fun getModelInfo(modelName: String): Map<String, Any> {
        val requirements = getModelRequirements(modelName)
        val variants = ModelVariants.getVariants(modelName)
        val isImplemented = isModelImplemented(modelName)
        
        return mapOf(
            "name" to modelName,
            "displayName" to modelName.replaceFirstChar { it.uppercase() },
            "requiresApiKey" to requirements.requiresApiKey,
            "supportsCustomBaseUrl" to requirements.supportsCustomBaseUrl,
            "variants" to variants,
            "defaultVariant" to ModelVariants.getDefaultVariant(modelName),
            "isImplemented" to isImplemented,
            "status" to if (isImplemented) "Available" else "Coming Soon"
        )
    }

    /**
     * Get all models with their information
     */
    fun getAllModelsInfo(): List<Map<String, Any>> {
        return getAvailableModels().map { getModelInfo(it) }
    }
}
