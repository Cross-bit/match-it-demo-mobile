package com.example.matchit.data.search


import com.example.matchit.BuildConfig
import com.example.matchit.data.remote.api.PeopleSearchApiService
import com.example.matchit.data.remote.client.ApiErrors.ApiError
import com.example.matchit.data.remote.client.AuthenticatedApiCaller
import com.example.matchit.data.remote.client.LoginSessionManager
import com.example.matchit.data.remote.client.RequestClientFactory
import com.example.matchit.data.remote.client.Resource
import com.example.matchit.data.remote.model.FriendsApi.CreateFriendRequest
import com.example.matchit.data.remote.model.FriendsApi.PersonFetchApiResponse
import java.io.IOException
import javax.inject.Inject


class SearchPersonDataSource @Inject constructor(
    private val loginSessionManager: LoginSessionManager,
    private val reAuthenticationHandler: AuthenticatedApiCaller,
    private val factory: RequestClientFactory
)
{
    suspend fun searchPersonRequest(email: String) : Resource<PersonFetchApiResponse, ApiError<*>>? {
        return try {
            reAuthenticationHandler.makeAuthenticatedApiCall(BuildConfig.FRIENDSHIPS_API_URL) { client ->
                client.create(PeopleSearchApiService::class.java).searchPerson(email)
            }
        } catch (e: Throwable) {
            Resource.GeneralError("Error while fetching all searched people", IOException("Error logging in", e))
        }
    }

    suspend fun sendFriendRequest(userId: String): Resource<Unit, ApiError<*>> {
        return try {
            reAuthenticationHandler.makeAuthenticatedApiCall(BuildConfig.FRIENDSHIPS_API_URL) { client ->
                client.create(PeopleSearchApiService::class.java).sendFriendRequest(CreateFriendRequest(userId))
            }
        } catch (e: Throwable) {
            Resource.GeneralError("Error while sending friend request", IOException("Error sending friend request", e))
        }
    }
}
