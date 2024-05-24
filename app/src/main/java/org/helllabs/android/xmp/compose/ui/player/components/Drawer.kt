package org.helllabs.android.xmp.compose.ui.player.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.material.icons.*
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.MaterialTheme.shapes
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.graphics.*
import androidx.compose.ui.platform.*
import androidx.compose.ui.res.*
import androidx.compose.ui.tooling.preview.*
import androidx.compose.ui.unit.*
import com.theapache64.rebugger.Rebugger
import java.util.Locale
import kotlin.random.Random
import org.helllabs.android.xmp.R
import org.helllabs.android.xmp.compose.components.RadioButtonItem
import org.helllabs.android.xmp.compose.theme.XmpTheme
import org.helllabs.android.xmp.compose.ui.player.PlayerViewModel

@Stable
sealed class PlayerDrawerEvent {
    data object OnAllSeq : PlayerDrawerEvent()
    data object OnMenuClose : PlayerDrawerEvent()
    data object OnMessage : PlayerDrawerEvent()
    data class OnSequence(val seq: Int) : PlayerDrawerEvent()
}

@Composable
fun PlayerDrawer(
    modifier: Modifier = Modifier,
    state: PlayerViewModel.PlayerDrawerState,
    onEvent: (PlayerDrawerEvent) -> Unit
) {
    val context = LocalContext.current
    val lazyState = rememberLazyListState()

    val drawerSequences = remember(state.numOfSequences) {
        val main = context.getString(R.string.sidebar_main_song)
        state.numOfSequences.mapIndexed { index, item ->
            val sub = context.getString(R.string.sidebar_sub_song, index)
            val text = if (index == 0) main else sub
            String.format(
                Locale.getDefault(),
                "%2d:%02d (%s)",
                item / 60000,
                item / 1000 % 60,
                text
            )
        }
    }

    ModalDrawerSheet(
        modifier = modifier,
        drawerContentColor = Color.White
    ) {
        Column(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.Start
            ) {
                IconButton(onClick = { onEvent(PlayerDrawerEvent.OnMenuClose) }) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.MenuOpen,
                        contentDescription = null
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            ModuleSection(text = stringResource(id = R.string.sidebar_details)) {
                IconButton(
                    modifier = Modifier
                        .weight(1f)
                        .wrapContentWidth(Alignment.End),
                    onClick = { onEvent(PlayerDrawerEvent.OnMessage) }
                ) {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = null,
                        tint = Color.White
                    )
                }
            }
            Spacer(modifier = Modifier.height(12.dp))

            ModuleInsDetails(stringResource(id = R.string.sidebar_channels), state.moduleInfo[3])

            ModuleInsDetails(stringResource(id = R.string.sidebar_instruments), state.moduleInfo[1])

            ModuleInsDetails(stringResource(id = R.string.sidebar_patterns), state.moduleInfo[0])

            ModuleInsDetails(stringResource(id = R.string.sidebar_samples), state.moduleInfo[2])

            ModuleInsDetails(stringResource(id = R.string.sidebar_length), state.moduleInfo[4])

            Spacer(modifier = Modifier.height(12.dp))

            ModuleSection(text = stringResource(id = R.string.sidebar_subsongs)) {
                Switch(
                    modifier = Modifier
                        .weight(1f)
                        .wrapContentWidth(Alignment.End),
                    checked = state.isPlayAllSequences,
                    onCheckedChange = { onEvent(PlayerDrawerEvent.OnAllSeq) }
                )
            }

            Spacer(Modifier.height(12.dp))

            LazyColumn(state = lazyState) {
                itemsIndexed(drawerSequences) { index, label ->
                    RadioButtonItem(
                        index = index,
                        selection = state.currentSequence,
                        text = label,
                        radioButtonColors = RadioButtonDefaults.colors(
                            unselectedColor = Color.White
                        ),
                        onClick = { onEvent(PlayerDrawerEvent.OnSequence(index)) }
                    )
                }
            }
        }
    }

    Rebugger(
        composableName = "PlayerDrawer",
        trackMap = mapOf(
            "modifier" to modifier,
            "state" to state,
            "onEvent" to onEvent,
            "context" to context,
            "lazyState" to lazyState,
            "drawerSequences" to drawerSequences,
            "Color.White" to Color.White,
        ),
    )
}

@Composable
private fun ModuleSection(
    text: String,
    content: @Composable RowScope.() -> Unit
) {
    Surface(
        modifier = Modifier.height(48.dp),
        shape = shapes.small,
        color = MaterialTheme.colorScheme.surfaceContainerHighest
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                modifier = Modifier
                    .weight(1f)
                    .wrapContentWidth(Alignment.Start),
                text = text,
                color = Color.White
            )

            content()
        }
    }
}

@Composable
private fun ModuleInsDetails(
    string: String,
    number: Int
) {
    Rebugger(
        composableName = "InsDetails",
        trackMap = mapOf(
            "string" to string,
            "number" to number
        )
    )

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 16.dp, end = 20.dp)
    ) {
        Text(
            modifier = Modifier
                .weight(1f)
                .wrapContentWidth(Alignment.Start),
            text = string,
            fontSize = 14.sp,
        )
        Text(
            modifier = Modifier
                .weight(1f)
                .wrapContentWidth(Alignment.End),
            text = number.toString(),
            fontSize = 14.sp,
        )
    }
}

@Preview
@Composable
private fun Preview_PlayerDrawer() {
    XmpTheme(useDarkTheme = true) {
        val drawerState = rememberDrawerState(DrawerValue.Open)
        Scaffold { paddingValues ->
            ModalNavigationDrawer(
                modifier = Modifier.padding(paddingValues),
                drawerState = drawerState,
                drawerContent = {
                    PlayerDrawer(
                        state = PlayerViewModel.PlayerDrawerState(
                            moduleInfo = listOf(111, 222, 333, 444, 555),
                            isPlayAllSequences = true,
                            currentSequence = 1,
                            numOfSequences = List(12) {
                                Random.nextInt(100, 10_000)
                            }
                        ),
                        onEvent = { },
                    )
                },
                content = {
                    Box(modifier = Modifier.fillMaxSize())
                }
            )
        }
    }
}
