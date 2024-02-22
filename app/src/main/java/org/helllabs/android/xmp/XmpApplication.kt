package org.helllabs.android.xmp

import android.app.Application
import android.net.Uri
import android.util.Log
import org.helllabs.android.xmp.core.PrefManager
import org.helllabs.android.xmp.di.ModArchiveModule
import org.helllabs.android.xmp.di.ModArchiveModuleImpl
import timber.log.Timber

// TODO add migration tool for older playlists.

class XmpApplication : Application() {
    var fileListUri: List<Uri>? = null

    override fun onCreate() {
        super.onCreate()
        setInstance(this)

        modArchiveModule = ModArchiveModuleImpl()

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
        fileListUri = null
    }

    companion object {
        lateinit var modArchiveModule: ModArchiveModule

        @get:Synchronized
        var instance: XmpApplication? = null
            private set

        private fun setInstance(app: XmpApplication) {
            instance = app
        }
    }
}
