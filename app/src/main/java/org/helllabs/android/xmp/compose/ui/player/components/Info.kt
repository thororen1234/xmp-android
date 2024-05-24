package org.helllabs.android.xmp.compose.ui.player.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.theapache64.rebugger.Rebugger
import org.helllabs.android.xmp.R
import org.helllabs.android.xmp.compose.theme.XmpTheme
import org.helllabs.android.xmp.compose.ui.player.PlayerViewModel

@Composable
fun PlayerInfo(
    state: PlayerViewModel.PlayerInfoState
) {
    val speed = remember(state.infoSpeed) { state.infoSpeed }
    val bpm = remember(state.infoBpm) { state.infoBpm }
    val pos = remember(state.infoPos) { state.infoPos }
    val pat = remember(state.infoPat) { state.infoPat }

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterHorizontally),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Speed
        SingleLineText(text = stringResource(id = R.string.info_speed, speed))
        // BPM
        SingleLineText(text = stringResource(id = R.string.info_bpm, bpm))
        // Pos
        SingleLineText(text = stringResource(id = R.string.info_position, pos))
        // Pat
        SingleLineText(text = stringResource(id = R.string.info_pattern, pat))
    }

    Rebugger(
        composableName = "PlayerInfo",
        trackMap = mapOf(
            "state" to state,
            "speed" to speed,
            "bpm" to bpm,
            "pos" to pos,
            "pat" to pat,
            "Modifier.fillMaxWidth()" to Modifier.fillMaxWidth(),
            "Arrangement.spacedBy(12.dp, Alignment.CenterHorizontally)" to Arrangement.spacedBy(
                12.dp,
                Alignment.CenterHorizontally
            ),
            "Alignment.CenterVertically" to Alignment.CenterVertically,
        ),
    )
}

@Composable
private fun SingleLineText(text: String) {
    Text(
        text = text,
        fontSize = 15.sp,
        maxLines = 1,
        fontWeight = FontWeight.Bold,
        fontFamily = FontFamily.Monospace
    )
}

@Preview
@Composable
private fun PlayerInfoPreview() {
    XmpTheme(useDarkTheme = true) {
        Surface {
            PlayerInfo(
                state = PlayerViewModel.PlayerInfoState(
                    infoSpeed = "000",
                    infoBpm = "000",
                    infoPos = "000",
                    infoPat = "000",
                )
            )
        }
    }
}
