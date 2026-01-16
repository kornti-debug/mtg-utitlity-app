package com.example.mtgutilityapp.util

import android.graphics.Bitmap
import android.graphics.Matrix
import androidx.camera.core.ImageProxy
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

data class ScanResult(
    val cardName: String,
    val formattedFooter: String,
    val rawFooter: String // Contains WHOLE OCR TEXT
)

object TextRecognitionHelper {
    private val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)

    suspend fun recognizeText(imageProxy: ImageProxy): ScanResult? = suspendCancellableCoroutine { continuation ->
        try {
            val rawBitmap = imageProxy.toBitmap()
            val rotation = imageProxy.imageInfo.rotationDegrees
            imageProxy.close()

            val uprightBitmap = if (rotation != 0) {
                val matrix = Matrix()
                matrix.postRotate(rotation.toFloat())
                Bitmap.createBitmap(rawBitmap, 0, 0, rawBitmap.width, rawBitmap.height, matrix, true)
            } else {
                rawBitmap
            }

            val fullImageInput = InputImage.fromBitmap(uprightBitmap, 0)

            recognizer.process(fullImageInput)
                .addOnSuccessListener { visionText ->
                    val name = extractNameFromFullScan(visionText, uprightBitmap.height)

                    if (name.isBlank()) {
                        continuation.resume(null)
                        return@addOnSuccessListener
                    }

                    // Return EVERYTHING found on the card
                    val fullRawText = visionText.text

                    continuation.resume(ScanResult(
                        cardName = name,
                        formattedFooter = "",
                        rawFooter = fullRawText
                    ))
                }
                .addOnFailureListener {
                    continuation.resume(null)
                }

        } catch (e: Exception) {
            e.printStackTrace()
            continuation.resume(null)
        }
    }

    private fun extractNameFromFullScan(
        visionText: com.google.mlkit.vision.text.Text,
        imageHeight: Int
    ): String {
        val roiBottomLimit = imageHeight * 0.45

        val nameCandidates = visionText.textBlocks.flatMap { it.lines }
            .filter { line ->
                val box = line.boundingBox ?: return@filter false
                box.bottom < roiBottomLimit &&
                        line.text.length > 2 &&
                        !isManaCost(line.text)
            }
            .sortedBy { it.boundingBox?.top ?: Int.MAX_VALUE }

        val rawName = nameCandidates.firstOrNull()?.text?.trim() ?: ""
        return cleanCardName(rawName)
    }

    private fun isManaCost(text: String): Boolean {
        val manaCostPattern = Regex("^[0-9WUBRGCXYZ{},/]+$")
        return text.matches(manaCostPattern) || text.length <= 1
    }

    private fun cleanCardName(name: String): String {
        var cleaned = name
        cleaned = cleaned.replace(Regex("[0-9WUBRGCXYZ{},]+$"), "").trim()

        if (cleaned.contains(",")) {
            val parts = cleaned.split(",")
            if (parts.size >= 2) {
                val suffix = parts.last().trim()
                if (suffix.length <= 5 || suffix.matches(Regex("[0-9WUBRGCXYZ{}]+"))) {
                    cleaned = parts.dropLast(1).joinToString(",").trim()
                }
            }
        }

        cleaned = cleaned
            .replace("\"", "")
            .replace("'", "'")
            .replace("  ", " ")
            .trim()

        return cleaned
    }
}