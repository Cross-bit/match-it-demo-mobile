package com.example.matchit.data.model.authentication

/**
 * Data class that captures user information for logged in users retrieved from LoginRepository
 */

data class LoggedInUser(
    val displayName: String,
    val email: String,
    val uuid: String
)