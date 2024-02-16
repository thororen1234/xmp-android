package org.helllabs.android.xmp

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.documentfile.provider.DocumentFile
import org.helllabs.android.xmp.core.Constants.DEFAULT_DOWNLOAD_DIR
import org.helllabs.android.xmp.model.Module
import org.helllabs.android.xmp.model.PlaylistItem
import timber.log.Timber

/**
 * This object class is kinda of a mash up of anything related to SAF and the Document Tree
 */
object StorageManager {

    /**
     * Checks if we have a URI in preferences, then checks to see if we have R/W access
     */
    fun checkPermissions(context: Context): Boolean {
        val isPreferenceEmpty = PrefManager.safStoragePath.isNullOrBlank()
        if (isPreferenceEmpty) {
            return false
        }

        val preference = Uri.parse(PrefManager.safStoragePath)
        val persistedUriPermissions = context.contentResolver.persistedUriPermissions
        return persistedUriPermissions.any {
            it.uri == preference && it.isReadPermission && it.isWritePermission
        }
    }

    /**
     * Get our parent/root directory
     */
    private fun getParentDirectory(context: Context, onError: (String) -> Unit): DocumentFile? {
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
    fun getPlaylistDirectory(context: Context, onError: (String) -> Unit = {}): DocumentFile? {
        return getParentDirectory(
            context = context,
            onError = {
                Timber.e(it)
                onError(it)
            }
        )?.findFile("playlists")
    }

    /**
     * Get the mod directory
     * This will be where modules are downloaded,
     * and where File Explorer should start
     */
    fun getModDirectory(context: Context, onError: (String) -> Unit = {}): DocumentFile? {
        return getParentDirectory(
            context = context,
            onError = {
                Timber.e(it)
                onError(it)
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
    fun getDownloadPath(
        context: Context,
        module: Module,
        onSuccess: (DocumentFile) -> Unit,
        onError: (String) -> Unit
    ) {
        val rootDir = getModDirectory(context)
        if (rootDir == null || !rootDir.isDirectory) {
            onError("Unable to access the mod directory.")
            return
        }

        var targetDir = rootDir

        if (PrefManager.modArchiveFolder) {
            val modArchiveDir = targetDir.findFile(DEFAULT_DOWNLOAD_DIR)
                ?: targetDir.createDirectory(DEFAULT_DOWNLOAD_DIR)

            if (modArchiveDir == null || !modArchiveDir.isDirectory) {
                onError("Failed to access or create TMA directory.")
                return
            }

            targetDir = modArchiveDir
        }

        if (PrefManager.artistFolder) {
            val artistName = module.getArtist()
            val artistDir = targetDir.findFile(artistName)
                ?: targetDir.createDirectory(artistName)

            if (artistDir == null || !artistDir.isDirectory) {
                onError("Failed to access or create the artist directory.")
                return
            }

            targetDir = artistDir
        }

        onSuccess(targetDir)
    }

    fun doesModuleExist(context: Context, module: Module?): Boolean {
        if (module == null || module.url.isBlank()) {
            return false
        }

        var exists = false
        doesModuleExist(
            context = context,
            module = module,
            onFound = { exists = true },
            onNotFound = { exists = false },
            onError = { exists = false }
        )

        return exists
    }

    /**
     * Check if a module exists in a location given the preferences
     * @see [PrefManager.artistFolder]
     * @see [PrefManager.modArchiveFolder]
     */
    fun doesModuleExist(
        context: Context,
        module: Module?,
        onFound: (Uri) -> Unit,
        onNotFound: (DocumentFile) -> Unit,
        onError: (String) -> Unit
    ) {
        if (module == null || module.url.isBlank()) {
            onError("Module or module URL is null or blank.")
            return
        }

        getDownloadPath(
            context = context,
            module = module,
            onSuccess = { directory ->
                val moduleFilename = module.url.substringAfterLast('#')
                directory.findFile(moduleFilename)?.let { file ->
                    if (file.exists() && !file.isDirectory) {
                        onFound(file.uri)
                    } else {
                        onNotFound(directory)
                    }
                } ?: onNotFound(directory)
            },
            onError = onError
        )
    }

    fun renamePlaylist(
        context: Context,
        item: PlaylistItem,
        name: String
    ): Boolean {
        TODO()

        // TODO make sure to update the playlist preference
//        with(PrefManager) {
//            putBoolean(
//                key = Playlist.optionName(newName, Playlist.SHUFFLE_MODE),
//                value = getBoolean(
//                    Playlist.optionName(oldName, Playlist.SHUFFLE_MODE),
//                    Playlist.DEFAULT_SHUFFLE_MODE
//                )
//            )
//            putBoolean(
//                key = Playlist.optionName(newName, Playlist.LOOP_MODE),
//                value = getBoolean(
//                    Playlist.optionName(oldName, Playlist.LOOP_MODE),
//                    Playlist.DEFAULT_LOOP_MODE
//                )
//            )
//            remove(Playlist.optionName(oldName, Playlist.SHUFFLE_MODE))
//            remove(Playlist.optionName(oldName, Playlist.LOOP_MODE))
//        }
    }

    fun editPlaylistComment(context: Context, item: PlaylistItem, comment: String): Boolean {
        TODO()
    }

    fun deletePlaylist(context: Context, name: String) {
        TODO()
    }
}
