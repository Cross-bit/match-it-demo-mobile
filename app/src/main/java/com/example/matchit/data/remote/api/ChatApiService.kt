package com.example.matchit.data.remote.api

import com.example.matchit.data.local.db.entities.MessageStatus
import com.example.matchit.data.model.chat.ChatAttachment
import com.example.matchit.data.model.chat.ChatCardPreview
import com.example.matchit.data.model.chat.ChatMessageDTO
import com.example.matchit.data.model.chat.ChatSystemEvent
import com.example.matchit.data.model.chat.MatchResultSystemEvent
import retrofit2.http.Query
import com.example.matchit.data.remote.model.Chat.AllChatMessagesApiResponse
import com.google.gson.Gson
import com.google.gson.JsonObject
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Path

/*data class ChatMessageApiDTO(
    val messageUuid: String,
    val sessionUuid: String,
    val userUuid: String,
    val createdAt: Long,
    val text: String? = null,
    val attachment: ChatAttachment? = null,
    val cardPreview: ChatCardPreview? = null,
    val systemEvent: JsonObject? = null,
    val status:  MessageStatus? = null
)

fun ChatMessageApiDTO.toDomain(): ChatMessageDTO {
    val parsedSystemEvent = systemEvent?.let { eventJson ->
        when (eventJson.get("type")?.asString) {
            "MATCH_RESULT" ->
                Gson().fromJson(eventJson, MatchResultSystemEvent::class.java)
            else -> null
        }
    }

    return ChatMessageDTO(
        messageUuid = messageUuid,
        sessionUuid = sessionUuid,
        userUuid = userUuid,
        createdAt = createdAt,
        text = text,
        attachment = attachment,
        cardPreview = cardPreview,
        systemEvent = parsedSystemEvent,
        status = MessageStatus.SENT
    )
}*/


interface ChatApiService {
    @GET("chat/load/{sessionUUID}")
    suspend fun getAllChatMessages(
        @Path("sessionUUID") sessionUUID: String,
        @Query("after") afterTimestamp: Long? = null
    ): Response<AllChatMessagesApiResponse>
}