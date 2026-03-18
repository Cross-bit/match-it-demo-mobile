package com.example.matchit.data.remote.api

import com.example.matchit.data.remote.model.FriendsApi.AdmitFriendRequestApiResponse
import com.example.matchit.data.remote.model.FriendsApi.FetchAllFriendshipCreationRequestsApiResponse
import com.example.matchit.data.remote.model.FriendsApi.FetchAllFriendsApiResponse
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST

/**
 * Api interface for friends and social connections
 */
interface FriendsDataApiService {
    @GET("friends")
    suspend fun getAllFriends() : Response<FetchAllFriendsApiResponse>

    @GET("friends-requests/check")
    suspend fun getAllFriendRequests() : Response<FetchAllFriendshipCreationRequestsApiResponse>

    @POST("friends-requests/admit")
    suspend fun admitFriendRequest(@Body request: AdmitFriendRequestApiResponse) : Response<Unit>
}