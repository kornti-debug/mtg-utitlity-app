package com.example.mtgutilityapp.ui.favorites

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.mtgutilityapp.data.repository.CardRepository
import com.example.mtgutilityapp.domain.model.Card
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class FavoritesUiState(
    val cards: List<Card> = emptyList(),
    val isLoading: Boolean = false,
    val selectedCard: Card? = null
)

class FavoritesViewModel(private val repository: CardRepository) : ViewModel() {

    private val _uiState = MutableStateFlow(FavoritesUiState())
    val uiState: StateFlow<FavoritesUiState> = _uiState.asStateFlow()

    init {
        loadFavorites()
    }

    private fun loadFavorites() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            // For now, we use the same repository. If you have a separate favorite flag, 
            // you'd filter here. Assuming all saved cards are in history/favorites for now.
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
}
