package com.example.matchit.ui.matchingSession.creation.invitation.parametersSettings

import com.example.matchit.data.model.session.AlgorithmType
import com.example.matchit.data.model.session.SessionParameters
import com.example.matchit.data.model.session.SessionType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SessionParametersFactoryTest {

    @Test
    fun createDefault_returns_movie_defaults() {
        val result = SessionParametersFactory.createDefault(SessionType.MOVIE)
        assertTrue(result is SessionParameters.MovieParameters)
        assertEquals(AlgorithmType.NONE, (result as SessionParameters.MovieParameters).algorithm)
    }

    @Test
    fun createDefault_returns_restaurant_defaults() {
        val result = SessionParametersFactory.createDefault(SessionType.RESTAURANT)
        assertTrue(result is SessionParameters.RestaurantParameters)

        val restaurant = result as SessionParameters.RestaurantParameters
        assertEquals("Prague", restaurant.searchCity)
        assertEquals(3f, restaurant.restaurantPriceRange)
        assertEquals(AlgorithmType.NONE, restaurant.algorithm)
        assertTrue(restaurant.selectedCategories.isEmpty())
    }
}
