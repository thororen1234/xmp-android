package org.helllabs.android.xmp

import android.app.Application
import com.android.volley.RequestQueue
import com.android.volley.toolbox.Volley

class XmpApplication : Application() {

    var fileList: MutableList<String>? = null

    private val mRequestQueue by lazy { Volley.newRequestQueue(applicationContext) }

    val requestQueue: RequestQueue
        get() = mRequestQueue

    override fun onCreate() {
        super.onCreate()
        setInstance(this)
    }

    fun clearFileList() {
        fileList = null
    }

    companion object {

        @get:Synchronized
        var instance: XmpApplication? = null
            private set

        private fun setInstance(instance: XmpApplication) {
            Companion.instance = instance
        }
    }
}
