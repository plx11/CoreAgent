package com.coreagent.data.api

import io.ktor.client.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import kotlinx.serialization.json.*

/**
 * 基於 OpenAI API 的 LLM 客戶端實作
 */
class OpenAIClient(
    private val apiKey: String,
    private val httpClient: HttpClient
) : LLMClient {

    override suspend fun sendMessage(prompt: String): String {
        return try {
            val response = httpClient.post("https://api.openai.com/v1/chat/completions") {
                header(HttpHeaders.Authorization, "Bearer $apiKey")
                contentType(ContentType.Application.Json)
                setBody(buildJsonObject {
                    put("model", "gpt-4")
                    putJsonArray("messages") {
                        addJsonObject {
                            put("role", "user")
                            put("content", prompt)
                        }
                    }
                }.toString())
            }

            if (!response.status.isSuccess()) {
                throw Exception("OpenAI API 請求失敗: ${response.status} - ${response.bodyAsText()}")
            }
            
            response.bodyAsText()
        } catch (e: Exception) {
            throw Exception("OpenAI Client 發生錯誤: ${e.message}", e)
        }
    }
}
