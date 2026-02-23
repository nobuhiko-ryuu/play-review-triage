package app.playreviewtriage.data.prefs.datastore

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.settingsDataStore: DataStore<Preferences> by preferencesDataStore(name = "settings_prefs")

/**
 * PreferencesDataStore でアプリ設定（パッケージ名・最終同期時刻・保持日数）を管理する。
 */
@Singleton
class SettingsStore @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    private val dataStore = context.settingsDataStore

    private object Keys {
        val PACKAGE_NAME = stringPreferencesKey("package_name")
        val LAST_SYNC_EPOCH_SEC = longPreferencesKey("last_sync_epoch_sec")
        val RETENTION_DAYS = intPreferencesKey("retention_days")
    }

    val packageNameFlow: Flow<String> = dataStore.data.map { prefs ->
        prefs[Keys.PACKAGE_NAME] ?: ""
    }

    val lastSyncFlow: Flow<Long> = dataStore.data.map { prefs ->
        prefs[Keys.LAST_SYNC_EPOCH_SEC] ?: 0L
    }

    val retentionDaysFlow: Flow<Int> = dataStore.data.map { prefs ->
        prefs[Keys.RETENTION_DAYS] ?: 30
    }

    suspend fun setPackageName(v: String) {
        dataStore.edit { prefs ->
            prefs[Keys.PACKAGE_NAME] = v
        }
    }

    suspend fun setLastSync(epochSec: Long) {
        dataStore.edit { prefs ->
            prefs[Keys.LAST_SYNC_EPOCH_SEC] = epochSec
        }
    }

    suspend fun setRetentionDays(days: Int) {
        dataStore.edit { prefs ->
            prefs[Keys.RETENTION_DAYS] = days
        }
    }
}
