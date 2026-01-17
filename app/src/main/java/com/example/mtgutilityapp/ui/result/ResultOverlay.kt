package com.example.mtgutilityapp.ui.result

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.AsyncImage
import com.example.mtgutilityapp.domain.model.Card
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ResultOverlay(
    card: Card,
    onSave: (Card) -> Unit,
    onDismiss: () -> Unit,
    matchConfidence: Double = 1.0,
    suggestedAlternatives: List<Card> = emptyList(),
    availableSubsets: List<String> = emptyList()
) {
    var isFavorite by remember { mutableStateOf(card.isFavorite) }
    var selectedSubset by remember { mutableStateOf(card.subset) }
    var showSubsetMenu by remember { mutableStateOf(false) }
    var showDetails by remember { mutableStateOf(true) }
    var showAlternatives by remember { mutableStateOf(false) }
    var currentCard by remember { mutableStateOf(card) }

    val context = LocalContext.current
    val displaySubsets = listOf("Uncategorized") + availableSubsets

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.5f))
        ) {
            Surface(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .fillMaxHeight(0.9f)
                    .clip(RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp)),
                color = Color(0xFF1E293B)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 24.dp)
                        .verticalScroll(rememberScrollState()),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Handle bar
                    Box(
                        modifier = Modifier
                            .padding(top = 12.dp, bottom = 12.dp)
                            .size(width = 40.dp, height = 4.dp)
                            .background(Color.Gray.copy(alpha = 0.5f), CircleShape)
                    )

                    // Close button
                    Box(modifier = Modifier.fillMaxWidth()) {
                        IconButton(
                            onClick = onDismiss,
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .background(Color.Black.copy(alpha = 0.3f), CircleShape)
                                .size(32.dp)
                        ) {
                            Icon(
                                Icons.Default.Close,
                                contentDescription = "Close",
                                tint = Color.White,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }

                    // Confidence Warning
                    if (matchConfidence < 0.7 && suggestedAlternatives.isNotEmpty()) {
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 8.dp)
                                .clickable { showAlternatives = !showAlternatives },
                            color = Color(0xFFFBBF24).copy(alpha = 0.2f),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    Icons.Default.Warning,
                                    contentDescription = null,
                                    tint = Color(0xFFFBBF24),
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "Edition Uncertain (${(matchConfidence * 100).toInt()}% match)",
                                        color = Color.White,
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = "Tap to view ${suggestedAlternatives.size} alternatives",
                                        color = Color.White.copy(alpha = 0.7f),
                                        fontSize = 12.sp
                                    )
                                }
                            }
                        }
                    }

                    // Alternatives List
                    if (showAlternatives && suggestedAlternatives.isNotEmpty()) {
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 8.dp),
                            color = Color(0xFF0F172A).copy(alpha = 0.5f),
                            shape = RoundedCornerShape(16.dp)
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text(
                                    text = "Other Editions:",
                                    color = Color.White,
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(modifier = Modifier.height(8.dp))

                                suggestedAlternatives.take(5).forEach { alt ->
                                    Surface(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 4.dp)
                                            .clickable {
                                                currentCard = alt
                                                showAlternatives = false
                                            },
                                        color = if (alt.id == currentCard.id)
                                            Color(0xFF38BDF8).copy(alpha = 0.2f)
                                        else Color.Transparent,
                                        shape = RoundedCornerShape(8.dp)
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(8.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                text = "${alt.setName} (${alt.setCode})",
                                                color = Color.White,
                                                fontSize = 14.sp,
                                                modifier = Modifier.weight(1f)
                                            )
                                            if (alt.id == currentCard.id) {
                                                Text(
                                                    text = "SELECTED",
                                                    color = Color(0xFF38BDF8),
                                                    fontSize = 12.sp,
                                                    fontWeight = FontWeight.Bold
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // Card Image
                    currentCard.imageUrl?.let { url ->
                        AsyncImage(
                            model = url,
                            contentDescription = currentCard.name,
                            modifier = Modifier
                                .fillMaxWidth(0.65f)
                                .aspectRatio(0.715f)
                                .clip(RoundedCornerShape(12.dp)),
                            contentScale = ContentScale.Fit
                        )
                        Spacer(modifier = Modifier.height(20.dp))
                    }

                    // Card Name + Favorite
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = currentCard.name,
                            color = Color.White,
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .background(
                                    if (isFavorite) Color(0xFF00E5FF).copy(alpha = 0.4f)
                                    else Color(0xFF00E5FF).copy(alpha = 0.1f),
                                    CircleShape
                                )
                                .clip(CircleShape)
                                .pointerInput(isFavorite) {
                                    detectTapGestures(
                                        onTap = {
                                            isFavorite = !isFavorite
                                            if (!isFavorite) {
                                                selectedSubset = null
                                            }
                                            onSave(currentCard.copy(isFavorite = isFavorite, subset = selectedSubset))
                                        }
                                    )
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = if (isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                                contentDescription = "Toggle Favorite",
                                tint = Color(0xFF00E5FF),
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }

                    // Set Info
                    Text(
                        text = "${currentCard.setName ?: "Unknown Set"} (${currentCard.setCode?.uppercase() ?: ""}) • ${currentCard.collectorNumber}",
                        color = Color.White.copy(alpha = 0.6f),
                        fontSize = 16.sp
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // Subset Dropdown
                    if (isFavorite) {
                        ExposedDropdownMenuBox(
                            expanded = showSubsetMenu,
                            onExpandedChange = { showSubsetMenu = !showSubsetMenu },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            OutlinedTextField(
                                value = selectedSubset ?: "Uncategorized",
                                onValueChange = {},
                                readOnly = true,
                                label = { Text("Category", color = Color.White.copy(alpha = 0.6f)) },
                                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = showSubsetMenu) },
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedTextColor = Color.White,
                                    unfocusedTextColor = Color.White,
                                    focusedBorderColor = Color(0xFF38BDF8),
                                    unfocusedBorderColor = Color.White.copy(alpha = 0.2f)
                                ),
                                modifier = Modifier.menuAnchor().fillMaxWidth()
                            )
                            ExposedDropdownMenu(
                                expanded = showSubsetMenu,
                                onDismissRequest = { showSubsetMenu = false },
                                modifier = Modifier.background(Color(0xFF1E293B))
                            ) {
                                displaySubsets.forEach { subset ->
                                    DropdownMenuItem(
                                        text = { Text(subset, color = Color.White) },
                                        onClick = {
                                            selectedSubset = if (subset == "Uncategorized") null else subset
                                            showSubsetMenu = false
                                            onSave(currentCard.copy(isFavorite = isFavorite, subset = selectedSubset))
                                        }
                                    )
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                    }

                    if (showDetails) {
                        // --- Market Price Display ---
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 12.dp),
                            color = Color(0xFF38BDF8).copy(alpha = 0.1f),
                            shape = RoundedCornerShape(16.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF38BDF8).copy(alpha = 0.3f))
                        ) {
                            Row(
                                modifier = Modifier.padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text(
                                        text = "Market Price (EUR)",
                                        color = Color.White.copy(alpha = 0.7f),
                                        fontSize = 12.sp
                                    )
                                    Text(
                                        text = if (currentCard.priceEur != null) "€${currentCard.priceEur}" else "N/A",
                                        color = Color(0xFF38BDF8),
                                        fontSize = 32.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }

                        // --- View Price History Button ---
                        Button(
                            onClick = {
                                currentCard.cardmarketUrl?.let { url ->
                                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
                                    context.startActivity(intent)
                                }
                            },
                            enabled = currentCard.cardmarketUrl != null,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 24.dp)
                                .height(50.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFF0F172A),
                                contentColor = Color(0xFF38BDF8),
                                disabledContainerColor = Color(0xFF0F172A).copy(alpha = 0.5f),
                                disabledContentColor = Color(0xFF38BDF8).copy(alpha = 0.3f)
                            ),
                            shape = RoundedCornerShape(12.dp),
                            border = androidx.compose.foundation.BorderStroke(
                                1.dp,
                                if (currentCard.cardmarketUrl != null) Color(0xFF38BDF8).copy(alpha = 0.5f)
                                else Color.Gray.copy(alpha = 0.2f)
                            )
                        ) {
                            Text("View Price History")
                            Spacer(modifier = Modifier.width(8.dp))
                            Icon(Icons.Default.OpenInNew, contentDescription = null, modifier = Modifier.size(16.dp))
                        }

                        // --- Info Grid ---
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            InfoBox(
                                label = "Type",
                                value = currentCard.typeLine.split("—").firstOrNull()?.trim() ?: "Card",
                                modifier = Modifier.weight(1f)
                            )
                            InfoBox(
                                label = "Rarity",
                                value = currentCard.rarity?.replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.ROOT) else it.toString() } ?: "-",
                                valueColor = getRarityColor(currentCard.rarity),
                                modifier = Modifier.weight(1f)
                            )
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // --- Artist & Finish ---
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            color = Color(0xFF0F172A).copy(alpha = 0.5f),
                            shape = RoundedCornerShape(16.dp)
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                DetailLine("Artist", currentCard.artist ?: "Unknown", Color.White)
                                Divider(modifier = Modifier.padding(vertical = 8.dp), color = Color.White.copy(alpha = 0.1f))

                                // Foil Status
                                val isFoil = currentCard.finishes.contains("foil")
                                val isEtched = currentCard.finishes.contains("etched")
                                val finishText = when {
                                    isEtched -> "Etched Foil"
                                    isFoil -> "Foil"
                                    else -> "Non-Foil"
                                }
                                DetailLine("Finish", finishText, if (isFoil || isEtched) Color(0xFFFFD700) else Color.White)
                            }
                        }

                        Spacer(modifier = Modifier.height(24.dp))
                    }

                    // Toggle Details Button
                    Button(
                        onClick = { showDetails = !showDetails },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF334155)),
                        shape = RoundedCornerShape(28.dp)
                    ) {
                        Icon(Icons.Default.Info, contentDescription = null, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(if (showDetails) "Hide Details" else "Show Details", color = Color.White)
                    }

                    Spacer(modifier = Modifier.height(32.dp))
                }
            }
        }
    }
}

@Composable
fun getRarityColor(rarity: String?): Color {
    return when (rarity?.lowercase()) {
        "common" -> Color.White
        "uncommon" -> Color(0xFFC0C0C0) // Silver
        "rare" -> Color(0xFFFFD700) // Gold
        "mythic" -> Color(0xFFFF8C00) // Orange/Red
        else -> Color.White
    }
}

@Composable
fun InfoBox(label: String, value: String, valueColor: Color = Color.White, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier.height(80.dp),
        color = Color(0xFF0F172A).copy(alpha = 0.5f),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.Center
        ) {
            Text(label, color = Color.White.copy(alpha = 0.4f), fontSize = 12.sp)
            Text(value, color = valueColor, fontSize = 16.sp, fontWeight = FontWeight.Bold, maxLines = 1)
        }
    }
}

@Composable
fun DetailLine(label: String, value: String, valueColor: Color) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, color = Color.White.copy(alpha = 0.4f), fontSize = 14.sp)
        Text(value, color = valueColor, fontSize = 14.sp, fontWeight = FontWeight.Medium)
    }
}