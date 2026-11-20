package io.github.rachmanzz.rarelauncer.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import io.github.rachmanzz.rarelauncer.data.model.AppDirectory
import io.github.rachmanzz.rarelauncer.data.model.AppItem
import io.github.rachmanzz.rarelauncer.data.model.AtmospherePreset
import io.github.rachmanzz.rarelauncer.data.model.ContactItem
import io.github.rachmanzz.rarelauncer.data.model.WallpaperConfig
import io.github.rachmanzz.rarelauncer.ui.components.AllAppsDrawer
import io.github.rachmanzz.rarelauncer.ui.components.AppDirectorySection
import io.github.rachmanzz.rarelauncer.ui.components.AtmosphericBackground
import io.github.rachmanzz.rarelauncer.ui.components.ClockAndHeader
import io.github.rachmanzz.rarelauncer.ui.components.CommunicationDrawer
import io.github.rachmanzz.rarelauncer.ui.components.FolderPopoverDialog
import io.github.rachmanzz.rarelauncer.ui.components.QuickRecentsDrawer
import io.github.rachmanzz.rarelauncer.ui.components.RecentAppsBubbleDialog
import io.github.rachmanzz.rarelauncer.ui.components.ScrollCueIndicator
import io.github.rachmanzz.rarelauncer.ui.components.SlideToOpenOverlay
import io.github.rachmanzz.rarelauncer.ui.theme.RareLauncerTheme
import io.github.rachmanzz.rarelauncer.ui.viewmodel.ClockUiState
import io.github.rachmanzz.rarelauncer.ui.viewmodel.DrawerTab
import io.github.rachmanzz.rarelauncer.ui.viewmodel.LauncherViewModel
import kotlin.math.abs

@Composable
fun LauncherScreen(
    viewModel: LauncherViewModel,
    modifier: Modifier = Modifier,
    onRequestBiometricAuth: ((String, () -> Unit) -> Unit)? = null,
    onPickCustomWallpaper: () -> Unit = {},
    onRequestContactsPermission: () -> Unit = {},
    hasUsageStatsPermission: () -> Boolean = { true },
    onRequestUsageStatsPermission: () -> Unit = {},
) {
    val clockState by viewModel.clockState.collectAsState()
    val batteryLevel by viewModel.batteryLevel.collectAsState()
    val isCharging by viewModel.isCharging.collectAsState()
    val wallpaperConfig by viewModel.wallpaperConfig.collectAsState()
    val appDirectory by viewModel.appDirectory.collectAsState()
    val isFolderPopoverOpen by viewModel.isFolderPopoverOpen.collectAsState()
    val isAppDrawerOpen by viewModel.isAppDrawerOpen.collectAsState()
    val allApps by viewModel.allApps.collectAsState()
    val filteredApps by viewModel.filteredApps.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val isCommDrawerOpen by viewModel.isCommDrawerOpen.collectAsState()
    val contacts by viewModel.contacts.collectAsState()
    val hasContactsPermission by viewModel.hasContactsPermission.collectAsState()
    val drawerTab by viewModel.drawerTab.collectAsState()
    val isVaultUnlocked by viewModel.isVaultUnlocked.collectAsState()
    val hiddenApps by viewModel.hiddenApps.collectAsState()
    val lockedApps by viewModel.lockedApps.collectAsState()
    val isQuickRecentsOpen by viewModel.isQuickRecentsOpen.collectAsState()
    val quickSearchQuery by viewModel.quickSearchQuery.collectAsState()
    val bookmarkedApps by viewModel.bookmarkedApps.collectAsState()
    val recentApps by viewModel.recentApps.collectAsState()
    val recentClusterApps by viewModel.recentClusterApps.collectAsState()
    val isRecentBubbleVisible by viewModel.isRecentBubbleVisible.collectAsState()
    val isRecentClusterDialogOpen by viewModel.isRecentClusterDialogOpen.collectAsState()
    val quickSearchResults by viewModel.quickSearchResults.collectAsState()
    val bookmarkedPackageNames by viewModel.bookmarkedPackageNames.collectAsState()

    // Handle back button hierarchically
    BackHandler(enabled = true) {
        when {
            isQuickRecentsOpen -> viewModel.closeQuickRecents()
            isFolderPopoverOpen -> viewModel.closeFolderPopover()
            isAppDrawerOpen -> viewModel.closeAppDrawer()
            isCommDrawerOpen -> viewModel.closeCommDrawer()
            else -> {
                // Stay on launcher home
            }
        }
    }

    var showUsageAccessDialog by remember { mutableStateOf(false) }

    LauncherContent(
        clockState = clockState,
        batteryLevel = batteryLevel,
        isCharging = isCharging,
        wallpaperConfig = wallpaperConfig,
        appDirectory = appDirectory,
        isFolderPopoverOpen = isFolderPopoverOpen,
        isAppDrawerOpen = isAppDrawerOpen,
        allApps = allApps,
        filteredApps = filteredApps,
        searchQuery = searchQuery,
        isCommDrawerOpen = isCommDrawerOpen,
        contacts = contacts,
        hasContactsPermission = hasContactsPermission,
        drawerTab = drawerTab,
        isVaultUnlocked = isVaultUnlocked,
        hiddenAppsCount = hiddenApps.size,
        hiddenApps = hiddenApps,
        lockedApps = lockedApps,
        onRequestUnlockVault = {
            if (onRequestBiometricAuth != null) {
                onRequestBiometricAuth("Unlock Hidden Apps") { viewModel.unlockVault() }
            } else {
                viewModel.unlockVault()
            }
        },
        onLockVault = { viewModel.lockVault() },
        onTabSelected = { tab ->
            if (tab == DrawerTab.HIDDEN && !isVaultUnlocked) {
                if (onRequestBiometricAuth != null) {
                    onRequestBiometricAuth("Unlock Hidden Apps") { viewModel.unlockVault() }
                } else {
                    viewModel.unlockVault()
                }
            } else {
                viewModel.setDrawerTab(tab)
            }
        },
        onHideApp = { pkg -> viewModel.hideApp(pkg) },
        onUnhideApp = { pkg -> viewModel.unhideApp(pkg) },
        onLockApp = { pkg ->
            viewModel.lockApp(pkg)
            if (!hasUsageStatsPermission()) {
                showUsageAccessDialog = true
            }
        },
        onUnlockApp = { pkg ->
            if (onRequestBiometricAuth != null) {
                onRequestBiometricAuth("Remove App Lock") { viewModel.unlockApp(pkg) }
            } else {
                viewModel.unlockApp(pkg)
            }
        },
        onVerifyLockedApp = { app, onSuccess ->
            if (onRequestBiometricAuth != null) {
                onRequestBiometricAuth("Open ${app.label}", onSuccess)
            } else {
                onSuccess()
            }
        },
        onPresetSelected = { viewModel.setAtmospherePreset(it) },
        onPickCustomWallpaper = onPickCustomWallpaper,
        onClockClicked = { viewModel.openClock() },
        onDateClicked = { viewModel.openCalendar() },
        onOpenFolderPopover = { viewModel.openFolderPopover() },
        onCloseFolderPopover = { viewModel.closeFolderPopover() },
        onOpenAppDrawer = { viewModel.openAppDrawer() },
        onCloseAppDrawer = { viewModel.closeAppDrawer() },
        isQuickRecentsOpen = isQuickRecentsOpen,
        quickSearchQuery = quickSearchQuery,
        bookmarkedApps = bookmarkedApps,
        recentApps = recentApps,
        recentClusterApps = recentClusterApps,
        isRecentBubbleVisible = isRecentBubbleVisible,
        isRecentClusterDialogOpen = isRecentClusterDialogOpen,
        onOpenRecentClusterDialog = { viewModel.openRecentClusterDialog() },
        onCloseRecentClusterDialog = { viewModel.closeRecentClusterDialog() },
        onRemoveFromRecents = { pkg -> viewModel.removeRecentApp(pkg) },
        onDismissRecentBubble = { viewModel.dismissRecentBubble() },
        quickSearchResults = quickSearchResults,
        bookmarkedPackageNames = bookmarkedPackageNames,
        onOpenQuickRecents = { viewModel.openQuickRecents() },
        onCloseQuickRecents = { viewModel.closeQuickRecents() },
        onQuickSearchQueryChange = { viewModel.onQuickSearchQueryChange(it) },
        onClearQuickSearch = { viewModel.clearQuickSearchQuery() },
        onToggleBookmark = { pkg -> viewModel.toggleBookmarkApp(pkg) },
        onAddAppToDirectory = { pkg -> viewModel.addAppToDirectory(pkg) },
        onRemoveAppFromDirectory = { pkg -> viewModel.removeAppFromDirectory(pkg) },
        onToggleAppInDirectory = { pkg -> viewModel.toggleAppInDirectory(pkg) },
        onClearDirectory = { viewModel.clearDirectory() },
        onOpenCommClicked = { viewModel.openCommDrawer() },
        onCloseCommClicked = { viewModel.closeCommDrawer() },
        onAppClicked = { app -> viewModel.launchApp(app.packageName) },
        onAppInfoClicked = { pkg -> viewModel.openAppInfo(pkg) },
        onSearchQueryChange = { viewModel.onSearchQueryChange(it) },
        onClearSearch = { viewModel.clearSearchQuery() },
        onCallClicked = { viewModel.openDialer(it) },
        onMessageClicked = { viewModel.openMessages(it) },
        onContactsClicked = { viewModel.openContacts() },
        onRequestContactsPermission = onRequestContactsPermission,
        modifier = modifier
    )

    if (showUsageAccessDialog) {
        AlertDialog(
            onDismissRequest = { showUsageAccessDialog = false },
            title = { Text(text = "Aktifkan Usage Access") },
            text = {
                Text(
                    text = "Agar aplikasi yang dikunci tetap meminta verifikasi saat dibuka lewat notifikasi atau Recent Apps, izinkan Usage Access untuk Rare Launcer.",
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showUsageAccessDialog = false
                        onRequestUsageStatsPermission()
                    },
                ) {
                    Text(text = "Buka Pengaturan")
                }
            },
            dismissButton = {
                TextButton(onClick = { showUsageAccessDialog = false }) {
                    Text(text = "Nanti")
                }
            },
        )
    }
}

@Composable
fun LauncherContent(
    clockState: ClockUiState,
    batteryLevel: Int,
    isCharging: Boolean,
    wallpaperConfig: WallpaperConfig,
    appDirectory: AppDirectory,
    isFolderPopoverOpen: Boolean,
    isAppDrawerOpen: Boolean,
    allApps: List<AppItem>,
    filteredApps: List<AppItem>,
    searchQuery: String,
    isCommDrawerOpen: Boolean,
    contacts: List<ContactItem>,
    hasContactsPermission: Boolean = true,
    drawerTab: DrawerTab = DrawerTab.ACTIVE,
    isVaultUnlocked: Boolean = false,
    hiddenAppsCount: Int = 0,
    hiddenApps: Set<String> = emptySet(),
    lockedApps: Set<String> = emptySet(),
    onRequestUnlockVault: () -> Unit = {},
    onLockVault: () -> Unit = {},
    onTabSelected: (DrawerTab) -> Unit = {},
    onHideApp: (String) -> Unit = {},
    onUnhideApp: (String) -> Unit = {},
    onLockApp: (String) -> Unit = {},
    onUnlockApp: (String) -> Unit = {},
    onVerifyLockedApp: (AppItem, () -> Unit) -> Unit = { _, onSuccess -> onSuccess() },
    onPresetSelected: (AtmospherePreset) -> Unit,
    onPickCustomWallpaper: () -> Unit,
    onClockClicked: () -> Unit,
    onDateClicked: () -> Unit,
    onOpenFolderPopover: () -> Unit,
    onCloseFolderPopover: () -> Unit,
    onOpenAppDrawer: () -> Unit,
    onCloseAppDrawer: () -> Unit,
    isQuickRecentsOpen: Boolean = false,
    quickSearchQuery: String = "",
    bookmarkedApps: List<AppItem> = emptyList(),
    recentApps: List<AppItem> = emptyList(),
    recentClusterApps: List<AppItem> = emptyList(),
    isRecentBubbleVisible: Boolean = false,
    isRecentClusterDialogOpen: Boolean = false,
    onOpenRecentClusterDialog: () -> Unit = {},
    onCloseRecentClusterDialog: () -> Unit = {},
    onRemoveFromRecents: (String) -> Unit = {},
    onDismissRecentBubble: () -> Unit = {},
    quickSearchResults: List<AppItem> = emptyList(),
    bookmarkedPackageNames: List<String> = emptyList(),
    onOpenQuickRecents: () -> Unit = {},
    onCloseQuickRecents: () -> Unit = {},
    onQuickSearchQueryChange: (String) -> Unit = {},
    onClearQuickSearch: () -> Unit = {},
    onToggleBookmark: (String) -> Unit = {},
    onAddAppToDirectory: (packageName: String) -> Unit,
    onRemoveAppFromDirectory: (packageName: String) -> Unit,
    onToggleAppInDirectory: (packageName: String) -> Unit,
    onClearDirectory: () -> Unit,
    onOpenCommClicked: () -> Unit,
    onCloseCommClicked: () -> Unit,
    onAppClicked: (AppItem) -> Unit,
    onAppInfoClicked: (String) -> Unit,
    onSearchQueryChange: (String) -> Unit,
    onClearSearch: () -> Unit,
    onCallClicked: (String?) -> Unit,
    onMessageClicked: (String?) -> Unit,
    onContactsClicked: () -> Unit,
    onRequestContactsPermission: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    var pendingLaunchApp by remember { mutableStateOf<AppItem?>(null) }
    val requestAppLaunch: (AppItem) -> Unit = { app -> pendingLaunchApp = app }

    Box(
        modifier = modifier
            .fillMaxSize()
            .pointerInput(Unit) {
                var accumulatedX = 0f
                var accumulatedY = 0f
                var gestureDecided = false

                detectDragGestures(
                    onDragStart = {
                        accumulatedX = 0f
                        accumulatedY = 0f
                        gestureDecided = false
                    },
                    onDragEnd = {
                        accumulatedX = 0f
                        accumulatedY = 0f
                        gestureDecided = false
                    },
                    onDragCancel = {
                        accumulatedX = 0f
                        accumulatedY = 0f
                        gestureDecided = false
                    },
                    onDrag = { change, dragAmount ->
                        change.consume()
                        if (!gestureDecided) {
                            accumulatedX += dragAmount.x
                            accumulatedY += dragAmount.y

                            val absX = abs(accumulatedX)
                            val absY = abs(accumulatedY)

                            // Threshold to determine intention (lower threshold for faster response)
                            if (absX >= 20f || absY >= 20f) {
                                gestureDecided = true
                                if (absY >= absX) {
                                    // VERTICAL: swipe up (< 0) opens All Apps drawer.
                                    // Swipe down is left to Android system for Status Bar / Notification shade.
                                    if (accumulatedY < -20f) {
                                        if (!isAppDrawerOpen && !isQuickRecentsOpen) {
                                            onOpenAppDrawer()
                                        }
                                    }
                                } else if (absX > absY * 1.5f) {
                                    // Strictly HORIZONTAL swipe to right: Opens Communication Hub
                                    if (accumulatedX > 35f && !isCommDrawerOpen) {
                                        onOpenCommClicked()
                                    } else if (accumulatedX < -35f && isCommDrawerOpen) {
                                        onCloseCommClicked()
                                    }
                                }
                            }
                        }
                    }
                )
            }
    ) {
        // Atmospheric multi-mode backdrop
        AtmosphericBackground(config = wallpaperConfig)

        // Clean Home Viewport
        Scaffold(
            containerColor = Color.Transparent,
            modifier = Modifier.fillMaxSize()
        ) { paddingValues ->
            Column(
                modifier = Modifier
                    .fillMaxSize()



                    .padding(paddingValues)
                    .statusBarsPadding()
                    .navigationBarsPadding(),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    // Top 1/3 of screen: Clock, live date, battery capsule, aura switcher, comm trigger
                    ClockAndHeader(
                        clockState = clockState,
                        batteryLevel = batteryLevel,
                        isCharging = isCharging,
                        wallpaperConfig = wallpaperConfig,
                        recentBubbleApps = recentClusterApps,
                        isRecentBubbleVisible = isRecentBubbleVisible,
                        onRecentBubbleClicked = onOpenRecentClusterDialog,
                        onDismissRecentBubble = onDismissRecentBubble,
                        onPresetSelected = onPresetSelected,
                        onPickCustomWallpaper = onPickCustomWallpaper,
                        onClockClicked = onClockClicked,
                        onDateClicked = onDateClicked,
                        onOpenCommClicked = onOpenCommClicked
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    // Middle 2/3 of screen: ONLY 1 APP DIRECTORY (strictly half-width 2-column card, 1:1 with code.html)
                    AppDirectorySection(
                        directory = appDirectory,
                        allApps = allApps,
                        onDirectoryClicked = onOpenFolderPopover,
                        onAppClicked = requestAppLaunch,
                        lockedApps = lockedApps
                    )
                }

                // Gestural scroll cue: Pinned at the bottom of the home screen, tapping opens full app drawer
                ScrollCueIndicator(
                    onScrollCueClicked = onOpenAppDrawer,
                    modifier = Modifier.padding(bottom = 12.dp)
                )
            }
        }

        // Full Slide-Up All Apps Drawer (Search bar pinned at top, ONLY applications list scrolls)
        AllAppsDrawer(
            isOpen = isAppDrawerOpen,
            apps = filteredApps,
            searchQuery = searchQuery,
            onSearchQueryChange = onSearchQueryChange,
            onClearSearch = onClearSearch,
            onAppClicked = requestAppLaunch,
            onAppInfoClicked = onAppInfoClicked,
            onAddAppToHome = onAddAppToDirectory,
            onRemoveAppFromHome = onRemoveAppFromDirectory,
            directoryPackageNames = appDirectory.packageNames,
            drawerTab = drawerTab,
            isVaultUnlocked = isVaultUnlocked,
            hiddenAppsCount = hiddenAppsCount,
            onTabSelected = onTabSelected,
            onRequestUnlockVault = onRequestUnlockVault,
            onLockVault = onLockVault,
            onHideApp = onHideApp,
            onUnhideApp = onUnhideApp,
            lockedApps = lockedApps,
            onLockApp = onLockApp,
            onUnlockApp = onUnlockApp,
            bookmarkedApps = bookmarkedApps,
            recentApps = recentApps,
            allApps = allApps.filter { it.packageName !in hiddenApps },
            bookmarkedPackageNames = bookmarkedPackageNames,
            onToggleBookmark = onToggleBookmark,
            onCloseDrawer = onCloseAppDrawer
        )

        // Floating 1:1 Folder Popover Dialog (#folder-popover in code.html)
        if (isFolderPopoverOpen) {
            FolderPopoverDialog(
                directory = appDirectory,
                allApps = allApps,
                hiddenApps = hiddenApps,
                onDismissRequest = onCloseFolderPopover,
                onAppClicked = requestAppLaunch,
                onRemoveApp = onRemoveAppFromDirectory,
                onAddApp = onAddAppToDirectory,
                onToggleApp = onToggleAppInDirectory,
                onClearDirectory = onClearDirectory
            )
        }

        // Floating Recent Apps Bubble Dialog (Shown when tapping the sticky bubble cluster)
        if (isRecentClusterDialogOpen) {
            RecentAppsBubbleDialog(
                recentApps = recentClusterApps,
                onDismissRequest = onCloseRecentClusterDialog,
                onAppClicked = requestAppLaunch,
                onAppInfoClicked = onAppInfoClicked,
                onRemoveFromRecents = onRemoveFromRecents,
            )
        }

        // Quick Recents & Bookmarks Drawer (Slide down from top)
        QuickRecentsDrawer(
            isOpen = isQuickRecentsOpen,
            searchQuery = quickSearchQuery,
            onSearchQueryChange = onQuickSearchQueryChange,
            onClearSearch = onClearQuickSearch,
            bookmarkedApps = bookmarkedApps,
            recentApps = recentApps,
            searchResults = quickSearchResults,
            allApps = allApps.filter { it.packageName !in hiddenApps },
            bookmarkedPackageNames = bookmarkedPackageNames,
            lockedApps = lockedApps,
            onAppClicked = requestAppLaunch,
            onToggleBookmark = onToggleBookmark,
            onClose = onCloseQuickRecents,
        )

        // Quick Action Communication Drawer (Slide to right hub) - strictly hidden when App Drawer or Quick Recents is open
        if (!isAppDrawerOpen && !isQuickRecentsOpen) {
            CommunicationDrawer(
                isOpen = isCommDrawerOpen,
                contacts = contacts,
                hasContactsPermission = hasContactsPermission,
                onClose = onCloseCommClicked,
                onCallClicked = { number ->
                    onCloseCommClicked()
                    onCallClicked(number)
                },
                onMessageClicked = { number ->
                    onCloseCommClicked()
                    onMessageClicked(number)
                },
                onContactsClicked = {
                    onCloseCommClicked()
                    onContactsClicked()
                },
                onRequestContactsPermission = onRequestContactsPermission,
            )
        }

        pendingLaunchApp?.let { app ->
            val isLocked = app.packageName in lockedApps
            SlideToOpenOverlay(
                app = app,
                isLocked = isLocked,
                onConfirm = {
                    pendingLaunchApp = null
                    if (isLocked) {
                        onVerifyLockedApp(app) { onAppClicked(app) }
                    } else {
                        onAppClicked(app)
                    }
                },
                onDismiss = { pendingLaunchApp = null },
            )
        }
    }
}

@Preview(showBackground = true, widthDp = 390, heightDp = 844)
@Composable
fun LauncherScreenPreview() {
    RareLauncerTheme {
        LauncherContent(
            clockState = ClockUiState(
                hours = "09",
                minutes = "54",
                seconds = "01",
                dateText = "Thursday, Oct 24",
                timeOfDayGreeting = "Good Morning"
            ),
            batteryLevel = 92,
            isCharging = false,
            wallpaperConfig = WallpaperConfig(preset = AtmospherePreset.SPACE),
            appDirectory = AppDirectory(
                id = "primary_dir",
                name = "Productivity",
                packageNames = listOf(
                    "com.android.calculator2",
                    "com.google.android.calendar",
                    "com.android.camera",
                    "com.android.chrome"
                )
            ),
            isFolderPopoverOpen = false,
            isAppDrawerOpen = false,
            allApps = listOf(
                AppItem("Calculator", "com.android.calculator2", ""),
                AppItem("Calendar", "com.google.android.calendar", ""),
                AppItem("Camera", "com.android.camera", ""),
                AppItem("Chrome", "com.android.chrome", ""),
                AppItem("Clock", "com.android.deskclock", ""),
                AppItem("Settings", "com.android.settings", ""),
                AppItem("YouTube", "com.google.android.youtube", "")
            ),
            filteredApps = listOf(
                AppItem("Calculator", "com.android.calculator2", ""),
                AppItem("Calendar", "com.google.android.calendar", ""),
                AppItem("Camera", "com.android.camera", ""),
                AppItem("Chrome", "com.android.chrome", ""),
                AppItem("Clock", "com.android.deskclock", ""),
                AppItem("Settings", "com.android.settings", ""),
                AppItem("YouTube", "com.google.android.youtube", "")
            ),
            searchQuery = "",
            isCommDrawerOpen = false,
            contacts = listOf(
                ContactItem("1", "Alexandre Laurent", "+123456789", "AL", "Mobile • 12m ago"),
                ContactItem("2", "Sarah Miller", "+987654321", "SM", "Missed • 1h ago")
            ),
            drawerTab = DrawerTab.ACTIVE,
            isVaultUnlocked = false,
            hiddenAppsCount = 2,
            onRequestUnlockVault = {},
            onLockVault = {},
            onTabSelected = {},
            onHideApp = {},
            onUnhideApp = {},
            onPresetSelected = {},
            onPickCustomWallpaper = {},
            onClockClicked = {},
            onDateClicked = {},
            onOpenFolderPopover = {},
            onCloseFolderPopover = {},
            onOpenAppDrawer = {},
            onCloseAppDrawer = {},
            onAddAppToDirectory = {},
            onRemoveAppFromDirectory = {},
            onToggleAppInDirectory = {},
            onClearDirectory = {},
            onOpenCommClicked = {},
            onCloseCommClicked = {},
            onAppClicked = {},
            onAppInfoClicked = {},
            onSearchQueryChange = {},
            onClearSearch = {},
            onCallClicked = {},
            onMessageClicked = {},
            onContactsClicked = {}
        )
    }
}
