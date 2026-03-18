package com.example.matchit.data.remote.model.FriendsApi



/**
 * Supposes to be retrieved by friends Api after fetch all friends request.
 */
data class FetchAllFriendsApiResponse(val friends: List<FetchFriendApiResponse>)


data class FetchFriendApiResponse(
    val uuid: String,
    val name: String,
    val email: String,
    val profilePicUrl: String
)