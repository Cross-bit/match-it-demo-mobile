package com.example.matchit.data.remote.model.Chat

import com.example.matchit.data.model.chat.ChatMessageDTO

data class AllChatMessagesApiResponse (
    val sessionUUID: String,
    val messages: List<ChatMessageDTO>
)