package org.helllabs.android.xmp.compose.ui.player

import android.net.Uri
import androidx.compose.runtime.*
import androidx.lifecycle.ViewModel
import java.util.Collections
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import org.helllabs.android.xmp.Xmp
import org.helllabs.android.xmp.compose.ui.player.viewer.PatternInfo
import org.helllabs.android.xmp.compose.ui.player.viewer.ViewerInfo
import org.helllabs.android.xmp.core.PrefManager
import org.helllabs.android.xmp.service.PlayerService
import timber.log.Timber

@Stable
data class PlayerState(
    val serviceConnected: Boolean = false,
    val showInfoDialog: Boolean = false,
    val showMessageDialog: Boolean = false,
    val showDeleteDialog: Boolean = false,
    val currentViewer: Int = 0,
    val infoTitle: String = "",
    val infoType: String = "",
    val screenOn: Boolean = false,
    val skipToPrevious: Boolean = false
)

@Stable
data class PlayerInfoState(
    val infoSpeed: String = "00",
    val infoBpm: String = "00",
    val infoPos: String = "00",
    val infoPat: String = "00",
    val isVisible: Boolean = true
)

@Stable
data class PlayerButtonsState(
    val isPlaying: Boolean = false,
    val isRepeating: Boolean = false
)

@Stable
data class PlayerTimeState(
    val timeNow: String = "-:--",
    val timeTotal: String = "-:--",
    val seekPos: Float = 0f,
    val seekMax: Float = 1f,
    val isVisible: Boolean = true,
    val isSeeking: Boolean = false
)

@Stable
data class PlayerSheetState(
    val moduleInfo: List<Int> = listOf(0, 0, 0, 0, 0),
    val isPlayAllSequences: Boolean = false,
    val numOfSequences: List<Int> = listOf(),
    val currentSequence: Int = 0
)

@Stable
data class PlayerActivitySate(
    val fileList: List<Uri> = listOf(),
    val keepFirst: Boolean = false,
    val loopListMode: Boolean = false,
    val playTime: Float = 0F,
    val shuffleMode: Boolean = false,
    val skipToPrevious: Boolean = false,
    val start: Int = 0,
    val totalTime: Int = 0,
    val stopUpdate: Boolean = false
)

@Stable
class PlayerViewModel : ViewModel() {

    // Phone CPU's are more than capable enough to do more work with drawing.
    // With android O+, we can use hardware rendering on the canvas, if supported.
    private val newWaveform: Boolean by lazy { PrefManager.useBetterWaveform }
    private val frameRate: Long = 1000L.div(if (newWaveform) 50 else 30)

    private val viewModelJob = Job()
    private val uiScope = CoroutineScope(Dispatchers.IO + viewModelJob)

    private val s = StringBuilder()
    private val c = CharArray(2)

    private val _activityState = MutableStateFlow(PlayerActivitySate())
    val activityState = _activityState.asStateFlow()

    /** Player Variables **/
    private val _uiState = MutableStateFlow(PlayerState())
    val uiState = _uiState.asStateFlow()

    private val _infoState = MutableStateFlow(PlayerInfoState())
    val infoState = _infoState.asStateFlow()

    private val _buttonState = MutableStateFlow(PlayerButtonsState())
    val buttonState = _buttonState.asStateFlow()

    private val _timeState = MutableStateFlow(PlayerTimeState())
    val timeState = _timeState.asStateFlow()

    private val _drawerState = MutableStateFlow(PlayerSheetState())
    val drawerState = _drawerState.asStateFlow()

    val isPlaying: Boolean
        get() = _buttonState.value.isPlaying

    /** Viewer Variables **/
    private val seqVars = MutableStateFlow(IntArray(Xmp.maxSeqFromHeader))

    val insName = MutableStateFlow(arrayOf<String>())

    val isMuted = MutableStateFlow(BooleanArray(0))

    val modVars = MutableStateFlow(IntArray(10))

    val patternInfo = MutableStateFlow(PatternInfo())

    val viewInfo = MutableStateFlow(ViewerInfo())

    /** Player Functions **/

    fun onConnected(value: Boolean) {
        _uiState.update { it.copy(serviceConnected = value) }
    }

    fun toggleLoop(value: Boolean) {
        _buttonState.update { it.copy(isRepeating = value) }
    }

    fun isPlaying(value: Boolean) {
        _buttonState.update { it.copy(isPlaying = value) }
    }

    fun screenOn(value: Boolean) {
        _uiState.update { it.copy(screenOn = value) }
    }

    fun showInfoLine(value: Boolean) {
        _timeState.update { it.copy(isVisible = value) }
        _infoState.update { it.copy(isVisible = value) }
    }

    fun isSeeking(value: Boolean) {
        Timber.w("Trying to seek: $value")
        _timeState.update { it.copy(isSeeking = value) }
    }

    fun onAllSequence(value: Boolean) {
        _drawerState.update { it.copy(isPlayAllSequences = value) }
    }

    /** Viewer Functions **/
    fun changeViewer() {
        val current = (_uiState.value.currentViewer + 1) % 3
        _uiState.update { it.copy(currentViewer = current) }
    }

    fun setup() {
        if (_uiState.value.serviceConnected) {
            Xmp.getModVars(modVars.value)
            Xmp.getSeqVars(seqVars.value)

            insName.value = Xmp.getInstruments()
                ?: Collections.nCopies(modVars.value[4], "").toTypedArray()

            val chn = modVars.value[3]
            isMuted.value = BooleanArray(chn)
            for (i in 0 until chn) {
                isMuted.value[i] = Xmp.mute(i, -1) == 1
            }
        }
    }

    private fun updateInfo(modPlayer: PlayerService) {
        if (isPlaying) {
            // update seekbar
            if (!_timeState.value.isSeeking && _activityState.value.playTime >= 0) {
                _timeState.update {
                    it.copy(seekPos = _activityState.value.playTime)
                }
            }

            // get current frame info
            modPlayer.getInfo(viewInfo.value.values)
            viewInfo.update {
                it.copy(time = modPlayer.time() / 1000)
            }

            // Frame Info - Speed
            updateFrameInfo(
                value = viewInfo.value.values[5],
                update = { value ->
                    _infoState.update {
                        it.copy(infoSpeed = value)
                    }
                }
            )
            // Frame Info - BPM
            updateFrameInfo(
                value = viewInfo.value.values[6],
                update = { value ->
                    _infoState.update {
                        it.copy(infoBpm = value)
                    }
                }
            )
            // Frame Info - Position
            updateFrameInfo(
                value = viewInfo.value.values[0],
                update = { value ->
                    _infoState.update {
                        it.copy(infoPos = value)
                    }
                }
            )
            // Frame Info - Pattern
            updateFrameInfo(
                value = viewInfo.value.values[1],
                update = { value ->
                    _infoState.update {
                        it.copy(infoPat = value)
                    }
                }
            )

            // display playback time
            var t = viewInfo.value.time
            if (t < 0) {
                t = 0
            }
            s.delete(0, s.length)
            Util.to2d(c, t / 60)
            s.append(c)
            s.append(":")
            Util.to02d(c, t % 60)
            s.append(c)
            _timeState.update {
                it.copy(timeNow = s.toString())
            }

            // display total playback time
            s.delete(0, s.length)
            Util.to2d(c, _activityState.value.totalTime / 60)
            s.append(c)
            s.append(":")
            Util.to02d(c, _activityState.value.totalTime % 60)
            s.append(c)
            _timeState.update {
                it.copy(timeTotal = s.toString())
            }
        }
    }

    fun showNewSequence(showSnack: (Int) -> Unit) {
        val time = modVars.value[0]

        _activityState.update {
            it.copy(totalTime = time / 1000)
        }

        _timeState.update {
            it.copy(seekPos = 0f, seekMax = time.div(100f))
        }

        _drawerState.update {
            it.copy(currentSequence = modVars.value[7])
        }

        showSnack(time)
    }

    fun showNewMod(modPlayer: PlayerService) {
        Timber.i("Show new module")

        modPlayer.getModVars(modVars.value)
        modPlayer.getSeqVars(seqVars.value)

        _activityState.update {
            it.copy(playTime = modPlayer.time().div(100F))
        }

        var name: String = modPlayer.getModName()
        val type: String = modPlayer.getModType()
        val allSeq: Boolean = modPlayer.getAllSequences()
        val loop: Boolean = modPlayer.getLoop()

        if (name.trim().isEmpty()) {
            name = modPlayer.getFileName()
        }

        val time = modVars.value[0]
        val len = modVars.value[1]
        val pat = modVars.value[2]
        val chn = modVars.value[3]
        val ins = modVars.value[4]
        val smp = modVars.value[5]
        val numSeq = modVars.value[6]
        val sequences = seqVars.value.take(numSeq)

        _drawerState.update {
            it.copy(
                moduleInfo = listOf(pat, ins, smp, chn, len),
                isPlayAllSequences = allSeq,
                currentSequence = 0,
                numOfSequences = sequences
            )
        }

        _activityState.update {
            it.copy(totalTime = time / 1000)
        }

        _timeState.update {
            it.copy(seekPos = _activityState.value.playTime, seekMax = time.div(100F))
        }

        toggleLoop(loop)

        _uiState.update {
            it.copy(
                infoTitle = name,
                infoType = type,
                skipToPrevious = _activityState.value.skipToPrevious
            )
        }
        skipToPrevious(false)

        viewInfo.update {
            it.copy(type = Xmp.getModType())
        }

        setup()

        _activityState.update {
            it.copy(stopUpdate = false)
        }

        startProgress(modPlayer)
    }

    private fun startProgress(modPlayer: PlayerService) {
        if (uiScope.isActive) {
            return
        }

        uiScope.launch {
            Timber.i("Start progress coroutine")

            val frameStartTime = System.nanoTime()
            var frameTime: Long

            _activityState.update {
                it.copy(playTime = 0F)
            }

            while (isActive) {
                if (_activityState.value.stopUpdate) {
                    Timber.i("Stop update")
                    break
                }

                _activityState.update {
                    it.copy(playTime = modPlayer.time() / 100F)
                }

                if (_uiState.value.screenOn) {
                    // Need to be in Main Thread to update info
                    updateInfo(modPlayer)
                }

                frameTime = (System.nanoTime() - frameStartTime) / 1000000
                if (frameTime < frameRate && !_activityState.value.stopUpdate) {
                    delay(frameRate - frameTime)
                }

                if (_activityState.value.playTime < 0) {
                    break
                }
            }

            modPlayer.allowRelease() // finished playing, we can release the module
        }
    }

    fun stopProgress() {
        uiScope.cancel()
    }

    /**
     * Updates the Player Info text either by Hex or Numerical Value
     */
    private fun updateFrameInfo(
        value: Int,
        update: (String) -> Unit
    ) {
        s.delete(0, s.length)
        if (PrefManager.showHex) {
            Util.to02X(c, value)
            s.append(c)
        } else {
            value.let {
                if (it < 10) s.append(0)
                s.append(it)
            }
        }
        update(s.toString())
    }

    fun setActivityState(
        fileList: List<Uri>,
        shuffleMode: Boolean,
        loopListMode: Boolean,
        keepFirst: Boolean,
        start: Int
    ) {
        _activityState.update {
            it.copy(
                fileList = fileList,
                shuffleMode = shuffleMode,
                loopListMode = loopListMode,
                keepFirst = keepFirst,
                start = start,
            )
        }
    }

    fun skipToPrevious(value: Boolean) {
        _activityState.update {
            it.copy(skipToPrevious = value)
        }
    }

    fun setPlayTime(time: Float) {
        _activityState.update {
            it.copy(playTime = time)
        }
    }

    fun stopUpdate(value: Boolean) {
        _activityState.update {
            it.copy(stopUpdate = value)
        }
    }

    fun showSheet(value: Boolean) {
        _uiState.update {
            it.copy(showInfoDialog = value)
        }
    }

    fun showMessage(value: Boolean) {
        _uiState.update {
            it.copy(showMessageDialog = value)
        }
    }

    fun showDeleteDialog(value: Boolean) {
        _uiState.update {
            it.copy(showDeleteDialog = value)
        }
    }
}
