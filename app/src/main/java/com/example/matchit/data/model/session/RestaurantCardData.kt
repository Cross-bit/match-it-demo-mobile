package com.example.matchit.data.model.session
import android.os.Parcelable
import com.example.matchit.data.model.googlePlaces.PlacePhoto
import com.example.matchit.data.model.googlePlaces.PlaceReview


@kotlinx.parcelize.Parcelize
data class OpeningDayUi(
    val day: Int,              // 0 = Sunday ... 6 = Saturday
    val dayName: String,        // "Monday"
    val openTime: String?,      // "10:00"
    val closeTime: String?,     // "20:00"
) : Parcelable

@kotlinx.parcelize.Parcelize
data class OpeningHoursUi(
    val nextOpenTime: String?,      // ISO string
    val week: List<OpeningDayUi>
) : Parcelable

/**
 * Represents data model for restaurant card item
 */
@kotlinx.parcelize.Parcelize
data class RestaurantCardData(
    val restaurantId: String,
    val placeUrl: String,
    val title: String,
    val placePhotos: List<PlacePhoto>,
    val locationAddress: String,
    val location: GpsCoordinates,
    val type: List<String>,
    val rating: Float,
    val priceLevel: Int,
    val delivery: Boolean,
    val outdoorSeating: Boolean,
    val goodForGroups: Boolean,
    val takeout: Boolean,
    val servesVegetarian: Boolean,
    val placeReviews: List<PlaceReview>,
    val openingHours: OpeningHoursUi?
) : CardData(cardId = ""), Parcelable





