package com.example.matchit.data.model.googlePlaces

import android.os.Parcelable


@kotlinx.parcelize.Parcelize
data class AuthorAttributions(
    val displayName: String,
    val photoUri: String,
    val uri: String,
) : Parcelable
