package org.helllabs.android.xmp.core

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit
import androidx.preference.PreferenceManager

object PrefManager {

    private lateinit var prefs: SharedPreferences

    fun init(context: Context) {
        prefs = PreferenceManager.getDefaultSharedPreferences(context)
    }

    /**
     * The URI where the app can store mods and playlists.
     */
    var safStoragePath: String?
        get() = prefs.getString("saf_storage_path", null)
        set(value) {
            prefs.edit { putString("saf_storage_path", value) }
        }

    var startOnPlayer: Boolean
        get() = prefs.getBoolean("start_on_player", true)
        set(value) {
            prefs.edit { putBoolean("start_on_player", value) }
        }

    var showToast: Boolean
        get() = prefs.getBoolean("show_toast", true)
        set(value) {
            prefs.edit { putBoolean("show_toast", value) }
        }

    /**
     * 1. Start playing at selection
     * 2. Play selected file
     * 3. Enqueue selected file
     */
    @Deprecated("")
    var playlistMode: Int
        get() = prefs.getInt("playlist_mode", 1)
        set(value) {
            prefs.edit { putInt("playlist_mode", value) }
        }

    var enableDelete: Boolean
        get() = prefs.getBoolean("enable_delete", false)
        set(value) {
            prefs.edit { putBoolean("enable_delete", value) }
        }

    var allSequences: Boolean
        get() = prefs.getBoolean("all_sequences", false)
        set(value) {
            prefs.edit { putBoolean("all_sequences", value) }
        }

    var keepScreenOn: Boolean
        get() = prefs.getBoolean("keep_screen_on", false)
        set(value) {
            prefs.edit { putBoolean("keep_screen_on", value) }
        }

    var showInfoLine: Boolean
        get() = prefs.getBoolean("show_info_line", true)
        set(value) {
            prefs.edit { putBoolean("show_info_line", value) }
        }

    var useFileName: Boolean
        get() = prefs.getBoolean("use_filename", false)
        set(value) {
            prefs.edit { putBoolean("use_filename", value) }
        }

    var installedExamplePlaylist: Boolean
        get() = prefs.getBoolean("example_playlist_created", false)
        set(value) {
            prefs.edit { putBoolean("example_playlist_created", value) }
        }

    var examples: Boolean
        get() = prefs.getBoolean("examples", true)
        set(value) {
            prefs.edit { putBoolean("examples", value) }
        }

    var backButtonNavigation: Boolean
        get() = prefs.getBoolean("back_button_navigation", true)
        set(value) {
            prefs.edit { putBoolean("back_button_navigation", value) }
        }

    var shuffleMode: Boolean
        get() = prefs.getBoolean("options_shuffleMode", true)
        set(value) {
            prefs.edit { putBoolean("options_shuffleMode", value) }
        }

    var loopMode: Boolean
        get() = prefs.getBoolean("options_loopMode", false)
        set(value) {
            prefs.edit { putBoolean("options_loopMode", value) }
        }

    var modArchiveFolder: Boolean
        get() = prefs.getBoolean("modarchive_folder", true)
        set(value) {
            prefs.edit { putBoolean("modarchive_folder", value) }
        }

    var artistFolder: Boolean
        get() = prefs.getBoolean("artist_folder", true)
        set(value) {
            prefs.edit { putBoolean("artist_folder", value) }
        }

    var bufferMs: Int
        get() = prefs.getInt("buffer_ms_opensl", 400)
        set(value) {
            prefs.edit { putInt("buffer_ms_opensl", value) }
        }

    var samplingRate: Int
        get() = prefs.getInt("sampling_rate", 44100)
        set(value) {
            prefs.edit { putInt("sampling_rate", value) }
        }

    var defaultPan: Int
        get() = prefs.getInt("default_pan", 50)
        set(value) {
            prefs.edit { putInt("default_pan", value) }
        }

    var volumeBoost: Int
        get() = prefs.getInt("vol_boost", 1)
        set(value) {
            prefs.edit { putInt("vol_boost", value) }
        }

    var interpType: Int
        get() = prefs.getInt("interp_type", 1)
        set(value) {
            prefs.edit { putInt("interp_type", value) }
        }

    var interpolate: Boolean
        get() = prefs.getBoolean("interpolate", true)
        set(value) {
            prefs.edit { putBoolean("interpolate", value) }
        }

    var stereoMix: Int
        get() = prefs.getInt("stereo_mix", 100)
        set(value) {
            prefs.edit { putInt("stereo_mix", value) }
        }

    var amigaMixer: Boolean
        get() = prefs.getBoolean("amiga_mixer", false)
        set(value) {
            prefs.edit { putBoolean("amiga_mixer", value) }
        }

    var headsetPause: Boolean
        get() = prefs.getBoolean("headset_pause", true)
        set(value) {
            prefs.edit { putBoolean("headset_pause", value) }
        }

    var bluetoothPause: Boolean
        get() = prefs.getBoolean("bluetooth_pause", true)
        set(value) {
            prefs.edit { putBoolean("bluetooth_pause", value) }
        }

    var useBetterWaveform: Boolean
        get() = prefs.getBoolean("new_waveform", true)
        set(value) {
            prefs.edit { putBoolean("new_waveform", value) }
        }

    var useMediaStyle: Boolean
        get() = prefs.getBoolean("use_media_style", true)
        set(value) {
            prefs.edit { putBoolean("use_media_style", value) }
        }

    var searchHistory: String
        get() = prefs.getString("search_history", "[]")!!
        set(value) {
            prefs.edit { putString("search_history", value) }
        }

    var showHex: Boolean
        get() = prefs.getBoolean("player_show_hex", false)
        set(value) {
            prefs.edit { putBoolean("player_show_hex", value) }
        }
}
