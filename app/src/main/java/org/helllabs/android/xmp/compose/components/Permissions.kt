package org.helllabs.android.xmp.compose.components

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.platform.*
import androidx.compose.ui.unit.*
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

/**
 * @author https://github.com/m-derakhshan/PermissionHandler
 */

data class PermissionModel(val permission: String, val rational: String)

data class PermissionState(
    val askPermission: Boolean,
    val showRational: Boolean = false,
    val rationals: List<String> = emptyList(),
    val permissions: List<String>,
    val navigateToSetting: Boolean = false
)

class PermissionViewModelFactory(
    private val permissions: List<PermissionModel>,
    private val askPermission: Boolean
) :
    ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return PermissionViewModel(permissions = permissions, askPermission = askPermission) as T
    }
}

class PermissionViewModel(
    private val permissions: List<PermissionModel>,
    askPermission: Boolean
) : ViewModel() {
    private var startPermissionRequest = 0L
    private var endPermissionRequest = 0L
    private val _state = MutableStateFlow(
        PermissionState(
            permissions = permissions.map { it.permission },
            askPermission = askPermission
        )
    )
    val state = _state.asStateFlow()

    fun onResult(result: Map<String, Boolean>) {
        _state.update { it.copy(askPermission = false) }
        endPermissionRequest = System.currentTimeMillis()
        val notGrantedPermissions =
            permissions.filter { it.permission in result.filter { permission -> permission.value.not() } }
        if (notGrantedPermissions.isEmpty())
            onConsumeRational()
        else {
            _state.update { state -> state.copy(permissions = notGrantedPermissions.map { it.permission }) }
            if (endPermissionRequest - startPermissionRequest < 200) {
                _state.update {
                    it.copy(navigateToSetting = true)
                }
            } else
                _state.update { state ->
                    state.copy(
                        showRational = notGrantedPermissions.isNotEmpty(),
                        rationals = notGrantedPermissions.map { it.rational }
                    )
                }
        }
    }

    fun onGrantPermissionClicked() {
        _state.update { it.copy(askPermission = true) }
        startPermissionRequest = System.currentTimeMillis()
    }

    fun onPermissionRequested() {
        _state.update {
            it.copy(
                askPermission = false,
                navigateToSetting = false
            )
        }
    }

    private fun onConsumeRational() {
        _state.update {
            it.copy(showRational = false)
        }
    }
}

@Composable
fun PermissionHandler(
    permissions: List<PermissionModel>,
    askPermission: Boolean,
    result: (Map<String, Boolean>) -> Unit = {}
) {
    val activity = LocalContext.current as Activity
    val viewModel: PermissionViewModel = viewModel(
        factory = PermissionViewModelFactory(permissions, askPermission)
    )
    val state by viewModel.state.collectAsStateWithLifecycle()

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions(),
    ) {
        result(it)
        viewModel.onResult(it)
    }

    LaunchedEffect(state.askPermission) {
        if (state.askPermission) {
            permissionLauncher.launch(state.permissions.toTypedArray())
        }
    }
    LaunchedEffect(state.navigateToSetting) {
        if (state.navigateToSetting) {
            activity.openAppSetting()
            viewModel.onPermissionRequested()
        }
    }
    AnimatedVisibility(
        visible = state.showRational,
        enter = slideInVertically() + fadeIn(),
        exit = slideOutVertically() + fadeOut()
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Access denied",
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.padding(vertical = 8.dp),
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
            Spacer(modifier = Modifier.padding(vertical = 4.dp))
            state.rationals.forEachIndexed { index, item ->
                Text(
                    text = "${index + 1}) $item",
                    modifier = Modifier.padding(vertical = 8.dp),
                    style = MaterialTheme.typography.bodyLarge
                )
            }
            Spacer(modifier = Modifier.padding(vertical = 4.dp))
            Button(
                onClick = viewModel::onGrantPermissionClicked,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 4.dp)
            ) {
                Text(text = "Grant Permission", modifier = Modifier.padding(vertical = 4.dp))
            }

        }
    }

}

private fun Activity.openAppSetting() {
    Intent(
        Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
        Uri.fromParts("package", packageName, null)
    ).also(::startActivity)
}
