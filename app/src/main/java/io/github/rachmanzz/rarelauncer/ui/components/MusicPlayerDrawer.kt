package io.github.rachmanzz.rarelauncer.ui.components

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.PlaylistAdd
import androidx.compose.material.icons.automirrored.filled.QueueMusic
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import io.github.rachmanzz.rarelauncer.data.model.MusicPlayerState
import io.github.rachmanzz.rarelauncer.data.model.MusicPlaylist
import io.github.rachmanzz.rarelauncer.data.model.MusicTrack
import io.github.rachmanzz.rarelauncer.data.model.PlaybackOrderMode
import io.github.rachmanzz.rarelauncer.ui.theme.AetherGlassBorder
import io.github.rachmanzz.rarelauncer.ui.theme.AetherGlassBorderSubtle
import io.github.rachmanzz.rarelauncer.ui.theme.AetherOnSurface
import io.github.rachmanzz.rarelauncer.ui.theme.AetherOnSurfaceVariant
import io.github.rachmanzz.rarelauncer.ui.theme.AetherPrimary

private enum class MusicDrawerTab {
    EXPLORE,
    PLAYLISTS,
}

@Composable
fun MusicPlayerDrawer(
    isOpen: Boolean,
    playerState: MusicPlayerState,
    searchQuery: String,
    searchResults: List<MusicTrack>,
    isSearching: Boolean,
    playlists: List<MusicPlaylist>,
    onClose: () -> Unit,
    onSearchQueryChange: (String) -> Unit,
    onClearSearch: () -> Unit,
    onTrackSelected: (MusicTrack, List<MusicTrack>) -> Unit,
    onPlayPlaylist: (List<MusicTrack>, Boolean) -> Unit,
    onTogglePlaybackOrderMode: () -> Unit,
    onCreatePlaylist: (String, MusicTrack?) -> Unit,
    onDeletePlaylist: (String) -> Unit,
    onAddTrackToPlaylist: (String, MusicTrack) -> Unit,
    onRemoveTrackFromPlaylist: (String, String) -> Unit,
    onTogglePlayPause: () -> Unit,
    onNextTrack: () -> Unit,
    onPreviousTrack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var activeTab by remember { mutableStateOf(MusicDrawerTab.EXPLORE) }
    var openedPlaylistId by remember { mutableStateOf<String?>(null) }
    var trackToAddToPlaylist by remember { mutableStateOf<MusicTrack?>(null) }
    var pendingTrackForNewPlaylist by remember { mutableStateOf<MusicTrack?>(null) }
    var showCreatePlaylistDialog by remember { mutableStateOf(false) }
    var newPlaylistName by remember { mutableStateOf("") }
    val openedPlaylist = playlists.find { it.id == openedPlaylistId }

    val configuration = LocalConfiguration.current
    val screenWidth = configuration.screenWidthDp.dp
    val drawerWidth = screenWidth * 0.92f

    val offsetX by animateDpAsState(
        targetValue = if (isOpen) 0.dp else -(drawerWidth + 40.dp),
        animationSpec = spring(
            dampingRatio = 0.85f,
            stiffness = 400f,
        ),
        label = "MusicDrawerOffset",
    )

    val scrimAlpha by animateFloatAsState(
        targetValue = if (isOpen) 0.5f else 0f,
        animationSpec = tween(durationMillis = 300),
        label = "MusicDrawerScrim",
    )

    Box(modifier = modifier.fillMaxSize()) {
        // Scrim backdrop (pass clicks through when closed)
        if (scrimAlpha > 0.01f) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = scrimAlpha))
                    .clickable(enabled = isOpen, onClick = onClose),
            )
        }

        // Sliding Drawer Container (stays in composition to keep audio stream alive in background)
        Box(
            modifier = Modifier
                .fillMaxHeight()
                .width(drawerWidth)
                .offset(x = offsetX)
                .background(
                    Brush.horizontalGradient(
                        listOf(
                            Color(0xF7090C12),
                            Color(0xF20E131E),
                        )
                    )
                )
                .border(
                    1.dp,
                    AetherGlassBorderSubtle,
                    RoundedCornerShape(topEnd = 24.dp, bottomEnd = 24.dp)
                )
                .clip(RoundedCornerShape(topEnd = 24.dp, bottomEnd = 24.dp))
                .clickable(enabled = false) {} // consume clicks inside
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(16.dp),
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                // Top Bar
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(AetherPrimary.copy(alpha = 0.15f))
                                .border(1.dp, AetherPrimary.copy(alpha = 0.4f), CircleShape),
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(
                                imageVector = Icons.Default.GraphicEq,
                                contentDescription = "Cyber Music",
                                tint = AetherPrimary,
                                modifier = Modifier.size(20.dp),
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "CYBER STREAM",
                                color = AetherOnSurface,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.sp,
                                fontFamily = FontFamily.Monospace,
                            )
                            Text(
                                text = "Music Online",
                                color = AetherPrimary,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Medium,
                            )
                        }
                    }

                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(Color.White.copy(alpha = 0.05f))
                            .clickable(onClick = onClose),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Close",
                            tint = AetherOnSurfaceVariant,
                            modifier = Modifier.size(18.dp),
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Mode Switcher Tabs: EXPLORE / PLAYLISTS
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(Color.White.copy(alpha = 0.05f))
                        .border(1.dp, AetherGlassBorderSubtle, RoundedCornerShape(14.dp))
                        .padding(4.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    DrawerTabPill(
                        label = "EXPLORE",
                        selected = activeTab == MusicDrawerTab.EXPLORE,
                        onClick = { activeTab = MusicDrawerTab.EXPLORE },
                        modifier = Modifier.weight(1f),
                    )
                    DrawerTabPill(
                        label = "PLAYLISTS (${playlists.size})",
                        selected = activeTab == MusicDrawerTab.PLAYLISTS,
                        onClick = { activeTab = MusicDrawerTab.PLAYLISTS },
                        modifier = Modifier.weight(1f),
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                if (activeTab == MusicDrawerTab.EXPLORE) {
                    // Search Bar
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(44.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .background(Color.White.copy(alpha = 0.06f))
                            .border(1.dp, AetherGlassBorderSubtle, RoundedCornerShape(14.dp))
                            .padding(horizontal = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = "Search",
                            tint = AetherPrimary.copy(alpha = 0.8f),
                            modifier = Modifier.size(18.dp),
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        val searchTextStyle = TextStyle(
                            color = AetherOnSurface,
                            fontSize = 13.sp,
                            lineHeight = 18.sp,
                            fontWeight = FontWeight.Normal,
                            fontFamily = FontFamily.Default,
                            letterSpacing = 0.sp,
                        )
                        BasicTextField(
                            value = searchQuery,
                            onValueChange = onSearchQueryChange,
                            modifier = Modifier.weight(1f),
                            textStyle = searchTextStyle,
                            singleLine = true,
                            cursorBrush = SolidColor(AetherPrimary),
                            decorationBox = { innerTextField ->
                                Box(
                                    modifier = Modifier.fillMaxWidth(),
                                    contentAlignment = Alignment.CenterStart,
                                ) {
                                    if (searchQuery.isEmpty()) {
                                        Text(
                                            text = "Cari lagu, artis, synthwave...",
                                            style = searchTextStyle.copy(
                                                color = AetherOnSurfaceVariant.copy(alpha = 0.5f),
                                            ),
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis,
                                        )
                                    }
                                    innerTextField()
                                }
                            },
                        )
                        if (searchQuery.isNotEmpty()) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Clear",
                                tint = AetherOnSurfaceVariant,
                                modifier = Modifier
                                    .size(18.dp)
                                    .clickable(onClick = onClearSearch),
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Track List Header
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = if (searchQuery.isNotBlank()) "HASIL PENCARIAN" else "TRENDING & CHILL VIBES",
                            color = AetherOnSurfaceVariant,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            letterSpacing = 1.sp,
                            fontFamily = FontFamily.Monospace,
                        )
                        playerState.error?.let { err ->
                            Text(
                                text = err,
                                color = Color(0xFFFF5252),
                                fontSize = 11.sp,
                                maxLines = 1,
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Track List
                    Box(modifier = Modifier.weight(1f)) {
                        if (isSearching) {
                            Box(
                                modifier = Modifier.fillMaxSize(),
                                contentAlignment = Alignment.Center,
                            ) {
                                CircularProgressIndicator(
                                    color = AetherPrimary,
                                    modifier = Modifier.size(32.dp),
                                    strokeWidth = 2.dp,
                                )
                            }
                        } else if (searchResults.isEmpty()) {
                            Box(
                                modifier = Modifier.fillMaxSize(),
                                contentAlignment = Alignment.Center,
                            ) {
                                Text(
                                    text = "Lagu tidak ditemukan. Coba kata kunci lain.",
                                    color = AetherOnSurfaceVariant,
                                    fontSize = 13.sp,
                                )
                            }
                        } else {
                            LazyColumn(
                                verticalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier.fillMaxSize(),
                            ) {
                                items(searchResults, key = { it.id }) { track ->
                                    val isSelected = playerState.currentTrack?.id == track.id
                                    TrackRow(
                                        track = track,
                                        isSelected = isSelected,
                                        isPlaying = isSelected && playerState.isPlaying,
                                        actionIcon = Icons.AutoMirrored.Filled.PlaylistAdd,
                                        actionContentDescription = "Tambah ke Playlist",
                                        onActionClick = { trackToAddToPlaylist = track },
                                        onClick = { onTrackSelected(track, searchResults) },
                                    )
                                }
                            }
                        }
                    }
                } else {
                    // PLAYLISTS TAB
                    if (openedPlaylist == null) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(
                                text = "PLAYLIST SAYA",
                                color = AetherOnSurfaceVariant,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                letterSpacing = 1.sp,
                                fontFamily = FontFamily.Monospace,
                            )
                            Row(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(AetherPrimary.copy(alpha = 0.16f))
                                    .border(1.dp, AetherPrimary.copy(alpha = 0.5f), RoundedCornerShape(10.dp))
                                    .clickable {
                                        pendingTrackForNewPlaylist = null
                                        newPlaylistName = ""
                                        showCreatePlaylistDialog = true
                                    }
                                    .padding(horizontal = 10.dp, vertical = 5.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Add,
                                    contentDescription = "Buat Playlist",
                                    tint = AetherPrimary,
                                    modifier = Modifier.size(14.dp),
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "Baru",
                                    color = AetherPrimary,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        Box(modifier = Modifier.weight(1f)) {
                            if (playlists.isEmpty()) {
                                Box(
                                    modifier = Modifier.fillMaxSize(),
                                    contentAlignment = Alignment.Center,
                                ) {
                                    Text(
                                        text = "Belum ada playlist. Tekan + Baru untuk membuat.",
                                        color = AetherOnSurfaceVariant,
                                        fontSize = 13.sp,
                                    )
                                }
                            } else {
                                LazyColumn(
                                    verticalArrangement = Arrangement.spacedBy(8.dp),
                                    modifier = Modifier.fillMaxSize(),
                                ) {
                                    items(playlists, key = { it.id }) { playlist ->
                                        PlaylistCardRow(
                                            playlist = playlist,
                                            onOpen = { openedPlaylistId = playlist.id },
                                            onPlayAll = {
                                                if (playlist.tracks.isNotEmpty()) {
                                                    onPlayPlaylist(
                                                        playlist.tracks,
                                                        playerState.playbackOrderMode == PlaybackOrderMode.SHUFFLE,
                                                    )
                                                }
                                            },
                                            onDelete = { onDeletePlaylist(playlist.id) },
                                        )
                                    }
                                }
                            }
                        }
                    } else {
                        // Opened Playlist Detail View
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.weight(1f),
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(30.dp)
                                        .clip(CircleShape)
                                        .background(Color.White.copy(alpha = 0.08f))
                                        .clickable { openedPlaylistId = null },
                                    contentAlignment = Alignment.Center,
                                ) {
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                        contentDescription = "Kembali ke daftar playlist",
                                        tint = AetherOnSurface,
                                        modifier = Modifier.size(16.dp),
                                    )
                                }
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    Text(
                                        text = openedPlaylist.name,
                                        color = AetherOnSurface,
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Bold,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                    )
                                    Text(
                                        text = "${openedPlaylist.tracks.size} lagu",
                                        color = AetherPrimary,
                                        fontSize = 11.sp,
                                    )
                                }
                            }

                            if (openedPlaylist.tracks.isNotEmpty()) {
                                val isShuffleMode = playerState.playbackOrderMode == PlaybackOrderMode.SHUFFLE
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(10.dp))
                                            .background(
                                                if (!isShuffleMode) AetherPrimary
                                                else Color.White.copy(alpha = 0.08f),
                                            )
                                            .border(
                                                1.dp,
                                                if (!isShuffleMode) AetherPrimary else AetherGlassBorderSubtle,
                                                RoundedCornerShape(10.dp),
                                            )
                                            .clickable {
                                                onPlayPlaylist(openedPlaylist.tracks, false)
                                            }
                                            .padding(horizontal = 9.dp, vertical = 6.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Repeat,
                                            contentDescription = "Putar sesuai urutan",
                                            tint = if (!isShuffleMode) Color.Black else AetherOnSurface,
                                            modifier = Modifier.size(14.dp),
                                        )
                                        Spacer(modifier = Modifier.width(3.dp))
                                        Text(
                                            text = "Urut",
                                            color = if (!isShuffleMode) Color.Black else AetherOnSurface,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                        )
                                    }
                                    Row(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(10.dp))
                                            .background(
                                                if (isShuffleMode) AetherPrimary
                                                else Color.White.copy(alpha = 0.08f),
                                            )
                                            .border(
                                                1.dp,
                                                if (isShuffleMode) AetherPrimary else AetherGlassBorderSubtle,
                                                RoundedCornerShape(10.dp),
                                            )
                                            .clickable {
                                                onPlayPlaylist(openedPlaylist.tracks, true)
                                            }
                                            .padding(horizontal = 9.dp, vertical = 6.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Shuffle,
                                            contentDescription = "Putar acak",
                                            tint = if (isShuffleMode) Color.Black else AetherOnSurface,
                                            modifier = Modifier.size(14.dp),
                                        )
                                        Spacer(modifier = Modifier.width(3.dp))
                                        Text(
                                            text = "Acak",
                                            color = if (isShuffleMode) Color.Black else AetherOnSurface,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                        )
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        Box(modifier = Modifier.weight(1f)) {
                            if (openedPlaylist.tracks.isEmpty()) {
                                Box(
                                    modifier = Modifier.fillMaxSize(),
                                    contentAlignment = Alignment.Center,
                                ) {
                                    Text(
                                        text = "Playlist masih kosong.\nCari lagu di tab EXPLORE lalu tekan ikon +.",
                                        color = AetherOnSurfaceVariant,
                                        fontSize = 12.sp,
                                    )
                                }
                            } else {
                                LazyColumn(
                                    verticalArrangement = Arrangement.spacedBy(8.dp),
                                    modifier = Modifier.fillMaxSize(),
                                ) {
                                    items(openedPlaylist.tracks, key = { it.id }) { track ->
                                        val isSelected = playerState.currentTrack?.id == track.id
                                        TrackRow(
                                            track = track,
                                            isSelected = isSelected,
                                            isPlaying = isSelected && playerState.isPlaying,
                                            actionIcon = Icons.Default.DeleteOutline,
                                            actionContentDescription = "Hapus dari playlist",
                                            onActionClick = {
                                                onRemoveTrackFromPlaylist(openedPlaylist.id, track.id)
                                            },
                                            onClick = { onTrackSelected(track, openedPlaylist.tracks) },
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // Bottom Now Playing Mini Player
                playerState.currentTrack?.let { track ->
                    Spacer(modifier = Modifier.height(12.dp))
                    NowPlayingMiniCard(
                        track = track,
                        playerState = playerState,
                        onTogglePlayPause = onTogglePlayPause,
                        onNextTrack = onNextTrack,
                        onPreviousTrack = onPreviousTrack,
                        onTogglePlaybackOrderMode = onTogglePlaybackOrderMode,
                    )
                }
            }
        }
    }

    // Dialog: Add track to playlist
    trackToAddToPlaylist?.let { targetTrack ->
        AlertDialog(
            onDismissRequest = { trackToAddToPlaylist = null },
            containerColor = Color(0xFF121824),
            titleContentColor = AetherOnSurface,
            textContentColor = AetherOnSurfaceVariant,
            title = {
                Text(
                    text = "Simpan ke Playlist",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = targetTrack.title,
                        color = AetherPrimary,
                        fontSize = 12.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    playlists.forEach { playlist ->
                        val alreadyIn = playlist.tracks.any { it.id == targetTrack.id }
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(Color.White.copy(alpha = 0.06f))
                                .clickable {
                                    onAddTrackToPlaylist(playlist.id, targetTrack)
                                    trackToAddToPlaylist = null
                                }
                                .padding(horizontal = 12.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = playlist.name,
                                    color = AetherOnSurface,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.SemiBold,
                                )
                                Text(
                                    text = "${playlist.tracks.size} lagu",
                                    color = AetherOnSurfaceVariant,
                                    fontSize = 11.sp,
                                )
                            }
                            Icon(
                                imageVector = if (alreadyIn) Icons.Default.Check else Icons.Default.Add,
                                contentDescription = null,
                                tint = if (alreadyIn) AetherPrimary else AetherOnSurfaceVariant,
                                modifier = Modifier.size(18.dp),
                            )
                        }
                    }
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(AetherPrimary.copy(alpha = 0.14f))
                            .border(1.dp, AetherPrimary.copy(alpha = 0.45f), RoundedCornerShape(12.dp))
                            .clickable {
                                pendingTrackForNewPlaylist = targetTrack
                                trackToAddToPlaylist = null
                                newPlaylistName = ""
                                showCreatePlaylistDialog = true
                            }
                            .padding(horizontal = 12.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = null,
                            tint = AetherPrimary,
                            modifier = Modifier.size(18.dp),
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Buat Playlist Baru",
                            color = AetherPrimary,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { trackToAddToPlaylist = null }) {
                    Text("Tutup", color = AetherOnSurfaceVariant)
                }
            },
        )
    }

    // Dialog: Create new playlist
    if (showCreatePlaylistDialog) {
        AlertDialog(
            onDismissRequest = {
                showCreatePlaylistDialog = false
                pendingTrackForNewPlaylist = null
            },
            containerColor = Color(0xFF121824),
            titleContentColor = AetherOnSurface,
            title = {
                Text(
                    text = "Buat Playlist Baru",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                )
            },
            text = {
                OutlinedTextField(
                    value = newPlaylistName,
                    onValueChange = { newPlaylistName = it },
                    placeholder = { Text("Contoh: Night Drive, Chill Coding...") },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = AetherPrimary,
                        unfocusedBorderColor = AetherGlassBorder,
                        focusedTextColor = AetherOnSurface,
                        unfocusedTextColor = AetherOnSurface,
                        cursorColor = AetherPrimary,
                    ),
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        val trimmed = newPlaylistName.trim()
                        if (trimmed.isNotBlank()) {
                            onCreatePlaylist(trimmed, pendingTrackForNewPlaylist)
                            showCreatePlaylistDialog = false
                            pendingTrackForNewPlaylist = null
                            newPlaylistName = ""
                        }
                    },
                ) {
                    Text("Simpan", color = AetherPrimary, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        showCreatePlaylistDialog = false
                        pendingTrackForNewPlaylist = null
                    },
                ) {
                    Text("Batal", color = AetherOnSurfaceVariant)
                }
            },
        )
    }
}

@Composable
private fun DrawerTabPill(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .background(if (selected) AetherPrimary.copy(alpha = 0.2f) else Color.Transparent)
            .border(
                1.dp,
                if (selected) AetherPrimary.copy(alpha = 0.6f) else Color.Transparent,
                RoundedCornerShape(10.dp),
            )
            .clickable(onClick = onClick)
            .padding(vertical = 8.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = label,
            color = if (selected) AetherPrimary else AetherOnSurfaceVariant,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 0.8.sp,
            fontFamily = FontFamily.Monospace,
        )
    }
}

@Composable
private fun PlaylistCardRow(
    playlist: MusicPlaylist,
    onOpen: () -> Unit,
    onPlayAll: () -> Unit,
    onDelete: () -> Unit,
) {
    val coverUrl = playlist.tracks.firstOrNull { it.thumbnailUrl.isNotBlank() }?.thumbnailUrl.orEmpty()
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(Color.White.copy(alpha = 0.04f))
            .border(1.dp, AetherGlassBorderSubtle, RoundedCornerShape(14.dp))
            .clickable(onClick = onOpen)
            .padding(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(46.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(AetherPrimary.copy(alpha = 0.14f)),
            contentAlignment = Alignment.Center,
        ) {
            if (coverUrl.isNotBlank()) {
                AsyncImage(
                    model = coverUrl,
                    contentDescription = playlist.name,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize(),
                )
            } else {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.QueueMusic,
                    contentDescription = null,
                    tint = AetherPrimary,
                    modifier = Modifier.size(22.dp),
                )
            }
        }

        Spacer(modifier = Modifier.width(10.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = playlist.name,
                color = AetherOnSurface,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = "${playlist.tracks.size} lagu",
                color = AetherOnSurfaceVariant,
                fontSize = 11.sp,
            )
        }

        if (playlist.tracks.isNotEmpty()) {
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(AetherPrimary.copy(alpha = 0.18f))
                    .clickable(onClick = onPlayAll),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.Default.PlayArrow,
                    contentDescription = "Putar playlist",
                    tint = AetherPrimary,
                    modifier = Modifier.size(18.dp),
                )
            }
            Spacer(modifier = Modifier.width(6.dp))
        }

        Box(
            modifier = Modifier
                .size(32.dp)
                .clip(CircleShape)
                .background(Color.White.copy(alpha = 0.05f))
                .clickable(onClick = onDelete),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = Icons.Default.DeleteOutline,
                contentDescription = "Hapus playlist",
                tint = AetherOnSurfaceVariant,
                modifier = Modifier.size(16.dp),
            )
        }
    }
}

@Composable
private fun TrackRow(
    track: MusicTrack,
    isSelected: Boolean,
    isPlaying: Boolean,
    actionIcon: ImageVector,
    actionContentDescription: String,
    onActionClick: () -> Unit,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(
                if (isSelected) AetherPrimary.copy(alpha = 0.12f)
                else Color.White.copy(alpha = 0.03f)
            )
            .border(
                1.dp,
                if (isSelected) AetherPrimary.copy(alpha = 0.5f) else Color.Transparent,
                RoundedCornerShape(14.dp)
            )
            .clickable(onClick = onClick)
            .padding(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // Thumbnail
        Box(
            modifier = Modifier
                .size(44.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(Color.Black.copy(alpha = 0.4f)),
            contentAlignment = Alignment.Center,
        ) {
            if (track.thumbnailUrl.isNotBlank()) {
                AsyncImage(
                    model = track.thumbnailUrl,
                    contentDescription = track.title,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize(),
                )
            } else {
                Icon(
                    imageVector = Icons.Default.MusicNote,
                    contentDescription = null,
                    tint = AetherOnSurfaceVariant,
                    modifier = Modifier.size(20.dp),
                )
            }
        }

        Spacer(modifier = Modifier.width(10.dp))

        // Info
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = track.title,
                color = if (isSelected) AetherPrimary else AetherOnSurface,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = track.artist,
                color = AetherOnSurfaceVariant,
                fontSize = 11.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }

        if (track.durationText.isNotBlank()) {
            Text(
                text = track.durationText,
                color = AetherOnSurfaceVariant.copy(alpha = 0.7f),
                fontSize = 10.sp,
                fontFamily = FontFamily.Monospace,
                modifier = Modifier.padding(horizontal = 4.dp),
            )
        }

        // Indicator
        if (isPlaying) {
            Icon(
                imageVector = Icons.Default.GraphicEq,
                contentDescription = "Playing",
                tint = AetherPrimary,
                modifier = Modifier
                    .padding(end = 4.dp)
                    .size(16.dp),
            )
        }

        // Action button (Add to Playlist / Remove from Playlist)
        Box(
            modifier = Modifier
                .size(30.dp)
                .clip(CircleShape)
                .background(Color.White.copy(alpha = 0.06f))
                .clickable(onClick = onActionClick),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = actionIcon,
                contentDescription = actionContentDescription,
                tint = AetherOnSurfaceVariant,
                modifier = Modifier.size(16.dp),
            )
        }
    }
}

@Composable
private fun NowPlayingMiniCard(
    track: MusicTrack,
    playerState: MusicPlayerState,
    onTogglePlayPause: () -> Unit,
    onNextTrack: () -> Unit,
    onPreviousTrack: () -> Unit,
    onTogglePlaybackOrderMode: () -> Unit,
) {
    val isShuffle = playerState.playbackOrderMode == PlaybackOrderMode.SHUFFLE
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(Color(0xFF141923))
            .border(1.dp, AetherGlassBorder, RoundedCornerShape(18.dp))
            .padding(12.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(Color.Black.copy(alpha = 0.5f)),
                contentAlignment = Alignment.Center,
            ) {
                if (track.thumbnailUrl.isNotBlank()) {
                    AsyncImage(
                        model = track.thumbnailUrl,
                        contentDescription = track.title,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize(),
                    )
                } else {
                    Icon(
                        imageVector = Icons.Default.MusicNote,
                        contentDescription = null,
                        tint = AetherPrimary,
                        modifier = Modifier.size(20.dp),
                    )
                }
            }

            Spacer(modifier = Modifier.width(10.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = track.title,
                    color = AetherOnSurface,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = track.artist,
                    color = AetherPrimary,
                    fontSize = 11.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }

            // Controls
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .clip(CircleShape)
                        .background(
                            if (isShuffle) AetherPrimary.copy(alpha = 0.2f)
                            else Color.White.copy(alpha = 0.06f),
                        )
                        .clickable(onClick = onTogglePlaybackOrderMode),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = if (isShuffle) Icons.Default.Shuffle else Icons.Default.Repeat,
                        contentDescription = if (isShuffle) "Mode Acak" else "Mode Sesuai Urutan",
                        tint = if (isShuffle) AetherPrimary else AetherOnSurfaceVariant,
                        modifier = Modifier.size(16.dp),
                    )
                }
                Spacer(modifier = Modifier.width(4.dp))
                Icon(
                    imageVector = Icons.Default.SkipPrevious,
                    contentDescription = "Previous",
                    tint = AetherOnSurfaceVariant,
                    modifier = Modifier
                        .size(26.dp)
                        .clickable(onClick = onPreviousTrack),
                )
                Spacer(modifier = Modifier.width(4.dp))
                Box(
                    modifier = Modifier
                        .size(34.dp)
                        .clip(CircleShape)
                        .background(AetherPrimary)
                        .clickable(onClick = onTogglePlayPause),
                    contentAlignment = Alignment.Center,
                ) {
                    if (playerState.isBuffering) {
                        CircularProgressIndicator(
                            color = Color.Black,
                            strokeWidth = 2.dp,
                            modifier = Modifier.size(16.dp),
                        )
                    } else {
                        Icon(
                            imageVector = if (playerState.isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                            contentDescription = if (playerState.isPlaying) "Pause" else "Play",
                            tint = Color.Black,
                            modifier = Modifier.size(20.dp),
                        )
                    }
                }
                Spacer(modifier = Modifier.width(4.dp))
                Icon(
                    imageVector = Icons.Default.SkipNext,
                    contentDescription = "Next",
                    tint = AetherOnSurfaceVariant,
                    modifier = Modifier
                        .size(26.dp)
                        .clickable(onClick = onNextTrack),
                )
            }
        }

        if (playerState.durationMs > 0) {
            Spacer(modifier = Modifier.height(8.dp))
            val progress = (playerState.currentPositionMs.toFloat() / playerState.durationMs.toFloat())
                .coerceIn(0f, 1f)
            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(3.dp)
                    .clip(CircleShape),
                color = AetherPrimary,
                trackColor = Color.White.copy(alpha = 0.1f),
            )
        }
    }
}
