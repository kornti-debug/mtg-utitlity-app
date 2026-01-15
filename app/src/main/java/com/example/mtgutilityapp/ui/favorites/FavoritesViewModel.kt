package com.example.mtgutilityapp.ui.favorites

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.mtgutilityapp.data.repository.CardRepository
import com.example.mtgutilityapp.domain.model.Card
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch

data class FavoritesUiState(
    val cards: List<Card> = emptyList(),
    val subsets: List<String> = emptyList(),
    val selectedSubset: String? = null,
    val isLoading: Boolean = false,
    val selectedCard: Card? = null
)

class FavoritesViewModel(private val repository: CardRepository) : ViewModel() {

    private val _selectedSubset = MutableStateFlow<String?>(null)
    private val _uiState = MutableStateFlow(FavoritesUiState())
    val uiState: StateFlow<FavoritesUiState> = _uiState.asStateFlow()

    init {
        loadData()
    }

    private fun loadData() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            
            // Combine all cards and the selected subset to produce the filtered list
            combine(
                repository.getAllCards(),
                _selectedSubset
            ) { allCards, selectedSubset ->
                val favorites = allCards.filter { it.isFavorite }
                val uniqueSubsets = favorites.mapNotNull { it.subset }.distinct().sorted()
                
                val filteredCards = if (selectedSubset == null) {
                    // Show favorites that ARE NOT in any subset
                    favorites.filter { it.subset == null }
                } else {
                    // Show favorites in the selected subset
                    favorites.filter { it.subset == selectedSubset }
                }

                FavoritesUiState(
                    cards = filteredCards,
                    subsets = uniqueSubsets,
                    selectedSubset = selectedSubset,
                    isLoading = false,
                    selectedCard = _uiState.value.selectedCard
                )
            }.collect { newState ->
                _uiState.value = newState
            }
        }
    }

    fun selectSubset(subset: String?) {
        _selectedSubset.value = subset
    }

    fun toggleFavorite(card: Card) {
        viewModelScope.launch {
            val updatedCard = card.copy(isFavorite = !card.isFavorite)
            repository.updateCard(updatedCard)
        }
    }

    fun updateCardSubset(card: Card, subset: String?) {
        viewModelScope.launch {
            val updatedCard = card.copy(subset = subset, isFavorite = true)
            repository.updateCard(updatedCard)
        }
    }

    fun selectCard(card: Card) {
        _uiState.value = _uiState.value.copy(selectedCard = card)
    }

    fun dismissCard() {
        _uiState.value = _uiState.value.copy(selectedCard = null)
    }
}
