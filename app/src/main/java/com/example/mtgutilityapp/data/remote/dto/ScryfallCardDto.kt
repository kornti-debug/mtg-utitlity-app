package com.example.mtgutilityapp.data.remote.dto

import com.example.mtgutilityapp.domain.model.Card
import com.google.gson.annotations.SerializedName

data class ScryfallCardDto(
    val id: String,
    val name: String,
    @SerializedName("mana_cost") val manaCost: String?,
    @SerializedName("type_line") val typeLine: String,
    @SerializedName("oracle_text") val oracleText: String?,
    val power: String?,
    val toughness: String?,
    @SerializedName("image_uris") val imageUris: ImageUris?,
    @SerializedName("set_name") val setName: String?,
    val rarity: String?,
    val artist: String?
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
        name = name,
        manaCost = manaCost,
        typeLine = typeLine,
        oracleText = oracleText,
        power = power,
        toughness = toughness,
        imageUrl = imageUris?.normal ?: imageUris?.large,
        setName = setName,
        rarity = rarity,
        artist = artist
    )
}
