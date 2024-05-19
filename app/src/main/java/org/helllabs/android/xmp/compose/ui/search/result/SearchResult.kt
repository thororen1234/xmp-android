package org.helllabs.android.xmp.compose.ui.search.result

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.res.*
import androidx.compose.ui.tooling.preview.*
import kotlinx.serialization.Serializable
import org.helllabs.android.xmp.R
import org.helllabs.android.xmp.compose.components.ErrorScreen
import org.helllabs.android.xmp.compose.components.ProgressbarIndicator
import org.helllabs.android.xmp.compose.components.XmpTopBar
import org.helllabs.android.xmp.compose.theme.XmpTheme
import org.helllabs.android.xmp.compose.ui.search.components.ItemModule
import org.helllabs.android.xmp.model.Artist
import org.helllabs.android.xmp.model.ArtistInfo
import org.helllabs.android.xmp.model.ArtistResult
import org.helllabs.android.xmp.model.Module
import org.helllabs.android.xmp.model.SearchListResult
import org.helllabs.android.xmp.model.Sponsor
import org.helllabs.android.xmp.model.SponsorDetails

@Serializable
data class NavSearchTitleResult(val searchQuery: String, val isArtistSearch: Int)

@Composable
fun TitleResultScreen(
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

@Preview
@Composable
private fun Preview_TitleResult() {
    XmpTheme(useDarkTheme = true) {
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

@Preview
@Composable
private fun Preview_TitleResult2() {
    XmpTheme(useDarkTheme = true) {
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
