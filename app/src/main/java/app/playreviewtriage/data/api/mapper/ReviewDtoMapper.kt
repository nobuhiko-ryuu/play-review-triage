package app.playreviewtriage.data.api.mapper

import app.playreviewtriage.data.api.dto.ReviewDto
import app.playreviewtriage.domain.entity.Importance
import app.playreviewtriage.domain.entity.Review

/**
 * ReviewDto を Domain エンティティ Review に変換する拡張関数。
 *
 * - comments の中から最新の userComment を取得する（lastModified.seconds が最大のもの）
 * - starRating が null なら 0 とする
 * - lastModified.seconds が null なら 0L とする
 * - reviewId が null なら null を返す（呼び出し元でスキップ）
 * - importance と reasonTags はこの段階では仮の値（LOW + emptySet）。
 *   実際のトリアージは UseCase 側で TriageEngine が行う。
 * - fetchedAtEpochSec はパラメータで受け取る
 */
fun ReviewDto.toDomain(fetchedAtEpochSec: Long): Review? {
    val id = reviewId ?: return null

    val latestUserComment = comments
        ?.mapNotNull { it.userComment }
        ?.maxByOrNull { it.lastModified?.seconds ?: 0L }

    return Review(
        reviewId = id,
        authorName = authorName,
        starRating = latestUserComment?.starRating ?: 0,
        text = latestUserComment?.text.orEmpty(),
        lastModifiedEpochSec = latestUserComment?.lastModified?.seconds ?: 0L,
        appVersionName = latestUserComment?.appVersionName,
        androidOsVersion = latestUserComment?.androidOsVersion,
        deviceManufacturer = latestUserComment?.deviceMetadata?.manufacturer,
        deviceModel = latestUserComment?.deviceMetadata?.productName,
        importance = Importance.LOW,
        reasonTags = emptySet(),
        fetchedAtEpochSec = fetchedAtEpochSec,
    )
}
