package com.example.matchit.data.remote.api

import com.example.matchit.data.remote.model.AccountApi.ProfilePicApiResponse
import com.example.matchit.data.remote.model.AuthenticationApi.UserLoggedInApiResponse
import com.example.matchit.data.remote.model.AuthenticationApi.UserLoginRequestData
import com.example.matchit.data.remote.model.SimpleApiResponse
import okhttp3.MultipartBody
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.Part


interface AccountApiService {

    @Multipart
    @POST("preferences/profilePic/upload")
    suspend fun uploadUserProfilePicture(
     //   @Part("name") name: RequestBody,
        @Part image: MultipartBody.Part
    ): Response<ProfilePicApiResponse>

    @GET("preferences/profilePic")
    suspend fun getUserProfilePicture(): Response<ProfilePicApiResponse>

    @DELETE("account/logout")
    suspend fun logoutUser(): Response<SimpleApiResponse>

    @DELETE("account/delete")
    suspend fun deleteUser(): Response<SimpleApiResponse>
}