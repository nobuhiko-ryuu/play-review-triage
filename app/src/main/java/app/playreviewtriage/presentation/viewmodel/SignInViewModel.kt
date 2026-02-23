package app.playreviewtriage.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.playreviewtriage.core.result.AppException
import app.playreviewtriage.domain.repository.AuthRepository
import app.playreviewtriage.presentation.uistate.SignInUiState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SignInViewModel @Inject constructor(
    private val authRepository: AuthRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow<SignInUiState>(SignInUiState.Idle)
    val uiState: StateFlow<SignInUiState> = _uiState.asStateFlow()

    fun signIn() {
        viewModelScope.launch {
            _uiState.value = SignInUiState.Loading
            val result = authRepository.signIn()
            _uiState.value = result.fold(
                onSuccess = { SignInUiState.Success },
                onFailure = { e ->
                    val msg = if (e is AppException) e.error.toUserMessage() else "連携に失敗しました。"
                    SignInUiState.Error(msg)
                },
            )
        }
    }

    fun resetState() {
        _uiState.value = SignInUiState.Idle
    }
}

private fun app.playreviewtriage.core.result.AppError.toUserMessage(): String = when (this) {
    app.playreviewtriage.core.result.AppError.AuthExpired -> "認証が期限切れです。再ログインしてください。"
    app.playreviewtriage.core.result.AppError.Forbidden -> "このアカウントは対象アプリにアクセスできません。"
    app.playreviewtriage.core.result.AppError.Network -> "通信できませんでした。電波状況を確認して再試行してください。"
    app.playreviewtriage.core.result.AppError.RateLimited -> "リクエストが多すぎます。しばらくしてから再試行してください。"
    is app.playreviewtriage.core.result.AppError.Unknown -> "予期しないエラーが発生しました。しばらくしてから再試行してください。"
}
