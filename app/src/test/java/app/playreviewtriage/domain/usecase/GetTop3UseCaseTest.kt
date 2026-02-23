package app.playreviewtriage.domain.usecase

import app.playreviewtriage.domain.entity.Importance
import app.playreviewtriage.domain.entity.ReasonTag
import app.playreviewtriage.domain.entity.Review
import app.playreviewtriage.domain.repository.ReviewRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Before
import org.junit.Test

class GetTop3UseCaseTest {

    private lateinit var reviewsFlow: MutableStateFlow<List<Review>>
    private lateinit var useCase: GetTop3UseCase

    @Before
    fun setUp() {
        reviewsFlow = MutableStateFlow(emptyList())
        val fakeRepository = object : ReviewRepository {
            override val reviewsFlow: Flow<List<Review>> = this@GetTop3UseCaseTest.reviewsFlow
            override suspend fun syncNow(packageName: String) = Result.success(
                app.playreviewtriage.domain.entity.SyncSummary(0, 0)
            )
            override suspend fun getReview(reviewId: String): Review? = null
            override suspend fun deleteExpired(retentionDays: Int) {}
        }
        useCase = GetTop3UseCase(fakeRepository)
    }

    private fun makeReview(
        id: String,
        importance: Importance,
        lastModifiedEpochSec: Long,
    ) = Review(
        reviewId = id,
        authorName = null,
        starRating = 3,
        text = "test review",
        lastModifiedEpochSec = lastModifiedEpochSec,
        appVersionName = null,
        androidOsVersion = null,
        deviceManufacturer = null,
        deviceModel = null,
        importance = importance,
        reasonTags = setOf(ReasonTag.OTHER),
        fetchedAtEpochSec = 0L,
    )

    @Test
    fun `HIGHレビューはMIDレビューより優先して返される`() = runTest {
        val reviews = listOf(
            makeReview("mid1", Importance.MID, lastModifiedEpochSec = 300L),
            makeReview("high1", Importance.HIGH, lastModifiedEpochSec = 100L),
            makeReview("mid2", Importance.MID, lastModifiedEpochSec = 200L),
        )
        reviewsFlow.value = reviews

        val result = useCase.invoke().first()

        assertEquals("high1", result[0].reviewId)
    }

    @Test
    fun `同じ重要度では新しい順に返される`() = runTest {
        val reviews = listOf(
            makeReview("high_old", Importance.HIGH, lastModifiedEpochSec = 100L),
            makeReview("high_new", Importance.HIGH, lastModifiedEpochSec = 300L),
            makeReview("high_mid", Importance.HIGH, lastModifiedEpochSec = 200L),
        )
        reviewsFlow.value = reviews

        val result = useCase.invoke().first()

        assertEquals("high_new", result[0].reviewId)
        assertEquals("high_mid", result[1].reviewId)
        assertEquals("high_old", result[2].reviewId)
    }

    @Test
    fun `LOWレビューは結果に含まれない`() = runTest {
        val reviews = listOf(
            makeReview("low1", Importance.LOW, lastModifiedEpochSec = 999L),
            makeReview("high1", Importance.HIGH, lastModifiedEpochSec = 100L),
            makeReview("low2", Importance.LOW, lastModifiedEpochSec = 888L),
        )
        reviewsFlow.value = reviews

        val result = useCase.invoke().first()

        assertFalse(result.any { it.importance == Importance.LOW })
        assertEquals(1, result.size)
        assertEquals("high1", result[0].reviewId)
    }

    @Test
    fun `最大3件しか返されない`() = runTest {
        val reviews = listOf(
            makeReview("high1", Importance.HIGH, lastModifiedEpochSec = 500L),
            makeReview("high2", Importance.HIGH, lastModifiedEpochSec = 400L),
            makeReview("high3", Importance.HIGH, lastModifiedEpochSec = 300L),
            makeReview("mid1", Importance.MID, lastModifiedEpochSec = 200L),
            makeReview("mid2", Importance.MID, lastModifiedEpochSec = 100L),
        )
        reviewsFlow.value = reviews

        val result = useCase.invoke().first()

        assertEquals(3, result.size)
    }

    @Test
    fun `HIGHが1件MIDが複数件の場合でも最大3件かつHIGHが先頭になる`() = runTest {
        val reviews = listOf(
            makeReview("mid1", Importance.MID, lastModifiedEpochSec = 400L),
            makeReview("high1", Importance.HIGH, lastModifiedEpochSec = 100L),
            makeReview("mid2", Importance.MID, lastModifiedEpochSec = 300L),
            makeReview("mid3", Importance.MID, lastModifiedEpochSec = 200L),
            makeReview("low1", Importance.LOW, lastModifiedEpochSec = 999L),
        )
        reviewsFlow.value = reviews

        val result = useCase.invoke().first()

        assertEquals(3, result.size)
        assertEquals("high1", result[0].reviewId)
        assertEquals("mid1", result[1].reviewId)
        assertEquals("mid2", result[2].reviewId)
    }

    @Test
    fun `レビューが空のときは空リストが返される`() = runTest {
        reviewsFlow.value = emptyList()

        val result = useCase.invoke().first()

        assertEquals(0, result.size)
    }
}
