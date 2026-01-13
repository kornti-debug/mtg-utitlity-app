package com.example.mtgutilityapp.data.remote

import com.example.mtgutilityapp.data.remote.dto.ScryfallSearchResponse
import retrofit2.http.GET
import retrofit2.http.Query

interface ScryfallApi {
    @GET("cards/search")
    suspend fun searchCards(
        @Query("q") query: String
    ): ScryfallSearchResponse

    companion object {
        const val BASE_URL = "https://api.scryfall.com/"
    }
}