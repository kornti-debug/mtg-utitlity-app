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

    suspend fun getCardByScanId(scanId: Long): Card? {
        return cardDao.getCardByScanId(scanId)?.toDomain()
    }

    suspend fun searchCardByName(name: String): Result<CardSearchResult> {
        return try {
            // Use the fuzzy named endpoint as per the working implementation
            val response = scryfallApi.getCardNamed(name)
            Result.success(CardSearchResult(
                card = response.toDomain(),
                isExactMatch = true
            ))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun saveCard(card: Card): Long {
        return cardDao.insertCard(card.toEntity())
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
