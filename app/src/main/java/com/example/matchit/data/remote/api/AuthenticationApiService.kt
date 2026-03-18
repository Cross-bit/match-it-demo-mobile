package com.example.matchit.data.remote.api

import com.example.matchit.data.model.authentication.GoogleAuthRequest
import com.example.matchit.data.remote.model.AuthenticationApi.UserLoginRequestData
import com.example.matchit.data.model.authentication.UserRegistrationData
import com.example.matchit.data.remote.model.AuthenticationApi.NewTokenApiResponse
import com.example.matchit.data.remote.model.AuthenticationApi.RefreshTokenApiRequest
import com.example.matchit.data.remote.model.AuthenticationApi.UserLoggedInApiResponse
import com.example.matchit.data.remote.model.AuthenticationApi.UserRegistrationApiResponse
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST

/**
 * Api interface for users authentication and account management(creation/...)
 */
interface AuthenticationApiService {

    @POST("credentials/login")
    suspend fun loginUser(
        @Body user: UserLoginRequestData
    ): Response<UserLoggedInApiResponse>

    @POST("credentials/create")
    suspend fun signupUser(
        @Body user: UserRegistrationData
    ): Response<UserRegistrationApiResponse>

    @POST("googleAuthentication/create")
    suspend fun signupGoogleUser(
        @Body user: GoogleAuthRequest
    ): Response<UserRegistrationApiResponse>

    @GET("authenticate")
    suspend fun authenticate()

    @POST("refresh")
    suspend fun refreshAccessToken(
        @Body refreshToken: RefreshTokenApiRequest
    ): Response<NewTokenApiResponse>
}