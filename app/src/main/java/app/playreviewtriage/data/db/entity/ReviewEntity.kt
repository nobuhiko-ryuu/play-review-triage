package app.playreviewtriage.data.db.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Room エンティティ。
 * - importance: Importance enum の name を文字列で保存
 * - reasonTags: ReasonTag の CSV 形式で保存（例: "CRASH,BILLING"）
 */
@Entity(tableName = "review")
data class ReviewEntity(
    @PrimaryKey val reviewId: String,
    val authorName: String?,
    val starRating: Int,
    val text: String,
    val lastModifiedEpochSec: Long,
    val appVersionName: String?,
    val androidOsVersion: Int?,
    val deviceManufacturer: String?,
    val deviceModel: String?,
    val importance: String,
    val reasonTags: String,
    val fetchedAtEpochSec: Long,
)
