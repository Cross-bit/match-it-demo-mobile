package com.example.matchit.data.users

import com.example.matchit.data.remote.api.UsersApiService
import com.example.matchit.data.remote.client.ApiErrors.ApiError
import com.example.matchit.data.remote.client.Resource
import com.example.matchit.data.remote.model.UserProfileApiResponse
import javax.inject.Inject
import javax.inject.Singleton


/**
 * Manages all the public metadata about all the users seen by the app .
 */
@Singleton
class UsersRepository @Inject constructor(
    private val usersRemoteDataSource: UsersRemoteDataSource
) {

    // Simple in-memory cache
    private val memoryCache = mutableMapOf<String, UserProfileApiResponse>()

    /**
     * Fetches public metadata of a single user by UUID.
     * Uses memory cache first, then remote if needed.
     */
    suspend fun fetchUserData(uuid: String): Resource<UserProfileApiResponse, ApiError<*>> {

        memoryCache[uuid]?.let { cached ->
            return Resource.Success(cached)
        }


        return when (val response = usersRemoteDataSource.fetchSingleUserData(uuid)) {
            is Resource.Success -> {
                Resource.Success(response.data)
            }

            is Resource.Error -> response
            is Resource.GeneralError -> response
        }
    }

    /**
     * Fetches public metadata for a list of users by UUIDs.
     * Cache is applied per-user. Remote is called only for missing entries.
     */
    suspend fun fetchUserDataBatch(uuids: List<String>): Resource<List<UserProfileApiResponse>, ApiError<*>> {

        // If the list is empty, return success with empty list.
        if (uuids.isEmpty()) {
            return Resource.Success(emptyList())
        }

        val resultList = mutableListOf<UserProfileApiResponse>()
        val missingUUIDs = mutableListOf<String>()

        // Split cached vs non-cached UUIDs
        uuids.forEach { uuid ->
            val cached = memoryCache[uuid]
            if (cached != null) resultList.add(cached)
            else missingUUIDs.add(uuid)
        }

        // everything was in the cache
        if (missingUUIDs.isEmpty())
            return Resource.Success(resultList)

        // Fetch the rest
        return when (val response = usersRemoteDataSource.fetchUserDataBatch(missingUUIDs)) {

            is Resource.Success -> {

                val profiles = response.data.profiles

                profiles.forEach { profile ->
                    memoryCache[profile.uuid] = profile
                    resultList.add(profile)
                }

                Resource.Success(resultList)
            }

            is Resource.Error -> response
            is Resource.GeneralError -> response
        }
    }
}
