package com.example.mtgutilityapp.domain.model

data class Card(
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

    // New Fields for Scryfall Data
    val priceEur: String? = null,
    val cardmarketUrl: String? = null,
    val finishes: List<String> = emptyList(),

    // App specific fields
    val subset: String? = null,
    val isFavorite: Boolean = false,
    val scanId: Long = System.currentTimeMillis(), // Unique ID for history list
    val scannedAt: Long = System.currentTimeMillis()
)