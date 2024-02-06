package org.helllabs.android.xmp.compose.ui.search

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ClearAll
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import kotlinx.serialization.json.Json
import org.helllabs.android.xmp.PrefManager
import org.helllabs.android.xmp.R
import org.helllabs.android.xmp.compose.components.ErrorScreen
import org.helllabs.android.xmp.compose.components.MessageDialog
import org.helllabs.android.xmp.compose.components.XmpTopBar
import org.helllabs.android.xmp.compose.theme.XmpTheme
import org.helllabs.android.xmp.compose.ui.search.components.ItemModule
import org.helllabs.android.xmp.compose.ui.search.result.Result
import org.helllabs.android.xmp.model.Module
import timber.log.Timber

class SearchHistory : ComponentActivity() {

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
            var historyList by remember {
                var list: MutableList<Module> = mutableListOf()

                try {
                    list = Json.decodeFromString(PrefManager.searchHistory)
                } catch (e: Exception) {
                    // Something happened or empty, make it an empty list
                    Timber.w("Failed to deserialize history!")
                    PrefManager.searchHistory = "[]"
                }

                mutableStateOf(list.toList())
            }

            /**
             * Clear history dialog
             */
            var clearDialog by remember { mutableStateOf(false) }
            MessageDialog(
                isShowing = clearDialog,
                title = "Clear History",
                text = "Are you sure you want to clear your Module search history?",
                confirmText = "Clear",
                onConfirm = {
                    PrefManager.searchHistory = "[]"
                    historyList = listOf()
                    clearDialog = false
                },
                onDismiss = { clearDialog = false }
            )

            XmpTheme {
                HistoryScreen(
                    historyList = historyList,
                    onBack = { onBackPressedDispatcher.onBackPressed() },
                    onClear = { clearDialog = true },
                    onClicked = {
                        Intent(this@SearchHistory, Result::class.java).apply {
                            putExtra(Search.MODULE_ID, it)
                        }.also(::startActivity)
                    }
                )
            }
        }
    }
}

@Composable
private fun HistoryScreen(
    historyList: List<Module>,
    onBack: () -> Unit,
    onClear: () -> Unit,
    onClicked: (Int) -> Unit
) {
    val scrollState = rememberLazyListState()
    val isScrolled = remember {
        derivedStateOf {
            scrollState.firstVisibleItemIndex > 0
        }
    }

    Scaffold(
        topBar = {
            XmpTopBar(
                isScrolled = isScrolled.value,
                onBack = onBack,
                title = stringResource(id = R.string.search_history),
                actions = {
                    if (historyList.isNotEmpty()) {
                        IconButton(onClick = onClear) {
                            Icon(
                                imageVector = Icons.Default.ClearAll,
                                contentDescription = null
                            )
                        }
                    }
                }
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier.padding(paddingValues),
            contentAlignment = Alignment.Center
        ) {
            LazyColumn(modifier = Modifier.fillMaxSize()) {
                itemsIndexed(items = historyList.reversed()) { _, item ->
                    ItemModule(
                        item = item,
                        onClick = { onClicked(item.id) }
                    )
                }
            }

            if (historyList.isEmpty()) {
                ErrorScreen(text = "Empty History")
            }
        }
    }
}

@Preview
@Composable
private fun Preview_HistoryScreen() {
    XmpTheme(useDarkTheme = true) {
        HistoryScreen(
            historyList = List(12) {
                Module(songtitle = "Module $it")
            },
            onBack = { },
            onClear = { },
            onClicked = { }
        )
    }
}
