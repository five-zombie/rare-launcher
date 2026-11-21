package io.github.rachmanzz.rarelauncer.data.model

data class MusicTrack(
    val id: String,
    val title: String,
    val artist: String,
    val durationText: String = "",
    val thumbnailUrl: String = "",
    val streamUrl: String = "",
    val isLiked: Boolean = false,
)

data class MusicPlaylist(
    val id: String,
    val name: String,
    val tracks: List<MusicTrack> = emptyList(),
)

enum class PlaybackOrderMode {
    SEQUENTIAL,
    SHUFFLE,
}

data class MusicPlayerState(
    val currentTrack: MusicTrack? = null,
    val isPlaying: Boolean = false,
    val isBuffering: Boolean = false,
    val currentPositionMs: Long = 0L,
    val durationMs: Long = 0L,
    val queue: List<MusicTrack> = emptyList(),
    val playbackOrderMode: PlaybackOrderMode = PlaybackOrderMode.SEQUENTIAL,
    val error: String? = null,
)

interface YouTubeWebController {
    fun loadVideo(videoId: String)
    fun play()
    fun pause()
    fun seekTo(seconds: Float)
}
