package org.helllabs.android.xmp.core

import kotlinx.coroutines.runBlocking
import org.helllabs.android.xmp.PrefManager
import org.helllabs.android.xmp.model.Module
import timber.log.Timber
import java.io.File
import java.io.IOException

/**
 * General helper functions related to Files
 */

// fun localFile(url: String?, path: String): File {
//    val filename = url!!.substring(url.lastIndexOf('#') + 1, url.length)
//    return File(path, filename)
// }

fun localFile(module: Module?): File? = runBlocking {
    if (module == null || module.url.isBlank()) {
        return@runBlocking null
    }

    val url = module.url
    val moduleFilename = url.substring(url.lastIndexOf('#') + 1, url.length)
    val path = getDownloadPath(module)

    Timber.d("Module path: $path")
    Timber.d("Module name: $moduleFilename")
    File(path, moduleFilename)
}

fun getDownloadPath(module: Module?): String {
    val sb = StringBuilder()
    val mediaPath = PrefManager.mediaPath
    val useModFolder = PrefManager.modArchiveFolder
    val useArtistFolder = PrefManager.artistFolder

    sb.append(mediaPath)

    if (useModFolder) {
        sb.append(File.separatorChar)
        sb.append(Constants.DEFAULT_DOWNLOAD_DIR)
    }

    if (useArtistFolder) {
        sb.append(File.separatorChar)
        sb.append(module!!.getArtist().asHtml())
    }

    return sb.toString()
}

fun deleteModuleFile(module: Module): Boolean = runBlocking {
    val file = localFile(module)!!

    if (file.isDirectory) {
        return@runBlocking false
    }

    if (!file.delete()) {
        return@runBlocking false
    }

    val useArtistFolder = PrefManager.artistFolder
    if (useArtistFolder) {
        val parent = file.parentFile!!
        val contents = parent.listFiles()
        if (contents != null && contents.isEmpty()) {
            try {
                val path = PrefManager.mediaPath
                val mediaPath = File(path).canonicalPath
                val parentPath = parent.canonicalPath

                if (parentPath.startsWith(mediaPath) &&
                    parentPath != mediaPath
                ) {
                    Timber.i("Remove empty directory ${parent.path}")
                    if (!parent.delete()) {
                        Timber.e("error removing directory")
                        return@runBlocking false
                    }
                }
                return@runBlocking true
            } catch (e: IOException) {
                Timber.e(e)
                return@runBlocking false
            }
        }
    }

    return@runBlocking true
}
