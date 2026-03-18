package com.example.matchit.ui.matchingSession.common

import kotlinx.coroutines.flow.StateFlow


interface SessionForm {
    fun isValid(): Boolean
    val isValidFlow: StateFlow<Boolean>
    fun recoverIfNeeded()
    fun createSnapshot()
}