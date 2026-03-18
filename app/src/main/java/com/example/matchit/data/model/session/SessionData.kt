package com.example.matchit.data.model.session

data class SessionData(
    val sessionUUID: String,
    val role: SessionRole,
    val sessionCreator: SessionCreator,
    val sessionType: SessionType

)
{
    enum class SessionRole(val value: String) {
        CREATOR("CREATOR"), // we are also creator of the session
        GUEST("GUEST") // if we jst connected to the session
    }
}