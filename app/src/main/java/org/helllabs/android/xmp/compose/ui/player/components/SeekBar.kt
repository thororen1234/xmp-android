package org.helllabs.android.xmp.compose.ui.player.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import org.helllabs.android.xmp.compose.theme.XmpTheme

@Composable
fun PlayerSeekBar(
    currentTime: String,
    totalTime: String,
    position: Float,
    range: Float,
    isSeeking: Boolean,
    onIsSeeking: (Boolean) -> Unit,
    onSeek: (Float) -> Unit
) {
    var newPosition by remember { mutableFloatStateOf(0F) }

    // https://issuetracker.google.com/issues/32226995
    // TODO: https://issuetracker.google.com/issues/322269951#comment8
    val onValueChangeFinished: () -> Unit = remember {
        {
            onIsSeeking(false)
            onSeek(newPosition)
        }
    }

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            modifier = Modifier.wrapContentWidth(Alignment.Start),
            text = currentTime,
            fontWeight = FontWeight.Bold
        )

        Slider(
            modifier = Modifier
                .fillMaxWidth(.8f)
                .height(20.dp),
            value = if (isSeeking) newPosition else position,
            valueRange = 0f..range,
            onValueChange = {
                onIsSeeking(true)
                newPosition = it
            },
            onValueChangeFinished = onValueChangeFinished
        )

        Text(
            modifier = Modifier.wrapContentWidth(Alignment.End),
            text = totalTime,
            fontWeight = FontWeight.Bold
        )
    }
}

@Preview
@Composable
private fun Preview_PlayerSeekBar() {
    XmpTheme(useDarkTheme = true) {
        Surface {
            PlayerSeekBar(
                currentTime = "00:00",
                totalTime = "00:00",
                position = 6f,
                range = 100f,
                isSeeking = false,
                onIsSeeking = {},
                onSeek = {}
            )
        }
    }
}
