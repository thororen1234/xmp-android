package org.helllabs.android.xmp.core

import android.content.Context
import java.io.File
import java.io.IOException
import java.io.InputStream

/**
 * General helper functions related to Recourses -> Assets
 */
object Assets {

    @Throws(IOException::class)
    fun install(context: Context, path: String, examples: Boolean) {
        if (!examples) {
            return
        }

        val dir = File(path)

        if (!dir.isDirectory && !dir.mkdirs()) {
            throw IOException("Can't create directory: $path")
        }

        with(context.resources.assets) {
            list("mod")?.forEach { asset ->
                open("mod/$asset").use { input -> copyAsset(input, "$path/$asset") }
            }
        }
    }

    @Throws(IOException::class)
    private fun copyAsset(inputStream: InputStream, dst: String) {
        val outFile = File(dst)
        inputStream.use { input ->
            outFile.outputStream().use { output ->
                input.copyTo(output)
            }
        }
    }
}
