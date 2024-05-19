package org.helllabs.android.xmp.compose.ui.playlist

import android.content.res.Configuration
import android.net.Uri
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.*
import androidx.compose.ui.hapticfeedback.*
import androidx.compose.ui.platform.*
import androidx.compose.ui.res.*
import androidx.compose.ui.tooling.preview.*
import androidx.compose.ui.unit.*
import kotlinx.serialization.Serializable
import org.helllabs.android.xmp.R
import org.helllabs.android.xmp.compose.components.BottomBarButtons
import org.helllabs.android.xmp.compose.components.ErrorScreen
import org.helllabs.android.xmp.compose.components.XmpTopBar
import org.helllabs.android.xmp.compose.theme.XmpTheme
import org.helllabs.android.xmp.compose.ui.playlist.components.PlaylistCardItem
import org.helllabs.android.xmp.compose.ui.playlist.components.PlaylistInfo
import org.helllabs.android.xmp.model.DropDownSelection
import org.helllabs.android.xmp.model.Playlist
import org.helllabs.android.xmp.model.PlaylistItem
import sh.calvin.reorderable.ReorderableItem
import sh.calvin.reorderable.rememberReorderableLazyListState
import sh.calvin.reorderable.rememberScroller

@Serializable
data class NavPlaylist(val playlist: String)

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun PlaylistScreen(
    state: Playlist,
    snackBarHostState: SnackbarHostState,
    onBack: () -> Unit,
    onItemClick: (index: Int) -> Unit,
    onMenuClick: (item: PlaylistItem, index: Int, sel: DropDownSelection) -> Unit,
    onShuffle: (value: Boolean) -> Unit,
    onLoop: (value: Boolean) -> Unit,
    onPlayAll: () -> Unit,
    onMove: (Int, Int) -> Unit,
    onDragStopped: () -> Unit
) {
    val listState = rememberLazyListState()
    val isScrolled = remember {
        derivedStateOf {
            listState.firstVisibleItemIndex > 0
        }
    }

    Scaffold(
        topBar = {
            Column {
                XmpTopBar(
                    title = stringResource(id = R.string.browser_playlist_title),
                    isScrolled = isScrolled.value,
                    onBack = onBack
                )
                PlaylistInfo(
                    isScrolled = isScrolled.value,
                    playlistName = state.name,
                    playlistComment = state.comment
                )
            }
        },
        bottomBar = {
            BottomBarButtons(
                isShuffle = state.isShuffle,
                isLoop = state.isLoop,
                onShuffle = onShuffle,
                onLoop = onLoop,
                onPlayAll = onPlayAll
            )
        },
        snackbarHost = { SnackbarHost(hostState = snackBarHostState) }
    ) { paddingValues ->
        Box(
            modifier = Modifier.padding(paddingValues),
            contentAlignment = Alignment.Center
        ) {
            val pixelAmount by remember {
                derivedStateOf { listState.layoutInfo.viewportSize.height * 0.05f }
            }
            val haptic = LocalHapticFeedback.current
            val reorderState =
                rememberReorderableLazyListState(
                    lazyListState = listState,
                    scroller = rememberScroller(
                        scrollableState = listState,
                        pixelAmount = pixelAmount,
                    )
                ) { from, to ->
                    onMove(from.index - 1, to.index - 1)
                    haptic.performHapticFeedback(HapticFeedbackType(26))
                }

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                state = listState,
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Dummy item. Compose (and libs) cannot handle 1st item animations
                // https://github.com/Calvin-LL/Reorderable/issues/4
                item {
                    ReorderableItem(
                        state = reorderState,
                        key = "dummy",
                        enabled = false,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(.01.dp)
                    ) {}
                }
                itemsIndexed(state.list, key = { _, item -> item.id }) { index, item ->
                    ReorderableItem(
                        state = reorderState,
                        key = item.id
                    ) { isDragging ->
                        val elevation by animateDpAsState(
                            if (isDragging) 4.dp else 0.dp,
                            label = ""
                        )
                        PlaylistCardItem(
                            scope = this,
                            elevation = elevation,
                            item = item,
                            onItemClick = { onItemClick(index) },
                            onMenuClick = { onMenuClick(item, index, it) },
                            onDragStopped = onDragStopped
                        )
                    }
                }
            }

            if (state.list.isEmpty()) {
                ErrorScreen(
                    text = stringResource(id = R.string.empty_playlist),
                    content = {
                        OutlinedButton(onClick = onBack) {
                            Text(text = stringResource(id = R.string.go_back))
                        }
                    }
                )
            }
        }
    }
}

@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES or Configuration.UI_MODE_TYPE_NORMAL)
@Composable
private fun Preview_PlaylistScreen() {
    XmpTheme {
        PlaylistScreen(
            state = Playlist(
                comment = stringResource(id = R.string.empty_comment),
                isLoop = true,
                isShuffle = false,
                name = stringResource(id = R.string.empty_playlist),
                list = List(15) {
                    PlaylistItem(
                        name = "Name $it",
                        type = "Comment $it",
                        uri = Uri.EMPTY
                    ).also { item ->
                        item.id = it
                    }
                }
            ),
            snackBarHostState = SnackbarHostState(),
            onItemClick = { _ -> },
            onMenuClick = { _, _, _ -> },
            onBack = {},
            onShuffle = {},
            onLoop = {},
            onPlayAll = {},
            onMove = { _, _ -> },
            onDragStopped = { }
        )
    }
}
