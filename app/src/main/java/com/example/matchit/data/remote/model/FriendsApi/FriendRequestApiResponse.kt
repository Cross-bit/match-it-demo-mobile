package com.example.matchit.data.remote.model.FriendsApi


/**
 * Supposes to be retrieved by friends Api after friend request was send.
 */
data class FriendRequestApiResponse(
    val requestId: String,
    val friendData: FriendRequestFriendDataApiResponse
)

data class FriendRequestFriendDataApiResponse(
    val uuid: String,
    val name: String,
    val email: String
)
