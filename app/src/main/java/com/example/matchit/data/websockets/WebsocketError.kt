package com.example.matchit.data.websockets


/**
 * Error object retrieved on websocket error fail (e.g. authentication error etc...)
 */
data class WebsocketError(
    val name: String,
    val message: String,
    val status: String,
);
