package com.coreagent.core.router

import com.coreagent.data.api.LLMClient
import com.coreagent.data.api.LLMClientFactory
import com.coreagent.settings.SettingsManager
import io.ktor.client.HttpClient

enum class AgentRole { SCHEDULER, CRITIC, REFINER }

class AgentRouter(
    private val httpClient: HttpClient,
    private val settingsManager: SettingsManager
) {
    suspend fun getClientForRole(role: AgentRole): LLMClient {
        val (modelType, apiKey) = settingsManager.getModelSettings(role.name.lowercase())
        return LLMClientFactory.createClient(modelType, apiKey, httpClient)
    }
}
