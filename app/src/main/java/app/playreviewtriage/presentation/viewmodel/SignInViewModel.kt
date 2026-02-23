package app.playreviewtriage.presentation.viewmodel

import android.content.Intent
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.playreviewtriage.core.result.AppError
import app.playreviewtriage.core.result.AppException
import app.playreviewtriage.domain.repository.AuthRepository
import app.playreviewtriage.presentation.uistate.SignInUiState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SignInViewModel @Inject constructor(
    private val authRepository: AuthRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow<SignInUiState>(SignInUiState.Idle)
    val uiState: StateFlow<SignInUiState> = _uiState.asStateFlow()

    /** スコープ許可ダイアログを起動すべき場合に発行する */
    private val _recoveryIntent = MutableSharedFlow<Intent>()
    val recoveryIntent: SharedFlow<Intent> = _recoveryIntent.asSharedFlow()

    private var lastAccountName: String? = null

    /** Google Sign-In で取得したアカウント名を受け取り、サインインを完了する */
    fun completeSignIn(accountName: String) {
        lastAccountName = accountName
        viewModelScope.launch {
            _uiState.value = SignInUiState.Loading
            val result = authRepository.completeSignIn(accountName)
            result.fold(
                onSuccess = { _uiState.value = SignInUiState.Success },
                onFailure = { e ->
                    val recoveryIntent = authRepository.consumeRecoveryIntent()
                    if (recoveryIntent != null) {
                        // スコープ未許可 → 許可ダイアログを表示させる
                        _uiState.value = SignInUiState.Idle
                        _recoveryIntent.emit(recoveryIntent)
                    } else {
                        val msg = if (e is AppException) e.error.toUserMessage() else "連携に失敗しました。"
                        _uiState.value = SignInUiState.Error(msg)
                    }
                },
            )
        }
    }

    /** スコープ許可後に再試行する */
    fun retryAfterPermissionGrant() {
        lastAccountName?.let { completeSignIn(it) }
    }

    fun onSignInFailed() {
        _uiState.value = SignInUiState.Error("Googleアカウントの選択がキャンセルされました。")
    }

    fun resetState() {
        _uiState.value = SignInUiState.Idle
    }
}

private fun AppError.toUserMessage(): String = when (this) {
    AppError.AuthExpired -> "認証が期限切れです。再ログインしてください。"
    AppError.Forbidden -> "このアカウントは対象アプリにアクセスできません。"
    AppError.Network -> "通信できませんでした。電波状況を確認して再試行してください。"
    AppError.RateLimited -> "リクエストが多すぎます。しばらくしてから再試行してください。"
    is AppError.Unknown -> "予期しないエラーが発生しました。しばらくしてから再試行してください。"
}
