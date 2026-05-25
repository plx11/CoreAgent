package com.coreagent.data.api

import io.ktor.client.*

/**
 * LLM 客戶端工廠，用於根據類型建立相應的模型客戶端
 */
object LLMClientFactory {
    fun createClient(modelType: String, apiKey: String, httpClient: HttpClient): LLMClient {
        return when (modelType) {
            "OpenAI" -> OpenAIClient(apiKey, httpClient)
            "Gemini" -> GeminiClient(apiKey, httpClient)
            else -> throw IllegalArgumentException("不支援的模型類型: $modelType")
        }
    }
}
