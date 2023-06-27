package org.helllabs.android.xmp.service

import android.app.Service
import android.content.Intent
import android.media.AudioManager
import android.media.AudioManager.OnAudioFocusChangeListener
import android.os.IBinder
import android.os.RemoteCallbackList
import android.os.RemoteException
import android.support.v4.media.session.MediaSessionCompat
import org.helllabs.android.xmp.PrefManager
import org.helllabs.android.xmp.Xmp.deinit
import org.helllabs.android.xmp.Xmp.getModName
import org.helllabs.android.xmp.Xmp.getModType
import org.helllabs.android.xmp.Xmp.getVolume
import org.helllabs.android.xmp.Xmp.init
import org.helllabs.android.xmp.Xmp.restartAudio
import org.helllabs.android.xmp.Xmp.seek
import org.helllabs.android.xmp.Xmp.setVolume
import org.helllabs.android.xmp.Xmp.stopAudio
import org.helllabs.android.xmp.Xmp.stopModule
import org.helllabs.android.xmp.Xmp.time
import org.helllabs.android.xmp.service.notifier.ModernNotifier
import org.helllabs.android.xmp.service.utils.QueueManager
import org.helllabs.android.xmp.service.utils.RemoteControl
import org.helllabs.android.xmp.service.utils.Watchdog
import org.helllabs.android.xmp.util.FileUtils.basename
import timber.log.Timber

class PlayerService : Service(), OnAudioFocusChangeListener {

    private val binder = ServiceBinder(this)
    internal lateinit var mediaSession: MediaSessionCompat

    internal val callbacks = RemoteCallbackList<PlayerCallback>()
    internal var audioInitialized = false
    internal var audioManager: AudioManager? = null
    internal var canRelease = false
    internal var cmd = 0
    internal var currentFileName: String? = null // currently playing file
    internal var discardBuffer = false // don't play current buffer if changing module while paused
    internal var ducking = false
    internal var hasAudioFocus = false
    internal var looped = false
    internal var notifier: ModernNotifier? = null
    internal var playAllSequences = false
    internal var playThread: Thread? = null
    internal var queue: QueueManager? = null
    internal var receiverHelper: ReceiverHelper? = null
    internal var remoteControl: RemoteControl? = null
    internal var restart = false
    internal var sampleRate = 0
    internal var sequenceNumber = 0
    internal var startIndex = 0
    internal var updateData = false
    internal var watchdog: Watchdog? = null
    private var previousPaused = false // save previous pause state
    private var volume = 0

    var isPlayerPaused = false
        private set

    override fun onCreate() {
        super.onCreate()

        mediaSession = MediaSessionCompat(this, "PlayerService")

        Timber.i("Create service")
        audioManager = getSystemService(AUDIO_SERVICE) as AudioManager
        remoteControl = RemoteControl(this, audioManager)
        hasAudioFocus = requestAudioFocus()
        if (!hasAudioFocus) {
            Timber.e("Can't get audio focus")
        }
        receiverHelper = ReceiverHelper(this)
        receiverHelper!!.registerReceivers()
        var bufferMs = PrefManager.bufferMs
        if (bufferMs < MIN_BUFFER_MS) {
            bufferMs = MIN_BUFFER_MS
        } else if (bufferMs > MAX_BUFFER_MS) {
            bufferMs = MAX_BUFFER_MS
        }
        sampleRate = PrefManager.samplingRate
        if (init(sampleRate, bufferMs)) {
            audioInitialized = true
        } else {
            Timber.e("error initializing audio")
        }

        volume = getVolume()
        isAlive = false
        isLoaded = false
        isPlayerPaused = false
        playAllSequences = PrefManager.allSequences

        // mediaSession.setCallback() // TODO
        mediaSession.isActive = true

        notifier = ModernNotifier(this)

        watchdog = Watchdog(10)
        watchdog!!.setOnTimeoutListener {
            Timber.e("Stopped by watchdog")
            audioManager!!.abandonAudioFocus(this@PlayerService)
            stopSelf()
        }
        watchdog!!.start()
    }

    override fun onStartCommand(intent: Intent, flags: Int, startId: Int): Int {
        return START_NOT_STICKY
    }

    override fun onDestroy() {
        receiverHelper!!.unregisterReceivers()
        watchdog!!.stop()
        notifier!!.cancel()

        mediaSession.isActive = false
        mediaSession.setCallback(null)
        mediaSession.release()

        if (audioInitialized) {
            end(if (hasAudioFocus) RESULT_OK else RESULT_NO_AUDIO_FOCUS)
        } else {
            end(RESULT_CANT_OPEN_AUDIO)
        }
        super.onDestroy()
    }

    override fun onBind(intent: Intent): IBinder {
        return binder
    }

    private fun requestAudioFocus(): Boolean {
        return audioManager!!.requestAudioFocus(
            this,
            AudioManager.STREAM_MUSIC,
            AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK
        ) == AudioManager.AUDIOFOCUS_REQUEST_GRANTED
    }

    internal fun updateNotification() {
        if (queue != null) { // It seems that queue can be null if we're called from PhoneStateListener
            var name = getModName()
            if (name.isEmpty()) {
                name = basename(queue!!.filename)
            }
            notifier!!.notify(
                name,
                getModType(),
                queue!!.index,
                if (isPlayerPaused) ModernNotifier.TYPE_PAUSE else 0
            )
        }
    }

    internal fun doPauseAndNotify() {
        isPlayerPaused = isPlayerPaused xor true
        updateNotification()
        if (isPlayerPaused) {
            stopAudio()
            remoteControl!!.setStatePaused()
        } else {
            remoteControl!!.setStatePlaying()
            restartAudio()
        }
    }

    fun actionStop() {
        stopModule()
        if (isPlayerPaused) {
            doPauseAndNotify()
        }
        cmd = CMD_STOP
    }

    fun actionPlayPause() {
        doPauseAndNotify()

        // Notify clients that we paused
        val numClients = callbacks.beginBroadcast()
        for (i in 0 until numClients) {
            try {
                callbacks.getBroadcastItem(i).pauseCallback()
            } catch (e: RemoteException) {
                Timber.e("Error notifying pause to client")
            }
        }
        callbacks.finishBroadcast()
    }

    fun actionPrev() {
        if (time() > 2000) {
            seek(0)
        } else {
            stopModule()
            cmd = CMD_PREV
        }
        if (isPlayerPaused) {
            doPauseAndNotify()
            discardBuffer = true
        }
    }

    fun actionNext() {
        stopModule()
        if (isPlayerPaused) {
            doPauseAndNotify()
            discardBuffer = true
        }
        cmd = CMD_NEXT
    }

    internal fun notifyNewSequence() {
        val numClients = callbacks.beginBroadcast()
        for (j in 0 until numClients) {
            try {
                callbacks.getBroadcastItem(j).newSequenceCallback()
            } catch (e: RemoteException) {
                Timber.e("Error notifying end of module to client")
            }
        }
        callbacks.finishBroadcast()
    }

    private fun end(result: Int) {
        Timber.i("End service")
        val numClients = callbacks.beginBroadcast()
        for (i in 0 until numClients) {
            try {
                callbacks.getBroadcastItem(i).endPlayCallback(result)
            } catch (e: RemoteException) {
                Timber.e("Error notifying end of play to client")
            }
        }
        callbacks.finishBroadcast()
        isAlive = false
        stopModule()
        if (isPlayerPaused) {
            doPauseAndNotify()
        }
        deinit()
        // audio.release();
    }

    // for audio focus loss
    private fun autoPause(pause: Boolean): Boolean {
        Timber.i("Auto pause changed to $pause, previously ${receiverHelper!!.isAutoPaused}")

        if (pause) {
            previousPaused = isPlayerPaused
            receiverHelper!!.isAutoPaused = true
            isPlayerPaused = false // set to complement, flip on doPause()
            doPauseAndNotify()
        } else {
            if (receiverHelper!!.isAutoPaused && !receiverHelper!!.isHeadsetPaused) {
                receiverHelper!!.isAutoPaused = false
                isPlayerPaused = !previousPaused // set to complement, flip on doPause()
                doPauseAndNotify()
            }
        }
        return receiverHelper!!.isAutoPaused
    }

    override fun onAudioFocusChange(focusChange: Int) {
        when (focusChange) {
            AudioManager.AUDIOFOCUS_LOSS_TRANSIENT -> {
                Timber.d("AUDIOFOCUS_LOSS_TRANSIENT")
                // Pause playback
                autoPause(true)
            }

            AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK -> {
                Timber.d("AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK")
                // Lower volume
                synchronized(audioManager!!) {
                    volume = getVolume()
                    setVolume(DUCK_VOLUME)
                    ducking = true
                }
            }

            AudioManager.AUDIOFOCUS_GAIN -> {
                Timber.d("AUDIOFOCUS_GAIN")
                // Resume playback/raise volume
                autoPause(false)
                synchronized(audioManager!!) {
                    setVolume(volume)
                    ducking = false
                }
            }

            AudioManager.AUDIOFOCUS_LOSS -> {
                Timber.d("AUDIOFOCUS_LOSS")
                // Stop playback
                actionStop()
            }

            else -> {}
        }
    }

    companion object {
        const val RESULT_OK = 0
        const val RESULT_CANT_OPEN_AUDIO = 1
        const val RESULT_NO_AUDIO_FOCUS = 2

        internal const val CMD_NONE = 0
        internal const val CMD_NEXT = 1
        internal const val CMD_PREV = 2
        internal const val CMD_STOP = 3
        private const val MIN_BUFFER_MS = 80
        private const val MAX_BUFFER_MS = 1000
        internal const val DUCK_VOLUME = 0x500

        var isAlive = false
        var isLoaded = false
    }
}
