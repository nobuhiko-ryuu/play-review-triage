package app.playreviewtriage.data.prefs.datastore

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.tokenDataStore: DataStore<Preferences> by preferencesDataStore(name = "token_prefs")

/**
 * PreferencesDataStore でアクセストークンとその有効期限を管理する。
 * トークン値はログ出力しないこと。
 */
@Singleton
class TokenStore @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    private val dataStore = context.tokenDataStore

    private object Keys {
        val ACCESS_TOKEN = stringPreferencesKey("access_token")
        val TOKEN_EXPIRY_EPOCH_SEC = longPreferencesKey("token_expiry_epoch_sec")
    }

    fun getAccessToken(): Flow<String?> = dataStore.data.map { prefs ->
        prefs[Keys.ACCESS_TOKEN]?.takeIf { it.isNotEmpty() }
    }

    fun getExpiryEpochSec(): Flow<Long> = dataStore.data.map { prefs ->
        prefs[Keys.TOKEN_EXPIRY_EPOCH_SEC] ?: 0L
    }

    suspend fun saveToken(token: String, expiryEpochSec: Long) {
        dataStore.edit { prefs ->
            prefs[Keys.ACCESS_TOKEN] = token
            prefs[Keys.TOKEN_EXPIRY_EPOCH_SEC] = expiryEpochSec
        }
    }

    suspend fun clearToken() {
        dataStore.edit { prefs ->
            prefs.remove(Keys.ACCESS_TOKEN)
            prefs.remove(Keys.TOKEN_EXPIRY_EPOCH_SEC)
        }
    }
}
