package com.example.matchit.data.remote.model.SessionApi

import com.example.matchit.data.model.session.MovieCardData


/**
 * This is a data model for data returned in initialisation phase for movie recommendations
 */

data class MoviesInitSessionDataApiResponse(
    val movies: List<MovieCardData>
)
