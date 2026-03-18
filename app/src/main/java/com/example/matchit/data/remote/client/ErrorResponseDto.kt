package com.example.matchit.data.remote.client

import com.google.gson.annotations.SerializedName

data class ErrorResponseDto(
    @SerializedName("name")
    val name: String?,

    @SerializedName("message")
    val message: String?,

    @SerializedName("status")
    val status: Int?
)