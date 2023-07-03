package org.helllabs.android.xmp.util

import org.helllabs.android.xmp.PrefManager
import org.helllabs.android.xmp.Xmp
import org.helllabs.android.xmp.core.Files
import org.helllabs.android.xmp.model.ModInfo
import java.io.BufferedReader
import java.io.File
import java.io.FileReader
import java.io.IOException

object InfoCache {
    fun clearCache(fileList: List<String>) {
        fileList.forEach { fileName ->
            clearCache(fileName)
        }
    }

    fun clearCache(filename: String): Boolean {
        fun File.deleteIfExists(): Boolean = this.takeIf { it.isFile }?.delete() ?: false

        val cacheFile = File(PrefManager.CACHE_DIR, "$filename.cache")
        val skipFile = File(PrefManager.CACHE_DIR, "$filename.skip")
        return cacheFile.deleteIfExists() or skipFile.deleteIfExists()
    }

    fun delete(filename: String): Boolean {
        val file = File(filename)
        clearCache(filename)
        return file.delete()
    }

    fun deleteRecursive(filename: String): Boolean {
        val file = File(filename)

        if (file.isDirectory) {
            file.deleteRecursively() // Delete the directory and its contents
        } else {
            clearCache(filename) // Clears cache
            file.delete() // Deletes the file
        }

        return !file.exists() // Returns true if the file/directory no longer exists
    }

    fun fileExists(filename: String): Boolean {
        val file = File(filename)
        if (file.isFile) {
            return true
        }
        clearCache(filename)
        return false
    }

    fun testModuleForceIfInvalid(filename: String): Boolean {
        val skipFile = File(PrefManager.CACHE_DIR, "$filename.skip")
        if (skipFile.isFile) {
            skipFile.delete()
        }
        return testModule(filename)
    }

    fun testModule(filename: String): Boolean =
        testModule(filename, ModInfo())

    @Throws(IOException::class)
    private fun checkIfCacheValid(file: File, cacheFile: File, info: ModInfo): Boolean {
        return BufferedReader(FileReader(cacheFile), 512).use { reader ->
            val size = reader.readLine()?.toIntOrNull()
            if (size?.toLong() == file.length()) {
                info.name = reader.readLine()
                reader.readLine() // skip filename
                info.type = reader.readLine()
                return true
            }
            false
        }
    }

    private fun testModule(filename: String, info: ModInfo): Boolean {
        if (!PrefManager.CACHE_DIR.isDirectory && !PrefManager.CACHE_DIR.mkdirs()) {
            // Can't use cache
            return Xmp.testModule(filename, info)
        }

        val file = File(filename)
        val cacheFile = File(PrefManager.CACHE_DIR, "$filename.cache")
        val skipFile = File(PrefManager.CACHE_DIR, "$filename.skip")

        return try {
            // If cache file exists and size matches, file is mod
            if (cacheFile.isFile) {
                // If we have cache and skip, delete skip
                if (skipFile.isFile) {
                    skipFile.delete()
                }

                // Check if our cache data is good
                if (checkIfCacheValid(file, cacheFile, info)) {
                    return true
                }

                cacheFile.delete() // Invalid or outdated cache file
            }

            if (skipFile.isFile) {
                return false
            }

            val isMod = Xmp.testModule(filename, info)
            if (isMod) {
                val lines = arrayOf<String?>(
                    file.length().toString(),
                    info.name,
                    filename,
                    info.type
                )
                val dir: File = cacheFile.parentFile!!
                if (!dir.isDirectory) {
                    dir.mkdirs()
                }
                cacheFile.createNewFile()
                Files.writeToFile(cacheFile, lines)
            } else {
                val dir: File = skipFile.parentFile!!
                if (!dir.isDirectory) {
                    dir.mkdirs()
                }
                skipFile.createNewFile()
            }

            isMod
        } catch (e: IOException) {
            Xmp.testModule(filename, info)
        }
    }
}
