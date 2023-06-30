package org.helllabs.android.xmp.compose.ui.playlist

import android.content.res.Configuration
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material3.BottomAppBar
import androidx.compose.material3.BottomAppBarDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.IconToggleButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.helllabs.android.xmp.PrefManager
import org.helllabs.android.xmp.R
import org.helllabs.android.xmp.browser.playlist.Playlist
import org.helllabs.android.xmp.browser.playlist.PlaylistItem
import org.helllabs.android.xmp.compose.components.XmpTopBar
import org.helllabs.android.xmp.compose.components.pullrefresh.ExperimentalMaterialApi
import org.helllabs.android.xmp.compose.components.pullrefresh.PullRefreshIndicator
import org.helllabs.android.xmp.compose.components.pullrefresh.pullRefresh
import org.helllabs.android.xmp.compose.components.pullrefresh.rememberPullRefreshState
import org.helllabs.android.xmp.compose.theme.XmpTheme
import org.helllabs.android.xmp.compose.ui.playlist.components.DraggableItem
import org.helllabs.android.xmp.compose.ui.playlist.components.dragContainer
import org.helllabs.android.xmp.compose.ui.playlist.components.rememberDragDropState
import timber.log.Timber
import java.io.IOException
import kotlin.time.Duration.Companion.seconds

class PlaylistActivityViewModel : ViewModel() {
    data class PlaylistState(
        val playlist: Playlist? = null,
        val playlistName: String = "",
        val playlistComment: String = "",
        val playlistItems: MutableList<PlaylistItem> = mutableListOf(),
        val useFileName: Boolean = false,
        val isLoop: Boolean = false,
        val isShuffle: Boolean = false
    )

    private val _uiState = MutableStateFlow(PlaylistState())
    val uiState = _uiState.asStateFlow()

    fun setPlaylistInfo(playlist: Playlist) {
        // PlaylistUtils.renumberIds(playlist.list)
        _uiState.update {
            it.copy(
                playlist = playlist,
                playlistName = playlist.name,
                playlistComment = playlist.comment,
                playlistItems = playlist.list,
                isLoop = playlist.isLoopMode,
                isShuffle = playlist.isShuffleMode
            )
        }
    }

    fun setShuffle(value: Boolean) {
        _uiState.value.playlist?.isShuffleMode = value
        _uiState.update { it.copy(isShuffle = value) }
        savePlaylist()
    }

    fun setLoop(value: Boolean) {
        _uiState.value.playlist?.isLoopMode = value
        _uiState.update { it.copy(isLoop = value) }
        savePlaylist()
    }

    fun savePlaylist() {
        uiState.value.playlist?.commit()
    }

    fun useFileName(value: Boolean) {
        _uiState.update {
            it.copy(useFileName = value)
        }
    }

    fun moveList(fromIndex: Int, toIndex: Int) {
        _uiState.update {
            it.copy(
                playlistItems = _uiState.value.playlistItems.apply {
                    add(toIndex, removeAt(fromIndex))
                }
            )
        }
    }

    fun saveList() {
        // TODO I broke something here when saving a re-ordered list
        _uiState.value.playlist?.apply {
            setList(_uiState.value.playlistItems)
            setListChanged(true)
            commit()
        }
    }
}

class PlaylistActivity : ComponentActivity() {

    private val viewModel by viewModels<PlaylistActivityViewModel>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Timber.d("onCreate")

        WindowCompat.setDecorFitsSystemWindows(window, false)

        val extras = intent.extras ?: return
        val name = extras.getString("name").orEmpty()
        try {
            val playlist = Playlist(name)
            viewModel.setPlaylistInfo(playlist)
        } catch (e: IOException) {
            Timber.e("Can't read playlist $name")
        }

        viewModel.useFileName(PrefManager.useFileName)

        setContent {
            val state by viewModel.uiState.collectAsStateWithLifecycle()

            XmpTheme {
                PlaylistScreen(
                    state = state,
                    onBack = { onBackPressedDispatcher.onBackPressed() },
                    onRefresh = {
                        // TODO
                    },
                    onPlayAll = {
                        // TODO
                    },
                    onShuffle = {
                        viewModel.setShuffle(it)
                    },
                    onLoop = {
                        viewModel.setLoop(it)
                    },
                    onDragMove = { fromIndex, toIndex ->
                        Timber.d("From: $fromIndex, To: $toIndex")
                        viewModel.moveList(fromIndex, toIndex)
                    },
                    onDragMoveFinish = {
                        Timber.d("Finished!!")
                        viewModel.saveList()
                    }
                )
            }
        }
    }

    override fun onResume() {
        super.onResume()
        viewModel.useFileName(PrefManager.useFileName)
    }

    public override fun onPause() {
        super.onPause()
        viewModel.savePlaylist()
    }

    // Playlist context menu
//    override fun onCreateContextMenu(menu: ContextMenu?, v: View?, menuInfo: ContextMenuInfo?) {
//        super.onCreateContextMenu(menu, v, menuInfo)
//
//        if (menu == null) {
//            return
//        }
//
//        val mode = PrefManager.playlistMode
//        menu.setHeaderTitle("Edit playlist")
//        menu.add(Menu.NONE, 0, 0, "Remove from playlist")
//        menu.add(Menu.NONE, 1, 1, "Add to play queue")
//        menu.add(Menu.NONE, 2, 2, "Add all to play queue")
//        if (mode != 2) {
//            menu.add(Menu.NONE, 3, 3, "Play this module")
//        }
//        if (mode != 1) {
//            menu.add(Menu.NONE, 4, 4, "Play all starting here")
//        }
//    }
//
//    override fun onContextItemSelected(item: MenuItem): Boolean {
//        val itemId = item.itemId
//        val position = mPlaylistAdapter.position
//        when (itemId) {
//            0 -> {
//                mPlaylist!!.remove(position)
//                mPlaylist!!.commit()
//                update()
//            }
//            1 -> addToQueue(mPlaylistAdapter.getFilename(position))
//            2 -> addToQueue(mPlaylistAdapter.filenameList)
//            3 -> playModule(mPlaylistAdapter.getFilename(position))
//            4 -> playModule(mPlaylistAdapter.filenameList, position)
//        }
//        return true
//    }
}

@OptIn(ExperimentalMaterialApi::class, ExperimentalFoundationApi::class)
@Composable
private fun PlaylistScreen(
    state: PlaylistActivityViewModel.PlaylistState,
    onBack: () -> Unit,
    onRefresh: () -> Unit,
    onShuffle: (value: Boolean) -> Unit,
    onLoop: (value: Boolean) -> Unit,
    onPlayAll: () -> Unit,
    onDragMove: (fromIndex: Int, toIndex: Int) -> Unit,
    onDragMoveFinish: () -> Unit
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
            XmpTopBar(
                title = stringResource(id = R.string.browser_playlist_title),
                isScrolled = isScrolled.value,
                onBack = onBack
            )
        },
        bottomBar = {
            BottomAppBar(
                actions = {
                    IconToggleButton(
                        checked = state.isShuffle,
                        onCheckedChange = { onShuffle(it) },
                        colors = IconButtonDefaults.iconToggleButtonColors(
                            checkedContentColor = Color.Green
                        ),
                        content = {
                            Icon(Icons.Filled.Shuffle, contentDescription = null)
                        }
                    )
                    IconToggleButton(
                        checked = state.isLoop,
                        onCheckedChange = { onLoop(it) },
                        colors = IconButtonDefaults.iconToggleButtonColors(
                            checkedContentColor = Color.Green
                        ),
                        content = {
                            Icon(Icons.Filled.Repeat, contentDescription = null)
                        }
                    )
                },
                floatingActionButton = {
                    FloatingActionButton(
                        onClick = onPlayAll,
                        containerColor = BottomAppBarDefaults.bottomAppBarFabColor,
                        elevation = FloatingActionButtonDefaults.bottomAppBarFabElevation(),
                        content = {
                            Icon(imageVector = Icons.Filled.PlayArrow, contentDescription = null)
                        }
                    )
                }
            )
        },
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .padding(paddingValues)
                .fillMaxSize()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        if (isScrolled.value) {
                            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = .5f)
                        } else {
                            MaterialTheme.colorScheme.surface
                        }
                    )
                    .align(Alignment.TopStart)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    Text(text = state.playlistName)
                    Text(
                        text = state.playlistComment,
                        fontSize = 12.sp,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        lineHeight = 16.sp
                    )
                }
                Divider(modifier = Modifier.fillMaxWidth())

                val scope = rememberCoroutineScope()
                var refreshing by remember { mutableStateOf(false) }
                fun refresh() = scope.launch {
                    refreshing = true
                    delay(1.seconds)
                    onRefresh()
                    refreshing = false
                }

                val pullState = rememberPullRefreshState(refreshing, ::refresh)
                Box(modifier = Modifier.pullRefresh(pullState)) {
                    val listState = rememberLazyListState()
                    val dragDropState = rememberDragDropState(
                        lazyListState = listState,
                        onMove = { fromIndex, toIndex ->
                            onDragMove(fromIndex, toIndex)
                        },
                        onStop = onDragMoveFinish
                    )

                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .dragContainer(dragDropState),
                        state = listState,
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        itemsIndexed(
                            items = state.playlistItems,
                            key = { _, item -> item.name }
                        ) { index, item ->
                            DraggableItem(dragDropState, index) { isDragging ->
                                val elevation by animateDpAsState(
                                    targetValue = if (isDragging) 4.dp else 1.dp,
                                    label = "drag-and-drop"
                                )
                                Card(
                                    elevation = CardDefaults.cardElevation(
                                        draggedElevation = elevation
                                    )
                                ) {
                                    Text(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(20.dp),
                                        text = "Item ${item.name}"
                                    )
                                }
                            }
                        }
                    }

                    PullRefreshIndicator(
                        refreshing,
                        pullState,
                        Modifier.align(Alignment.TopCenter)
                    )
                }
            }
        }
    }
}

@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES or Configuration.UI_MODE_TYPE_NORMAL)
@Composable
private fun Preview_PlaylistScreen() {
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
                playlistItems = list.toMutableList(),
                playlistName = stringResource(id = R.string.empty_playlist),
                playlistComment = stringResource(id = R.string.empty_comment),
                isShuffle = false,
                isLoop = true
            ),
            onRefresh = {},
            onBack = {},
            onShuffle = {},
            onLoop = {},
            onPlayAll = {},
            onDragMove = { _, _ -> },
            onDragMoveFinish = {}
        )
    }
}
