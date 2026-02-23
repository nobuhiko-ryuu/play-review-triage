package app.playreviewtriage.domain.entity

data class Review(
    val reviewId: String,
    val authorName: String?,
    val starRating: Int,
    val text: String,
    val lastModifiedEpochSec: Long,
    val appVersionName: String?,
    val androidOsVersion: Int?,
    val deviceManufacturer: String?,
    val deviceModel: String?,
    val importance: Importance,
    val reasonTags: Set<ReasonTag>,
    val fetchedAtEpochSec: Long,
)
