package org.helllabs.android.xmp.service

import android.os.RemoteException
import org.helllabs.android.xmp.Xmp
import org.helllabs.android.xmp.service.utils.QueueManager
import org.helllabs.android.xmp.util.InfoCache
import timber.log.Timber
import java.lang.RuntimeException
import java.lang.ref.WeakReference

class ServiceBinder(playerService: PlayerService) : ModInterface.Stub() {
    private val weakService = WeakReference(playerService)

    private val service: PlayerService
        get() = weakService.get() ?: throw RuntimeException("Weak service null")

    override fun play(
        fileList: MutableList<String>,
        start: Int,
        shuffle: Boolean,
        loopList: Boolean,
        keepFirst: Boolean
    ) {
        if (!service.audioInitialized || !service.hasAudioFocus) {
            service.stopSelf()
            return
        }
        service.queue = QueueManager(fileList, start, shuffle, loopList, keepFirst)
        service.notifier!!.setQueue(service.queue)
        // notifier.clean();
        service.cmd = PlayerService.CMD_NONE
        if (isPaused) {
            service.doPauseAndNotify()
        }
        if (PlayerService.isAlive) {
            Timber.i("Use existing player thread")
            service.restart = true
            service.startIndex = if (keepFirst) 0 else start
            nextSong()
        } else {
            Timber.i("Start player thread")
            service.playThread = Thread(ServiceRunnable(service))
            service.playThread!!.start()
        }
        PlayerService.isAlive = true
    }

    override fun add(fileList: List<String>) {
        service.queue!!.add(fileList)
        service.updateNotification()
    }

    override fun stop() {
        service.actionStop()
    }

    override fun pause() {
        service.doPauseAndNotify()
        service.receiverHelper?.isHeadsetPaused = false
    }

    override fun getInfo(values: IntArray) {
        Xmp.getInfo(values)
    }

    override fun seek(seconds: Int) {
        Xmp.seek(seconds)
    }

    override fun time(): Int {
        return Xmp.time()
    }

    override fun getModVars(vars: IntArray) {
        Xmp.getModVars(vars)
    }

    override fun getModName(): String {
        return Xmp.getModName()
    }

    override fun getModType(): String {
        return Xmp.getModType()
    }

    override fun getChannelData(
        volumes: IntArray,
        finalvols: IntArray,
        pans: IntArray,
        instruments: IntArray,
        keys: IntArray,
        periods: IntArray
    ) {
        if (service.updateData) {
            synchronized(service.playThread!!) {
                Xmp.getChannelData(
                    volumes,
                    finalvols,
                    pans,
                    instruments,
                    keys,
                    periods
                )
            }
        }
    }

    override fun getSampleData(
        trigger: Boolean,
        ins: Int,
        key: Int,
        period: Int,
        chn: Int,
        width: Int,
        buffer: ByteArray
    ) {
        if (service.updateData) {
            synchronized(service.playThread!!) {
                Xmp.getSampleData(
                    trigger,
                    ins,
                    key,
                    period,
                    chn,
                    width,
                    buffer
                )
            }
        }
    }

    override fun nextSong() {
        Xmp.stopModule()
        service.cmd = PlayerService.CMD_NEXT
        if (isPaused) {
            service.doPauseAndNotify()
        }
        service.discardBuffer = true
    }

    override fun prevSong() {
        Xmp.stopModule()
        service.cmd = PlayerService.CMD_PREV
        if (isPaused) {
            service.doPauseAndNotify()
        }
        service.discardBuffer = true
    }

    @Throws(RemoteException::class)
    override fun toggleLoop(): Boolean {
        service.looped = service.looped.xor(true)
        return service.looped
    }

    @Throws(RemoteException::class)
    override fun toggleAllSequences(): Boolean {
        service.playAllSequences = allSequences xor true
        return service.playAllSequences
    }

    @Throws(RemoteException::class)
    override fun getLoop(): Boolean {
        return service.looped
    }

    @Throws(RemoteException::class)
    override fun getAllSequences(): Boolean {
        return service.playAllSequences
    }

    override fun isPaused(): Boolean {
        return service.isPlayerPaused
    }

    override fun setSequence(seq: Int): Boolean {
        val ret = Xmp.setSequence(seq)
        if (ret) {
            service.sequenceNumber = seq
            service.notifyNewSequence()
        }
        return ret
    }

    override fun allowRelease() {
        service.canRelease = true
    }

    override fun getSeqVars(vars: IntArray) {
        Xmp.getSeqVars(vars)
    }

    // for Reconnection
    override fun getFileName(): String {
        return service.currentFileName.orEmpty()
    }

    override fun getInstruments(): Array<String>? {
        return Xmp.getInstruments()
    }

    override fun getPatternRow(
        pat: Int,
        row: Int,
        rowNotes: ByteArray,
        rowInstruments: ByteArray,
        rowFxType: IntArray,
        rowFxParm: IntArray
    ) {
        if (PlayerService.isAlive) {
            Xmp.getPatternRow(pat, row, rowNotes, rowInstruments, rowFxType, rowFxParm)
        }
    }

    override fun mute(chn: Int, status: Int): Int {
        return Xmp.mute(chn, status)
    }

    override fun hasComment(): Boolean {
        return Xmp.getComment() != null
    }

    // File management
    override fun deleteFile(): Boolean {
        Timber.i("Delete file $fileName")
        return InfoCache.delete(fileName)
    }

    // Callback
    override fun registerCallback(callback: PlayerCallback?) {
        callback?.let {
            service.callbacks.register(callback)
        }
    }

    override fun unregisterCallback(callback: PlayerCallback?) {
        callback?.let {
            service.callbacks.unregister(callback)
        }
    }
}
