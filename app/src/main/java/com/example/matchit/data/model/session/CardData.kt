package com.example.matchit.data.model.session

import com.google.gson.annotations.SerializedName

abstract class CardData(
    @SerializedName("cardId") open val cardId: String
)
