package com.example.matchit.data.chat

import android.content.Context
import com.example.matchit.BuildConfig
import com.example.matchit.data.chat.DataSources.ChatLocalDataSource
import com.example.matchit.data.chat.DataSources.ChatRemoteDataSource
import com.example.matchit.data.local.db.entities.ChatMessageEntity
import com.example.matchit.data.local.db.entities.MessageStatus
import com.example.matchit.data.model.chat.ChatAttachment
import com.example.matchit.data.model.chat.ChatCardPreview
import com.example.matchit.data.model.chat.ChatMessageDTO
import com.example.matchit.data.model.chat.ChatSystemEvent
import com.example.matchit.data.remote.client.ApiErrors.ApiError
import com.example.matchit.data.remote.client.Resource
import com.example.matchit.data.userAuthentication.DataSources.UserDataLocalDataSource
import com.example.matchit.data.websockets.WebSocketEvent
import com.example.matchit.data.websockets.WebsocketManager
import com.example.matchit.data.websockets.WebsocketOutDTO
import com.example.matchit.di.ApplicationScope
import com.google.gson.Gson
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Singleton


@Singleton
class ChatRepository @Inject constructor(
    private val webSocketManager: WebsocketManager,
    private val chatLocalDataSource: ChatLocalDataSource,
    private val chatRemoteDataSource: ChatRemoteDataSource,
    private var usersLocalDataSource: UserDataLocalDataSource,
    @ApplicationScope private val appScope: CoroutineScope
) {

    init {
        subscribeToIncomingMessages()
        subscribeToAckEvents()
    }

    // holds mapping to all websocketMessageIds => chatMessageId
    private val wsToChatMessageMap =
        ConcurrentHashMap<String, String>()

    // this holds all the received ACKs so far (this is for the case server sends ACK sooner than thread writes into the wsToChatMessageMap)
    private val pendingAcks = ConcurrentHashMap<String, Unit>()

    private fun subscribeToIncomingMessages() {
        webSocketManager.messagesEvents
            .filterIsInstance<WebSocketEvent.ChatMessage>()
            .onEach { event ->
                handleIncomingChatMessage(event.dto)
            }
            .launchIn(appScope)
    }

    private fun subscribeToAckEvents() {
        webSocketManager.messagesEvents
            .filterIsInstance<WebSocketEvent.AckReceived>()
            .onEach { ack ->

                // we try to find the message UUID in the map
                val chatMessageUUID =
                    wsToChatMessageMap.remove(ack.messagaId)

                if (chatMessageUUID == null) {
                    // ACK came sooner --> store
                    pendingAcks[ack.messagaId] = Unit
                    return@onEach
                }

                // Acked
                markMessageSent(chatMessageUUID)
            }
            .launchIn(appScope)
    }

    private val _unreadCount = MutableStateFlow(0)
    val unreadCount: StateFlow<Int> = _unreadCount

    private val _incomingMessages = MutableSharedFlow<ChatMessageDTO>()
    val incomingMessages: SharedFlow<ChatMessageDTO> = _incomingMessages

    /**
     * Is raised the moment user sends new message so the message is in the PENDING state
     */
    private val _outgoingMessages = MutableSharedFlow<ChatMessageDTO>()
    val outgoingMessages = _outgoingMessages.asSharedFlow()

    /**
     * Is raised the moment server ACKs sent message therefore goes from the PENDING to the SENT state
     */
    private val _sentMessageEvents = MutableSharedFlow<String>()  // messageUUID
    val sentMessageEvents = _sentMessageEvents.asSharedFlow()

    private suspend fun handleIncomingChatMessage(messageDto: ChatMessageDTO) {
        val curUserUUID = usersLocalDataSource.getCurrentUser()?.uuid
            ?: throw IllegalStateException("User not logged in")

        chatLocalDataSource.storeChatMessage(curUserUUID, messageDto, null) // incoming messages have null message status

        _incomingMessages.emit(messageDto)
    }

    suspend fun ensureConnectedToWs() {
        val url = BuildConfig.DATA_API_WEBSOCKET_URL
        webSocketManager.connect(url)
    }

    suspend fun sendPerSessionChatMessage(
        sessionUuid: String,
        text: String?,
        attachment: ChatAttachment? = null,
        cardPreview: ChatCardPreview? = null,
        systemEvent: ChatSystemEvent? = null
    ) {
        ensureConnectedToWs()

        val curUserUUID = usersLocalDataSource.getCurrentUser()?.uuid
            ?: throw IllegalStateException("User not logged in")


        val messageUUID = UUID.randomUUID().toString()
        val createdAt = System.currentTimeMillis()

        val dto = ChatMessageDTO(
            messageUuid = messageUUID,
            sessionUuid = sessionUuid,
            userUuid = curUserUUID,
            createdAt = createdAt,
            text = text,
            attachment = attachment,
            cardPreview = cardPreview,
            systemEvent = systemEvent,
            status = MessageStatus.PENDING
        )

        // notify rest of app about it (yes we notify before storing so it is as fast as possible)
        _outgoingMessages.emit(dto)

        // store to local storage
        chatLocalDataSource.storeChatMessage(
            curUserUUID,
            dto,
            MessageStatus.PENDING
        )

        try {
            val json = Gson().toJson(dto)

            val wsMessageId = webSocketManager.sendReliable(
                WebsocketOutDTO.WsDataType.CHAT_MESSAGE,
                json
            )

            wsToChatMessageMap[wsMessageId] = messageUUID

            // ack came sooner than we wrote into the wsToChatMessageMap map...
            if (pendingAcks.remove(wsMessageId) != null) {
                markMessageSent(messageUUID)
            }

            // notify message sent
            //_sentMessageEvents.emit(messageUUID)
        } catch (e: Exception) {
            // we try to resend it later
            throw Exception("Websocket send failed", e)
        }
    }

    /**
     * We resend all the pending messages (called on init bulk load ==> we try to send all pending messages...)
     */
    public suspend fun resendPendingMessage(message: ChatMessageDTO) {

        val json = Gson().toJson(message)

        val wsMessageId = webSocketManager.sendReliable(
            WebsocketOutDTO.WsDataType.CHAT_MESSAGE,
            json
        )

        wsToChatMessageMap[wsMessageId] = message.messageUuid

        if (pendingAcks.remove(wsMessageId) != null) {
            markMessageSent(message.messageUuid)
        }
    }

    /**
     * Marks message as sent
     */
    private suspend fun markMessageSent(chatMessageUUID: String) {
        val curUserUUID = usersLocalDataSource.getCurrentUser()?.uuid ?: return

        chatLocalDataSource.updateChatMessagesStatus(
            curUserUUID,
            chatMessageUUID,
            MessageStatus.SENT
        )

        _sentMessageEvents.emit(chatMessageUUID)
    }

    suspend fun loadAllSessionChatMessages(sessionUUID: String, explicitTimestamp: Long? = null) : Resource<List<ChatMessageDTO>, *> {
        ensureConnectedToWs()

        // first we load all the cached messages from the room
        val localMessages: List<ChatMessageEntity> = chatLocalDataSource.loadAllChatMessages(sessionUUID)

        // find the timestamp of the last received message (we want to fetch only newer messages)

        val lastMessageTimestamp = 0L //localMessages.lastOrNull()?.createdAt ?: 0L
        val fromTimestamp: Long = explicitTimestamp ?: lastMessageTimestamp

        return when(val remoteResult = chatRemoteDataSource.loadAllMessageFromTheServer(sessionUUID, fromTimestamp)){
            is Resource.Success -> {

                val remoteDtos = remoteResult.data.messages


                // we store all the missing chat messages into the cache
                chatLocalDataSource.storeChatMessages(
                    currentUserUuid = sessionUUID,
                    dtos = remoteDtos
                )

                val localMapped = localMessages.map { it.toDTO() }
                val finalMessages: List<ChatMessageDTO> = (localMapped + remoteDtos)
                                    .distinctBy { it: ChatMessageDTO -> it.messageUuid } // we don't expect to have too many messages
                                    .sortedBy { it: ChatMessageDTO -> it.createdAt }

                Resource.Success<List<ChatMessageDTO>, ApiError<*>>(finalMessages)
            }
            is Resource.Error -> {
                Resource.Error(remoteResult.errorMessage, remoteResult.error)
            }
            is Resource.GeneralError -> {
                Resource.GeneralError(remoteResult.message)
            }
        }
    }
}