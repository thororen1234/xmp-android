package org.helllabs.android.xmp.compose.ui.search.result

import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.*
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.platform.*
import androidx.compose.ui.res.*
import androidx.compose.ui.text.*
import androidx.compose.ui.tooling.preview.*
import androidx.compose.ui.unit.*
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import org.helllabs.android.xmp.R
import org.helllabs.android.xmp.XmpApplication
import org.helllabs.android.xmp.compose.components.ErrorScreen
import org.helllabs.android.xmp.compose.components.MessageDialog
import org.helllabs.android.xmp.compose.components.ProgressbarIndicator
import org.helllabs.android.xmp.compose.components.XmpTopBar
import org.helllabs.android.xmp.compose.theme.XmpTheme
import org.helllabs.android.xmp.compose.ui.player.PlayerActivity
import org.helllabs.android.xmp.compose.ui.search.components.ModuleLayout
import org.helllabs.android.xmp.core.Constants
import org.helllabs.android.xmp.core.StorageManager
import org.helllabs.android.xmp.model.Artist
import org.helllabs.android.xmp.model.ArtistInfo
import org.helllabs.android.xmp.model.License
import org.helllabs.android.xmp.model.Module
import org.helllabs.android.xmp.model.ModuleResult
import org.helllabs.android.xmp.model.Sponsor
import org.helllabs.android.xmp.model.SponsorDetails
import timber.log.Timber

@Serializable
data class NavSearchResult(val moduleID: Int) // -1 will random result.

@Composable
fun ModuleResultScreenImpl(
    viewModel: ResultViewModel,
    snackBarHostState: SnackbarHostState,
    moduleID: Int,
    onShare: (String) -> Unit,
    onError: (String?) -> Unit,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    val playerResult = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == 1) {
            result.data?.getStringExtra("error")?.let { str ->
                Timber.w("Result with error: $str")
                scope.launch {
                    snackBarHostState.showSnackbar(message = str)
                }
            }
        }
        if (result.resultCode == 2) {
            viewModel.update()
        }
    }

    LaunchedEffect(state.hardError) {
        if (state.hardError != null) {
            Timber.w("Hard error has occurred")
            onError(state.hardError)
        }
    }

    LaunchedEffect(Unit) {
        viewModel.getModuleById(moduleID)
    }

    ModuleResultScreen(
        state = state,
        snackBarHostState = snackBarHostState,
        onBack = onBack,
        onRandom = viewModel::getRandomModule,
        onShare = { onShare(it) },
        onDeleteModule = viewModel::deleteModule,
        onPlay = { module ->
            StorageManager.doesModuleExist(module = module)
                .onSuccess { dfc ->
                    if (dfc.isFile()) {
                        val modListUri = arrayListOf(dfc.uri)

                        XmpApplication.instance?.fileListUri = modListUri

                        Timber.i("Play ${dfc.uri.path}")
                        Intent(context, PlayerActivity::class.java).apply {
                            putExtra(Constants.PARM_START, 0)
                        }.also(playerResult::launch)
                    } else {
                        viewModel.downloadModule(module, dfc)
                    }
                }.onFailure {
                    viewModel.showSoftError(it.message ?: context.getString(R.string.error))
                }
        }
    )
}

@Composable
private fun ModuleResultScreen(
    state: ModuleResultState,
    snackBarHostState: SnackbarHostState,
    onBack: () -> Unit,
    onRandom: () -> Unit,
    onShare: (String) -> Unit,
    onPlay: (module: Module) -> Unit,
    onDeleteModule: () -> Unit
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
        title = stringResource(id = R.string.delete_file_title),
        text = stringResource(
            id = R.string.delete_file_message,
            state.module?.module?.filename ?: ""
        ),
        confirmText = stringResource(id = R.string.delete),
        onConfirm = {
            onDeleteModule()
            deleteModule = false
        },
        onDismiss = { deleteModule = false }
    )

    Scaffold(
        snackbarHost = { SnackbarHost(hostState = snackBarHostState) },
        topBar = {
            XmpTopBar(
                isScrolled = isScrolled.value,
                title = if (state.isRandom) {
                    stringResource(id = R.string.screen_title_random)
                } else {
                    stringResource(id = R.string.screen_title_result)
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
                        Icon(
                            imageVector = Icons.Default.Share,
                            contentDescription = "Share Module"
                        )
                    }
                }
            )
        },
        bottomBar = {
            val buttonText = when {
                state.isLoading -> stringResource(id = R.string.loading)
                state.moduleExists -> stringResource(id = R.string.play)
                !state.moduleSupported -> stringResource(id = R.string.unsupported)
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
                        Text(text = stringResource(id = R.string.random))
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

                ErrorScreen(text = state.softError)

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
            state = ModuleResultState(
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
            onDeleteModule = {},
        )
    }
}
