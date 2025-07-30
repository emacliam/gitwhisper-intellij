package com.gitwhisper.intellij.utils

import java.io.IOException
import java.net.SocketTimeoutException
import java.net.UnknownHostException

/**
 * Error handler for API and network errors
 * Based on the ErrorHandler from the reference implementation
 */
object ErrorHandler {

    /**
     * Parse HTTP error and convert to ApiException
     */
    fun parseHttpError(error: Throwable, modelName: String): ApiException {
        return when (error) {
            is SocketTimeoutException -> ApiException(
                "Request timed out",
                modelName = modelName,
                errorType = ApiException.ErrorType.TIMEOUT,
                cause = error
            )
            is UnknownHostException -> ApiException(
                "Cannot connect to API server",
                modelName = modelName,
                errorType = ApiException.ErrorType.NETWORK_ERROR,
                cause = error
            )
            is IOException -> ApiException(
                "Network error: ${error.message}",
                modelName = modelName,
                errorType = ApiException.ErrorType.NETWORK_ERROR,
                cause = error
            )
            is ApiException -> error
            else -> {
                // Try to parse HTTP status codes from error message
                val statusCode = extractStatusCode(error.message)
                val errorType = when (statusCode) {
                    401, 403 -> ApiException.ErrorType.AUTHENTICATION
                    429 -> ApiException.ErrorType.RATE_LIMIT
                    402, 413 -> ApiException.ErrorType.QUOTA_EXCEEDED
                    400, 422 -> ApiException.ErrorType.INVALID_REQUEST
                    in 500..599 -> ApiException.ErrorType.SERVER_ERROR
                    else -> ApiException.ErrorType.UNKNOWN
                }
                
                ApiException(
                    error.message ?: "Unknown error",
                    statusCode = statusCode,
                    modelName = modelName,
                    errorType = errorType,
                    cause = error
                )
            }
        }
    }

    /**
     * Extract HTTP status code from error message
     */
    private fun extractStatusCode(message: String?): Int? {
        if (message == null) return null
        
        val statusCodeRegex = Regex("\\b(\\d{3})\\b")
        val match = statusCodeRegex.find(message)
        return match?.groupValues?.get(1)?.toIntOrNull()
    }

    /**
     * Check if error suggests switching models
     */
    fun shouldSuggestModelSwitch(error: ApiException): Boolean {
        return when (error.errorType) {
            ApiException.ErrorType.AUTHENTICATION,
            ApiException.ErrorType.QUOTA_EXCEEDED,
            ApiException.ErrorType.RATE_LIMIT -> true
            else -> false
        }
    }

    /**
     * Get model switch suggestions
     */
    fun getModelSwitchSuggestions(error: ApiException): List<String> {
        val currentModel = error.modelName ?: "current model"
        return when (error.errorType) {
            ApiException.ErrorType.AUTHENTICATION -> listOf(
                "Try using a different AI model",
                "Check if you have API keys for other models",
                "Consider using Ollama for local AI"
            )
            ApiException.ErrorType.QUOTA_EXCEEDED -> listOf(
                "Switch to a different AI model",
                "Try using Ollama for unlimited local usage",
                "Consider upgrading your API plan"
            )
            ApiException.ErrorType.RATE_LIMIT -> listOf(
                "Switch to a different AI model temporarily",
                "Try using Ollama for local processing",
                "Wait and retry with $currentModel later"
            )
            else -> listOf(
                "Try using a different AI model",
                "Consider using Ollama for local AI"
            )
        }
    }

    /**
     * Handle general errors (non-API)
     */
    fun handleGeneralError(error: Throwable, context: String): String {
        val message = when (error) {
            is IllegalStateException -> error.message ?: "Invalid state"
            is IllegalArgumentException -> error.message ?: "Invalid argument"
            is SecurityException -> "Permission denied"
            else -> error.message ?: "Unknown error"
        }
        
        println("Error in $context: $message")
        return "Error in $context: $message"
    }

    /**
     * Check if error is retryable
     */
    fun isRetryable(error: Throwable): Boolean {
        return when (error) {
            is ApiException -> error.isRetryable()
            is SocketTimeoutException,
            is IOException -> true
            else -> false
        }
    }

    /**
     * Get retry delay in milliseconds
     */
    fun getRetryDelay(attempt: Int): Long {
        // Exponential backoff: 1s, 2s, 4s, 8s, 16s
        return (1000L * (1 shl (attempt - 1))).coerceAtMost(16000L)
    }
}
