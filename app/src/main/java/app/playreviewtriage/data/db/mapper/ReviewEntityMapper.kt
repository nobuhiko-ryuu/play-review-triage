package app.playreviewtriage.data.db.mapper

import app.playreviewtriage.data.db.entity.ReviewEntity
import app.playreviewtriage.domain.entity.Importance
import app.playreviewtriage.domain.entity.ReasonTag
import app.playreviewtriage.domain.entity.Review

/**
 * ReviewEntity ↔ Review（Domain）の変換。
 *
 * Entity → Domain:
 * - importance: String → Importance enum（不正値は LOW）
 * - reasonTags: CSV String → Set<ReasonTag>（不正値はスキップ）
 *
 * Domain → Entity:
 * - importance: Importance → name 文字列
 * - reasonTags: Set<ReasonTag> → joinToString(",")
 */
fun ReviewEntity.toDomain(): Review = Review(
    reviewId = reviewId,
    authorName = authorName,
    starRating = starRating,
    text = text,
    lastModifiedEpochSec = lastModifiedEpochSec,
    appVersionName = appVersionName,
    androidOsVersion = androidOsVersion,
    deviceManufacturer = deviceManufacturer,
    deviceModel = deviceModel,
    importance = runCatching { Importance.valueOf(importance) }.getOrDefault(Importance.LOW),
    reasonTags = reasonTags
        .split(",")
        .filter { it.isNotBlank() }
        .mapNotNull { tag -> runCatching { ReasonTag.valueOf(tag.trim()) }.getOrNull() }
        .toSet(),
    fetchedAtEpochSec = fetchedAtEpochSec,
)

fun Review.toEntity(): ReviewEntity = ReviewEntity(
    reviewId = reviewId,
    authorName = authorName,
    starRating = starRating,
    text = text,
    lastModifiedEpochSec = lastModifiedEpochSec,
    appVersionName = appVersionName,
    androidOsVersion = androidOsVersion,
    deviceManufacturer = deviceManufacturer,
    deviceModel = deviceModel,
    importance = importance.name,
    reasonTags = reasonTags.joinToString(",") { it.name },
    fetchedAtEpochSec = fetchedAtEpochSec,
)
