package com.example.mtgutilityapp.data.repository

import com.example.mtgutilityapp.data.local.CardDao
import com.example.mtgutilityapp.data.local.SubsetDao
import com.example.mtgutilityapp.data.local.entity.SubsetEntity
import com.example.mtgutilityapp.data.local.entity.toDomain
import com.example.mtgutilityapp.data.local.entity.toEntity
import com.example.mtgutilityapp.data.remote.ScryfallApi
import com.example.mtgutilityapp.data.remote.dto.toDomain
import com.example.mtgutilityapp.domain.model.Card
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

data class CardSearchResult(
    val card: Card,
    val isExactMatch: Boolean,
    val confidence: Double = 0.0,
    val suggestedAlternatives: List<Card> = emptyList()
)

private data class VersionMatch(
    val card: Card,
    val confidence: Double
)

class CardRepository(
    private val cardDao: CardDao,
    private val subsetDao: SubsetDao,
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

    /**
     * Strict Search Strategy:
     * 1. Fuzzy Name Search -> Get canonical name.
     * 2. Fetch ALL prints.
     * 3. MUST find the set code in the text to return a result.
     */
    suspend fun searchCard(rawName: String, rawText: String): Result<CardSearchResult> {
        return try {
            // STEP 1: Get the Canonical Name & Prints
            val nameMatch = scryfallApi.getCardNamed(rawName)
            val correctName = nameMatch.name

            // Fetch ALL prints of this card
            val versionsResponse = scryfallApi.searchCardVersions(
                query = "!\"$correctName\"",
                unique = "prints"
            )

            val candidates = versionsResponse.data.map { it.toDomain() }

            if (candidates.isEmpty()) {
                return Result.failure(Exception("No prints found for $correctName"))
            }

            // STEP 2: Find the matching Set Code in the WHOLE text
            val bestMatch = findBestVersionBySetCode(candidates, rawText)

            if (bestMatch != null) {
                // Success: We found the specific set
                Result.success(CardSearchResult(
                    card = bestMatch.card,
                    isExactMatch = true,
                    confidence = bestMatch.confidence,
                    suggestedAlternatives = candidates.filter { it.id != bestMatch.card.id }.take(5)
                ))
            } else {
                // FAILURE: Strict mode enabled.
                // We did NOT find the set code in the text, so we return an error.
                Result.failure(Exception("Identified \"$correctName\", but could not detect set code. Please rescan."))
            }

        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun findBestVersionBySetCode(
        candidates: List<Card>,
        rawText: String
    ): VersionMatch? {
        val normalizedText = rawText.uppercase()

        val scores = candidates.map { card ->
            var confidence = 0.0
            val setCode = card.setCode?.uppercase() ?: ""

            if (setCode.isNotEmpty()) {
                val regex = generateSmartSetCodeRegex(setCode)

                if (regex.containsMatchIn(normalizedText)) {
                    confidence = 1.0
                }
            }

            VersionMatch(card, confidence)
        }

        return scores
            .filter { it.confidence > 0.0 }
            .maxByOrNull { it.confidence }
    }

    private fun generateSmartSetCodeRegex(code: String): Regex {
        val patternBuilder = StringBuilder()

        // Boundaries: Start of line, whitespace, or non-word char
        patternBuilder.append("(?i)(^|\\s|\\W)")

        code.forEach { char ->
            val charPattern = when (char) {
                'O', '0' -> "[O0QDo]"
                'I', '1' -> "[I1l|i]"
                'S', '5' -> "[S5]"
                'B', '8' -> "[B8]"
                'Z', '2' -> "[Z2]"
                'A', '4' -> "[A4]"
                'E', '3' -> "[E3]"
                'G', '6' -> "[G6]"
                else -> "$char"
            }
            patternBuilder.append(charPattern)
            patternBuilder.append("\\s*")
        }

        patternBuilder.append("($|\\s|\\W)")

        return Regex(patternBuilder.toString())
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

    // Subset persistence methods
    fun getAllSubsets(): Flow<List<String>> {
        return subsetDao.getAllSubsets().map { entities -> entities.map { it.name } }
    }

    suspend fun insertSubset(name: String) {
        subsetDao.insertSubset(SubsetEntity(name))
    }

    suspend fun deleteSubset(name: String) {
        subsetDao.deleteSubset(SubsetEntity(name))
    }
}