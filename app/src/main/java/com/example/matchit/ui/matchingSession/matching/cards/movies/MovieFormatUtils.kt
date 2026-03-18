package com.example.matchit.ui.matchingSession.matching.cards.movies

import java.util.Locale

object MovieFormatUtils {

    fun formatRating(rating: Float?): String {
        return rating?.let { String.format(Locale.US, "%.1f", it) } ?: "5"
    }
}