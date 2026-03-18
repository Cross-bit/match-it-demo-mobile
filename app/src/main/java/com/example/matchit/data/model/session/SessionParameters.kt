package com.example.matchit.data.model.session

enum class AlgorithmType(val value: String) {
    NONE("NONE"), // this is actually important state, it is required so the backend knows whose algorithm settings to choose (only the creator has non NONE value)
    SYNC("SYNC"),
    ASYNC("ASYNC"),
    HYBRID("HYBRID")
}

/**
 * User specific session settings.
 */
sealed class SessionParameters {
    data class
    RestaurantParameters(
        val searchCity: String,
        val restaurantPriceRange: Float,
        val selectedCategories: Set<Category>,
        val algorithm: AlgorithmType = AlgorithmType.NONE
    ) : SessionParameters() {
        enum class Category {
            VEGETARIAN_ONLY,
            BARS,
            BUFFET,
            CAFES,
            TEAS,
            SWEETS_DESSERTS,
            EUROPEAN,
            FAST_FOOD,
            TAKEAWAY,
            FINE_DINING,
            LATIN_AMERICAN,
            MIDDLE_EASTERN,
            ASIAN,
            INDIAN,
            HEALTHY
        }
    }
    data class MovieParameters(
        val algorithm: AlgorithmType = AlgorithmType.NONE
    ) : SessionParameters()
}