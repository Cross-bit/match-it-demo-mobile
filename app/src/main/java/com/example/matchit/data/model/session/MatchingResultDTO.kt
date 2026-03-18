package com.example.matchit.data.model.session

/**
 * Information about one of the matched items
 */
data class MatchedItemDTO(
    val itemId: String,
    val rank: Int,
    val run: Int,
    val score: Double,
    val cardData: CardData? = null
)

/**
 * Result of the last deck of cards -- if matched [matchedItems] is not null
 * otherwise [nextDeckOfCards] contains next set of cards for matching
 */
data class MatchingResultDTO(
    val matched: Boolean,
    val nextDeckOfCards: List<CardData>,
    val matchedItems: List<MatchedItemDTO>? = null,
    val matchedItemUUID: String,
    val sessionType: SessionType
)