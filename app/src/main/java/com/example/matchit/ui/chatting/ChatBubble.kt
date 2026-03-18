package com.example.matchit.ui.chatting

import com.example.matchit.data.local.db.entities.MessageStatus
import com.example.matchit.data.model.chat.ChatAttachment
import com.example.matchit.data.model.chat.ChatCardPreview
import com.example.matchit.data.model.chat.ChatMessageDTO
import com.example.matchit.data.model.chat.ChatSystemEvent
import com.example.matchit.data.model.session.CardData
import com.example.matchit.data.model.session.SessionType
import com.example.matchit.data.remote.model.UserProfileApiResponse

sealed interface ChatSystemEventUi {
    val type: Type

    enum class Type { MATCH_RESULT }
}

data class MatchResultUi(
    val sessionType: SessionType,
    val cards: List<CardData>
) : ChatSystemEventUi {
    override val type = ChatSystemEventUi.Type.MATCH_RESULT
}


/**
 * Represents the text of a bubble in the main UI
 */
data class ChatBubble(
    val messageUuid: String,
    val isMine: Boolean,
    val userName: String? = null,
    val userAvatarUrl: String?= null,
    val text: String?= null,
    val attachment: ChatAttachment?= null,
    val cardPreview: ChatCardPreview?= null,
    val systemEvent: ChatSystemEventUi? = null,
    val createdAt: Long,
    val bubbleType: BubbleType,
    val status: MessageStatus,
    val content: Content
) {
    enum class BubbleType { TEXT, SYSTEM_EVENT }

    sealed interface Content {
        data class Text(val text: String) : Content
        data class System(val event: ChatSystemEventUi) : Content
    }
}

/*fun ChatMessageDTO.toBubble(currentUserUuid: String, authorProfile: UserProfileApiResponse?): ChatBubble {
    val type = when {
        systemEvent != null -> ChatBubble.BubbleType.SYSTEM_EVENT
        attachment != null -> ChatBubble.BubbleType.ATTACHMENT
        cardPreview != null -> ChatBubble.BubbleType.CARD_PREVIEW
        else -> ChatBubble.BubbleType.TEXT
    }

    return ChatBubble(
        messageUuid = messageUuid,
        isMine = (userUuid == currentUserUuid),
        userName = authorProfile?.name ?: "Unknown",
        userAvatarUrl = authorProfile?.avatarUrl,
        text = text,
        attachment = attachment,
        cardPreview = cardPreview,
        systemEvent = systemEvent,
        createdAt = createdAt,
        bubbleType = type,
        status = when {
            userUuid != currentUserUuid -> MessageStatus.SENT
            status != null -> status
            else -> MessageStatus.SENT
        } // if this is not our message always SENT
    )
}*/