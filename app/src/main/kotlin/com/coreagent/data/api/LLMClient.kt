package com.coreagent.data.api

/**
 * 定義 LLM 客戶端介面，用於對接不同的模型服務商
 */
interface LLMClient {
    /**
     * 發送訊息給大模型，並獲取回傳結果
     */
    suspend fun sendMessage(prompt: String): String
}
