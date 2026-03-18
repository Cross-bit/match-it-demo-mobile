package com.example.matchit.data.chat.DataSources

import com.example.matchit.data.local.db.dao.ChatMessagesDao
import com.example.matchit.data.local.db.entities.ChatMessageEntity
import com.example.matchit.data.local.db.entities.MessageStatus
import com.example.matchit.data.model.chat.ChatMessageDTO
import com.example.matchit.data.model.chat.ChatSystemEvent
import com.example.matchit.data.model.chat.MatchResultSystemEvent
import com.example.matchit.data.model.session.SessionParameters
import com.example.matchit.data.userAuthentication.DataSources.UserDataLocalDataSource
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.JsonDeserializationContext
import com.google.gson.JsonDeserializer
import com.google.gson.JsonElement
import com.google.gson.JsonSerializationContext
import com.google.gson.JsonSerializer
import kotlinx.coroutines.flow.first
import java.lang.reflect.Type
import java.util.UUID
import javax.inject.Inject


class ChatSystemEventJsonAdapter : JsonSerializer<ChatSystemEvent>,
    JsonDeserializer<ChatSystemEvent> {

    override fun serialize(
        src: ChatSystemEvent?,
        typeOfSrc: Type?,
        context: JsonSerializationContext
    ): JsonElement {
        return context.serialize(src)
    }

    override fun deserialize(
        json: JsonElement,
        typeOfT: Type?,
        context: JsonDeserializationContext
    ): ChatSystemEvent? {

        if (!json.isJsonObject) return null
        val obj = json.asJsonObject

        val type = obj["type"]?.asString ?: return null

        return when (type) {
            "MATCH_RESULT" ->
                context.deserialize(obj, MatchResultSystemEvent::class.java)
            else -> null
        }
    }
}

class ChatLocalDataSource @Inject constructor(
    private var chatMessageDao: ChatMessagesDao
)
{
    /**
     * Stores a single chat message into the local database.
     */
    suspend fun storeChatMessage(currentUserUuid: String, msgDto: ChatMessageDTO, msgStatus: MessageStatus? = null) {
        val entity = dtoToEntity(currentUserUuid, msgDto, msgStatus)
        chatMessageDao.insertMessage(entity)
    }

    /**
     *  Stores multiple chat messages into the local database in bulk.
     */
    suspend fun storeChatMessages(
        currentUserUuid: String,
        dtos: List<ChatMessageDTO>,
        defaultStatusForOutgoing: MessageStatus? = null
    ) {
        val entities = dtos.map { dto ->
            dtoToEntity(currentUserUuid, dto, defaultStatusForOutgoing)
        }
        chatMessageDao.insertMessages(entities)
    }

    /**
     * Load all the messages for given [sessionUUID]
     */
    suspend fun loadAllChatMessages (
        sessionUUID: String
    ) :  List<ChatMessageEntity>  {
        return chatMessageDao.getMessagesForSession(sessionUUID).first()
    }

    /**
     *  Update chat message status (e.g when the message was successfully send/delivered ...)
     */
    suspend fun updateChatMessagesStatus(
        currentUserUuid: String,
        messageUuid: String,
        status: MessageStatus = MessageStatus.DELIVERED
    ) {
        chatMessageDao.updateMessageStatus(messageUuid, status)
    }


    private fun dtoToEntity(
        currentUserUuid: String,
        dto: ChatMessageDTO,
        outgoingStatus: MessageStatus? = null
    ): ChatMessageEntity {

        val gson = GsonBuilder()
            .registerTypeAdapter(
                ChatSystemEvent::class.java,
                ChatSystemEventJsonAdapter()
            ).create()

        val payloadJson = gson.toJson(dto)

        // make sure we only make correct messages outgoing for current user
        val isOutgoing = dto.userUuid == currentUserUuid

        val status = if (isOutgoing) outgoingStatus else null

        return ChatMessageEntity(
            messageUuid = dto.messageUuid,
            sessionUuid = dto.sessionUuid,
            userUuid = dto.userUuid,
            payloadJson = payloadJson,
            createdAt = dto.createdAt,
            status = status,
            isOutgoing = isOutgoing
        )
    }
}