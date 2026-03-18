package com.example.matchit.data.remote.model.AssetsApi

import com.example.matchit.data.model.session.CardData
import com.example.matchit.data.model.session.SessionType

data class CardsDataApiResponse (
    val sessionType: SessionType,
    val cards: List<CardData>
)