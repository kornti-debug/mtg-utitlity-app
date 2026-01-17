package com.example.mtgutilityapp.ui.history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.mtgutilityapp.data.repository.CardRepository
import com.example.mtgutilityapp.domain.model.Card
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class HistoryUiState(
    val cards: List<Card> = emptyList(),
    val subsets: List<String> = emptyList(), // Added to hold dynamic categories
    val selectedCard: Card? = null,
    val isLoading: Boolean = true
)

class HistoryViewModel(
    private val repository: CardRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(HistoryUiState())
    val uiState: StateFlow<HistoryUiState> = _uiState.asStateFlow()

    init {
        loadData()
    }

    private fun loadData() {
        viewModelScope.launch {
            // Combine cards and subsets so the UI always has the latest categories
            combine(
                repository.getAllCards(),
                repository.getAllSubsets()
            ) { cards, manualSubsets ->
                // Calculate all available categories (Manual + ones currently assigned to cards)
                val cardSubsets = cards.mapNotNull { it.subset }.distinct()
                val allSubsets = (cardSubsets + manualSubsets).distinct().sorted()

                Pair(cards, allSubsets)
            }.collect { (cards, allSubsets) ->
                _uiState.update { currentState ->
                    // Sync selection to avoid "stuck" state
                    val updatedSelection = currentState.selectedCard?.let { selected ->
                        cards.find { it.scanId == selected.scanId }
                    }

                    currentState.copy(
                        cards = cards,
                        subsets = allSubsets,
                        isLoading = false,
                        selectedCard = updatedSelection
                    )
                }
            }
        }
    }

    fun selectCard(card: Card) {
        _uiState.update { it.copy(selectedCard = card) }
    }

    fun dismissCard() {
        _uiState.update { it.copy(selectedCard = null) }
    }

    fun toggleFavorite(card: Card) {
        viewModelScope.launch {
            val updatedCard = card.copy(
                isFavorite = !card.isFavorite,
                subset = null // Always reset to Uncategorized when toggling
            )
            repository.updateCard(updatedCard)
        }
    }

    fun updateCardSubset(card: Card, subset: String?) {
        viewModelScope.launch {
            val updatedCard = card.copy(subset = subset, isFavorite = true)
            repository.updateCard(updatedCard)

            // If it's a new custom subset, save it to the DB so it persists
            if (subset != null && subset != "Uncategorized") {
                repository.insertSubset(subset)
            }
        }
    }

    fun updateCard(card: Card) {
        viewModelScope.launch {
            repository.updateCard(card)
        }
    }

    fun deleteCard(card: Card) {
        viewModelScope.launch {
            repository.deleteCard(card)
        }
    }

    fun deleteAllCards() {
        viewModelScope.launch {
            repository.deleteAllCards()
        }
    }
}