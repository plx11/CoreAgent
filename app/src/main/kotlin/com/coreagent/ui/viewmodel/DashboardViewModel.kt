package com.coreagent.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.coreagent.billing.BillingManager
import com.coreagent.core.memory.MemoryDao
import com.coreagent.core.memory.WorkspaceEntity
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.UUID

class DashboardViewModel(
    private val memoryDao: MemoryDao,
    private val billingManager: BillingManager
) : ViewModel() {
    
    val workspaces: StateFlow<List<WorkspaceEntity>> = memoryDao.getAllWorkspacesFlow()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    private val _showPaywallDialog = MutableStateFlow(false)
    val showPaywallDialog: StateFlow<Boolean> = _showPaywallDialog

    fun createNewWorkspace(name: String) {
        viewModelScope.launch {
            val id = UUID.randomUUID().toString()
            if (billingManager.hasAccess(id, memoryDao)) {
                val workspace = WorkspaceEntity(id, name)
                memoryDao.insertWorkspace(workspace)
            } else {
                _showPaywallDialog.value = true
            }
        }
    }

    fun dismissPaywall() {
        _showPaywallDialog.value = false
    }
}
