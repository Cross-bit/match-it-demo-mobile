package com.example.matchit.ui.matchingSession.matching.cards.movies

import org.junit.Assert.assertEquals
import org.junit.Test

class MovieFormatUtilsTest {

    @Test
    fun formatRating_returns_default_when_null() {
        assertEquals("5", MovieFormatUtils.formatRating(null))
    }

    @Test
    fun formatRating_rounds_to_one_decimal() {
        assertEquals("4.3", MovieFormatUtils.formatRating(4.26f))
    }

    @Test
    fun formatRating_handles_zero() {
        assertEquals("0.0", MovieFormatUtils.formatRating(0f))
    }
}
