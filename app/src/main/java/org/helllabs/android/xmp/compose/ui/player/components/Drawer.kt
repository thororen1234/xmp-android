package org.helllabs.android.xmp.compose.ui.player.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.MenuOpen
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme.shapes
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.*
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.theapache64.rebugger.Rebugger
import java.util.Locale
import kotlin.random.Random
import org.helllabs.android.xmp.R
import org.helllabs.android.xmp.compose.components.RadioButtonItem
import org.helllabs.android.xmp.compose.theme.XmpTheme

@Composable
fun PlayerDrawer(
    modifier: Modifier = Modifier,
    currentSequence: Int,
    moduleInfo: List<Int>,
    onAllSeq: (bool: Boolean) -> Unit,
    onMenuClose: () -> Unit,
    onMessage: () -> Unit,
    onSequence: (int: Int) -> Unit,
    playAllSeq: Boolean,
    sequences: List<Int>
) {
    val context = LocalContext.current
    val state = rememberLazyListState()

    val drawerCurrentSequence by remember(currentSequence) {
        mutableIntStateOf(currentSequence)
    }
    val drawerModInfo by remember(moduleInfo) {
        mutableStateOf(moduleInfo)
    }
    val drawerPlayAllSeq by remember(playAllSeq) {
        mutableStateOf(playAllSeq)
    }
    val drawerSequences by remember(sequences) {
        val main = context.getString(R.string.sidebar_main_song)
        val list = sequences.mapIndexed { index, item ->
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

        mutableStateOf(list)
    }

    Rebugger(
        composableName = "Drawer",
        trackMap = mapOf(
            "context" to context,
            "state" to state,
            "drawerCurrentSequence" to drawerCurrentSequence,
            "drawerModInfo" to drawerModInfo,
            "drawerPlayAllSeq" to drawerPlayAllSeq,
            "drawerSequences" to drawerSequences,
            "onAllSeq" to onAllSeq,
            "onMenuClose" to onMenuClose,
            "onMessage" to onMessage,
            "onSequence" to onSequence,
        )
    )

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
                IconButton(onClick = onMenuClose) {
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
                    onClick = onMessage
                ) {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = null,
                        tint = Color.White
                    )
                }
            }
            Spacer(modifier = Modifier.height(12.dp))

            ModuleInsDetails(stringResource(id = R.string.sidebar_channels), drawerModInfo[3])

            ModuleInsDetails(stringResource(id = R.string.sidebar_instruments), drawerModInfo[1])

            ModuleInsDetails(stringResource(id = R.string.sidebar_patterns), drawerModInfo[0])

            ModuleInsDetails(stringResource(id = R.string.sidebar_samples), drawerModInfo[2])

            ModuleInsDetails(stringResource(id = R.string.sidebar_length), drawerModInfo[4])

            Spacer(modifier = Modifier.height(12.dp))

            ModuleSection(text = stringResource(id = R.string.sidebar_subsongs)) {
                Switch(
                    modifier = Modifier
                        .weight(1f)
                        .wrapContentWidth(Alignment.End),
                    checked = drawerPlayAllSeq,
                    onCheckedChange = onAllSeq
                )
            }

            Spacer(Modifier.height(12.dp))

            LazyColumn(state = state) {
                itemsIndexed(drawerSequences) { index, label ->
                    Rebugger(
                        composableName = "LazyColumn",
                        trackMap = mapOf(
                            "list" to this@LazyColumn,
                            "seq" to drawerSequences,
                            "sheet" to this@ModalDrawerSheet,
                            "col" to this@Column,
                            "drawerCurrentSequence2" to drawerCurrentSequence
                        )
                    )

                    RadioButtonItem(
                        index = index,
                        selection = drawerCurrentSequence,
                        text = label,
                        radioButtonColors = RadioButtonDefaults.colors(
                            unselectedColor = Color.White
                        ),
                        onClick = { onSequence(index) }
                    )
                }
            }
        }
    }
}

@Composable
private fun ModuleSection(
    text: String,
    content: @Composable RowScope.() -> Unit
) {
    Surface(
        modifier = Modifier.height(48.dp),
        shape = shapes.small,
        // color = sectionBackground
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
                        onMenuClose = {},
                        onMessage = {},
                        moduleInfo = listOf(111, 222, 333, 444, 555),
                        playAllSeq = true,
                        onAllSeq = {},
                        sequences = List(12) {
                            Random.nextInt(100, 10_000)
                        },
                        currentSequence = 1,
                        onSequence = {}
                    )
                },
                content = {
                    Box(modifier = Modifier.fillMaxSize())
                }
            )
        }
    }
}
