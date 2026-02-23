package app.playreviewtriage.domain.usecase

import app.playreviewtriage.domain.entity.Importance
import app.playreviewtriage.domain.entity.Review
import app.playreviewtriage.domain.repository.ReviewRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class GetTop3UseCase @Inject constructor(
    private val reviewRepository: ReviewRepository,
) {
    fun invoke(): Flow<List<Review>> {
        return reviewRepository.reviewsFlow.map { reviews ->
            val highReviews = reviews
                .filter { it.importance == Importance.HIGH }
                .sortedByDescending { it.lastModifiedEpochSec }

            val midReviews = reviews
                .filter { it.importance == Importance.MID }
                .sortedByDescending { it.lastModifiedEpochSec }

            val combined = (highReviews + midReviews)
            combined.take(3)
        }
    }
}
