package app.playreviewtriage.di

import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
object WorkerModule {
    // TODO(Domain Agent + Data Agent): DailySyncWorker の HiltWorkerFactory バインドを追加する
}
