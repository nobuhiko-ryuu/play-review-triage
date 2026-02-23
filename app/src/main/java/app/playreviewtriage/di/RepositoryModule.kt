package app.playreviewtriage.di

import app.playreviewtriage.BuildConfig
import app.playreviewtriage.data.fake.FakeAuthRepository
import app.playreviewtriage.data.fake.FakeReviewRepository
import app.playreviewtriage.data.repository.AuthRepositoryImpl
import app.playreviewtriage.data.repository.ConfigRepositoryImpl
import app.playreviewtriage.data.repository.ReviewRepositoryImpl
import app.playreviewtriage.domain.repository.AuthRepository
import app.playreviewtriage.domain.repository.ConfigRepository
import app.playreviewtriage.domain.repository.ReviewRepository
import app.playreviewtriage.domain.triage.RuleBasedTriageEngineV1
import app.playreviewtriage.domain.triage.TriageEngine
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds @Singleton
    abstract fun bindConfigRepository(impl: ConfigRepositoryImpl): ConfigRepository

    @Binds @Singleton
    abstract fun bindTriageEngine(impl: RuleBasedTriageEngineV1): TriageEngine

    companion object {

        @Provides @Singleton
        fun provideAuthRepository(
            real: AuthRepositoryImpl,
            fake: FakeAuthRepository,
        ): AuthRepository = if (BuildConfig.USE_FAKE_DATA) fake else real

        @Provides @Singleton
        fun provideReviewRepository(
            real: ReviewRepositoryImpl,
            fake: FakeReviewRepository,
        ): ReviewRepository = if (BuildConfig.USE_FAKE_DATA) fake else real
    }
}
