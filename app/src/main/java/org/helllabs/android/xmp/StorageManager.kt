package org.helllabs.android.xmp

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.documentfile.provider.DocumentFile
import timber.log.Timber

/**
 * This object class is kinda of a mash up of anything related to SAF and the Document Tree
 */
object StorageManager {

    /**
     * Get our parent/root directory
     */
    fun getParentDirectory(context: Context, onError: (String) -> Unit): DocumentFile? {
        val prefUri = PrefManager.safStoragePath.let { Uri.parse(it) }

        if (prefUri == null) {
            onError("Getting saved uri returned null")
            return null
        }

        val parent = DocumentFile.fromTreeUri(context, prefUri)
        if (parent == null) {
            onError("Getting parent directory returned null")
            return null
        }

        return parent
    }

    /**
     * Get the playlist directory that was set
     */
    fun getPlaylistDirectory(context: Context): DocumentFile? {
        return getParentDirectory(
            context = context,
            onError = {
                Timber.e(it)
            }
        )?.findFile("playlists")
    }

    /**
     * Get the mod directory
     * This will be where modules are downloaded,
     * and where File Explorer should start
     */
    fun getModDirectory(context: Context): DocumentFile? {
        return getParentDirectory(
            context = context,
            onError = {
                Timber.e(it)
            }
        )?.findFile("mods")
    }

    /**
     * Set the playlist directory to the specified URI
     * Create `playlist` and `mod` folders respectively.
     */
    fun setPlaylistDirectory(
        context: Context,
        uri: Uri?,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        if (uri == null) {
            onError("Setting playlist directory uri was null")
            return
        }

        // Save our Uri
        PrefManager.safStoragePath = uri.toString()

        val flags = Intent.FLAG_GRANT_READ_URI_PERMISSION or
            Intent.FLAG_GRANT_WRITE_URI_PERMISSION

        context.contentResolver.takePersistableUriPermission(uri, flags)

        // Get parent directory
        val parentDocument = getParentDirectory(context, onError) ?: return

        // Create sub directories
        listOf("mods", "playlists").forEach { directoryName ->
            val exists = parentDocument.findFile(directoryName) != null
            if (!exists) {
                parentDocument.createDirectory(directoryName)
            }
        }

        onSuccess()
    }

    /**
     * Get the name of the default path we're allowed to work in.
     */
    fun getDefaultPathName(
        context: Context,
        onSuccess: (String) -> Unit,
        onError: (String) -> Unit
    ) {
        val parent = getParentDirectory(
            context = context,
            onError = {
                onError("Default path name uri was null")
            }
        )

        if (parent == null || parent.name == null) {
            onError("Getting default root path name is null ")
            return
        }

        onSuccess(parent.name!!)
    }

    /**
     * Get the download path a mod should be downloaded to.
     *
     * @see [PrefManager.modArchiveFolder] if the pref was set to download
     * @see [PrefManager.artistFolder]
     */

}
