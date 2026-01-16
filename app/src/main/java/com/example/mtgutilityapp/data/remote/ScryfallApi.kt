package com.example.mtgutilityapp.data.remote

import com.example.mtgutilityapp.data.remote.dto.ScryfallCardDto
import com.example.mtgutilityapp.data.remote.dto.ScryfallSearchResponse
import retrofit2.http.GET
import retrofit2.http.Query

interface ScryfallApi {
    @GET("cards/search")
    suspend fun searchCards(
        @Query("q") query: String
    ): ScryfallSearchResponse

    // Used to fetch all prints of a card name
    @GET("cards/search")
    suspend fun searchCardVersions(
        @Query("q") query: String,
        @Query("unique") unique: String = "prints", // Get every printing
        @Query("order") order: String = "released" // Newest first
    ): ScryfallSearchResponse

    @GET("cards/named")
    suspend fun getCardNamed(
        @Query("fuzzy") name: String
    ): ScryfallCardDto

    companion object {
        const val BASE_URL = "https://api.scryfall.com/"
    }
}