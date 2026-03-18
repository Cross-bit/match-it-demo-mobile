package com.example.matchit.data.websockets

data class WsOnlineStatusChangedDTO(
    val userUUID: String,
    val isOnline: Boolean
)