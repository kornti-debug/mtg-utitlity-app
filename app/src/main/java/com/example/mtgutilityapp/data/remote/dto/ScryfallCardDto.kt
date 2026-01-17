package com.example.mtgutilityapp.data.remote.dto

import com.example.mtgutilityapp.domain.model.Card
import com.google.gson.annotations.SerializedName

data class ScryfallCardDto(
    val id: String,
    val name: String,
    @SerializedName("printed_name") val printedName: String?, // localized name
    @SerializedName("lang") val lang: String?,                // "en", "de", etc.
    @SerializedName("mana_cost") val manaCost: String?,
    @SerializedName("type_line") val typeLine: String?,
    @SerializedName("oracle_text") val oracleText: String?,
    val power: String?,
    val toughness: String?,
    @SerializedName("image_uris") val imageUris: ImageUris?,
    @SerializedName("set_name") val setName: String?,
    @SerializedName("set") val setCode: String?,
    @SerializedName("collector_number") val collectorNumber: String?,
    val rarity: String?,
    val artist: String?,

    // Pricing & Links
    val prices: Prices?,
    @SerializedName("purchase_uris") val purchaseUris: PurchaseUris?,
    val finishes: List<String>?
)

data class Prices(
    val usd: String?,
    val eur: String?,
    val tix: String?
)

data class PurchaseUris(
    val tcgplayer: String?,
    val cardmarket: String?,
    val cardhoarder: String?
)

data class ImageUris(
    val small: String?,
    val normal: String?,
    val large: String?,
    @SerializedName("art_crop") val artCrop: String?
)

data class ScryfallSearchResponse(
    val data: List<ScryfallCardDto>
)

fun ScryfallCardDto.toDomain(): Card {
    return Card(
        id = id,
        // Use the printed (localized) name if available, otherwise the English name
        name = printedName ?: name,
        manaCost = manaCost,
        typeLine = typeLine ?: "",
        oracleText = oracleText,
        power = power,
        toughness = toughness,
        imageUrl = imageUris?.normal ?: imageUris?.large,
        setName = setName,
        setCode = setCode,
        collectorNumber = collectorNumber,
        rarity = rarity,
        artist = artist,

        // Map Scryfall data
        priceEur = prices?.eur,
        cardmarketUrl = purchaseUris?.cardmarket,
        finishes = finishes ?: emptyList()
    )
}