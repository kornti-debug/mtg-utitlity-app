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
import kotlinx.coroutines.launch
import java.util.concurrent.atomic.AtomicBoolean

data class CameraUiState(
    val isScanning: Boolean = false,
    val recognizedText: String? = null,
    val selectedCard: Card? = null,
    val error: String? = null,
    val isOffline: Boolean = false,
    val matchConfidence: Double = 0.0,
    val suggestedAlternatives: List<Card> = emptyList()
)

class CameraViewModel(
    private val repository: CardRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(CameraUiState())
    val uiState: StateFlow<CameraUiState> = _uiState.asStateFlow()

    private val isProcessing = AtomicBoolean(false)

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
                    _uiState.value = _uiState.value.copy(
                        isScanning = true,
                        recognizedText = scanResult.cardName,
                        error = null,
                        matchConfidence = 0.0,
                        suggestedAlternatives = emptyList()
                    )

                    // Pass full RAW text for regex search
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
                _uiState.value = _uiState.value.copy(
                    isScanning = false,
                    isOffline = true,
                    error = "No internet connection"
                )
                return@launch
            }

            val result = repository.searchCard(name, rawText)

            result.onSuccess { data ->
                val scanId = repository.saveCard(data.card)
                val cardWithId = data.card.copy(scanId = scanId)

                _uiState.value = _uiState.value.copy(
                    selectedCard = cardWithId,
                    isScanning = false,
                    isOffline = false,
                    matchConfidence = data.confidence,
                    suggestedAlternatives = data.suggestedAlternatives
                )
            }.onFailure { exception ->
                // This now handles the strict "No set code found" error
                _uiState.value = _uiState.value.copy(
                    isScanning = false,
                    error = exception.message ?: "Card not found"
                )
            }
        }
    }

    fun saveCard(card: Card) {
        viewModelScope.launch {
            repository.updateCard(card)
            _uiState.value = _uiState.value.copy(selectedCard = card)
        }
    }

    fun dismissCard() {
        _uiState.value = _uiState.value.copy(
            selectedCard = null,
            recognizedText = null,
            error = null,
            matchConfidence = 0.0,
            suggestedAlternatives = emptyList()
        )
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }
}