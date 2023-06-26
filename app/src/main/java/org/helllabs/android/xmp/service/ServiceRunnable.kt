package org.helllabs.android.xmp.service

import android.os.RemoteException
import org.helllabs.android.xmp.PrefManager
import org.helllabs.android.xmp.Xmp
import org.helllabs.android.xmp.service.notifier.Notifier
import org.helllabs.android.xmp.util.FileUtils
import org.helllabs.android.xmp.util.InfoCache
import timber.log.Timber

class ServiceRunnable(private val service: PlayerService) : Runnable {
    override fun run() {
        service.cmd = PlayerService.CMD_NONE
        val vars = IntArray(8)
        service.remoteControl!!.setStatePlaying()
        var lastRecognized = 0
        do {
            service.currentFileName = service.queue?.filename // Used in reconnection

            // If this file is unrecognized, and we're going backwards, go to previous
            // If we're at the start of the list, go to the last recognized file
            if (service.currentFileName == null || !InfoCache.testModule(service.currentFileName!!)) {
                Timber.w("${service.currentFileName}: unrecognized format")
                if (service.cmd == PlayerService.CMD_PREV) {
                    if (service.queue!!.index <= 0) {
                        service.queue!!.index =
                            lastRecognized - 1 // -1 because we have queue.next() in the while condition
                        continue
                    }
                    service.queue!!.previous()
                }
                continue
            }

            // Set default pan before we load the module
            val defpan = PrefManager.defaultPan
            Timber.i("Set default pan to $defpan")
            Xmp.setPlayer(Xmp.PLAYER_DEFPAN, defpan)

            // Ditto if we can't load the module
            Timber.i("Load ${service.currentFileName}")
            if (Xmp.loadModule(service.currentFileName) < 0) {
                Timber.e("Error loading ${service.currentFileName}")
                if (service.cmd == PlayerService.CMD_PREV) {
                    if (service.queue!!.index <= 0) {
                        service.queue!!.index = lastRecognized - 1
                        continue
                    }
                    service.queue!!.previous()
                }
                continue
            }
            lastRecognized = service.queue!!.index
            service.cmd = PlayerService.CMD_NONE
            var name = Xmp.getModName()
            if (name.isEmpty()) {
                name = FileUtils.basename(service.currentFileName)
            }
            service.notifier!!.notify(
                name,
                Xmp.getModType(),
                service.queue!!.index,
                Notifier.TYPE_TICKER
            )
            PlayerService.isLoaded = true

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
            Xmp.startPlayer(service.sampleRate)
            synchronized(service.audioManager!!) {
                if (service.ducking) {
                    Xmp.setPlayer(Xmp.PLAYER_VOLUME, PlayerService.DUCK_VOLUME)
                }
            }

            // Unmute all channels
            for (i in 0..63) {
                Xmp.mute(i, 0)
            }

            var numClients = service.callbacks.beginBroadcast()
            for (j in 0 until numClients) {
                try {
                    service.callbacks.getBroadcastItem(j).newModCallback()
                } catch (e: RemoteException) {
                    Timber.e("Error notifying new module to client")
                }
            }
            service.callbacks.finishBroadcast()
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
            service.updateData = true
            service.sequenceNumber = 0
            var playNewSequence: Boolean
            Xmp.setSequence(service.sequenceNumber)
            Xmp.playAudio()
            Timber.i("Enter play loop")
            do {
                Xmp.getModVars(vars)
                service.remoteControl!!.setMetadata(
                    Xmp.getModName(),
                    Xmp.getModType(),
                    vars[0].toLong()
                )
                while (service.cmd == PlayerService.CMD_NONE) {
                    service.discardBuffer = false

                    // Wait if paused
                    while (service.isPlayerPaused) {
                        try {
                            Thread.sleep(100)
                        } catch (e: InterruptedException) {
                            break
                        }
                        service.watchdog!!.refresh()
                        service.receiverHelper!!.checkReceivers()
                    }
                    if (service.discardBuffer) {
                        Timber.d("discard buffer")
                        Xmp.dropAudio()
                        break
                    }

                    // Wait if no buffers available
                    while (!Xmp.hasFreeBuffer() && !service.isPlayerPaused && service.cmd == PlayerService.CMD_NONE) {
                        try {
                            Thread.sleep(40)
                        } catch (e: InterruptedException) {
                            /* Nothing */
                        }
                    }

                    // Fill a new buffer
                    if (Xmp.fillBuffer(service.looped) < 0) {
                        break
                    }
                    service.watchdog!!.refresh()
                    service.receiverHelper!!.checkReceivers()
                }

                // Subsong explorer
                // Do all this if we've exited normally and explorer is active
                playNewSequence = false
                if (service.playAllSequences && service.cmd == PlayerService.CMD_NONE) {
                    service.sequenceNumber++
                    Timber.i("Play sequence ${service.sequenceNumber}")
                    if (Xmp.setSequence(service.sequenceNumber)) {
                        playNewSequence = true
                        service.notifyNewSequence()
                    }
                }
            } while (playNewSequence)
            Xmp.endPlayer()
            PlayerService.isLoaded = false

            // notify end of module to our clients
            numClients = service.callbacks.beginBroadcast()
            if (numClients > 0) {
                service.canRelease = false
                for (j in 0 until numClients) {
                    try {
                        Timber.i("Call end of module callback")
                        service.callbacks.getBroadcastItem(j).endModCallback()
                    } catch (e: RemoteException) {
                        Timber.e("Error notifying end of module to client")
                    }
                }
                service.callbacks.finishBroadcast()

                // if we have clients, make sure we can release module
                var timeout = 0
                try {
                    while (!service.canRelease && timeout < 20) {
                        Thread.sleep(100)
                        timeout++
                    }
                } catch (e: InterruptedException) {
                    Timber.e("Sleep interrupted: $e")
                }
            } else {
                service.callbacks.finishBroadcast()
            }
            Timber.i("Release module")
            Xmp.releaseModule()

            // audio.stop();

            // Used when current files are replaced by a new set
            if (service.restart) {
                Timber.i("Restart")
                service.queue!!.index = service.startIndex - 1
                service.cmd = PlayerService.CMD_NONE
                service.restart = false
            } else if (service.cmd == PlayerService.CMD_PREV) {
                service.queue!!.previous()
                // returnToPrev = false;
            }
        } while (service.cmd != PlayerService.CMD_STOP && service.queue!!.next())
        synchronized(service.playThread!!) {
            service.updateData = false // stop getChannelData update
        }
        service.watchdog!!.stop()
        service.notifier!!.cancel()
        service.remoteControl!!.setStateStopped()
        service.audioManager!!.abandonAudioFocus(service)

        // end();
        Timber.i("Stop service")
        service.stopSelf()
    }
}
