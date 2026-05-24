package com.coreagent.settings

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

val Context.dataStore by preferencesDataStore(name = "user_settings")

/**
 * 用戶可配置設定中心
 */
class SettingsManager(private val context: Context) {
    
    private val API_KEY = stringPreferencesKey("api_key")
    private val CRITIC_LIMIT = intPreferencesKey("critic_limit")

    val apiKey: Flow<String?> = context.dataStore.data.map { it[API_KEY] }
    val criticLimit: Flow<Int> = context.dataStore.data.map { it[CRITIC_LIMIT] ?: 3 }

    suspend fun updateApiKey(key: String) {
        context.dataStore.edit { it[API_KEY] = key }
    }

    suspend fun updateCriticLimit(limit: Int) {
        context.dataStore.edit { it[CRITIC_LIMIT] = limit }
    }
}
