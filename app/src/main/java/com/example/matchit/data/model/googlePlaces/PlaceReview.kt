package com.example.matchit.data.model.googlePlaces

import android.os.Parcelable


@kotlinx.parcelize.Parcelize
data class PlaceReview(
    val name: String,
    val rating: Float,
    val text: String,
    val publishTime: String,
    val authorAttribution: AuthorAttributions,
) : Parcelable