package org.helllabs.android.xmp.browser

import android.content.Context
import timber.log.Timber
import java.io.File
import java.io.FileNotFoundException
import java.io.FileOutputStream
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream

object Examples {

    fun install(context: Context, path: String, examples: Boolean): Int {
        val dir = File(path)
        if (dir.isDirectory) {
            Timber.d("install: $path directory not found")
            return 0
        }
        if (!dir.mkdirs()) {
            Timber.e("can't create directory: $path")
            return -1
        }
        val am = context.resources.assets
        val assets: Array<String>?
        try {
            assets = am.list("mod")
            if (!examples || assets == null) {
                return 0
            }
            for (a in assets) {
                copyAsset(am.open("mod/$a"), "$path/$a")
            }
        } catch (e: IOException) {
            return -1
        }
        return 0
    }

    private fun copyAsset(inputStream: InputStream, dst: String): Int {
        val buf = ByteArray(1024)
        var len: Int
        try {
            val out: OutputStream = FileOutputStream(File(dst))
            while (inputStream.read(buf).also { len = it } > 0) {
                out.write(buf, 0, len)
            }
            inputStream.close()
            out.close()
        } catch (e: FileNotFoundException) {
            return -1
        } catch (e: IOException) {
            return -1
        }
        return 0
    }
}
