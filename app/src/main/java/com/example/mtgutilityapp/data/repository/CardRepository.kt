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
     * Enhanced Smart Search Logic with Confidence Scoring:
     * 1. Fixes typos in the Name (Fuzzy Search)
     * 2. Fetches ALL versions of that card
     * 3. Uses multi-factor confidence scoring to find best match
     */
    suspend fun searchCard(rawName: String, footerText: String): Result<CardSearchResult> {
        return try {
            // STEP 1: Get the Correct Name
            val nameMatch = try {
                scryfallApi.getCardNamed(rawName)
            } catch (e: Exception) {
                throw e
            }

            val correctName = nameMatch.name

            // STEP 2: Fetch ALL Prints
            val versionsResponse = scryfallApi.searchCardVersions(
                query = "!\"$correctName\"",
                unique = "prints"
            )

            val candidates = versionsResponse.data.map { it.toDomain() }

            if (candidates.isEmpty()) {
                return Result.success(CardSearchResult(
                    card = nameMatch.toDomain(),
                    isExactMatch = false,
                    confidence = 0.0
                ))
            }

            // STEP 3: Smart Match with Confidence Scoring
            val bestMatch = findBestVersionWithConfidence(candidates, footerText)

            if (bestMatch != null) {
                Result.success(CardSearchResult(
                    card = bestMatch.card,
                    isExactMatch = bestMatch.confidence >= 0.8,
                    confidence = bestMatch.confidence,
                    suggestedAlternatives = if (bestMatch.confidence < 0.7) {
                        candidates.take(3)
                    } else {
                        emptyList()
                    }
                ))
            } else {
                // No confident match - return most recent printing
                Result.success(CardSearchResult(
                    card = candidates.first(),
                    isExactMatch = false,
                    confidence = 0.0,
                    suggestedAlternatives = candidates.take(5)
                ))
            }

        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Multi-factor confidence scoring system
     */
    private fun findBestVersionWithConfidence(
        candidates: List<Card>,
        footerText: String
    ): VersionMatch? {
        val scores = candidates.map { card ->
            var confidence = 0.0

            // Factor 1: Set Code Fuzzy Match (40% weight)
            val setCodeScore = fuzzyMatchSetCode(
                footerText.uppercase(),
                card.setCode?.uppercase() ?: ""
            )
            confidence += setCodeScore * 0.4

            // Factor 2: Collector Number Match (30% weight)
            val collectorScore = fuzzyMatchCollectorNumber(
                footerText,
                card.collectorNumber ?: ""
            )
            confidence += collectorScore * 0.3

            // Factor 3: Release Date Heuristic (20% weight)
            val recencyScore = calculateRecencyScore(card.setCode)
            confidence += recencyScore * 0.2

            // Factor 4: Set Name Match (10% weight)
            val setNameScore = fuzzyMatchSetName(
                footerText.uppercase(),
                card.setName?.uppercase() ?: ""
            )
            confidence += setNameScore * 0.1

            VersionMatch(card, confidence)
        }

        // Return highest confidence match above threshold
        return scores
            .filter { it.confidence > 0.4 } // Minimum 40% confidence
            .maxByOrNull { it.confidence }
    }

    /**
     * Levenshtein distance-based fuzzy matching for set codes
     */
    private fun fuzzyMatchSetCode(ocrText: String, setCode: String): Double {
        if (setCode.isEmpty()) return 0.0

        // Direct match bonus
        if (ocrText.contains(setCode)) return 1.0

        // Character-swap-tolerant match (common OCR errors)
        val cleaned = ocrText
            .replace("0", "O")
            .replace("1", "I")
            .replace("5", "S")
            .replace("8", "B")
            .replace("6", "G")
            .replace("2", "Z")

        if (cleaned.contains(setCode)) return 0.9

        // Levenshtein distance (allows for 1-2 character errors)
        val extractedCode = extractLikelySetCode(ocrText)
        if (extractedCode.isNotEmpty()) {
            val distance = levenshteinDistance(extractedCode, setCode)
            val maxLen = maxOf(setCode.length, 3)
            val score = maxOf(0.0, 1.0 - (distance.toDouble() / maxLen))
            if (score > 0.6) return score
        }

        return 0.0
    }

    /**
     * Extract likely 3-letter set code from footer text
     */
    private fun extractLikelySetCode(text: String): String {
        // Look for 3-letter uppercase sequences
        val pattern = Regex("[A-Z0-9]{3}")
        val matches = pattern.findAll(text).map { it.value }.toList()

        // Return first match that looks like a set code
        return matches.firstOrNull { candidate ->
            // Filter out unlikely candidates (all numbers, etc.)
            candidate.any { it.isLetter() } &&
                    candidate.count { it.isLetter() } >= 2
        } ?: ""
    }

    /**
     * Fuzzy match collector numbers
     */
    private fun fuzzyMatchCollectorNumber(footerText: String, collectorNum: String): Double {
        if (collectorNum.isEmpty()) return 0.0

        // Extract all numbers from footer
        val numbers = Regex("\\d+").findAll(footerText).map { it.value }.toList()

        // Get the base collector number (before any '/')
        val baseCollectorNum = collectorNum.split("/").first()

        // Check if collector number appears
        return if (numbers.any { it == baseCollectorNum }) {
            1.0
        } else if (numbers.any {
                // Allow for OCR errors in numbers (off by 1-2)
                kotlin.math.abs(it.toIntOrNull()?.minus(baseCollectorNum.toIntOrNull() ?: 0) ?: 999) <= 2
            }) {
            0.7
        } else {
            0.0
        }
    }

    /**
     * Calculate recency score based on set code
     * Newer sets are more likely to be scanned
     */
    private fun calculateRecencyScore(setCode: String?): Double {
        if (setCode == null) return 0.3

        // Map of recent sets (2023-2024) - update periodically
        val recentSets = mapOf(
            // 2024 Sets
            "MKM" to 1.0,  // Murders at Karlov Manor
            "OTJ" to 0.98, // Outlaws of Thunder Junction
            "BIG" to 0.96, // The Big Score
            "MH3" to 0.95, // Modern Horizons 3
            "BLB" to 0.93, // Bloomburrow
            "DSK" to 0.91, // Duskmourn

            // 2023 Sets
            "ONE" to 0.85, // Phyrexia: All Will Be One
            "MOM" to 0.83, // March of the Machine
            "MAT" to 0.81, // March of the Machine: The Aftermath
            "WOE" to 0.79, // Wilds of Eldraine
            "LCI" to 0.77, // The Lost Caverns of Ixalan

            // 2022 Sets
            "DMU" to 0.70, // Dominaria United
            "BRO" to 0.68, // The Brothers' War
            "SNC" to 0.66, // Streets of New Capenna
            "NEO" to 0.64, // Kamigawa: Neon Dynasty

            // Common reprints
            "2X2" to 0.75, // Double Masters 2022
            "CLB" to 0.72  // Commander Legends: Battle for Baldur's Gate
        )

        return recentSets[setCode.uppercase()] ?: 0.3
    }

    /**
     * Fuzzy match set names (less reliable, lower weight)
     */
    private fun fuzzyMatchSetName(ocrText: String, setName: String): Double {
        if (setName.isEmpty()) return 0.0

        // Extract key words from set name
        val keywords = setName.split(" ")
            .filter { it.length > 3 } // Ignore short words
            .take(2) // First two significant words

        val matchCount = keywords.count { keyword ->
            ocrText.contains(keyword, ignoreCase = true)
        }

        return if (keywords.isEmpty()) 0.0
        else matchCount.toDouble() / keywords.size
    }

    /**
     * Levenshtein distance algorithm
     */
    private fun levenshteinDistance(s1: String, s2: String): Int {
        val m = s1.length
        val n = s2.length
        val dp = Array(m + 1) { IntArray(n + 1) }

        for (i in 0..m) dp[i][0] = i
        for (j in 0..n) dp[0][j] = j

        for (i in 1..m) {
            for (j in 1..n) {
                val cost = if (s1[i - 1] == s2[j - 1]) 0 else 1
                dp[i][j] = minOf(
                    dp[i - 1][j] + 1,          // deletion
                    dp[i][j - 1] + 1,          // insertion
                    dp[i - 1][j - 1] + cost    // substitution
                )
            }
        }
        return dp[m][n]
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