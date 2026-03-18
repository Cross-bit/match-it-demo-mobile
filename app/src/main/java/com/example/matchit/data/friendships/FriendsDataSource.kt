package com.example.matchit.data.friendships


import com.example.matchit.BuildConfig
import com.example.matchit.data.remote.api.FriendsDataApiService
import com.example.matchit.data.remote.client.ApiErrors.ApiError
import com.example.matchit.data.remote.client.AuthenticatedApiCaller
import com.example.matchit.data.remote.client.RequestClientFactory
import com.example.matchit.data.remote.client.Resource
import com.example.matchit.data.remote.model.FriendsApi.AdmitFriendRequestApiResponse
import com.example.matchit.data.remote.model.FriendsApi.FetchAllFriendshipCreationRequestsApiResponse
import com.example.matchit.data.remote.model.FriendsApi.FetchAllFriendsApiResponse
import java.io.IOException
import javax.inject.Inject


class FriendsDataSource @Inject constructor(
    private val apiAuthenticator: AuthenticatedApiCaller,
    private val factory: RequestClientFactory
)
{
    suspend fun fetchAllFriends() : Resource<FetchAllFriendsApiResponse, ApiError<*>> {
        return apiAuthenticator.makeAuthenticatedApiCall(BuildConfig.FRIENDSHIPS_API_URL)
        { client -> client.create(FriendsDataApiService::class.java).getAllFriends() }
    }

    suspend fun fetchAllFriendshipCreationRequests() : Resource<FetchAllFriendshipCreationRequestsApiResponse, ApiError<*>> {
        return apiAuthenticator.makeAuthenticatedApiCall(BuildConfig.FRIENDSHIPS_API_URL)
        { client -> client.create(FriendsDataApiService::class.java).getAllFriendRequests() }
    }

    suspend fun admitFriendshipCreationRequest(requestId: String) : Resource<Unit, ApiError<*>> {
        return try {
            return apiAuthenticator.makeAuthenticatedApiCall(BuildConfig.FRIENDSHIPS_API_URL) { client ->
                val service = client.create(FriendsDataApiService::class.java)
                service.admitFriendRequest(AdmitFriendRequestApiResponse(requestId))
            }
        } catch (e: Throwable) {
            Resource.GeneralError("Error while admitting friend request", IOException("Error admitting friend request", e))
        }
    }
}
