package com.packpal.kids.data.prefs

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "packpal_prefs")

data class AppPreferences(
    val selectedTemplateId: Long?,
    val animationsEnabled: Boolean,
    val seeded: Boolean,
)

class PreferencesRepository(context: Context) {
    private val store = context.applicationContext.dataStore

    private object Keys {
        val SELECTED = longPreferencesKey("selected_template_id")
        val ANIMATIONS = booleanPreferencesKey("animations_enabled")
        val SEEDED = booleanPreferencesKey("starter_data_seeded")
    }

    val preferences: Flow<AppPreferences> = store.data.map {
        AppPreferences(
            selectedTemplateId = it[Keys.SELECTED],
            animationsEnabled = it[Keys.ANIMATIONS] ?: true,
            seeded = it[Keys.SEEDED] ?: false,
        )
    }

    suspend fun current(): AppPreferences = preferences.first()

    suspend fun setSelectedTemplate(id: Long?) {
        store.edit { if (id == null) it.remove(Keys.SELECTED) else it[Keys.SELECTED] = id }
    }

    suspend fun setAnimationsEnabled(enabled: Boolean) {
        store.edit { it[Keys.ANIMATIONS] = enabled }
    }

    suspend fun setSeeded(seeded: Boolean) {
        store.edit { it[Keys.SEEDED] = seeded }
    }

    suspend fun clear() {
        store.edit { it.clear() }
    }
}
