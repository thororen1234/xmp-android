// package org.helllabs.android.xmp.core
//
// import kotlinx.coroutines.runBlocking
// import org.helllabs.android.xmp.PrefManager
// import org.helllabs.android.xmp.core.Strings.asHtml
// import org.helllabs.android.xmp.model.Module
// import timber.log.Timber
// import java.io.BufferedReader
// import java.io.BufferedWriter
// import java.io.File
// import java.io.FileWriter
// import java.io.IOException
//
// /**
// * General helper functions related to Files
// */
// object Files {
//
//    @Throws(IOException::class)
//    fun writeToFile(file: File, lines: Array<String?>) {
//        val out = BufferedWriter(FileWriter(file, true), 512)
//        for (line in lines) {
//            out.write(line)
//            out.newLine()
//        }
//        out.close()
//    }
//
//    @Throws(IOException::class)
//    fun writeToFile(file: File, line: String) {
//        val lines = arrayOf<String?>(line)
//        writeToFile(file, lines)
//    }
//
//    @Throws(IOException::class)
//    fun readFromFile(file: File): String =
//        file.bufferedReader().useLines { it.firstOrNull() ?: "" }
//
//    @Throws(IOException::class)
//    fun removeLineFromFile(file: File, num: Int): Boolean {
//        val nums = intArrayOf(num)
//        return removeLineFromFile(file, nums)
//    }
//
//    @Throws(IOException::class)
//    fun removeLineFromFile(file: File, num: IntArray): Boolean {
//        val linesToSkip = num.toSet()
//        val tempFile = File(file.absolutePath + ".tmp")
//
//        file.bufferedReader().use { reader ->
//            tempFile.printWriter().use { writer ->
//                reader.forEachLineIndexed { index, line ->
//                    if (index !in linesToSkip) {
//                        writer.println(line)
//                    }
//                }
//            }
//        }
//
//        return if (!file.delete()) {
//            false
//        } else {
//            tempFile.renameTo(file)
//        }
//    }
//
//    // Extension for BufferedReader to handle reading each line with an index
//    private fun BufferedReader.forEachLineIndexed(action: (Int, String) -> Unit) {
//        var index = 0
//        for (line in this.lineSequence()) {
//            action(index++, line)
//        }
//    }
//
//    fun basename(pathname: String?): String = pathname?.let { File(it).name }.orEmpty()
//
//    fun recursiveList(path: String): List<String> =
//        recursiveList(File(path))
//
//    fun recursiveList(file: File?): List<String> =
//        file?.walk()
//            ?.filter { it.isFile }
//            ?.map { it.path }
//            ?.sortedBy { it.lowercase() }
//            ?.toList()
//            ?: emptyList()
//
//    @Deprecated("", level = DeprecationLevel.ERROR)
//    fun localFile(module: Module?): File? = runBlocking {
//        if (module == null || module.url.isBlank()) {
//            return@runBlocking null
//        }
//
//        val url = module.url
//        val moduleFilename = url.substring(url.lastIndexOf('#') + 1, url.length)
//        val path = getDownloadPath(module)
//
//        Timber.d("Module path: $path")
//        Timber.d("Module name: $moduleFilename")
//        File(path, moduleFilename)
//    }
//
//    @Deprecated("", level = DeprecationLevel.ERROR)
//    fun getDownloadPath(module: Module?): String {
//        val sb = StringBuilder()
//        val mediaPath = PrefManager.mediaPath
//        val useModFolder = PrefManager.modArchiveFolder
//        val useArtistFolder = PrefManager.artistFolder
//
//        sb.append(mediaPath)
//
//        if (useModFolder) {
//            sb.append(File.separatorChar)
//            sb.append(Constants.DEFAULT_DOWNLOAD_DIR)
//        }
//
//        if (useArtistFolder) {
//            sb.append(File.separatorChar)
//            sb.append(module!!.getArtist().asHtml())
//        }
//
//        return sb.toString()
//    }
//
//    fun deleteModuleFile(module: Module): Boolean = runBlocking {
//        val file = localFile(module)!!
//
//        if (file.isDirectory) {
//            return@runBlocking false
//        }
//
//        if (!file.delete()) {
//            return@runBlocking false
//        }
//
//        val useArtistFolder = PrefManager.artistFolder
//        if (useArtistFolder) {
//            val parent = file.parentFile!!
//            val contents = parent.listFiles()
//            if (contents != null && contents.isEmpty()) {
//                try {
//                    val path = PrefManager.mediaPath
//                    val mediaPath = File(path).canonicalPath
//                    val parentPath = parent.canonicalPath
//
//                    if (parentPath.startsWith(mediaPath) &&
//                        parentPath != mediaPath
//                    ) {
//                        Timber.i("Remove empty directory ${parent.path}")
//                        if (!parent.delete()) {
//                            Timber.e("error removing directory")
//                            return@runBlocking false
//                        }
//                    }
//                    return@runBlocking true
//                } catch (e: IOException) {
//                    Timber.e(e)
//                    return@runBlocking false
//                }
//            }
//        }
//
//        return@runBlocking true
//    }
// }
