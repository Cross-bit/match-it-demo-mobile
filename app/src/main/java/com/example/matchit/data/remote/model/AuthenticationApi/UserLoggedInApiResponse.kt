package com.example.matchit.data.remote.model.AuthenticationApi

import com.example.matchit.data.model.authentication.LoggedInUser

data class UserLoggedInApiResponse(
    val name: String,
    val result: String,
    val uuid: String,
    val accessRights: String,
    val authenticationMethod: String,
    val accessToken: String,
    val refreshToken: String,
    val isRefresh: String,
    val preferences: UserPreferences
){
    enum class TaskStatus {
        ACTIVE,
        COMPLETE
    }
}

data class UserPreferences(val profilePictureUrl: String)

/**
 * Performs mapping of UserLoggedInApiResponse received from api to local data model
 */
fun UserLoggedInApiResponse.toExternal() = LoggedInUser(
    displayName = name,
    email = "johnsmail@gmail.com", // TODO: remove or resolve... :)
    uuid = uuid
)