package io.github.rachmanzz.rarelauncer.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
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
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyHorizontalGrid
import androidx.compose.foundation.lazy.grid.items as gridItems
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.PlaylistAdd
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
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
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.rachmanzz.rarelauncer.data.model.AppItem
import io.github.rachmanzz.rarelauncer.ui.theme.AetherGlassBorder
import io.github.rachmanzz.rarelauncer.ui.theme.AetherGlassBorderSubtle
import io.github.rachmanzz.rarelauncer.ui.theme.AetherOnSurface
import io.github.rachmanzz.rarelauncer.ui.theme.AetherOnSurfaceVariant
import io.github.rachmanzz.rarelauncer.ui.theme.AetherPrimary
import io.github.rachmanzz.rarelauncer.ui.theme.AetherSurfaceBright
import io.github.rachmanzz.rarelauncer.ui.theme.AetherSurfaceContainerHigh
import io.github.rachmanzz.rarelauncer.ui.theme.RareLauncerTheme
import io.github.rachmanzz.rarelauncer.ui.viewmodel.DrawerTab

@Composable
fun ScrollCueIndicator(
    onScrollCueClicked: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clickable { onScrollCueClicked() }
            .padding(vertical = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .width(42.dp)
                .height(4.dp)
                .clip(RoundedCornerShape(9999.dp))
                .background(AetherSurfaceBright)
        )
        Spacer(modifier = Modifier.height(6.dp))
        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Scroll down for all apps",
                color = AetherOnSurfaceVariant,
                fontSize = 11.sp,
                fontFamily = FontFamily.Monospace,
                letterSpacing = 0.5.sp
            )
            Spacer(modifier = Modifier.width(4.dp))
            Icon(
                imageVector = Icons.Default.KeyboardArrowDown,
                contentDescription = null,
                tint = AetherPrimary,
                modifier = Modifier.size(16.dp)
            )
        }
    }
}

/**
 * Slide-Up Full App Drawer
 * - Slides full up from bottom
 * - Search bar is PINNED at the top (never scrolls off screen)
 * - Fingerprint-protected Hidden Vault tab
 * - Only the application items inside LazyColumn scroll
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun AllAppsDrawer(
    isOpen: Boolean,
    apps: List<AppItem>,
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    onClearSearch: () -> Unit,
    onAppClicked: (AppItem) -> Unit,
    onAppInfoClicked: (String) -> Unit,
    onAddAppToHome: (String) -> Unit,
    onRemoveAppFromHome: ((String) -> Unit)? = null,
    directoryPackageNames: List<String> = emptyList(),
    drawerTab: DrawerTab = DrawerTab.ACTIVE,
    isVaultUnlocked: Boolean = false,
    hiddenAppsCount: Int = 0,
    onTabSelected: (DrawerTab) -> Unit = {},
    onRequestUnlockVault: () -> Unit = {},
    onLockVault: () -> Unit = {},
    onHideApp: (String) -> Unit = {},
    onUnhideApp: (String) -> Unit = {},
    lockedApps: Set<String> = emptySet(),
    onLockApp: (String) -> Unit = {},
    onUnlockApp: (String) -> Unit = {},
    bookmarkedApps: List<AppItem> = emptyList(),
    recentApps: List<AppItem> = emptyList(),
    allApps: List<AppItem> = emptyList(),
    bookmarkedPackageNames: List<String> = emptyList(),
    onToggleBookmark: (String) -> Unit = {},
    onCloseDrawer: () -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedAppForMenu by remember { mutableStateOf<AppItem?>(null) }
    var isBookmarkPickerOpen by remember { mutableStateOf(false) }
    var pickerSearchQuery by remember { mutableStateOf("") }
    val lazyListState = rememberLazyListState()

    AnimatedVisibility(
        visible = isOpen,
        enter = slideInVertically(
            initialOffsetY = { it },
            animationSpec = tween(350, easing = FastOutSlowInEasing)
        ) + fadeIn(animationSpec = tween(250)),
        exit = slideOutVertically(
            targetOffsetY = { it },
            animationSpec = tween(300, easing = FastOutSlowInEasing)
        ) + fadeOut(animationSpec = tween(200)),
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

                // --- TOP FIXED / PINNED SECTION (NEVER SCROLLS) ---
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 6.dp)
                        .pointerInput(Unit) {
                            detectVerticalDragGestures { change, dragAmount ->
                                change.consume()
                                if (dragAmount > 25) {
                                    onCloseDrawer()
                                }
                            }
                        }
                ) {
                    // Top drag pill handle
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .width(44.dp)
                                .height(4.dp)
                                .clip(RoundedCornerShape(9999.dp))
                                .background(AetherSurfaceBright)
                                .clickable { onCloseDrawer() }
                        )
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    // Pinned Search Bar (Always at top!)
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
                                    text = if (drawerTab == DrawerTab.ACTIVE) "Search ${apps.size} applications..." else "Search hidden apps...",
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

                    Spacer(modifier = Modifier.height(10.dp))

                    // Segmented Filter Tabs: Active Apps | 🔒 Hidden Vault
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            modifier = Modifier
                                .clip(RoundedCornerShape(9999.dp))
                                .background(Color(0xFF131720))
                                .border(1.dp, AetherGlassBorderSubtle, RoundedCornerShape(9999.dp))
                                .padding(2.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Active Apps Tab
                            val isActiveTab = drawerTab == DrawerTab.ACTIVE
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(9999.dp))
                                    .background(if (isActiveTab) AetherSurfaceBright else Color.Transparent)
                                    .clickable { onTabSelected(DrawerTab.ACTIVE) }
                                    .padding(horizontal = 12.dp, vertical = 5.dp)
                            ) {
                                Text(
                                    text = "APPS (${apps.size})",
                                    color = if (isActiveTab) AetherPrimary else AetherOnSurfaceVariant,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = FontFamily.Monospace
                                )
                            }

                            // Hidden Vault Tab (Fingerprint Protected)
                            val isHiddenTab = drawerTab == DrawerTab.HIDDEN
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(9999.dp))
                                    .background(if (isHiddenTab) AetherSurfaceBright else Color.Transparent)
                                    .clickable {
                                        if (!isVaultUnlocked) {
                                            onRequestUnlockVault()
                                        } else {
                                            onTabSelected(DrawerTab.HIDDEN)
                                        }
                                    }
                                    .padding(horizontal = 12.dp, vertical = 5.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Icon(
                                        imageVector = if (isVaultUnlocked) Icons.Default.LockOpen else Icons.Default.Lock,
                                        contentDescription = null,
                                        tint = if (isHiddenTab) AetherPrimary else AetherOnSurfaceVariant,
                                        modifier = Modifier.size(12.dp)
                                    )
                                    Text(
                                        text = "VAULT ($hiddenAppsCount)",
                                        color = if (isHiddenTab) AetherPrimary else AetherOnSurfaceVariant,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        fontFamily = FontFamily.Monospace
                                    )
                                }
                            }
                        }

                        // Re-lock vault button if vault is currently unlocked
                        if (isVaultUnlocked && drawerTab == DrawerTab.HIDDEN) {
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(9999.dp))
                                    .background(Color(0xFF1F1218))
                                    .border(1.dp, Color(0x66FF3366), RoundedCornerShape(9999.dp))
                                    .clickable { onLockVault() }
                                    .padding(horizontal = 10.dp, vertical = 4.dp)
                            ) {
                                Text(
                                    text = "LOCK VAULT",
                                    color = Color(0xFFFF3366),
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = FontFamily.Monospace
                                )
                            }
                        }
                    }
                }

                // Thin separator line
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(1.dp)
                        .background(AetherGlassBorderSubtle)
                )

                // --- CONTENT AREA ---
                if (drawerTab == DrawerTab.HIDDEN && !isVaultUnlocked) {
                    // Fingerprint Locked State
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(16.dp),
                            modifier = Modifier
                                .clip(RoundedCornerShape(20.dp))
                                .background(Color(0xFF131720))
                                .border(1.dp, AetherGlassBorder, RoundedCornerShape(20.dp))
                                .padding(32.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(72.dp)
                                    .clip(CircleShape)
                                    .background(AetherPrimary.copy(alpha = 0.12f))
                                    .border(1.dp, AetherPrimary.copy(alpha = 0.4f), CircleShape)
                                    .clickable { onRequestUnlockVault() },
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Fingerprint,
                                    contentDescription = "Scan Fingerprint",
                                    tint = AetherPrimary,
                                    modifier = Modifier.size(44.dp)
                                )
                            }

                            Text(
                                text = "Hidden Vault Locked",
                                color = AetherOnSurface,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.SemiBold
                            )

                            Text(
                                text = "Scan your fingerprint or enter screen lock credential to view hidden applications.",
                                color = AetherOnSurfaceVariant,
                                fontSize = 12.sp,
                                textAlign = TextAlign.Center
                            )

                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(9999.dp))
                                    .background(AetherPrimary)
                                    .clickable { onRequestUnlockVault() }
                                    .padding(horizontal = 20.dp, vertical = 10.dp)
                            ) {
                                Text(
                                    text = "SCAN FINGERPRINT",
                                    color = Color.Black,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = FontFamily.Monospace
                                )
                            }
                        }
                    }
                } else if (apps.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(vertical = 48.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = if (drawerTab == DrawerTab.HIDDEN) "No hidden applications in vault" else "No matching applications found",
                            color = AetherOnSurfaceVariant,
                            fontSize = 13.sp
                        )
                    }
                } else {
                    // Scrollable List: Bookmarks (2 rows) + Recents (up to 20) when in ACTIVE tab and search is empty, then all apps
                    LazyColumn(
                        state = lazyListState,
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp),
                        contentPadding = PaddingValues(top = 10.dp, bottom = 24.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // 1. BOOKMARKS (2-row horizontal grid, max 10) - shown when in ACTIVE tab and not searching
                        if (drawerTab == DrawerTab.ACTIVE && searchQuery.isEmpty()) {
                            item {
                                Column(modifier = Modifier.fillMaxWidth().padding(bottom = 6.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(
                                                imageVector = Icons.Default.Bookmark,
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
                                                text = "${bookmarkedApps.size}/10",
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

                                    Spacer(modifier = Modifier.height(8.dp))

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
                                                .padding(vertical = 12.dp, horizontal = 16.dp),
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
                                                    text = "Ketuk untuk memilih aplikasi Bookmark (Max 10)",
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
                                            gridItems(bookmarkedApps.take(10), key = { it.packageName }) { bApp ->
                                                val isLocked = bApp.packageName in lockedApps
                                                BookmarkedItem(
                                                    app = bApp,
                                                    isLocked = isLocked,
                                                    onClicked = {
                                                        onCloseDrawer()
                                                        onAppClicked(bApp)
                                                    },
                                                    onLongClicked = { selectedAppForMenu = bApp }
                                                )
                                            }
                                        }
                                    }
                                }
                            }

                            // 2. RECENTLY USED (MAX 20)
                            if (recentApps.isNotEmpty()) {
                                item {
                                    Row(
                                        modifier = Modifier.fillMaxWidth().padding(top = 8.dp, bottom = 4.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(
                                            text = "RECENTLY USED (MAX 20)",
                                            color = AetherOnSurfaceVariant,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            fontFamily = FontFamily.Monospace,
                                            letterSpacing = 1.sp
                                        )
                                        Text(
                                            text = "${recentApps.size}/20",
                                            color = AetherOnSurfaceVariant.copy(alpha = 0.6f),
                                            fontSize = 11.sp,
                                            fontFamily = FontFamily.Monospace
                                        )
                                    }
                                }

                                items(recentApps.take(20), key = { "recent_${it.packageName}" }) { rApp ->
                                    val isInDirectory = rApp.packageName in directoryPackageNames
                                    val isLocked = rApp.packageName in lockedApps
                                    AppRowItem(
                                        app = rApp,
                                        isInDirectory = isInDirectory,
                                        isHidden = false,
                                        isLocked = isLocked,
                                        onClicked = {
                                            onCloseDrawer()
                                            onAppClicked(rApp)
                                        },
                                        onLongClicked = { selectedAppForMenu = rApp }
                                    )
                                }
                            }
                        }

                        // ALL / FILTERED APPS LIST - shown ONLY when searching or inside HIDDEN VAULT
                        if (searchQuery.isNotEmpty() || drawerTab == DrawerTab.HIDDEN) {
                            if (searchQuery.isNotEmpty()) {
                                item {
                                    Text(
                                        text = "SEARCH RESULTS (${apps.size})",
                                        color = AetherOnSurfaceVariant,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        fontFamily = FontFamily.Monospace,
                                        letterSpacing = 1.sp,
                                        modifier = Modifier.padding(top = 4.dp, bottom = 4.dp)
                                    )
                                }
                            }

                            items(apps, key = { it.packageName }) { app ->
                                val isInDirectory = app.packageName in directoryPackageNames
                                val isLocked = app.packageName in lockedApps
                                AppRowItem(
                                    app = app,
                                    isInDirectory = isInDirectory,
                                    isHidden = drawerTab == DrawerTab.HIDDEN,
                                    isLocked = isLocked,
                                    onClicked = {
                                        onCloseDrawer()
                                        onAppClicked(app)
                                    },
                                    onLongClicked = { selectedAppForMenu = app }
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    // App Long-Press Menu
    if (selectedAppForMenu != null) {
        val app = selectedAppForMenu!!
        val isInDirectory = app.packageName in directoryPackageNames
        val isHidden = drawerTab == DrawerTab.HIDDEN
        val isLocked = app.packageName in lockedApps

        AlertDialog(
            onDismissRequest = { selectedAppForMenu = null },
            title = {
                Text(
                    text = app.label,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = AetherOnSurface
                )
            },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    // Per-App Lock (Fingerprint / PIN) - keeps app visible in list & directory
                    TextButton(
                        onClick = {
                            if (isLocked) {
                                onUnlockApp(app.packageName)
                            } else {
                                onLockApp(app.packageName)
                            }
                            selectedAppForMenu = null
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(
                                imageVector = if (isLocked) Icons.Default.LockOpen else Icons.Default.Lock,
                                contentDescription = null,
                                tint = if (isLocked) Color(0xFFFFD54F) else AetherPrimary,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = if (isLocked) "Remove App Lock" else "Lock App (Fingerprint / PIN)",
                                color = if (isLocked) Color(0xFFFFD54F) else AetherPrimary
                            )
                        }
                    }

                    // Bookmark / Unbookmark Toggle (available for non-hidden apps)
                    if (!isHidden) {
                        val isBookmarked = app.packageName in bookmarkedPackageNames
                        TextButton(
                            onClick = {
                                onToggleBookmark(app.packageName)
                                selectedAppForMenu = null
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Icon(
                                    imageVector = if (isBookmarked) Icons.Default.BookmarkBorder else Icons.Default.Bookmark,
                                    contentDescription = null,
                                    tint = Color(0xFFFFD54F),
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = if (isBookmarked) "Remove from Bookmarks" else "Add to Bookmarks (Slide Down)",
                                    color = Color(0xFFFFD54F)
                                )
                            }
                        }
                    }

                    // Hide / Unhide button
                    TextButton(
                        onClick = {
                            if (isHidden) {
                                onUnhideApp(app.packageName)
                            } else {
                                onHideApp(app.packageName)
                            }
                            selectedAppForMenu = null
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(
                                imageVector = if (isHidden) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                contentDescription = null,
                                tint = if (isHidden) AetherPrimary else Color(0xFFFFB2BA),
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = if (isHidden) "Unhide Application" else "Hide & Lock with Fingerprint",
                                color = if (isHidden) AetherPrimary else Color(0xFFFFB2BA)
                            )
                        }
                    }

                    // Add / Remove from Directory (Strictly NOT allowed for hidden apps; max 4 apps limit)
                    if (!isHidden) {
                        val isDirFull = !isInDirectory && directoryPackageNames.size >= 4
                        TextButton(
                            onClick = {
                                if (isInDirectory) {
                                    onRemoveAppFromHome?.invoke(app.packageName)
                                } else if (!isDirFull) {
                                    onAddAppToHome(app.packageName)
                                }
                                selectedAppForMenu = null
                            },
                            enabled = isInDirectory || !isDirFull,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                if (isInDirectory) {
                                    Icon(Icons.Default.Delete, contentDescription = null, tint = Color(0xFFFF8A80), modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(text = "Remove from Directory", color = Color(0xFFFF8A80))
                                } else {
                                    Icon(
                                        Icons.Default.PlaylistAdd,
                                        contentDescription = null,
                                        tint = if (isDirFull) AetherOnSurfaceVariant.copy(alpha = 0.4f) else AetherPrimary,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = if (isDirFull) "Directory Full (Max 4)" else "Add to Directory (Max 4)",
                                        color = if (isDirFull) AetherOnSurfaceVariant.copy(alpha = 0.4f) else AetherPrimary
                                    )
                                }
                            }
                        }
                    }

                    // App Info
                    TextButton(
                        onClick = {
                            onAppInfoClicked(app.packageName)
                            selectedAppForMenu = null
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Default.Info, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(text = "App Info")
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { selectedAppForMenu = null }) {
                    Text(text = "Cancel", color = AetherOnSurfaceVariant)
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
                        text = "${bookmarkedPackageNames.size}/10",
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
                        items(filtered, key = { it.packageName }) { pApp ->
                            val isBookmarked = pApp.packageName in bookmarkedPackageNames
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
                                            onToggleBookmark(pApp.packageName)
                                        }
                                    }
                                    .padding(horizontal = 10.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                if (pApp.icon != null) {
                                    Image(
                                        bitmap = pApp.icon,
                                        contentDescription = pApp.label,
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
                                            text = pApp.label.take(1).uppercase(),
                                            color = AetherPrimary,
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.width(10.dp))

                                Text(
                                    text = pApp.label,
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
private fun BookmarkedItem(
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
private fun AppRowItem(
    app: AppItem,
    isInDirectory: Boolean,
    isHidden: Boolean,
    isLocked: Boolean,
    onClicked: () -> Unit,
    onLongClicked: () -> Unit
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
                    .size(42.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(AetherSurfaceContainerHigh),
                contentAlignment = Alignment.Center
            ) {
                if (app.icon != null) {
                    Image(
                        bitmap = app.icon,
                        contentDescription = app.label,
                        modifier = Modifier.size(30.dp)
                    )
                } else {
                    Text(
                        text = app.label.take(1).uppercase(),
                        color = AetherPrimary,
                        fontSize = 16.sp,
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

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
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

            if (isHidden) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(9999.dp))
                        .background(Color(0x33FF3366))
                        .border(1.dp, Color(0x66FF3366), RoundedCornerShape(9999.dp))
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = "HIDDEN",
                        color = Color(0xFFFFB2BA),
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }

            if (isInDirectory) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(9999.dp))
                        .background(AetherPrimary.copy(alpha = 0.15f))
                        .border(1.dp, AetherPrimary.copy(alpha = 0.35f), RoundedCornerShape(9999.dp))
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = "IN DIR",
                        color = AetherPrimary,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }
        }
    }
}

@Preview(showBackground = true, widthDp = 390, heightDp = 844)
@Composable
fun AllAppsDrawerPreview() {
    RareLauncerTheme {
        AllAppsDrawer(
            isOpen = true,
            apps = listOf(
                AppItem("Calculator", "com.android.calculator2", ""),
                AppItem("Calendar", "com.google.android.calendar", ""),
                AppItem("Camera", "com.android.camera", ""),
                AppItem("Chrome", "com.android.chrome", ""),
                AppItem("Clock", "com.android.deskclock", "")
            ),
            searchQuery = "",
            onSearchQueryChange = {},
            onClearSearch = {},
            onAppClicked = {},
            onAppInfoClicked = {},
            onAddAppToHome = {},
            directoryPackageNames = listOf("com.android.calculator2"),
            drawerTab = DrawerTab.ACTIVE,
            isVaultUnlocked = false,
            hiddenAppsCount = 3,
            onCloseDrawer = {}
        )
    }
}
