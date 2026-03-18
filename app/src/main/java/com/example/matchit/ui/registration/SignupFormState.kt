package com.example.matchit.ui.registration

/**
 * Represents the state of the users credential registration form
 */
data class SignupFormState(
    val emailError: Int? = null,
    val usernameError: Int? = null,
    val passwordError: Int? = null,
    val isDataValid: Boolean = false
)
