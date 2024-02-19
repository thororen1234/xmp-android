package org.helllabs.android.xmp.compose.ui.playlist

import android.content.res.Configuration
import android.net.Uri
import android.os.Bundle
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
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
import androidx.compose.material3.pulltorefresh.PullToRefreshContainer
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.delay
import org.helllabs.android.xmp.R
import org.helllabs.android.xmp.compose.components.BottomBarButtons
import org.helllabs.android.xmp.compose.components.ErrorScreen
import org.helllabs.android.xmp.compose.components.XmpTopBar
import org.helllabs.android.xmp.compose.theme.XmpTheme
import org.helllabs.android.xmp.compose.ui.BasePlaylistActivity
import org.helllabs.android.xmp.compose.ui.playlist.components.DraggableItem
import org.helllabs.android.xmp.compose.ui.playlist.components.PlaylistCardItem
import org.helllabs.android.xmp.compose.ui.playlist.components.PlaylistInfo
import org.helllabs.android.xmp.compose.ui.playlist.components.dragContainer
import org.helllabs.android.xmp.compose.ui.playlist.components.rememberDragDropState
import org.helllabs.android.xmp.core.PlaylistManager
import org.helllabs.android.xmp.core.PrefManager
import org.helllabs.android.xmp.model.DropDownSelection
import org.helllabs.android.xmp.model.Playlist
import org.helllabs.android.xmp.model.PlaylistItem
import timber.log.Timber
import kotlin.time.Duration.Companion.seconds

class PlaylistActivity : BasePlaylistActivity() {

    private val viewModel by viewModels<PlaylistActivityViewModel>()

    override val isShuffleMode: Boolean
        get() = viewModel.uiState.value.isShuffle

    override val isLoopMode: Boolean
        get() = viewModel.uiState.value.isLoop

    override val allFiles: List<Uri>
        get() = viewModel.getUriItems()

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
                    onBack = onBackPressedDispatcher::onBackPressed,
                    onRefresh = ::update,
                    onClick = { index ->
                        onItemClick(
                            viewModel.getUriItems(),
                            viewModel.getUriItems(), // TODO what?
                            0,
                            index
                        )
                    },
                    onMenuClick = { item, index, sel ->
                        when (sel) {
                            DropDownSelection.DELETE -> {
                                viewModel.removeItem(index)
                                // update()
                            }

                            DropDownSelection.FILE_ADD_TO_QUEUE ->
                                addToQueue(item.uri!!)

                            DropDownSelection.FILE_PLAY_HERE ->
                                playModule(viewModel.getUriItems(), index)

                            DropDownSelection.FILE_PLAY_THIS_ONLY ->
                                playModule(listOf(item.uri!!))

                            else -> Unit
                        }
                    },
                    onPlayAll = ::onPlayAll,
                    onShuffle = viewModel::setShuffle,
                    onLoop = viewModel::setLoop,
                    onDragEnd = viewModel::onDragEnd
                )
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)
@Composable
private fun PlaylistScreen(
    state: PlaylistActivityViewModel.PlaylistState,
    onBack: () -> Unit,
    onRefresh: () -> Unit,
    onClick: (index: Int) -> Unit,
    onMenuClick: (item: PlaylistItem, index: Int, sel: DropDownSelection) -> Unit,
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
                    playlistName = state.manager.playlist.name,
                    playlistComment = state.manager.playlist.comment
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
                var list by remember(state.manager.playlist) {
                    mutableStateOf(state.manager.playlist.list.toList())
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

                if (state.manager.playlist.list.isEmpty()) {
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
    PrefManager.init(context)

    XmpTheme {
        PlaylistScreen(
            state = PlaylistActivityViewModel.PlaylistState(
                manager = PlaylistManager().apply {
                    playlist = Playlist(
                        name = stringResource(id = R.string.empty_playlist),
                        comment = stringResource(id = R.string.empty_comment),
                        isShuffle = false,
                        isLoop = true,
                        list = List(15) {
                            PlaylistItem(
                                name = "Name $it",
                                type = "Comment $it"
                            )
                        }
                    )
                }
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
