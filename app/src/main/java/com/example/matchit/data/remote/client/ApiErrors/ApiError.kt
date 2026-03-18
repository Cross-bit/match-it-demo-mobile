package com.example.matchit.data.remote.client.ApiErrors

/**
 * Represents an standardized API error response for our services.
 * @param name The name or type of the error.
 * @param message The detailed error message describing the issue.
 * @param status The status code or identifier of the error (more specifies the error or is simply HTTP status code).
 * @param errors Additional error details, if any, associated with the error.
 *              This can be a specific object containing more detailed error information.
 *              It's nullable, allowing absence of additional error data.
 */
data class ApiError<T>(
    val name: String,
    val message: String,
    val status: Int,
    val errors: T?
)
