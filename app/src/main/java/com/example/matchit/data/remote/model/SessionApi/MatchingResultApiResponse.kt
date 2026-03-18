package com.example.matchit.data.remote.model.SessionApi

import com.example.matchit.data.model.session.MovieCardData


/**
 * Supposes to be retrieved by recommendation data Api once we notify service that last deck of cards was completed.
 *
 * @property nextDeckOfCards The next deck of cards to recommend.
 * @property matched Indicates whether a match was found.
 * @property matchedItemID The id of the matched item (if matched == True, else empty string).
 */
data class MatchingResultApiResponse(
    val matched: Boolean,
    val nextDeckOfCards: List<MovieCardData>,
    val matchedItemUUID: String // TODO: this is actually jst internal item Id not UUID... change it (has to be changed also in the backend)
)