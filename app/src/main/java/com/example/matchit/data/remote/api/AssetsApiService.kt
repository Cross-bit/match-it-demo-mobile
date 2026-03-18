package com.example.matchit.data.remote.api


import com.example.matchit.data.model.session.SessionType
import com.example.matchit.data.remote.model.AssetsApi.CardsDataApiResponse
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Query

/**
 * Api interface for cards data
 */
interface AssetsDataApiService {

    @GET("assets/cards")
    suspend fun getCardsData(
        @Query("cardIds") cardIds: List<String>,
        @Query("sessionType") sessionType: SessionType
    ): Response<CardsDataApiResponse>
}