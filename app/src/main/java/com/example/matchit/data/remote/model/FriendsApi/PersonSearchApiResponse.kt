package com.example.matchit.data.remote.model.FriendsApi


/**
 * Supposes to be retrieved by friends Api (search endpoint) on successful person search.
 */
data class PersonSearchApiResponse(
    val uuid: String,
    val name: String,
    val email: String,
    val isFriend: Boolean,
    val hasInvitation: Boolean
)
