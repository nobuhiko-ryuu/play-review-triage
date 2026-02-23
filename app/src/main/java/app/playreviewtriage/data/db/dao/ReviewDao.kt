package app.playreviewtriage.data.db.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import app.playreviewtriage.data.db.entity.ReviewEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ReviewDao {

    @Query("SELECT * FROM review ORDER BY lastModifiedEpochSec DESC")
    fun observeAll(): Flow<List<ReviewEntity>>

    @Upsert
    suspend fun upsertAll(reviews: List<ReviewEntity>)

    @Query("SELECT * FROM review WHERE reviewId = :reviewId LIMIT 1")
    suspend fun findById(reviewId: String): ReviewEntity?

    @Query("DELETE FROM review WHERE fetchedAtEpochSec < :thresholdEpochSec")
    suspend fun deleteOlderThan(thresholdEpochSec: Long)
}
