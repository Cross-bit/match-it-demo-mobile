package com.example.matchit.ui.matchingSession.creation.invitation

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.matchit.BuildConfig
import com.example.matchit.R
import com.example.matchit.data.friendships.FriendsRepository
import com.example.matchit.data.model.session.AlgorithmType
import com.example.matchit.data.model.session.SessionParameters
import com.example.matchit.data.model.session.SessionType
import com.example.matchit.data.model.session.UsersAvailabilityInfo
import com.example.matchit.data.notifications.pushNotifications.model.MemberSessionConnectionResultDTO
import com.example.matchit.data.notifications.pushNotifications.model.SessionTerminatedDTO
import com.example.matchit.data.remote.client.Resource
import com.example.matchit.data.session.SessionParametersRepository
import com.example.matchit.data.session.SessionRepository
import com.example.matchit.data.websockets.WebSocketEvent
import com.example.matchit.data.websockets.WebsocketManager
import com.example.matchit.ui.matchingSession.creation.invitation.friendList.FriendInviteListItem
import com.example.matchit.ui.matchingSession.creation.invitation.parametersSettings.SessionParametersFactory
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject

@HiltViewModel
class SessionInviteViewModel @Inject constructor(
    private val friendsRepository: FriendsRepository,
    private val sessionRepository: SessionRepository,
    private val sessionParametersRepository: SessionParametersRepository,
    private val websocketManager: WebsocketManager
) : ViewModel() {

    private val _allFriends = MutableLiveData<ArrayList<FriendInviteListItem>>()
    val allFriends: LiveData<ArrayList<FriendInviteListItem>> = _allFriends

    /**
     * Notify of updating item in allFriends collection
     */
    private val _friendDataUpdateEvent = MutableSharedFlow<Int>()
    val friendDataUpdate = _friendDataUpdateEvent

    /**
     * SessionUUID
     */
    private val _sessionUUID = MutableLiveData<String>()
    val sessionUUID: LiveData<String> = _sessionUUID

    /**
     * Specifies whether session can be started
     */
    private val _sessionIsReadyEvent = MutableSharedFlow<SessionReadyState>()
    val sessionIsReadyToBeStarted = _sessionIsReadyEvent.asSharedFlow()


    private val _sessionStartedEvent = MutableSharedFlow<SessionReadyState>()
    val sessionStarted = _sessionStartedEvent.asSharedFlow()

    private val _sessionInvitationCanceledEvent = MutableSharedFlow<Unit>()
    val sessionInvitationCanceledEvent = _sessionInvitationCanceledEvent.asSharedFlow()

    /**
     * Informs UI if invitation is in progress
     */
    private val _sessionInvitationStatusUpdated = MutableSharedFlow<Boolean>()
    val sessionInvitationStatusUpdated = _sessionInvitationStatusUpdated.asSharedFlow()

    private val _friendConnectionResultEvent = MutableSharedFlow<ConnectionResult>()
    val friendConnectionResult = _friendConnectionResultEvent.asSharedFlow()

    private val _sessionTerminatedEvent = MutableSharedFlow<Int>()
    val sessionTerminatedEvent = _sessionTerminatedEvent.asSharedFlow()

    /**
     * Raised when any time user was selected (only on positive selection, is not raised on disselection and otherwise)
     */
    private val _userSelectedEvent = MutableSharedFlow<String>()
    val userSelectedEvent = _userSelectedEvent.asSharedFlow()

    /**
     * Maximal number of users creator can invite.
     */
    private var maxNumberOfClientsToInvite = 3

    private val _numberOfClientsSelected = MutableLiveData<Int>(0)
    val numberOfClientsSelected: LiveData<Int> = _numberOfClientsSelected

    private val instanceId = UUID.randomUUID().toString()
    private var numberOfClientsInvited = 0

    /**
     * Current number of clients selected (used to check if we can more users to the selection)
     */
    private var numberOfClientsRejectedConnection = 0
    /**
     * Number of others users (not counting us as creator) connected to the session
     */
    private var numberOfClientsConnectedToTheSession = 0
    /**
     * Minimal number of other users (than us as creator) to connect to the session
     */
    private val minimalNumberOfClientsToConnect = 1

    private var _currentSessionType = MutableSharedFlow<SessionType>(replay = 1)
    val currentSessionType = _currentSessionType.asSharedFlow()

    private val webSocketJobs = mutableListOf<Job>()

    init {
        viewModelScope.launch {
            sessionRepository.lastSessionType.collect { it?.let { _currentSessionType.emit(it) } }
        }

        webSocketJobs += viewModelScope.launch {

            // When new notification about new user connection comes
            launch {
                websocketManager.messagesEvents.collect { event ->
                    when(event) {
                        is WebSocketEvent.MemberConnectionResult -> {
                            event.result?.let { connectedUser ->
                                handleMemberConnectionResult(connectedUser);
                            }
                        }
                        is WebSocketEvent.SessionStarted -> {
                            event.dto?.let {
                                _sessionStartedEvent.emit(SessionReadyState(
                                    true,
                                    R.string.session_started
                                ))
                            }
                        }
                        is WebSocketEvent.SessionTerminated -> {
                            if (event.dto.sessionUUID != _sessionUUID.value) return@collect

                            val message = when(event.dto.reason) {
                                SessionTerminatedDTO.TerminationReason.SESSION_CREATION_TIMEOUT -> R.string.session_creation_timeout_reached
                                SessionTerminatedDTO.TerminationReason.SESSION_MAX_DURATION -> R.string.session_duration_timeout_reached
                                SessionTerminatedDTO.TerminationReason.UNEXPECTED -> R.string.session_invitation_failed
                                SessionTerminatedDTO.TerminationReason.EXPLICIT -> null
                                else -> null
                            }
                            message?.let{ _sessionTerminatedEvent.emit(message) }
                        }
                        else -> Unit
                    }

                }
            }
        }
    }

    fun setAlgorithm(algorithm: AlgorithmType) {
        viewModelScope.launch {
            val curSessionType = sessionRepository.lastSessionType.firstOrNull() ?: run {
                Log.e("session-type-null", "Session type was null")
                return@launch
            }

            val params = sessionParametersRepository.getSessionParameters(curSessionType)
                ?: SessionParametersFactory.createDefault(curSessionType)

            val updated = when(params) {
                is SessionParameters.RestaurantParameters -> params.copy(algorithm = algorithm)
                is SessionParameters.MovieParameters -> params.copy(algorithm = algorithm)
            }
            sessionParametersRepository.updateSessionParameters(
                updated,
                curSessionType
            )
        }
    }

    /**
     * Reloads completely all friends data from the repository to the current invitation list.
     */
    fun loadAllFriendsData() {
        viewModelScope.launch {

            when (val result = friendsRepository.getAllFriends()) {

                is Resource.Success -> {

                    val friends = result.data.friends

                    val friendsUUIDs = friends.map { f -> f.uuid }
                    val friendsAvailability = getUsersAvailability(friendsUUIDs)
                    val availableFriends = friendsAvailability.filter { u -> u.state == UsersAvailabilityInfo.AvailableState.AVAILABLE }.map { u -> u.uuid }

                    val friendsData: ArrayList<FriendInviteListItem> = ArrayList(
                        friends.map {
                        FriendInviteListItem(
                            it.uuid,
                            it.profilePicUrl,
                            it.name,
                            if (availableFriends.contains(it.uuid)) FriendInviteListItem.InvitationState.INVITABLE else FriendInviteListItem.InvitationState.NOT_INVITABLE //TODO: add state that would state availability explicitly
                        )
                    })

                    _allFriends.value = ArrayList(friendsData)
                }
                is Resource.Error -> {
                    _sessionIsReadyEvent.emit(SessionReadyState(false, R.string.session_init_failed))
                    Log.e("friendsError", result.errorMessage)
                }
                is Resource.GeneralError -> {
                    _sessionIsReadyEvent.emit(SessionReadyState(false, R.string.session_init_failed))
                    Log.e("friendsConnectionError", result.message)
                }
                else -> {
                    Log.e("panicError", "Panicking during friends list data update")
                    throw AssertionError() // kill the app is it ok?
                }
            }
        }
    }

    /**
     * Adds member to the selected invitation list based on their uuid [invitedFriendUUID].
     */
    fun addMemberToSelection(invitedFriendUUID: String) {
        viewModelScope.launch {
            updateSelectedFriends(invitedFriendUUID);
            _numberOfClientsSelected.value?.let { _numberOfClientsSelected.value = it + 1 }
        }
    }

    /**
     * Removed member from the selected invitation list based on their [uuid].
     */
    fun removeMemberFromSelection(uuid: String) {

        if (_numberOfClientsSelected.value == 0) {
            Log.e("error", "Trying to unselect negative amount of users...");
        }

        _allFriends.value?.let { friends ->

            val updatedList: ArrayList<FriendInviteListItem> = ArrayList(friends.map {
                FriendInviteListItem(
                    it.uuid,
                    it.thumbnail,
                    it.username,
                    if (it.uuid == uuid) FriendInviteListItem.InvitationState.INVITABLE else it.invitationState
                )
            })

            _allFriends.value = updatedList;
        }

        _numberOfClientsSelected.value?.let { _numberOfClientsSelected.value = it - 1 }
    }

    private suspend fun getUsersAvailability(friendsUUIDs:  List<String>) : List<UsersAvailabilityInfo> {
         val availability = sessionRepository.getFriendsAvailability(friendsUUIDs);

        if (availability is Resource.Success) {
            return availability.data.usersInfo;

        }
        return emptyList();
    }

    /**
     * Updates invitation list of selected friends (before use actually sends the invite) with newly selected friend having [invitedFriendUUID].
     */
    private suspend fun updateSelectedFriends(invitedFriendUUID: String) {
        _allFriends.value?.let { friends ->

            // Currently we allow only sessions of two => we block all users except the invited one

            val updatedList: ArrayList<FriendInviteListItem> = ArrayList(friends.mapIndexed { index, friend ->

                var itemState = friend.invitationState;

                if (canSelectUserItem(index) && friend.uuid == invitedFriendUUID) {
                    itemState = FriendInviteListItem.InvitationState.SELECTED
                    this._userSelectedEvent.emit(invitedFriendUUID)
                }

                FriendInviteListItem(
                    friend.uuid,
                    friend.thumbnail,
                    friend.username,
                    itemState
                )
            })

            _allFriends.value = updatedList;

        }
    }

    /**
     * Action called by the the user(session creator), once the session is ready.
     */
    fun startMatchingSession() {

        viewModelScope.launch {
                val res = sessionRepository.startSession()
                if (res is Resource.Error || res is Resource.GeneralError) {
                    Log.e("errorSession", "Session start failed originated from creators request to start the session")
                    _sessionIsReadyEvent.emit(SessionReadyState(false, R.string.session_init_failed))
                }
        }
    }

    /**
     * Creates new session based on the friends selected in the invitation list.
     */
    fun inviteMembersAndCreateSession() {

        viewModelScope.launch {
            websocketManager.disconnect()
            websocketManager.connect(BuildConfig.DATA_API_WEBSOCKET_URL)
        }

        viewModelScope.launch {

            var selectedMembersUUIDs: ArrayList<String> = ArrayList<String>();

            _allFriends.value?.let { friends ->

                selectedMembersUUIDs = ArrayList(
                friends.filter { friend -> friend.isSelected() }
                .map { friend -> friend.uuid }
                )

                friends.onEachIndexed { index, friend ->
                    if (friend.isSelected()) setUserItemInvited(index) else setUserItemNotInvitable(index)
                }
            }

            numberOfClientsInvited = selectedMembersUUIDs.size;

            if (numberOfClientsInvited == 0)
                return@launch

            val result = sessionRepository.createSession(_currentSessionType.replayCache.first(), selectedMembersUUIDs)

            if (result is Resource.Success) {
                val newSessionUUID = result.data
                if (newSessionUUID.isNotEmpty()) {
                    _sessionUUID.value = newSessionUUID
                    _sessionInvitationStatusUpdated.emit(true)
                }
                else {
                    _sessionIsReadyEvent.emit(SessionReadyState(false, R.string.session_init_failed))
                    Log.e("errorSession", "Friend list item state update in view model failed")
                }
            }
            else {
                _sessionIsReadyEvent.emit(SessionReadyState(false, R.string.connection_error_2))
                Log.e("errorSession", "Friend list item state update in view model failed")
            }
        }
    }

    fun cancelInvitation() {
        viewModelScope.launch {
            val terminationResult = sessionRepository.terminateSession()

            when(terminationResult) {
                is Resource.Success -> {
                    if (terminationResult.data) {
                        clearSessionState() // we make sure that all the data previous session data are cleared

                        _sessionInvitationCanceledEvent.emit(Unit) // notify UI
                        _sessionInvitationStatusUpdated.emit(false)

                        loadAllFriendsData()
                    }
                }
                else -> {
                    //TODO:
                }
            }
        }
    }

    /**
     * Sets user item of [index] to be invited, so the UI updates changes on its update.
     */
    private suspend fun setUserItemInvited(index: Int)
    {
        if (updateFriendState(index, FriendInviteListItem.InvitationState.INVITED)) {
            _friendDataUpdateEvent.emit(index)
        }
        else {
            _sessionIsReadyEvent.emit(SessionReadyState(false, R.string.session_init_failed))
            Log.e("errorSession", "Friend list item state update in view model failed, updating item ${index.toString()} to invited")
        }
    }

    private suspend fun setUserItemNotInvitable(index: Int)
    {
        if (updateFriendState(index, FriendInviteListItem.InvitationState.NOT_INVITABLE)) {
            _friendDataUpdateEvent.emit(index)
        }
        else {
            _sessionIsReadyEvent.emit(SessionReadyState(false, R.string.session_init_failed))
            Log.e("errorSession", "Friend list item state update in view model failed, updating item ${index.toString()} to not invitable")
        }
    }

    private fun isUserItemItemInvitable(index: Int): Boolean {
        return _allFriends.value?.let {
            if (index > it.size)
                return false

            it[index].invitationState == FriendInviteListItem.InvitationState.INVITABLE;
        } ?: false;
    }

    /**
     * Checks whether creator of the session can select given item of [index] from the invitation list.
     */
    private fun canSelectUserItem(index: Int): Boolean {
        return _allFriends.value?.let {
            canInviteAnotherUser() && isUserItemItemInvitable(index);
        } ?: false;
    }

    /**
     * Handles result of user invitation (Once we receive result – [connectionInfo] of the user invitation, whether he accepted/rejected it).
     *
     */
    private suspend fun handleMemberConnectionResult(connectionInfo: MemberSessionConnectionResultDTO)
    {
        val userUUID = connectionInfo.userUUID

        val dataIndex = (_allFriends.value?.indexOfFirst { it.uuid == userUUID }) ?: -1
        val friendData = _allFriends.value?.getOrNull(dataIndex)

        friendData?.let {

            val userAcceptedConnection =
                (connectionInfo.result == MemberSessionConnectionResultDTO.ConnectionResult.CONNECTED)

            updateUserConnectionCounters(userAcceptedConnection)

            if (!isSessionStartable())
                return handleNoUsersConnected()


            if (updateUserItemStateOnConnectionResult(dataIndex, userAcceptedConnection)) {

                _friendConnectionResultEvent.emit(ConnectionResult(
                    friendData.username,
                    R.string.invited_user_connected_to_session
                ))

                // notify we updated
                _friendDataUpdateEvent.emit(dataIndex)

            } else {
                _friendConnectionResultEvent.emit(ConnectionResult(
                    null,
                    R.string.user_session_connection_failed
                ))
            }

            tryMakeSessionReady();
        }
    }

    private fun updateUserConnectionCounters(userAcceptedConnection: Boolean) {
        if (userAcceptedConnection)
            numberOfClientsConnectedToTheSession++;
        else
            numberOfClientsRejectedConnection++
    }

    /**
     * Checks whether we have enough connected clients to start the session.
     */
    private fun canSessionBeStarted(): Boolean {
        return isSessionStartable() && numberOfClientsConnectedToTheSession >= minimalNumberOfClientsToConnect; // we have enough clients
    }

    /**
     * Checks whether it is possible for session to be started. True if not all invitations were rejected yet. (Then there must be some invitation not resolved, or others had to be accpeted)
     */
    private fun isSessionStartable(): Boolean {
        return numberOfClientsRejectedConnection < numberOfClientsInvited;

    }

    /**
     * Checks whether the maximal allowed number of matching participants was selected.
     */
    private fun canInviteAnotherUser(): Boolean {
        return _numberOfClientsSelected.value?.let { it <= maxNumberOfClientsToInvite } ?: false
    }

    /**
     * Requests repository for resources disposal(/for needed API calls etc.).
     */
    private fun terminateSession(sessionUUID: String) {
        viewModelScope.launch {
            sessionRepository.terminateSession()
        }
    }

    /**
     * Checks if session can be started, (e.g. minimal amount of clients has connected).
     */
    private suspend fun tryMakeSessionReady() {
        if (canSessionBeStarted()) {
            _sessionIsReadyEvent.emit(SessionReadyState(true, null))
        }
    }

    /**
     * If we invite n people and all of n people reject the connection we terminate the session.
     */
    private suspend fun handleNoUsersConnected() {
        _sessionIsReadyEvent.emit(SessionReadyState(
            false,
            R.string.no_users_connected
        ))

        _sessionUUID.value?.let {
            terminateSession(it)
        }
    }

    /**
     * Updates list item state when new user connects to the session.
     */
    private fun updateUserItemStateOnConnectionResult(itemIndex: Int, userAcceptedConnection: Boolean) : Boolean {
        return updateFriendState(
            itemIndex,
            if(userAcceptedConnection) FriendInviteListItem.InvitationState.CONNECTED else FriendInviteListItem.InvitationState.REJECTED
        )
    }

    /**
     * Updates single item state in the friends list
     */
    private fun updateFriendState(itemIndex: Int, newState: FriendInviteListItem.InvitationState) : Boolean {

        return _allFriends.value?.let {

            if (itemIndex >= it.size || itemIndex < 0)
                false

            val newUserData = FriendInviteListItem(
                it[itemIndex].uuid,
                it[itemIndex].thumbnail,
                it[itemIndex].username,
                newState
            )

            it[itemIndex] = newUserData

            true
        } ?: false
    }

    override fun onCleared() {
        webSocketJobs.forEach { it.cancel() }
        webSocketJobs.clear()
        super.onCleared()
    }

    fun clearSessionState() {
        _sessionUUID.value = ""
        _allFriends.value = arrayListOf()
        _numberOfClientsSelected.value = 0
        numberOfClientsInvited = 0
        numberOfClientsRejectedConnection = 0
        numberOfClientsConnectedToTheSession = 0
    }


}