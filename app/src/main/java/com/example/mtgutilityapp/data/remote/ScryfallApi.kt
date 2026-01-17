package com.example.mtgutilityapp.data.remote

import com.example.mtgutilityapp.data.remote.dto.ScryfallCardDto
import com.example.mtgutilityapp.data.remote.dto.ScryfallSearchResponse
import retrofit2.http.GET
import retrofit2.http.Query

interface ScryfallApi {
    @GET("cards/search")
    suspend fun searchCards(
        @Query("q") query: String,
        @Query("unique") unique: String = "cards",
        @Query("include_multilingual") includeMultilingual: Boolean = true // Enable finding foreign cards
    ): ScryfallSearchResponse

    // Used to fetch all prints of a card name (including foreign)
    @GET("cards/search")
    suspend fun searchCardVersions(
        @Query("q") query: String,
        @Query("unique") unique: String = "prints", // Get every printing
        @Query("order") order: String = "released", // Newest first
        @Query("include_multilingual") includeMultilingual: Boolean = true // Critical for DE/EN check
    ): ScryfallSearchResponse

    @GET("cards/named")
    suspend fun getCardNamed(
        @Query("fuzzy") name: String
    ): ScryfallCardDto

    companion object {
        const val BASE_URL = "https://api.scryfall.com/"
    }
}