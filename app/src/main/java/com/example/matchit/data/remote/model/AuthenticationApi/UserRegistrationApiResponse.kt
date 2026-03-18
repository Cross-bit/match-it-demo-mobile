package com.example.matchit.data.remote.model.AuthenticationApi

data class UserRegistrationApiResponse(
    val status: RegistrationStatus
)

enum class RegistrationStatus {
    CREATED,
    VERIFICATION_MAIL_SEND,
    ALREADY_EXIST
}
