package com.example.matchit.data.session.DataSources

import com.example.matchit.BuildConfig
import com.example.matchit.data.model.session.CardData
import com.example.matchit.data.model.session.SessionInitCheckDTO
import com.example.matchit.data.model.session.SessionType
import com.example.matchit.data.model.session.SessionUpdateRequestDTO
import com.example.matchit.data.model.session.UsersAvailabilityDTO
import com.example.matchit.data.model.session.UsersAvailabilityRequest
import com.example.matchit.data.remote.api.AssetsDataApiService
import com.example.matchit.data.remote.api.SessionApiService
import com.example.matchit.data.remote.client.ApiErrors.ApiError
import com.example.matchit.data.remote.client.AuthenticatedApiCaller
import com.example.matchit.data.remote.client.Resource
import com.example.matchit.data.remote.model.AssetsApi.CardsDataApiResponse
import com.example.matchit.data.remote.model.SessionApi.CreateSessionApiResponse
import com.example.matchit.data.remote.model.SessionApi.CreateSessionRequest
import com.example.matchit.data.remote.model.SessionApi.MoviesInitSessionDataApiResponse
import com.example.matchit.data.remote.model.SessionApi.SessionStateApiResponse
import com.example.matchit.data.remote.model.SessionApi.SessionsHistoryApiResponse
import com.example.matchit.data.remote.model.SimpleApiResponse
import com.example.matchit.data.websockets.CardDataTypeRegistry
import com.google.gson.GsonBuilder
import com.google.gson.JsonDeserializationContext
import com.google.gson.JsonDeserializer
import com.google.gson.JsonElement
import java.io.IOException
import java.lang.reflect.Type
import javax.inject.Inject


class CardsDataApiResponseDeserializer : JsonDeserializer<CardsDataApiResponse> {

    override fun deserialize(
        json: JsonElement,
        typeOfT: Type,
        context: JsonDeserializationContext
    ): CardsDataApiResponse {

        val jsonObject = json.asJsonObject

        val sessionType = SessionType.valueOf(
            jsonObject.get("sessionType").asString
        )

        val cardsType = CardDataTypeRegistry.cardListType(sessionType)

        val cards: List<CardData> =
            context.deserialize(jsonObject.get("cards"), cardsType)

        return CardsDataApiResponse(
            sessionType = sessionType,
            cards = cards
        )
    }
}

class SessionDataSource @Inject constructor(
    private val authenticatedApiCaller: AuthenticatedApiCaller,
)
{

    suspend fun createSession(request: CreateSessionRequest) : Resource<CreateSessionApiResponse, ApiError<*>> {
        return try {
            authenticatedApiCaller.makeAuthenticatedApiCall(BuildConfig.DATA_API_URL) { client ->
                client.create(SessionApiService::class.java).createNewSession(request)
            }
        } catch (e: Throwable) {
            Resource.GeneralError("Error while sending create session request", IOException("Error create session", e))
        }
    }

    suspend fun startSession(sessionUUID: String) : Resource<SimpleApiResponse, ApiError<*>> {
        return try {
            authenticatedApiCaller.makeAuthenticatedApiCall(BuildConfig.DATA_API_URL) { client ->
                client.create(SessionApiService::class.java).sessionStart(sessionUUID)
            }
        } catch (e: Throwable) {
            Resource.GeneralError("Error while sending start session request", IOException("Error start session", e))
        }
    }

    suspend fun terminateSession(sessionUUID: String) : Resource<SimpleApiResponse, ApiError<*>> {
        return try {
            authenticatedApiCaller.makeAuthenticatedApiCall(BuildConfig.DATA_API_URL) {
                it.create(SessionApiService::class.java).terminateSession(sessionUUID)
            }
        } catch (e: Throwable) {
            Resource.GeneralError("Error while sending terminate session request", IOException("Error terminate session", e))
        }
    }

    suspend fun confirmSessionInvitation(sessionUUID: String) : Resource<SimpleApiResponse, ApiError<*>> {
        return try {
            authenticatedApiCaller.makeAuthenticatedApiCall(BuildConfig.DATA_API_URL) {
                it.create(SessionApiService::class.java).connectToSession(sessionUUID)
            }
        } catch (e: Throwable) {
            Resource.GeneralError("Error while sending connect to session request", IOException("Error connect to session", e))
        }
    }

    suspend fun rejectSessionInvitation(sessionUUID: String) : Resource<SimpleApiResponse, ApiError<*>> {
        return try {
            authenticatedApiCaller.makeAuthenticatedApiCall(BuildConfig.DATA_API_URL) {
                it.create(SessionApiService::class.java).rejectConnectionToSession(sessionUUID)
            }
        } catch (e: Throwable) {
            Resource.GeneralError("Error while sending reject session invite request", IOException("Error reject session connection", e))
        }
    }

    suspend fun disconnectFromSession(sessionUUID: String) : Resource<SimpleApiResponse, ApiError<*>>
    {
        return try {
            authenticatedApiCaller.makeAuthenticatedApiCall(BuildConfig.DATA_API_URL) {
                it.create(SessionApiService::class.java).disconnectFromSession(sessionUUID)
            }
        } catch (e: Throwable) {
            Resource.GeneralError("Error while sending connect to session request", IOException("Error connect to session", e))
        }
    }

    /**
     * Fetches session state
     */
    suspend fun getSessionState(sessionUUID: String) : Resource<SessionStateApiResponse, ApiError<*>> {
        return try {
            authenticatedApiCaller.makeAuthenticatedApiCall(BuildConfig.DATA_API_URL) {
                it.create(SessionApiService::class.java).getSessionState(sessionUUID)
            }
        } catch (e: Throwable){
            Resource.GeneralError("Error while getting session state", IOException("Error getting session state", e))
        }
    }

    /**
     * Check session is initialised
     */
    suspend fun isSessionInitialised(sessionInitData: SessionInitCheckDTO) : Resource<SimpleApiResponse, ApiError<*>> {
        return try {
            authenticatedApiCaller.makeAuthenticatedApiCall(BuildConfig.DATA_API_URL) {
                it.create(SessionApiService::class.java).sessionInitCheck(sessionInitData)
            }
        } catch (e: Throwable){
            Resource.GeneralError("Error while getting session state", IOException("Error getting session state", e))
        }
    }

    suspend fun postMovieInitVotingResults(votingResult: SessionUpdateRequestDTO) : Resource<SimpleApiResponse, ApiError<*>>
    {
        return try {
            authenticatedApiCaller.makeAuthenticatedApiCall(BuildConfig.DATA_API_URL) {
                it.create(SessionApiService::class.java).updateInitMovieResults(votingResult)
            }
        } catch (e: Throwable) {
            Resource.GeneralError("Error while sending connect to session request", IOException("Error connect to session", e))
        }
    }

    suspend fun getAllUserHistorySessions() : Resource<SessionsHistoryApiResponse, ApiError<*>> {
        return try {
            authenticatedApiCaller.makeAuthenticatedApiCall(BuildConfig.DATA_API_URL) {
                it.create(SessionApiService::class.java).getUserHistorySessions()
            }
        } catch (e: Throwable) {
            Resource.GeneralError("Error while sending connect to session request", IOException("Error connect to session", e))
        }
    }

    suspend fun getMovieInitSessionRecommendations() : Resource<MoviesInitSessionDataApiResponse, ApiError<*>> {
        return try {
            authenticatedApiCaller.makeAuthenticatedApiCall(BuildConfig.DATA_API_URL) {
                it.create(SessionApiService::class.java).getInitMovieCards()
            }
        } catch (e: Throwable) {
            Resource.GeneralError("Error while requesting movies sessions init info", IOException("Error connect to session", e))
        }
    }

    suspend fun getUsersAvailability(data: UsersAvailabilityRequest) : Resource<UsersAvailabilityDTO, ApiError<*>>
    {
        return try {
            authenticatedApiCaller.makeAuthenticatedApiCall(BuildConfig.DATA_API_URL) {
                it.create(SessionApiService::class.java).getUsersAvailability(data)
            }
        } catch (e: Throwable) {
            Resource.GeneralError("Error while requesting users availability", IOException("Error connect to session", e))
        }
    }

    suspend fun getSessionHistoryChats(data: UsersAvailabilityRequest) : Resource<UsersAvailabilityDTO, ApiError<*>>
    {
        return try {
            authenticatedApiCaller.makeAuthenticatedApiCall(BuildConfig.DATA_API_URL) {
                it.create(SessionApiService::class.java).getUsersAvailability(data)
            }
        } catch (e: Throwable) {
            Resource.GeneralError("Error while requesting users availability", IOException("Error connect to session", e))
        }
    }

    suspend fun getSessionCardsData(cardIds: List<String>, sessionType: SessionType) : Resource<CardsDataApiResponse, ApiError<*>>
    {
        return try {

            val gson = GsonBuilder()
                .registerTypeAdapter(
                    CardsDataApiResponse::class.java,
                    CardsDataApiResponseDeserializer()
                ).create()


            authenticatedApiCaller.makeAuthenticatedApiCall(BuildConfig.DATA_API_URL, gson) { client ->
                client.create(AssetsDataApiService::class.java).getCardsData(cardIds, sessionType)
            }
        } catch (e: Throwable) {
            Resource.GeneralError("Error while requesting cards data", IOException("Error create session", e))
        }
    }
}