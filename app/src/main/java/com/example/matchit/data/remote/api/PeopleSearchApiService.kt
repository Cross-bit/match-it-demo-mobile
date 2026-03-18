package com.example.matchit.data.remote.api

import com.example.matchit.data.remote.model.FriendsApi.CreateFriendRequest
import com.example.matchit.data.remote.model.FriendsApi.PersonFetchApiResponse
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path

/**
 * Api interface for people search
 */
interface PeopleSearchApiService {

    /**
     * Performs a search for a friend.
     *
     * @param id The ID of the user who is performing the search.
     * @param email The email of the friend to search for.
     * @return A Response containing the FriendSearchApiResponse.
     */
    @GET("search/{email}")
    suspend fun searchPerson(@Path("email") email: String) : Response<PersonFetchApiResponse>

    @POST("friends-requests/send")
    suspend fun sendFriendRequest(@Body request: CreateFriendRequest): Response<Unit>
}