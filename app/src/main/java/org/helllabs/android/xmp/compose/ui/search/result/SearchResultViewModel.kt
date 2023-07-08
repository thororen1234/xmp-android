package org.helllabs.android.xmp.compose.ui.search.result

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.helllabs.android.xmp.api.Repository
import timber.log.Timber
import javax.inject.Inject

@HiltViewModel
class SearchResultViewModel @Inject constructor(
    private val repository: Repository
) : ViewModel() {
    data class SearchResultState(
        val hardError: Throwable? = null,
        val isLoading: Boolean = false,
        val softError: String? = null,
        val title: String = "",
        var result: Any? = null
    )

    private val _uiState = MutableStateFlow(SearchResultState())
    val uiState = _uiState.asStateFlow()

    fun getFileOrTitle(string: String, query: String) =
        viewModelScope.launch {
            _uiState.update { it.copy(title = string, isLoading = true) }

            try {
                val result = repository.getFileNameOrTitle(query)
                if (!result.error.isNullOrBlank()) {
                    _uiState.update { it.copy(softError = result.error) }
                } else {
                    _uiState.update { it.copy(result = result, softError = "") }
                }
            } catch (e: Exception) {
                Timber.e(e)
                _uiState.update { it.copy(hardError = e) }
            } finally {
                _uiState.update { it.copy(isLoading = false) }
            }
        }

    fun getArtistById(id: Int) =
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }

            try {
                val result = repository.getArtistById(id)
                if (!result.error.isNullOrBlank()) {
                    _uiState.update { it.copy(softError = result.error) }
                } else {
                    _uiState.update { it.copy(result = result, softError = "") }
                }
            } catch (e: Exception) {
                Timber.e(e)
                _uiState.update { it.copy(hardError = e) }
            } finally {
                _uiState.update { it.copy(isLoading = false) }
            }
        }

    fun getArtists(string: String, query: String) =
        viewModelScope.launch {
            _uiState.update { it.copy(title = string, isLoading = true) }

            try {
                val result = repository.getArtistSearch(query)
                if (!result.error.isNullOrBlank()) {
                    _uiState.update { it.copy(softError = result.error) }
                } else {
                    _uiState.update { it.copy(result = result, softError = "") }
                }
            } catch (e: Exception) {
                Timber.e(e)
                _uiState.update { it.copy(hardError = e) }
            } finally {
                _uiState.update { it.copy(isLoading = false) }
            }
        }
}
