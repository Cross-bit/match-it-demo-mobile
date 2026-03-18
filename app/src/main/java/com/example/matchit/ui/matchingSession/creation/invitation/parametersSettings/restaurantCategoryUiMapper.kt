package com.example.matchit.ui.matchingSession.creation.invitation.parametersSettings

import com.example.matchit.R
import com.example.matchit.data.model.session.SessionParameters


/**
 * Extension function mapping a [SessionParameters.RestaurantParameters.Category]
 * to a string resource ID used for checkbox labels in the UI.
 */
fun SessionParameters.RestaurantParameters.Category.toResId(): Int = when (this) {
    SessionParameters.RestaurantParameters.Category.VEGETARIAN_ONLY -> R.string.vegetarian_only_text
    SessionParameters.RestaurantParameters.Category.BARS -> R.string.bars_text
    SessionParameters.RestaurantParameters.Category.BUFFET -> R.string.buffet_text
    SessionParameters.RestaurantParameters.Category.CAFES -> R.string.cafes_text
    SessionParameters.RestaurantParameters.Category.TEAS -> R.string.teas_text
    SessionParameters.RestaurantParameters.Category.SWEETS_DESSERTS -> R.string.sweets_desserts_text
    SessionParameters.RestaurantParameters.Category.EUROPEAN -> R.string.european_text
    SessionParameters.RestaurantParameters.Category.FAST_FOOD -> R.string.fast_food_text
    SessionParameters.RestaurantParameters.Category.TAKEAWAY -> R.string.takeaway_text
    SessionParameters.RestaurantParameters.Category.FINE_DINING -> R.string.fine_dining_text
    SessionParameters.RestaurantParameters.Category.LATIN_AMERICAN -> R.string.latin_american_text
    SessionParameters.RestaurantParameters.Category.MIDDLE_EASTERN -> R.string.middle_eastern_text
    SessionParameters.RestaurantParameters.Category.INDIAN -> R.string.south_asian_text
    SessionParameters.RestaurantParameters.Category.ASIAN -> R.string.asian_text
    SessionParameters.RestaurantParameters.Category.HEALTHY -> R.string.healthy_text
}