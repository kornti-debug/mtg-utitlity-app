package com.example.mtgutilityapp.util

import android.graphics.Rect
import androidx.camera.core.ImageProxy
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

object TextRecognitionHelper {
    private val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)

    suspend fun recognizeText(imageProxy: ImageProxy): String? = suspendCancellableCoroutine { continuation ->
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
                    val cardName = extractCardName(visionText.textBlocks, width, height)
                    continuation.resume(cardName)
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

    private fun extractCardName(
        textBlocks: List<com.google.mlkit.vision.text.Text.TextBlock>,
        imageWidth: Int,
        imageHeight: Int
    ): String? {
        // We assume the user attempts to align the card with the center of the screen/preview.
        // The standard Magic card is 63mm x 88mm.
        // Due to preview cropping on different aspect ratios, the "centered" card
        // usually starts somewhere around 20-30% down the image height and ends around 70-80%.
        // The Card Name is in the top 10-15% of the card.

        // Defines a "Region of Interest" (ROI) where we expect the card name to be.
        // This allows for detection without a strictly rigid frame, but optimized for the overlay guide.

        val roiTop = imageHeight * 0.15 // Start searching 15% down (skips top edge noise)
        val roiBottom = imageHeight * 0.50 // Name should definitely be in the top half of the image

        // Exclude edges
        val roiLeft = imageWidth * 0.10
        val roiRight = imageWidth * 0.90

        // For right-side exclusion (mana cost), we'll be more specific per line

        val candidateTexts = textBlocks
            .flatMap { it.lines }
            .filter { line ->
                val box = line.boundingBox ?: return@filter false

                // Check if line is within our generic ROI
                val inRoi = box.top > roiTop &&
                        box.bottom < roiBottom &&
                        box.left > roiLeft &&
                        box.right < roiRight

                if (!inRoi) return@filter false

                // Further filtering:
                // 1. Text should not be too short
                // 2. Text should not look like mana cost
                // 3. To avoid mana symbols on the right, ensure the text starts on the left side of the card area
                //    (Assuming the text line itself is the name, it usually starts left-aligned)

                line.text.length > 2 && !isManaCost(line.text)
            }
            .sortedBy { it.boundingBox?.top ?: Int.MAX_VALUE }

        // Get the first valid line as the card name
        val cardName = candidateTexts.firstOrNull()?.text?.trim()

        return cardName?.let { cleanCardName(it) }
    }

    private fun isManaCost(text: String): Boolean {
        // Check if text looks like mana cost symbols
        // Mana costs are typically: numbers, single letters (W, U, B, R, G), or symbols like {1}, {W}, etc.
        val manaCostPattern = Regex("^[0-9WUBRGCXYZ{},]+$")
        return text.matches(manaCostPattern) || text.length <= 2
    }

    private fun cleanCardName(name: String): String {
        var cleaned = name

        // Remove trailing mana symbols like "1", "2G", "{W}{U}", etc.
        cleaned = cleaned.replace(Regex("[0-9WUBRGCXYZ{},]+$"), "").trim()

        // Remove common OCR artifacts or "Creature - ..." type lines if we accidentally caught the type line
        // But type lines are usually lower.

        // Remove text after a comma if it looks like mana cost (e.g. "Name, 3G")
        if (cleaned.contains(",")) {
            val parts = cleaned.split(",")
            if (parts.size >= 2) {
                val suffix = parts.last().trim()
                if (suffix.length <= 5 || suffix.matches(Regex("[0-9WUBRGCXYZ{}]+"))) {
                    cleaned = parts.dropLast(1).joinToString(",").trim()
                }
            }
        }

        return cleaned.trim()
    }
}