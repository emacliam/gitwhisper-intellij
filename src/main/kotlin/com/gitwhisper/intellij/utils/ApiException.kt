package com.gitwhisper.intellij.utils

/**
 * Exception for API-related errors
 * Based on the ApiException from the reference implementation
 */
class ApiException(
    message: String,
    val statusCode: Int? = null,
    val modelName: String? = null,
    val errorType: ErrorType = ErrorType.UNKNOWN,
    cause: Throwable? = null
) : Exception(message, cause) {

    enum class ErrorType {
        AUTHENTICATION,
        RATE_LIMIT,
        QUOTA_EXCEEDED,
        INVALID_REQUEST,
        SERVER_ERROR,
        NETWORK_ERROR,
        TIMEOUT,
        UNKNOWN
    }

    /**
     * Check if this is a retryable error
     */
    fun isRetryable(): Boolean {
        return when (errorType) {
            ErrorType.RATE_LIMIT,
            ErrorType.SERVER_ERROR,
            ErrorType.NETWORK_ERROR,
            ErrorType.TIMEOUT -> true
            else -> false
        }
    }

    /**
     * Get user-friendly error message
     */
    fun getUserMessage(): String {
        return when (errorType) {
            ErrorType.AUTHENTICATION -> "Invalid API key for $modelName. Please check your API key."
            ErrorType.RATE_LIMIT -> "Rate limit exceeded for $modelName. Please try again later."
            ErrorType.QUOTA_EXCEEDED -> "API quota exceeded for $modelName. Please check your billing."
            ErrorType.INVALID_REQUEST -> "Invalid request to $modelName API: $message"
            ErrorType.SERVER_ERROR -> "$modelName API is experiencing issues. Please try again later."
            ErrorType.NETWORK_ERROR -> "Network error connecting to $modelName API. Please check your connection."
            ErrorType.TIMEOUT -> "Request to $modelName API timed out. Please try again."
            ErrorType.UNKNOWN -> "Unknown error with $modelName API: $message"
        }
    }
}
