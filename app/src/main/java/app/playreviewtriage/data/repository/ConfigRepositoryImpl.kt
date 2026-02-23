package app.playreviewtriage.data.repository

import app.playreviewtriage.data.prefs.datastore.SettingsStore
import app.playreviewtriage.domain.entity.AppConfig
import app.playreviewtriage.domain.repository.ConfigRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ConfigRepositoryImpl @Inject constructor(
    private val settingsStore: SettingsStore,
) : ConfigRepository {

    override val configFlow: Flow<AppConfig> = combine(
        settingsStore.packageNameFlow,
        settingsStore.lastSyncFlow,
        settingsStore.retentionDaysFlow,
    ) { packageName, lastSyncEpochSec, retentionDays ->
        AppConfig(
            packageName = packageName,
            lastSyncEpochSec = lastSyncEpochSec,
            retentionDays = retentionDays,
        )
    }

    override suspend fun setPackageName(packageName: String) {
        settingsStore.setPackageName(packageName)
    }

    override suspend fun setRetentionDays(days: Int) {
        settingsStore.setRetentionDays(days)
    }

    override suspend fun updateLastSync(epochSec: Long) {
        settingsStore.setLastSync(epochSec)
    }
}
