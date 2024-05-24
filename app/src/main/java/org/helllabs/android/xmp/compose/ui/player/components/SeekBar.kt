package org.helllabs.android.xmp.compose.ui.player.components

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.text.font.*
import androidx.compose.ui.tooling.preview.*
import androidx.compose.ui.unit.*
import com.theapache64.rebugger.Rebugger
import org.helllabs.android.xmp.compose.theme.XmpTheme
import org.helllabs.android.xmp.compose.ui.player.PlayerViewModel

@Composable
fun PlayerSeekBar(
    state: PlayerViewModel.PlayerTimeState,
    onIsSeeking: (Boolean) -> Unit,
    onSeek: (Float) -> Unit
) {
    val newPosition = remember { mutableFloatStateOf(0F) }

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            modifier = Modifier.wrapContentWidth(Alignment.Start),
            text = state.timeNow,
            fontWeight = FontWeight.Bold
        )

        Slider(
            modifier = Modifier
                .fillMaxWidth(.8f)
                .height(20.dp),
            value = if (state.isSeeking) newPosition.floatValue else state.seekPos,
            valueRange = 0f..state.seekMax,
            onValueChange = {
                onIsSeeking(true)
                newPosition.floatValue = it
            },
            onValueChangeFinished = {
                onSeek(newPosition.floatValue)
                onIsSeeking(false)
            }
        )

        Text(
            modifier = Modifier.wrapContentWidth(Alignment.End),
            text = state.timeTotal,
            fontWeight = FontWeight.Bold
        )
    }

    Rebugger(
        composableName = "PlayerSeek",
        trackMap = mapOf(
            "state" to state,
            "onIsSeeking" to onIsSeeking,
            "onSeek" to onSeek,
            "newPosition" to newPosition,
            "Modifier.fillMaxWidth()" to Modifier.fillMaxWidth(),
            "Arrangement.SpaceEvenly" to Arrangement.SpaceEvenly,
            "Alignment.CenterVertically" to Alignment.CenterVertically,
        ),
    )
}

@Preview
@Composable
private fun Preview_PlayerSeekBar() {
    XmpTheme(useDarkTheme = true) {
        Surface {
            PlayerSeekBar(
                state = PlayerViewModel.PlayerTimeState(
                    timeNow = "00:40",
                    timeTotal = "66:90",
                    seekPos = 6f,
                    seekMax = 100f,
                    isSeeking = false,
                ),
                onIsSeeking = {},
                onSeek = {}
            )
        }
    }
}
