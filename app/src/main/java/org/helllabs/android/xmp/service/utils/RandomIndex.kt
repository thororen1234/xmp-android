package org.helllabs.android.xmp.service.utils

import java.util.Date
import java.util.Random

class RandomIndex(start: Int, size: Int) {

    private var idx = IntArray(size) { it }
    private val random = Random(Date().time)

    init {
        randomize(start, size - start)
    }

    fun randomize(start: Int = 0, length: Int = idx.size) {
        val end = start + length
        for (i in start until end) {
            val num = random.nextInt(i + 1)
            idx[i] = idx[num].also { idx[num] = idx[i] }
        }
    }

    fun extend(amount: Int, index: Int) {
        val length = idx.size
        idx += IntArray(amount) { length + it }
        randomize(index, idx.size - index)
    }

    fun getIndex(num: Int): Int = idx[num]
}
