package com.example.mtgutilityapp.ui.favorites

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.mtgutilityapp.data.repository.CardRepository
import com.example.mtgutilityapp.domain.model.Card
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

data class FavoritesUiState(
    val cards: List<Card> = emptyList(),
    val subsets: List<String> = emptyList(),
    val selectedSubset: String? = "Uncategorized",
    val isLoading: Boolean = false,
    val selectedCard: Card? = null
)

class FavoritesViewModel(private val repository: CardRepository) : ViewModel() {

    private val _selectedSubset = MutableStateFlow<String?>("Uncategorized")
    private val _uiState = MutableStateFlow(FavoritesUiState())
    val uiState: StateFlow<FavoritesUiState> = _uiState.asStateFlow()

    init {
        loadData()
    }

    private fun loadData() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)

            combine(
                repository.getAllCards(),
                _selectedSubset,
                repository.getAllSubsets()
            ) { allCards, selectedSubset, manualSubsets ->
                val favorites = allCards.filter { it.isFavorite }
                
                // Get subsets from cards + manually added ones from DB
                val cardSubsets = favorites.mapNotNull { it.subset }.distinct()
                val allSubsets = (cardSubsets + manualSubsets).distinct().sorted()

                val filteredCards = when (selectedSubset) {
                    "Uncategorized" -> favorites.filter { it.subset == null }
                    else -> favorites.filter { it.subset == selectedSubset }
                }

                FavoritesUiState(
                    cards = filteredCards,
                    subsets = allSubsets,
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

    fun addSubset(name: String) {
        if (name.isNotBlank() && name != "Uncategorized") {
            viewModelScope.launch {
                repository.insertSubset(name)
                _selectedSubset.value = name
            }
        }
    }

    fun deleteSubset(name: String) {
        viewModelScope.launch {
            // 1. Remove from DB
            repository.deleteSubset(name)
            
            // 2. Update all cards that were in this subset to be Uncategorized (null)
            val allCards = repository.getAllCards().first()
            allCards.filter { it.subset == name }.forEach { card ->
                repository.updateCard(card.copy(subset = null))
            }
            
            // 3. If the deleted subset was selected, go back to Uncategorized
            if (_selectedSubset.value == name) {
                _selectedSubset.value = "Uncategorized"
            }
        }
    }

    fun toggleFavorite(card: Card) {
        viewModelScope.launch {
            val isCurrentlyFavorite = card.isFavorite
            val updatedCard = card.copy(
                isFavorite = !isCurrentlyFavorite,
                // If we're unfavoriting, we also clear the subset
                subset = if (isCurrentlyFavorite) null else card.subset
            )
            repository.updateCard(updatedCard)
        }
    }

    fun updateCardSubset(card: Card, subset: String?) {
        viewModelScope.launch {
            val updatedCard = card.copy(subset = subset, isFavorite = true)
            repository.updateCard(updatedCard)
            
            // If it's a new subset (not Uncategorized), ensure it's in the persistent list
            if (subset != null && subset != "Uncategorized") {
                repository.insertSubset(subset)
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
