package org.helllabs.android.xmp.compose.ui.player.viewer

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.tooling.preview.Preview
import org.helllabs.android.xmp.compose.theme.XmpTheme

@Composable
internal fun ComposePatternViewer(
    viewInfo: Viewer.Info,
    isMuted: BooleanArray,
    modVars: IntArray,
    ) {
    Text("TODO")
}

@Preview
@Composable
private fun Preview_PatternViewer() {
    val info = remember {
        Viewer.Info().apply {
            time = 109
            values = intArrayOf(16, 12, 8, 64, 0, 7, 134)
            volumes = intArrayOf(
                64, 17, 32, 48, 64, 19, 53, 15, 0, 7, 39, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
                0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
                0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0
            )
            finalVols = intArrayOf(
                64, 16, 32, 48, 64, 19, 3, 15, 0, 26, 16, 22, 0, 0, 0, 0, 0, 0,
                0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
                0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0
            )
            pans = intArrayOf(
                128, 128, 135, 128, 112, 112, 128, 160, 208, 148, 200, 128, 0, 0, 0,
                0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
                0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0
            )
            instruments = intArrayOf(
                1, 1, 3, 14, 10, 12, 17, 11, 18, 20, 20, 15, 0, 0, 0, 0, 0, 0,
                0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
                0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0
            )
            keys = intArrayOf(
                72, 69, 67, 72, 72, 72, 77, 77, -1, -1, 74, -1, 0, 0, 0, 0, 0, 0, 0,
                0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
                0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0
            )
            periods = intArrayOf(
                3424, 4071, 4570, 1298, 1227, 3424, 1225, 2565, 6848, 762, 762,
                3424, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
                0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0
            )
            type = "FastTracker v2.00 XM 1.04"
        }
    }

    val viewInfo by remember {
        mutableStateOf(info)
    }
    val currentViewer by remember {
        mutableIntStateOf(1)
    }
    val modVars by remember {
        mutableStateOf(intArrayOf(190968, 30, 25, 12, 24, 18, 1, 0, 0, 0))
    }

    XmpTheme(useDarkTheme = true) {
        XmpCanvas(
            onSizeChanged = { _, _ -> },
            onChangeViewer = {},
            currentViewer = currentViewer,
            viewInfo = viewInfo,
            isMuted = BooleanArray(modVars[3]) { false },
            modVars = modVars,
            insName = Array(modVars[4]) { String.format("%02X %s", it + 1, "Instrument Name") },
        )
    }
}
