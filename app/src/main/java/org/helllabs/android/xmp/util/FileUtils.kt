package org.helllabs.android.xmp.util

import java.io.BufferedReader
import java.io.BufferedWriter
import java.io.File
import java.io.FileReader
import java.io.FileWriter
import java.io.IOException
import java.io.PrintWriter

object FileUtils {

    @JvmStatic
    @Throws(IOException::class)
    fun writeToFile(file: File, lines: Array<String?>) {
        val out = BufferedWriter(FileWriter(file, true), 512)
        for (line in lines) {
            out.write(line)
            out.newLine()
        }
        out.close()
    }

    @JvmStatic
    @Throws(IOException::class)
    fun writeToFile(file: File, line: String) {
        val lines = arrayOf<String?>(line)
        writeToFile(file, lines)
    }

    @JvmStatic
    @Throws(IOException::class)
    fun readFromFile(file: File): String {
        val `in` = BufferedReader(FileReader(file), 512)
        val line = `in`.readLine()
        `in`.close()
        return line
    }

    @Throws(IOException::class)
    fun removeLineFromFile(file: File, num: Int): Boolean {
        val nums = intArrayOf(num)
        return removeLineFromFile(file, nums)
    }

    @JvmStatic
    @Throws(IOException::class)
    fun removeLineFromFile(file: File, num: IntArray): Boolean {
        val tempFile = File(file.absolutePath + ".tmp")
        val reader = BufferedReader(FileReader(file), 512)
        val writer = PrintWriter(FileWriter(tempFile))
        var line: String?
        var flag: Boolean
        var lineNum = 0
        while (reader.readLine().also { line = it } != null) {
            flag = false
            for (n in num) {
                if (lineNum == n) {
                    flag = true
                    break
                }
            }
            if (!flag) {
                writer.println(line)
                writer.flush()
            }
            lineNum++
        }
        writer.close()
        reader.close()

        // Delete the original file
        return if (!file.delete()) {
            false
        } else {
            // Rename the new file to the filename the original file had.
            tempFile.renameTo(file)
        }
    }

    @JvmStatic
    fun basename(pathname: String?): String = pathname?.let { File(it).name }.orEmpty()
}