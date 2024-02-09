package org.helllabs.android.xmp.compose.ui.player.viewer

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.tooling.preview.Preview
import org.helllabs.android.xmp.compose.theme.XmpTheme

@Composable
internal fun ComposePatternViewer(
    viewInfo: Viewer.Info,
    isMuted: BooleanArray,
    modVars: IntArray
) {
    Text("TODO")
}

@Preview
@Composable
private fun Preview_PatternViewer() {
    val viewInfo = remember {
        composeViewerSampleData()
    }
    val modVars by remember {
        val array = intArrayOf(190968, 30, 25, 12, 40, 18, 1, 0, 0, 0)
        mutableStateOf(array)
    }

    XmpTheme(useDarkTheme = true) {
        XmpCanvas(
            onChangeViewer = {},
            currentViewer = 1,
            viewInfo = viewInfo,
            isMuted = BooleanArray(modVars[3]) { false },
            modVars = modVars,
            insName = Array(modVars[4]) { String.format("%02X %s", it + 1, "Instrument Name") }
        )
    }
}
