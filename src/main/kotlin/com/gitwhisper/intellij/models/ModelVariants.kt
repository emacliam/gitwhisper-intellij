package com.gitwhisper.intellij.models

/**
 * Model variant information
 */
data class ModelVariant(
    val name: String,
    val displayName: String,
    val description: String
)

/**
 * Model variants for different AI providers
 * Based on the ModelVariants from the reference implementation
 */
object ModelVariants {

    private val openaiVariants = listOf(
        ModelVariant("gpt-4o", "GPT-4o", "Latest GPT-4 Omni model"),
        ModelVariant("gpt-4o-mini", "GPT-4o Mini", "Faster, cost-effective GPT-4o"),
        ModelVariant("gpt-4-turbo", "GPT-4 Turbo", "High-performance GPT-4"),
        ModelVariant("gpt-4", "GPT-4", "Standard GPT-4 model"),
        ModelVariant("gpt-3.5-turbo", "GPT-3.5 Turbo", "Fast and efficient model")
    )

    private val claudeVariants = listOf(
        ModelVariant("claude-3-5-sonnet-20241022", "Claude 3.5 Sonnet", "Latest Claude 3.5 Sonnet"),
        ModelVariant("claude-3-5-haiku-20241022", "Claude 3.5 Haiku", "Fast Claude 3.5 Haiku"),
        ModelVariant("claude-3-opus-20240229", "Claude 3 Opus", "Most capable Claude 3 model"),
        ModelVariant("claude-3-sonnet-20240229", "Claude 3 Sonnet", "Balanced Claude 3 model"),
        ModelVariant("claude-3-haiku-20240307", "Claude 3 Haiku", "Fast Claude 3 model")
    )

    private val geminiVariants = listOf(
        ModelVariant("gemini-1.5-pro", "Gemini 1.5 Pro", "Advanced Gemini model"),
        ModelVariant("gemini-1.5-flash", "Gemini 1.5 Flash", "Fast Gemini model"),
        ModelVariant("gemini-pro", "Gemini Pro", "Standard Gemini model")
    )

    private val grokVariants = listOf(
        ModelVariant("grok-beta", "Grok Beta", "Latest Grok model"),
        ModelVariant("grok-vision-beta", "Grok Vision Beta", "Grok with vision capabilities")
    )

    private val llamaVariants = listOf(
        ModelVariant("llama-3.2-90b-text-preview", "Llama 3.2 90B", "Large Llama 3.2 model"),
        ModelVariant("llama-3.2-11b-text-preview", "Llama 3.2 11B", "Medium Llama 3.2 model"),
        ModelVariant("llama-3.1-70b-instruct", "Llama 3.1 70B", "Llama 3.1 70B Instruct"),
        ModelVariant("llama-3.1-8b-instruct", "Llama 3.1 8B", "Llama 3.1 8B Instruct")
    )

    private val deepseekVariants = listOf(
        ModelVariant("deepseek-chat", "DeepSeek Chat", "DeepSeek conversational model"),
        ModelVariant("deepseek-coder", "DeepSeek Coder", "DeepSeek coding model")
    )

    private val githubVariants = listOf(
        ModelVariant("gpt-4o", "GPT-4o", "GitHub's GPT-4o model"),
        ModelVariant("gpt-4o-mini", "GPT-4o Mini", "GitHub's GPT-4o Mini"),
        ModelVariant("claude-3-5-sonnet", "Claude 3.5 Sonnet", "GitHub's Claude 3.5 Sonnet"),
        ModelVariant("claude-3-haiku", "Claude 3 Haiku", "GitHub's Claude 3 Haiku")
    )

    private val ollamaVariants = listOf(
        ModelVariant("llama3.2:3b", "Llama 3.2 3B", "Llama 3.2 3B model"),
        ModelVariant("llama3.2:1b", "Llama 3.2 1B", "Llama 3.2 1B model"),
        ModelVariant("qwen2.5:7b", "Qwen 2.5 7B", "Qwen 2.5 7B model"),
        ModelVariant("qwen2.5:3b", "Qwen 2.5 3B", "Qwen 2.5 3B model"),
        ModelVariant("qwen2.5:1.5b", "Qwen 2.5 1.5B", "Qwen 2.5 1.5B model"),
        ModelVariant("deepseek-r1:1.5b", "DeepSeek R1 1.5B", "DeepSeek R1 1.5B model"),
        ModelVariant("deepseek-r1:7b", "DeepSeek R1 7B", "DeepSeek R1 7B model"),
        ModelVariant("deepseek-r1:8b", "DeepSeek R1 8B", "DeepSeek R1 8B model"),
        ModelVariant("deepseek-r1:14b", "DeepSeek R1 14B", "DeepSeek R1 14B model"),
        ModelVariant("deepseek-r1:32b", "DeepSeek R1 32B", "DeepSeek R1 32B model"),
        ModelVariant("deepseek-r1:70b", "DeepSeek R1 70B", "DeepSeek R1 70B model")
    )

    /**
     * Get variants for a specific model
     */
    fun getVariants(modelName: String): List<ModelVariant> {
        return when (modelName.lowercase()) {
            "openai" -> openaiVariants
            "claude" -> claudeVariants
            "gemini" -> geminiVariants
            "grok" -> grokVariants
            "llama" -> llamaVariants
            "deepseek" -> deepseekVariants
            "github" -> githubVariants
            "ollama" -> ollamaVariants
            else -> emptyList()
        }
    }

    /**
     * Get variants with custom Ollama variant included
     */
    fun getVariantsWithCustom(modelName: String, customOllamaVariant: String?): List<ModelVariant> {
        val variants = getVariants(modelName).toMutableList()
        
        if (modelName.lowercase() == "ollama" && !customOllamaVariant.isNullOrBlank()) {
            val customVariant = ModelVariant(
                customOllamaVariant.trim(),
                "Custom: ${customOllamaVariant.trim()}",
                "Custom Ollama model variant"
            )
            variants.add(0, customVariant) // Add at the beginning
        }
        
        return variants
    }

    /**
     * Get default variant for a model
     */
    fun getDefaultVariant(modelName: String): String {
        return when (modelName.lowercase()) {
            "openai" -> "gpt-4o"
            "claude" -> "claude-3-5-sonnet-20241022"
            "gemini" -> "gemini-1.5-pro"
            "grok" -> "grok-beta"
            "llama" -> "llama-3.2-11b-text-preview"
            "deepseek" -> "deepseek-chat"
            "github" -> "gpt-4o"
            "ollama" -> "llama3.2:3b"
            else -> ""
        }
    }

    /**
     * Check if a model is supported
     */
    fun isModelSupported(modelName: String): Boolean {
        return getVariants(modelName).isNotEmpty()
    }

    /**
     * Get all available models
     */
    fun getAvailableModels(): List<String> {
        return listOf("openai", "claude", "gemini", "grok", "llama", "deepseek", "github", "ollama")
    }

    /**
     * Get implemented models (models that have actual implementations)
     */
    fun getImplementedModels(): List<String> {
        return getAvailableModels() // All models are now implemented
    }
}
