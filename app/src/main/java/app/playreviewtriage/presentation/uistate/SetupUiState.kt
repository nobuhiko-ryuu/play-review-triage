package app.playreviewtriage.presentation.uistate

sealed class SetupUiState {
    data object Idle : SetupUiState()
    data object Loading : SetupUiState()
    data object Success : SetupUiState()
    data class ValidationError(val message: String) : SetupUiState()
    data class ApiError(val message: String) : SetupUiState()
}
