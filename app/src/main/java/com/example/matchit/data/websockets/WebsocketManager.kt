package com.example.matchit.data.websockets

import android.util.Log
import okhttp3.ConnectionPool
import com.example.matchit.data.chat.DataSources.ChatSystemEventJsonAdapter
import com.example.matchit.data.dataStores.WebsocketSessionStoreImpl
import com.example.matchit.data.model.chat.ChatMessageDTO
import com.example.matchit.data.model.chat.ChatSystemEvent
import com.example.matchit.data.model.session.CardData
import com.example.matchit.data.model.session.MatchedItemDTO
import com.example.matchit.data.model.session.MatchingResultDTO
import com.example.matchit.data.model.session.MovieCardData
import com.example.matchit.data.model.session.RestaurantCardData
import com.example.matchit.data.model.session.SessionType
import com.example.matchit.data.model.session.UsersAvailabilityDTO
import com.example.matchit.data.notifications.pushNotifications.model.MemberSessionConnectionResultDTO
import com.example.matchit.data.notifications.pushNotifications.model.SessionStartedDTO
import com.example.matchit.data.notifications.pushNotifications.model.SessionTerminatedDTO
import com.example.matchit.data.remote.client.LoginSessionManager
import com.example.matchit.data.remote.client.Resource
import com.example.matchit.data.userAuthentication.DataSources.LoginDataSource
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.JsonDeserializationContext
import com.google.gson.JsonDeserializer
import com.google.gson.JsonElement
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import okhttp3.OkHttpClient
import okhttp3.WebSocket
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocketListener
import java.lang.reflect.Type
import java.net.SocketException
import java.util.LinkedList
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.pow

object CardDataTypeRegistry {

    fun cardClass(sessionType: SessionType): Class<out CardData> =
        when (sessionType) {
            SessionType.MOVIE -> MovieCardData::class.java
            SessionType.RESTAURANT -> RestaurantCardData::class.java
        }

    fun cardListType(sessionType: SessionType): Type =
        TypeToken.getParameterized(
            List::class.java,
            cardClass(sessionType)
        ).type
}

class CardDataDeserializer(
    private val sessionType: SessionType
) : JsonDeserializer<CardData> {

    override fun deserialize(
        json: JsonElement,
        typeOfT: Type,
        context: JsonDeserializationContext
    ): CardData {
        return context.deserialize(
            json,
            CardDataTypeRegistry.cardClass(sessionType)
        )
    }
}


class UnrecoverableWsException(
    val stuckId: String
) : Exception("Unrecoverable WebSocket desync, stuck message: $stuckId")

@Singleton
class WebsocketManager @Inject constructor(
    private val loginDataSource: LoginDataSource,
    private val loginSessionManager: LoginSessionManager,
    private val wsSessionStore: WebsocketSessionStoreImpl
) {
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private val _messagesEvents = MutableSharedFlow<WebSocketEvent>(replay = 1)
    val messagesEvents: SharedFlow<WebSocketEvent> = _messagesEvents.asSharedFlow()

    private val _connectionEvents = MutableSharedFlow<WebSocketEvent>(replay = 1)
    val connectionEvents: SharedFlow<WebSocketEvent> = _connectionEvents.asSharedFlow()

    private val flushMutex = Mutex()

    private var client = OkHttpClient.Builder()
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(45, TimeUnit.SECONDS)
        .pingInterval(10, TimeUnit.SECONDS)
        .retryOnConnectionFailure(true)
        .build()

    private var heartbeatJob: Job? = null
    private var heartbeatTimeout: Long = 20_000 // in ms

    private val MAX_RECONNECT_ATTEMPTS = 10
    private var reconnectAttempts = 0
    private var currentUrl: String? = null
    private var reconnectJob: Job? = null
    private var isManualClose = false

    private var webSocket: WebSocket? = null

    private val reconnectScope = CoroutineScope(Dispatchers.IO + SupervisorJob()) // TODO: do we need this?

    private val MAX_SOCKET_LIFETIME_MS = 2 * 60 * 1000L
    private val MAX_MESSAGE_SIZE = 1024 * 64  // max size of

    // -- unreliable transmits --
    private data class QueueItem(
        val id: String?,
        val data: String
    )

    private val processedIncommingReliable = mutableSetOf<String>()

    // queue buffer for outgoing messages
    private val unreliableMessageQueue = LinkedList<QueueItem>()

    // -- reliable transmits --
    data class OutgoingMessageRecord(
        val id: String,
        val requiresAck: Boolean,
        val sequence: Long,
        val message: String,
        var lastSendTime: Long = 0L,
        var retryCount: Int = 0
    )

    private val reliableSendQueue = ArrayDeque<OutgoingMessageRecord>()

    private var inFlight: OutgoingMessageRecord? = null

    private val RESEND_INTERVAL = 5000L     // 5s
    private val MAX_RETRY = 20              // nebo třeba 10
    private var resendJob: Job? = null


    private val listener = object : WebSocketListener() {

        override fun onOpen (webSocket: WebSocket, response: Response) {
            Log.d("WS", "Connected: ${System.identityHashCode(webSocket)}")

            // -- initiate the state --

            // for each connection we generate new session id and reset the sequence number

            lastPingSentAt = 0L       // <-- reset
            lastPongReceivedAt = 0L

            startHeartbeat()
            reconnectAttempts = 0
            reconnectJob?.cancel()
            isManualClose = false

            // -- try send pending message --
            scope.launch {
                trySendNext()
            }

            scope.launch {
                _connectionEvents.emit(WebSocketEvent.ConnectionOpened)
            }
        }

        private var closeDeferred: CompletableDeferred<Unit>? = null

        override fun onMessage(webSocket: WebSocket, text: String) {
            Log.d("WS", "Received: $text")

            val wsDTO = try {
                Gson().fromJson(text, WebsocketInDTO::class.java)
            } catch (_: Exception) {
                Log.e("WS", "Failed to parse base WS DTO")
                return
            }

            // try process the reliable message
            if (wsDTO.requiresAck && wsDTO.messageId != null) {

                val messageId = wsDTO.messageId!!

                scope.launch {

                    if (processedIncommingReliable.contains(messageId)) {
                        Log.d("WS", "Duplicate reliable message $messageId → ACK + ignore")
                        sendAck(messageId) // we only send ack to the other side
                        return@launch
                    }

                    // otherwise we process the message (the first time we received it)
                    val parsed = try {
                        parseData(wsDTO)
                    } catch (e: Exception) {
                        Log.e("WS", "Failed to process message — NOT ACKING")
                        return@launch
                    }

                    // ack reliable message
                    processedIncommingReliable.add(messageId)
                    sendAck(wsDTO.messageId)

                    // notify UI once the message is OK
                    _messagesEvents.emit(parsed)
                }

                return
            }

            // ACK/HEARTBEAT or other unreliable messages (e.g. incomming chat message)
            when (wsDTO.type) {
                WebsocketInDTO.WsDataType.ACK -> {
                    val ackData = Gson().fromJson(wsDTO.data, AckDTO::class.java)
                    if (ackData.ackId != null) {
                        handleAck(ackData.ackId)
                    } else {
                        Log.e("WS", "ACK without ackId")
                    }
                }
                WebsocketInDTO.WsDataType.HEARTBEAT_PONG -> {
                    Log.d("WS", "HEARTBEAT_PONG received")
                    lastPongReceivedAt = System.currentTimeMillis()
                }
                else -> {
                    scope.launch {
                        val parsed = parseData(wsDTO)
                        _messagesEvents.emit(parsed)
                    }
                }
            }
        }

        private fun handleAck(ackId: String) {
            val cur = inFlight

            if (cur != null && cur.id == ackId) {
                Log.d("WS", "ACK received for " + ackId)
                inFlight = null
                _messagesEvents.tryEmit(WebSocketEvent.AckReceived(ackId))
                trySendNext() // try to send another message
            } else {
                Log.d("WS", "ACK for unknown or old message $ackId — ignoring")
            }
        }

        private fun sendAck(id: String) {
            val ackMessage = WsAckMessage(
                type = WebsocketInDTO.WsDataType.ACK,
                data = AckDTO(ackId = id)
            )

            val json = Gson().toJson(ackMessage)
            webSocket?.send(json)
        }


        override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
            val msg = t.message ?: "unknown"
            val isSocketClosed = t is SocketException && (
                    msg.contains("Socket closed", ignoreCase = true) || msg == "unknown"
                    )

            if (isManualClose || isSocketClosed) {
                Log.i("WS", "Ignoring expected close failure (${t.javaClass.simpleName}: $msg)")
                return
            }

            resendJob?.cancel()
            resendJob = null

            Log.e("WS", "WebSocket failure: ${t.javaClass.simpleName}: $msg, code=${response?.code}, message=${response?.message}")
            scheduleReconnect()
        }

        override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
            Log.i("WS", "Socket closed: $code $reason")
            closeDeferred?.complete(Unit)
            closeDeferred = null
            resendJob?.cancel()
            resendJob = null
        }

        override fun onClosing(webSocket: WebSocket, code: Int, reason: String) {
            Log.e("WS", "Closing: $code")

            if (!isManualClose) {
                Log.w("WS", "Server initiated close – scheduling reconnect.")
                scheduleReconnect()
            } else {
                Log.i("WS", "Manual close – no reconnect.")
            }

            scope.launch {
                _connectionEvents.emit(WebSocketEvent.ConnectionClosing(code, reason))
            }

            isManualClose = false
        }
    }

    suspend fun connect(url: String): Boolean {

        if (webSocket != null) {
            Log.w("WS", "connect() called but websocket already active -> skipping")
            return false
        }

        currentUrl = url

        try { // for case ... we make some mistake and not dispose the webSocket before ...
            webSocket?.cancel()
        } catch (e: Exception) {
            Log.e("WS", "Error cancelling previous socket: ${e.message}")
        }

        webSocket = null

        processedIncommingReliable.clear()

        // Authentication
        val refreshToken = loginSessionManager.getRefreshToken() ?: throw UserIsLoggedOutError()

        val refreshResult = loginDataSource.reauthenticateUser(refreshToken)

        when (refreshResult) {
            is Resource.Success -> loginSessionManager.saveAuthToken(refreshResult.data.updatedToken)
            else -> {
                Log.e("WS", "Reauthentication failed")
                scope.launch {
                    _messagesEvents.emit(WebSocketEvent.AuthenticationFailed)
                }
                return false
            }
        }

        val authenticationToken = loginSessionManager.getAuthToken() ?: throw UserIsLoggedOutError()
        val request = Request.Builder()
            .url(url)
            .addHeader("Authorization", "Bearer $authenticationToken")
            .build()

        Log.d("WS", "Connecting to $url")

        client = OkHttpClient.Builder()
            .readTimeout(60, TimeUnit.SECONDS)
            .writeTimeout(45, TimeUnit.SECONDS)
            .pingInterval(10, TimeUnit.SECONDS)
            .retryOnConnectionFailure(true)
            .connectionPool(ConnectionPool(0, 1, TimeUnit.NANOSECONDS))
            .build()

        // clear all old not acked messages from the previous connection
        processedIncommingReliable.clear()

        webSocket = client.newWebSocket(request, listener)
        Log.d("WS", "Connected: $webSocket")

        return true
    }

    private fun flushOutgoingMessagesQueue() {
        Log.i("WS", "Flushing ${unreliableMessageQueue.size} queued messages…")
        CoroutineScope(Dispatchers.IO).launch {
            flushMutex.withLock {
                while (true) {
                    val message = synchronized(unreliableMessageQueue) { unreliableMessageQueue.poll() } ?: break // we send all messages => done
                    val sent = webSocket?.send(message.data) ?: false
                    if (!sent) {
                        Log.w("WS", "Send failed, queued: $message")
                        synchronized(unreliableMessageQueue) { unreliableMessageQueue.addFirst(message) } // return message back
                        break
                    }
                    else {
                        Log.d("WS", "Sent: $message")
                    }
                }
            }
        }
    }

    enum class ConnectionHealth {
        HEALTHY,
        STALE,
        DISCONNECTED
    }

    fun isConnectionHealthy(): ConnectionHealth {
        if (webSocket == null) return ConnectionHealth.DISCONNECTED

        if (lastPingSentAt == 0L) return ConnectionHealth.HEALTHY

        if (lastPongReceivedAt >= lastPingSentAt) return ConnectionHealth.HEALTHY

        val silenceSinceLastPing = System.currentTimeMillis() - lastPingSentAt
        return if (silenceSinceLastPing >= heartbeatTimeout) {
            ConnectionHealth.STALE
        } else {
            ConnectionHealth.HEALTHY
        }
    }

    private fun trySendNext() {

        // we can't send next if something is already waiting for ACK (send & wait)
        inFlight?.let { msg ->
            val sent = webSocket?.send(msg.message) ?: false
            if (sent) {
                msg.lastSendTime = System.currentTimeMillis()
                startResendJob()  // make sure timer runs
            }
            return
        }

        // select next message in the queue (or do nothing)
        val next = reliableSendQueue.removeFirstOrNull() ?: return

        // send message
        val sent = webSocket?.send(next.message) ?: false
        if (!sent) {
            // return back to queue
            reliableSendQueue.addFirst(next)
            return
        }

        // set it as inflight
        next.lastSendTime = System.currentTimeMillis()
        inFlight = next

        // make sure the resend job is running
        startResendJob()
    }

    suspend fun sendReliable(type: WebsocketOutDTO.WsDataType, data: String) : String {
        val sessionId = wsSessionStore.getOrCreateSessionId()
        val seq = wsSessionStore.nextSequence()
        val msgId = "$sessionId-$seq"

        val dto = WebsocketOutDTO(
            sessionId = sessionId,
            messageId = msgId,
            sequence = seq,
            requiresAck = true,
            type = type,
            data = data
        )

        val record = OutgoingMessageRecord(
            id = msgId,
            sequence = seq,
            requiresAck = true,
            message = dto.toJson(),
            lastSendTime = 0,
            retryCount = 0
        )

        reliableSendQueue.addLast(record)
        trySendNext()

        return msgId
    }

    fun sendUnreliable(type: WebsocketOutDTO.WsDataType, data: String) {
        scope.launch {
            val sessionId = wsSessionStore.getOrCreateSessionId()

            val dto = WebsocketOutDTO(
                sessionId = sessionId,
                messageId = null,
                sequence = null,
                requiresAck = false,
                type = type,
                data = data
            )

            val jsonString = dto.toJson()

            synchronized(unreliableMessageQueue) {
                unreliableMessageQueue.addLast(QueueItem(null, jsonString))
            }

            flushOutgoingMessagesQueue()
        }
    }

    private fun startResendJob() {
        if (resendJob?.isActive == true) return

        resendJob = scope.launch(Dispatchers.IO) {
            while (isActive) {
                delay(RESEND_INTERVAL)

                // no message to resend => skip
                val msg = inFlight ?: continue
                val now = System.currentTimeMillis()

                if (now - msg.lastSendTime >= RESEND_INTERVAL) {

                    // too many attempts ==> fatal error
                    if (msg.retryCount >= MAX_RETRY) {
                        Log.e("WS", "Reliable message ${msg.id} failed after MAX_RETRY")
                        resolveResendFailure()
                        return@launch
                    }

                    Log.w("WS", "Resending in-flight msg ${msg.id} retry=${msg.retryCount}")

                    val sent = webSocket?.send(msg.message) ?: false
                    if (sent) {
                        msg.retryCount++
                        msg.lastSendTime = now
                    }
                }
            }
        }
    }

    private fun resolveResendFailure() {

        val msg = inFlight

        Log.e("WS", "Unrecoverable WS error: messages stuck (${msg})")

        // Inform UI / upper layers
        scope.launch {
            _connectionEvents.emit(
                WebSocketEvent.Error(
                    UnrecoverableWsException(msg?.id.toString())
                )
            )
        }

        // Clean the queue
        inFlight = null
        reliableSendQueue.clear()
    }


    private fun scheduleReconnect(delayMs: Long = 2000L) {
        if (reconnectJob?.isActive == true) return

        reconnectJob = scope.launch {
            if (reconnectAttempts >= MAX_RECONNECT_ATTEMPTS) {
                Log.e("WS", "Max reconnect attempts reached ($MAX_RECONNECT_ATTEMPTS). Giving up.")
                _connectionEvents.tryEmit(WebSocketEvent.ConnectionClosing(1001, "Max reconnect attempts reached"))
                return@launch
            }

            reconnectAttempts++
            val nextDelay = (1000L * 2.0.pow(reconnectAttempts)).toLong().coerceAtMost(60_000L)

            delay(delayMs)

            cleanupSocket("before reconnect #$reconnectAttempts")

            Log.w("WS", "Reconnecting attempt #$reconnectAttempts (next delay=$nextDelay ms)")

            try {
                currentUrl?.let {
                    if (!connect(it)) {
                        reconnectJob?.cancel()
                        scheduleReconnect(nextDelay)
                    }

                }
            } catch (e: Exception) {
                Log.e("WS", "Reconnect failed: ${e.message}")
                reconnectJob?.cancel()
                scheduleReconnect(nextDelay)
            }
        }
    }

    fun onNetworkChanged(isOnline: Boolean) {
        if (!isOnline) {
            Log.w("WS", "Network lost → closing socket")
            disconnect()
        } else {
            Log.w("WS", "Network reconnected → trying reconnect")

            scope.launch {
                disconnect()

                // Adding small wait before the reconnect
                delay(3000)

                client.connectionPool.evictAll()

                currentUrl?.let { url ->
                    Log.i("WS", "Reconnecting to $url after network change")
                    connect(url)
                }
            }
        }
    }

    /** Hard kill (call consciously) */
    private fun cleanupSocket(reason: String? = null) {
        Log.w("WS", "Cleaning up socket. Reason: $reason")

        heartbeatJob?.cancel()
        heartbeatJob = null
        
        processedIncommingReliable.clear()

        processedIncommingReliable.clear()

        processedIncommingReliable.clear()

        webSocket?.close(NORMAL_CLOSURE_STATUS, reason)
        webSocket = null

    }

    fun disconnect() {
        try {
            Log.w("WS", "Manual disconnect called")
            isManualClose = true

            // cancle related jobs
            reconnectJob?.cancel()
            heartbeatJob?.cancel()

            resendJob?.cancel()
            resendJob = null

            processedIncommingReliable.clear()

            //close the connection
            webSocket?.let { ws ->
                try {
                    ws.cancel()
                    ws.close(NORMAL_CLOSURE_STATUS, "Manual disconnect")
                } catch (e: Exception) {
                    Log.e("WS", "Error closing socket: ${e.message}")
                }
            }

            // clean websocket
            webSocket = null

            //clean tcp
            client.connectionPool.evictAll()

            Log.d("WS", "Disconnected and cleaned up")
        } catch (e: Exception) {
            Log.e("WS", "Disconnect exception: ${e.message}")
        }
    }

    @Volatile private var lastPongReceivedAt: Long = 0L
    @Volatile private var lastPingSentAt: Long = 0L

    private fun startHeartbeat() {
        heartbeatJob?.cancel()
        heartbeatJob = scope.launch {
            while (isActive) {
                delay(heartbeatTimeout)

                if (lastPingSentAt > 0L && lastPongReceivedAt < lastPingSentAt) {
                    val silenceMs = System.currentTimeMillis() - lastPingSentAt
                    if (silenceMs >= heartbeatTimeout) {
                        Log.e("WS", "Heartbeat watchdog fired — no PONG for ${silenceMs}ms → reconnecting")
                        // reset aby se watchdog nespustil znovu během reconnectu
                        lastPingSentAt = 0L
                        lastPongReceivedAt = 0L
                        scheduleReconnect(0L)
                        return@launch
                    }
                }

                // --- pošli ping ---
                lastPingSentAt = System.currentTimeMillis()

                try {
                    webSocket?.send("""{"type":"HEARTBEAT"}""")
                    Log.d("WS", "Heartbeat sent")
                } catch (e: Exception) {
                    Log.e("WS", "Heartbeat failed: $e")
                }
            }
        }
    }

    private fun stopHeartbeat() {
        heartbeatJob?.cancel()
        heartbeatJob = null
    }


    private fun parseData(webSocketDTO: WebsocketInDTO): WebSocketEvent {
        return try {

            when (webSocketDTO.type) {
                WebsocketInDTO.WsDataType.INVITATION_RESULT -> {
                    val dataMessage = Gson().fromJson(webSocketDTO.data, MemberSessionConnectionResultDTO::class.java)
                    WebSocketEvent.MemberConnectionResult(dataMessage)
                }
                WebsocketInDTO.WsDataType.SESSION_STARTED -> {
                    val dataMessage = Gson().fromJson(webSocketDTO.data, SessionStartedDTO::class.java)
                    WebSocketEvent.SessionStarted(dataMessage)
                }
                WebsocketInDTO.WsDataType.SESSION_TERMINATED -> {
                    val dataMessage = Gson().fromJson(webSocketDTO.data, SessionTerminatedDTO::class.java)
                    WebSocketEvent.SessionTerminated(dataMessage)
                }
                WebsocketInDTO.WsDataType.MATCHING_RESULT -> {
                    val jsonObject = webSocketDTO.data.asJsonObject
                    val matched = jsonObject.get("matched").asBoolean
                    val matchedItemUUID = jsonObject.get("matchedItemUUID").asString
                    val sessionType = SessionType.valueOf(jsonObject.get("sessionType").asString)

                    // we build gson with special deserializer for the card data type
                    val gson = GsonBuilder()
                        .registerTypeAdapter(
                            CardData::class.java,
                            CardDataDeserializer(sessionType)
                        ).create()

                    val matchedItems: List<MatchedItemDTO> =
                        gson.fromJson(
                            jsonObject.get("matchedItems"),
                            object : TypeToken<List<MatchedItemDTO>>() {}.type
                        )

                    val parsedCards: List<CardData> = gson.fromJson(jsonObject.get("nextDeckOfCards"), CardDataTypeRegistry.cardListType(sessionType))
                    WebSocketEvent.MatchingResult(MatchingResultDTO(matched, parsedCards, matchedItems, matchedItemUUID, sessionType))
                }
                WebsocketInDTO.WsDataType.AVAILABILITY_UPDATE -> {
                    val dataMessage = Gson().fromJson(webSocketDTO.data, UsersAvailabilityDTO::class.java)
                    WebSocketEvent.AvailabilityUpdate(dataMessage)
                }
                WebsocketInDTO.WsDataType.CHAT_MESSAGE -> {
                    val gson = GsonBuilder()
                        .registerTypeAdapter(
                            ChatSystemEvent::class.java,
                            ChatSystemEventJsonAdapter()
                        ).create()

                    val finalMessage = gson.fromJson(webSocketDTO.data, ChatMessageDTO::class.java)
                    WebSocketEvent.ChatMessage(finalMessage)
                }
                WebsocketInDTO.WsDataType.ONLINE_STATUS_CHANGED -> {
                    val dataMessage = Gson().fromJson(webSocketDTO.data, WsOnlineStatusChangedDTO::class.java)
                    WebSocketEvent.UserOnlineStatusUpdate(dataMessage)
                }
                WebsocketInDTO.WsDataType.ERROR_OCCURRED -> {
                    val dataMessage = Gson().fromJson(webSocketDTO.data, WsErrorDTO::class.java)
                    WebSocketEvent.DataError(dataMessage)
                }
                else -> WebSocketEvent.Unknown(webSocketDTO?.type?.value ?: "null")
            }
        } catch (e: Exception) {
            Log.e("WS", "Error parsing: $e")
            WebSocketEvent.Error(e)
        }
    }

    companion object {
        private const val NORMAL_CLOSURE_STATUS = 1000
    }
}

sealed class WebSocketEvent {
    data class MemberConnectionResult(val result: MemberSessionConnectionResultDTO) : WebSocketEvent()
    data class SessionStarted(val dto: SessionStartedDTO) : WebSocketEvent()
    data class SessionTerminated(val dto: SessionTerminatedDTO) : WebSocketEvent()
    data class MatchingResult(val dto: MatchingResultDTO) : WebSocketEvent()
    data class ChatMessage(val dto: ChatMessageDTO) : WebSocketEvent()
    data class AckReceived(val messagaId: String) : WebSocketEvent()
    data class AvailabilityUpdate(val dto: UsersAvailabilityDTO) : WebSocketEvent()
    data class UserOnlineStatusUpdate(val dto: WsOnlineStatusChangedDTO) : WebSocketEvent()
    object ConnectionOpened : WebSocketEvent()
    data class ConnectionClosing(val code: Int, val reason: String) : WebSocketEvent()
    data class Unknown(val type: String) : WebSocketEvent()
    data class DataError(val wsError: WsErrorDTO) : WebSocketEvent()
    data class Error(val exception: Throwable) : WebSocketEvent()
    object AuthenticationFailed : WebSocketEvent()
}

class UserIsLoggedOutError : Exception("User is logged out")

