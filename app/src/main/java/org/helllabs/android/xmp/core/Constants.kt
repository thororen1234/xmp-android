package org.helllabs.android.xmp.core

import org.helllabs.android.xmp.BuildConfig
import org.helllabs.android.xmp.model.Module

object Constants {
    const val APIKEY: String = BuildConfig.API_KEY

    const val DEFAULT_DOWNLOAD_DIR = "TheModArchive"

    /* Playlist */
    const val SUFFIX = ".json"

    /* Player */
    const val PARM_KEEPFIRST = "keepFirst"
    const val PARM_LOOP = "loop"
    const val PARM_SHUFFLE = "shuffle"
    const val PARM_START = "start"

    /* Search */
    const val BASE_URL: String = "https://api.modarchive.org"
    const val BY_ARTIST: String = "search_artist"
    const val BY_ARTIST_ID: String = "view_modules_by_artistid"
    const val BY_MODULE_ID: String = "view_by_moduleid"
    const val BY_RANDOM: String = "random"
    const val BY_SEARCH: String = "search"
    const val TYPE_FILE_OR_TITLE = "filename_or_songtitle"

    private val UNSUPPORTED = listOf("AHX", "HVL", "MO3")
    fun Module.isSupported(): Boolean = !UNSUPPORTED.contains(this.format)
}
