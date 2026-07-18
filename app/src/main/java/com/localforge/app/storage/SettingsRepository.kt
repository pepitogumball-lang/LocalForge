package com.localforge.app.storage

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "settings")

class SettingsRepository(private val context: Context) {
    private val PORT = intPreferencesKey("server_port")
    private val ROOT_URI = stringPreferencesKey("root_uri")
    private val AUTO_START = booleanPreferencesKey("auto_start")

    val port: Flow<Int> = context.dataStore.data.map { it[PORT] ?: 8080 }
    val rootUri: Flow<String?> = context.dataStore.data.map { it[ROOT_URI] }
    val autoStart: Flow<Boolean> = context.dataStore.data.map { it[AUTO_START] ?: false }

    suspend fun saveSettings(port: Int, uri: String?, autoStart: Boolean) {
        context.dataStore.edit {
            it[PORT] = port
            it[ROOT_URI] = uri ?: ""
            it[AUTO_START] = autoStart
        }
    }
}
