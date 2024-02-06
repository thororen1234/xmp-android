package org.helllabs.android.xmp.compose.ui.player.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.RepeatOneOn
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.IconToggleButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import org.helllabs.android.xmp.compose.theme.XmpTheme
import org.helllabs.android.xmp.compose.theme.accent

@Composable
fun PlayerControls(
    modifier: Modifier = Modifier,
    onStop: () -> Unit,
    onPrev: () -> Unit,
    onPlay: () -> Unit,
    onNext: () -> Unit,
    onRepeat: (Boolean) -> Unit,
    isPlaying: Boolean,
    isRepeating: Boolean
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = onStop) {
            Icon(
                modifier = Modifier.scale(1.2f),
                imageVector = Icons.Default.Stop,
                contentDescription = null
            )
        }
        IconButton(onClick = onPrev) {
            Icon(
                modifier = Modifier.scale(1.2f),
                imageVector = Icons.Default.SkipPrevious,
                contentDescription = null
            )
        }
        FloatingActionButton(
            onClick = onPlay,
            containerColor = accent,
            contentColor = Color.White
        ) {
            Icon(
                imageVector = if (isPlaying) {
                    Icons.Default.Pause
                } else {
                    Icons.Default.PlayArrow
                },
                contentDescription = null
            )
        }
        IconButton(onClick = onNext) {
            Icon(
                modifier = Modifier.scale(1.2f),
                imageVector = Icons.Default.SkipNext,
                contentDescription = null
            )
        }
        IconToggleButton(
            colors = IconButtonDefaults.iconToggleButtonColors(
                checkedContentColor = accent
            ),
            checked = isRepeating,
            onCheckedChange = onRepeat
        ) {
            val repeat = if (isRepeating) Icons.Default.RepeatOneOn else Icons.Default.Repeat
            Icon(
                modifier = Modifier.scale(1.2f),
                imageVector = repeat,
                contentDescription = null
            )
        }
    }
}

@Preview
@Composable
private fun Preview_PlayerButtons() {
    XmpTheme(useDarkTheme = true) {
        PlayerBottomAppBar {
            PlayerControls(
                onStop = {},
                onPrev = {},
                onPlay = {},
                onNext = {},
                onRepeat = {},
                isPlaying = false,
                isRepeating = true
            )
        }
    }
}
