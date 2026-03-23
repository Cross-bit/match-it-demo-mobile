package com.example.matchit.ui.socialConnections.peopleSearch

data class SearchPersonItem(
    val userUUID: String,
    val thumbnail: String,
    val username: String,
    val isFriend: Boolean,
    val hasInvitation: Boolean
)
