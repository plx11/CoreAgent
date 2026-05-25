package com.coreagent.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.coreagent.billing.BillingManager
import com.coreagent.core.engine.AgentService
import com.coreagent.core.memory.MemoryDao
import com.coreagent.core.memory.TaskStepEntity
import com.coreagent.core.router.AgentRouter
import com.coreagent.core.scheduler.TaskScheduler
import com.coreagent.settings.SettingsManager
import com.coreagent.data.tool.ToolDao
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class ChatViewModel(
    private val workspaceId: String,
    private val memoryDao: MemoryDao,
    private val agentRouter: AgentRouter,
    private val settingsManager: SettingsManager,
    private val agentService: AgentService,
    private val billingManager: BillingManager,
    private val toolDao: ToolDao
) : ViewModel() {
    
    private val scheduler = TaskScheduler(agentRouter, settingsManager, agentService, toolDao)
    
    val memoryBank: StateFlow<List<TaskStepEntity>> = memoryDao.getWorkspaceStepsFlow(workspaceId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
        
    val messages: StateFlow<List<com.coreagent.core.memory.ChatMessageEntity>> = memoryDao.getMessagesFlow(workspaceId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _showPaywallDialog = MutableStateFlow(false)
    val showPaywallDialog: StateFlow<Boolean> = _showPaywallDialog

// ... inside ChatViewModel
    val isPremium = billingManager.premiumStatusFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    fun sendMessage(userInstruction: String) {
        viewModelScope.launch {
            if (!billingManager.hasAccess(workspaceId, memoryDao)) {
                _showPaywallDialog.value = true
                return@launch
            }
            // ...

    private suspend fun executeTaskLoop() {
        while (true) {
            val nextStep = memoryDao.getNextPendingStep(workspaceId) ?: break
                        
            try {
                scheduler.runStep(workspaceId, nextStep.stepId, memoryDao)
            } catch (e: Exception) {
                memoryDao.updateStepStatus(nextStep.stepId, "FAILED")
                break
            }
        }
    }
    
    fun dismissPaywall() {
        _showPaywallDialog.value = false
    }
}
