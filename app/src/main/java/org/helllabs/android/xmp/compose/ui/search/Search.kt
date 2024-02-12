package org.helllabs.android.xmp.compose.ui.search

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.ClickableText
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.History
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import org.helllabs.android.xmp.R
import org.helllabs.android.xmp.compose.components.XmpTopBar
import org.helllabs.android.xmp.compose.components.annotatedLinkString
import org.helllabs.android.xmp.compose.theme.XmpTheme
import org.helllabs.android.xmp.compose.ui.search.result.Result
import org.helllabs.android.xmp.compose.ui.search.result.SearchResult
import timber.log.Timber

enum class SearchType {
    TYPE_ARTIST,
    TYPE_TITLE_OR_FILENAME
}

class Search : ComponentActivity() {

    public override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        enableEdgeToEdge(
            navigationBarStyle = SystemBarStyle.auto(
                Color.Transparent.toArgb(),
                Color.Transparent.toArgb()
            )
        )

        Timber.d("onCreate")
        setContent {
            XmpTheme {
                SearchScreen(
                    onBack = {
                        onBackPressedDispatcher.onBackPressed()
                    },
                    onSearch = { query, type ->
                        Intent(this, SearchResult::class.java).apply {
                            putExtra(SEARCH_TEXT, query.trim())
                            putExtra("search_type", type)
                        }.also(::startActivity)
                    },
                    onRandom = {
                        Intent(this, Result::class.java).apply {
                            putExtra(MODULE_ID, -1)
                        }.also(::startActivity)
                    },
                    onHistory = {
                        Intent(this, SearchHistory::class.java).also(::startActivity)
                    }
                )
            }
        }
    }

    companion object {
        const val SEARCH_TEXT = "search_text"
        const val MODULE_ID = "module_id"
        const val ERROR = "error"
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SearchScreen(
    onBack: () -> Unit,
    onSearch: (query: String, type: SearchType) -> Unit,
    onRandom: () -> Unit,
    onHistory: () -> Unit
) {
    val focusManager = LocalFocusManager.current
    val focusRequester = remember { FocusRequester() }
    var searchText by rememberSaveable { mutableStateOf("") }
    var searchType by rememberSaveable { mutableStateOf(SearchType.TYPE_TITLE_OR_FILENAME) }
    val searchOptions by remember {
        val list = listOf(
            R.string.search_artist,
            R.string.search_title_or_filename
        )
        mutableStateOf(list)
    }

    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
    }

    Scaffold(
        modifier = Modifier.imePadding(),
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            XmpTopBar(
                title = stringResource(id = R.string.search_title),
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
                                onSearch(searchText, searchType)
                            }

                            focusManager.clearFocus()
                        }
                    ),
                    keyboardOptions = KeyboardOptions.Default.copy(
                        imeAction = ImeAction.Search,
                        keyboardType = KeyboardType.Text
                    ),
                    maxLines = 1,
                    label = { Text(text = stringResource(id = R.string.search_search)) }
                )
                Spacer(modifier = Modifier.height(32.dp))
                SingleChoiceSegmentedButtonRow(
                    modifier = Modifier
                        .padding(horizontal = 32.dp)
                        .fillMaxWidth()
                ) {
                    searchOptions.forEachIndexed { index, label ->
                        SegmentedButton(
                            shape = SegmentedButtonDefaults.itemShape(
                                index = index,
                                count = searchOptions.size
                            ),
                            onClick = { searchType = SearchType.entries.toTypedArray()[index] },
                            selected = index == searchType.ordinal,
                            label = { Text(stringResource(id = label)) }
                        )
                    }
                }
                Spacer(modifier = Modifier.height(32.dp))
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
                        onClick = { onSearch(searchText, searchType) }
                    ) {
                        Text(text = stringResource(id = R.string.search_search))
                    }
                    Spacer(modifier = Modifier.width(16.dp))
                    OutlinedButton(
                        modifier = Modifier
                            .weight(.75f),
                        onClick = onRandom
                    ) {
                        Text(text = stringResource(id = R.string.search_random_pick))
                    }
                }
            }

            val uriHandler = LocalUriHandler.current
            val linkString = annotatedLinkString(
                text = stringResource(id = R.string.search_provided_by),
                url = "modarchive.org"
            )
            ClickableText(
                modifier = Modifier
                    .padding(vertical = 8.dp)
                    .align(Alignment.BottomCenter),
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
    }
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
