package net.packsi.tunnels.data

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

// Single DataStore instance for the whole process. Mirrors the prototype's
// localStorage keys (iranconn_secs / iranconn_connected) plus server & app selection.
private val Context.dataStore by preferencesDataStore(name = "iranconn_prefs")

class VpnPreferences(private val context: Context) {

    private object Keys {
        val CONNECTED = booleanPreferencesKey("connected")
        val SECONDS = longPreferencesKey("seconds")
        val SELECTED_SERVER = stringPreferencesKey("selected_server")
        val ENABLED_APPS = stringSetPreferencesKey("enabled_apps")
        @Suppress("unused") val SCHEMA = intPreferencesKey("schema")
    }

    val connected: Flow<Boolean> =
        context.dataStore.data.map { it[Keys.CONNECTED] ?: false }

    val seconds: Flow<Long> =
        context.dataStore.data.map { it[Keys.SECONDS] ?: 0L }

    val selectedServer: Flow<String?> =
        context.dataStore.data.map { it[Keys.SELECTED_SERVER] }

    val enabledApps: Flow<Set<String>> =
        context.dataStore.data.map { it[Keys.ENABLED_APPS] ?: emptySet() }

    suspend fun setConnected(value: Boolean) =
        context.dataStore.edit { it[Keys.CONNECTED] = value }

    suspend fun setSeconds(value: Long) =
        context.dataStore.edit { it[Keys.SECONDS] = value }

    suspend fun setSelectedServer(id: String?) =
        context.dataStore.edit {
            if (id == null) it.remove(Keys.SELECTED_SERVER) else it[Keys.SELECTED_SERVER] = id
        }

    suspend fun setEnabledApps(ids: Set<String>) =
        context.dataStore.edit { it[Keys.ENABLED_APPS] = ids }
}
