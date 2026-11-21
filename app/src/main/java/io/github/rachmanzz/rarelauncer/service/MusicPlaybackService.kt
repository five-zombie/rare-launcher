package io.github.rachmanzz.rarelauncer.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.ServiceInfo
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.drawable.Icon
import android.media.AudioManager
import android.media.MediaMetadata
import android.media.session.MediaSession
import android.media.session.PlaybackState
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import androidx.core.content.ContextCompat
import io.github.rachmanzz.rarelauncer.MainActivity
import java.net.URL
import java.util.concurrent.Executors

/**
 * Foreground Media Playback service (foregroundServiceType="mediaPlayback")
 * that keeps audio streaming alive off-screen and when screen is off,
 * while exposing native MediaSession controls in Notification Shade, Lock Screen,
 * and Bluetooth headsets.
 */
class MusicPlaybackService : Service() {

    private var mediaSession: MediaSession? = null
    private var wakeLock: PowerManager.WakeLock? = null
    private var isForegroundStarted = false
    private var currentTitle = "Cyber Stream"
    private var currentArtist = "Online Radio"
    private var currentThumbnailUrl = ""
    private var currentArtwork: Bitmap? = null
    private var currentIsPlaying = false
    private val artworkExecutor = Executors.newSingleThreadExecutor()

    private val noisyReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action == AudioManager.ACTION_AUDIO_BECOMING_NOISY && currentIsPlaying) {
                callbacks?.onPause()
            }
        }
    }
    private var isNoisyRegistered = false

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()

        val powerManager = getSystemService(PowerManager::class.java)
        wakeLock = powerManager?.newWakeLock(
            PowerManager.PARTIAL_WAKE_LOCK,
            "RareLauncer::MusicPlaybackWakeLock",
        )?.apply {
            setReferenceCounted(false)
        }

        mediaSession = MediaSession(this, "RareLauncerMusicSession").apply {
            setCallback(object : MediaSession.Callback() {
                override fun onPlay() {
                    callbacks?.onPlay()
                }

                override fun onPause() {
                    callbacks?.onPause()
                }

                override fun onSkipToNext() {
                    callbacks?.onNext()
                }

                override fun onSkipToPrevious() {
                    callbacks?.onPrevious()
                }

                override fun onStop() {
                    callbacks?.onStop()
                    stopPlaybackService()
                }
            })
            isActive = true
        }

        val filter = IntentFilter(AudioManager.ACTION_AUDIO_BECOMING_NOISY)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(noisyReceiver, filter, RECEIVER_NOT_EXPORTED)
        } else {
            @Suppress("DEPRECATION")
            registerReceiver(noisyReceiver, filter)
        }
        isNoisyRegistered = true
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_SYNC -> {
                val title = intent.getStringExtra(EXTRA_TITLE).orEmpty().ifBlank { "Cyber Stream" }
                val artist = intent.getStringExtra(EXTRA_ARTIST).orEmpty().ifBlank { "Online Radio" }
                val thumbUrl = intent.getStringExtra(EXTRA_THUMBNAIL).orEmpty()
                val isPlaying = intent.getBooleanExtra(EXTRA_IS_PLAYING, false)
                val durationMs = intent.getLongExtra(EXTRA_DURATION_MS, 0L)

                currentTitle = title
                currentArtist = artist
                currentIsPlaying = isPlaying

                if (thumbUrl != currentThumbnailUrl) {
                    currentThumbnailUrl = thumbUrl
                    currentArtwork = null
                    if (thumbUrl.isNotBlank()) {
                        loadArtworkAsync(thumbUrl)
                    }
                }

                updateWakeLock(isPlaying)
                updateMediaSession(title, artist, isPlaying, durationMs, currentArtwork)
                showOrUpdateForegroundNotification()
            }

            ACTION_TOGGLE_PLAY_PAUSE -> {
                if (currentIsPlaying) {
                    callbacks?.onPause()
                } else {
                    callbacks?.onPlay()
                }
            }

            ACTION_NEXT -> {
                callbacks?.onNext()
            }

            ACTION_PREVIOUS -> {
                callbacks?.onPrevious()
            }

            ACTION_STOP -> {
                callbacks?.onStop()
                stopPlaybackService()
            }
        }
        return START_NOT_STICKY
    }

    private fun loadArtworkAsync(url: String) {
        artworkExecutor.execute {
            val bitmap = runCatching {
                URL(url).openStream().use { BitmapFactory.decodeStream(it) }
            }.getOrNull()
            if (bitmap != null && url == currentThumbnailUrl) {
                currentArtwork = bitmap
                updateMediaSession(currentTitle, currentArtist, currentIsPlaying, 0L, bitmap)
                if (isForegroundStarted) {
                    val manager = getSystemService(NotificationManager::class.java)
                    manager?.notify(NOTIFICATION_ID, buildNotification())
                }
            }
        }
    }

    private fun updateWakeLock(isPlaying: Boolean) {
        runCatching {
            if (isPlaying) {
                if (wakeLock?.isHeld == false) {
                    // Hold wake lock up to 6 hours while playing music with screen off
                    wakeLock?.acquire(6 * 60 * 60 * 1000L)
                }
            } else {
                if (wakeLock?.isHeld == true) {
                    wakeLock?.release()
                }
            }
        }
    }

    private fun updateMediaSession(
        title: String,
        artist: String,
        isPlaying: Boolean,
        durationMs: Long,
        artwork: Bitmap?,
    ) {
        val session = mediaSession ?: return
        val metadataBuilder = MediaMetadata.Builder()
            .putString(MediaMetadata.METADATA_KEY_TITLE, title)
            .putString(MediaMetadata.METADATA_KEY_ARTIST, artist)
        if (durationMs > 0L) {
            metadataBuilder.putLong(MediaMetadata.METADATA_KEY_DURATION, durationMs)
        }
        if (artwork != null) {
            metadataBuilder.putBitmap(MediaMetadata.METADATA_KEY_ALBUM_ART, artwork)
        }
        session.setMetadata(metadataBuilder.build())

        val actions = PlaybackState.ACTION_PLAY or
            PlaybackState.ACTION_PAUSE or
            PlaybackState.ACTION_PLAY_PAUSE or
            PlaybackState.ACTION_SKIP_TO_NEXT or
            PlaybackState.ACTION_SKIP_TO_PREVIOUS or
            PlaybackState.ACTION_STOP

        val state = PlaybackState.Builder()
            .setActions(actions)
            .setState(
                if (isPlaying) PlaybackState.STATE_PLAYING else PlaybackState.STATE_PAUSED,
                PlaybackState.PLAYBACK_POSITION_UNKNOWN,
                if (isPlaying) 1f else 0f,
            )
            .build()
        session.setPlaybackState(state)
    }

    private fun showOrUpdateForegroundNotification() {
        val notification = buildNotification()
        if (!isForegroundStarted) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                startForeground(
                    NOTIFICATION_ID,
                    notification,
                    ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK,
                )
            } else {
                startForeground(NOTIFICATION_ID, notification)
            }
            isForegroundStarted = true
        } else {
            val manager = getSystemService(NotificationManager::class.java)
            manager?.notify(NOTIFICATION_ID, notification)
        }
    }

    private fun buildNotification(): Notification {
        val openAppIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra(MainActivity.EXTRA_OPEN_MUSIC_DRAWER, true)
        }
        val contentPendingIntent = PendingIntent.getActivity(
            this,
            100,
            openAppIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )

        val prevAction = Notification.Action.Builder(
            Icon.createWithResource(this, android.R.drawable.ic_media_previous),
            "Previous",
            servicePendingIntent(ACTION_PREVIOUS, 101),
        ).build()

        val playPauseAction = Notification.Action.Builder(
            Icon.createWithResource(
                this,
                if (currentIsPlaying) android.R.drawable.ic_media_pause else android.R.drawable.ic_media_play,
            ),
            if (currentIsPlaying) "Pause" else "Play",
            servicePendingIntent(ACTION_TOGGLE_PLAY_PAUSE, 102),
        ).build()

        val nextAction = Notification.Action.Builder(
            Icon.createWithResource(this, android.R.drawable.ic_media_next),
            "Next",
            servicePendingIntent(ACTION_NEXT, 103),
        ).build()

        val stopAction = Notification.Action.Builder(
            Icon.createWithResource(this, android.R.drawable.ic_menu_close_clear_cancel),
            "Stop",
            servicePendingIntent(ACTION_STOP, 104),
        ).build()

        val style = Notification.MediaStyle()
            .setMediaSession(mediaSession?.sessionToken)
            .setShowActionsInCompactView(0, 1, 2)

        return Notification.Builder(this, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_media_play)
            .setContentTitle(currentTitle)
            .setContentText(currentArtist)
            .setSubText("Cyber Stream")
            .setLargeIcon(currentArtwork)
            .setContentIntent(contentPendingIntent)
            .setOngoing(currentIsPlaying)
            .setOnlyAlertOnce(true)
            .setVisibility(Notification.VISIBILITY_PUBLIC)
            .addAction(prevAction)
            .addAction(playPauseAction)
            .addAction(nextAction)
            .addAction(stopAction)
            .setStyle(style)
            .build()
    }

    private fun servicePendingIntent(action: String, requestCode: Int): PendingIntent {
        val intent = Intent(this, MusicPlaybackService::class.java).setAction(action)
        return PendingIntent.getService(
            this,
            requestCode,
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
    }

    private fun createNotificationChannel() {
        val manager = getSystemService(NotificationManager::class.java) ?: return
        val channel = NotificationChannel(
            CHANNEL_ID,
            "Cyber Stream Music",
            NotificationManager.IMPORTANCE_LOW,
        ).apply {
            description = "Background music playback controls"
            setShowBadge(false)
            lockscreenVisibility = Notification.VISIBILITY_PUBLIC
        }
        manager.createNotificationChannel(channel)
    }

    private fun stopPlaybackService() {
        updateWakeLock(false)
        isForegroundStarted = false
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        updateWakeLock(false)
        if (isNoisyRegistered) {
            runCatching { unregisterReceiver(noisyReceiver) }
            isNoisyRegistered = false
        }
        mediaSession?.run {
            isActive = false
            release()
        }
        mediaSession = null
        artworkExecutor.shutdownNow()
        isForegroundStarted = false
        super.onDestroy()
    }

    interface PlaybackCallbacks {
        fun onPlay()
        fun onPause()
        fun onNext()
        fun onPrevious()
        fun onStop()
    }

    companion object {
        private const val CHANNEL_ID = "rare_music_playback"
        private const val NOTIFICATION_ID = 4202

        private const val ACTION_SYNC = "io.github.rachmanzz.rarelauncer.music.SYNC"
        private const val ACTION_TOGGLE_PLAY_PAUSE = "io.github.rachmanzz.rarelauncer.music.TOGGLE"
        private const val ACTION_NEXT = "io.github.rachmanzz.rarelauncer.music.NEXT"
        private const val ACTION_PREVIOUS = "io.github.rachmanzz.rarelauncer.music.PREV"
        private const val ACTION_STOP = "io.github.rachmanzz.rarelauncer.music.STOP"

        private const val EXTRA_TITLE = "extra_title"
        private const val EXTRA_ARTIST = "extra_artist"
        private const val EXTRA_THUMBNAIL = "extra_thumbnail"
        private const val EXTRA_IS_PLAYING = "extra_is_playing"
        private const val EXTRA_DURATION_MS = "extra_duration_ms"

        @Volatile
        private var callbacks: PlaybackCallbacks? = null

        fun registerCallbacks(cb: PlaybackCallbacks?) {
            callbacks = cb
        }

        fun syncState(
            context: Context,
            title: String,
            artist: String,
            thumbnailUrl: String,
            isPlaying: Boolean,
            durationMs: Long = 0L,
        ) {
            val intent = Intent(context, MusicPlaybackService::class.java).apply {
                action = ACTION_SYNC
                putExtra(EXTRA_TITLE, title)
                putExtra(EXTRA_ARTIST, artist)
                putExtra(EXTRA_THUMBNAIL, thumbnailUrl)
                putExtra(EXTRA_IS_PLAYING, isPlaying)
                putExtra(EXTRA_DURATION_MS, durationMs)
            }
            runCatching {
                ContextCompat.startForegroundService(context, intent)
            }
        }

        fun stop(context: Context) {
            runCatching {
                context.stopService(Intent(context, MusicPlaybackService::class.java))
            }
        }
    }
}
