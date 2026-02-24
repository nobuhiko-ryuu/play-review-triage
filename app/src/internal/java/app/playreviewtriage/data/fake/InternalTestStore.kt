package app.playreviewtriage.data.fake

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.internalTestDataStore: DataStore<Preferences>
        by preferencesDataStore(name = "internal_test")

@Singleton
class InternalTestStore @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    private val KEY_SCENARIO = stringPreferencesKey("scenario")

    val scenario: Flow<FakeScenario> = context.internalTestDataStore.data.map { prefs ->
        prefs[KEY_SCENARIO]?.let { runCatching { FakeScenario.valueOf(it) }.getOrNull() }
            ?: FakeScenario.SUCCESS
    }

    suspend fun setScenario(scenario: FakeScenario) {
        context.internalTestDataStore.edit { prefs ->
            prefs[KEY_SCENARIO] = scenario.name
        }
    }
}
