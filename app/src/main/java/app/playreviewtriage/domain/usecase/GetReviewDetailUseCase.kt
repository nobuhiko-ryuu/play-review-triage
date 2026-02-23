package app.playreviewtriage.domain.usecase

import app.playreviewtriage.domain.entity.Review
import app.playreviewtriage.domain.repository.ReviewRepository
import javax.inject.Inject

class GetReviewDetailUseCase @Inject constructor(
    private val reviewRepository: ReviewRepository,
) {
    suspend fun invoke(reviewId: String): Review? {
        return reviewRepository.getReview(reviewId)
    }
}
