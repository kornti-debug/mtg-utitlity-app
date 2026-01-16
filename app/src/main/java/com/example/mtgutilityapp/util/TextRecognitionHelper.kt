package com.example.mtgutilityapp.util

import android.graphics.Bitmap
import androidx.camera.core.ImageProxy
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

data class ScanResult(
    val cardName: String,
    val footerText: String // Contains raw text like "NEO • EN • 123"
)

object TextRecognitionHelper {
    private val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)

    suspend fun recognizeText(imageProxy: ImageProxy): ScanResult? = suspendCancellableCoroutine { continuation ->
        val mediaImage = imageProxy.image
        if (mediaImage != null) {
            val rotation = imageProxy.imageInfo.rotationDegrees
            val image = InputImage.fromMediaImage(mediaImage, rotation)

            // Handle dimension rotation for logic calculations
            var width = imageProxy.width
            var height = imageProxy.height
            if (rotation == 90 || rotation == 270) {
                width = imageProxy.height
                height = imageProxy.width
            }

            recognizer.process(image)
                .addOnSuccessListener { visionText ->
                    val result = extractCardInfo(visionText.textBlocks, width, height)
                    continuation.resume(result)
                }
                .addOnFailureListener {
                    continuation.resume(null)
                }
                .addOnCompleteListener {
                    imageProxy.close()
                }
        } else {
            imageProxy.close()
            continuation.resume(null)
        }
    }

    private fun extractCardInfo(
        textBlocks: List<com.google.mlkit.vision.text.Text.TextBlock>,
        imageWidth: Int,
        imageHeight: Int
    ): ScanResult? {
        val lines = textBlocks.flatMap { it.lines }

        // --- 1. Extract Name (Top Half) ---
        val roiNameBottom = imageHeight * 0.45

        val nameCandidates = lines.filter { line ->
            val box = line.boundingBox ?: return@filter false
            box.bottom < roiNameBottom &&
                    line.text.length > 2 &&
                    !isManaCost(line.text)
        }.sortedBy { it.boundingBox?.top ?: Int.MAX_VALUE }

        val rawName = nameCandidates.firstOrNull()?.text?.trim() ?: return null
        val cleanedName = cleanCardName(rawName)

        // --- 2. Extract Footer Info (Bottom 15%) ---
        // Enhanced: Extract more detail and apply preprocessing
        val roiFooterTop = imageHeight * 0.82

        val footerLines = lines.filter { line ->
            val box = line.boundingBox ?: return@filter false
            box.top > roiFooterTop
        }

        // Combine all footer text with preprocessing
        val footerText = footerLines
            .joinToString(" ") { it.text }
            .let { preprocessFooterText(it) }

        return ScanResult(cleanedName, footerText)
    }

    /**
     * Enhanced footer text preprocessing
     * Applies common OCR error corrections
     */
    private fun preprocessFooterText(text: String): String {
        var cleaned = text.uppercase()

        // Common OCR substitutions for set codes
        cleaned = cleaned
            .replace("0", "O")  // Zero to O
            .replace("1", "I")  // One to I
            .replace("5", "S")  // Five to S
            .replace("8", "B")  // Eight to B
            .replace("6", "G")  // Six to G
            .replace("2", "Z")  // Two to Z (rare)

        // Remove common punctuation that interferes
        cleaned = cleaned.replace("[.,:;]".toRegex(), " ")

        // Normalize whitespace
        cleaned = cleaned.replace("\\s+".toRegex(), " ").trim()

        return cleaned
    }

    private fun isManaCost(text: String): Boolean {
        // Enhanced mana cost detection
        val manaCostPattern = Regex("^[0-9WUBRGCXYZ{},/]+$")
        return text.matches(manaCostPattern) || text.length <= 1
    }

    private fun cleanCardName(name: String): String {
        var cleaned = name

        // Remove trailing mana symbols
        cleaned = cleaned.replace(Regex("[0-9WUBRGCXYZ{},]+$"), "").trim()

        // Handle "Name, The Something" split by mana cost or other artifacts
        if (cleaned.contains(",")) {
            val parts = cleaned.split(",")
            if (parts.size >= 2) {
                val suffix = parts.last().trim()
                // If suffix is very short or looks like mana, remove it
                if (suffix.length <= 5 || suffix.matches(Regex("[0-9WUBRGCXYZ{}]+"))) {
                    cleaned = parts.dropLast(1).joinToString(",").trim()
                }
            }
        }

        // Remove common OCR artifacts
        cleaned = cleaned
            .replace("\"", "")  // Stray quotes
            .replace("'", "'")  // Normalize apostrophes
            .replace("  ", " ") // Double spaces
            .trim()

        return cleaned
    }

    /**
     * Future enhancement: Crop and enhance footer region before OCR
     * This would be called before recognizer.process()
     */
    private fun enhanceFooterRegion(bitmap: Bitmap): Bitmap {
        // TODO: Implement image preprocessing
        // 1. Crop to bottom 15% of image
        // 2. Convert to grayscale
        // 3. Apply contrast enhancement (histogram equalization)
        // 4. Apply sharpening filter
        // 5. Optional: Denoise
        return bitmap
    }
}