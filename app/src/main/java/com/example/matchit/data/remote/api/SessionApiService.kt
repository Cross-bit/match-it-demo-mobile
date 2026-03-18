package com.example.matchit.data.remote.api

import com.example.matchit.data.model.session.SessionInitCheckDTO
import com.example.matchit.data.model.session.SessionUpdateRequestDTO
import com.example.matchit.data.model.session.UsersAvailabilityDTO
import com.example.matchit.data.model.session.UsersAvailabilityRequest
import com.example.matchit.data.remote.model.SessionApi.CreateSessionApiResponse
import com.example.matchit.data.remote.model.SessionApi.CreateSessionRequest
import com.example.matchit.data.remote.model.SessionApi.MatchingResultApiResponse
import com.example.matchit.data.remote.model.SessionApi.MoviesInitSessionDataApiResponse
import com.example.matchit.data.remote.model.SessionApi.SessionStateApiResponse
import com.example.matchit.data.remote.model.SessionApi.SessionsHistoryApiResponse
import com.example.matchit.data.remote.model.SimpleApiResponse
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path


/**
 * Api interface for matching session related tasks
 */
interface SessionApiService {

    @POST("session/create")
    suspend fun createNewSession(@Body request: CreateSessionRequest) : Response<CreateSessionApiResponse>

    @POST("session/terminate/{sessionUUID}")
    suspend fun terminateSession(@Path("sessionUUID") sessionUUID: String) : Response<SimpleApiResponse>

    @POST("session/start/{sessionUUID}")
    suspend fun sessionStart(@Path("sessionUUID") sessionUUID: String) : Response<SimpleApiResponse>

    @POST("session/connect/{sessionUUID}")
    suspend fun connectToSession(@Path("sessionUUID") sessionUUID: String) : Response<SimpleApiResponse>

    @POST("session/disconnect/{sessionUUID}")
    suspend fun disconnectFromSession(@Path("sessionUUID") sessionUUID: String) : Response<SimpleApiResponse>

    @POST("session/reject/{sessionUUID}")
    suspend fun rejectConnectionToSession(@Path("sessionUUID") sessionUUID: String) : Response<SimpleApiResponse>

    @GET("session/sessionState/{sessionUUID}")
    suspend fun getSessionState(@Path("sessionUUID") sessionUUID: String) : Response<SessionStateApiResponse>

    @GET("session/history")
    suspend fun getUserHistorySessions() : Response<SessionsHistoryApiResponse>

    @POST("init/check")
    suspend fun sessionInitCheck(
        @Body initCheckDTO: SessionInitCheckDTO
    ) : Response<SimpleApiResponse>

    @POST("init/movies")
    suspend fun updateInitMovieResults(
        @Body voting: SessionUpdateRequestDTO
    ) : Response<SimpleApiResponse>

    @GET("init/movies")
    suspend fun getInitMovieCards() : Response<MoviesInitSessionDataApiResponse>

    /**
     * Note this is the long pooling endpoint for the next deck of cards for movies
     */
    @POST("movie/next/{sessionUUID}")
    suspend fun nextDeckOfCards(
        @Path("sessionUUID") sessionUUID: String,
        @Body voting: SessionUpdateRequestDTO
    ) : Response<MatchingResultApiResponse>


    // maybe this should be moved into separate interface??

    @POST("users/availability")
    suspend fun getUsersAvailability(@Body request: UsersAvailabilityRequest) : Response<UsersAvailabilityDTO>

}