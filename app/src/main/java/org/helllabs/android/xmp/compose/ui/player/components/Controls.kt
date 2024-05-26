package org.helllabs.android.xmp.compose.ui.player.components

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.*
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.draw.*
import androidx.compose.ui.graphics.*
import androidx.compose.ui.tooling.preview.*
import com.theapache64.rebugger.Rebugger
import org.helllabs.android.xmp.compose.theme.XmpTheme
import org.helllabs.android.xmp.compose.ui.player.PlayerButtonsState

@Stable
sealed class PlayerControlsEvent {
    data object OnStop : PlayerControlsEvent()
    data object OnPrev : PlayerControlsEvent()
    data object OnPlay : PlayerControlsEvent()
    data object OnNext : PlayerControlsEvent()
    data object OnRepeat : PlayerControlsEvent()
}

@Composable
fun PlayerControls(
    modifier: Modifier = Modifier,
    onEvent: (PlayerControlsEvent) -> Unit,
    state: PlayerButtonsState
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = { onEvent(PlayerControlsEvent.OnStop) }) {
            Icon(
                modifier = Modifier.scale(1.2f),
                imageVector = Icons.Default.Stop,
                contentDescription = null
            )
        }
        IconButton(onClick = { onEvent(PlayerControlsEvent.OnPrev) }) {
            Icon(
                modifier = Modifier.scale(1.2f),
                imageVector = Icons.Default.SkipPrevious,
                contentDescription = null
            )
        }
        FloatingActionButton(
            onClick = { onEvent(PlayerControlsEvent.OnPlay) },
            contentColor = Color.White
        ) {
            Icon(
                imageVector = if (state.isPlaying) {
                    Icons.Default.Pause
                } else {
                    Icons.Default.PlayArrow
                },
                contentDescription = null
            )
        }
        IconButton(onClick = { onEvent(PlayerControlsEvent.OnNext) }) {
            Icon(
                modifier = Modifier.scale(1.2f),
                imageVector = Icons.Default.SkipNext,
                contentDescription = null
            )
        }
        IconToggleButton(
            checked = state.isRepeating,
            onCheckedChange = { onEvent(PlayerControlsEvent.OnRepeat) }
        ) {
            val repeatMode = remember(state.isRepeating) {
                if (state.isRepeating) Icons.Default.RepeatOneOn else Icons.Default.Repeat
            }
            Icon(
                modifier = Modifier.scale(1.2f),
                imageVector = repeatMode,
                contentDescription = null
            )
        }
    }

    Rebugger(
        composableName = "PlayerControls",
        trackMap = mapOf(
            "modifier" to modifier,
            "onEvent" to onEvent,
            "state" to state,
            "modifier.fillMaxWidth()" to modifier.fillMaxWidth(),
            "Arrangement.SpaceEvenly" to Arrangement.SpaceEvenly,
            "Alignment.CenterVertically" to Alignment.CenterVertically,
        ),
    )
}

@Preview
@Composable
private fun Preview_PlayerButtons() {
    XmpTheme(useDarkTheme = true) {
        PlayerBottomAppBar {
            PlayerControls(
                onEvent = { },
                state = PlayerButtonsState(isPlaying = true, isRepeating = true)
            )
        }
    }
}
