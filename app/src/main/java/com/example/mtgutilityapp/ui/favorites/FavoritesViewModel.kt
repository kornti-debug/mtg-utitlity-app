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

// Inside FavoritesViewModel.kt

    private fun loadData() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)

            combine(
                repository.getAllCards(),
                _selectedSubset
            ) { allCards, selectedSubset ->
                // 1. Get all favorites
                val favorites = allCards.filter { it.isFavorite }

                // 2. Build the list of available category tabs
                // We start with "All" (represented by null in UI logic, but handled separately here)
                // We find all unique non-null subsets existing in the DB
                val existingSubsets = favorites.mapNotNull { it.subset }.distinct().sorted()

                // 3. Determine which cards to show based on selection
                val filteredCards = when (selectedSubset) {
                    null -> favorites // "All Favorites" shows EVERYTHING now
                    "Uncategorized" -> favorites.filter { it.subset == null } // Dedicated filter for unsorted
                    else -> favorites.filter { it.subset == selectedSubset } // Specific category
                }

                FavoritesUiState(
                    cards = filteredCards,
                    // We pass the categories to the UI.
                    // We don't add "Uncategorized" here yet, we'll do it in the UI to keep it distinct.
                    subsets = existingSubsets,
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
