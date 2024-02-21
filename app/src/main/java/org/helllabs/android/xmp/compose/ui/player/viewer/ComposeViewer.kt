package org.helllabs.android.xmp.compose.ui.player.viewer

import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

@Composable
internal fun XmpCanvas(
    modifier: Modifier = Modifier,
    serviceConnected: Boolean,
    onChangeViewer: () -> Unit,
    currentViewer: Int,
    viewInfo: ViewerInfo,
    patternInfo: PatternInfo = PatternInfo(),
    isMuted: BooleanArray,
    modVars: IntArray,
    insName: Array<String>
) {
    Box(modifier = modifier) {
        when (currentViewer) {
            0 -> InstrumentViewer(onChangeViewer, serviceConnected, viewInfo, isMuted, modVars, insName)
            1 -> ComposePatternViewer(onChangeViewer, viewInfo, patternInfo, isMuted, modVars)
            2 -> ComposeChannelViewer(onChangeViewer, viewInfo, isMuted, modVars, insName)
        }
    }
}
