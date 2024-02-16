package org.helllabs.android.xmp.compose.ui.search.result

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.BottomAppBar
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import org.helllabs.android.xmp.R
import org.helllabs.android.xmp.StorageManager
import org.helllabs.android.xmp.XmpApplication
import org.helllabs.android.xmp.compose.components.ErrorScreen
import org.helllabs.android.xmp.compose.components.MessageDialog
import org.helllabs.android.xmp.compose.components.ProgressbarIndicator
import org.helllabs.android.xmp.compose.components.XmpTopBar
import org.helllabs.android.xmp.compose.theme.XmpTheme
import org.helllabs.android.xmp.compose.ui.player.PlayerActivity
import org.helllabs.android.xmp.compose.ui.search.Search
import org.helllabs.android.xmp.compose.ui.search.SearchError
import org.helllabs.android.xmp.compose.ui.search.components.ModuleLayout
import org.helllabs.android.xmp.model.Artist
import org.helllabs.android.xmp.model.ArtistInfo
import org.helllabs.android.xmp.model.License
import org.helllabs.android.xmp.model.Module
import org.helllabs.android.xmp.model.ModuleResult
import org.helllabs.android.xmp.model.Sponsor
import org.helllabs.android.xmp.model.SponsorDetails
import timber.log.Timber

open class Result : ComponentActivity() {

    private val viewModel by viewModels<ResultViewModel> { ResultViewModel.Factory }

    private lateinit var snackbarHostState: SnackbarHostState

    private val playerContract = ActivityResultContracts.StartActivityForResult()
    private var playerResult = registerForActivityResult(playerContract) { result ->
        if (result.resultCode == 1) {
            result.data?.getStringExtra("error")?.let {
                Timber.w("Result with error: $it")
                lifecycleScope.launch {
                    snackbarHostState.showSnackbar(message = it)
                }
            }
        }
        if (result.resultCode == 2) {
            viewModel.update(this)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Timber.d("onCreate")

        enableEdgeToEdge(
            navigationBarStyle = SystemBarStyle.auto(
                Color.Transparent.toArgb(),
                Color.Transparent.toArgb()
            )
        )

        val id = intent.getIntExtra(Search.MODULE_ID, -1)
        if (id < 0) {
            viewModel.getRandomModule(this)
        } else {
            viewModel.getModuleById(this, id)
        }

        snackbarHostState = SnackbarHostState()

        setContent {
            val state by viewModel.uiState.collectAsStateWithLifecycle()

            LaunchedEffect(state.hardError) {
                if (state.hardError != null) {
                    Timber.w("Hard error has occurred")
                    Intent(this@Result, SearchError::class.java).apply {
                        putExtra(Search.ERROR, state.hardError)
                    }.also(::startActivity)
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
                    viewModel.deleteModule(this)
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
                    val module = moduleExists
                    StorageManager.getDownloadPath(
                        context = this,
                        module = module!!,
                        onSuccess = {
                            val mod = module.filename
                            val url = module.url
                            viewModel.downloadModule(this, mod, url, it.uri)
                        },
                        onError = viewModel::showSoftError
                    )


                    moduleExists = null
                },
                onDismiss = { moduleExists = null }
            )

            XmpTheme {
                ModuleResultScreen(
                    state = state,
                    snackbarHostState = snackbarHostState,
                    onBack = { onBackPressedDispatcher.onBackPressed() },
                    onRandom = { viewModel.getRandomModule(this) },
                    onDelete = { deleteModule = true },
                    onShare = ::shareLink,
                    onPlay = { module ->
                        StorageManager.doesModuleExist(
                            context = this,
                            module = module,
                            onFound = {
                                val modListUri = arrayListOf(it)

                                XmpApplication.instance?.fileListUri = modListUri

                                Timber.i("Play ${it.path}")
                                Intent(this@Result, PlayerActivity::class.java).apply {
                                    putExtra(PlayerActivity.PARM_START, 0)
                                }.also(playerResult::launch)
                            },
                            onNotFound = {
                                Timber.i("Downloading ${module.url} to ${it.uri.path}")
                                viewModel.downloadModule(
                                    this,
                                    module.filename,
                                    module.url,
                                    it.uri
                                )
                            },
                            onError = viewModel::showSoftError
                        )
                    }
                )
            }
        }
    }

    private fun shareLink(url: String) {
        val sendIntent: Intent = Intent().apply {
            action = Intent.ACTION_SEND
            putExtra(Intent.EXTRA_TEXT, url)
            type = "text/html"
        }

        val shareIntent = Intent.createChooser(sendIntent, null)
        startActivity(shareIntent)
    }
}

@Composable
private fun ModuleResultScreen(
    state: ResultViewModel.ModuleResultState,
    snackbarHostState: SnackbarHostState,
    onBack: () -> Unit,
    onRandom: () -> Unit,
    onShare: (String) -> Unit,
    onDelete: () -> Unit,
    onPlay: (module: Module) -> Unit
) {
    val scrollState = rememberScrollState()
    val isScrolled = remember {
        derivedStateOf {
            scrollState.value > 0
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
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
                        IconButton(onClick = onDelete) {
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
            snackbarHostState = SnackbarHostState(),
            onBack = {},
            onDelete = {},
            onPlay = {},
            onRandom = {},
            onShare = {}
        )
    }
}
