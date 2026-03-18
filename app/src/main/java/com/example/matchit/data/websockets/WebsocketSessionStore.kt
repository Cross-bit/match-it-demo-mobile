package com.example.matchit.data.websockets

interface WebsocketSessionStore {

    /**
     * Returns an existing websocket session ID,
     * or creates a new one if it does not exist.
     *
     * @return session identifier string
     */
    suspend fun getOrCreateSessionId(): String

    /**
     * Returns the current sequence number and increments it.
     *
     * @return previous sequence value before increment
     */
    suspend fun nextSequence(): Long

    /**
     * Clears the stored websocket session data,
     * including session ID and sequence counter.
     */
    suspend fun reset()
}