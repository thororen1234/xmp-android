package org.helllabs.android.xmp.modarchive.model

import android.text.Html

class Module {

    var artist: String = Artist.UNKNOWN
    var filename: String? = null
    var format: String? = null
    var url: String? = null
    var bytes = 0
    var songTitle: String? = null
        set(value) {
            field = if (value.isNullOrEmpty()) {
                UNTITLED
            } else {
                Html.fromHtml(value).toString()
            }
        }
    var instruments: String? = null
        set(value) {
            val lines = value?.split("\n".toRegex())?.dropLastWhile { it.isEmpty() }?.toTypedArray()
            val buffer = StringBuilder()
            for (line in lines.orEmpty()) {
                buffer.append(Html.fromHtml(line).toString())
                buffer.append('\n')
            }
            field = buffer.toString()
        }
    var license: String? = null
        set(license) {
            field = Html.fromHtml(license).toString()
        }
    var licenseDescription: String? = null
        set(licenseDescription) {
            field = Html.fromHtml(licenseDescription).toString()
        }
    var legalUrl: String? = null
        set(legalUrl) {
            field = Html.fromHtml(legalUrl).toString()
        }

    var id: Long = 0

    override fun toString(): String {
        return songTitle!!
    }

    companion object {
        private const val UNTITLED = "(untitled)"
    }
}
