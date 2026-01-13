package com.example.mtgutilityapp.data.repository

import com.example.mtgutilityapp.data.local.CardDao
import com.example.mtgutilityapp.data.local.entity.toDomain
import com.example.mtgutilityapp.data.local.entity.toEntity
import com.example.mtgutilityapp.data.remote.ScryfallApi
import com.example.mtgutilityapp.data.remote.dto.toDomain
import com.example.mtgutilityapp.domain.model.Card
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

data class CardSearchResult(
    val card: Card,
    val isExactMatch: Boolean
)

class CardRepository(
    private val cardDao: CardDao,
    private val scryfallApi: ScryfallApi
) {
    fun getAllCards(): Flow<List<Card>> {
        return cardDao.getAllCards().map { entities ->
            entities.map { it.toDomain() }
        }
    }

    suspend fun getCardById(cardId: String): Card? {
        return cardDao.getCardById(cardId)?.toDomain()
    }

    suspend fun searchCardByName(name: String): Result<CardSearchResult> {
        return try {
            // Use exact name search with quotes for precise matching
            val response = scryfallApi.searchCards("!\"$name\"")

            when {
                response.data.isEmpty() -> {
                    // If exact match fails, try fuzzy search
                    val fuzzyResponse = scryfallApi.searchCards("name:\"$name\"")
                    if (fuzzyResponse.data.isEmpty()) {
                        Result.failure(Exception("Card not found"))
                    } else if (fuzzyResponse.data.size == 1) {
                        // Single result from fuzzy search is acceptable
                        Result.success(CardSearchResult(
                            card = fuzzyResponse.data.first().toDomain(),
                            isExactMatch = true
                        ))
                    } else {
                        // Multiple results - ambiguous
                        Result.success(CardSearchResult(
                            card = fuzzyResponse.data.first().toDomain(),
                            isExactMatch = false
                        ))
                    }
                }
                response.data.size == 1 -> {
                    // Perfect - exactly one match
                    Result.success(CardSearchResult(
                        card = response.data.first().toDomain(),
                        isExactMatch = true
                    ))
                }
                else -> {
                    // Multiple exact matches (rare but possible with reprints)
                    // Take the first one but mark as ambiguous
                    Result.success(CardSearchResult(
                        card = response.data.first().toDomain(),
                        isExactMatch = false
                    ))
                }
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun saveCard(card: Card) {
        cardDao.insertCard(card.toEntity())
    }

    suspend fun updateCard(card: Card) {
        cardDao.updateCard(card.toEntity())
    }

    suspend fun deleteCard(card: Card) {
        cardDao.deleteCard(card.toEntity())
    }

    suspend fun deleteAllCards() {
        cardDao.deleteAllCards()
    }
}