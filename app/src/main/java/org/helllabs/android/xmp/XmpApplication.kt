package org.helllabs.android.xmp

import android.app.Application
import android.util.Log
import dagger.hilt.android.HiltAndroidApp
import timber.log.Timber

@HiltAndroidApp
class XmpApplication : Application() {

    var fileList: MutableList<String>? = null

    override fun onCreate() {
        super.onCreate()
        setInstance(this)

        if (BuildConfig.DEBUG) {
            Timber.plant(Timber.DebugTree())
        } else {
            Timber.plant(
                object : Timber.Tree() {
                    override fun log(priority: Int, tag: String?, message: String, t: Throwable?) {
                        when (priority) {
                            Log.ERROR -> System.err.println("[ERROR]: $message")
                            Log.WARN -> System.err.println("[WARN]: $message")
                            else -> Unit // Ignore other log types.
                        }
                    }
                }
            )
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
