package org.helllabs.android.xmp.compose.ui.search

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.*
import androidx.compose.material.icons.*
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.*
import androidx.compose.ui.*
import androidx.compose.ui.focus.*
import androidx.compose.ui.platform.*
import androidx.compose.ui.res.*
import androidx.compose.ui.text.*
import androidx.compose.ui.text.input.*
import androidx.compose.ui.tooling.preview.*
import androidx.compose.ui.unit.*
import kotlinx.serialization.Serializable
import org.helllabs.android.xmp.R
import org.helllabs.android.xmp.compose.components.XmpTopBar
import org.helllabs.android.xmp.compose.components.annotatedLinkString
import org.helllabs.android.xmp.compose.theme.XmpTheme

@Serializable
object NavSearch

@Composable
fun SearchScreen(
    onBack: () -> Unit,
    onSearch: (String, Int) -> Unit,
    onRandom: () -> Unit,
    onHistory: () -> Unit
) {
    val focusManager = LocalFocusManager.current
    val focusRequester = remember { FocusRequester() }
    var searchText by rememberSaveable { mutableStateOf("") }
    var isArtistSearch by rememberSaveable { mutableIntStateOf(0) }

    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
    }

    Scaffold(
        modifier = Modifier.imePadding(),
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            XmpTopBar(
                title = stringResource(id = R.string.screen_title_search_module),
                onBack = onBack,
                actions = {
                    IconButton(onClick = onHistory) {
                        Icon(imageVector = Icons.Default.History, contentDescription = null)
                    }
                }
            )
        }
    ) { paddingValues ->
        // Weird bottom padding workaround:
        // https://issuetracker.google.com/issues/249727298
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .consumeWindowInsets(paddingValues)
                .systemBarsPadding()
        ) {
            Column(
                modifier = Modifier.align(Alignment.TopCenter),
                verticalArrangement = Arrangement.Top,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Spacer(modifier = Modifier.height(16.dp))
                OutlinedTextField(
                    modifier = Modifier
                        .focusRequester(focusRequester)
                        .padding(horizontal = 16.dp)
                        .fillMaxWidth(),
                    value = searchText,
                    onValueChange = { searchText = it },
                    isError = searchText.isEmpty(),
                    keyboardActions = KeyboardActions(
                        onSearch = {
                            if (searchText.isNotEmpty()) {
                                onSearch(searchText, isArtistSearch)
                            }

                            focusManager.clearFocus()
                        }
                    ),
                    keyboardOptions = KeyboardOptions.Default.copy(
                        imeAction = ImeAction.Search,
                        keyboardType = KeyboardType.Text
                    ),
                    maxLines = 1,
                    label = { Text(text = stringResource(id = R.string.search)) }
                )
                Spacer(modifier = Modifier.height(32.dp))

                SegmentedButtons(
                    isArtistSearch = isArtistSearch,
                    onSearchType = { isArtistSearch = it }
                )

                Spacer(modifier = Modifier.height(32.dp))

                SearchButtons(
                    searchText = searchText,
                    onSearch = { onSearch(it, isArtistSearch) },
                    onRandom = onRandom
                )
            }

            DownloadsText(
                modifier = Modifier
                    .padding(vertical = 8.dp)
                    .align(Alignment.BottomCenter)
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SegmentedButtons(isArtistSearch: Int, onSearchType: (Int) -> Unit) {
    val searchOptions by remember {
        val list = listOf(R.string.artist, R.string.title_or_filename)
        mutableStateOf(list)
    }

    SingleChoiceSegmentedButtonRow(
        modifier = Modifier
            .padding(horizontal = 32.dp)
            .fillMaxWidth()
    ) {
        val activeColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = .75f)
        searchOptions.forEachIndexed { index, label ->
            SegmentedButton(
                colors = SegmentedButtonDefaults.colors(
                    activeContainerColor = activeColor
                ),
                shape = SegmentedButtonDefaults.itemShape(
                    index = index,
                    count = searchOptions.size
                ),
                onClick = { onSearchType(index) },
                selected = index == isArtistSearch,
                label = { Text(text = stringResource(id = label)) }
            )
        }
    }
}

@Composable
private fun SearchButtons(
    searchText: String,
    onSearch: (String) -> Unit,
    onRandom: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 32.dp),
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        Button(
            modifier = Modifier
                .weight(.75f),
            enabled = searchText.isNotEmpty(),
            onClick = { onSearch(searchText) }
        ) {
            Text(text = stringResource(id = R.string.search))
        }
        Spacer(modifier = Modifier.width(16.dp))
        OutlinedButton(
            modifier = Modifier
                .weight(.75f),
            onClick = onRandom
        ) {
            Text(text = stringResource(id = R.string.random))
        }
    }
}

@Composable
private fun DownloadsText(
    modifier: Modifier = Modifier
) {
    val uriHandler = LocalUriHandler.current
    val linkString = annotatedLinkString(
        text = stringResource(id = R.string.search_provided_by),
        url = "modarchive.org"
    )
    // TODO
    ClickableText(
        modifier = modifier,
        text = linkString,
        style = TextStyle(color = MaterialTheme.colorScheme.onBackground),
        onClick = {
            linkString
                .getStringAnnotations("URL", it, it)
                .firstOrNull()
                ?.let { stringAnnotation -> uriHandler.openUri(stringAnnotation.item) }
        }
    )
}

@Preview
@Composable
private fun Preview_SearchScreen() {
    XmpTheme(useDarkTheme = true) {
        SearchScreen(
            onBack = {},
            onSearch = { _, _ -> },
            onRandom = {},
            onHistory = {}
        )
    }
}
