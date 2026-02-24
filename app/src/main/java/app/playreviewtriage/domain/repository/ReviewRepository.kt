package app.playreviewtriage.domain.repository

import app.playreviewtriage.domain.entity.Review
import app.playreviewtriage.domain.entity.SyncSummary
import kotlinx.coroutines.flow.Flow

interface ReviewRepository {
    val reviewsFlow: Flow<List<Review>>
    suspend fun syncNow(packageName: String): Result<SyncSummary>
    suspend fun getReview(reviewId: String): Review?
    suspend fun deleteExpired(retentionDays: Int)
    /** パッケージ名のアクセス権を疎通チェック（DB保存なし） */
    suspend fun checkAccess(packageName: String): Result<Unit>
}
