package app.playreviewtriage.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.playreviewtriage.core.result.AppError
import app.playreviewtriage.core.result.AppException
import app.playreviewtriage.domain.usecase.SetPackageNameUseCase
import app.playreviewtriage.presentation.uistate.SetupUiState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SetupViewModel @Inject constructor(
    private val setPackageNameUseCase: SetPackageNameUseCase,
) : ViewModel() {

    private val _uiState = MutableStateFlow<SetupUiState>(SetupUiState.Idle)
    val uiState: StateFlow<SetupUiState> = _uiState.asStateFlow()

    fun save(packageName: String) {
        viewModelScope.launch {
            _uiState.value = SetupUiState.Loading
            val result = setPackageNameUseCase.invoke(packageName)
            _uiState.value = result.fold(
                onSuccess = { SetupUiState.Success },
                onFailure = { e ->
                    when {
                        e is IllegalArgumentException ->
                            SetupUiState.ValidationError(e.message ?: "入力値が正しくありません。")
                        e is AppException -> when (e.error) {
                            AppError.AuthExpired ->
                                SetupUiState.ApiError("認証が切れています。ログアウトして再度サインインしてください。")
                            AppError.Forbidden ->
                                SetupUiState.ApiError("このアカウントは対象アプリにアクセスできません。\nPlay Consoleで権限を確認してください。")
                            AppError.Network ->
                                SetupUiState.ApiError("通信エラーが発生しました。ネットワーク接続を確認してください。")
                            AppError.RateLimited ->
                                SetupUiState.ApiError("リクエスト制限に達しました。しばらく待ってから再試行してください。")
                            is AppError.Unknown ->
                                SetupUiState.ApiError(e.error.message ?: "予期しないエラーが発生しました。")
                        }
                        else -> SetupUiState.ApiError("予期しないエラーが発生しました。")
                    }
                },
            )
        }
    }

    fun resetState() {
        _uiState.value = SetupUiState.Idle
    }
}
