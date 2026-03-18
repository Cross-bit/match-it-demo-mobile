package com.example.matchit.data.model.googlePlaces

import android.os.Parcelable

@kotlinx.parcelize.Parcelize
data class PlacePhoto(
    val name: String,
    val url: String,
    val authorAttributions: AuthorAttributions?
) : Parcelable