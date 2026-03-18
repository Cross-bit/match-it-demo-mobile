package com.example.matchit.data.websockets

import com.google.gson.Gson
import com.google.gson.JsonElement

/**
 * Defines websockets message DTO for outgoing messages
 */

data class WebsocketOutDTO (
    val sessionId: String?,
    val messageId: String?, // only reliable messages have id
    val sequence: Long?, // only reliable messages have sequence number for ordering
    val requiresAck: Boolean,
    val type: WsDataType,
    val data: Any
) {
    /**
     * Defines websockets outgoing message types.
     */
    enum class WsDataType(val value: String) {
        VOTING_RESULT("VOTING_RESULT"),
        CHAT_MESSAGE("CHAT_MESSAGE")
    }
}

fun WebsocketOutDTO.toJson(): String {
    return Gson().toJson(this)
}


/**
 * Defines websockets message DTO for incoming messages (JsonElement is used specially for parsing in gson) ... TODO: maybe generic or other method would be evenutaly better if we decide to send other data than json, but ...for now I am fine with json data type as standard for everything
 */
data class WebsocketInDTO (
    val sessionId: String?,
    val messageId: String?,
    val sequence: Long?,
    val requiresAck: Boolean,
    val type: WsDataType,
    val data: JsonElement
) {

    /**
     * Defines websockets incoming message types.
     */
    enum class WsDataType(val value: String) {
        INVITATION_RESULT("INVITATION_RESULT"),
        SESSION_STARTED("SESSION_STARTED"),
        SESSION_TERMINATED("SESSION_TERMINATED"),
        MATCHING_RESULT("MATCHING_RESULT"),
        AVAILABILITY_UPDATE("AVAILABILITY_UPDATE"),
        ONLINE_STATUS_CHANGED("ONLINE_STATUS_CHANGED"),

        CHAT_MESSAGE("CHAT_MESSAGE"),
        ERROR_OCCURRED("ERROR_OCCURRED"),
        ACK("ACK"),
        HEARTBEAT_PONG("HEARTBEAT_PONG"),
        CONNECTION_OK("CONNECTION_OK")
    }
}

data class AckDTO(
    val ackId: String
)

data class WsAckMessage(
    val type: WebsocketInDTO.WsDataType = WebsocketInDTO.WsDataType.ACK,
    val data: AckDTO
)

data class WsErrorDTO(
    val name: String,
    val message: String,
    val status: Int
)