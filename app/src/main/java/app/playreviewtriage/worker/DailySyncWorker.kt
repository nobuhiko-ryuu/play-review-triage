package app.playreviewtriage.worker

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import app.playreviewtriage.domain.usecase.RunDailySyncUseCase
import app.playreviewtriage.notification.NotificationHelper
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject

@HiltWorker
class DailySyncWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted workerParams: WorkerParameters,
    private val runDailySyncUseCase: RunDailySyncUseCase,
    private val notificationHelper: NotificationHelper,
) : CoroutineWorker(context, workerParams) {

    override suspend fun doWork(): Result {
        return try {
            val result = runDailySyncUseCase.invoke()
            if (result.isSuccess) {
                val summary = result.getOrThrow()
                if (summary.highCount > 0) {
                    notificationHelper.notifyHighReviews(summary.highCount)
                }
                Result.success()
            } else {
                Result.retry()
            }
        } catch (e: Exception) {
            Result.failure()
        }
    }
}
