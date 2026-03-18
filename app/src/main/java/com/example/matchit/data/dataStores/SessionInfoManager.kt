package com.example.matchit.data.dataStores

public interface SessionInfoManager {
    suspend fun setMovieInitialized(initialized: Boolean)
    suspend fun isMovieInitialized() : Boolean;

    suspend fun setRestaurantInitialized(initialized: Boolean)
    suspend fun isRestaurantInitialized() : Boolean;
}