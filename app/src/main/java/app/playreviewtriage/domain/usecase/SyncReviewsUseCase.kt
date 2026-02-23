package app.playreviewtriage.domain.usecase

import app.playreviewtriage.core.result.AppError
import app.playreviewtriage.core.result.AppException
import app.playreviewtriage.core.time.Clock
import app.playreviewtriage.domain.entity.SyncSummary
import app.playreviewtriage.domain.repository.AuthRepository
import app.playreviewtriage.domain.repository.ConfigRepository
import app.playreviewtriage.domain.repository.ReviewRepository
import javax.inject.Inject

class SyncReviewsUseCase @Inject constructor(
    private val authRepository: AuthRepository,
    private val reviewRepository: ReviewRepository,
    private val configRepository: ConfigRepository,
    private val clock: Clock,
) {
    suspend fun invoke(packageName: String): Result<SyncSummary> {
        val token = authRepository.getValidAccessTokenOrNull()
            ?: return Result.failure(AppException(AppError.AuthExpired))

        val result = reviewRepository.syncNow(packageName)

        if (result.isSuccess) {
            configRepository.updateLastSync(clock.nowEpochSec())
        }

        return result
    }
}
