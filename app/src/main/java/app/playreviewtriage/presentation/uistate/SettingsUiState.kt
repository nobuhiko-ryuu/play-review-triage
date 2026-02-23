package app.playreviewtriage.presentation.uistate

data class SettingsUiState(
    val packageName: String = "",
    val retentionDays: Int = 30,
    val isLoading: Boolean = false,
    val message: String? = null,
)
