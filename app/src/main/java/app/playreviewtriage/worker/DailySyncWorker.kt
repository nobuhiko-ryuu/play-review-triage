package app.playreviewtriage.worker

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import app.playreviewtriage.domain.usecase.RunDailySyncUseCase
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject

@HiltWorker
class DailySyncWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted workerParams: WorkerParameters,
    private val runDailySyncUseCase: RunDailySyncUseCase,
) : CoroutineWorker(context, workerParams) {

    override suspend fun doWork(): Result {
        return try {
            val result = runDailySyncUseCase.invoke()
            if (result.isSuccess) Result.success() else Result.retry()
        } catch (e: Exception) {
            Result.failure()
        }
    }
}
