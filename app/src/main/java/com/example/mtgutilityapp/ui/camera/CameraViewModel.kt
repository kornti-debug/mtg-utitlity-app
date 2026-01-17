package com.example.mtgutilityapp.ui.camera

import android.content.Context
import androidx.camera.core.ImageProxy
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.mtgutilityapp.data.repository.CardRepository
import com.example.mtgutilityapp.domain.model.Card
import com.example.mtgutilityapp.util.NetworkHelper
import com.example.mtgutilityapp.util.TextRecognitionHelper
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.concurrent.atomic.AtomicBoolean

data class CameraUiState(
    val isScanning: Boolean = false,
    val recognizedText: String? = null,
    val selectedCard: Card? = null,
    val error: String? = null,
    val isOffline: Boolean = false,
    val matchConfidence: Double = 0.0,
    val suggestedAlternatives: List<Card> = emptyList(),
    // NEW: Needed for the overlay dropdown
    val subsets: List<String> = emptyList()
)

class CameraViewModel(
    private val repository: CardRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(CameraUiState())
    val uiState: StateFlow<CameraUiState> = _uiState.asStateFlow()

    private val isProcessing = AtomicBoolean(false)

    init {
        // NEW: Load subsets immediately so they are ready when scanning
        loadSubsets()
    }

    private fun loadSubsets() {
        viewModelScope.launch {
            repository.getAllSubsets().collect { subsets ->
                _uiState.update { it.copy(subsets = subsets) }
            }
        }
    }

    fun processImage(imageProxy: ImageProxy, context: Context) {
        if (_uiState.value.isScanning || _uiState.value.selectedCard != null || isProcessing.get()) {
            imageProxy.close()
            return
        }

        if (!isProcessing.compareAndSet(false, true)) {
            imageProxy.close()
            return
        }

        viewModelScope.launch {
            try {
                val scanResult = TextRecognitionHelper.recognizeText(imageProxy)

                if (scanResult != null && scanResult.cardName.isNotBlank()) {
                    _uiState.update {
                        it.copy(
                            isScanning = true,
                            recognizedText = scanResult.cardName,
                            error = null,
                            matchConfidence = 0.0,
                            suggestedAlternatives = emptyList()
                        )
                    }

                    searchCard(scanResult.cardName, scanResult.rawFooter, context)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                isProcessing.set(false)
            }
        }
    }

    private fun searchCard(name: String, rawText: String, context: Context) {
        viewModelScope.launch {
            if (!NetworkHelper.isNetworkAvailable(context)) {
                _uiState.update {
                    it.copy(
                        isScanning = false,
                        isOffline = true,
                        error = "No internet connection"
                    )
                }
                return@launch
            }

            val result = repository.searchCard(name, rawText)

            result.onSuccess { data ->
                val scanId = repository.saveCard(data.card)
                val cardWithId = data.card.copy(scanId = scanId)

                _uiState.update {
                    it.copy(
                        selectedCard = cardWithId,
                        isScanning = false,
                        isOffline = false,
                        matchConfidence = data.confidence,
                        suggestedAlternatives = data.suggestedAlternatives
                    )
                }
            }.onFailure { exception ->
                _uiState.update {
                    it.copy(
                        isScanning = false,
                        error = exception.message ?: "Card not found"
                    )
                }
            }
        }
    }

    fun saveCard(card: Card) {
        viewModelScope.launch {
            repository.updateCard(card)
            _uiState.update { it.copy(selectedCard = card) }

            // NEW: If the user typed a custom category in the overlay, ensure it's saved
            if (card.subset != null && card.subset != "Uncategorized") {
                repository.insertSubset(card.subset)
            }
        }
    }

    fun dismissCard() {
        _uiState.update {
            it.copy(
                selectedCard = null,
                recognizedText = null,
                error = null,
                matchConfidence = 0.0,
                suggestedAlternatives = emptyList()
            )
        }
    }

    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }
}