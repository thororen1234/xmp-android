package org.helllabs.android.xmp.compose.ui.filelist

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.input.nestedscroll.*
import androidx.compose.ui.res.*
import androidx.compose.ui.tooling.preview.*
import androidx.compose.ui.unit.*
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.debounce
import kotlinx.serialization.Serializable
import org.helllabs.android.xmp.R
import org.helllabs.android.xmp.compose.components.BottomBarButtons
import org.helllabs.android.xmp.compose.components.ErrorScreen
import org.helllabs.android.xmp.compose.components.ProgressbarIndicator
import org.helllabs.android.xmp.compose.components.XmpTopBar
import org.helllabs.android.xmp.compose.theme.XmpTheme
import org.helllabs.android.xmp.compose.ui.filelist.components.BreadCrumbs
import org.helllabs.android.xmp.compose.ui.filelist.components.FileListCard
import org.helllabs.android.xmp.model.DropDownSelection
import org.helllabs.android.xmp.model.FileItem

@Serializable
object NavFileList

@OptIn(FlowPreview::class, ExperimentalMaterial3Api::class)
@Composable
fun FileListScreen(
    state: FileListViewModel.FileListState,
    snackBarHostState: SnackbarHostState,
    onBack: () -> Unit,
    onScrollPosition: (Int) -> Unit,
    onRefresh: () -> Unit,
    onRestore: () -> Unit,
    onShuffle: (value: Boolean) -> Unit,
    onLoop: (value: Boolean) -> Unit,
    onPlayAll: () -> Unit,
    onCrumbMenu: (DropDownSelection) -> Unit,
    onCrumbClick: (crumb: FileListViewModel.BreadCrumb, index: Int) -> Unit,
    onItemClick: (item: FileItem, index: Int) -> Unit,
    onItemLongClick: (item: FileItem, index: Int, sel: DropDownSelection) -> Unit
) {
    val scrollState = rememberLazyListState(initialFirstVisibleItemIndex = state.lastScrollPosition)
    val crumbScrollState = rememberLazyListState()
    val isScrolled by remember {
        derivedStateOf {
            scrollState.firstVisibleItemIndex > 0
        }
    }

    // Save our scroll position
    LaunchedEffect(scrollState) {
        snapshotFlow {
            scrollState.firstVisibleItemIndex
        }.debounce(1.seconds).collectLatest {
            onScrollPosition(it)
        }
    }

    // Refresh our list's scroll position if our crumbs changes.
    LaunchedEffect(state.crumbs) {
        if (state.crumbs.isEmpty()) return@LaunchedEffect

        scrollState.animateScrollToItem(state.lastScrollPosition)
        crumbScrollState.animateScrollToItem(state.crumbs.lastIndex)
    }

    Scaffold(
        topBar = {
            Column {
                XmpTopBar(
                    title = stringResource(id = R.string.browser_filelist_title),
                    isScrolled = isScrolled,
                    onBack = onBack
                )
                BreadCrumbs(
                    modifier = Modifier.background(MaterialTheme.colorScheme.background),
                    crumbScrollState = crumbScrollState,
                    crumbs = state.crumbs,
                    onCrumbMenu = onCrumbMenu,
                    onCrumbClick = onCrumbClick
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
        // Outer Box() to properly hide Pull-Refresh
        Box(modifier = Modifier.padding(paddingValues)) {
            val pullRefreshState = rememberPullToRefreshState()
            if (pullRefreshState.isRefreshing) {
                LaunchedEffect(true) {
                    onRefresh()
                    pullRefreshState.endRefresh()
                }
            }

            Box(
                modifier = Modifier.nestedScroll(pullRefreshState.nestedScrollConnection),
                contentAlignment = Alignment.Center
            ) {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    state = scrollState,
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    itemsIndexed(state.list) { index, item ->
                        FileListCard(
                            item = item,
                            onItemClick = { onItemClick(item, index) },
                            onItemLongClick = { onItemLongClick(item, index, it) }
                        )
                    }
                }

                if (state.list.isEmpty() && !state.isLoading) {
                    ErrorScreen(
                        text = stringResource(id = R.string.empty_directory),
                        content = {
                            OutlinedButton(onClick = onRestore) {
                                Text(text = stringResource(id = R.string.go_back))
                            }
                        }
                    )
                }

                ProgressbarIndicator(isLoading = state.isLoading)

                PullToRefreshContainer(
                    modifier = Modifier.align(Alignment.TopCenter),
                    state = pullRefreshState
                )
            }
        }
    }
}

@Preview
@Composable
private fun Preview_FileListScreen() {
    XmpTheme(useDarkTheme = true) {
        FileListScreen(
            state = FileListViewModel.FileListState(
                isLoading = true,
                list = List(10) {
                    FileItem(
                        name = "Name $it",
                        comment = "Comment $it",
                        docFile = null
                    )
                },
                crumbs = List(4) {
                    FileListViewModel.BreadCrumb(
                        name = "Crumb $it",
                        path = null
                    )
                },
                isLoop = true,
                isShuffle = false
            ),
            snackBarHostState = SnackbarHostState(),
            onBack = {},
            onScrollPosition = {},
            onRefresh = {},
            onRestore = {},
            onLoop = {},
            onShuffle = {},
            onPlayAll = {},
            onCrumbClick = { _, _ -> },
            onCrumbMenu = {},
            onItemClick = { _, _ -> },
            onItemLongClick = { _, _, _ -> },
        )
    }
}
