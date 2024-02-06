package org.helllabs.android.xmp.compose.ui.player.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.BottomAppBarDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.contentColorFor
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import org.helllabs.android.xmp.compose.theme.XmpTheme
import org.helllabs.android.xmp.compose.ui.player.PlayerViewModel

/**
 * Height-less [androidx.compose.material3.BottomAppBar]
 */
@Composable
fun PlayerBottomAppBar(
    modifier: Modifier = Modifier,
    containerColor: Color = BottomAppBarDefaults.containerColor,
    contentColor: Color = contentColorFor(containerColor),
    tonalElevation: Dp = BottomAppBarDefaults.ContainerElevation,
    contentPadding: PaddingValues = PaddingValues(vertical = 8.dp),
    windowInsets: WindowInsets = BottomAppBarDefaults.windowInsets,
    content: @Composable ColumnScope.() -> Unit
) {
    Surface(
        color = containerColor,
        contentColor = contentColor,
        tonalElevation = tonalElevation,
        shape = RectangleShape,
        modifier = modifier
    ) {
        Column(
            Modifier
                .fillMaxWidth()
                .windowInsetsPadding(windowInsets)
                .padding(contentPadding),
            horizontalAlignment = Alignment.CenterHorizontally,

            content = content
        )
    }
}

@Preview
@Composable
private fun Preview_PlayerBottomAppBar() {
    val info = PlayerViewModel.PlayerInfoState(
        infoSpeed = "11",
        infoBpm = "22",
        infoPos = "33",
        infoPat = "44"
    )
    val seek = PlayerViewModel.PlayerTimeState(
        timeNow = "00:00",
        timeTotal = "00:00",
        seekPos = 25f,
        seekMax = 100f
    )
    XmpTheme(useDarkTheme = true) {
        PlayerBottomAppBar {
            PlayerInfo(
                speed = info.infoSpeed,
                bpm = info.infoBpm,
                pos = info.infoPos,
                pat = info.infoPat
            )
            Spacer(modifier = Modifier.height(12.dp))
            PlayerSeekBar(
                currentTime = seek.timeNow,
                totalTime = seek.timeTotal,
                position = seek.seekPos,
                range = seek.seekMax,
                isSeeking = false,
                onIsSeeking = { },
                onSeek = { }
            )
            Spacer(modifier = Modifier.height(12.dp))
            PlayerControls(
                onStop = { },
                onPrev = { },
                onPlay = { },
                onNext = { },
                onRepeat = { },
                isPlaying = true,
                isRepeating = true
            )
        }
    }
}
