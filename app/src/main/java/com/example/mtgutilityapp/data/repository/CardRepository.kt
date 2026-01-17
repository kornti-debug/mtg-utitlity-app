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
import kotlin.math.min

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
     * Search Strategy for EN/DE Support:
     * 1. Search Scryfall allowing ANY language to find the "Oracle Identity" (Canonical Name).
     * 2. Fetch ALL prints (multilingual) for that card.
     * 3. Match set code.
     * 4. If set code matches multiple (e.g. EN and DE), compare scanned title with printed name.
     */
    suspend fun searchCard(rawName: String, rawText: String): Result<CardSearchResult> {
        return try {
            // STEP 1: Find the card object (in any language) to get the canonical English name
            // We search for the exact name first in any language
            val initialSearch = scryfallApi.searchCards(
                query = "!\"$rawName\" lang:any",
                unique = "prints",
                includeMultilingual = true
            )

            // Fallback to fuzzy search if exact quote fails
            val candidatesList = if (initialSearch.data.isNotEmpty()) {
                initialSearch.data
            } else {
                scryfallApi.searchCards(
                    query = "$rawName lang:any",
                    unique = "prints",
                    includeMultilingual = true
                ).data
            }

            if (candidatesList.isEmpty()) {
                return Result.failure(Exception("No card found for \"$rawName\""))
            }

            // Get the canonical English name from the best match
            val correctName = candidatesList.first().name

            // STEP 2: Fetch ALL prints/languages for this specific card
            val versionsResponse = scryfallApi.searchCardVersions(
                query = "!\"$correctName\" include:extras",
                unique = "prints",
                includeMultilingual = true // MUST be true to get DE cards
            )

            val allCandidates = versionsResponse.data.map { it.toDomain() }

            if (allCandidates.isEmpty()) {
                return Result.failure(Exception("No prints found for $correctName"))
            }

            // STEP 3: Find the specific version (Set Code + Language Match)
            val bestMatch = findBestVersion(allCandidates, rawText, rawName)

            if (bestMatch != null) {
                Result.success(CardSearchResult(
                    card = bestMatch.card,
                    isExactMatch = true,
                    confidence = bestMatch.confidence,
                    suggestedAlternatives = allCandidates
                        .filter { it.id != bestMatch.card.id }
                        .sortedByDescending { it.id == bestMatch.card.id } // Just a placeholder sort
                        .take(5)
                ))
            } else {
                Result.failure(Exception("Identified \"$correctName\", but could not match Set Code or Language."))
            }

        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun findBestVersion(
        candidates: List<Card>,
        rawText: String,
        rawNameTitle: String
    ): VersionMatch? {
        val normalizedText = rawText.uppercase()

        // 1. Filter candidates that match the Set Code found in the footer
        val setCodeMatches = candidates.filter { card ->
            val setCode = card.setCode?.uppercase() ?: ""
            if (setCode.isNotEmpty()) {
                val regex = generateSmartSetCodeRegex(setCode)
                regex.containsMatchIn(normalizedText)
            } else {
                false
            }
        }

        // If no set code matched, we can't be sure.
        if (setCodeMatches.isEmpty()) return null

        // 2. Disambiguate Language (EN vs DE)
        // If we have multiple matches for the same set code (e.g. #123 EN and #123 DE),
        // we compare the scanned title (rawNameTitle) with the card's name (or printedName).

        val bestMatch = setCodeMatches.maxByOrNull { card ->
            // Calculate similarity between scanned title and this card's name
            val similarity = calculateSimilarity(rawNameTitle, card.name)
            similarity
        }

        return bestMatch?.let { VersionMatch(it, 1.0) }
    }

    // Simple similarity check (0.0 to 1.0)
    private fun calculateSimilarity(s1: String, s2: String): Double {
        val longer = if (s1.length > s2.length) s1 else s2
        val shorter = if (s1.length > s2.length) s2 else s1
        if (longer.isEmpty()) return 1.0
        val distance = levenshtein(longer.lowercase(), shorter.lowercase())
        return (longer.length - distance).toDouble() / longer.length.toDouble()
    }

    private fun levenshtein(lhs: CharSequence, rhs: CharSequence): Int {
        val lhsLen = lhs.length
        val rhsLen = rhs.length

        var cost = IntArray(lhsLen + 1) { it }
        var newCost = IntArray(lhsLen + 1) { 0 }

        for (i in 1..rhsLen) {
            newCost[0] = i
            for (j in 1..lhsLen) {
                val match = if (lhs[j - 1] == rhs[i - 1]) 0 else 1
                val costReplace = cost[j - 1] + match
                val costInsert = cost[j] + 1
                val costDelete = newCost[j - 1] + 1
                newCost[j] = min(min(costInsert, costDelete), costReplace)
            }
            val swap = cost
            cost = newCost
            newCost = swap
        }
        return cost[lhsLen]
    }

    private fun generateSmartSetCodeRegex(code: String): Regex {
        val patternBuilder = StringBuilder()
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