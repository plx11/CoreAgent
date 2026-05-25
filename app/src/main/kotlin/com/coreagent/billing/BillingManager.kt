package com.coreagent.billing

import android.content.Context
import com.coreagent.core.memory.MemoryDao
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow

interface BillingManager {
    val premiumStatusFlow: SharedFlow<Boolean>
    suspend fun hasAccess(workspaceId: String, memoryDao: MemoryDao): Boolean
    suspend fun verifyPurchase(purchaseToken: String, productId: String)
}

class LocalBillingManager(context: Context) : BillingManager {
    
    private val _premiumStatusFlow = MutableSharedFlow<Boolean>(replay = 1)
    override val premiumStatusFlow: SharedFlow<Boolean> = _premiumStatusFlow
    
    // ... (existing masterKey and sharedPreferences initialization)
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
    
    init {
        // Initialize flow with current state
        _premiumStatusFlow.tryEmit(sharedPreferences.getBoolean("is_premium", false))
    }

    override suspend fun hasAccess(workspaceId: String, memoryDao: MemoryDao): Boolean {
        val isPremium = sharedPreferences.getBoolean("is_premium", false)
        if (isPremium) return true
        
        if (memoryDao.workspaceExists(workspaceId)) return true
        
        return memoryDao.getWorkspaceCount() < 3
    }

    override suspend fun verifyPurchase(purchaseToken: String, productId: String) {
        sharedPreferences.edit().putBoolean("is_premium", true).apply()
        _premiumStatusFlow.emit(true)
    }
}
