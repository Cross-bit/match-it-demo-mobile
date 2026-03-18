package com.example.matchit.ui.matchingSession.matching.uiModels

data class UserOnlineStatusChanged(
    val userUUID: String,
    val username: String,
    val isOnline: Boolean,
)