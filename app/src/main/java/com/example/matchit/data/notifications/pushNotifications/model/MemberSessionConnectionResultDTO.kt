package com.example.matchit.data.notifications.pushNotifications.model

class MemberSessionConnectionResultDTO(
    val result: ConnectionResult,
    val userUUID: String
){
    enum class ConnectionResult(val result: String) {
        CONNECTED("CONNECTED"),
        REJECTED("REJECTED")
    }
}