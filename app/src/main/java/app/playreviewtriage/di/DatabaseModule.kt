package app.playreviewtriage.di

import android.content.Context
import androidx.room.Room
import app.playreviewtriage.data.db.AppDatabase
import app.playreviewtriage.data.db.dao.ReviewDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideAppDatabase(@ApplicationContext context: Context): AppDatabase =
        Room.databaseBuilder(context, AppDatabase::class.java, "play_review_triage.db")
            .fallbackToDestructiveMigration()
            .build()

    @Provides
    fun provideReviewDao(db: AppDatabase): ReviewDao = db.reviewDao()
}
