package org.helllabs.android.xmp

import android.app.Application
import com.android.volley.RequestQueue
import com.android.volley.toolbox.Volley
import dagger.hilt.android.HiltAndroidApp
import timber.log.Timber

@HiltAndroidApp
class XmpApplication : Application() {

    private val mRequestQueue by lazy { Volley.newRequestQueue(applicationContext) }

    var fileList: MutableList<String>? = null

    val requestQueue: RequestQueue
        get() = mRequestQueue

    override fun onCreate() {
        super.onCreate()
        setInstance(this)

        if (BuildConfig.DEBUG) {
            Timber.plant(Timber.DebugTree())
        }

        PrefManager.init(applicationContext)
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
