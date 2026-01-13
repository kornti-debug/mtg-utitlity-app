package com.example.mtgutilityapp.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.mtgutilityapp.domain.model.Card

@Entity(tableName = "cards")
data class CardEntity(
    @PrimaryKey val id: String,
    val name: String,
    val manaCost: String?,
    val typeLine: String,
    val oracleText: String?,
    val power: String?,
    val toughness: String?,
    val imageUrl: String?,
    val setName: String?,
    val rarity: String?,
    val scannedAt: Long
)

fun CardEntity.toDomain(): Card {
    return Card(
        id = id,
        name = name,
        manaCost = manaCost,
        typeLine = typeLine,
        oracleText = oracleText,
        power = power,
        toughness = toughness,
        imageUrl = imageUrl,
        setName = setName,
        rarity = rarity,
        scannedAt = scannedAt
    )
}

fun Card.toEntity(): CardEntity {
    return CardEntity(
        id = id,
        name = name,
        manaCost = manaCost,
        typeLine = typeLine,
        oracleText = oracleText,
        power = power,
        toughness = toughness,
        imageUrl = imageUrl,
        setName = setName,
        rarity = rarity,
        scannedAt = scannedAt
    )
}