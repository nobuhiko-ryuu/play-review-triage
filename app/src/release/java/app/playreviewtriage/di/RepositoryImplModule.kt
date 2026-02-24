package app.playreviewtriage.di

import app.playreviewtriage.data.repository.AuthRepositoryImpl
import app.playreviewtriage.data.repository.ReviewRepositoryImpl
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
    abstract fun bindAuthRepository(impl: AuthRepositoryImpl): AuthRepository

    @Binds @Singleton
    abstract fun bindReviewRepository(impl: ReviewRepositoryImpl): ReviewRepository
}
