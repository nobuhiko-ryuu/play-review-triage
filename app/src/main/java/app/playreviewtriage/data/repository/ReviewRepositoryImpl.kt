package app.playreviewtriage.data.repository

import app.playreviewtriage.core.result.AppError
import app.playreviewtriage.core.result.toFailure
import app.playreviewtriage.core.time.Clock
import app.playreviewtriage.data.api.httpCodeToAppError
import app.playreviewtriage.data.api.mapper.toDomain
import app.playreviewtriage.data.api.service.PublisherService
import app.playreviewtriage.data.db.dao.ReviewDao
import app.playreviewtriage.data.db.mapper.toDomain
import app.playreviewtriage.data.db.mapper.toEntity
import app.playreviewtriage.domain.entity.Importance
import app.playreviewtriage.domain.entity.Review
import app.playreviewtriage.domain.entity.SyncSummary
import app.playreviewtriage.domain.repository.ReviewRepository
import app.playreviewtriage.domain.triage.TriageEngine
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ReviewRepositoryImpl @Inject constructor(
    private val dao: ReviewDao,
    private val service: PublisherService,
    private val triageEngine: TriageEngine,
    private val clock: Clock,
) : ReviewRepository {

    override val reviewsFlow: Flow<List<Review>> =
        dao.observeAll().map { entities -> entities.map { it.toDomain() } }

    override suspend fun syncNow(packageName: String): Result<SyncSummary> {
        return try {
            val fetchedAtEpochSec = clock.nowEpochSec()
            val response = service.listReviews(packageName)

            if (!response.isSuccessful) {
                return httpCodeToAppError(response.code()).toFailure()
            }

            val dtos = response.body()?.reviews.orEmpty()

            val reviews = dtos.mapNotNull { dto ->
                val review = dto.toDomain(fetchedAtEpochSec) ?: return@mapNotNull null
                val triageResult = triageEngine.evaluate(review.text, review.starRating)
                review.copy(
                    importance = triageResult.importance,
                    reasonTags = triageResult.tags,
                )
            }

            val entities = reviews.map { it.toEntity() }
            dao.upsertAll(entities)

            val highCount = reviews.count { it.importance == Importance.HIGH }
            Result.success(SyncSummary(fetchedCount = reviews.size, highCount = highCount))
        } catch (e: IOException) {
            AppError.Network.toFailure()
        }
    }

    override suspend fun getReview(reviewId: String): Review? =
        dao.findById(reviewId)?.toDomain()

    override suspend fun deleteExpired(retentionDays: Int) {
        val thresholdEpochSec = clock.nowEpochSec() - retentionDays * 86400L
        dao.deleteOlderThan(thresholdEpochSec)
    }
}
