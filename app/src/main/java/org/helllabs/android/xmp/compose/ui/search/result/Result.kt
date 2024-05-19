package org.helllabs.android.xmp.compose.ui.search.result

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.*
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.res.*
import androidx.compose.ui.text.*
import androidx.compose.ui.tooling.preview.*
import androidx.compose.ui.unit.*
import kotlinx.serialization.Serializable
import org.helllabs.android.xmp.R
import org.helllabs.android.xmp.compose.components.ErrorScreen
import org.helllabs.android.xmp.compose.components.MessageDialog
import org.helllabs.android.xmp.compose.components.ProgressbarIndicator
import org.helllabs.android.xmp.compose.components.XmpTopBar
import org.helllabs.android.xmp.compose.theme.XmpTheme
import org.helllabs.android.xmp.compose.ui.search.components.ModuleLayout
import org.helllabs.android.xmp.model.Artist
import org.helllabs.android.xmp.model.ArtistInfo
import org.helllabs.android.xmp.model.License
import org.helllabs.android.xmp.model.Module
import org.helllabs.android.xmp.model.ModuleResult
import org.helllabs.android.xmp.model.Sponsor
import org.helllabs.android.xmp.model.SponsorDetails

@Serializable
data class NavSearchResult(val moduleID: Int) // -1 will random result.

@Composable
fun ModuleResultScreen(
    state: ResultViewModel.ModuleResultState,
    snackBarHostState: SnackbarHostState,
    onBack: () -> Unit,
    onRandom: () -> Unit,
    onShare: (String) -> Unit,
    onPlay: (module: Module) -> Unit,
    onDeleteModule: () -> Unit,
    onDownloadModule: (Module) -> Unit
) {
    val scrollState = rememberScrollState()
    val isScrolled = remember {
        derivedStateOf {
            scrollState.value > 0
        }
    }

    var deleteModule by remember { mutableStateOf(false) }
    MessageDialog(
        isShowing = deleteModule,
        icon = Icons.Default.Delete,
        title = stringResource(id = R.string.delete_file),
        text = stringResource(
            id = R.string.delete_file_message,
            state.module?.module?.filename ?: ""
        ),
        confirmText = stringResource(id = R.string.menu_delete),
        onConfirm = {
            onDeleteModule()
            deleteModule = false
        },
        onDismiss = { deleteModule = false }
    )

    // Hopefully this shouldn't happen, but let's be sure to handle it.
    var moduleExists: Module? by remember { mutableStateOf(null) }
    MessageDialog(
        isShowing = moduleExists != null,
        icon = Icons.Default.Delete,
        title = stringResource(id = R.string.msg_file_exists),
        text = stringResource(id = R.string.msg_file_exists_message),
        confirmText = stringResource(id = R.string.ok),
        onConfirm = {
            onDownloadModule(moduleExists!!)
            moduleExists = null
        },
        onDismiss = { moduleExists = null }
    )

    Scaffold(
        snackbarHost = { SnackbarHost(hostState = snackBarHostState) },
        topBar = {
            XmpTopBar(
                isScrolled = isScrolled.value,
                title = if (state.isRandom) {
                    stringResource(id = R.string.search_random_title)
                } else {
                    stringResource(id = R.string.search_result_title)
                },
                onBack = onBack,
                actions = {
                    if (state.moduleExists) {
                        IconButton(onClick = { deleteModule = true }) {
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = null
                            )
                        }
                    }
                    IconButton(onClick = { onShare(state.module!!.module.infopage) }) {
                        Icon(imageVector = Icons.Default.Share, contentDescription = "Share Module")
                    }
                }
            )
        },
        bottomBar = {
            val buttonText = when {
                state.isLoading -> stringResource(id = R.string.button_loading)
                state.moduleExists -> stringResource(id = R.string.button_play)
                !state.moduleSupported ->
                    stringResource(id = R.string.button_download_unsupported)

                else -> stringResource(id = R.string.download)
            }

            BottomAppBar {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceAround,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Button(
                        modifier = Modifier.width(112.dp),
                        enabled = !state.isLoading && state.moduleSupported,
                        onClick = { onPlay(state.module!!.module) }
                    ) {
                        Text(text = buttonText)
                    }
                    Button(
                        modifier = Modifier.width(112.dp),
                        enabled = !state.isLoading,
                        onClick = onRandom
                    ) {
                        Text(text = stringResource(id = R.string.search_random_pick))
                    }
                }
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .padding(paddingValues)
                .fillMaxSize()
                .verticalScroll(scrollState),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .weight(.5f),
                contentAlignment = Alignment.Center
            ) {
                ModuleLayout(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    moduleResult = state.module
                )

                state.softError?.let {
                    ErrorScreen(text = it)
                }

                ProgressbarIndicator(isLoading = state.isLoading)
            }
        }
    }
}

@Preview
@Composable
private fun Preview_ModuleResult() {
    XmpTheme(useDarkTheme = true) {
        ModuleResultScreen(
            state = ResultViewModel.ModuleResultState(
                module = ModuleResult(
                    sponsor = Sponsor(
                        details = SponsorDetails(
                            link = "",
                            text = "Some Sponsor Text"
                        )
                    ),
                    module = Module(
                        filename = "",
                        bytes = 669669,
                        format = "XM",
                        artistInfo = ArtistInfo(artist = listOf(Artist(alias = "Some Artist"))),
                        infopage = "",
                        license = License(
                            title = "Some License Title",
                            legalurl = "",
                            description = "Some License Description"
                        ),
                        comment = "Some Comment",
                        instruments = buildAnnotatedString {
                            repeat(20) {
                                append("Some Instrument $it\n")
                            }
                        }.toString()
                    )
                )
            ),
            snackBarHostState = SnackbarHostState(),
            onBack = {},
            onPlay = {},
            onRandom = {},
            onShare = {},
            onDownloadModule = {},
            onDeleteModule = {},
        )
    }
}
