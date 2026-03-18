package com.example.matchit.data.app

import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ProcessLifecycleOwner

/**
 * Checks if app is in the foreground(any activity is opened) or background.
 */
object AppVisibilityTracker {

    fun isAppInForeground(): Boolean {
        val state = ProcessLifecycleOwner.get().lifecycle.currentState
        return state.isAtLeast(Lifecycle.State.STARTED)
    }
}
