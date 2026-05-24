package com.coreagent.billing

import android.content.Context
import com.android.billingclient.api.*
import com.coreagent.BuildConfig
import io.ktor.client.*
import io.ktor.client.request.*
import io.ktor.http.*

class BillingException(message: String) : Exception(message)

interface BillingManager {
    suspend fun hasAccess(userId: String, featureId: String): Boolean
    suspend fun verifyPurchase(purchaseToken: String, productId: String): Boolean
    suspend fun syncWithBackend(userId: String)
}

/**
 * 生產級 BillingManager
 * 接入編譯期配置常量並實作真實後端驗證邏輯
 */
class BillingManagerImpl(
    context: Context,
    private val httpClient: HttpClient
) : BillingManager {
    
    private val productId = BuildConfig.PREMIUM_PRODUCT_ID
    private val webhookUrl = BuildConfig.WEBHOOK_URL

    // 必須接入真實的 Room Database 或 DataStore 檢查訂閱狀態
    override suspend fun hasAccess(userId: String, featureId: String): Boolean {
        throw NotImplementedError("需整合本地 Room Database 訂閱表查詢")
    }

    override suspend fun verifyPurchase(purchaseToken: String, productId: String): Boolean {
        if (productId != this.productId) return false

        val response = httpClient.post(webhookUrl) {
            contentType(ContentType.Application.Json)
            setBody(mapOf("token" to purchaseToken, "productId" to productId))
        }
        
        if (!response.status.isSuccess()) {
            throw BillingException("後端驗證拒絕: ${response.status.value}")
        }
        return true
    }

    override suspend fun syncWithBackend(userId: String) {
        val response = httpClient.post("$webhookUrl/sync") {
            contentType(ContentType.Application.Json)
            setBody(mapOf("userId" to userId))
        }
        if (!response.status.isSuccess()) {
            throw BillingException("同步失敗: ${response.status.value}")
        }
    }
}
