package app.playreviewtriage.domain.usecase

import app.playreviewtriage.domain.entity.Importance
import app.playreviewtriage.domain.entity.SyncSummary
import app.playreviewtriage.domain.repository.ConfigRepository
import app.playreviewtriage.domain.repository.ReviewRepository
import kotlinx.coroutines.flow.first
import javax.inject.Inject

class RunDailySyncUseCase @Inject constructor(
    private val syncReviewsUseCase: SyncReviewsUseCase,
    private val reviewRepository: ReviewRepository,
    private val configRepository: ConfigRepository,
) {
    suspend fun invoke(): Result<SyncSummary> {
        val config = configRepository.configFlow.first()

        if (config.packageName.isBlank()) {
            return Result.success(SyncSummary(fetchedCount = 0, highCount = 0))
        }

        val result = syncReviewsUseCase.invoke(config.packageName)

        if (result.isSuccess) {
            reviewRepository.deleteExpired(config.retentionDays)
            val fetchedCount = result.getOrDefault(SyncSummary(0, 0)).fetchedCount
            val highCount = reviewRepository.reviewsFlow.first()
                .count { it.importance == Importance.HIGH }
            return Result.success(SyncSummary(fetchedCount = fetchedCount, highCount = highCount))
        }

        return result
    }
}
