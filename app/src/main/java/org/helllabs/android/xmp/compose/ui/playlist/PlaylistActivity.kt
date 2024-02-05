package org.helllabs.android.xmp.compose.ui.playlist

import android.content.res.Configuration
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.surfaceColorAtElevation
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.delay
import org.helllabs.android.xmp.PrefManager
import org.helllabs.android.xmp.R
import org.helllabs.android.xmp.compose.components.BottomBarButtons
import org.helllabs.android.xmp.compose.components.ErrorScreen
import org.helllabs.android.xmp.compose.components.XmpTopBar
import org.helllabs.android.xmp.compose.components.pullrefresh.PullToRefreshContainer
import org.helllabs.android.xmp.compose.components.pullrefresh.rememberPullToRefreshState
import org.helllabs.android.xmp.compose.theme.XmpTheme
import org.helllabs.android.xmp.compose.ui.BasePlaylistActivity
import org.helllabs.android.xmp.compose.ui.playlist.components.DraggableItem
import org.helllabs.android.xmp.compose.ui.playlist.components.PlaylistCardItem
import org.helllabs.android.xmp.compose.ui.playlist.components.PlaylistInfo
import org.helllabs.android.xmp.compose.ui.playlist.components.dragContainer
import org.helllabs.android.xmp.compose.ui.playlist.components.rememberDragDropState
import org.helllabs.android.xmp.model.Playlist
import org.helllabs.android.xmp.model.PlaylistItem
import timber.log.Timber
import java.io.File
import kotlin.time.Duration.Companion.seconds

class PlaylistActivity : BasePlaylistActivity() {

    private val viewModel by viewModels<PlaylistActivityViewModel>()

    override val isShuffleMode: Boolean
        get() = viewModel.uiState.value.isShuffle

    override val isLoopMode: Boolean
        get() = viewModel.uiState.value.isLoop

    override val allFiles: List<String>
        get() = viewModel.getFilenameList()

    override fun update() {
        val extras = intent.extras ?: return
        val name = extras.getString("name").orEmpty()

        viewModel.onRefresh(name)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Timber.d("onCreate")

        setContent {
            val state by viewModel.uiState.collectAsStateWithLifecycle()

            XmpTheme {
                PlaylistScreen(
                    state = state,
                    onBack = { onBackPressedDispatcher.onBackPressed() },
                    onRefresh = ::update,
                    onClick = { index ->
                        onItemClick(
                            viewModel.getItems(),
                            viewModel.getFilenameList(),
                            viewModel.getDirectoryCount(),
                            index
                        )
                    },
                    onMenuClick = { item, index, menuIndex ->
                        when (menuIndex) {
                            0 -> {
                                viewModel.removeItem(index)
                                update()
                            }

                            1 -> addToQueue(item.file!!.path)
                            2 -> addToQueue(viewModel.getFilenameList())
                            3 -> playModule(listOf(item.file!!.path))
                            4 -> playModule(viewModel.getFilenameList(), index)
                        }
                    },
                    onPlayAll = ::onPlayAll,
                    onShuffle = viewModel::setShuffle,
                    onLoop = viewModel::setLoop,
                    onDragEnd = viewModel::saveList
                )
            }
        }
    }

    public override fun onPause() {
        super.onPause()
        viewModel.savePlaylist()
    }
}

@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)
@Composable
private fun PlaylistScreen(
    state: PlaylistActivityViewModel.PlaylistState,
    onBack: () -> Unit,
    onRefresh: () -> Unit,
    onClick: (index: Int) -> Unit,
    onMenuClick: (item: PlaylistItem, index: Int, menuIndex: Int) -> Unit,
    onShuffle: (value: Boolean) -> Unit,
    onLoop: (value: Boolean) -> Unit,
    onPlayAll: () -> Unit,
    onDragEnd: (newList: List<PlaylistItem>) -> Unit
) {
    val snackbarHostState = remember { SnackbarHostState() }
    val scrollState = rememberLazyListState()
    val isScrolled = remember {
        derivedStateOf {
            scrollState.firstVisibleItemIndex > 0
        }
    }

    Scaffold(
        topBar = {
            Column {
                // Headline TopAppBars use two TopAppBars in a Column for the effect.
                // why not mimic it here!
                XmpTopBar(
                    title = stringResource(id = R.string.browser_playlist_title),
                    isScrolled = isScrolled.value,
                    onBack = onBack
                )
                PlaylistInfo(
                    isScrolled = isScrolled.value,
                    playlistName = state.playlist!!.name,
                    playlistComment = state.playlist.comment
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
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) }
    ) { paddingValues ->
        Box(modifier = Modifier.padding(paddingValues)) {
            var refreshing by remember { mutableStateOf(false) }

            val pullRefreshState = rememberPullToRefreshState()
            if (pullRefreshState.isRefreshing) {
                LaunchedEffect(true) {
                    refreshing = true
                    delay(1.seconds)
                    onRefresh()
                    refreshing = false
                    pullRefreshState.endRefresh()
                }
            }

            Box(
                modifier = Modifier.nestedScroll(pullRefreshState.nestedScrollConnection),
                contentAlignment = Alignment.Center
            ) {
                var list by remember(state.playlist!!.list) {
                    mutableStateOf(state.playlist.list.toList())
                }
                val dragDropState = rememberDragDropState(
                    lazyListState = scrollState,
                    onMove = { fromIndex, toIndex ->
                        list = list.toMutableList().apply {
                            add(toIndex, removeAt(fromIndex))
                        }
                    },
                    onDragEnd = { onDragEnd(list) }
                )
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .dragContainer(dragDropState),
                    state = scrollState,
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    itemsIndexed(list, key = { _, item -> item.name }) { index, item ->
                        DraggableItem(dragDropState, index) { isDragging ->
                            val elevation by animateDpAsState(
                                targetValue = if (isDragging) 4.dp else 1.dp,
                                label = "isDragging dp"
                            )
                            val elevationColor by animateColorAsState(
                                targetValue = if (isDragging) {
                                    MaterialTheme.colorScheme.surfaceColorAtElevation(elevation)
                                } else {
                                    MaterialTheme.colorScheme.surfaceVariant
                                },
                                label = "isDragging color"
                            )

                            PlaylistCardItem(
                                elevationColor = elevationColor,
                                elevation = elevation,
                                item = item,
                                onItemClick = { onClick(index) },
                                onMenuClick = { onMenuClick(item, index, it) }
                            )
                        }
                    }
                }

                if (state.playlist?.list.isNullOrEmpty()) {
                    ErrorScreen(
                        text = stringResource(id = R.string.empty_playlist),
                        content = {
                            OutlinedButton(onClick = onBack) {
                                Text(text = stringResource(id = R.string.go_back))
                            }
                        }
                    )
                }

                PullToRefreshContainer(
                    modifier = Modifier.align(Alignment.TopCenter),
                    state = pullRefreshState
                )
            }
        }
    }
}

@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES or Configuration.UI_MODE_TYPE_NORMAL)
@Composable
private fun Preview_PlaylistScreen() {
    val context = LocalContext.current
    PrefManager.init(context, File(""))

    val list = List(15) {
        PlaylistItem(
            type = PlaylistItem.TYPE_PLAYLIST,
            name = "Name $it",
            comment = "Comment $it"
        ).also { item -> item.id = it }
    }

    XmpTheme {
        PlaylistScreen(
            state = PlaylistActivityViewModel.PlaylistState(
                playlist = Playlist(stringResource(id = R.string.empty_playlist)).apply {
                    comment = stringResource(id = R.string.empty_comment)
                    setNewList(list)
                },

                isShuffle = false,
                isLoop = true
            ),
            onRefresh = {},
            onClick = { _ -> },
            onMenuClick = { _, _, _ -> },
            onBack = {},
            onShuffle = {},
            onLoop = {},
            onPlayAll = {},
            onDragEnd = {}
        )
    }
}
