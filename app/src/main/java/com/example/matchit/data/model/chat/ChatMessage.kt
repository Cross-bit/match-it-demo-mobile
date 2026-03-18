package com.example.matchit.data.model.chat

import com.example.matchit.data.local.db.entities.MessageStatus
import com.example.matchit.data.model.session.MatchedItemDTO
import com.example.matchit.data.model.session.SessionType
import com.google.gson.Gson
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient

/**
 * Represents an optional attachment included in a chat message.
 * Attachments may include images, videos, or any file with associated metadata.
 */
data class ChatAttachment(

    /** Public URL pointing to the uploaded attachment */
    val url: String?,

    /** MIME type of the attachment, e.g. "image/jpeg" or "application/pdf" */
    val mimeType: String?,

    /** Optional width (used mainly for images or thumbnails) */
    val width: Int? = null,

    /** Optional height (used mainly for images or thumbnails) */
    val height: Int? = null,

    /** Additional metadata provided by the client or server */
    val metadata: Map<String, Any>? = null
)

/**
 * Represents a preview of an internal app "card" or content item that users can share in chat.
 */
data class ChatCardPreview(
    /** Unique ID of the referenced card */
    val cardId: String,

    /** Title of the previewed card */
    val title: String,

    /** Optional thumbnail for visual preview */
    val thumbnailUrl: String?,

    /** Optional extra structured data related to the card */
    val metadata: Map<String, Any>? = null
)

@Serializable
sealed interface ChatSystemEvent {
    val type: String
}

@Serializable
@SerialName("MATCH_RESULT")
data class MatchResultSystemEvent(
    override val type: String = "MATCH_RESULT",

    val sessionRun: Int,
    val sessionType: SessionType,
    val matchedItems: List<MatchedItemDTO>,
    val matchedItemUUID: String
) : ChatSystemEvent

/**
 * Universal chat message data transfer object.
 * This DTO is used for:
 * - WebSocket outgoing messages
 * - WebSocket incoming messages
 * - Local storage in Room (serialized into JSON)
 * - Rendering inside the UI layer
 *
 * All fields are immutable and represent the full message payload.
 */
data class ChatMessageDTO(
    val messageUuid: String,
    val sessionUuid: String,
    val userUuid: String,
    val createdAt: Long,
    val text: String? = null,
    val attachment: ChatAttachment? = null,
    val cardPreview: ChatCardPreview? = null,

    //@kotlin.jvm.Transient
    val systemEvent: ChatSystemEvent? = null,
    val status: MessageStatus = MessageStatus.SENT,
)