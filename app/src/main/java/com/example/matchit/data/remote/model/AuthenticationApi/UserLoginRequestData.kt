package com.example.matchit.data.remote.model.AuthenticationApi

data class UserLoginRequestData(
    val email: String,
    val password: String,
    val fcmToken: String,
)


