package app.playreviewtriage.presentation.uistate

import app.playreviewtriage.domain.entity.Review

sealed class DetailUiState {
    data object Loading : DetailUiState()
    data class Success(val review: Review) : DetailUiState()
    data object NotFound : DetailUiState()
}
