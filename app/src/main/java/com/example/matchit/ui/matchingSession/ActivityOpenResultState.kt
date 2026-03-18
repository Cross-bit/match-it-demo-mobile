package com.example.matchit.ui.matchingSession

import com.example.matchit.data.model.session.SessionType

/**
 * State of the sessions activity start. If the matching activity is not open, then
 */
data class ActivityOpenResultState(
    val result: ResultState,
    val sessionType: SessionType,
    val error: Int? = null
){
    public enum class ResultState(val value: String)
    {
        OK("OK"),
        NOT_INITIALISED("NOT_INITIALISED"),
        ERROR("ERROR")
    }
}
