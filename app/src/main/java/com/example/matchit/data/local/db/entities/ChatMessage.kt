package com.example.matchit.data.local.db.entities

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.example.matchit.data.chat.DataSources.ChatSystemEventJsonAdapter
import com.example.matchit.data.model.chat.ChatMessageDTO
import com.example.matchit.data.model.chat.ChatSystemEvent
import com.google.gson.Gson
import com.google.gson.GsonBuilder

@Entity(tableName = "chat_messages",
    indices = [
    Index(
        value = ["message_uuid", "session_uuid"],
        unique = true
    )
])

data class ChatMessageEntity(
    @PrimaryKey(autoGenerate = true)
    val localId: Long = 0L,

    @ColumnInfo(name = "message_uuid")
    val messageUuid: String,

    @ColumnInfo(name = "session_uuid")
    val sessionUuid: String,

    @ColumnInfo(name = "user_uuid")
    val userUuid: String,

    @ColumnInfo(name = "payload_json")
    val payloadJson: String,

    @ColumnInfo(name = "created_at")
    val createdAt: Long = System.currentTimeMillis(),

    @ColumnInfo(name = "status")
    val status: MessageStatus? = null, // is null for incoming messages

    @ColumnInfo(name = "is_outgoing")
    val isOutgoing: Boolean // check if is for
){

    fun toDTO(): ChatMessageDTO {
        val gson = GsonBuilder()
            .registerTypeAdapter(
                ChatSystemEvent::class.java,
                ChatSystemEventJsonAdapter()
            ).create()
        val dto = gson.fromJson(payloadJson, ChatMessageDTO::class.java)
        return dto.copy(
            status = this.status ?: MessageStatus.SENT
        )
    }
}



enum class MessageStatus {
    PENDING,
    SENT,
    DELIVERED
}