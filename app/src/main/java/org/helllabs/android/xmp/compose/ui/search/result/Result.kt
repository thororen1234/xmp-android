package org.helllabs.android.xmp.compose.ui.search.result

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.AndroidEntryPoint
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient
import okhttp3.Request
import okio.Buffer
import okio.ForwardingSource
import okio.buffer
import okio.sink
import org.helllabs.android.xmp.PrefManager
import org.helllabs.android.xmp.R
import org.helllabs.android.xmp.XmpApplication
import org.helllabs.android.xmp.api.Repository
import org.helllabs.android.xmp.compose.components.ErrorScreen
import org.helllabs.android.xmp.compose.components.MessageDialog
import org.helllabs.android.xmp.compose.components.ProgressbarIndicator
import org.helllabs.android.xmp.compose.components.XmpTopBar
import org.helllabs.android.xmp.compose.theme.XmpTheme
import org.helllabs.android.xmp.compose.ui.player.PlayerActivity
import org.helllabs.android.xmp.compose.ui.search.Search
import org.helllabs.android.xmp.compose.ui.search.SearchError
import org.helllabs.android.xmp.compose.ui.search.components.ModuleLayout
import org.helllabs.android.xmp.core.Constants.isSupported
import org.helllabs.android.xmp.core.Files
import org.helllabs.android.xmp.model.Artist
import org.helllabs.android.xmp.model.ArtistInfo
import org.helllabs.android.xmp.model.License
import org.helllabs.android.xmp.model.Module
import org.helllabs.android.xmp.model.ModuleResult
import org.helllabs.android.xmp.model.Sponsor
import org.helllabs.android.xmp.model.SponsorDetails
import timber.log.Timber
import java.io.File
import java.io.IOException
import javax.inject.Inject

@HiltViewModel
class ModuleResultViewModel @Inject constructor(
    private val okHttpClient: OkHttpClient,
    private val repository: Repository
) : ViewModel() {
    sealed class DownloadStatus {
        data class Error(val error: Exception) : DownloadStatus()
        data class Progress(val percent: Int) : DownloadStatus()
        object None : DownloadStatus()
        object Loading : DownloadStatus()
        object Success : DownloadStatus()
    }

    data class ModuleResultState(
        val isRandom: Boolean = false,
        val isLoading: Boolean = false,
        val softError: String? = null,
        val hardError: Throwable? = null,
        val module: ModuleResult? = null,
        val moduleExists: Boolean = false,
        val moduleSupported: Boolean = true,
        val downloadStatus: DownloadStatus = DownloadStatus.None
    )

    private val _uiState = MutableStateFlow(ModuleResultState())
    val uiState = _uiState.asStateFlow()

    private var currentDownloadJob: Job? = null

    override fun onCleared() {
        super.onCleared()
        currentDownloadJob?.cancel()
    }

    fun downloadModule(mod: String, url: String, file: String) {
        currentDownloadJob?.cancel()

        val pathFile = File(file, mod)
        if (pathFile.parentFile?.exists() == false) {
            pathFile.parentFile?.mkdirs()
        }

        currentDownloadJob = viewModelScope.launch(Dispatchers.IO) {
            try {
                val request = Request.Builder()
                    .url(url)
                    .build()

                _uiState.update {
                    it.copy(
                        isLoading = true,
                        downloadStatus = DownloadStatus.Loading
                    )
                }

                Timber.d("Mod file is: $mod")
                Timber.d("Trying to download a file to $pathFile")
                Timber.d("Download URL is: $url")

                okHttpClient.newCall(request).execute().use { response ->
                    if (!response.isSuccessful) throw IOException("Unexpected code $response")

                    response.body?.let { body ->
                        val contentLength = body.contentLength()
                        val source = body.source()

                        val progressSource = object : ForwardingSource(source) {
                            var totalBytesRead = 0L

                            override fun read(sink: Buffer, byteCount: Long): Long {
                                val bytesRead = super.read(sink, byteCount)
                                totalBytesRead += if (bytesRead != -1L) bytesRead else 0
                                _uiState.update {
                                    val value = (totalBytesRead * 100 / contentLength).toInt()
                                    it.copy(downloadStatus = DownloadStatus.Progress(value))
                                }
                                return bytesRead
                            }
                        }

                        progressSource.use { input ->
                            pathFile.sink().buffer().use { output ->
                                output.writeAll(input)
                            }
                        }
                    }
                }

                _uiState.update {
                    it.copy(
                        isLoading = false,
                        downloadStatus = DownloadStatus.Success
                    )
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        downloadStatus = DownloadStatus.Error(e)
                    )
                }
            } finally {
                _uiState.update {
                    it.copy(
                        moduleExists = doesModuleExist(_uiState.value.module),
                        moduleSupported = isModuleSupported(_uiState.value.module)
                    )
                }
            }
        }
    }

    fun getModuleById(id: Int) {
        viewModelScope.launch {
            _uiState.update { it.copy(isRandom = false, isLoading = true) }

            try {
                val result = repository.getModuleById(id)
                if (result.error != null) {
                    _uiState.update { it.copy(softError = result.error) }
                } else {
                    saveModuleToHistory(result.module)
                    _uiState.update {
                        it.copy(
                            module = result,
                            moduleExists = doesModuleExist(result),
                            moduleSupported = isModuleSupported(result)
                        )
                    }
                }

                _uiState.update { it.copy(isLoading = false) }
            } catch (e: Exception) {
                Timber.e(e)
                _uiState.update { it.copy(hardError = e) }
            }
        }
    }

    fun getRandomModule() {
        viewModelScope.launch {
            _uiState.update { it.copy(isRandom = true, isLoading = true) }

            try {
                val result = repository.getRandomModule()
                if (!result.error.isNullOrBlank()) {
                    _uiState.update { it.copy(softError = result.error) }
                } else {
                    saveModuleToHistory(result.module)
                    _uiState.update {
                        it.copy(
                            module = result,
                            moduleExists = doesModuleExist(result),
                            moduleSupported = isModuleSupported(result)
                        )
                    }
                }

                _uiState.update { it.copy(isLoading = false) }
            } catch (e: Exception) {
                Timber.e(e)
                _uiState.update { it.copy(hardError = e) }
            }
        }
    }

    fun deleteModule() {
        val result = Files.deleteModuleFile(_uiState.value.module?.module!!)
        Timber.d("Module deleted was: $result")
        _uiState.update {
            it.copy(
                moduleExists = doesModuleExist(_uiState.value.module),
                moduleSupported = isModuleSupported(_uiState.value.module)
            )
        }
    }

    private fun doesModuleExist(result: ModuleResult?): Boolean {
        val exist = Files.localFile(result?.module)?.exists()
        Timber.d("Does module exist? -> $exist")
        return exist ?: false
    }

    private fun isModuleSupported(result: ModuleResult?): Boolean {
        val supported = result?.module?.isSupported() ?: true
        Timber.d("Is module supported? -> $supported")
        return supported
    }

    private fun saveModuleToHistory(module: Module) {
        val history = mutableListOf<Module>()

        // We only care about a few things to store.
        try {
            Json.decodeFromString<MutableList<Module>>(PrefManager.searchHistory).map {
                Module(
                    id = it.id,
                    format = it.format,
                    songtitle = it.songtitle,
                    artistInfo = it.artistInfo,
                    bytes = it.bytes
                )
            }.also { history.addAll(it) }
        } catch (e: Exception) {
            // Something happened or empty, make it an empty list
            Timber.w("Failed to deserialize history!")
            PrefManager.searchHistory = "[]"
        }

        if (history.any { it.id == module.id }) {
            Timber.i("Module ${module.id} already exists in history. Skipping")
            return
        }

        val moduleToAdd = Module(
            id = module.id,
            format = module.format,
            songtitle = module.songtitle,
            artistInfo = module.artistInfo,
            bytes = module.bytes
        )
        history.add(moduleToAdd)

        if (history.size >= 50) {
            history.removeFirst()
        }

        PrefManager.searchHistory = Json.encodeToString(history)
    }
}

@AndroidEntryPoint
open class Result : ComponentActivity() {

    private val viewModel by viewModels<ModuleResultViewModel>()

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
            // TODO file was deleted
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Timber.d("onCreate")

        WindowCompat.setDecorFitsSystemWindows(window, false)

        val id = intent.getIntExtra(Search.MODULE_ID, -1)
        if (id < 0) {
            viewModel.getRandomModule()
        } else {
            viewModel.getModuleById(id)
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
                    viewModel.deleteModule()
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
                    val mod = module!!.filename
                    val modDir = Files.getDownloadPath(module)
                    val url = module.url
                    viewModel.downloadModule(mod, url, modDir)
                    moduleExists = null
                },
                onDismiss = { moduleExists = null }
            )

            XmpTheme {
                ModuleResultScreen(
                    state = state,
                    snackbarHostState = snackbarHostState,
                    onBack = { onBackPressedDispatcher.onBackPressed() },
                    onRandom = viewModel::getRandomModule,
                    onDelete = { deleteModule = true },
                    onPlay = { module ->
                        if (Files.localFile(module)!!.exists()) {
                            val path = Files.localFile(module)!!.path
                            val modList = ArrayList<String>()

                            modList.add(path)
                            XmpApplication.instance?.fileList = modList

                            Timber.i("Play $path")
                            Intent(this@Result, PlayerActivity::class.java).apply {
                                putExtra(PlayerActivity.PARM_START, 0)
                            }.also { playerResult.launch(it) }
                        } else {
                            // Does not exist, download module
                            val modDir = Files.getDownloadPath(module)
                            val url = module.url

                            if (Files.localFile(state.module?.module)?.exists() == true) {
                                moduleExists = module
                            } else {
                                Timber.i("Downloading $url to $modDir")
                                val mod = module.filename
                                viewModel.downloadModule(mod, url, modDir)
                            }
                        }
                    }
                )
            }
        }
    }
}

@Composable
private fun ModuleResultScreen(
    state: ModuleResultViewModel.ModuleResultState,
    snackbarHostState: SnackbarHostState,
    onBack: () -> Unit,
    onRandom: () -> Unit,
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
    XmpTheme {
        ModuleResultScreen(
            state = ModuleResultViewModel.ModuleResultState(
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
            onRandom = {}
        )
    }
}
