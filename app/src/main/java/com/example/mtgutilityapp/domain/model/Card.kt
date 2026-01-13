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
    val rarity: String?,
    val scannedAt: Long = System.currentTimeMillis()
)