package com.example.matchit.data.model.session

/**
 * Represents the lifecycle state of a session.
 */
enum class SessionState(val value: String)  {
    CREATED("CREATED"),
    RUNNING("RUNNING"),
    MATCHED("MATCHED"),
    BROKEN("BROKEN"),
    FINISHED("FINISHED")
}