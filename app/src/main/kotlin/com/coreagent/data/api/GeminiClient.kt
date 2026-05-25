package com.coreagent.data.api

import io.ktor.client.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import kotlinx.serialization.json.*

/**
 * 基於 Google Gemini API 的 LLM 客戶端實作
 */
class GeminiClient(
    private val apiKey: String,
    private val httpClient: HttpClient
) : LLMClient {

    override suspend fun sendMessage(prompt: String): String {
        return try {
            // Gemini API Endpoint (v1beta)
            val url = "https://generativelanguage.googleapis.com/v1beta/models/gemini-pro:generateContent?key=$apiKey"
            
            val response = httpClient.post(url) {
                contentType(ContentType.Application.Json)
                setBody(buildJsonObject {
                    putJsonArray("contents") {
                        addJsonObject {
                            putJsonArray("parts") {
                                addJsonObject {
                                    put("text", prompt)
                                }
                            }
                        }
                    }
                }.toString())
            }

            if (!response.status.isSuccess()) {
                throw Exception("Gemini API 請求失敗: ${response.status} - ${response.bodyAsText()}")
            }
            
            response.bodyAsText()
        } catch (e: Exception) {
            throw Exception("Gemini Client 發生錯誤: ${e.message}", e)
        }
    }
}
