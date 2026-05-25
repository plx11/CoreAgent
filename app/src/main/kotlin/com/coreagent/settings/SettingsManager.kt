package com.coreagent.settings

import android.content.Context
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey

class SettingsManager(private val context: Context) {
    
    private val masterKey = MasterKey.Builder(context)
        .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
        .build()

    private val prefs = EncryptedSharedPreferences.create(
        context,
        "secure_settings",
        masterKey,
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
    )

    private val CRITIC_LIMIT = "critic_limit"
    private val REPO_URL = "repo_url"

    fun getCriticLimit(): Int = prefs.getInt(CRITIC_LIMIT, 3)
    fun updateCriticLimit(limit: Int) = prefs.edit().putInt(CRITIC_LIMIT, limit).apply()

    fun getRepoUrl(): String = prefs.getString(REPO_URL, "") ?: ""
    fun updateRepoUrl(url: String) = prefs.edit().putString(REPO_URL, url).apply()
    
    fun getModelSettings(role: String): Pair<String, String> {
        val type = prefs.getString("${role}_model_type", "OpenAI") ?: "OpenAI"
        val key = prefs.getString("${role}_api_key", "") ?: ""
        return Pair(type, key)
    }

    fun updateModelSettings(role: String, type: String, key: String) {
        prefs.edit()
            .putString("${role}_model_type", type)
            .putString("${role}_api_key", key)
            .apply()
    }
}
