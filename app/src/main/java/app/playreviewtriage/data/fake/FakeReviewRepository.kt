package app.playreviewtriage.data.fake

import app.playreviewtriage.core.result.AppError
import app.playreviewtriage.core.result.toFailure
import app.playreviewtriage.domain.entity.Importance
import app.playreviewtriage.domain.entity.ReasonTag
import app.playreviewtriage.domain.entity.Review
import app.playreviewtriage.domain.entity.SyncSummary
import app.playreviewtriage.domain.repository.ReviewRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Fake実装：internal ビルド (USE_FAKE_DATA=true) 用。
 * Play Console API を呼ばずにハードコードのレビューを返す。
 */
@Singleton
class FakeReviewRepository @Inject constructor() : ReviewRepository {

    private val _reviews = MutableStateFlow(SEED_REVIEWS)

    override val reviewsFlow: Flow<List<Review>> = _reviews.asStateFlow()

    override suspend fun syncNow(packageName: String): Result<SyncSummary> {
        // 同期ごとに新しいレビューを1件追加してUIの変化を確認できるようにする
        val current = _reviews.value.toMutableList()
        current.add(
            Review(
                reviewId = "fake-new-${System.currentTimeMillis()}",
                authorName = "新規ユーザー",
                starRating = 2,
                text = "アップデートしたら動作が重くなりました。改善をお願いします。",
                lastModifiedEpochSec = System.currentTimeMillis() / 1000,
                appVersionName = "2.1.0",
                androidOsVersion = "14",
                deviceManufacturer = "Google",
                deviceModel = "Pixel 8",
                importance = Importance.MID,
                reasonTags = setOf(ReasonTag.UI),
                fetchedAtEpochSec = System.currentTimeMillis() / 1000,
            )
        )
        _reviews.value = current
        return Result.success(SyncSummary(fetchedCount = 1, highCount = 0))
    }

    override suspend fun getReview(reviewId: String): Review? =
        _reviews.value.find { it.reviewId == reviewId }

    override suspend fun deleteExpired(retentionDays: Int) { /* no-op */ }

    companion object {
        private val BASE_TIME = System.currentTimeMillis() / 1000

        val SEED_REVIEWS = listOf(
            Review(
                reviewId = "fake-001",
                authorName = "田中 太郎",
                starRating = 1,
                text = "起動直後にクラッシュします。Pixel 7で再現しました。早急に修正をお願いします。バックグラウンドから復帰するたびに落ちるので使い物になりません。",
                lastModifiedEpochSec = BASE_TIME - 3600,
                appVersionName = "2.0.1",
                androidOsVersion = "14",
                deviceManufacturer = "Google",
                deviceModel = "Pixel 7",
                importance = Importance.HIGH,
                reasonTags = setOf(ReasonTag.CRASH),
                fetchedAtEpochSec = BASE_TIME,
            ),
            Review(
                reviewId = "fake-002",
                authorName = "鈴木 花子",
                starRating = 1,
                text = "課金したのにアイテムが付与されませんでした。サポートに連絡しても返答がありません。返金を求めます。",
                lastModifiedEpochSec = BASE_TIME - 7200,
                appVersionName = "2.0.0",
                androidOsVersion = "13",
                deviceManufacturer = "Samsung",
                deviceModel = "Galaxy S23",
                importance = Importance.HIGH,
                reasonTags = setOf(ReasonTag.BILLING),
                fetchedAtEpochSec = BASE_TIME,
            ),
            Review(
                reviewId = "fake-003",
                authorName = "佐藤 次郎",
                starRating = 2,
                text = "ボタンが小さくて押しにくいです。特にホーム画面の右下のアイコンはタップしにくい。UIの改善をお願いします。",
                lastModifiedEpochSec = BASE_TIME - 10800,
                appVersionName = "2.0.1",
                androidOsVersion = "13",
                deviceManufacturer = "Sony",
                deviceModel = "Xperia 1 V",
                importance = Importance.MID,
                reasonTags = setOf(ReasonTag.UI),
                fetchedAtEpochSec = BASE_TIME,
            ),
            Review(
                reviewId = "fake-004",
                authorName = "山田 美咲",
                starRating = 5,
                text = "とても使いやすいアプリです！毎日使っています。",
                lastModifiedEpochSec = BASE_TIME - 14400,
                appVersionName = "2.0.1",
                androidOsVersion = "14",
                deviceManufacturer = "Google",
                deviceModel = "Pixel 8 Pro",
                importance = Importance.LOW,
                reasonTags = setOf(ReasonTag.NOISE),
                fetchedAtEpochSec = BASE_TIME,
            ),
            Review(
                reviewId = "fake-005",
                authorName = "伊藤 健一",
                starRating = 3,
                text = "機能は良いのですが、通知が来ない場合があります。バグかもしれません。",
                lastModifiedEpochSec = BASE_TIME - 18000,
                appVersionName = "1.9.5",
                androidOsVersion = "12",
                deviceManufacturer = "SHARP",
                deviceModel = "AQUOS sense7",
                importance = Importance.MID,
                reasonTags = setOf(ReasonTag.OTHER),
                fetchedAtEpochSec = BASE_TIME,
            ),
        )
    }
}
