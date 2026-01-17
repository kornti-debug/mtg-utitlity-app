package com.example.mtgutilityapp.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.mtgutilityapp.domain.model.Card

@Entity(tableName = "cards")
data class CardEntity(
    @PrimaryKey(autoGenerate = true) val scanId: Long = 0,
    val id: String,
    val name: String,
    val manaCost: String?,
    val typeLine: String,
    val oracleText: String?,
    val power: String?,
    val toughness: String?,
    val imageUrl: String?,
    val setName: String?,
    val setCode: String?,
    val collectorNumber: String?,
    val rarity: String?,
    val artist: String?,

    // --- ADD THESE MISSING FIELDS ---
    val priceEur: String? = null,
    val cardmarketUrl: String? = null,
    val finishes: String? = null, // Saved as "foil,etched"
    // --------------------------------

    val subset: String? = null,
    val isFavorite: Boolean = false,
    val scannedAt: Long
)

fun CardEntity.toDomain(): Card {
    return Card(
        scanId = scanId,
        id = id,
        name = name,
        manaCost = manaCost,
        typeLine = typeLine,
        oracleText = oracleText,
        power = power,
        toughness = toughness,
        imageUrl = imageUrl,
        setName = setName,
        setCode = setCode,
        collectorNumber = collectorNumber,
        rarity = rarity,
        artist = artist,

        // Map the new fields back to the App
        priceEur = priceEur,
        cardmarketUrl = cardmarketUrl,
        finishes = finishes?.split(",")?.filter { it.isNotEmpty() } ?: emptyList(),

        subset = subset,
        isFavorite = isFavorite,
        scannedAt = scannedAt
    )
}

fun Card.toEntity(): CardEntity {
    return CardEntity(
        scanId = scanId,
        id = id,
        name = name,
        manaCost = manaCost,
        typeLine = typeLine,
        oracleText = oracleText,
        power = power,
        toughness = toughness,
        imageUrl = imageUrl,
        setName = setName,
        setCode = setCode,
        collectorNumber = collectorNumber,
        rarity = rarity,
        artist = artist,

        // Save the new fields to the Database
        priceEur = priceEur,
        cardmarketUrl = cardmarketUrl,
        finishes = finishes.joinToString(","),

        subset = subset,
        isFavorite = isFavorite,
        scannedAt = scannedAt
    )
}