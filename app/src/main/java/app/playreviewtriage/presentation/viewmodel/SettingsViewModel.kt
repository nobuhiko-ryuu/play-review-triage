package app.playreviewtriage.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.playreviewtriage.domain.repository.AuthRepository
import app.playreviewtriage.domain.repository.ConfigRepository
import app.playreviewtriage.presentation.uistate.SettingsUiState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val configRepository: ConfigRepository,
    private val authRepository: AuthRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            configRepository.configFlow.collect { config ->
                _uiState.update { it.copy(packageName = config.packageName, retentionDays = config.retentionDays) }
            }
        }
    }

    fun logout() {
        viewModelScope.launch {
            authRepository.signOut()
        }
    }

    fun updatePackageName(packageName: String) {
        viewModelScope.launch {
            configRepository.setPackageName(packageName)
            _uiState.update { it.copy(message = "保存しました") }
        }
    }

    fun clearMessage() {
        _uiState.update { it.copy(message = null) }
    }
}
