package org.helllabs.android.xmp.modarchive.response

abstract class ListResponse<T> : ModArchiveResponse() {

    val list: MutableList<T> = ArrayList()

    fun add(item: T) {
        list.add(item)
    }

    val isEmpty: Boolean
        get() = list.isEmpty()

    operator fun get(location: Int): T {
        return list[location]
    }
}
