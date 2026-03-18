package com.example.matchit.ui.matchingSession.matching

import com.example.matchit.data.model.session.MovieCardData
import com.example.matchit.data.model.session.SessionType

data class MatchingEndResult(
    val sessionType: SessionType? = null,
    val error: Int? = null
)
