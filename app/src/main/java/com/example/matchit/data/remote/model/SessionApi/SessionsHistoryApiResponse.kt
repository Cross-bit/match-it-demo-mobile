package com.example.matchit.data.remote.model.SessionApi
import com.example.matchit.data.model.session.CardData
import com.example.matchit.data.model.session.SessionState
import com.example.matchit.data.model.session.SessionType


data class SessionSummaryApiResponse (
    val sessionUUID: String,
    val sessionType: SessionType,
    val state: SessionState,
    val createdAt: Long,
    val size: Int,
    val users: List<String>,
    val isCreator: Boolean
)

data class SessionsHistoryApiResponse (
    val sessions: List<SessionSummaryApiResponse>
)