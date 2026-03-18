package com.example.matchit.data.friendships

import com.example.matchit.data.local.db.dao.FriendsDao
import com.example.matchit.data.local.db.entities.FriendEntity
import com.example.matchit.data.remote.client.ApiErrors.ApiError
import com.example.matchit.data.remote.client.Resource
import com.example.matchit.data.remote.model.FriendsApi.FetchAllFriendshipCreationRequestsApiResponse
import com.example.matchit.data.remote.model.FriendsApi.FetchAllFriendsApiResponse
import com.example.matchit.data.remote.model.FriendsApi.FetchFriendApiResponse
import javax.inject.Inject

interface FriendsRepository {
    suspend fun getAllFriends(forceRefresh: Boolean = true) : Resource<FetchAllFriendsApiResponse, ApiError<*>>?

    suspend fun getAllFriendshipCreationRequests(): Resource<FetchAllFriendshipCreationRequestsApiResponse, ApiError<*>>?

    suspend fun admitFriendshipCreationRequest(requestId: String): Resource<Unit, ApiError<*>>?

    suspend fun getFriendFromCache(uuid: String): FetchFriendApiResponse?

    suspend fun clearCache()
}

class FriendsRepositoryImpl @Inject constructor(
    private val friendsRemoteDataSource: FriendsDataSource,
    private val friendsDao: FriendsDao,
) : FriendsRepository {

    override suspend fun getAllFriends(forceRefresh: Boolean): Resource<FetchAllFriendsApiResponse, ApiError<*>>? {

        if (!forceRefresh) {
            val localFriends = friendsDao.getAllFriends()
            if (localFriends.isNotEmpty()) {
                return Resource.Success(
                    FetchAllFriendsApiResponse(
                        friends = localFriends.map { it.toApiModel() }
                    )
                )
            }
        }

        return when (val response = friendsRemoteDataSource.fetchAllFriends()) {
            is Resource.Success -> {
                val fetchedFriends = response.data.friends
                friendsDao.deleteAllFriends()
                friendsDao.insertAllFriends(fetchedFriends.map { it.toEntity() })
                response
            }
            is Resource.Error -> response
            is Resource.GeneralError -> response
        }
    }


    override suspend fun getFriendFromCache(uuid: String): FetchFriendApiResponse? {
        return friendsDao.getFriendByUUID(uuid)?.toApiModel()
    }

    override suspend fun clearCache() {
        friendsDao.deleteAllFriends()
    }

    /**
     * Friendship creation
     * */

    override suspend fun getAllFriendshipCreationRequests(): Resource<FetchAllFriendshipCreationRequestsApiResponse, ApiError<*>>? {
        return friendsRemoteDataSource.fetchAllFriendshipCreationRequests()
    }

    override suspend fun admitFriendshipCreationRequest(requestId: String): Resource<Unit, ApiError<*>> {
        return friendsRemoteDataSource.admitFriendshipCreationRequest(requestId)
    }
}


fun FetchFriendApiResponse.toEntity(): FriendEntity {
    return FriendEntity(
        uuid = uuid,
        name = name,
        email = email,
        profilePic = profilePicUrl
    )
}

fun FriendEntity.toApiModel(): FetchFriendApiResponse {
    return FetchFriendApiResponse(
        uuid = uuid,
        name = name,
        email = email,
        profilePicUrl = profilePic
    )
}