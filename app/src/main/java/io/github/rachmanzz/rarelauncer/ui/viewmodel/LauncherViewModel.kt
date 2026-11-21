package io.github.rachmanzz.rarelauncer.ui.viewmodel

import android.app.Application
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import io.github.rachmanzz.rarelauncer.data.model.AppDirectory
import io.github.rachmanzz.rarelauncer.data.model.AppItem
import io.github.rachmanzz.rarelauncer.data.model.AtmospherePreset
import io.github.rachmanzz.rarelauncer.data.model.ContactItem
import io.github.rachmanzz.rarelauncer.data.model.MusicPlayerState
import io.github.rachmanzz.rarelauncer.data.model.MusicPlaylist
import io.github.rachmanzz.rarelauncer.data.model.MusicTrack
import io.github.rachmanzz.rarelauncer.data.model.PlaybackOrderMode
import io.github.rachmanzz.rarelauncer.data.model.WallpaperConfig
import io.github.rachmanzz.rarelauncer.data.model.YouTubeWebController
import io.github.rachmanzz.rarelauncer.data.repository.LauncherRepository
import io.github.rachmanzz.rarelauncer.data.repository.MusicRepository
import io.github.rachmanzz.rarelauncer.service.LockScreenService
import io.github.rachmanzz.rarelauncer.service.MusicPlaybackService
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

enum class DrawerTab {
    ACTIVE,
    HIDDEN
}

data class ClockUiState(
    val hours: String = "00",
    val minutes: String = "00",
    val seconds: String = "00",
    val dateText: String = "",
    val timeOfDayGreeting: String = "Welcome"
)

class LauncherViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = LauncherRepository(application)
    private val musicRepository = MusicRepository()

    private var exoPlayer: ExoPlayer? = null
    private var progressTrackingJob: Job? = null
    private var musicSearchJob: Job? = null
    private var youTubeController: YouTubeWebController? = null
    private var pendingYouTubeVideoId: String? = null
    private var shuffledTrackIds: List<String> = emptyList()
    private var shuffledQueueSignature: List<String> = emptyList()
    private var userPausedYouTube: Boolean = false
    private var lastTrackAdvanceTimestampMs: Long = 0L

    // Music Player State
    private val _isMusicDrawerOpen = MutableStateFlow(false)
    val isMusicDrawerOpen: StateFlow<Boolean> = _isMusicDrawerOpen.asStateFlow()

    private val _musicPlayerState = MutableStateFlow(
        MusicPlayerState(
            playbackOrderMode = musicRepository.loadPlaybackOrderMode(application),
        ),
    )
    val musicPlayerState: StateFlow<MusicPlayerState> = _musicPlayerState.asStateFlow()

    private val _musicSearchQuery = MutableStateFlow("")
    val musicSearchQuery: StateFlow<String> = _musicSearchQuery.asStateFlow()

    private val _musicSearchResults = MutableStateFlow<List<MusicTrack>>(musicRepository.curatedTrendingTracks)
    val musicSearchResults: StateFlow<List<MusicTrack>> = _musicSearchResults.asStateFlow()

    private val _isSearchingMusic = MutableStateFlow(false)
    val isSearchingMusic: StateFlow<Boolean> = _isSearchingMusic.asStateFlow()

    private val _musicPlaylists = MutableStateFlow<List<MusicPlaylist>>(
        musicRepository.loadPlaylists(application),
    )
    val musicPlaylists: StateFlow<List<MusicPlaylist>> = _musicPlaylists.asStateFlow()

    private val _clockState = MutableStateFlow(ClockUiState())
    val clockState: StateFlow<ClockUiState> = _clockState.asStateFlow()

    private val _allApps = MutableStateFlow<List<AppItem>>(emptyList())
    val allApps: StateFlow<List<AppItem>> = _allApps.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    // Hidden Apps & Fingerprint Vault
    private val _hiddenApps = MutableStateFlow<Set<String>>(emptySet())
    val hiddenApps: StateFlow<Set<String>> = _hiddenApps.asStateFlow()

    // Per-App Lock (visible in All Apps & Directory, requires Fingerprint/PIN on open)
    private val _lockedApps = MutableStateFlow<Set<String>>(emptySet())
    val lockedApps: StateFlow<Set<String>> = _lockedApps.asStateFlow()

    private val _isVaultUnlocked = MutableStateFlow(false)
    val isVaultUnlocked: StateFlow<Boolean> = _isVaultUnlocked.asStateFlow()

    private val _drawerTab = MutableStateFlow(DrawerTab.ACTIVE)
    val drawerTab: StateFlow<DrawerTab> = _drawerTab.asStateFlow()

    val filteredApps: StateFlow<List<AppItem>> = combine(
        _allApps,
        _searchQuery,
        _hiddenApps,
        _drawerTab,
        _isVaultUnlocked
    ) { apps, query, hidden, tab, unlocked ->
        val q = query.trim().lowercase()

        val baseApps = when (tab) {
            DrawerTab.ACTIVE -> {
                // Show ONLY non-hidden apps in active view
                apps.filter { it.packageName !in hidden }
            }
            DrawerTab.HIDDEN -> {
                // In hidden vault: show hidden apps ONLY if unlocked with fingerprint
                if (unlocked) {
                    apps.filter { it.packageName in hidden }
                } else {
                    emptyList()
                }
            }
        }

        if (q.isBlank()) {
            baseApps
        } else {
            baseApps.filter { it.label.lowercase().contains(q) || it.packageName.lowercase().contains(q) }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Exactly 1 App Directory (strictly controlled by the user, NO automatic selection)
    private val _appDirectory = MutableStateFlow(AppDirectory("primary_dir", "Workspaces", emptyList()))
    val appDirectory: StateFlow<AppDirectory> = _appDirectory.asStateFlow()

    private val _isFolderPopoverOpen = MutableStateFlow(false)
    val isFolderPopoverOpen: StateFlow<Boolean> = _isFolderPopoverOpen.asStateFlow()

    // Slide-up Full App Drawer State
    private val _isAppDrawerOpen = MutableStateFlow(false)
    val isAppDrawerOpen: StateFlow<Boolean> = _isAppDrawerOpen.asStateFlow()

    // Slide-down Quick Recents & Search Drawer State
    private val _isQuickRecentsOpen = MutableStateFlow(false)
    val isQuickRecentsOpen: StateFlow<Boolean> = _isQuickRecentsOpen.asStateFlow()

    private val _quickSearchQuery = MutableStateFlow("")
    val quickSearchQuery: StateFlow<String> = _quickSearchQuery.asStateFlow()

    private val _bookmarkedPackageNames = MutableStateFlow<List<String>>(emptyList())
    val bookmarkedPackageNames: StateFlow<List<String>> = _bookmarkedPackageNames.asStateFlow()

    private val _recentPackageNames = MutableStateFlow<List<String>>(emptyList())
    val recentPackageNames: StateFlow<List<String>> = _recentPackageNames.asStateFlow()

    val bookmarkedApps: StateFlow<List<AppItem>> = combine(_allApps, _bookmarkedPackageNames, _hiddenApps) { apps, bookmarks, hidden ->
        val appMap = apps.associateBy { it.packageName }
        bookmarks.filter { it !in hidden }.take(10).mapNotNull { appMap[it] }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val recentApps: StateFlow<List<AppItem>> = combine(_allApps, _recentPackageNames, _hiddenApps) { apps, recents, hidden ->
        val visibleApps = apps.filter { it.packageName !in hidden }
        val appMap = visibleApps.associateBy { it.packageName }
        val recentList = recents.filter { it !in hidden }.take(20).mapNotNull { appMap[it] }

        if (recentList.size >= 20) {
            recentList
        } else {
            // Fill remaining slots up to 20 with visible apps (alphabetical fallback, excluding already shown recents)
            val recentPkgSet = recentList.map { it.packageName }.toSet()
            val fallback = visibleApps.filter { it.packageName !in recentPkgSet }
                .sortedBy { it.label.lowercase() }
                .take(20 - recentList.size)
            recentList + fallback
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val recentClusterApps: StateFlow<List<AppItem>> = combine(_allApps, _recentPackageNames, _hiddenApps) { apps, recents, hidden ->
        val appMap = apps.associateBy { it.packageName }
        recents.filter { it !in hidden }.take(4).mapNotNull { appMap[it] }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val lastActiveRecentApp: StateFlow<AppItem?> = combine(_allApps, _recentPackageNames, _hiddenApps) { apps, recents, hidden ->
        val appMap = apps.associateBy { it.packageName }
        recents.firstOrNull { it !in hidden }?.let { appMap[it] }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    private val _isRecentBubbleVisible = MutableStateFlow(false)
    val isRecentBubbleVisible: StateFlow<Boolean> = _isRecentBubbleVisible.asStateFlow()
    private var bubbleDismissJob: Job? = null

    private val _isRecentClusterDialogOpen = MutableStateFlow(false)
    val isRecentClusterDialogOpen: StateFlow<Boolean> = _isRecentClusterDialogOpen.asStateFlow()

    val quickSearchResults: StateFlow<List<AppItem>> = combine(_allApps, _quickSearchQuery, _hiddenApps) { apps, query, hidden ->
        val q = query.trim().lowercase()
        val visible = apps.filter { it.packageName !in hidden }
        if (q.isBlank()) {
            emptyList()
        } else {
            visible.filter { it.label.lowercase().contains(q) || it.packageName.lowercase().contains(q) }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _isCommDrawerOpen = MutableStateFlow(false)
    val isCommDrawerOpen: StateFlow<Boolean> = _isCommDrawerOpen.asStateFlow()

    private val _wallpaperConfig = MutableStateFlow(WallpaperConfig())
    val wallpaperConfig: StateFlow<WallpaperConfig> = _wallpaperConfig.asStateFlow()

    private val _contacts = MutableStateFlow<List<ContactItem>>(emptyList())
    val contacts: StateFlow<List<ContactItem>> = _contacts.asStateFlow()

    private val _hasContactsPermission = MutableStateFlow(repository.hasContactsPermission())
    val hasContactsPermission: StateFlow<Boolean> = _hasContactsPermission.asStateFlow()

    private val _batteryLevel = MutableStateFlow(100)
    val batteryLevel: StateFlow<Int> = _batteryLevel.asStateFlow()

    private val _isCharging = MutableStateFlow(false)
    val isCharging: StateFlow<Boolean> = _isCharging.asStateFlow()

    private val batteryReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            intent?.let {
                val level = it.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
                val scale = it.getIntExtra(BatteryManager.EXTRA_SCALE, -1)
                if (level >= 0 && scale > 0) {
                    _batteryLevel.value = (level * 100) / scale
                }
                val status = it.getIntExtra(BatteryManager.EXTRA_STATUS, -1)
                _isCharging.value = status == BatteryManager.BATTERY_STATUS_CHARGING ||
                        status == BatteryManager.BATTERY_STATUS_FULL
            }
        }
    }

    init {
        resumeClock()
        loadInstalledAppsAndDirs()
        loadWallpaperConfig()
        loadContacts()
        registerBatteryReceiver()
        initExoPlayer()
        registerMusicServiceCallbacks()
    }

    private fun registerMusicServiceCallbacks() {
        MusicPlaybackService.registerCallbacks(object : MusicPlaybackService.PlaybackCallbacks {
            override fun onPlay() {
                viewModelScope.launch { resumePlayback() }
            }

            override fun onPause() {
                viewModelScope.launch { pausePlayback() }
            }

            override fun onNext() {
                viewModelScope.launch { playNextTrack() }
            }

            override fun onPrevious() {
                viewModelScope.launch { playPreviousTrack() }
            }

            override fun onStop() {
                viewModelScope.launch { stopMusicPlayback() }
            }
        })
    }

    private fun syncMusicService() {
        val state = _musicPlayerState.value
        val track = state.currentTrack
        if (track == null) {
            MusicPlaybackService.stop(getApplication())
        } else {
            MusicPlaybackService.syncState(
                context = getApplication(),
                title = track.title,
                artist = track.artist,
                thumbnailUrl = track.thumbnailUrl,
                isPlaying = state.isPlaying || state.isBuffering,
                durationMs = state.durationMs,
            )
        }
    }

    private fun initExoPlayer() {
        val player = ExoPlayer.Builder(getApplication()).build()
        player.addListener(object : Player.Listener {
            override fun onPlaybackStateChanged(playbackState: Int) {
                val isBuffering = playbackState == Player.STATE_BUFFERING
                val isEnded = playbackState == Player.STATE_ENDED
                _musicPlayerState.value = _musicPlayerState.value.copy(
                    isBuffering = isBuffering,
                    durationMs = player.duration.coerceAtLeast(0L),
                )
                if (isEnded && _musicPlayerState.value.currentTrack?.streamUrl?.isNotBlank() == true) {
                    handleTrackEnded()
                }
            }

            override fun onIsPlayingChanged(isPlaying: Boolean) {
                _musicPlayerState.value = _musicPlayerState.value.copy(
                    isPlaying = isPlaying,
                    isBuffering = false,
                    durationMs = player.duration.coerceAtLeast(0L),
                )
                if (isPlaying) {
                    startProgressTracking()
                } else {
                    stopProgressTracking()
                }
                syncMusicService()
            }
        })
        exoPlayer = player
    }

    private fun startProgressTracking() {
        progressTrackingJob?.cancel()
        progressTrackingJob = viewModelScope.launch {
            while (isActive) {
                exoPlayer?.let { player ->
                    _musicPlayerState.value = _musicPlayerState.value.copy(
                        currentPositionMs = player.currentPosition.coerceAtLeast(0L),
                        durationMs = player.duration.coerceAtLeast(0L),
                    )
                }
                delay(500L)
            }
        }
    }

    private fun stopProgressTracking() {
        progressTrackingJob?.cancel()
        progressTrackingJob = null
    }

    fun openMusicDrawer() {
        _isMusicDrawerOpen.value = true
    }

    fun closeMusicDrawer() {
        _isMusicDrawerOpen.value = false
    }

    fun onMusicSearchQueryChange(query: String) {
        _musicSearchQuery.value = query
        musicSearchJob?.cancel()
        if (query.isBlank()) {
            _musicSearchResults.value = musicRepository.curatedTrendingTracks
            _isSearchingMusic.value = false
            return
        }
        musicSearchJob = viewModelScope.launch {
            delay(500L) // Debounce search
            _isSearchingMusic.value = true
            val results = musicRepository.searchTracks(query)
            _musicSearchResults.value = if (results.isNotEmpty()) results else musicRepository.curatedTrendingTracks
            _isSearchingMusic.value = false
        }
    }

    fun clearMusicSearch() {
        _musicSearchQuery.value = ""
        musicSearchJob?.cancel()
        _musicSearchResults.value = musicRepository.curatedTrendingTracks
        _isSearchingMusic.value = false
    }

    fun setYouTubeController(controller: YouTubeWebController) {
        youTubeController = controller
        pendingYouTubeVideoId?.let { videoId ->
            controller.loadVideo(videoId)
            pendingYouTubeVideoId = null
        }
    }

    fun onYouTubePlayerStateChanged(state: Int) {
        when (state) {
            1 -> { // PLAYING
                userPausedYouTube = false
                _musicPlayerState.value = _musicPlayerState.value.copy(
                    isPlaying = true,
                    isBuffering = false,
                    error = null,
                )
                syncMusicService()
            }
            2 -> { // PAUSED
                val current = _musicPlayerState.value
                val reachedEnd = !userPausedYouTube &&
                    current.currentTrack?.streamUrl?.isBlank() == true &&
                    current.durationMs > 3000L &&
                    current.currentPositionMs >= current.durationMs - 1500L
                if (reachedEnd) {
                    handleTrackEnded()
                } else {
                    _musicPlayerState.value = current.copy(
                        isPlaying = false,
                        isBuffering = false,
                    )
                    syncMusicService()
                }
            }
            3 -> { // BUFFERING
                _musicPlayerState.value = _musicPlayerState.value.copy(
                    isBuffering = true,
                )
            }
            0 -> { // ENDED
                handleTrackEnded()
            }
        }
    }

    private fun handleTrackEnded() {
        val now = System.currentTimeMillis()
        if (now - lastTrackAdvanceTimestampMs < 1500L) {
            return
        }
        lastTrackAdvanceTimestampMs = now
        _musicPlayerState.value = _musicPlayerState.value.copy(
            isPlaying = false,
            isBuffering = false,
        )
        playNextTrack()
    }

    fun onYouTubeCurrentSecond(second: Float) {
        val currentTrack = _musicPlayerState.value.currentTrack
        if (currentTrack != null && currentTrack.streamUrl.isBlank()) {
            _musicPlayerState.value = _musicPlayerState.value.copy(
                currentPositionMs = (second * 1000).toLong(),
            )
        }
    }

    fun onYouTubeVideoDuration(duration: Float) {
        val currentTrack = _musicPlayerState.value.currentTrack
        if (currentTrack != null && currentTrack.streamUrl.isBlank()) {
            _musicPlayerState.value = _musicPlayerState.value.copy(
                durationMs = (duration * 1000).toLong(),
            )
        }
    }

    fun onYouTubeError(errorCode: Int) {
        android.util.Log.e("RareMusic", "YouTube player error code: $errorCode")
        val msg = when (errorCode) {
            101, 150 -> "Video tidak mengizinkan pemutaran embed"
            100 -> "Video YouTube tidak ditemukan"
            2 -> "Parameter video tidak valid"
            else -> "Error pemutaran YouTube ($errorCode)"
        }
        _musicPlayerState.value = _musicPlayerState.value.copy(
            isBuffering = false,
            isPlaying = false,
            error = msg,
        )
    }

    fun setPlaybackOrderMode(mode: PlaybackOrderMode) {
        val currentState = _musicPlayerState.value
        if (currentState.playbackOrderMode == mode) return
        musicRepository.savePlaybackOrderMode(getApplication(), mode)
        _musicPlayerState.value = currentState.copy(playbackOrderMode = mode)
        if (mode == PlaybackOrderMode.SHUFFLE) {
            val activeQueue = currentState.queue.ifEmpty { _musicSearchResults.value }
            if (activeQueue.isNotEmpty()) {
                shuffledTrackIds = buildShuffledIds(
                    queue = activeQueue,
                    anchorFirstId = currentState.currentTrack?.id,
                )
                shuffledQueueSignature = activeQueue.map { it.id }
            }
        }
    }

    fun togglePlaybackOrderMode() {
        val nextMode = if (_musicPlayerState.value.playbackOrderMode == PlaybackOrderMode.SHUFFLE) {
            PlaybackOrderMode.SEQUENTIAL
        } else {
            PlaybackOrderMode.SHUFFLE
        }
        setPlaybackOrderMode(nextMode)
    }

    fun playPlaylist(tracks: List<MusicTrack>, shuffle: Boolean) {
        if (tracks.isEmpty()) return
        val mode = if (shuffle) PlaybackOrderMode.SHUFFLE else PlaybackOrderMode.SEQUENTIAL
        musicRepository.savePlaybackOrderMode(getApplication(), mode)
        _musicPlayerState.value = _musicPlayerState.value.copy(playbackOrderMode = mode)

        if (shuffle) {
            shuffledTrackIds = buildShuffledIds(tracks)
            shuffledQueueSignature = tracks.map { it.id }
            val firstTrack = tracks.find { it.id == shuffledTrackIds.firstOrNull() } ?: tracks.first()
            playTrack(firstTrack, tracks, preserveShuffleOrder = true)
        } else {
            playTrack(tracks.first(), tracks, preserveShuffleOrder = false)
        }
    }

    fun playTrack(
        track: MusicTrack,
        queue: List<MusicTrack>? = null,
        preserveShuffleOrder: Boolean = false,
    ) {
        viewModelScope.launch {
            val currentState = _musicPlayerState.value
            val activeQueue = queue ?: currentState.queue.ifEmpty { _musicSearchResults.value }
            userPausedYouTube = false
            lastTrackAdvanceTimestampMs = System.currentTimeMillis()

            if (!preserveShuffleOrder && currentState.playbackOrderMode == PlaybackOrderMode.SHUFFLE) {
                shuffledTrackIds = buildShuffledIds(activeQueue, anchorFirstId = track.id)
                shuffledQueueSignature = activeQueue.map { it.id }
            }

            _musicPlayerState.value = currentState.copy(
                currentTrack = track,
                isBuffering = true,
                isPlaying = false,
                currentPositionMs = 0L,
                durationMs = 0L,
                queue = activeQueue,
                error = null,
            )
            syncMusicService()

            android.util.Log.d("RareMusic", "playTrack: id=${track.id}, title=${track.title}, streamUrl=${track.streamUrl}")

            if (track.streamUrl.isNotBlank()) {
                // Direct MP3 stream (e.g. Nightride FM)
                youTubeController?.pause()
                exoPlayer?.let { player ->
                    val mediaItem = MediaItem.Builder()
                        .setUri(track.streamUrl)
                        .setMediaMetadata(
                            MediaMetadata.Builder()
                                .setTitle(track.title)
                                .setArtist(track.artist)
                                .build()
                        )
                        .build()
                    player.setMediaItem(mediaItem)
                    player.prepare()
                    player.play()
                }
            } else {
                // Native YouTube stream via embedded player
                exoPlayer?.stop()
                exoPlayer?.clearMediaItems()
                val controller = youTubeController
                if (controller != null) {
                    controller.loadVideo(track.id)
                } else {
                    pendingYouTubeVideoId = track.id
                }
            }
        }
    }

    fun resumePlayback() {
        val currentTrack = _musicPlayerState.value.currentTrack ?: return
        if (currentTrack.streamUrl.isBlank()) {
            userPausedYouTube = false
            youTubeController?.play()
        } else {
            exoPlayer?.let { player ->
                if (player.playbackState == Player.STATE_IDLE || player.playbackState == Player.STATE_ENDED) {
                    playTrack(currentTrack)
                } else {
                    player.play()
                }
            }
        }
    }

    fun pausePlayback() {
        val currentTrack = _musicPlayerState.value.currentTrack ?: return
        if (currentTrack.streamUrl.isBlank()) {
            userPausedYouTube = true
            youTubeController?.pause()
            _musicPlayerState.value = _musicPlayerState.value.copy(isPlaying = false, isBuffering = false)
            syncMusicService()
        } else {
            exoPlayer?.pause()
        }
    }

    fun stopMusicPlayback() {
        userPausedYouTube = true
        youTubeController?.pause()
        exoPlayer?.stop()
        exoPlayer?.clearMediaItems()
        stopProgressTracking()
        _musicPlayerState.value = MusicPlayerState(
            playbackOrderMode = _musicPlayerState.value.playbackOrderMode,
        )
        MusicPlaybackService.stop(getApplication())
    }

    fun togglePlayPause() {
        if (_musicPlayerState.value.isPlaying) {
            pausePlayback()
        } else {
            resumePlayback()
        }
    }

    fun seekTo(positionMs: Long) {
        val currentTrack = _musicPlayerState.value.currentTrack
        if (currentTrack != null && currentTrack.streamUrl.isBlank()) {
            youTubeController?.seekTo(positionMs / 1000f)
        } else {
            exoPlayer?.seekTo(positionMs)
        }
    }

    fun playNextTrack() {
        val state = _musicPlayerState.value
        val currentQueue = state.queue.ifEmpty { _musicSearchResults.value }
        if (currentQueue.isEmpty()) return
        val currentId = state.currentTrack?.id

        val nextTrack = when (state.playbackOrderMode) {
            PlaybackOrderMode.SEQUENTIAL -> {
                val currentIdx = currentQueue.indexOfFirst { it.id == currentId }
                if (currentIdx in 0 until currentQueue.lastIndex) {
                    currentQueue[currentIdx + 1]
                } else {
                    currentQueue.first()
                }
            }
            PlaybackOrderMode.SHUFFLE -> {
                if (currentQueue.size == 1) {
                    currentQueue.first()
                } else {
                    ensureShuffleOrder(currentQueue, anchorTrackId = currentId)
                    val idx = shuffledTrackIds.indexOf(currentId)
                    val nextId = if (idx in 0 until shuffledTrackIds.lastIndex) {
                        shuffledTrackIds[idx + 1]
                    } else {
                        val fresh = buildShuffledIds(currentQueue, avoidFirstId = currentId)
                        shuffledTrackIds = fresh
                        fresh.first()
                    }
                    currentQueue.find { it.id == nextId } ?: currentQueue.first()
                }
            }
        }
        playTrack(nextTrack, currentQueue, preserveShuffleOrder = true)
    }

    fun playPreviousTrack() {
        val state = _musicPlayerState.value
        val currentQueue = state.queue.ifEmpty { _musicSearchResults.value }
        if (currentQueue.isEmpty()) return
        val currentId = state.currentTrack?.id

        val prevTrack = when (state.playbackOrderMode) {
            PlaybackOrderMode.SEQUENTIAL -> {
                val currentIdx = currentQueue.indexOfFirst { it.id == currentId }
                if (currentIdx > 0) {
                    currentQueue[currentIdx - 1]
                } else {
                    currentQueue.last()
                }
            }
            PlaybackOrderMode.SHUFFLE -> {
                if (currentQueue.size == 1) {
                    currentQueue.first()
                } else {
                    ensureShuffleOrder(currentQueue, anchorTrackId = currentId)
                    val idx = shuffledTrackIds.indexOf(currentId)
                    val prevId = if (idx > 0) {
                        shuffledTrackIds[idx - 1]
                    } else {
                        shuffledTrackIds.last()
                    }
                    currentQueue.find { it.id == prevId } ?: currentQueue.last()
                }
            }
        }
        playTrack(prevTrack, currentQueue, preserveShuffleOrder = true)
    }

    private fun ensureShuffleOrder(queue: List<MusicTrack>, anchorTrackId: String?) {
        val signature = queue.map { it.id }
        if (shuffledQueueSignature != signature || shuffledTrackIds.isEmpty()) {
            shuffledTrackIds = buildShuffledIds(queue, anchorFirstId = anchorTrackId)
            shuffledQueueSignature = signature
        }
    }

    private fun buildShuffledIds(
        queue: List<MusicTrack>,
        anchorFirstId: String? = null,
        avoidFirstId: String? = null,
    ): List<String> {
        val ids = queue.map { it.id }.distinct()
        if (ids.size <= 1) return ids
        if (anchorFirstId != null && anchorFirstId in ids) {
            return listOf(anchorFirstId) + ids.filterNot { it == anchorFirstId }.shuffled()
        }
        val shuffled = ids.shuffled().toMutableList()
        if (avoidFirstId != null && shuffled.first() == avoidFirstId) {
            val swapIndex = (1 until shuffled.size).random()
            val tmp = shuffled[0]
            shuffled[0] = shuffled[swapIndex]
            shuffled[swapIndex] = tmp
        }
        return shuffled
    }

    fun createPlaylist(name: String, initialTrack: MusicTrack? = null) {
        val cleanName = name.trim()
        if (cleanName.isBlank()) return
        val newPlaylist = MusicPlaylist(
            id = "playlist_${System.currentTimeMillis()}",
            name = cleanName,
            tracks = listOfNotNull(initialTrack),
        )
        val updated = _musicPlaylists.value + newPlaylist
        _musicPlaylists.value = updated
        musicRepository.savePlaylists(getApplication(), updated)
    }

    fun deletePlaylist(playlistId: String) {
        val updated = _musicPlaylists.value.filterNot { it.id == playlistId }
        _musicPlaylists.value = updated
        musicRepository.savePlaylists(getApplication(), updated)
    }

    fun addTrackToPlaylist(playlistId: String, track: MusicTrack) {
        val updated = _musicPlaylists.value.map { playlist ->
            if (playlist.id == playlistId && playlist.tracks.none { it.id == track.id }) {
                playlist.copy(tracks = playlist.tracks + track)
            } else {
                playlist
            }
        }
        _musicPlaylists.value = updated
        musicRepository.savePlaylists(getApplication(), updated)
    }

    fun removeTrackFromPlaylist(playlistId: String, trackId: String) {
        val updated = _musicPlaylists.value.map { playlist ->
            if (playlist.id == playlistId) {
                playlist.copy(tracks = playlist.tracks.filterNot { it.id == trackId })
            } else {
                playlist
            }
        }
        _musicPlaylists.value = updated
        musicRepository.savePlaylists(getApplication(), updated)
    }

    private fun registerBatteryReceiver() {
        val filter = IntentFilter(Intent.ACTION_BATTERY_CHANGED)
        getApplication<Application>().registerReceiver(batteryReceiver, filter)
    }

    override fun onCleared() {
        super.onCleared()
        stopProgressTracking()
        MusicPlaybackService.registerCallbacks(null)
        MusicPlaybackService.stop(getApplication())
        exoPlayer?.release()
        exoPlayer = null
        youTubeController = null
        try {
            getApplication<Application>().unregisterReceiver(batteryReceiver)
        } catch (e: Exception) {
            // ignore
        }
    }

    private var clockJob: Job? = null

    fun resumeClock() {
        if (clockJob?.isActive == true) return
        clockJob = viewModelScope.launch {
            val hourFormat = SimpleDateFormat("HH", Locale.getDefault())
            val minFormat = SimpleDateFormat("mm", Locale.getDefault())
            val secFormat = SimpleDateFormat("ss", Locale.getDefault())
            val dateFormat = SimpleDateFormat("EEEE, MMM d", Locale.getDefault())

            while (isActive) {
                val now = Date()
                val currentHour = now.hours
                val greeting = when (currentHour) {
                    in 5..11 -> "Good Morning"
                    in 12..17 -> "Good Afternoon"
                    in 18..21 -> "Good Evening"
                    else -> "Night Mode"
                }

                _clockState.value = ClockUiState(
                    hours = hourFormat.format(now),
                    minutes = minFormat.format(now),
                    seconds = secFormat.format(now),
                    dateText = dateFormat.format(now),
                    timeOfDayGreeting = greeting
                )
                delay(1000)
            }
        }
    }

    fun pauseClock() {
        clockJob?.cancel()
        clockJob = null
    }

    fun loadInstalledAppsAndDirs() {
        viewModelScope.launch {
            val apps = repository.getInstalledApps()
            val hidden = repository.getHiddenApps()
            val locked = repository.getLockedApps()
            val storedBookmarks = repository.getBookmarkedApps()
            val recents = repository.getRecentApps()
            var dir = repository.getAppDirectory()
            // Strictly exclude hidden apps from directory and cap at max 4 apps
            val sanitizedList = dir.packageNames.filter { it !in hidden }.take(4)
            if (sanitizedList != dir.packageNames) {
                dir = dir.copy(packageNames = sanitizedList)
                repository.saveAppDirectory(dir)
            }

            // If bookmarks not yet initialized, pick first 4 non-hidden apps (or apps in directory) as defaults
            val effectiveBookmarks = if (storedBookmarks == null) {
                val defaults = if (sanitizedList.isNotEmpty()) {
                    sanitizedList
                } else {
                    apps.filter { it.packageName !in hidden }.take(4).map { it.packageName }
                }
                repository.saveBookmarkedApps(defaults)
                defaults
            } else {
                storedBookmarks.filter { it !in hidden }
            }

            _allApps.value = apps
            _appDirectory.value = dir
            _hiddenApps.value = hidden
            _lockedApps.value = locked
            _bookmarkedPackageNames.value = effectiveBookmarks
            _recentPackageNames.value = recents.filter { it !in hidden }.take(10)
        }
    }

    private fun loadWallpaperConfig() {
        _wallpaperConfig.value = repository.getWallpaperConfig()
    }

    fun loadContacts() {
        viewModelScope.launch {
            val granted = repository.hasContactsPermission()
            _hasContactsPermission.value = granted
            _contacts.value = if (granted) repository.getQuickContacts() else emptyList()
        }
    }

    fun onSearchQueryChange(query: String) {
        _searchQuery.value = query
    }

    fun clearSearchQuery() {
        _searchQuery.value = ""
    }

    fun setDrawerTab(tab: DrawerTab) {
        _drawerTab.value = tab
    }

    fun unlockVault() {
        _isVaultUnlocked.value = true
        _drawerTab.value = DrawerTab.HIDDEN
    }

    fun lockVault() {
        _isVaultUnlocked.value = false
        _drawerTab.value = DrawerTab.ACTIVE
    }

    fun hideApp(packageName: String) {
        val current = _hiddenApps.value + packageName
        _hiddenApps.value = current
        repository.saveHiddenApps(current)
        // If app is currently in directory, remove it so it's not visible
        if (packageName in _appDirectory.value.packageNames) {
            removeAppFromDirectory(packageName)
        }
        // If app is currently bookmarked, remove it immediately
        if (packageName in _bookmarkedPackageNames.value) {
            val updatedBookmarks = _bookmarkedPackageNames.value - packageName
            _bookmarkedPackageNames.value = updatedBookmarks
            repository.saveBookmarkedApps(updatedBookmarks)
        }
    }

    fun unhideApp(packageName: String) {
        val current = _hiddenApps.value - packageName
        _hiddenApps.value = current
        repository.saveHiddenApps(current)
    }

    fun toggleAppHidden(packageName: String) {
        if (packageName in _hiddenApps.value) {
            unhideApp(packageName)
        } else {
            hideApp(packageName)
        }
    }

    fun lockApp(packageName: String) {
        val current = _lockedApps.value + packageName
        _lockedApps.value = current
        repository.saveLockedApps(current)
    }

    fun unlockApp(packageName: String) {
        val current = _lockedApps.value - packageName
        _lockedApps.value = current
        repository.saveLockedApps(current)
    }

    fun openAppDrawer() {
        _isCommDrawerOpen.value = false
        _isFolderPopoverOpen.value = false
        _isAppDrawerOpen.value = true
    }

    fun closeAppDrawer() {
        _isAppDrawerOpen.value = false
        _searchQuery.value = ""
        // Re-lock vault on drawer close for maximum privacy
        _isVaultUnlocked.value = false
        _drawerTab.value = DrawerTab.ACTIVE
    }

    fun openFolderPopover() {
        _isCommDrawerOpen.value = false
        _isAppDrawerOpen.value = false
        _isFolderPopoverOpen.value = true
    }

    fun closeFolderPopover() {
        _isFolderPopoverOpen.value = false
    }

    fun toggleAppInDirectory(packageName: String) {
        val current = _appDirectory.value
        if (packageName in current.packageNames) {
            val updatedList = current.packageNames - packageName
            val updated = current.copy(packageNames = updatedList)
            _appDirectory.value = updated
            repository.saveAppDirectory(updated)
        } else {
            // Cannot add if app is hidden or if directory already has 4 apps
            if (packageName in _hiddenApps.value) return
            if (current.packageNames.size >= 4) return
            val updatedList = current.packageNames + packageName
            val updated = current.copy(packageNames = updatedList)
            _appDirectory.value = updated
            repository.saveAppDirectory(updated)
        }
    }

    fun addAppToDirectory(packageName: String) {
        // Cannot add if app is hidden or directory already has 4 apps
        if (packageName in _hiddenApps.value) return
        val current = _appDirectory.value
        if (current.packageNames.size >= 4) return
        if (packageName !in current.packageNames) {
            val updated = current.copy(packageNames = current.packageNames + packageName)
            _appDirectory.value = updated
            repository.saveAppDirectory(updated)
        }
    }

    fun removeAppFromDirectory(packageName: String) {
        val current = _appDirectory.value
        val updated = current.copy(packageNames = current.packageNames.filter { it != packageName })
        _appDirectory.value = updated
        repository.saveAppDirectory(updated)
    }

    fun clearDirectory() {
        val current = _appDirectory.value
        val updated = current.copy(packageNames = emptyList())
        _appDirectory.value = updated
        repository.saveAppDirectory(updated)
    }

    fun renameDirectory(newName: String) {
        val current = _appDirectory.value
        val updated = current.copy(name = newName)
        _appDirectory.value = updated
        repository.saveAppDirectory(updated)
    }

    fun openCommDrawer() {
        _isAppDrawerOpen.value = false
        _isFolderPopoverOpen.value = false
        _isCommDrawerOpen.value = true
    }

    fun closeCommDrawer() {
        _isCommDrawerOpen.value = false
    }

    fun setAtmospherePreset(preset: AtmospherePreset) {
        repository.saveWallpaperPreset(preset)
        _wallpaperConfig.value = WallpaperConfig(preset = preset, customImageUri = null)
    }

    fun setCustomWallpaper(uri: String) {
        repository.saveCustomWallpaperUri(uri)
        _wallpaperConfig.value = WallpaperConfig(
            preset = _wallpaperConfig.value.preset,
            customImageUri = uri
        )
    }

    fun openQuickRecents() {
        _isAppDrawerOpen.value = false
        _isFolderPopoverOpen.value = false
        _isCommDrawerOpen.value = false
        _quickSearchQuery.value = ""
        _isQuickRecentsOpen.value = true
    }

    fun closeQuickRecents() {
        _isQuickRecentsOpen.value = false
        _quickSearchQuery.value = ""
    }

    fun onQuickSearchQueryChange(query: String) {
        _quickSearchQuery.value = query
    }

    fun clearQuickSearchQuery() {
        _quickSearchQuery.value = ""
    }

    fun toggleBookmarkApp(packageName: String) {
        if (packageName in _hiddenApps.value) return // Strictly prohibited for hidden apps in vault
        val current = _bookmarkedPackageNames.value
        val updated = if (packageName in current) {
            current - packageName
        } else {
            if (current.size >= 10) return
            current + packageName
        }
        _bookmarkedPackageNames.value = updated
        repository.saveBookmarkedApps(updated)
    }

    fun launchApp(packageName: String): Boolean {
        if (packageName in _lockedApps.value) {
            LockScreenService.markTempUnlocked(packageName)
        }
        repository.recordRecentApp(packageName)
        val recents = repository.getRecentApps().filter { it !in _hiddenApps.value }.take(20)
        _recentPackageNames.value = recents
        return repository.launchApp(packageName)
    }

    fun openDialer(phoneNumber: String? = null) {
        repository.openDialer(phoneNumber)
    }

    fun openMessages(phoneNumber: String? = null) {
        repository.openMessages(phoneNumber)
    }

    fun openContacts() {
        repository.openContacts()
    }

    fun openClock() {
        repository.openClock()
    }

    fun openCalendar() {
        repository.openCalendar()
    }

    fun openAppInfo(packageName: String) {
        repository.openAppInfo(packageName)
    }

    fun onLauncherResumed() {
        viewModelScope.launch {
            // First check if an app was opened outside (or query the latest foreground app)
            val fgPkg = repository.getMostRecentForegroundPackage()
            if (!fgPkg.isNullOrEmpty() && fgPkg !in _hiddenApps.value) {
                repository.recordRecentApp(fgPkg)
            }

            val storedRecents = repository.getRecentApps().filter { it !in _hiddenApps.value }
            if (storedRecents.isNotEmpty()) {
                _recentPackageNames.value = storedRecents
                showRecentBubble()
            }
        }
    }

    fun showRecentBubble() {
        bubbleDismissJob?.cancel()
        _isRecentBubbleVisible.value = true
        bubbleDismissJob = viewModelScope.launch {
            delay(8000L) // Auto-fade after 8 seconds
            _isRecentBubbleVisible.value = false
        }
    }

    fun dismissRecentBubble() {
        bubbleDismissJob?.cancel()
        _isRecentBubbleVisible.value = false
    }

    fun openRecentClusterDialog() {
        bubbleDismissJob?.cancel()
        _isRecentClusterDialogOpen.value = true
    }

    fun closeRecentClusterDialog() {
        _isRecentClusterDialogOpen.value = false
    }

    fun removeRecentApp(packageName: String) {
        val updated = repository.removeRecentApp(packageName).filter { it !in _hiddenApps.value }
        _recentPackageNames.value = updated

        if (updated.isEmpty()) {
            _isRecentBubbleVisible.value = false
            _isRecentClusterDialogOpen.value = false
        }
    }

    fun clearAllRecentApps() {
        val current = _recentPackageNames.value
        repository.clearAllRecentApps(current)
        _recentPackageNames.value = emptyList()
        _isRecentBubbleVisible.value = false
        _isRecentClusterDialogOpen.value = false
    }
}
