package com.example.mtgutilityapp.ui.history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.mtgutilityapp.data.repository.CardRepository
import com.example.mtgutilityapp.domain.model.Card
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
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
                _uiState.value = _uiState.value.copy(
                    cards = cards,
                    isLoading = false
                )
            }
        }
    }

    fun selectCard(card: Card) {
        _uiState.value = _uiState.value.copy(selectedCard = card)
    }

    fun dismissCard() {
        _uiState.value = _uiState.value.copy(selectedCard = null)
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