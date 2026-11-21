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
 * Music repository connecting to curated royalty-free synthwave / lo-fi / chillout
 * radio web streams and open audio sources. Fully compliant for public distribution.
 */
class MusicRepository {

    /** Curated starter cyberpunk, synthwave & chill royalty-free radio stations */
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
            id = "synth_somafm_groovesalad",
            title = "Groove Salad Ambient Chill",
            artist = "SomaFM",
            durationText = "LIVE",
            thumbnailUrl = "https://images.unsplash.com/photo-1470225620780-dba8ba36b745?auto=format&fit=crop&w=400&q=80",
            streamUrl = "https://ice6.somafm.com/groovesalad-128-mp3",
        ),
        MusicTrack(
            id = "synth_somafm_defcon",
            title = "DEF CON Hacker Radio",
            artist = "SomaFM",
            durationText = "LIVE",
            thumbnailUrl = "https://images.unsplash.com/photo-1526374965328-7f61d4dc18c5?auto=format&fit=crop&w=400&q=80",
            streamUrl = "https://ice6.somafm.com/defcon-128-mp3",
        ),
        MusicTrack(
            id = "synth_somafm_dronezone",
            title = "Drone Zone Atmospheric Ambient",
            artist = "SomaFM",
            durationText = "LIVE",
            thumbnailUrl = "https://images.unsplash.com/photo-1509198397868-475647b2a1e5?auto=format&fit=crop&w=400&q=80",
            streamUrl = "https://ice6.somafm.com/dronezone-128-mp3",
        ),
        MusicTrack(
            id = "synth_somafm_vaporwaves",
            title = "Vaporwaves Aesthetic Chill",
            artist = "SomaFM",
            durationText = "LIVE",
            thumbnailUrl = "https://images.unsplash.com/photo-1534447677768-be436bb09401?auto=format&fit=crop&w=400&q=80",
            streamUrl = "https://ice6.somafm.com/vaporwaves-128-mp3",
        ),
    )

    suspend fun searchTracks(query: String): List<MusicTrack> = withContext(Dispatchers.IO) {
        if (query.isBlank()) return@withContext curatedTrendingTracks

        val results = mutableListOf<MusicTrack>()

        // 1. Search Open Radio Browser API (Free global radio & music streams)
        try {
            val encodedQuery = URLEncoder.encode(query, "UTF-8")
            val url = URL("https://de1.api.radio-browser.info/json/stations/byname/$encodedQuery?limit=25")
            val conn = (url.openConnection() as HttpURLConnection).apply {
                connectTimeout = 4000
                readTimeout = 4000
                setRequestProperty("User-Agent", "RareLauncher/1.0")
            }
            if (conn.responseCode == 200) {
                val body = conn.inputStream.bufferedReader().use { it.readText() }
                val items = JSONArray(body)
                for (i in 0 until items.length()) {
                    val item = items.getJSONObject(i)
                    val streamUrl = item.optString("url_resolved", "").ifBlank { item.optString("url", "") }
                    val name = item.optString("name", "")
                    if (streamUrl.isNotBlank() && name.isNotBlank() && (streamUrl.startsWith("http://") || streamUrl.startsWith("https://"))) {
                        val tags = item.optString("tags", "").replace(",", " • ")
                        val favicon = item.optString("favicon", "")
                        val stationUuid = item.optString("stationuuid", "radio_$i")
                        results.add(
                            MusicTrack(
                                id = stationUuid,
                                title = name,
                                artist = tags.ifBlank { "Online Stream" },
                                durationText = "LIVE",
                                thumbnailUrl = favicon,
                                streamUrl = streamUrl,
                            )
                        )
                    }
                }
                if (results.isNotEmpty()) {
                    return@withContext results
                }
            }
        } catch (_: Exception) {}

        // 2. Fallback: Search iTunes preview songs
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

        if (results.isNotEmpty()) results else curatedTrendingTracks
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
