package com.example.matchit.data.users

import com.example.matchit.BuildConfig
import com.example.matchit.data.remote.api.UsersApiService
import com.example.matchit.data.remote.client.ApiErrors.ApiError
import com.example.matchit.data.remote.client.AuthenticatedApiCaller
import com.example.matchit.data.remote.client.Resource
import com.example.matchit.data.remote.model.UserProfileApiResponse
import com.example.matchit.data.remote.model.UserProfilesApiResponse
import javax.inject.Inject


class UsersRemoteDataSource @Inject constructor(
    private val apiAuthenticator: AuthenticatedApiCaller
)
{
    suspend fun fetchSingleUserData(userUUID: String) : Resource<UserProfileApiResponse, ApiError<*>> {
        return apiAuthenticator.makeAuthenticatedApiCall(BuildConfig.ACCOUNTS_API_URL)
        { client -> client.create(UsersApiService::class.java).getUser(userUUID)}
    }

    suspend fun fetchUserDataBatch(userUUIDs: List<String>) : Resource<UserProfilesApiResponse, ApiError<*>> {
        return apiAuthenticator.makeAuthenticatedApiCall(BuildConfig.ACCOUNTS_API_URL)
        { client -> client.create(UsersApiService::class.java).getUsers(userUUIDs)}
    }

}