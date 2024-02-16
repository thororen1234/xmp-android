package org.helllabs.android.xmp.service.utils

import android.net.Uri
import java.util.Collections

class QueueManager(
    fileList: MutableList<Uri>,
    private var start: Int,
    shuffle: Boolean,
    loop: Boolean,
    keepFirst: Boolean
) {

    private val list: MutableList<Uri>
    private val loopListMode: Boolean
    private val ridx: RandomIndex
    private val shuffleMode: Boolean
    private var randomStart = 0
    var index: Int

    val filename: Uri
        get() {
            val idx = if (shuffleMode) ridx.getIndex(index) else index
            return list[idx]
        }

    init {
        if (start >= fileList.size) {
            start = fileList.size - 1
        }

        if (keepFirst) {
            Collections.swap(fileList, 0, start)
            start = 0
            randomStart = 1
        }

        index = start
        list = fileList
        loopListMode = loop
        ridx = RandomIndex(randomStart, fileList.size)
        shuffleMode = shuffle
    }

    fun add(fileList: List<Uri>) {
        if (fileList.isNotEmpty()) {
            ridx.extend(fileList.size, index + 1)
            list.addAll(fileList)
        }
    }

    fun size(): Int = list.size

    operator fun next(): Boolean {
        index++
        if (index >= list.size) {
            index = if (loopListMode) {
                ridx.randomize()
                0
            } else {
                return false
            }
        }
        return true
    }

    fun previous() {
        index -= 2
        if (index < -1) {
            if (loopListMode) {
                index += list.size
            } else {
                index = -1
            }
        }
    }

    fun restart() {
        index = -1
    }
}
