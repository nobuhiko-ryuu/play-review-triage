package app.playreviewtriage.presentation.uistate

import app.playreviewtriage.domain.entity.Review

sealed class TodayUiState {
    data object Loading : TodayUiState()
    data class Success(
        val top3: List<Review>,
        val lastSyncLabel: String,
        val isSyncing: Boolean = false,
    ) : TodayUiState()
    data class Error(
        val message: String,
        val cachedTop3: List<Review> = emptyList(),
    ) : TodayUiState()
}
