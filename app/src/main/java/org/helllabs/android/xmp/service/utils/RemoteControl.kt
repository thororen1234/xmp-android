package org.helllabs.android.xmp.service.utils

import android.app.PendingIntent
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.media.AudioManager
import android.media.MediaMetadataRetriever
import android.media.RemoteControlClient
import org.helllabs.android.xmp.service.receiver.RemoteControlReceiver
import timber.log.Timber

@Suppress("deprecation")
class RemoteControl(context: Context, private val audioManager: AudioManager?) {

    private val remoteControlReceiver: ComponentName
    private var remoteControlClient: RemoteControlClientCompat? = null

    init {
        remoteControlReceiver =
            ComponentName(context.packageName, RemoteControlReceiver::class.java.name)
        if (remoteControlClient == null) {
            Timber.i("Register remote control client")
            audioManager!!.registerMediaButtonEventReceiver(remoteControlReceiver)
            val intent = Intent(Intent.ACTION_MEDIA_BUTTON)
            intent.component = remoteControlReceiver
            val pendingIntent = PendingIntent.getBroadcast(context, 0, intent, 0)
            remoteControlClient = RemoteControlClientCompat(pendingIntent)
            remoteControlClient?.setTransportControlFlags(
                RemoteControlClient.FLAG_KEY_MEDIA_PLAY_PAUSE or
                    RemoteControlClient.FLAG_KEY_MEDIA_STOP or
                    RemoteControlClient.FLAG_KEY_MEDIA_NEXT or
                    RemoteControlClient.FLAG_KEY_MEDIA_PREVIOUS
            )
            RemoteControlHelper.registerRemoteControlClient(audioManager, remoteControlClient!!)
        }
    }

    fun unregisterReceiver() {
        Timber.w("Unregister remote control client")
        audioManager!!.unregisterMediaButtonEventReceiver(remoteControlReceiver)
        RemoteControlHelper.unregisterRemoteControlClient(audioManager, remoteControlClient)
    }

    fun setStatePlaying() {
        if (remoteControlClient != null) {
            Timber.i("Set state to playing")
            remoteControlClient?.setPlaybackState(RemoteControlClient.PLAYSTATE_PLAYING)
        }
    }

    fun setStatePaused() {
        if (remoteControlClient != null) {
            Timber.i("Set state to paused")
            remoteControlClient?.setPlaybackState(RemoteControlClient.PLAYSTATE_PAUSED)
        }
    }

    fun setStateStopped() {
        if (remoteControlClient != null) {
            Timber.i("Set state to stopped")
            remoteControlClient?.setPlaybackState(RemoteControlClient.PLAYSTATE_STOPPED)
        }
    }

    fun setMetadata(title: String, type: String, duration: Long) {
        if (remoteControlClient != null) {
            val editor = remoteControlClient?.editMetadata(true)
            // editor.putBitmap(MetadataEditor.BITMAP_KEY_ARTWORK, dummyAlbumArt);
            editor!!.putLong(MediaMetadataRetriever.METADATA_KEY_DURATION, duration)
            editor.putString(MediaMetadataRetriever.METADATA_KEY_ARTIST, type)
            editor.putString(MediaMetadataRetriever.METADATA_KEY_TITLE, title)
            editor.apply()
        }
    }

    companion object {
        private const val TAG = "RemoteControl"
    }
}
