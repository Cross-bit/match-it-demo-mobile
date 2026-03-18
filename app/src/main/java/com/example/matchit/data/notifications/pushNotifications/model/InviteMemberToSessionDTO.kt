package com.example.matchit.data.notifications.pushNotifications.model

import com.example.matchit.data.model.session.SessionCreator
import com.example.matchit.data.model.session.SessionState
import com.example.matchit.data.model.session.SessionType


data class InviteMemberToSessionDTO(
    val sessionUUID: String,
    val sessionCreator: SessionCreator,
    val sessionType: SessionType
)