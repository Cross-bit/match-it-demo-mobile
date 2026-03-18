package com.example.matchit.data.session

import com.example.matchit.BuildConfig
import com.example.matchit.data.dataStores.SessionInfoManager
import com.example.matchit.data.model.session.CardData
import com.example.matchit.data.model.session.MatchedItemDTO
import com.example.matchit.data.model.session.MovieCardData
import com.example.matchit.data.model.session.RestaurantCardData
import com.example.matchit.data.model.session.SessionInitCheckDTO
import com.example.matchit.data.model.session.SessionMetadata
import com.example.matchit.data.model.session.SessionState
import com.example.matchit.data.model.session.SessionType
import com.example.matchit.data.model.session.SessionUpdateRequestDTO
import com.example.matchit.data.model.session.UserVotingResult
import com.example.matchit.data.model.session.UsersAvailabilityDTO
import com.example.matchit.data.model.session.UsersAvailabilityRequest
import com.example.matchit.data.remote.client.Resource
import com.example.matchit.data.remote.client.mapToBooleanResource
import com.example.matchit.data.remote.model.SessionApi.CreateSessionApiResponse
import com.example.matchit.data.remote.model.SessionApi.CreateSessionRequest
import com.example.matchit.data.remote.model.SessionApi.MoviesInitSessionDataApiResponse
import com.example.matchit.data.remote.model.SessionApi.SessionStateApiResponse
import com.example.matchit.data.remote.model.SimpleApiResponse
import com.example.matchit.data.session.DataSources.SessionDataSource
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import java.io.IOException
import javax.inject.Inject
import com.example.matchit.data.remote.client.ApiErrors.ApiError
import com.example.matchit.data.remote.model.AssetsApi.CardsDataApiResponse
import com.example.matchit.data.remote.model.SessionApi.SessionSummaryApiResponse
import com.example.matchit.data.remote.model.SessionApi.SessionsHistoryApiResponse
import com.example.matchit.data.websockets.WebsocketManager
import com.example.matchit.data.websockets.WebsocketOutDTO
import com.google.gson.Gson
import kotlinx.coroutines.flow.StateFlow

/**
 * Represents immediate user-initiated actions for session management.
 * These events are triggered immediately on repository call
 * and do not wait for server confirmation.
 *
 */
sealed class SessionAction {
    class InviteRequested() : SessionAction()
    class SessionInviteAcceptRequested() : SessionAction()
    class SessionInviteRejectRequested() : SessionAction()
    class SessionStartRequested() : SessionAction()
    class SessionTerminationRequested() : SessionAction()
    class SessionDisconnectRequested() : SessionAction()
    data class InviteCancelRequested(val sessionId: String) : SessionAction()
}

/**
 * Represents immediate user-initiated actions for session management.
 * These events are triggered immediately on repository call
 * and do not wait for server confirmation.
 *
 * Following events (actions) are specifically intended
 * for the initialisation sessions.
 */
sealed class InitSessionAction {
    class UpdateInitSessionMoviesDataRequested() : InitSessionAction()
    class GetInitSessionMoviesCardsRequested() : InitSessionAction()
}

class SessionRepository @Inject constructor(
    private val sessionInfoManager: SessionInfoManager,
    private val dataSource: SessionDataSource,
    private val webSocketManager: WebsocketManager
) {

    val lastSessionUUID: Flow<String?>
        get() = _lastSessionUUID

    private val _lastSessionUUID = MutableStateFlow<String?>(null)

    private val _sessionActions = MutableSharedFlow<SessionAction>(replay = 0)
    val sessionActions: SharedFlow<SessionAction> get() = _sessionActions.asSharedFlow()

    private val _initSessionActions = MutableSharedFlow<InitSessionAction>(replay = 0)
    val initSessionActions: SharedFlow<InitSessionAction> get() = _initSessionActions.asSharedFlow()

    // informs if user is in a session
    private val _isInSession = MutableStateFlow(false)
    val isInSession: StateFlow<Boolean> = _isInSession

    public fun setIsInSession(value: Boolean) {
        _isInSession.value = value
    }
 
    val lastMatchedMovieCard: Flow<MovieCardData?>
        get() = _lastMatchedMovieCard

    private val _lastMatchedMovieCard = MutableStateFlow<MovieCardData?>(null)

    val lastRestaurantMovieCard: Flow<RestaurantCardData?>
        get() = _lastRestaurantMovieCard

    private val _lastRestaurantMovieCard = MutableStateFlow<RestaurantCardData?>(null)

    val lastlyInvitedMemberUUIDs: Flow<List<String>?>
        get() = _lastlyInvitedMemberUUIDs

    private val _lastlyInvitedMemberUUIDs = MutableStateFlow<List<String>?>(null)

    val lastSessionType: Flow<SessionType?>
        get() = _lastSessionType

    private val _lastSessionType = MutableStateFlow<SessionType?>(null)

    private fun updateLastSessionUUID(sessionUUID: String?) {
        _lastSessionUUID.value = sessionUUID
    }

    fun updateLastMatchedMovieCard(cardData: MovieCardData) {
        _lastMatchedMovieCard.value = cardData
    }

    fun updateLastMatchedRestaurantCard(cardData: RestaurantCardData) {
        _lastRestaurantMovieCard.value = cardData
    }

    private val _matchedItems =
        MutableStateFlow<List<MatchedItemDTO>>(emptyList())

    val matchedItems: StateFlow<List<MatchedItemDTO>> =
        _matchedItems

    fun updateMatchedItems(items: List<MatchedItemDTO>) {
        _matchedItems.value = items
    }

    fun clearMatchedItems() {
        _matchedItems.value = emptyList()
    }

    fun setLastSessionType(sessionType: SessionType) {
        _lastSessionType.value = sessionType;
    }

    /**
     * Requests creation of the new session.
     * If successful returns new session UUID.
     */
    suspend fun createSession(sessionType: SessionType, invitedMembers: List<String>) : Resource<String, ApiError<*>> {
        _sessionActions.emit(SessionAction.InviteRequested())

        val result: Resource<CreateSessionApiResponse, ApiError<*>> = dataSource.createSession(CreateSessionRequest(invitedMembers, sessionType))

        return when (result) {
            is Resource.Success -> {

                _lastlyInvitedMemberUUIDs.value = invitedMembers

                updateLastSessionUUID(result.data.sessionUUID)
                Resource.Success(result.data.sessionUUID)
            }
            is Resource.Error -> Resource.Error(result.errorMessage, result.error)
            is Resource.GeneralError -> Resource.GeneralError(result.message, IOException())
            else -> throw AssertionError()
        }
    }

    suspend fun startSession() : Resource<Boolean, ApiError<*>> {
        _sessionActions.emit(SessionAction.SessionStartRequested())
        return try {
            _lastSessionUUID.value?.let {
                dataSource.startSession(it)
            }?.mapToBooleanResource() ?:
                Resource.GeneralError("Last session UUID was null while trying to start session!", java.lang.Exception())
        }
        catch (e: Exception) {
            Resource.GeneralError("Error occurred while starting session!", e);
        }
    }

    suspend fun terminateSession() : Resource<Boolean, ApiError<*>> {
        _sessionActions.emit(SessionAction.SessionTerminationRequested())
        return try {
            _lastSessionUUID.value?.let {
                dataSource.terminateSession(it)
            }?.mapToBooleanResource() ?:
            Resource.GeneralError("Last session UUID was null while trying to terminate session!", java.lang.Exception())
        }
        catch (e: Exception) {
            Resource.GeneralError("Error occurred while terminating the session!", e);
        }
    }

    suspend fun confirmSessionInvitation(sessionUUID: String) : Resource<Boolean, ApiError<*>> {
        _sessionActions.emit(SessionAction.SessionInviteAcceptRequested())
        updateLastSessionUUID(sessionUUID);
        val result: Resource<SimpleApiResponse, ApiError<*>> = dataSource.confirmSessionInvitation(sessionUUID)

        return result.mapToBooleanResource()
    }

    suspend fun rejectSessionInvitation(sessionUUID: String) : Resource<Boolean, ApiError<*>> {
        _sessionActions.emit(SessionAction.SessionInviteRejectRequested())

        val result: Resource<SimpleApiResponse, ApiError<*>> = dataSource.rejectSessionInvitation(sessionUUID)
        return result.mapToBooleanResource()
    }

    suspend fun disconnectFromSession(sessionUUID: String) : Resource<Boolean, ApiError<*>> {
        _sessionActions.emit(SessionAction.SessionDisconnectRequested())

        val result: Resource<SimpleApiResponse, ApiError<*>> = dataSource.disconnectFromSession(sessionUUID)
        return result.mapToBooleanResource()
    }

    suspend fun updateInitMovieResults(movieRatings: SessionUpdateRequestDTO) : Resource<Boolean, ApiError<*>> {
        _initSessionActions.emit(InitSessionAction.UpdateInitSessionMoviesDataRequested())
        val result: Resource<SimpleApiResponse, ApiError<*>> = dataSource.postMovieInitVotingResults(movieRatings)
        return result.mapToBooleanResource()
    }


    private val cardsDataCache =
        mutableMapOf<SessionType, MutableMap<String, CardData>>()

    suspend fun getSessionCardsData(cardsIds: List<String>, sessionType: SessionType) : Resource<CardsDataApiResponse, ApiError<*>> {
        val sessionCache = cardsDataCache.getOrPut(sessionType) { mutableMapOf() }

        // cached cards
        val cachedCards = cardsIds
            .mapNotNull { id -> sessionCache[id] }

        val missingIds = cardsIds.filterNot { sessionCache.containsKey(it) }

        // if nothing is missing we return all from the cache
        if (missingIds.isEmpty()) {
            return Resource.Success(
                CardsDataApiResponse(sessionType, cachedCards)
            )
        }

        // fetch only missing cards
        val result = dataSource.getSessionCardsData(missingIds, sessionType)

        if (result is Resource.Success) {
            result.data.cards.forEach { card ->
                sessionCache[card.cardId] = card
            }

            // combine cached and remote results
            val allCards = cardsIds.mapNotNull { sessionCache[it] }

            return Resource.Success(
                CardsDataApiResponse(sessionType, allCards)
            )
        }

        return result
    }

    /**
     * Returns all users matched historical sessions
     */
    suspend fun getAllUserHistorySessions() : Resource<List<SessionSummaryApiResponse>, ApiError<*>> {
        return when(val result: Resource<SessionsHistoryApiResponse, ApiError<*>> = dataSource.getAllUserHistorySessions()){
            is Resource.Success -> Resource.Success(result.data.sessions)
            is Resource.Error -> Resource.Error(result.errorMessage, result.error)
            is Resource.GeneralError -> Resource.GeneralError(result.message, result.throwable)
        }
    }

    suspend fun getInitMoviesCards() : Resource<MoviesInitSessionDataApiResponse, ApiError<*>> {
        val result: Resource<MoviesInitSessionDataApiResponse, ApiError<*>> = dataSource.getMovieInitSessionRecommendations()
        return result;
    }

    /**
     * Checks whether user has already initialized session of [sessionType] type
     */
    suspend fun checkSessionIsInitialised(sessionType: SessionType) : Resource<Boolean, ApiError<*>> {
        return when (sessionType) {
            SessionType.MOVIE -> {
                val initRes = dataSource.isSessionInitialised(SessionInitCheckDTO(sessionType))
                return when (initRes) {
                    is Resource.Success -> {
                        sessionInfoManager.setMovieInitialized(true)
                        Resource.Success(initRes.data.result == SimpleApiResponse.Status.OK)
                    }
                    is Resource.GeneralError -> {
                        sessionInfoManager.setMovieInitialized(false)
                        Resource.GeneralError(initRes.message, initRes.throwable)
                    }
                    is Resource.Error -> {
                        sessionInfoManager.setMovieInitialized(false)
                        Resource.Error(initRes.errorMessage, initRes.error)
                    }
                }
            }
            SessionType.RESTAURANT -> {
                sessionInfoManager.setRestaurantInitialized(true)
                Resource.Success(true)
            }
            else -> throw Exception("Invalid session type");
        }
    }

    suspend fun getFriendsAvailability(friendsUUIDs: List<String>) : Resource<UsersAvailabilityDTO, ApiError<*>> {
        val result: Resource<UsersAvailabilityDTO, ApiError<*>> = dataSource.getUsersAvailability(
            UsersAvailabilityRequest(friendsUUIDs)
        )
        return result;
    }

    /**
     * Checks whether sessions with given [sessionUUID] has already started (meaning is in the [SessionState.RUNNING] state)
     */
    suspend fun checkSessionStarted(sessionUUID: String) : Resource<Boolean, ApiError<*>> {
        val result: Resource<SessionStateApiResponse, ApiError<*>> = dataSource.getSessionState(sessionUUID)

        return when (result) {
            is Resource.Success -> Resource.Success(result.data.state == SessionState.RUNNING)
            is Resource.GeneralError -> Resource.GeneralError(result.message, result.throwable)
            is Resource.Error -> Resource.Error(result.errorMessage, result.error)
        }
    }

    /**
     * Checks whether sessions with given [sessionUUID] is in CREATED state (e.i. not started yet)
     */
    suspend fun checkSessionIsCreated(sessionUUID: String) : Resource<Boolean, ApiError<*>> {
        val result: Resource<SessionStateApiResponse, ApiError<*>> = dataSource.getSessionState(sessionUUID)

        return when (result) {
            is Resource.Success -> Resource.Success(result.data.state == SessionState.CREATED)
            is Resource.GeneralError -> Resource.GeneralError(result.message, result.throwable)
            is Resource.Error -> Resource.Error(result.errorMessage, result.error)
        }
    }

    /**
     * In data card
     */

    suspend fun connectToWS() {
        webSocketManager.connect(BuildConfig.DATA_API_WEBSOCKET_URL);
    }

    suspend fun updateSessionVotes(sessionUUID: String, lastVotingResults: List<UserVotingResult>, metadata: SessionMetadata?) {
        val votingResult = SessionUpdateRequestDTO(sessionUUID, lastVotingResults, metadata);

        val votingResultJson = Gson().toJson(votingResult)
        try {
            webSocketManager.sendReliable(WebsocketOutDTO.WsDataType.VOTING_RESULT, votingResultJson)
        }
        catch (err: Exception) {
            throw Exception("Websocket connection error.", err);
        }
    }
}
