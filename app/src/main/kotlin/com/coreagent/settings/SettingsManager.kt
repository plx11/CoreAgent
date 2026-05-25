package com.coreagent.settings

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

val Context.dataStore by preferencesDataStore(name = "user_settings")

/**
 * 用戶可配置設定中心
 */
class SettingsManager(private val context: Context) {
    
    private val CRITIC_LIMIT = intPreferencesKey("critic_limit")

    suspend fun getCriticLimit(): Int = context.dataStore.data.map { it[CRITIC_LIMIT] ?: 3 }.first()
    
    suspend fun getModelSettings(role: String): Pair<String, String> {
        val typeKey = stringPreferencesKey("${role}_model_type")
        val keyKey = stringPreferencesKey("${role}_api_key")
        val prefs = context.dataStore.data.first()
        return Pair(prefs[typeKey] ?: "OpenAI", prefs[keyKey] ?: "")
    }

    suspend fun updateModelSettings(role: String, type: String, key: String) {
        val typeKey = stringPreferencesKey("${role}_model_type")
        val keyKey = stringPreferencesKey("${role}_api_key")
        context.dataStore.edit {
            it[typeKey] = type
            it[keyKey] = key
        }
    }
    
    suspend fun updateCriticLimit(limit: Int) {
        context.dataStore.edit { it[CRITIC_LIMIT] = limit }
    }
}
