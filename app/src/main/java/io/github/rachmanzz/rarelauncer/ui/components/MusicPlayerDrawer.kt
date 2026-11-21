package io.github.rachmanzz.rarelauncer.ui.components

import android.annotation.SuppressLint
import android.content.Context
import android.view.ViewGroup
import android.webkit.CookieManager
import android.webkit.JavascriptInterface
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
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
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
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
import androidx.compose.ui.viewinterop.AndroidView
import coil.compose.AsyncImage
import io.github.rachmanzz.rarelauncer.data.model.MusicPlayerState
import io.github.rachmanzz.rarelauncer.data.model.MusicPlaylist
import io.github.rachmanzz.rarelauncer.data.model.MusicTrack
import io.github.rachmanzz.rarelauncer.data.model.PlaybackOrderMode
import io.github.rachmanzz.rarelauncer.data.model.YouTubeWebController
import io.github.rachmanzz.rarelauncer.ui.theme.AetherGlassBorder
import io.github.rachmanzz.rarelauncer.ui.theme.AetherGlassBorderSubtle
import io.github.rachmanzz.rarelauncer.ui.theme.AetherOnSurface
import io.github.rachmanzz.rarelauncer.ui.theme.AetherOnSurfaceVariant
import io.github.rachmanzz.rarelauncer.ui.theme.AetherPrimary

private class BackgroundAudioWebView(context: Context) : WebView(context) {
    override fun onWindowVisibilityChanged(visibility: Int) {
        // Keep YouTube audio playing when drawer hides or launcher goes to background
        super.onWindowVisibilityChanged(VISIBLE)
    }
}

private const val YOUTUBE_BRIDGE_JS = """
(function() {
  if (window.__rareBridgeInstalled) return;
  window.__rareBridgeInstalled = true;
  window.__rareUserPaused = false;
  window.__rareEndedNotified = false;
  window.__rareHasPlayedMain = false;
  var lastState = -1;
  var lastDuration = 0;
  var lastReportedTimeMs = 0;

  function getUrlVideoId() {
    try {
      return new URLSearchParams(window.location.search).get('v') || '';
    } catch (e) {
      return '';
    }
  }

  if (!window.__rareExpectedVideoId) {
    window.__rareExpectedVideoId = getUrlVideoId();
  }

  // Prevent YouTube from auto-pausing when launcher goes to background or screen turns off
  try {
    Object.defineProperty(document, 'hidden', { get: function() { return false; }, configurable: true });
    Object.defineProperty(document, 'webkitHidden', { get: function() { return false; }, configurable: true });
    Object.defineProperty(document, 'visibilityState', { get: function() { return 'visible'; }, configurable: true });
    window.addEventListener('visibilitychange', function(e) { e.stopImmediatePropagation(); }, true);
    document.addEventListener('visibilitychange', function(e) { e.stopImmediatePropagation(); }, true);
  } catch (e) {}

  // Inject clean full-bleed player CSS to hide YouTube page chrome
  var style = document.createElement('style');
  style.id = 'rare-yt-style';
  style.textContent =
    'html, body { margin:0 !important; padding:0 !important; background:#000 !important; overflow:hidden !important; }' +
    'ytm-mobile-topbar-renderer, .mobile-topbar-header, ytm-pivot-bar-renderer, ' +
    'ytm-watch-below-the-player, .watch-below-the-player, ytm-promoted-sparkles-web-renderer, ' +
    '.ytm-autonav-bar, ytm-single-column-watch-next-results-renderer, .open-app-button, ' +
    'ytm-mealbar-promo-renderer, .ytm-upsell-dialog-renderer { display: none !important; }' +
    '#player-container-id, .player-container, #player, .html5-video-player { ' +
    '  position: fixed !important; top: 0 !important; left: 0 !important; ' +
    '  width: 100vw !important; height: 100vh !important; max-height: 100vh !important; ' +
    '  z-index: 99999 !important; background: #000 !important; ' +
    '}' +
    'video.html5-main-video { width: 100vw !important; height: 100vh !important; object-fit: contain !important; }';
  (document.head || document.documentElement).appendChild(style);

  function isPlayingAd() {
    return !!document.querySelector('.ad-showing, .ad-interrupting');
  }

  function isPlayerEnded() {
    if (document.querySelector('.html5-video-player.ended-mode')) return true;
    if (document.querySelector('.ytm-autonav-endscreen-countdown-overlay, .ytp-autonav-endscreen-countdown-overlay, .ytp-autonav-endscreen-upnext-container')) return true;
    var replayBtn = document.querySelector('button[aria-label="Replay"], button[aria-label="Putar ulang"], .ytp-play-button[title="Replay"]');
    if (replayBtn && replayBtn.offsetParent !== null) return true;
    return false;
  }

  function notifyState(st) {
    if (st !== lastState) {
      lastState = st;
      if (window.AndroidBridge && window.AndroidBridge.onStateChange) {
        window.AndroidBridge.onStateChange(st, window.__rareExpectedVideoId || '');
      }
    }
  }

  function notifyEnded() {
    if (window.__rareEndedNotified) return;
    window.__rareEndedNotified = true;
    var cancelBtn = document.querySelector('.ytp-autonav-endscreen-upnext-cancel-button');
    if (cancelBtn) {
      try { cancelBtn.click(); } catch (e) {}
    }
    document.querySelectorAll('video').forEach(function(v) {
      try { v.pause(); } catch (e) {}
    });
    notifyState(0);
  }

  function checkVideoCompleted(v) {
    if (window.__rareEndedNotified || isPlayingAd()) return false;
    var curVid = getUrlVideoId();
    if (window.__rareExpectedVideoId && curVid && curVid !== window.__rareExpectedVideoId) {
      notifyEnded();
      return true;
    }
    if (!window.__rareHasPlayedMain) return false;
    if (v && v.ended) {
      notifyEnded();
      return true;
    }
    if (isPlayerEnded()) {
      notifyEnded();
      return true;
    }
    if (v && isFinite(v.duration) && v.duration > 3 && v.currentTime >= v.duration - 0.35) {
      notifyEnded();
      return true;
    }
    return false;
  }

  function hookVideo(v) {
    if (!v || v.__rareHooked) return;
    v.__rareHooked = true;

    v.addEventListener('playing', function() {
      if (window.__rareEndedNotified) return;
      if (!isPlayingAd() && isFinite(v.duration) && v.duration > 3) {
        window.__rareHasPlayedMain = true;
      }
      notifyState(1);
      if (!isPlayingAd() && isFinite(v.duration) && v.duration > 0 && window.AndroidBridge && window.AndroidBridge.onDuration) {
        lastDuration = v.duration;
        window.AndroidBridge.onDuration(v.duration);
      }
    });

    v.addEventListener('timeupdate', function() {
      if (window.__rareEndedNotified) return;
      if (!isPlayingAd() && isFinite(v.duration) && v.duration > 3) {
        if (v.currentTime > 0.4) {
          window.__rareHasPlayedMain = true;
        }
        var now = Date.now();
        if (now - lastReportedTimeMs >= 400) {
          lastReportedTimeMs = now;
          if (Math.abs(v.duration - lastDuration) > 1 && window.AndroidBridge && window.AndroidBridge.onDuration) {
            lastDuration = v.duration;
            window.AndroidBridge.onDuration(v.duration);
          }
          if (window.AndroidBridge && window.AndroidBridge.onCurrentTime) {
            window.AndroidBridge.onCurrentTime(v.currentTime || 0);
          }
        }
      }
      checkVideoCompleted(v);
    });

    v.addEventListener('pause', function() {
      if (window.__rareEndedNotified) return;
      if (checkVideoCompleted(v)) return;
      if (!isPlayingAd() && window.__rareHasPlayedMain && isFinite(v.duration) && v.duration > 3 && v.currentTime >= v.duration - 0.8) {
        notifyEnded();
        return;
      }
      if (!v.ended) notifyState(2);
    });

    v.addEventListener('waiting', function() {
      if (!window.__rareEndedNotified) notifyState(3);
    });

    v.addEventListener('ended', function() {
      if (!isPlayingAd()) {
        notifyEnded();
      }
    });

    v.addEventListener('durationchange', function() {
      if (!isPlayingAd() && isFinite(v.duration) && v.duration > 0 && window.AndroidBridge && window.AndroidBridge.onDuration) {
        lastDuration = v.duration;
        window.AndroidBridge.onDuration(v.duration);
      }
    });
  }

  window.__rarePlay = function() {
    window.__rareUserPaused = false;
    var v = document.querySelector('video');
    if (v) {
      v.muted = false;
      v.volume = 1.0;
      var p = v.play();
      if (p && p.catch) p.catch(function(){});
    }
  };

  window.__rareReplay = function() {
    window.__rareUserPaused = false;
    window.__rareEndedNotified = false;
    window.__rareHasPlayedMain = false;
    lastState = -1;
    var v = document.querySelector('video');
    if (v) {
      v.currentTime = 0;
      v.muted = false;
      v.volume = 1.0;
      var p = v.play();
      if (p && p.catch) p.catch(function(){});
    }
  };

  window.__rarePause = function() {
    window.__rareUserPaused = true;
    document.querySelectorAll('video').forEach(function(v) { v.pause(); });
  };

  window.__rareSeek = function(sec) {
    var v = document.querySelector('video');
    if (v) v.currentTime = sec;
  };

  setInterval(function() {
    if (window.__rareEndedNotified) return;

    var curVid = getUrlVideoId();
    if (window.__rareExpectedVideoId && curVid && curVid !== window.__rareExpectedVideoId) {
      notifyEnded();
      return;
    }

    // Auto-skip ads if skip button is visible
    var skipBtn = document.querySelector('.ytp-skip-ad-button, .ytp-ad-skip-button, .ytp-ad-skip-button-modern');
    if (skipBtn) skipBtn.click();

    // Auto-dismiss tap-to-unmute overlay
    var unmuteBtn = document.querySelector('.ytp-unmute, button[aria-label*="Unmute"], button[aria-label*="Bunyikan"]');
    if (unmuteBtn && unmuteBtn.offsetParent !== null) unmuteBtn.click();

    var v = document.querySelector('video');
    if (!v) {
      if (!window.__rareUserPaused) {
        var playOverlay = document.querySelector('.ytp-large-play-button, button[aria-label="Play"], button[aria-label="Putar"]');
        if (playOverlay) playOverlay.click();
      }
      return;
    }

    hookVideo(v);

    if (checkVideoCompleted(v)) return;
    if (!isPlayingAd() && window.__rareHasPlayedMain && isFinite(v.duration) && v.duration > 3 && v.currentTime >= v.duration - 0.6 && v.paused) {
      notifyEnded();
      return;
    }

    var isNearEnd = !isPlayingAd() && isFinite(v.duration) && v.duration > 3 && v.currentTime >= v.duration - 1.0;
    if (!window.__rareUserPaused && !window.__rareEndedNotified && !isPlayerEnded() && !isNearEnd) {
      if (v.muted) {
        v.muted = false;
        v.volume = 1.0;
      }
      if (v.paused && !v.ended && v.readyState >= 2) {
        var p = v.play();
        if (p && p.catch) p.catch(function(){});
      }
    }

    if (!v.paused && !v.ended && !isPlayingAd()) {
      if (isFinite(v.duration) && v.duration > 3 && v.currentTime > 0.4) {
        window.__rareHasPlayedMain = true;
      }
      notifyState(1);
      if (isFinite(v.duration) && v.duration > 0 && Math.abs(v.duration - lastDuration) > 1) {
        lastDuration = v.duration;
        if (window.AndroidBridge && window.AndroidBridge.onDuration) {
          window.AndroidBridge.onDuration(v.duration);
        }
      }
      if (window.AndroidBridge && window.AndroidBridge.onCurrentTime) {
        window.AndroidBridge.onCurrentTime(v.currentTime || 0);
      }
    }
  }, 500);
})();
"""

private enum class MusicDrawerTab {
    EXPLORE,
    PLAYLISTS,
}

@SuppressLint("SetJavaScriptEnabled")
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
    onYouTubePlayerReady: (YouTubeWebController) -> Unit,
    onYouTubeStateChange: (Int) -> Unit,
    onYouTubeCurrentSecond: (Float) -> Unit,
    onYouTubeDuration: (Float) -> Unit,
    onYouTubeError: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val latestOnYouTubePlayerReady by rememberUpdatedState(onYouTubePlayerReady)
    val latestOnYouTubeStateChange by rememberUpdatedState(onYouTubeStateChange)
    val latestOnYouTubeCurrentSecond by rememberUpdatedState(onYouTubeCurrentSecond)
    val latestOnYouTubeDuration by rememberUpdatedState(onYouTubeDuration)
    val latestOnYouTubeError by rememberUpdatedState(onYouTubeError)

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

    val hasActiveYouTubeVideo = playerState.currentTrack != null && playerState.currentTrack.streamUrl.isBlank()

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

                // YouTube Video & Audio Player Viewport
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(if (hasActiveYouTubeVideo) 195.dp else 1.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(Color.Black)
                        .border(
                            1.dp,
                            if (hasActiveYouTubeVideo) AetherGlassBorder else Color.Transparent,
                            RoundedCornerShape(16.dp),
                        )
                        .alpha(if (hasActiveYouTubeVideo) 1f else 0.001f),
                ) {
                    AndroidView(
                        factory = { ctx ->
                            BackgroundAudioWebView(ctx).apply {
                                layoutParams = ViewGroup.LayoutParams(
                                    ViewGroup.LayoutParams.MATCH_PARENT,
                                    ViewGroup.LayoutParams.MATCH_PARENT,
                                )
                                setBackgroundColor(android.graphics.Color.BLACK)
                                settings.apply {
                                    javaScriptEnabled = true
                                    mediaPlaybackRequiresUserGesture = false
                                    domStorageEnabled = true
                                    databaseEnabled = true
                                    useWideViewPort = true
                                    loadWithOverviewMode = true
                                    cacheMode = WebSettings.LOAD_DEFAULT
                                    mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
                                    userAgentString =
                                        "Mozilla/5.0 (Linux; Android 14; Pixel 8) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/128.0.0.0 Mobile Safari/537.36"
                                }
                                val webView = this
                                CookieManager.getInstance().apply {
                                    setAcceptCookie(true)
                                    setAcceptThirdPartyCookies(webView, true)
                                }

                                var currentExpectedVideoId: String? = null

                                val controller = object : YouTubeWebController {
                                    override fun loadVideo(videoId: String) {
                                        post {
                                            val isSameVideo = currentExpectedVideoId == videoId &&
                                                url?.contains("v=$videoId") == true
                                            currentExpectedVideoId = videoId
                                            if (isSameVideo) {
                                                android.util.Log.d("RareMusic", "WebView replaying same videoId=$videoId")
                                                evaluateJavascript("window.__rareReplay && window.__rareReplay();", null)
                                            } else {
                                                android.util.Log.d("RareMusic", "WebView loading m.youtube.com/watch?v=$videoId")
                                                evaluateJavascript("window.__rareEndedNotified = true;", null)
                                                stopLoading()
                                                loadUrl("https://m.youtube.com/watch?v=$videoId")
                                            }
                                        }
                                    }

                                    override fun play() {
                                        post { evaluateJavascript("window.__rarePlay && window.__rarePlay();", null) }
                                    }

                                    override fun pause() {
                                        post { evaluateJavascript("window.__rarePause && window.__rarePause();", null) }
                                    }

                                    override fun seekTo(seconds: Float) {
                                        post { evaluateJavascript("window.__rareSeek && window.__rareSeek($seconds);", null) }
                                    }
                                }

                                val bridge = object {
                                    @JavascriptInterface
                                    fun onStateChange(state: Int, videoId: String) {
                                        post {
                                            val expected = currentExpectedVideoId
                                            if (videoId.isBlank() || expected == null || videoId == expected) {
                                                latestOnYouTubeStateChange(state)
                                            }
                                        }
                                    }

                                    @JavascriptInterface
                                    fun onCurrentTime(time: Float) {
                                        post { latestOnYouTubeCurrentSecond(time) }
                                    }

                                    @JavascriptInterface
                                    fun onDuration(duration: Float) {
                                        post { latestOnYouTubeDuration(duration) }
                                    }

                                    @JavascriptInterface
                                    fun onError(code: Int) {
                                        post { latestOnYouTubeError(code) }
                                    }
                                }

                                addJavascriptInterface(bridge, "AndroidBridge")
                                webChromeClient = WebChromeClient()
                                webViewClient = object : WebViewClient() {
                                    override fun shouldOverrideUrlLoading(
                                        view: WebView?,
                                        request: WebResourceRequest?,
                                    ): Boolean {
                                        val uri = request?.url ?: return false
                                        val scheme = uri.scheme ?: return false
                                        // Block intent:// or vnd.youtube:// redirects so playback stays inside launcher
                                        if (scheme != "http" && scheme != "https") return true
                                        // Prevent YouTube's own autoplay from navigating away from our playlist track
                                        val targetVid = uri.getQueryParameter("v")
                                        val expected = currentExpectedVideoId
                                        if (uri.host?.contains("youtube.com") == true &&
                                            !targetVid.isNullOrBlank() &&
                                            expected != null &&
                                            targetVid != expected
                                        ) {
                                            post { latestOnYouTubeStateChange(0) }
                                            return true
                                        }
                                        return false
                                    }

                                    override fun onPageFinished(view: WebView?, url: String?) {
                                        super.onPageFinished(view, url)
                                        if (url != null && url.contains("youtube.com")) {
                                            val safeVid = currentExpectedVideoId.orEmpty()
                                                .replace("\\", "")
                                                .replace("'", "")
                                            view?.evaluateJavascript(
                                                "window.__rareExpectedVideoId = '$safeVid';",
                                                null,
                                            )
                                            view?.evaluateJavascript(YOUTUBE_BRIDGE_JS, null)
                                        }
                                    }
                                }
                                post { latestOnYouTubePlayerReady(controller) }
                            }
                        },
                        modifier = Modifier.fillMaxSize(),
                    )
                }

                if (hasActiveYouTubeVideo) {
                    Spacer(modifier = Modifier.height(10.dp))
                }

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
