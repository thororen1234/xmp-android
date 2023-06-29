package org.helllabs.android.xmp.core

import android.os.Build
import android.text.Html
import android.text.Spanned
import androidx.core.text.toSpanned
import kotlin.String

/**
 * General helper functions related to Strings
 */

object Strings {
    fun String?.asHtml(): Spanned {
        if (this.isNullOrEmpty()) {
            return "".toSpanned()
        }
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            Html.fromHtml(this, Html.FROM_HTML_MODE_LEGACY)
        } else {
            @Suppress("DEPRECATION")
            Html.fromHtml(this)
        }
    }
}
