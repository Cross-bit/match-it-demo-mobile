package com.example.matchit.data.remote.model

/**
 * Represents standardized API error response across all backend services.
 */
data class ApiError(
    val statusCode: Int?,
    val endpoint: String?,
    val message: String = "Unknown error."
)