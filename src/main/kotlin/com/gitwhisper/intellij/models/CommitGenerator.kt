package com.gitwhisper.intellij.models

import com.gitwhisper.intellij.utils.ErrorHandler
import com.gitwhisper.intellij.utils.ApiException

/**
 * Abstract base class for AI commit message generators
 * Based on the CommitGenerator from the reference implementation
 */
abstract class CommitGenerator(
    protected val apiKey: String?,
    protected val variant: String?
) {

    /**
     * Generate a commit message based on the git diff
     */
    abstract fun generateCommitMessage(
        diff: String,
        language: Language,
        prefix: String? = null
    ): String

    /**
     * Generate an analysis of the provided diff
     */
    abstract fun analyzeChanges(diff: String, language: Language): String

    /**
     * Returns the name of the model
     */
    abstract val modelName: String

    /**
     * Returns the default variant to use if none specified
     */
    abstract val defaultVariant: String

    /**
     * Gets the actual variant to use (specified or default)
     */
    val actualVariant: String
        get() = if (!variant.isNullOrBlank()) variant else defaultVariant

    /**
     * The maximum number of tokens allowed for the commit message generation
     */
    open val maxTokens: Int = 300

    /**
     * The maximum number of tokens allowed for the analysis message generation
     */
    open val maxAnalysisTokens: Int = 8000

    /**
     * Validate that the generator has the necessary configuration
     */
    protected open fun validateConfiguration() {
        if (apiKey.isNullOrBlank() && modelName != "ollama") {
            throw IllegalStateException("No API key provided for $modelName")
        }
    }

    /**
     * Handle API errors with appropriate error messages
     */
    protected fun handleApiError(error: Throwable, context: String): Nothing {
        println("$modelName API error in $context: ${error.message}")
        
        // Use the error handler to parse and throw appropriate exception
        val apiException = ErrorHandler.parseHttpError(error, modelName)
        throw apiException
    }

    /**
     * Clean up the generated response by removing markdown code blocks
     */
    protected fun cleanResponse(response: String): String {
        // Remove markdown code blocks
        val codeBlockPattern = Regex("^```(\\w+)?\\n?|```$", RegexOption.MULTILINE)
        return response.replace(codeBlockPattern, "").trim()
    }

    /**
     * Validate the generated commit message
     */
    protected fun validateCommitMessage(message: String) {
        if (message.isBlank()) {
            throw IllegalStateException("Generated commit message is empty")
        }

        val firstLine = message.trim().split("\n")[0]

        // Check for length constraint
        if (firstLine.length > 72) {
            println("Generated commit message is too long (${firstLine.length} chars): $firstLine")
            // Don't throw error, let the validation in extension handle it
        }

        // Check for basic conventional commit format
        val conventionalCommitPattern = Regex(
            "^(feat|fix|docs|style|refactor|test|chore|perf|ci|build|revert):\\s*[✨🐛📚💄♻️🧪🔧⚡👷📦⏪]\\s*.+"
        )
        if (!conventionalCommitPattern.matches(message.trim())) {
            println("Generated commit message may not follow conventional commit format: $message")
        }
    }

    /**
     * Get common headers for API requests
     */
    protected fun getCommonHeaders(): Map<String, String> {
        return mapOf(
            "Content-Type" to "application/json",
            "User-Agent" to "GitWhisper-IntelliJ/1.0.0"
        )
    }

    /**
     * Get timeout configuration for API requests (in milliseconds)
     */
    protected fun getTimeoutConfig(): Long {
        return 30000L // 30 seconds
    }
}
