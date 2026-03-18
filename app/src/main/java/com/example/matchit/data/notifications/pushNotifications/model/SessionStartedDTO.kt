package com.example.matchit.data.notifications.pushNotifications.model

import com.example.matchit.data.model.session.SessionType

data class MemberData(
    val uuid: String,
    val isCreator: Boolean
)

data class SessionInfo (
    val sessionUUID: String,
    val sessionSize: Int,
    val sessionType: SessionType,
    val members: List<MemberData>
)

data class SessionStartedDTO(
    val sessionInfo: SessionInfo
)