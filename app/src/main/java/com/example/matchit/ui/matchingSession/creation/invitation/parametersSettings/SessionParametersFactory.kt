package com.example.matchit.ui.matchingSession.creation.invitation.parametersSettings

import com.example.matchit.data.model.session.AlgorithmType
import com.example.matchit.data.model.session.SessionParameters
import com.example.matchit.data.model.session.SessionType

/**
 * Central object with default session parameters settings
 */
object SessionParametersFactory {
    fun createDefault(sessionType: SessionType): SessionParameters {
        return when (sessionType) {

            SessionType.RESTAURANT ->
                SessionParameters.RestaurantParameters(
                    searchCity = "Prague",
                    restaurantPriceRange = 3f,
                    selectedCategories = emptySet(),
                    algorithm = AlgorithmType.SYNC
                )

            SessionType.MOVIE ->
                SessionParameters.MovieParameters(
                    algorithm = AlgorithmType.ASYNC
                )
        }
    }
}