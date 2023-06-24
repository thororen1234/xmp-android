package org.helllabs.android.xmp.modarchive.model

import org.helllabs.android.xmp.util.Log

class Artist {

    var alias: String? = null
        set(value) {
            Log.d("Artist", "Setting $value")
            field = if (value.isNullOrEmpty()) {
                UNKNOWN
            } else {
                value
            }
        }

    var id: Long = 0

    override fun toString(): String {
        return alias ?: UNKNOWN
    }

    companion object {
        const val UNKNOWN = "unknown"
    }
}
