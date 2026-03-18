package com.example.matchit.data.model.chat

data class ChatPayload(
    val type: String,
    val text: String? = null,
    val attachment: Map<String, Any>? = null
)
