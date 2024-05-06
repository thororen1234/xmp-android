package org.helllabs.android.xmp.compose.ui.playlist

import android.content.res.Configuration
import android.net.Uri
import android.os.Bundle
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.helllabs.android.xmp.R
import org.helllabs.android.xmp.compose.components.BottomBarButtons
import org.helllabs.android.xmp.compose.components.ErrorScreen
import org.helllabs.android.xmp.compose.components.XmpTopBar
import org.helllabs.android.xmp.compose.theme.XmpTheme
import org.helllabs.android.xmp.compose.ui.BasePlaylistActivity
import org.helllabs.android.xmp.compose.ui.playlist.components.PlaylistCardItem
import org.helllabs.android.xmp.compose.ui.playlist.components.PlaylistInfo
import org.helllabs.android.xmp.core.PrefManager
import org.helllabs.android.xmp.model.DropDownSelection
import org.helllabs.android.xmp.model.Playlist
import org.helllabs.android.xmp.model.PlaylistItem
import sh.calvin.reorderable.ReorderableItem
import sh.calvin.reorderable.rememberReorderableLazyListState
import sh.calvin.reorderable.rememberScroller
import timber.log.Timber

class PlaylistActivity : BasePlaylistActivity() {

    private val viewModel by viewModels<PlaylistActivityViewModel>()

    override val isShuffleMode: Boolean
        get() = viewModel.uiState.value.isShuffle

    override val isLoopMode: Boolean
        get() = viewModel.uiState.value.isLoop

    override suspend fun allFiles(): List<Uri> = viewModel.getUriItems()

    override fun update() {
        val extras = intent.extras ?: return
        val name = extras.getString("name").orEmpty()

        viewModel.onRefresh(name)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        enableEdgeToEdge(
            navigationBarStyle = SystemBarStyle.auto(
                Color.Transparent.toArgb(),
                Color.Transparent.toArgb()
            )
        )

        Timber.d("onCreate")
        setContent {
            val state by viewModel.uiState.collectAsStateWithLifecycle()

            XmpTheme {
                PlaylistScreen(
                    state = state,
                    snackBarHostState = snackBarHostState,
                    onBack = onBackPressedDispatcher::onBackPressed,
                    onItemClick = { index ->
                        onItemClick(
                            viewModel.getUriItems(),
                            index
                        )
                    },
                    onMenuClick = { item, index, sel ->
                        when (sel) {
                            DropDownSelection.DELETE -> {
                                viewModel.removeItem(index)
                                update()
                            }

                            DropDownSelection.ADD_TO_QUEUE ->
                                addToQueue(item.uri)

                            DropDownSelection.FILE_PLAY_HERE ->
                                playModule(viewModel.getUriItems(), index)

                            DropDownSelection.FILE_PLAY_THIS_ONLY ->
                                playModule(item.uri)

                            else -> Unit
                        }
                    },
                    onPlayAll = ::onPlayAll,
                    onShuffle = viewModel::setShuffle,
                    onLoop = viewModel::setLoop,
                    onMove = viewModel::onMove,
                    onDragStopped = viewModel::onDragStopped
                )
            }
        }
    }

    override fun onPause() {
        super.onPause()
        Timber.d("onPause")
        viewModel.save()
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun PlaylistScreen(
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
                    lazyListState = listState, scroller = rememberScroller(
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
    val context = LocalContext.current
    PrefManager.init(context)

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
