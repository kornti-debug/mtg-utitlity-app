package com.example.mtgutilityapp.ui.camera

import android.content.Context
import android.graphics.Rect
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
    val multipleResults: Boolean = false
)

class CameraViewModel(
    private val repository: CardRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(CameraUiState())
    val uiState: StateFlow<CameraUiState> = _uiState.asStateFlow()

    private val isProcessing = AtomicBoolean(false)

    fun processImage(imageProxy: ImageProxy, context: Context) {
        // Stop processing if we're already scanning, processing, or if we have a result selected
        if (_uiState.value.isScanning || _uiState.value.selectedCard != null || isProcessing.get()) {
            imageProxy.close()
            return
        }

        if (!isProcessing.compareAndSet(false, true)) {
            imageProxy.close()
            return
        }

        // Apply crop to focus on the name bar (top 15% of the frame)
        val width = imageProxy.width
        val height = imageProxy.height
        val cropRect = Rect(0, 0, width, (height * 0.15).toInt())
        imageProxy.setCropRect(cropRect)

        viewModelScope.launch {
            try {
                val cardName = TextRecognitionHelper.recognizeText(imageProxy)

                if (cardName != null && cardName.isNotBlank()) {
                    _uiState.value = _uiState.value.copy(
                        isScanning = true,
                        recognizedText = cardName,
                        error = null,
                        multipleResults = false
                    )
                    searchCard(cardName, context)
                }
            } finally {
                isProcessing.set(false)
            }
        }
    }

    private fun searchCard(name: String, context: Context) {
        viewModelScope.launch {
            if (!NetworkHelper.isNetworkAvailable(context)) {
                _uiState.value = _uiState.value.copy(
                    isScanning = false,
                    isOffline = true,
                    error = "No internet connection"
                )
                return@launch
            }

            val result = repository.searchCardByName(name)
            result.onSuccess { data ->
                // Check if we got exactly one result or multiple results
                if (data.isExactMatch) {
                    _uiState.value = _uiState.value.copy(
                        selectedCard = data.card,
                        isScanning = false,
                        isOffline = false,
                        multipleResults = false
                    )
                } else {
                    _uiState.value = _uiState.value.copy(
                        isScanning = false,
                        error = "Multiple cards found. Please scan more clearly.",
                        multipleResults = true
                    )
                }
            }.onFailure { exception ->
                _uiState.value = _uiState.value.copy(
                    isScanning = false,
                    error = exception.message ?: "Card not found"
                )
            }
        }
    }

    fun saveCard() {
        viewModelScope.launch {
            _uiState.value.selectedCard?.let { card ->
                repository.saveCard(card)
            }
        }
    }

    fun dismissCard() {
        _uiState.value = _uiState.value.copy(
            selectedCard = null,
            recognizedText = null,
            error = null,
            multipleResults = false
        )
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }
}
