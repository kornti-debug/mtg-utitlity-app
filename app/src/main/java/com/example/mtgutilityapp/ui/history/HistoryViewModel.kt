package com.example.mtgutilityapp.ui.history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.mtgutilityapp.data.repository.CardRepository
import com.example.mtgutilityapp.domain.model.Card
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class HistoryUiState(
    val cards: List<Card> = emptyList(),
    val selectedCard: Card? = null,
    val isLoading: Boolean = true
)

class HistoryViewModel(
    private val repository: CardRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(HistoryUiState())
    val uiState: StateFlow<HistoryUiState> = _uiState.asStateFlow()

    init {
        loadCards()
    }

    private fun loadCards() {
        viewModelScope.launch {
            repository.getAllCards().collect { cards ->
                _uiState.update { currentState ->
                    // 1. Sync the currently selected card with the new list
                    // This prevents selection from getting "stuck" on an old version of the card
                    val updatedSelection = currentState.selectedCard?.let { selected ->
                        cards.find { it.scanId == selected.scanId }
                    }

                    // 2. Return new state
                    currentState.copy(
                        cards = cards,
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
            // Logic:
            // 1. Toggle the Favorite status.
            // 2. ALWAYS reset subset to null.
            //    - If unfavoriting: It removes the category (null).
            //    - If favoriting: It starts as "Uncategorized" (null).
            val updatedCard = card.copy(
                isFavorite = !card.isFavorite,
                subset = null
            )
            repository.updateCard(updatedCard)
        }
    }

    fun updateCardSubset(card: Card, subset: String?) {
        viewModelScope.launch {
            val updatedCard = card.copy(subset = subset, isFavorite = true)
            repository.updateCard(updatedCard)

            // If it's a new subset, ensure it's in the persistent list
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