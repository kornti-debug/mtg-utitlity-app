package com.example.mtgutilityapp.ui.favorites

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
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

@Composable
fun FavoritesScreen(
    viewModel: FavoritesViewModel,
    onNavigateToScan: () -> Unit,
    onNavigateToHistory: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    var showSubsetDialogForCard by remember { mutableStateOf<Card?>(null) }
    var showAddSubsetDialog by remember { mutableStateOf(false) }

    Box(modifier = Modifier.fillMaxSize().background(Color(0xFF0F172A))) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp)
        ) {
            Spacer(modifier = Modifier.height(64.dp))

            // Header
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(
                    Icons.Default.Favorite,
                    contentDescription = null,
                    tint = Color(0xFF38BDF8),
                    modifier = Modifier.size(32.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = "Favorites",
                    color = Color.White,
                    fontSize = 32.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            // Subset Selector (Horizontal scroll for categories)
            Row(
                modifier = Modifier.padding(vertical = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                LazyRow(
                    modifier = Modifier.weight(1f),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // 1. "Uncategorized" Button (Default)
                    item {
                        SubsetTab(
                            name = "Uncategorized",
                            isSelected = uiState.selectedSubset == "Uncategorized",
                            onClick = { viewModel.selectSubset("Uncategorized") },
                            onDelete = null
                        )
                    }

                    // 2. Dynamic Category Buttons
                    items(uiState.subsets) { subset ->
                        SubsetTab(
                            name = subset,
                            isSelected = uiState.selectedSubset == subset,
                            onClick = { viewModel.selectSubset(subset) },
                            onDelete = { viewModel.deleteSubset(subset) }
                        )
                    }
                }

                // 3. Add Category Button
                IconButton(
                    onClick = { showAddSubsetDialog = true },
                    modifier = Modifier
                        .padding(start = 8.dp)
                        .size(36.dp)
                        .background(Color(0xFF38BDF8).copy(alpha = 0.2f), CircleShape)
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "Add Category",
                        tint = Color(0xFF38BDF8),
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            Text(
                text = if (uiState.selectedSubset == "Uncategorized")
                    "Cards not in any category"
                else "Category: ${uiState.selectedSubset}",
                color = Color.White.copy(alpha = 0.5f),
                fontSize = 14.sp,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            // List
            if (uiState.isLoading) {
                Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = Color(0xFF38BDF8))
                }
            } else if (uiState.cards.isEmpty()) {
                Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                    Text("No cards in this section", color = Color.White.copy(alpha = 0.4f))
                }
            } else {
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(bottom = 100.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    items(uiState.cards, key = { it.scanId }) { card ->
                        FavoriteCardItem(
                            card = card,
                            onClick = { viewModel.selectCard(card) },
                            onFavoriteToggle = { viewModel.toggleFavorite(card) },
                            onLongPressHeart = { showSubsetDialogForCard = card }
                        )
                    }
                }
            }
        }

        // Add Subset Dialog
        if (showAddSubsetDialog) {
            AddSubsetDialog(
                onDismiss = { showAddSubsetDialog = false },
                onAdd = { name ->
                    viewModel.addSubset(name)
                    showAddSubsetDialog = false
                }
            )
        }

        // Subset Selection Dialog
        showSubsetDialogForCard?.let { card ->
            SubsetSelectionDialog(
                currentSubset = card.subset,
                onDismiss = { showSubsetDialogForCard = null },
                onSubsetSelected = { subset ->
                    viewModel.updateCardSubset(card, subset)
                    showSubsetDialogForCard = null
                }
            )
        }

        // Bottom Navigation
        CustomBottomNavigation(
            modifier = Modifier.align(Alignment.BottomCenter),
            onHistoryClick = onNavigateToHistory,
            onFavoritesClick = { /* Already here */ },
            onScanClick = onNavigateToScan,
            activeScreen = "Favorites"
        )

        // Detail Overlay
        uiState.selectedCard?.let { card ->
            ResultOverlay(
                card = card,
                onSave = { updatedCard ->
                    viewModel.updateCardSubset(updatedCard, updatedCard.subset)
                    if (!updatedCard.isFavorite) {
                        viewModel.dismissCard()
                    }
                },
                onDismiss = { viewModel.dismissCard() }
            )
        }
    }
}

@Composable
fun AddSubsetDialog(onDismiss: () -> Unit, onAdd: (String) -> Unit) {
    var name by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("New Category", color = Color.White) },
        containerColor = Color(0xFF1E293B),
        text = {
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Category Name") },
                modifier = Modifier.fillMaxWidth(),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White,
                    focusedBorderColor = Color(0xFF38BDF8)
                )
            )
        },
        confirmButton = {
            TextButton(onClick = { if (name.isNotBlank()) onAdd(name) }) {
                Text("ADD", color = Color(0xFF38BDF8))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("CANCEL", color = Color.White)
            }
        }
    )
}

@Composable
fun SubsetTab(
    name: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    onDelete: (() -> Unit)?
) {
    var showDeleteIcon by remember { mutableStateOf(false) }

    Surface(
        modifier = Modifier
            .pointerInput(Unit) {
                detectTapGestures(
                    onTap = {
                        if (showDeleteIcon) showDeleteIcon = false
                        else onClick()
                    },
                    onLongPress = {
                        if (onDelete != null) showDeleteIcon = true
                    }
                )
            },
        color = if (isSelected) Color(0xFF38BDF8) else Color(0xFF1E293B),
        shape = RoundedCornerShape(20.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
            Text(
                text = name,
                color = if (isSelected) Color.Black else Color.White,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium
            )

            if (showDeleteIcon && onDelete != null) {
                Spacer(modifier = Modifier.width(8.dp))
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Delete",
                    tint = if (isSelected) Color.Black else Color.White,
                    modifier = Modifier
                        .size(16.dp)
                        .clickable {
                            onDelete()
                            showDeleteIcon = false
                        }
                )
            }
        }
    }
}

@Composable
fun FavoriteCardItem(
    card: Card,
    onClick: () -> Unit,
    onFavoriteToggle: () -> Unit,
    onLongPressHeart: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        color = Color(0xFF1E293B),
        shape = RoundedCornerShape(24.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            AsyncImage(
                model = card.imageUrl,
                contentDescription = card.name,
                modifier = Modifier
                    .size(width = 60.dp, height = 84.dp)
                    .clip(RoundedCornerShape(8.dp)),
                contentScale = ContentScale.Crop
            )

            Spacer(modifier = Modifier.width(16.dp))

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
                if (card.subset != null) {
                    Text(
                        text = "Category: ${card.subset}",
                        color = Color(0xFF38BDF8),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            // Favorite Toggle with Long Press
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .background(
                        if (card.isFavorite) Color(0xFF00E5FF).copy(alpha = 0.2f)
                        else Color.Transparent,
                        CircleShape
                    )
                    .clip(CircleShape)
                    // FIX: Pass 'card' as key to ensure it refreshes on change
                    .pointerInput(card) {
                        detectTapGestures(
                            onTap = { onFavoriteToggle() },
                            onLongPress = { onLongPressHeart() }
                        )
                    },
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

@Composable
fun SubsetSelectionDialog(
    currentSubset: String?,
    onDismiss: () -> Unit,
    onSubsetSelected: (String?) -> Unit
) {
    var newSubset by remember { mutableStateOf("") }
    val predefinedSubsets = listOf("Cheap", "Expensive", "Commander", "Modern")

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Move to Category", color = Color.White) },
        containerColor = Color(0xFF1E293B),
        text = {
            Column {
                Text("Select an existing category or create a new one:", color = Color.White.copy(alpha = 0.7f))
                Spacer(modifier = Modifier.height(16.dp))

                // Existing Subsets
                predefinedSubsets.forEach { subset ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onSubsetSelected(subset) }
                            .padding(vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Folder, contentDescription = null, tint = Color(0xFF38BDF8))
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(subset, color = Color.White)
                    }
                }

                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp), color = Color.White.copy(alpha = 0.1f))

                // Custom Subset Input
                OutlinedTextField(
                    value = newSubset,
                    onValueChange = { newSubset = it },
                    placeholder = { Text("New Category Name") },
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedBorderColor = Color(0xFF38BDF8)
                    )
                )
            }
        },
        confirmButton = {
            TextButton(onClick = { if (newSubset.isNotBlank()) onSubsetSelected(newSubset) }) {
                Text("ADD NEW", color = Color(0xFF38BDF8))
            }
        },
        dismissButton = {
            TextButton(onClick = { onSubsetSelected(null) }) {
                Text("Uncategorized", color = Color.Red.copy(alpha = 0.7f))
            }
        }
    )
}