package com.example.matchit.ui.chatting

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.matchit.R
import com.example.matchit.data.chat.ChatRepository
import com.example.matchit.data.model.chat.ChatMessageDTO
import com.example.matchit.data.remote.client.Resource
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import android.util.Log
import com.example.matchit.data.local.db.entities.MessageStatus
import com.example.matchit.data.model.authentication.LoggedInUser
import com.example.matchit.data.model.chat.MatchResultSystemEvent
import com.example.matchit.data.remote.model.UserProfileApiResponse
import com.example.matchit.data.session.SessionRepository
import com.example.matchit.data.userAccount.UserAccountRepository
import com.example.matchit.data.users.UsersRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import javax.inject.Inject

data class MessagesLoadUpdate(
    val error: Int? = null,
    val chatMessages: List<ChatBubble> = emptyList()
)

data class UpdateMessageEvent(
    val message: ChatBubble
)

data class InsertMessageEvent(
    val error: Int? = null,
    val message: ChatBubble? = null,
)

data class OutgoingMessageEvent(
    val error: Int? = null,
    val message: ChatBubble? = null,
)

@HiltViewModel
class ChatWindowViewModel @Inject  constructor(
    private val chatRepository: ChatRepository,
    private val sessionRepository: SessionRepository,
    private val userAccountRepository: UserAccountRepository,
    private val usersRepository: UsersRepository,
) : ViewModel() {

    /**
     * Holds all the messages from the last bulk load
     */
    private val _chatMessages = MutableLiveData<MessagesLoadUpdate>()
    val chatMessages: LiveData<MessagesLoadUpdate> = _chatMessages

    /**
     * Is used for the new incoming message from other users
     */
    private val _newIncomingMessageEvent = MutableSharedFlow<InsertMessageEvent>()
    val newIncomingMessageEvent = _newIncomingMessageEvent.asSharedFlow()

    /**
     * Is uses for the pending messages sent by the user
     */
    private val _newOutgoingMessageEvent = MutableSharedFlow<OutgoingMessageEvent>()
    val newOutgoingMessageEvent = _newOutgoingMessageEvent.asSharedFlow()

    /**
     * We inform UI when state of a message changes (e.g. the message went from PENDING to SENT etc.)
     */
    private val _messageUpdatesEvent = MutableSharedFlow<UpdateMessageEvent>()
    val messageUpdatesEvent = _messageUpdatesEvent.asSharedFlow()

    private var currentUser: LoggedInUser? = null

    private var isInitialized = false

    /**
     * Sorted list of all received messages by timestamp
     */
    private val messages = mutableListOf<ChatMessageDTO>()

    // we let the fragment to initialize data
    public fun initialize(sessionUUID: String) {
        if (isInitialized) return
        isInitialized = true

        viewModelScope.launch {
            // load current user
            currentUser = userAccountRepository.getUserProfileData()

            // load all previous session data
            initSessionChatWindow(sessionUUID)

            // start observe to new incoming messages from other users
            launch { observeIncomingMessages() }

            // start observe that our message was sent to the repository
            launch { observePendingMessages() }

            // start observe that our message sent to the repository was ACKed by the server
            launch { observeSentMessages() }
        }
    }

    private suspend fun observeIncomingMessages() {
        chatRepository.incomingMessages.collect { message ->
            handleNewIncomingMessage(messageDTO = message)
        }
    }

    private suspend fun observePendingMessages() {
        chatRepository.outgoingMessages.collect { dto ->
            handlePendingMessage(dto)
        }
    }

    private suspend fun observeSentMessages() {
        chatRepository.sentMessageEvents.collect { messageUUID ->
            handleMessageSent(messageUUID)
        }
    }

    /**
     * Once the message user sent was ACKed by the server it changes it's status from PENDING to SENT
     * We process it here and notify UI about this new state
     */
    private suspend fun handleMessageSent(messageUUID: String) {

        val currentUserUUID = currentUser?.uuid ?: ""

        if (currentUserUUID.isEmpty()) {
            Log.e("chatError", "Missing current user UUID")
            return
        }

        // we find the message (we previously received it as pending form)
        val index = messages.indexOfFirst { it.messageUuid == messageUUID }
        if (index == -1) return

        messages[index] = messages[index].copy(status = MessageStatus.SENT)
        val dto = messages[index]

        val profile = when (val res = usersRepository.fetchUserData(dto.userUuid)) {
            is Resource.Success -> res.data
            else -> null
        }

        /*val updatedBubble = dto.toBubble(
            currentUserUuid = currentUserUUID,
            authorProfile = profile
        )*/

        // map to ChatBubble
        dto.toUiBubble(
            currentUserUuid = currentUser?.uuid ?: "",
            authorProfile = profile,
            assetsRepository = sessionRepository
        ).collect { b ->
            // Notify UI adapter
            _messageUpdatesEvent.emit(UpdateMessageEvent(message = b))
        }
    }


    /**
     * Repository informs about new PENDING message created (message that has not been ACKed by the server)
     * We process it here and notify UI to append it to the existing messages list view (as PENDING)
     */
    private suspend fun handlePendingMessage(dto: ChatMessageDTO) {

        val currentUserUUID = currentUser?.uuid ?: ""

        if (currentUserUUID.isEmpty()){
            Log.e("chatError", "Missing current user UUID")
            return
        }

        when (val currentUserProfile = usersRepository.fetchUserData(currentUserUUID)) {
            is Resource.Success -> {

                // map to ChatBubble
                dto.toUiBubble(
                    currentUserUuid = currentUser?.uuid ?: "",
                    authorProfile = currentUserProfile.data,
                    assetsRepository = sessionRepository
                ).collect { b ->
                    // notify UI
                    messages.add(dto)
                    _newOutgoingMessageEvent.emit(OutgoingMessageEvent(message = b))
                }

            }
            else -> {
                _newOutgoingMessageEvent.emit(OutgoingMessageEvent(error = R.string.sending_message_failure))
            }
        }
    }

    private suspend fun handleNewIncomingMessage(messageDTO: ChatMessageDTO) {
        viewModelScope.launch {

            val existingIndex = messages.indexOfFirst {
                it.messageUuid == messageDTO.messageUuid
            }

            // in case we obtained already existing message
            if (existingIndex != -1) {
                messages[existingIndex] = messageDTO
            } else {
                // new message
                messages.add(messageDTO)
                messages.sortBy { it.createdAt }

                when (val authorProfileRes = usersRepository.fetchUserData(messageDTO.userUuid)) {
                    is Resource.Success -> {
                        // map to ChatBubble
                        messageDTO.toUiBubble(
                                currentUserUuid = currentUser?.uuid ?: "",
                                authorProfile = authorProfileRes.data,
                                assetsRepository = sessionRepository
                            ).collect { bubble ->
                            // notify UI
                            _newIncomingMessageEvent.emit(InsertMessageEvent(message = bubble))
                        }
                    }
                    else -> {}
                }
            }
        }
    }

    /**
     * Ensures all history messages are loaded at start of the chat window
     */
    private suspend fun initSessionChatWindow(sessionUUID: String) {
        when (val historyMessages = chatRepository.loadAllSessionChatMessages(sessionUUID)){
            is Resource.Success -> {

                messages.clear()
                messages.addAll(historyMessages.data.sortedBy { it.createdAt })

                // extract unique user UUIDs
                val userUuids = historyMessages.data.map { it.userUuid }.distinct()

                // fetch all the users metadata
                val profiles = usersRepository.fetchUserDataBatch(userUuids)

                val profileMap = when (profiles) {
                    is Resource.Success -> profiles.data.associateBy { it.uuid }
                    else -> emptyMap()
                }

                val curUserUUID = currentUser?.uuid

                if (curUserUUID == null) {
                    _chatMessages.value = MessagesLoadUpdate(error = R.string.chat_messages_load_failure)
                    Log.e("chatError", "Unable to get user info")
                    return
                }

                val chatBubbles = mutableListOf<ChatBubble>()

                historyMessages.data.forEach { msg ->
                    msg.toUiBubble(
                        currentUserUuid = curUserUUID,
                        authorProfile = profileMap[msg.userUuid],
                        assetsRepository = sessionRepository
                    ).collect { bubble ->
                        chatBubbles.add(bubble)
                    }
                }

                _chatMessages.value = MessagesLoadUpdate(chatMessages = chatBubbles)

                // Make sure once we successfully load the data --> we are connected to the WS
                chatRepository.ensureConnectedToWs()

                // resend all PENDING messages
                val pendingMessages = messages.filter { it.status == MessageStatus.PENDING }
                this.resendPendingMessages(pendingMessages)

            }
            is Resource.Error -> {
                _chatMessages.value = MessagesLoadUpdate(error = R.string.chat_messages_load_failure)
                Log.e("chatError", historyMessages.errorMessage)
            }
            is Resource.GeneralError -> {
                _chatMessages.value = MessagesLoadUpdate(error = R.string.chat_messages_load_failure)
                Log.e("chatError", historyMessages.message)
            }
        }
    }

    private suspend fun resendPendingMessages(pendingMessages: List<ChatMessageDTO>){
        pendingMessages.forEach { message ->
            chatRepository.resendPendingMessage(message)
        }
    }

    /**
     * Sends user message to the other clients.
     * Calls repository that uses websockets to transmit message to all other recipients.
     */
    fun sendMessage(sessionUuid: String, text: String) {
        val textClean = text.trim()

        if (textClean.isEmpty()) return

        viewModelScope.launch {
            chatRepository.sendPerSessionChatMessage(sessionUuid, textClean)
        }
    }

    fun ChatMessageDTO.toUiBubble(
        currentUserUuid: String,
        authorProfile: UserProfileApiResponse?,
        assetsRepository: SessionRepository
    ): Flow<ChatBubble> = flow {

        if (systemEvent is MatchResultSystemEvent) {

            val matchedItems = systemEvent.matchedItems

            val cardIds = matchedItems.map { it.itemId }

            val cardsData = when(val res = assetsRepository.getSessionCardsData(
                cardsIds = cardIds,
                sessionType = systemEvent.sessionType,
            )) {
                is Resource.Success -> res.data
                else -> null
            }

            val rankById = matchedItems.associate { it.itemId to it.rank }

            val cards = cardsData?.cards ?: return@flow

            val sortedCards = cards.sortedBy { card ->
                rankById[card.cardId] ?: Int.MAX_VALUE
            }

            emit(
                ChatBubble(
                    messageUuid = messageUuid,
                    isMine = userUuid == currentUserUuid,
                    userName = authorProfile?.name ?: "Unknown",
                    userAvatarUrl = authorProfile?.avatarUrl,
                    text = null,
                    createdAt = createdAt,
                    status = MessageStatus.SENT,
                    bubbleType = ChatBubble.BubbleType.SYSTEM_EVENT,
                    content = ChatBubble.Content.System(
                        MatchResultUi(
                            sessionType = systemEvent.sessionType,
                            cards = sortedCards
                        )
                    )
                )
            )
        }
        else if (systemEvent == null)
        {
            emit(
                ChatBubble(
                    messageUuid = messageUuid,
                    isMine = userUuid == currentUserUuid,
                    userName = authorProfile?.name ?: "Unknown",
                    userAvatarUrl = authorProfile?.avatarUrl,
                    text = text,
                    createdAt = createdAt,
                    status = status,
                    bubbleType = ChatBubble.BubbleType.TEXT,
                    content = ChatBubble.Content.Text(text.toString())
                )
            )
        }
    }

}