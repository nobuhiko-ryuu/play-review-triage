package app.playreviewtriage.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.playreviewtriage.domain.usecase.GetReviewDetailUseCase
import app.playreviewtriage.presentation.uistate.DetailUiState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class DetailViewModel @Inject constructor(
    private val getReviewDetailUseCase: GetReviewDetailUseCase,
) : ViewModel() {

    private val _uiState = MutableStateFlow<DetailUiState>(DetailUiState.Loading)
    val uiState: StateFlow<DetailUiState> = _uiState.asStateFlow()

    fun load(reviewId: String) {
        viewModelScope.launch {
            _uiState.value = DetailUiState.Loading
            val review = getReviewDetailUseCase.invoke(reviewId)
            _uiState.value = if (review != null) DetailUiState.Success(review) else DetailUiState.NotFound
        }
    }
}
