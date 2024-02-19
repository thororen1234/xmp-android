package org.helllabs.android.xmp.model

import com.lazygeniouz.dfc.file.DocumentFileCompat
import org.helllabs.android.xmp.Xmp
import org.helllabs.android.xmp.XmpApplication

data class FileItem(
    val name: String,
    val comment: String,
    val docFile: DocumentFileCompat?,
    val isSpecial: Boolean = false
) : Comparable<FileItem> {
    private val context = XmpApplication.instance?.applicationContext

    private var isValidModule = false

    init {
        if (context != null && docFile != null && docFile.isFile()) {
            isValidModule = Xmp.testFromFd(docFile.uri)
        }
    }

    val isValid = isSpecial || docFile?.isDirectory() == true || isValidModule

    val isFile: Boolean
        get() = docFile?.isFile() ?: false

    override fun compareTo(other: FileItem): Int {
        if (isSpecial != other.isSpecial) {
            return if (isSpecial) -1 else 1
        }

        if (isValid != other.isValid) {
            return if (isValid) -1 else 1
        }

        val isDirectory = docFile?.isDirectory() ?: false
        val otherIsDirectory = other.docFile?.isDirectory() ?: false
        if (isDirectory != otherIsDirectory) {
            return if (isDirectory) -1 else 1
        }

        return name.compareTo(other.name, ignoreCase = true)
    }
}
