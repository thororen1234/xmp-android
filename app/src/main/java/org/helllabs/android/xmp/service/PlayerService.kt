package org.helllabs.android.xmp.service

import android.app.Service
import android.content.Intent
import android.media.AudioManager
import android.media.AudioManager.OnAudioFocusChangeListener
import android.net.Uri
import android.os.Binder
import android.os.IBinder
import android.support.v4.media.session.MediaSessionCompat
import androidx.media.AudioAttributesCompat
import androidx.media.AudioFocusRequestCompat
import androidx.media.AudioManagerCompat
import java.lang.ref.WeakReference
import org.helllabs.android.xmp.Xmp
import org.helllabs.android.xmp.core.PrefManager
import org.helllabs.android.xmp.core.StorageManager
import org.helllabs.android.xmp.service.notifier.ModernNotifier
import org.helllabs.android.xmp.service.utils.QueueManager
import org.helllabs.android.xmp.service.utils.RemoteControl
import org.helllabs.android.xmp.service.utils.Watchdog
import timber.log.Timber

interface PlayerServiceCallback {
    fun onPlayerPause()
    fun onNewSequence()
    fun onNewMod()
    fun onEndMod()
    fun onEndPlayCallback(result: Int)
}

class PlayerBinder(playerService: PlayerService) : Binder() {
    private val service = WeakReference(playerService)

    fun getService(): PlayerService? = service.get()
}

class PlayerService : Service(), OnAudioFocusChangeListener {

    private var playerServiceCallback: PlayerServiceCallback? = null
    private val binder = PlayerBinder(this)

    internal lateinit var mediaSession: MediaSessionCompat
    private var notifier: ModernNotifier? = null
    private var remoteControl: RemoteControl? = null

    private var audioInitialized = false
    private var audioManager: AudioManager? = null
    private var focusRequest: AudioFocusRequestCompat? = null
    private var ducking = false
    private var hasAudioFocus = false

    private var canRelease = false
    private var cmd = 0
    private var playThread: Thread? = null
    private var restart = false
    private var sampleRate = 0
    private var volume = 0
    private var watchdog: Watchdog? = null

    private var discardBuffer = false // don't play current buffer if changing module while paused
    private var looped = false
    private var playerAllSequences = false
    private var playerFileName: Uri? = null // currently playing file
    private var previousPaused = false // save previous pause state
    private var queue: QueueManager? = null
    private var receiverHelper: ReceiverHelper? = null
    private var sequenceNumber = 0
    private var startIndex = 0
    private var updateData = false

    internal var isPlayerPaused = false

    override fun onCreate() {
        super.onCreate()
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

        if (Xmp.init(sampleRate, bufferMs)) {
            audioInitialized = true
        } else {
            Timber.e("error initializing audio")
        }

        volume = Xmp.getVolume()
        isAlive = false
        isLoaded = false
        isPlayerPaused = false
        playerAllSequences = PrefManager.allSequences

        mediaSession = MediaSessionCompat(this, packageName)
        mediaSession.isActive = true
        notifier = ModernNotifier(this)

        watchdog = Watchdog(10).apply {
            setOnTimeoutListener {
                Timber.e("Stopped by watchdog")
                AudioManagerCompat.abandonAudioFocusRequest(audioManager!!, focusRequest!!)

                stopSelf()
            }
            start()
        }
    }

    override fun onStartCommand(intent: Intent, flags: Int, startId: Int): Int = START_NOT_STICKY

    override fun onDestroy() {
        Timber.d("onDestroy")
        receiverHelper?.unregisterReceivers()
        watchdog?.stop()
        notifier?.cancel()

        mediaSession.isActive = false

        if (audioInitialized) {
            end(if (hasAudioFocus) RESULT_OK else RESULT_NO_AUDIO_FOCUS)
        } else {
            end(RESULT_CANT_OPEN_AUDIO)
        }

        super.onDestroy()
    }

    override fun onBind(intent: Intent): IBinder = binder

    fun setCallback(callback: PlayerServiceCallback?) {
        playerServiceCallback = callback
    }

    private fun requestAudioFocus(): Boolean {
        val playbackAttributes = AudioAttributesCompat.Builder()
            .setUsage(AudioAttributesCompat.USAGE_MEDIA)
            .setContentType(AudioAttributesCompat.CONTENT_TYPE_MUSIC)
            .build()

        focusRequest = AudioFocusRequestCompat.Builder(AudioManagerCompat.AUDIOFOCUS_GAIN)
            .setAudioAttributes(playbackAttributes ?: return false)
            .setWillPauseWhenDucked(false)
            .setOnAudioFocusChangeListener(this)
            .build()

        return AudioManagerCompat.requestAudioFocus(
            audioManager!!,
            focusRequest!!
        ) == AudioManager.AUDIOFOCUS_REQUEST_GRANTED
    }

    private fun updateNotification() {
        // It seems that queue can be null if we're called from PhoneStateListener
        if (queue != null) {
            var name = Xmp.getModName()
            if (name.isEmpty()) {
                name = StorageManager.getFileName(queue?.filename) ?: "<Unknown Title>"
            }

            notifier?.notify(
                name,
                Xmp.getModType(),
                queue!!.index,
                if (isPlayerPaused) ModernNotifier.TYPE_PAUSE else 0
            )
        }
    }

    private fun doPauseAndNotify() {
        isPlayerPaused = isPlayerPaused xor true

        updateNotification()

        if (isPlayerPaused) {
            Xmp.stopAudio()
            remoteControl!!.setStatePaused()
        } else {
            remoteControl!!.setStatePlaying()
            Xmp.restartAudio()
        }
    }

    fun actionStop() {
        Xmp.stopModule()

        if (isPlayerPaused) {
            doPauseAndNotify()
        }

        cmd = CMD_STOP
    }

    fun actionPlayPause() {
        doPauseAndNotify()

        // Notify clients that we paused
        playerServiceCallback?.onPlayerPause()
    }

    fun actionPrev() {
        if (Xmp.time() > 2000) {
            Xmp.seek(0)
        } else {
            Xmp.stopModule()
            cmd = CMD_PREV
        }

        if (isPlayerPaused) {
            doPauseAndNotify()
            discardBuffer = true
        }
    }

    fun actionNext() {
        Xmp.stopModule()

        if (isPlayerPaused) {
            doPauseAndNotify()
            discardBuffer = true
        }

        cmd = CMD_NEXT
    }

    private fun notifyNewSequence() {
        playerServiceCallback?.onNewSequence()
    }

    private inner class PlayRunnable : Runnable {
        override fun run() {
            cmd = CMD_NONE

            val vars = IntArray(8)

            remoteControl!!.setStatePlaying()

            var lastRecognized = 0

            do {
                playerFileName = queue?.filename // Used in reconnection

                // If this file is unrecognized, and we're going backwards, go to previous
                // If we're at the start of the list, go to the last recognized file
                if (playerFileName == null || !Xmp.testFromFd(playerFileName!!)) {
                    Timber.w("$playerFileName: unrecognized format")
                    if (cmd == CMD_PREV) {
                        if (queue!!.index <= 0) {
                            // -1 because we have queue.next() in the while condition
                            queue!!.index = lastRecognized - 1
                            continue
                        }
                        queue!!.previous()
                    }
                    continue
                }

                // Set default pan before we load the module
                val defpan = PrefManager.defaultPan
                Timber.i("Set default pan to $defpan")
                Xmp.setPlayer(Xmp.PLAYER_DEFPAN, defpan)

                // Ditto if we can't load the module
                Timber.i("Load $playerFileName")
                if (Xmp.loadFromFd(playerFileName!!) < 0) {
                    Timber.e("Error loading $playerFileName")
                    if (cmd == CMD_PREV) {
                        if (queue!!.index <= 0) {
                            queue!!.index = lastRecognized - 1
                            continue
                        }
                        queue!!.previous()
                    }
                    continue
                }
                lastRecognized = queue!!.index
                cmd = CMD_NONE
                var name = Xmp.getModName()
                if (name.isEmpty()) {
                    name = StorageManager.getFileName(playerFileName) ?: "<Unkown Title>"
                }

                notifier?.notify(name, Xmp.getModType(), queue!!.index, ModernNotifier.TYPE_TICKER)
                isLoaded = true

                val volBoost = PrefManager.volumeBoost
                val interpTypes =
                    intArrayOf(Xmp.INTERP_NEAREST, Xmp.INTERP_LINEAR, Xmp.INTERP_SPLINE)
                val temp = PrefManager.interpType
                var interpType: Int
                interpType = if (temp in 1..2) {
                    interpTypes[temp]
                } else {
                    Xmp.INTERP_LINEAR
                }
                if (!PrefManager.interpolate) {
                    interpType = Xmp.INTERP_NEAREST
                }
                Xmp.startPlayer(sampleRate)
                synchronized(audioManager!!) {
                    if (ducking) {
                        Xmp.setPlayer(Xmp.PLAYER_VOLUME, DUCK_VOLUME)
                    }
                }

                // Unmute all channels
                for (i in 0..63) {
                    Xmp.mute(i, 0)
                }

                playerServiceCallback?.onNewMod()

                Xmp.setPlayer(Xmp.PLAYER_AMP, volBoost)
                Xmp.setPlayer(Xmp.PLAYER_MIX, PrefManager.stereoMix)
                Xmp.setPlayer(Xmp.PLAYER_INTERP, interpType)
                Xmp.setPlayer(Xmp.PLAYER_DSP, Xmp.DSP_LOWPASS)
                var flags = Xmp.getPlayer(Xmp.PLAYER_CFLAGS)
                flags = if (PrefManager.amigaMixer) {
                    flags or Xmp.FLAGS_A500
                } else {
                    flags and Xmp.FLAGS_A500.inv()
                }
                Xmp.setPlayer(Xmp.PLAYER_CFLAGS, flags)
                updateData = true
                sequenceNumber = 0
                var playNewSequence: Boolean
                Xmp.setSequence(sequenceNumber)

                Xmp.playAudio()

                Timber.i("Enter play loop")
                do {
                    Xmp.getModVars(vars)

                    remoteControl!!.setMetadata(
                        Xmp.getModName(),
                        Xmp.getModType(),
                        vars[0].toLong()
                    )

                    while (cmd == CMD_NONE) {
                        discardBuffer = false

                        // Wait if paused
                        while (isPlayerPaused) {
                            try {
                                Thread.sleep(100)
                            } catch (e: InterruptedException) {
                                break
                            }
                            watchdog!!.refresh()
                            receiverHelper!!.checkReceivers()
                        }

                        if (discardBuffer) {
                            Timber.d("discard buffer")
                            Xmp.dropAudio()
                            break
                        }

                        // Wait if no buffers available
                        while (!Xmp.hasFreeBuffer() && !isPlayerPaused && cmd == CMD_NONE) {
                            try {
                                Thread.sleep(40)
                            } catch (e: InterruptedException) {
                                // Nothing
                            }
                        }

                        // Fill a new buffer
                        if (Xmp.fillBuffer(looped) < 0) {
                            break
                        }
                        watchdog!!.refresh()
                        receiverHelper!!.checkReceivers()
                    }

                    // Subsong explorer
                    // Do all this if we've exited normally and explorer is active
                    playNewSequence = false

                    if (playerAllSequences && cmd == CMD_NONE) {
                        sequenceNumber++
                        Timber.i("Play sequence $sequenceNumber")
                        if (Xmp.setSequence(sequenceNumber)) {
                            playNewSequence = true
                            notifyNewSequence()
                        }
                    }
                } while (playNewSequence)

                Xmp.endPlayer()
                isLoaded = false

                // notify end of module to our clients
                playerServiceCallback?.onEndMod()

                var timeout = 0
                try {
                    while (!canRelease && timeout < 20) {
                        Thread.sleep(100)
                        timeout++
                    }
                } catch (e: InterruptedException) {
                    Timber.e("Sleep interrupted: $e")
                }

                Timber.i("Release module")
                Xmp.releaseModule()

                // Used when current files are replaced by a new set
                if (restart) {
                    Timber.i("Restart")
                    queue!!.index = startIndex - 1
                    cmd = CMD_NONE
                    restart = false
                } else if (cmd == CMD_PREV) {
                    queue!!.previous()
                }
            } while (cmd != CMD_STOP && queue!!.next())

            synchronized(playThread!!) {
                updateData = false // stop getChannelData update
            }

            watchdog?.stop()
            notifier?.cancel()
            remoteControl!!.setStateStopped()
            AudioManagerCompat.abandonAudioFocusRequest(audioManager!!, focusRequest!!)

            Timber.i("Stop service")
            stopSelf()
        }
    }

    private fun end(result: Int) {
        Timber.i("End service")

        playerServiceCallback?.onEndPlayCallback(result)
        isAlive = false

        Xmp.stopModule()

        if (isPlayerPaused) {
            doPauseAndNotify()
        }

        Xmp.deinit()
    }

    // region [Region] Was Stub
    fun play(
        fileList: List<Uri>,
        start: Int,
        shuffle: Boolean,
        loopList: Boolean,
        keepFirst: Boolean
    ) {
        if (!audioInitialized || !hasAudioFocus) {
            stopSelf()
            return
        }

        queue = QueueManager(fileList, start, shuffle, loopList, keepFirst)
        notifier?.queueManager = queue!!

        cmd = CMD_NONE

        if (isPlayerPaused) {
            doPauseAndNotify()
        }
        if (isAlive) {
            Timber.i("Use existing player thread")
            restart = true
            startIndex = if (keepFirst) 0 else start
            nextSong()
        } else {
            Timber.i("Start player thread")
            playThread = Thread(PlayRunnable())
            playThread!!.start()
        }

        isAlive = true
    }

    fun add(fileList: List<Uri>) {
        queue!!.add(fileList)

        updateNotification()
    }

    fun stop() {
        actionStop()
    }

    fun pause() {
        doPauseAndNotify()

        receiverHelper!!.isHeadsetPaused = false
    }

    fun getInfo(values: IntArray) {
        Xmp.getInfo(values)
    }

    fun seek(seconds: Int) {
        Xmp.seek(seconds)
    }

    fun time(): Int = Xmp.time()

    fun getModVars(vars: IntArray) {
        Xmp.getModVars(vars)
    }

    fun getModName(): String = Xmp.getModName()

    fun getModType(): String = Xmp.getModType()

    fun getSampleData(
        trigger: Boolean,
        ins: Int,
        key: Int,
        period: Int,
        chn: Int,
        width: Int,
        buffer: ByteArray
    ) {
        if (updateData) {
            synchronized(playThread!!) {
                Xmp.getSampleData(trigger, ins, key, period, chn, width, buffer)
            }
        }
    }

    fun nextSong() {
        Xmp.stopModule()

        cmd = CMD_NEXT

        if (isPlayerPaused) {
            doPauseAndNotify()
        }

        discardBuffer = true
    }

    fun prevSong() {
        Xmp.stopModule()

        cmd = CMD_PREV

        if (isPlayerPaused) {
            doPauseAndNotify()
        }

        discardBuffer = true
    }

    fun toggleLoop(): Boolean {
        looped = looped.xor(true)

        return looped
    }

    fun toggleAllSequences(): Boolean {
        playerAllSequences = playerAllSequences.xor(true)

        return playerAllSequences
    }

    fun getLoop(): Boolean = looped

    fun getAllSequences(): Boolean = playerAllSequences

    fun isPaused(): Boolean = isPlayerPaused

    fun setSequence(seq: Int): Boolean {
        val ret = Xmp.setSequence(seq)
        if (ret) {
            sequenceNumber = seq
            notifyNewSequence()
        }

        return ret
    }

    fun allowRelease() {
        canRelease = true
    }

    fun getSeqVars(vars: IntArray) {
        Xmp.getSeqVars(vars)
    }

    // for Reconnection
    fun getFileName(): String = StorageManager.getFileName(playerFileName) ?: "<Unknown Title>"

    fun getInstruments(): Array<String> = Xmp.getInstruments()!!

    fun mute(chn: Int, status: Int): Int = Xmp.mute(chn, status)

    // File management
    fun deleteFile(): Boolean {
        Timber.i("Delete file ${getFileName()}")
        return StorageManager.deleteFileOrDirectory(playerFileName)
    }
    // endregion

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
                    volume = Xmp.getVolume()
                    Xmp.setVolume(DUCK_VOLUME)
                    ducking = true
                }
            }

            AudioManager.AUDIOFOCUS_GAIN -> {
                Timber.d("AUDIOFOCUS_GAIN")
                // Resume playback/raise volume
                autoPause(false)
                synchronized(audioManager!!) {
                    Xmp.setVolume(volume)
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

        private const val CMD_NONE = 0
        private const val CMD_NEXT = 1
        private const val CMD_PREV = 2
        private const val CMD_STOP = 3

        private const val MIN_BUFFER_MS = 80
        private const val MAX_BUFFER_MS = 1000
        private const val DUCK_VOLUME = 0x500

        var isAlive = false
        var isLoaded = false
    }
}
