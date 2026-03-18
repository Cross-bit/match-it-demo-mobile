package com.example.matchit.data.remote.Errors

class UserIsLoggedOutError(message: String = "User is logged out", cause: Throwable? = null) : Error(message, cause)