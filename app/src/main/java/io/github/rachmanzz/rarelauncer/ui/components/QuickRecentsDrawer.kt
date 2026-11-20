package io.github.rachmanzz.rarelauncer.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyHorizontalGrid
import androidx.compose.foundation.lazy.grid.items as gridItems
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.rachmanzz.rarelauncer.data.model.AppItem
import io.github.rachmanzz.rarelauncer.ui.theme.AetherGlassBorderSubtle
import io.github.rachmanzz.rarelauncer.ui.theme.AetherOnSurface
import io.github.rachmanzz.rarelauncer.ui.theme.AetherOnSurfaceVariant
import io.github.rachmanzz.rarelauncer.ui.theme.AetherPrimary
import io.github.rachmanzz.rarelauncer.ui.theme.AetherSurfaceBright
import io.github.rachmanzz.rarelauncer.ui.theme.AetherSurfaceContainerHigh

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun QuickRecentsDrawer(
    isOpen: Boolean,
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    onClearSearch: () -> Unit,
    bookmarkedApps: List<AppItem>,
    recentApps: List<AppItem>,
    searchResults: List<AppItem>,
    allApps: List<AppItem> = emptyList(),
    bookmarkedPackageNames: List<String>,
    lockedApps: Set<String>,
    onAppClicked: (AppItem) -> Unit,
    onToggleBookmark: (String) -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var selectedAppForMenu by remember { mutableStateOf<AppItem?>(null) }
    var isBookmarkPickerOpen by remember { mutableStateOf(false) }
    var pickerSearchQuery by remember { mutableStateOf("") }

    AnimatedVisibility(
        visible = isOpen,
        enter = slideInVertically(initialOffsetY = { -it }) + fadeIn(),
        exit = slideOutVertically(targetOffsetY = { -it }) + fadeOut(),
        modifier = modifier.fillMaxSize()
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xF80B0E15))
                .statusBarsPadding()
                .navigationBarsPadding()
        ) {
            Column(modifier = Modifier.fillMaxSize()) {

                // --- HEADER & SEARCH BAR ---
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 6.dp)
                        .pointerInput(Unit) {
                            detectVerticalDragGestures { change, dragAmount ->
                                change.consume()
                                if (dragAmount < -25) {
                                    onClose()
                                }
                            }
                        }
                ) {
                    // Quick Recents Header Row
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.History,
                                contentDescription = null,
                                tint = AetherPrimary,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "QUICK ACCESS",
                                color = AetherPrimary,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace,
                                letterSpacing = 1.2.sp
                            )
                        }

                        IconButton(
                            onClick = onClose,
                            modifier = Modifier.size(28.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Close",
                                tint = AetherOnSurfaceVariant,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    // Pinned Search Bar
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(9999.dp))
                            .background(Color(0xFF131720))
                            .border(1.dp, AetherGlassBorderSubtle, RoundedCornerShape(9999.dp))
                            .padding(horizontal = 14.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = "Search",
                            tint = AetherPrimary,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Box(modifier = Modifier.weight(1f)) {
                            if (searchQuery.isEmpty()) {
                                Text(
                                    text = "Search all applications...",
                                    color = AetherOnSurfaceVariant.copy(alpha = 0.6f),
                                    fontSize = 13.sp
                                )
                            }
                            BasicTextField(
                                value = searchQuery,
                                onValueChange = onSearchQueryChange,
                                singleLine = true,
                                textStyle = TextStyle(
                                    color = AetherOnSurface,
                                    fontSize = 13.sp
                                ),
                                cursorBrush = SolidColor(AetherPrimary),
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                        if (searchQuery.isNotEmpty()) {
                            IconButton(
                                onClick = onClearSearch,
                                modifier = Modifier.size(24.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Clear search",
                                    tint = AetherOnSurfaceVariant,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }
                }

                // --- CONTENT AREA ---
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .padding(horizontal = 16.dp),
                    contentPadding = PaddingValues(top = 8.dp, bottom = 24.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // IF SEARCHING: Show only Search Results
                    if (searchQuery.isNotBlank()) {
                        item {
                            Text(
                                text = "SEARCH RESULTS (${searchResults.size})",
                                color = AetherOnSurfaceVariant,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace,
                                letterSpacing = 1.sp
                            )
                        }

                        if (searchResults.isEmpty()) {
                            item {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 32.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = "No applications found for \"$searchQuery\"",
                                        color = AetherOnSurfaceVariant.copy(alpha = 0.6f),
                                        fontSize = 13.sp
                                    )
                                }
                            }
                        } else {
                            items(searchResults, key = { it.packageName }) { app ->
                                val isLocked = app.packageName in lockedApps
                                QuickAppRowItem(
                                    app = app,
                                    isLocked = isLocked,
                                    onClicked = { onAppClicked(app) },
                                    onLongClicked = { selectedAppForMenu = app }
                                )
                            }
                        }
                    } else {
                        // DEFAULT VIEW: Top Bookmarks, then Recents (max 10)

                        // 1. TOP BOOKMARKED APPS SECTION
                        item {
                            Column(modifier = Modifier.fillMaxWidth()) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            imageVector = Icons.Default.Star,
                                            contentDescription = null,
                                            tint = Color(0xFFFFD54F),
                                            modifier = Modifier.size(14.dp)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = "BOOKMARKS",
                                            color = AetherOnSurfaceVariant,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            fontFamily = FontFamily.Monospace,
                                            letterSpacing = 1.sp
                                        )
                                    }

                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Text(
                                            text = "${bookmarkedApps.size}",
                                            color = AetherOnSurfaceVariant.copy(alpha = 0.6f),
                                            fontSize = 11.sp,
                                            fontFamily = FontFamily.Monospace
                                        )
                                        Box(
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(6.dp))
                                                .background(AetherSurfaceBright.copy(alpha = 0.4f))
                                                .clickable {
                                                    pickerSearchQuery = ""
                                                    isBookmarkPickerOpen = true
                                                }
                                                .padding(horizontal = 8.dp, vertical = 3.dp)
                                        ) {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Icon(
                                                    imageVector = Icons.Default.Add,
                                                    contentDescription = "Pilih",
                                                    tint = AetherPrimary,
                                                    modifier = Modifier.size(12.dp)
                                                )
                                                Spacer(modifier = Modifier.width(3.dp))
                                                Text(
                                                    text = "Pilih",
                                                    color = AetherPrimary,
                                                    fontSize = 11.sp,
                                                    fontWeight = FontWeight.SemiBold
                                                )
                                            }
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.height(10.dp))

                                if (bookmarkedApps.isEmpty()) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(12.dp))
                                            .background(Color(0xFF131720))
                                            .border(1.dp, AetherGlassBorderSubtle, RoundedCornerShape(12.dp))
                                            .clickable {
                                                pickerSearchQuery = ""
                                                isBookmarkPickerOpen = true
                                            }
                                            .padding(vertical = 14.dp, horizontal = 16.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(
                                                imageVector = Icons.Default.Add,
                                                contentDescription = null,
                                                tint = AetherPrimary,
                                                modifier = Modifier.size(16.dp)
                                            )
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text(
                                                text = "Ketuk untuk memilih aplikasi Bookmark",
                                                color = AetherPrimary,
                                                fontSize = 12.sp,
                                                textAlign = TextAlign.Center
                                            )
                                        }
                                    }
                                } else {
                                    LazyHorizontalGrid(
                                        rows = GridCells.Fixed(2),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(156.dp),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                        verticalArrangement = Arrangement.spacedBy(8.dp),
                                        contentPadding = PaddingValues(horizontal = 2.dp)
                                    ) {
                                        gridItems(bookmarkedApps.take(10), key = { it.packageName }) { app ->
                                            val isLocked = app.packageName in lockedApps
                                            BookmarkedAppGridItem(
                                                app = app,
                                                isLocked = isLocked,
                                                onClicked = { onAppClicked(app) },
                                                onLongClicked = { selectedAppForMenu = app }
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        // 2. RECENTLY USED APPS (MAX 20)
                        item {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.History,
                                        contentDescription = null,
                                        tint = AetherPrimary,
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "RECENTLY USED (MAX 20)",
                                        color = AetherOnSurfaceVariant,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        fontFamily = FontFamily.Monospace,
                                        letterSpacing = 1.sp
                                    )
                                }
                                Text(
                                    text = "${recentApps.size}/20",
                                    color = AetherOnSurfaceVariant.copy(alpha = 0.6f),
                                    fontSize = 11.sp,
                                    fontFamily = FontFamily.Monospace
                                )
                            }
                        }

                        if (recentApps.isEmpty()) {
                            item {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(Color(0xFF131720))
                                        .border(1.dp, AetherGlassBorderSubtle, RoundedCornerShape(12.dp))
                                        .padding(vertical = 20.dp, horizontal = 16.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = "Belum ada aplikasi yang baru dibuka",
                                        color = AetherOnSurfaceVariant.copy(alpha = 0.6f),
                                        fontSize = 12.sp
                                    )
                                }
                            }
                        } else {
                            items(recentApps, key = { it.packageName }) { app ->
                                val isLocked = app.packageName in lockedApps
                                QuickAppRowItem(
                                    app = app,
                                    isLocked = isLocked,
                                    onClicked = { onAppClicked(app) },
                                    onLongClicked = { selectedAppForMenu = app }
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    // App Context Dialog for Bookmark Toggle
    if (selectedAppForMenu != null) {
        val app = selectedAppForMenu!!
        val isBookmarked = app.packageName in bookmarkedPackageNames

        AlertDialog(
            onDismissRequest = { selectedAppForMenu = null },
            title = { Text(text = app.label) },
            text = { Text(text = "Pilih aksi untuk aplikasi:") },
            confirmButton = {
                TextButton(
                    onClick = {
                        onToggleBookmark(app.packageName)
                        selectedAppForMenu = null
                    }
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = if (isBookmarked) Icons.Default.BookmarkBorder else Icons.Default.Bookmark,
                            contentDescription = null,
                            tint = Color(0xFFFFD54F),
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (isBookmarked) "Hapus dari Bookmark" else "Tambah ke Bookmark",
                            color = Color(0xFFFFD54F)
                        )
                    }
                }
            },
            dismissButton = {
                TextButton(onClick = { selectedAppForMenu = null }) {
                    Text(text = "Batal")
                }
            }
        )
    }

    // Bookmark Picker Dialog
    if (isBookmarkPickerOpen) {
        val filtered = if (pickerSearchQuery.isBlank()) {
            allApps
        } else {
            val q = pickerSearchQuery.trim().lowercase()
            allApps.filter { it.label.lowercase().contains(q) || it.packageName.lowercase().contains(q) }
        }

        AlertDialog(
            onDismissRequest = { isBookmarkPickerOpen = false },
            title = {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(text = "Kelola Bookmark", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    Text(
                        text = "${bookmarkedPackageNames.size} dipilih",
                        color = Color(0xFFFFD54F),
                        fontSize = 12.sp,
                        fontFamily = FontFamily.Monospace
                    )
                }
            },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(360.dp)
                ) {
                    // Search bar inside picker
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color(0xFF131720))
                            .border(1.dp, AetherGlassBorderSubtle, RoundedCornerShape(8.dp))
                            .padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = null,
                            tint = AetherPrimary,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Box(modifier = Modifier.weight(1f)) {
                            if (pickerSearchQuery.isEmpty()) {
                                Text(
                                    text = "Cari aplikasi...",
                                    color = AetherOnSurfaceVariant.copy(alpha = 0.5f),
                                    fontSize = 12.sp
                                )
                            }
                            BasicTextField(
                                value = pickerSearchQuery,
                                onValueChange = { pickerSearchQuery = it },
                                singleLine = true,
                                textStyle = TextStyle(color = AetherOnSurface, fontSize = 12.sp),
                                cursorBrush = SolidColor(AetherPrimary),
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                        if (pickerSearchQuery.isNotEmpty()) {
                            IconButton(
                                onClick = { pickerSearchQuery = "" },
                                modifier = Modifier.size(20.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Clear",
                                    tint = AetherOnSurfaceVariant,
                                    modifier = Modifier.size(14.dp)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        items(filtered, key = { it.packageName }) { app ->
                            val isBookmarked = app.packageName in bookmarkedPackageNames
                            val isMaxReached = !isBookmarked && bookmarkedPackageNames.size >= 10

                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(
                                        when {
                                            isBookmarked -> Color(0x26FFD54F)
                                            isMaxReached -> Color(0xFF131720).copy(alpha = 0.5f)
                                            else -> Color(0xFF131720)
                                        }
                                    )
                                    .border(
                                        1.dp,
                                        if (isBookmarked) Color(0x66FFD54F) else Color.Transparent,
                                        RoundedCornerShape(10.dp)
                                    )
                                    .clickable {
                                        if (isBookmarked || !isMaxReached) {
                                            onToggleBookmark(app.packageName)
                                        }
                                    }
                                    .padding(horizontal = 10.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                if (app.icon != null) {
                                    Image(
                                        bitmap = app.icon,
                                        contentDescription = app.label,
                                        modifier = Modifier.size(28.dp)
                                    )
                                } else {
                                    Box(
                                        modifier = Modifier
                                            .size(28.dp)
                                            .clip(RoundedCornerShape(6.dp))
                                            .background(AetherSurfaceBright),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = app.label.take(1).uppercase(),
                                            color = AetherPrimary,
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.width(10.dp))

                                Text(
                                    text = app.label,
                                    color = when {
                                        isBookmarked -> Color(0xFFFFD54F)
                                        isMaxReached -> AetherOnSurfaceVariant.copy(alpha = 0.4f)
                                        else -> AetherOnSurface
                                    },
                                    fontSize = 13.sp,
                                    fontWeight = if (isBookmarked) FontWeight.SemiBold else FontWeight.Normal,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.weight(1f)
                                )

                                if (isMaxReached) {
                                    Text(
                                        text = "MAX 10",
                                        color = AetherOnSurfaceVariant.copy(alpha = 0.5f),
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Bold,
                                        fontFamily = FontFamily.Monospace,
                                        modifier = Modifier.padding(end = 4.dp)
                                    )
                                }

                                Icon(
                                    imageVector = if (isBookmarked) Icons.Default.Bookmark else Icons.Default.BookmarkBorder,
                                    contentDescription = null,
                                    tint = when {
                                        isBookmarked -> Color(0xFFFFD54F)
                                        isMaxReached -> AetherOnSurfaceVariant.copy(alpha = 0.2f)
                                        else -> AetherOnSurfaceVariant.copy(alpha = 0.4f)
                                    },
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { isBookmarkPickerOpen = false }) {
                    Text(text = "Selesai")
                }
            }
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun BookmarkedAppGridItem(
    app: AppItem,
    isLocked: Boolean,
    onClicked: () -> Unit,
    onLongClicked: () -> Unit,
) {
    Column(
        modifier = Modifier
            .width(66.dp)
            .height(72.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(Color(0xFF131720))
            .combinedClickable(
                onClick = onClicked,
                onLongClick = onLongClicked
            )
            .padding(vertical = 6.dp, horizontal = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(AetherSurfaceContainerHigh),
            contentAlignment = Alignment.Center
        ) {
            if (app.icon != null) {
                Image(
                    bitmap = app.icon,
                    contentDescription = app.label,
                    modifier = Modifier.size(26.dp)
                )
            } else {
                Text(
                    text = app.label.take(1).uppercase(),
                    color = AetherPrimary,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            if (isLocked) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(2.dp)
                        .size(12.dp)
                        .clip(RoundedCornerShape(9999.dp))
                        .background(Color(0xCC000000)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Lock,
                        contentDescription = "Locked",
                        tint = Color(0xFFFFD54F),
                        modifier = Modifier.size(8.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(3.dp))

        Text(
            text = app.label,
            color = AetherOnSurface,
            fontSize = 10.sp,
            fontWeight = FontWeight.Medium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun QuickAppRowItem(
    app: AppItem,
    isLocked: Boolean,
    onClicked: () -> Unit,
    onLongClicked: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(Color(0xFF131720))
            .combinedClickable(
                onClick = onClicked,
                onLongClick = onLongClicked
            )
            .padding(horizontal = 12.dp, vertical = 9.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.weight(1f)
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(AetherSurfaceContainerHigh),
                contentAlignment = Alignment.Center
            ) {
                if (app.icon != null) {
                    Image(
                        bitmap = app.icon,
                        contentDescription = app.label,
                        modifier = Modifier.size(28.dp)
                    )
                } else {
                    Text(
                        text = app.label.take(1).uppercase(),
                        color = AetherPrimary,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = app.label,
                color = AetherOnSurface,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )
        }

        if (isLocked) {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(9999.dp))
                    .background(Color(0x26FFD54F))
                    .border(1.dp, Color(0x66FFD54F), RoundedCornerShape(9999.dp))
                    .padding(horizontal = 6.dp, vertical = 2.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(3.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Lock,
                        contentDescription = "Locked",
                        tint = Color(0xFFFFD54F),
                        modifier = Modifier.size(10.dp)
                    )
                    Text(
                        text = "LOCKED",
                        color = Color(0xFFFFD54F),
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }
        }
    }
}
