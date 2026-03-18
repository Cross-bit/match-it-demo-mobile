package com.example.matchit.data.remote.model

data class UserProfileApiResponse(
    val uuid: String,
    val name: String?,
    val avatarUrl: String?,
)

data class UserProfilesApiResponse(
    val profiles: List<UserProfileApiResponse>
)
