package com.example.mtgutilityapp.domain.model

data class Card(
    val scanId: Long = 0,
    val id: String,
    val name: String,
    val manaCost: String?,
    val typeLine: String,
    val oracleText: String?,
    val power: String?,
    val toughness: String?,
    val imageUrl: String?,
    val setName: String?,
    val setCode: String?,         // NEW
    val collectorNumber: String?, // NEW
    val rarity: String?,
    val artist: String?,
    val subset: String? = null,
    val isFavorite: Boolean = false,
    val scannedAt: Long = System.currentTimeMillis()
)