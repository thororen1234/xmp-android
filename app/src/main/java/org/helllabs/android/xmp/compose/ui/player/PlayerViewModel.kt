package org.helllabs.android.xmp.compose.ui.player

import androidx.compose.material3.DrawerState
import androidx.compose.material3.DrawerValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class PlayerViewModel : ViewModel() {
    sealed class FlipperDirection {
        object Forward : FlipperDirection()
        object Previous : FlipperDirection()
    }

    data class PlayerState(
        val infoName: List<String> = listOf(""),
        val infoType: List<String> = listOf(""),
        val screenOn: Boolean = false,
        val currentViewer: Int = 0,
    )

    data class PlayerInfoState(
        val infoSpeed: String = "00",
        val infoBpm: String = "00",
        val infoPos: String = "00",
        val infoPat: String = "00",
        val isVisible: Boolean = true
    )

    data class PlayerButtonsState(
        val isPlaying: Boolean = false,
        val isRepeating: Boolean = false
    )

    data class PlayerTimeState(
        val timeNow: String = "-:--",
        val timeTotal: String = "-:--",
        val seekPos: Float = 0f,
        val seekMax: Float = 1f,
        val isVisible: Boolean = true,
        val isSeeking: Boolean = false
    )

    data class PlayerDrawerState(
        val drawerState: DrawerState = DrawerState(DrawerValue.Closed),
        val moduleInfo: List<Int> = listOf(0, 0, 0, 0, 0),
        val isPlayAllSequences: Boolean = false,
        val numOfSequences: List<Int> = listOf(),
        val currentSequence: Int = 0
    )

    private val _uiState = MutableStateFlow(PlayerState())
    val uiState = _uiState.asStateFlow()

    private val _infoState = MutableStateFlow(PlayerInfoState())
    val infoState = _infoState.asStateFlow()

    private val _buttonState = MutableStateFlow(PlayerButtonsState())
    val buttonState = _buttonState.asStateFlow()

    private val _timeState = MutableStateFlow(PlayerTimeState())
    val timeState = _timeState.asStateFlow()

    private val _drawerState = MutableStateFlow(PlayerDrawerState())
    val drawerState = _drawerState.asStateFlow()

    private val _flipperState = MutableSharedFlow<FlipperDirection>()
    val flipperState = _flipperState.asSharedFlow()

    val isPlaying: Boolean
        get() = _buttonState.value.isPlaying

    val screenOn: Boolean
        get() = _uiState.value.screenOn

    val isSeeking: Boolean
        get() = _timeState.value.isSeeking

    val currentViewer: Int
        get() = _uiState.value.currentViewer

    fun toggleLoop(value: Boolean) {
        _buttonState.update { it.copy(isRepeating = value) }
    }

    fun isPlaying(value: Boolean) {
        _buttonState.update { it.copy(isPlaying = value) }
    }

    fun screenOn(value: Boolean) {
        _uiState.update { it.copy(screenOn = value) }
    }

    fun seekPos(value: Float) {
        _timeState.update { it.copy(seekPos = value) }
    }

    fun seekMax(value: Float) {
        _timeState.update { it.copy(seekMax = value) }
    }

    fun currentSequence(value: Int) {
        _drawerState.update { it.copy(currentSequence = value) }
    }

    fun setInfoSpeed(value: String) {
        _infoState.update { it.copy(infoSpeed = value) }
    }

    fun setInfoBpm(value: String) {
        _infoState.update { it.copy(infoBpm = value) }
    }

    fun setInfoPos(value: String) {
        _infoState.update { it.copy(infoPos = value) }
    }

    fun setInfoPat(value: String) {
        _infoState.update { it.copy(infoPat = value) }
    }

    fun setTimeNow(value: String) {
        _timeState.update { it.copy(timeNow = value) }
    }

    fun setTimeTotal(value: String) {
        _timeState.update { it.copy(timeTotal = value) }
    }

    fun showInfoLine(value: Boolean) {
        _timeState.update { it.copy(isVisible = value) }
        _infoState.update { it.copy(isVisible = value) }
    }

    fun isSeeking(value: Boolean) {
        _timeState.update { it.copy(isSeeking = value) }
    }

    fun onAllSequence(value: Boolean) {
        _drawerState.update { it.copy(isPlayAllSequences = value) }
    }

    fun setDetails(
        pat: Int,
        ins: Int,
        smp: Int,
        chn: Int,
        len: Int,
        allSeq: Boolean,
        currentSequence: Int,
        sequences: List<Int>
    ) {
        _drawerState.update {
            it.copy(
                moduleInfo = listOf(pat, ins, smp, chn, len),
                isPlayAllSequences = allSeq,
                currentSequence = currentSequence,
                numOfSequences = sequences
            )
        }
    }

    fun setSeekBar(pos: Float, max: Float) {
        _timeState.update { it.copy(seekPos = pos, seekMax = max) }
    }

    /**
     * Add to the flipper list.
     * If we press "Next" to play the next song in queue, info will be added.
     * If we press "Previous", we shouldn't add anything and let PagerState handle it
     */
    fun addFlipperInfo(name: String, type: String) {
        val infoName = _uiState.value.infoName.toMutableList().apply { add(name) }
        val infoType = _uiState.value.infoType.toMutableList().apply { add(type) }

        _uiState.update { it.copy(infoName = infoName, infoType = infoType) }
    }

    fun clearFlipperInfo() {
        val infoName = _uiState.value.infoName.toMutableList().apply { clear() }
        val infoType = _uiState.value.infoType.toMutableList().apply { clear() }
        _uiState.update { it.copy(infoName = infoName, infoType = infoType) }
    }

    fun postFlipperDirection(direction: FlipperDirection) {
        viewModelScope.launch {
            _flipperState.emit(direction)
        }
    }

    fun changeCurrentViewer() {
        val current = (currentViewer + 1) % 3
        _uiState.update { it.copy(currentViewer = current) }
    }
}
