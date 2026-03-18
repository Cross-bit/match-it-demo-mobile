package com.example.matchit.data.model.session

/**
 * Global enum for all types of sessions.
 * Add new entries when introducing additional session types.
 */
enum class SessionType(val value: String) {
    MOVIE("MOVIE"),
    RESTAURANT("RESTAURANT")
}