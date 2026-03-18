package com.example.matchit.ui.matchingSession.matching

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.matchit.R
import com.example.matchit.data.chat.ChatRepository
import com.example.matchit.data.deviceSensors.LocationRepository
import com.example.matchit.data.friendships.FriendsRepository
import com.example.matchit.data.model.session.CardData
import com.example.matchit.data.model.session.MatchedItemDTO
import com.example.matchit.data.model.session.MatchingResultDTO
import com.example.matchit.data.model.session.SensorData
import com.example.matchit.data.model.session.SessionMetadata
import com.example.matchit.data.model.session.SessionType
import com.example.matchit.data.model.session.UserVotingResult
import com.example.matchit.data.notifications.pushNotifications.model.SessionInfo
import com.example.matchit.data.notifications.pushNotifications.model.SessionTerminatedDTO
import com.example.matchit.data.session.SessionParametersRepository
import com.example.matchit.data.session.SessionRepository
import com.example.matchit.data.websockets.WebSocketEvent
import com.example.matchit.data.websockets.WebsocketManager
import com.example.matchit.ui.matchingSession.matching.uiModels.UserOnlineStatusChanged
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SessionMatchingViewModel @Inject constructor(
    private val locationRepository: LocationRepository,
    private val websocketManager: WebsocketManager,
    private val friendsRepository: FriendsRepository,
    private val sessionRepository: SessionRepository,
    private val chatRepository: ChatRepository,
    private val sessionParametersRepository: SessionParametersRepository,
) : ViewModel() {

    private var _sessionInfo: SessionInfo? = null

    /**
     * Holds information about how user voted in current deck
     */
    private var _lastVotingData: MutableList<UserVotingResult> = mutableListOf()

    /**
     * Current deck of matching cards showed in the UI (we inform the UI on update)
     */
    private val _currentMatchingDeck = MutableLiveData<ArrayList<CardData>>()
    val currentMatchingDeck: LiveData<ArrayList<CardData>> = _currentMatchingDeck

    /**
     * Matching results over all runs sorted as they came in
     */
    private val _matchedCards = MutableLiveData<ArrayList<List<MatchedItemDTO>>>()
    val matchedCards: LiveData<ArrayList<List<MatchedItemDTO>>> = _matchedCards

    /**
     * Should we show progress bar e.g. when we are waiting for the next deck of cards
     */
    private val _showProgressBar = MutableLiveData<Boolean>()
    val showProgressBar: LiveData<Boolean> = _showProgressBar

    /**
     * Notifies client that user lost connection.
     */
    private val _notifyMemberOnlineStatusChanged = MutableSharedFlow<UserOnlineStatusChanged>()
    val notifyMemberOnlineStatusChanged = _notifyMemberOnlineStatusChanged.asSharedFlow()

    /**
     * Informs UI to open session parameters panel
     */
    private val _openSessionParametersEvent = MutableSharedFlow<SessionType>()
    val openSessionParametersEvent = _openSessionParametersEvent.asSharedFlow()

    private val _sessionVoteUpdateEvent = MutableSharedFlow<Unit>()
    val sessionVoteUpdateEvent = _sessionVoteUpdateEvent.asSharedFlow()

    /**
     * Informs UI whether to show session parameters option
     */
    private val _showParametersOption = MutableStateFlow<Boolean>(false)
    val showParametersOption = _showParametersOption.asStateFlow()

    /**
     * Informs UI whether user finished current deck
     */
    private val _currentDeckFinishedEvent = MutableSharedFlow<Unit>()
    val currentDeckFinishedEvent = _currentDeckFinishedEvent.asSharedFlow()

    /**
     * Informs UI once the matching completed successfully (we found a match)
     */
    private val _sessionEndedWithMatchEvent = MutableSharedFlow<MatchingEndResult>()
    val sessionEndedWithMatchEvent = _sessionEndedWithMatchEvent.asSharedFlow()

    /**
     * Informs UI if user disconnected from the matching session prematurely
     */
    private val _userDisconnectedEvent = MutableSharedFlow<Unit>()
    val userDisconnectedEvent = _userDisconnectedEvent.asSharedFlow()

    private var messageCounter = 0
    /**
     * Informs UI about unread chat messages
     */
    private val _showChatBadge = MutableStateFlow(false)
    val showChatBadge: StateFlow<Boolean> = _showChatBadge

    private val _unreadMessagesInfoEvent = MutableSharedFlow<Int>()
    val unreadChatMessagesInfoEvent = _unreadMessagesInfoEvent.asSharedFlow()

    private var _canShowUnreadMessagesInfo = true

    private var _currentCardIndex = 0
    val currentCardIndex: Int
        get() = _currentCardIndex

    /**
     * Informs UI about session termination event, we send appropriate resource message id (in all cases it is on UI to gracefully terminate the matching session)
     */
    private val _sessionTerminatedEvent = MutableSharedFlow<Int>()
    val sessionTerminatedEvent = _sessionTerminatedEvent.asSharedFlow()

    private var _isConnected = true;

    init {
        viewModelScope.launch {
            websocketManager.messagesEvents.collectLatest { event ->
                when(event)
                {
                    is WebSocketEvent.SessionTerminated -> {
                        event.dto.let {
                            _showProgressBar.postValue(false)
                            handleSessionTerminated(event.dto)
                        }
                    }
                    is WebSocketEvent.SessionStarted -> {
                        event.dto.sessionInfo.let {
                            _sessionInfo = event.dto.sessionInfo
                            _showProgressBar.postValue(true)

                            _sessionInfo?.sessionType?.let { type ->
                                _showParametersOption.emit(showParametersOption(type))
                            }

                            loadNextSessionResult()
                        }
                    }
                    is WebSocketEvent.MatchingResult -> {
                        event.dto.let {
                            updateTopCardIndex(0)
                            _showProgressBar.postValue(false)
                            handleNextDataOnSuccess(event.dto)
                        }
                    }
                    is WebSocketEvent.UserOnlineStatusUpdate -> {
                        event.dto.let {
                            val memberInfo = event.dto // data of the user that

                            val friendData = friendsRepository.getFriendFromCache(memberInfo.userUUID)

                            _notifyMemberOnlineStatusChanged.emit(
                                UserOnlineStatusChanged(
                                    userUUID = memberInfo.userUUID,
                                    username = friendData?.name.orEmpty(),
                                    isOnline = memberInfo.isOnline
                                )
                            )
                        }
                    }
                    else -> Unit
                }
            }
        }

        viewModelScope.launch {
            launch { observeIncomingChatMessages() }
        }
    }

    private suspend fun observeIncomingChatMessages() {
        chatRepository.incomingMessages.collect { message ->
            messageCounter++

            _showChatBadge.value = true

            if (messageCounter >= 3 && _canShowUnreadMessagesInfo) {
                viewModelScope.launch {
                    _unreadMessagesInfoEvent.emit(R.string.unread_chat_message_simple_info)
                    _canShowUnreadMessagesInfo = false
                }
                messageCounter = 0 // reset
            }
        }
    }

    fun clearBadge() {
        _showChatBadge.value = false
        _canShowUnreadMessagesInfo = true
        messageCounter = 0
    }

    private fun showParametersOption(sessionType: SessionType) : Boolean {
        val res = sessionType == SessionType.RESTAURANT
        return res
    }

    /**
     * Returns the session UUID of the current session.
     * Returns an empty string if no session is active or data is missing.
     */
    fun getCurrentSessionUUID() : String {
        return _sessionInfo?.sessionUUID ?: ""
    }

    fun openSessionParametersPanel() {
        viewModelScope.launch {
            _sessionInfo?.sessionType?.let { _openSessionParametersEvent.emit(it) }
        }
    }

    fun updateTopCardIndex(index: Int) {
        _currentCardIndex = index
    }

    fun updateUserVoting(itemIndex: Int, swipeResult: Int) {
        val card = _currentMatchingDeck.value?.getOrNull(itemIndex)
            ?: run {
                Log.e("nextSessionDataError", "Failed to load next session cards.")
                return
            }

        _lastVotingData.add(
            UserVotingResult(card.cardId, swipeResult)
        )

        viewModelScope.launch {
            _sessionVoteUpdateEvent.emit(Unit)
        }

        if (checkIfDeckFinished(itemIndex)) {
            viewModelScope.launch {
                _currentDeckFinishedEvent.emit(Unit)
                _showProgressBar.postValue(true)
                loadNextSessionResult()
            }
        }
    }

    /**
     * Removes last voted result (this is useful for rewind operation)
     */
    fun removeLastUserVoting() {
        // simply remove the last data
        _lastVotingData.removeLast()
    }

    fun disconnectFromSession() {
        viewModelScope.launch {
            _sessionInfo?.let {
                sessionRepository.disconnectFromSession(it.sessionUUID);
                _userDisconnectedEvent.emit(Unit);
                _isConnected = false;
            }
        }
    }

    private fun handleSessionTerminated(terminationData: SessionTerminatedDTO)
    {
        viewModelScope.launch {
            when (terminationData.reason) {
                SessionTerminatedDTO.TerminationReason.NO_MORE_USERS -> _sessionTerminatedEvent.emit(R.string.no_more_users_termination_message)
                SessionTerminatedDTO.TerminationReason.MATCHING_COMPLETE -> _sessionTerminatedEvent.emit(R.string.matching_complete_termination_message)
                SessionTerminatedDTO.TerminationReason.SESSION_MAX_DURATION -> _sessionTerminatedEvent.emit(R.string.session_duration_timeout_reached)
                SessionTerminatedDTO.TerminationReason.UNEXPECTED -> _sessionTerminatedEvent.emit(R.string.fatal_network_error)
                else -> _sessionTerminatedEvent.emit(R.string.fatal_network_error)
            }
        }
    }

    private fun checkIfDeckFinished(currentIndex: Int) : Boolean {
        return _currentMatchingDeck.value?.let {
            currentIndex == it.size - 1
        } ?: false
    }

    private fun loadNextSessionResult() {

        viewModelScope.launch {
            _sessionInfo?.sessionUUID?.let { sessionUUID ->
                try {
                    val sessionMetadata = getSessionMetadata()
                    val currentRoundVotes = getCurrentDeckVotes()

                    sessionRepository.updateSessionVotes(sessionUUID, currentRoundVotes, sessionMetadata);

                }
                catch (err: Exception) {
                    Log.e("sessionError", err.message.toString())
                }

                _showProgressBar.postValue(true)
            } ?:
            Log.e("sessionError", "missing session UUID unable to fetch next session data.")
        }
    }

    private fun getCurrentDeckVotes(): List<UserVotingResult> {
        val currentDeckIds = _currentMatchingDeck.value
            ?.map { it.cardId }
            ?.toSet()
            ?: emptySet()

        return _lastVotingData.filter { it.itemId in currentDeckIds }
    }

    private suspend fun getSessionMetadata() : SessionMetadata? {

        return sessionRepository.lastSessionType.firstOrNull()?.let {
            val sessionParameters = sessionParametersRepository.getSessionParameters(it)

            val gps = locationRepository.getCurrentLocation().getOrElse { error ->
                Log.e("sessionError", "Failed to load gps location for session metadata, reason: ${error.message}")
                null
            }

            SessionMetadata(sessionParameters, SensorData(gps))

        } ?: run {
            Log.e("sessionError", "Failed to create metadata, reason: Missing last session type.")
            null
        }
    }

    private fun handleNextDataOnSuccess(matchingRes: MatchingResultDTO) {
        if (!matchingRes.matched) {
            updateDeckOfCards(matchingRes.nextDeckOfCards)
        }
        else {
            handleMatchedItems(matchingRes)
            notifyUserAboutMatch(matchingRes.matchedItems)
        }
    }

    private fun updateDeckOfCards(nextDeckOfCards: List<CardData>) {
        _currentMatchingDeck.postValue(ArrayList(nextDeckOfCards))
    }

    private fun handleMatchedItems(result: MatchingResultDTO) {
        val items = result.matchedItems ?: return
        sessionRepository.updateMatchedItems(items)
    }

    private fun notifyUserAboutMatch(matchedItems: List<MatchedItemDTO>?) {
        viewModelScope.launch {
            _sessionInfo?.sessionType?.let {
                _sessionEndedWithMatchEvent.emit(MatchingEndResult(sessionType = _sessionInfo?.sessionType))
            } ?: run {
                _sessionEndedWithMatchEvent.emit(MatchingEndResult(error = R.string.matched_session_error))
                Log.e("matching_session", "Error after successful match of card. Session info not set!")
            }

        }
    }
}