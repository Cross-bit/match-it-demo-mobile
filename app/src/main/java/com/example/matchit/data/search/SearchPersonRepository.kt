package com.example.matchit.data.search

import com.example.matchit.data.remote.client.ApiErrors.ApiError
import com.example.matchit.data.remote.client.Resource
import com.example.matchit.data.remote.model.FriendsApi.PersonFetchApiResponse
import javax.inject.Inject

interface SearchPersonRepository {
    suspend fun searchPerson(email: String) : Resource<PersonFetchApiResponse, ApiError<*>>?
    suspend fun sendFriendRequest(userId: String) : Resource<Unit, ApiError<*>>?
}

class SearchPersonRepositoryImpl @Inject constructor(
    private val searchPersonRemoteDataSource: SearchPersonDataSource
) : SearchPersonRepository {
    override suspend fun searchPerson(email: String) : Resource<PersonFetchApiResponse, ApiError<*>>? {
       return searchPersonRemoteDataSource.searchPersonRequest(email);
    }

    override suspend fun sendFriendRequest(userId: String): Resource<Unit, ApiError<*>> {
        return searchPersonRemoteDataSource.sendFriendRequest(userId);
    }
}