package com.example.mtgutilityapp.ui.history

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.History
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.mtgutilityapp.domain.model.Card
import com.example.mtgutilityapp.ui.camera.CustomBottomNavigation
import com.example.mtgutilityapp.ui.result.ResultOverlay
import java.text.SimpleDateFormat
import java.util.*
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.rememberSwipeToDismissBoxState

@Composable
fun HistoryScreen(
    viewModel: HistoryViewModel,
    onNavigateBack: () -> Unit,
    onNavigateToScan: () -> Unit,
    onNavigateToFavorites: () -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsState()

    var showDeleteAllDialog by remember { mutableStateOf(false) }

    Box(modifier = Modifier.fillMaxSize().background(Color(0xFF0F172A))) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp)
        ) {
            Spacer(modifier = Modifier.height(64.dp))

            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
            // Header with Back Button
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onNavigateBack) {
                    Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = Color.White)
                }
                Spacer(modifier = Modifier.width(8.dp))
                Icon(
                    Icons.Default.History,
                    contentDescription = null,
                    tint = Color(0xFF38BDF8),
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Scan History",
                    color = Color.White,
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Bold
                )
            }
                Spacer(modifier = Modifier.weight(1f)) // Pushes the next item to the far right

                // Right side: Delete All Button (Only show if list is not empty)
                if (uiState.cards.isNotEmpty()) {
                    IconButton(onClick = { showDeleteAllDialog = true }) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Clear History",
                            tint = Color.Red.copy(alpha = 0.8f)
                        )
                    }
                }
            }

            Text(
                text = "${uiState.cards.size} cards scanned",
                color = Color.White.copy(alpha = 0.5f),
                fontSize = 16.sp,
                modifier = Modifier.padding(top = 8.dp, start = 48.dp)
            )

            Spacer(modifier = Modifier.height(24.dp))

            // History List
            if (uiState.isLoading) {
                Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = Color(0xFF38BDF8))
                }
            } else if (uiState.cards.isEmpty()) {
                Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                    Text("No cards scanned yet", color = Color.White.copy(alpha = 0.4f))
                }
            } else {
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(bottom = 100.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Inside LazyColumn...
                    items(uiState.cards, key = { it.scanId }) { card ->
                        // 1. Setup the Swipe State
                        val dismissState = rememberSwipeToDismissBoxState(
                            confirmValueChange = {
                                if (it == SwipeToDismissBoxValue.EndToStart) {
                                    // This triggers the delete in the database
                                    viewModel.deleteCard(card)
                                    true
                                } else {
                                    false
                                }
                            }
                        )

                        // 2. Wrap the item in the Swipe Box
                        SwipeToDismissBox(
                            state = dismissState,
                            enableDismissFromStartToEnd = false, // Only swipe left to delete
                            backgroundContent = {
                                // The red background behind the card
                                val color = Color.Red.copy(alpha = 0.8f)
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .padding(vertical = 4.dp) // Match card margin
                                        .background(color, RoundedCornerShape(24.dp))
                                        .padding(horizontal = 24.dp),
                                    contentAlignment = Alignment.CenterEnd
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Delete,
                                        contentDescription = "Delete",
                                        tint = Color.White
                                    )
                                }
                            },
                            content = {
                                // Your existing card item
                                HistoryCardItem(
                                    card = card,
                                    onClick = { viewModel.selectCard(card) },
                                    onFavoriteToggle = { viewModel.toggleFavorite(card) }
                                )
                            }
                        )
                    }
                }
            }
        }

        if (showDeleteAllDialog) {
            AlertDialog(
                onDismissRequest = { showDeleteAllDialog = false },
                title = { Text("Clear History?", color = Color.White) },
                text = { Text("This will delete all scanned cards. Your Favorites will remain safe.", color = Color.White.copy(alpha = 0.7f)) },
                containerColor = Color(0xFF1E293B),
                confirmButton = {
                    TextButton(
                        onClick = {
                            viewModel.deleteAllCards() // Function already exists in your VM!
                            showDeleteAllDialog = false
                        }
                    ) {
                        Text("DELETE ALL", color = Color.Red)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showDeleteAllDialog = false }) {
                        Text("Cancel", color = Color.White)
                    }
                }
            )
        }

        // Bottom Navigation
        CustomBottomNavigation(
            modifier = Modifier.align(Alignment.BottomCenter),
            onHistoryClick = { /* Already here */ },
            onFavoritesClick = onNavigateToFavorites,
            onScanClick = onNavigateToScan,
            activeScreen = "History"
        )

        // Detail Overlay
        uiState.selectedCard?.let { card ->
            ResultOverlay(
                card = card,
                onSave = { updatedCard -> 
                    viewModel.updateCard(updatedCard)
                },
                onDismiss = { viewModel.dismissCard() }
            )
        }
    }
}

@Composable
fun HistoryCardItem(
    card: Card, 
    onClick: () -> Unit,
    onFavoriteToggle: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        color = Color(0xFF1E293B), // Card background
        shape = RoundedCornerShape(24.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Thumbnail
            AsyncImage(
                model = card.imageUrl,
                contentDescription = card.name,
                modifier = Modifier
                    .size(width = 60.dp, height = 84.dp)
                    .clip(RoundedCornerShape(8.dp)),
                contentScale = ContentScale.Crop
            )

            Spacer(modifier = Modifier.width(16.dp))

            // Content
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = card.name,
                    color = Color.White,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = card.setName ?: "Unknown Set",
                    color = Color.White.copy(alpha = 0.5f),
                    fontSize = 14.sp
                )
                Text(
                    text = "Scanned ${formatTime(card.scannedAt)}",
                    color = Color.White.copy(alpha = 0.4f),
                    fontSize = 12.sp,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }

            // Favorite Toggle
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .background(
                        if (card.isFavorite) Color(0xFF00E5FF).copy(alpha = 0.2f) 
                        else Color.Transparent, 
                        CircleShape
                    )
                    .clip(CircleShape)
                    .clickable { onFavoriteToggle() },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (card.isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                    contentDescription = "Favorite",
                    tint = if (card.isFavorite) Color(0xFF00E5FF) else Color.White.copy(alpha = 0.4f),
                    modifier = Modifier.size(20.dp)
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            // Mana Cost Tag
            if (!card.manaCost.isNullOrBlank()) {
                Surface(
                    color = Color(0xFF0EA5E9).copy(alpha = 0.1f),
                    shape = RoundedCornerShape(8.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF0EA5E9).copy(alpha = 0.3f))
                ) {
                    Text(
                        text = card.manaCost ?: "",
                        color = Color(0xFF38BDF8),
                        fontSize = 12.sp,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
    }
}

private fun formatTime(timestamp: Long): String {
    val now = Calendar.getInstance()
    val time = Calendar.getInstance().apply { timeInMillis = timestamp }
    
    return when {
        now.get(Calendar.DATE) == time.get(Calendar.DATE) -> {
            "Today, " + SimpleDateFormat("h:mm a", Locale.getDefault()).format(Date(timestamp))
        }
        now.get(Calendar.DATE) - time.get(Calendar.DATE) == 1 -> {
            "Yesterday, " + SimpleDateFormat("h:mm a", Locale.getDefault()).format(Date(timestamp))
        }
        else -> SimpleDateFormat("MMM d, h:mm a", Locale.getDefault()).format(Date(timestamp))
    }
}
