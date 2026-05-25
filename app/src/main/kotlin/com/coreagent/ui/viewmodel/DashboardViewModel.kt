package com.coreagent.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.coreagent.core.memory.MemoryDao
import com.coreagent.core.memory.WorkspaceEntity
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.UUID

class DashboardViewModel(private val memoryDao: MemoryDao) : ViewModel() {
    
    val workspaces: StateFlow<List<WorkspaceEntity>> = memoryDao.getAllWorkspacesFlow()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    fun createNewWorkspace(name: String) {
        viewModelScope.launch {
            val id = UUID.randomUUID().toString()
            val workspace = WorkspaceEntity(id, name)
            memoryDao.insertWorkspace(workspace)
        }
    }
}
