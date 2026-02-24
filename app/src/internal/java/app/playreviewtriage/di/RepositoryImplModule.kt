package app.playreviewtriage.di

import app.playreviewtriage.data.fake.FakeAuthRepository
import app.playreviewtriage.data.fake.FakeReviewRepository
import app.playreviewtriage.domain.repository.AuthRepository
import app.playreviewtriage.domain.repository.ReviewRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryImplModule {

    @Binds @Singleton
    abstract fun bindAuthRepository(impl: FakeAuthRepository): AuthRepository

    @Binds @Singleton
    abstract fun bindReviewRepository(impl: FakeReviewRepository): ReviewRepository
}
