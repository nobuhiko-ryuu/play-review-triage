package app.playreviewtriage.di

import app.playreviewtriage.data.repository.ConfigRepositoryImpl
import app.playreviewtriage.domain.repository.ConfigRepository
import app.playreviewtriage.domain.triage.RuleBasedTriageEngineV1
import app.playreviewtriage.domain.triage.TriageEngine
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryBindingsModule {

    @Binds @Singleton
    abstract fun bindConfigRepository(impl: ConfigRepositoryImpl): ConfigRepository

    @Binds @Singleton
    abstract fun bindTriageEngine(impl: RuleBasedTriageEngineV1): TriageEngine
}
