package com.coreagent.billing

import android.content.Context
import com.coreagent.core.memory.MemoryDao
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey

interface BillingManager {
    suspend fun hasAccess(workspaceId: String, memoryDao: MemoryDao): Boolean
    suspend fun verifyPurchase(purchaseToken: String, productId: String)
}

class LocalBillingManager(context: Context) : BillingManager {
    
    private val masterKey = MasterKey.Builder(context)
        .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
        .build()

    private val sharedPreferences = EncryptedSharedPreferences.create(
        context,
        "billing_prefs",
        masterKey,
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
    )

    override suspend fun hasAccess(workspaceId: String, memoryDao: MemoryDao): Boolean {
        val isPremium = sharedPreferences.getBoolean("is_premium", false)
        if (isPremium) return true
        
        // 核心邏輯：如果空間已存在，直接返回 true 放行；若是新建空間，檢查總數是否 < 3
        if (memoryDao.workspaceExists(workspaceId)) return true
        
        return memoryDao.getWorkspaceCount() < 3
    }

    override suspend fun verifyPurchase(purchaseToken: String, productId: String) {
        sharedPreferences.edit().putBoolean("is_premium", true).apply()
    }
}
