package io.github.rachmanzz.rarelauncer.data.repository

import android.content.Context
import androidx.core.content.edit
import io.github.rachmanzz.rarelauncer.data.model.MusicPlaylist
import io.github.rachmanzz.rarelauncer.data.model.MusicTrack
import io.github.rachmanzz.rarelauncer.data.model.PlaybackOrderMode
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.util.Locale
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject

/**
 * Music repository connecting to YouTube Music / YouTube InnerTube API
 * with automatic fallback and fast direct streaming.
 */
class MusicRepository {

    /** Curated starter cyberpunk, synthwave & chill tracks for instant playback on launch */
    val curatedTrendingTracks = listOf(
        MusicTrack(
            id = "synth_chillsynth_1",
            title = "ChillSynth Radio (24/7 Lo-Fi)",
            artist = "Nightride FM",
            durationText = "LIVE",
            thumbnailUrl = "https://images.unsplash.com/photo-1518709268805-4e9042af9f23?auto=format&fit=crop&w=400&q=80",
            streamUrl = "https://stream.nightride.fm/chillsynth.mp3",
        ),
        MusicTrack(
            id = "synth_nightride_2",
            title = "Cyberpunk Synthwave Waves",
            artist = "Nightride FM",
            durationText = "LIVE",
            thumbnailUrl = "https://images.unsplash.com/photo-1508739773434-c26b3d09e071?auto=format&fit=crop&w=400&q=80",
            streamUrl = "https://stream.nightride.fm/nightride.mp3",
        ),
        MusicTrack(
            id = "synth_darksynth_3",
            title = "Data Highway Darkwave",
            artist = "Nightride FM",
            durationText = "LIVE",
            thumbnailUrl = "https://images.unsplash.com/photo-1511671782779-c97d3d27a1d4?auto=format&fit=crop&w=400&q=80",
            streamUrl = "https://stream.nightride.fm/darksynth.mp3",
        ),
        MusicTrack(
            id = "synth_spacesynth_4",
            title = "Orbital Horizon Ambient",
            artist = "Nightride FM",
            durationText = "LIVE",
            thumbnailUrl = "https://images.unsplash.com/photo-1506703719100-a0f3a48c0f86?auto=format&fit=crop&w=400&q=80",
            streamUrl = "https://stream.nightride.fm/spacesynth.mp3",
        ),
        MusicTrack(
            id = "8IsnNdTIh-k",
            title = "Risk It All",
            artist = "Bruno Mars (Cover)",
            durationText = "03:45",
            thumbnailUrl = "https://i.ytimg.com/vi/8IsnNdTIh-k/hqdefault.jpg",
            streamUrl = "",
        ),
        MusicTrack(
            id = "hT_nvWreIhg",
            title = "Counting Stars",
            artist = "OneRepublic",
            durationText = "04:18",
            thumbnailUrl = "https://i.ytimg.com/vi/hT_nvWreIhg/hqdefault.jpg",
            streamUrl = "",
        ),
    )

    suspend fun searchTracks(query: String): List<MusicTrack> = withContext(Dispatchers.IO) {
        if (query.isBlank()) return@withContext curatedTrendingTracks

        val results = mutableListOf<MusicTrack>()

        // 1. Direct Official YouTube Search (InnerTube Endpoint - 100% Real YouTube / YouTube Music)
        try {
            val payload = JSONObject().apply {
                put("context", JSONObject().apply {
                    put("client", JSONObject().apply {
                        put("clientName", "WEB")
                        put("clientVersion", "2.20240101.00.00")
                    })
                })
                put("query", query)
            }

            val url = URL("https://www.youtube.com/youtubei/v1/search")
            val conn = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "POST"
                connectTimeout = 5000
                readTimeout = 5000
                doOutput = true
                setRequestProperty("Content-Type", "application/json")
                setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) Chrome/120.0.0.0")
            }

            conn.outputStream.use { os ->
                os.write(payload.toString().toByteArray(Charsets.UTF_8))
            }

            if (conn.responseCode == 200) {
                val body = conn.inputStream.bufferedReader().use { it.readText() }
                val json = JSONObject(body)
                parseYouTubeInnerTube(json, results)
                if (results.isNotEmpty()) {
                    return@withContext results
                }
            }
        } catch (e: Exception) {
            android.util.Log.e("RareMusic", "YouTube search failed: ${e.message}")
        }

        // 2. Fallback: iTunes Music Search
        try {
            val encodedQuery = URLEncoder.encode(query, "UTF-8")
            val url = URL("https://itunes.apple.com/search?term=$encodedQuery&entity=song&limit=25")
            val conn = (url.openConnection() as HttpURLConnection).apply {
                connectTimeout = 4000
                readTimeout = 4000
                setRequestProperty("User-Agent", "Mozilla/5.0")
            }
            if (conn.responseCode == 200) {
                val body = conn.inputStream.bufferedReader().use { it.readText() }
                val json = JSONObject(body)
                val items = json.optJSONArray("results") ?: JSONArray()
                for (i in 0 until items.length()) {
                    val item = items.getJSONObject(i)
                    val previewUrl = item.optString("previewUrl", "")
                    val trackName = item.optString("trackName", "")
                    if (previewUrl.isNotBlank() && trackName.isNotBlank()) {
                        val artist = item.optString("artistName", "Unknown Artist")
                        val art = item.optString("artworkUrl100", "").replace("100x100bb", "400x400bb")
                        val trackId = item.optString("trackId", "track_$i")
                        val millis = item.optLong("trackTimeMillis", 0L)
                        val durationFormatted = if (millis > 0) {
                            val secs = millis / 1000
                            String.format(Locale.getDefault(), "%02d:%02d", secs / 60, secs % 60)
                        } else "03:30"

                        results.add(
                            MusicTrack(
                                id = trackId,
                                title = trackName,
                                artist = artist,
                                durationText = durationFormatted,
                                thumbnailUrl = art,
                                streamUrl = previewUrl,
                            )
                        )
                    }
                }
            }
        } catch (_: Exception) {}

        results
    }

    private fun parseYouTubeInnerTube(obj: Any, results: MutableList<MusicTrack>) {
        if (results.size >= 30) return
        if (obj is JSONObject) {
            if (obj.has("videoRenderer")) {
                val vr = obj.getJSONObject("videoRenderer")
                val vid = vr.optString("videoId", "")
                val titleObj = vr.optJSONObject("title")
                val titleRuns = titleObj?.optJSONArray("runs")
                val title = if (titleRuns != null && titleRuns.length() > 0) {
                    titleRuns.getJSONObject(0).optString("text", "")
                } else ""

                val ownerObj = vr.optJSONObject("ownerText")
                val ownerRuns = ownerObj?.optJSONArray("runs")
                val artist = if (ownerRuns != null && ownerRuns.length() > 0) {
                    ownerRuns.getJSONObject(0).optString("text", "")
                } else "YouTube Music"

                val durObj = vr.optJSONObject("lengthText")
                val dur = durObj?.optString("simpleText", "") ?: ""

                val thumbObj = vr.optJSONObject("thumbnail")
                val thumbList = thumbObj?.optJSONArray("thumbnails")
                val thumb = if (thumbList != null && thumbList.length() > 0) {
                    thumbList.getJSONObject(thumbList.length() - 1).optString("url", "")
                } else ""

                if (vid.isNotBlank() && title.isNotBlank()) {
                    results.add(
                        MusicTrack(
                            id = vid,
                            title = title,
                            artist = artist,
                            durationText = dur,
                            thumbnailUrl = thumb,
                        )
                    )
                }
            }
            val keys = obj.keys()
            while (keys.hasNext()) {
                val k = keys.next()
                parseYouTubeInnerTube(obj.get(k), results)
            }
        } else if (obj is JSONArray) {
            for (i in 0 until obj.length()) {
                parseYouTubeInnerTube(obj.get(i), results)
            }
        }
    }

    fun loadPlaylists(context: Context): List<MusicPlaylist> {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val raw = prefs.getString(KEY_PLAYLISTS, null)
        if (raw.isNullOrBlank()) {
            val defaults = listOf(
                MusicPlaylist(
                    id = "playlist_favorites",
                    name = "Favorites",
                    tracks = emptyList(),
                ),
            )
            savePlaylists(context, defaults)
            return defaults
        }

        return runCatching {
            val arr = JSONArray(raw)
            buildList {
                for (i in 0 until arr.length()) {
                    val obj = arr.getJSONObject(i)
                    val id = obj.optString("id", "playlist_$i")
                    val name = obj.optString("name", "Playlist").ifBlank { "Playlist" }
                    val tracksJson = obj.optJSONArray("tracks") ?: JSONArray()
                    val tracks = buildList {
                        for (j in 0 until tracksJson.length()) {
                            val t = tracksJson.getJSONObject(j)
                            val tid = t.optString("id", "")
                            val title = t.optString("title", "")
                            if (tid.isNotBlank() && title.isNotBlank()) {
                                add(
                                    MusicTrack(
                                        id = tid,
                                        title = title,
                                        artist = t.optString("artist", ""),
                                        durationText = t.optString("durationText", ""),
                                        thumbnailUrl = t.optString("thumbnailUrl", ""),
                                        streamUrl = t.optString("streamUrl", ""),
                                    ),
                                )
                            }
                        }
                    }
                    add(MusicPlaylist(id = id, name = name, tracks = tracks))
                }
            }
        }.getOrDefault(emptyList())
    }

    fun savePlaylists(context: Context, playlists: List<MusicPlaylist>) {
        val arr = JSONArray()
        playlists.forEach { playlist ->
            val tracksArr = JSONArray()
            playlist.tracks.forEach { track ->
                tracksArr.put(
                    JSONObject().apply {
                        put("id", track.id)
                        put("title", track.title)
                        put("artist", track.artist)
                        put("durationText", track.durationText)
                        put("thumbnailUrl", track.thumbnailUrl)
                        put("streamUrl", track.streamUrl)
                    },
                )
            }
            arr.put(
                JSONObject().apply {
                    put("id", playlist.id)
                    put("name", playlist.name)
                    put("tracks", tracksArr)
                },
            )
        }
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit {
            putString(KEY_PLAYLISTS, arr.toString())
        }
    }

    fun loadPlaybackOrderMode(context: Context): PlaybackOrderMode {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val raw = prefs.getString(KEY_PLAYBACK_ORDER_MODE, PlaybackOrderMode.SEQUENTIAL.name)
        return runCatching {
            PlaybackOrderMode.valueOf(raw ?: PlaybackOrderMode.SEQUENTIAL.name)
        }.getOrDefault(PlaybackOrderMode.SEQUENTIAL)
    }

    fun savePlaybackOrderMode(context: Context, mode: PlaybackOrderMode) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit {
            putString(KEY_PLAYBACK_ORDER_MODE, mode.name)
        }
    }

    private companion object {
        const val PREFS_NAME = "rare_music_prefs"
        const val KEY_PLAYLISTS = "music_playlists_json"
        const val KEY_PLAYBACK_ORDER_MODE = "music_playback_order_mode"
    }
}
