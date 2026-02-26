package app.playreviewtriage.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.playreviewtriage.core.result.AppException
import app.playreviewtriage.domain.repository.ConfigRepository
import app.playreviewtriage.domain.usecase.GetTop3UseCase
import app.playreviewtriage.domain.usecase.SyncReviewsUseCase
import app.playreviewtriage.presentation.uistate.TodayUiState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import javax.inject.Inject

@HiltViewModel
class TodayViewModel @Inject constructor(
    private val getTop3UseCase: GetTop3UseCase,
    private val syncReviewsUseCase: SyncReviewsUseCase,
    private val configRepository: ConfigRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow<TodayUiState>(TodayUiState.Loading)
    val uiState: StateFlow<TodayUiState> = _uiState.asStateFlow()

    private val formatter = DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm")

    init {
        viewModelScope.launch {
            getTop3UseCase.invoke().collectLatest { reviews ->
                val config = configRepository.configFlow.first()
                val label = if (config.lastSyncEpochSec == 0L) "未同期"
                else "最終同期: " + Instant.ofEpochSecond(config.lastSyncEpochSec)
                    .atZone(ZoneId.systemDefault()).format(formatter)
                _uiState.value = TodayUiState.Success(top3 = reviews, lastSyncLabel = label)
            }
        }
    }

    fun sync() {
        val current = _uiState.value
        if (current is TodayUiState.Success && current.isSyncing) return

        viewModelScope.launch {
            val currentList = if (current is TodayUiState.Success) current.top3 else emptyList()
            val currentLabel = (current as? TodayUiState.Success)?.lastSyncLabel ?: "未同期"
            _uiState.value = TodayUiState.Success(
                top3 = currentList,
                lastSyncLabel = currentLabel,
                isSyncing = true,
            )
            val config = configRepository.configFlow.first()
            val result = syncReviewsUseCase.invoke(config.packageName)
            result.fold(
                onSuccess = {
                    val freshConfig = configRepository.configFlow.first()
                    val label = if (freshConfig.lastSyncEpochSec == 0L) "未同期"
                    else "最終同期: " + Instant.ofEpochSecond(freshConfig.lastSyncEpochSec)
                        .atZone(ZoneId.systemDefault()).format(formatter)
                    val top3 = getTop3UseCase.invoke().first()
                    _uiState.value = TodayUiState.Success(top3 = top3, lastSyncLabel = label)
                },
                onFailure = { e ->
                    val msg = if (e is AppException) e.error.toUserMessage() else "同期に失敗しました。"
                    _uiState.value = TodayUiState.Error(message = msg, cachedTop3 = currentList)
                },
            )
        }
    }
}

private fun app.playreviewtriage.core.result.AppError.toUserMessage(): String = when (this) {
    app.playreviewtriage.core.result.AppError.AuthExpired -> "認証が期限切れです。再ログインしてください。"
    app.playreviewtriage.core.result.AppError.Forbidden -> "このアカウントは対象アプリにアクセスできません。"
    app.playreviewtriage.core.result.AppError.Network -> "通信できませんでした。電波状況を確認して再試行してください。"
    app.playreviewtriage.core.result.AppError.RateLimited -> "リクエストが多すぎます。しばらくしてから再試行してください。"
    is app.playreviewtriage.core.result.AppError.Unknown -> message ?: "予期しないエラーが発生しました。"
}
