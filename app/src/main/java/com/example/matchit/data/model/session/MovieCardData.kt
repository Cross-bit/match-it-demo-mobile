package com.example.matchit.data.model.session

import android.os.Parcelable
import com.google.gson.annotations.SerializedName

@kotlinx.parcelize.Parcelize
data class WatchProviderData(
    val name: String?,
    val logo: String?,   // URL
    val type: String     // "flatrate", "rent", "buy"
) : Parcelable


/**
 * Represents data model for movie card item
 */
@kotlinx.parcelize.Parcelize
data class MovieCardData(
    val title: String,
    val genres: List<String>,
    val imageUrl: String?,
    val year: Int?,
    val description: String?,
    val ratingTMDB: Float?,

    val backdropUrl: String? = null,
    val runtime: Int? = null,
    val originCountry: List<String> = emptyList(),
    val actors: List<String> = emptyList(),
    val directors: List<String> = emptyList(),
    val popularity: Float? = null,
    val tagline: String? = null,

    val trailerUrl: String? = null,
    val watchProviders: List<WatchProviderData> = emptyList()

) : CardData(cardId = ""), Parcelable