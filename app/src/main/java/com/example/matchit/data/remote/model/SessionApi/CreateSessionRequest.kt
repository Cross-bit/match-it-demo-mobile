package com.example.matchit.data.remote.model.SessionApi

import com.example.matchit.data.model.session.SessionType

data class CreateSessionRequest(
    val invitedMemberUUIDs: List<String>,
    val sessionType: SessionType
);