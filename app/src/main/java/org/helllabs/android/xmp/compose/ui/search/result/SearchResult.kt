package org.helllabs.android.xmp.compose.ui.search.result

import android.content.Intent
import android.content.res.Configuration
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.ListItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.core.view.WindowCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dagger.hilt.android.AndroidEntryPoint
import org.helllabs.android.xmp.R
import org.helllabs.android.xmp.compose.components.ErrorScreen
import org.helllabs.android.xmp.compose.components.ProgressbarIndicator
import org.helllabs.android.xmp.compose.components.XmpTopBar
import org.helllabs.android.xmp.compose.theme.XmpTheme
import org.helllabs.android.xmp.compose.ui.search.Search
import org.helllabs.android.xmp.compose.ui.search.SearchError
import org.helllabs.android.xmp.compose.ui.search.SearchType
import org.helllabs.android.xmp.compose.ui.search.components.ItemModule
import org.helllabs.android.xmp.model.Artist
import org.helllabs.android.xmp.model.ArtistInfo
import org.helllabs.android.xmp.model.ArtistResult
import org.helllabs.android.xmp.model.Module
import org.helllabs.android.xmp.model.SearchListResult
import org.helllabs.android.xmp.model.Sponsor
import org.helllabs.android.xmp.model.SponsorDetails
import timber.log.Timber

@AndroidEntryPoint
class SearchResult : ComponentActivity() {

    private val viewModel by viewModels<SearchResultViewModel>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Timber.d("onCreate")

        WindowCompat.setDecorFitsSystemWindows(window, false)

        val searchType = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            intent.getSerializableExtra("search_type", SearchType::class.java)
                ?: throw IllegalArgumentException("Failed to get search type")
        } else {
            @Suppress("DEPRECATION")
            intent.getSerializableExtra("search_type") as SearchType
        }

        val query = intent.getStringExtra(Search.SEARCH_TEXT)
            ?: throw IllegalArgumentException("Failed to get search query")

        when (searchType) {
            SearchType.TYPE_ARTIST -> {
                val title = getString(R.string.search_artist_title)
                viewModel.getArtists(title, query)
            }
            SearchType.TYPE_TITLE_OR_FILENAME -> {
                val title = getString(R.string.search_title_title)
                viewModel.getFileOrTitle(title, query)
            }
        }

        setContent {
            XmpTheme {
                val state by viewModel.uiState.collectAsStateWithLifecycle()

                LaunchedEffect(state.hardError) {
                    if (state.hardError != null) {
                        Timber.w("Hard error has occurred")
                        Intent(this@SearchResult, SearchError::class.java).apply {
                            putExtra(Search.ERROR, state.hardError)
                        }.also(::startActivity)
                    }
                }

                TitleResultScreen(
                    state = state,
                    onBack = {
                        onBackPressedDispatcher.onBackPressed()
                    },
                    onItemId = {
                        Intent(this, Result::class.java).apply {
                            putExtra(Search.MODULE_ID, it)
                        }.also(::startActivity)
                    },
                    onArtistId = viewModel::getArtistById
                )
            }
        }
    }
}

@Composable
private fun TitleResultScreen(
    state: SearchResultViewModel.SearchResultState,
    onBack: () -> Unit,
    onItemId: (id: Int) -> Unit,
    onArtistId: (id: Int) -> Unit
) {
    val listState = rememberLazyListState()
    val isScrolled = remember {
        derivedStateOf {
            listState.firstVisibleItemIndex > 0
        }
    }

    Scaffold(
        topBar = {
            XmpTopBar(
                title = state.title,
                onBack = onBack,
                isScrolled = isScrolled.value
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .padding(paddingValues)
                .fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            ProgressbarIndicator(isLoading = state.isLoading)

            if (!state.softError.isNullOrEmpty()) {
                ErrorScreen(text = state.softError)
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    state = listState,
                    content = {
                        when (val items = state.result) {
                            is SearchListResult -> {
                                items(items.module) { item ->
                                    ItemModule(
                                        item = item,
                                        onClick = { onItemId(item.id) }
                                    )
                                }
                            }
                            is ArtistResult -> {
                                items(items.listItems) { item ->
                                    ListItem(
                                        modifier = Modifier.clickable {
                                            onArtistId(item.id)
                                        },
                                        headlineContent = {
                                            Text(text = item.alias)
                                        }
                                    )
                                }
                            }
                        }
                    }
                )
            }
        }
    }
}

@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES or Configuration.UI_MODE_TYPE_NORMAL)
@Composable
private fun Preview_TitleResult() {
    XmpTheme {
        TitleResultScreen(
            state = SearchResultViewModel.SearchResultState(
                isLoading = true,
                title = stringResource(id = R.string.search_artist_title),
                result = null,
                softError = "Soft Error"
            ),
            onBack = {},
            onItemId = {},
            onArtistId = {}
        )
    }
}

@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES or Configuration.UI_MODE_TYPE_NORMAL)
@Composable
private fun Preview_TitleResult2() {
    XmpTheme {
        TitleResultScreen(
            state = SearchResultViewModel.SearchResultState(
                isLoading = false,
                title = stringResource(id = R.string.search_artist_title),
                result = SearchListResult(
                    sponsor = Sponsor(
                        details = SponsorDetails(
                            link = "",
                            image = "",
                            text = "",
                            imagehtml = ""
                        )
                    ),
                    error = "",
                    results = 1,
                    totalpages = 1,
                    module = List(15) {
                        Module(
                            format = "XM",
                            songtitle = "Some Song Title $it",
                            artistInfo = ArtistInfo(
                                artist = listOf(Artist(alias = "Some Artist"))
                            ),
                            bytes = 669669
                        )
                    }
                )
            ),
            onBack = {},
            onItemId = {},
            onArtistId = {}
        )
    }
}
