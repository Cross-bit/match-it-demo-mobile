package com.example.matchit.data.model.authentication

data class UserRegistrationData(
    val name: String,
    val email: String,
    val password: String,
    val token: String
)
