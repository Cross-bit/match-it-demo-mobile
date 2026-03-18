package com.example.matchit.data.notifications.pushNotifications.model

data class SessionTerminatedDTO(
    val sessionUUID: String,
    val reason: TerminationReason,
    val message: String
) {

    enum class TerminationReason(val value: String) {
        UNEXPECTED("UNEXPECTED"),
        NO_MORE_USERS("NO_MORE_USERS"),
        MATCHING_COMPLETE("MATCHING_COMPLETE"),
        SESSION_CREATION_TIMEOUT("SESSION_CREATION_TIMEOUT"),
        SESSION_MAX_DURATION("SESSION_MAX_DURATION"),
        EXPLICIT("EXPLICIT"),
    }

}