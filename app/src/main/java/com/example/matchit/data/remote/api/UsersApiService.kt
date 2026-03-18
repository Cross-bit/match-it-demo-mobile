package com.example.matchit.data.remote.api


import com.example.matchit.data.remote.model.UserProfileApiResponse
import com.example.matchit.data.remote.model.UserProfilesApiResponse
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query


interface UsersApiService {

    @GET("users/batch")
    suspend fun getUsers(
        @Query("uuids") uuids: List<String>
    ): Response<UserProfilesApiResponse>

    @GET("users/{uuid}")
    suspend fun getUser(
        @Path("uuid") uuid: String
    ): Response<UserProfileApiResponse>
}
