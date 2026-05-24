package com.coreagent.core.workspace

import java.util.UUID
import java.io.File
import java.io.IOException

/**
 * 負責管理多任務空間 (Multi-Workspace)
 * 確保每個 workspace 擁有隔離的執行環境，並具備嚴格的 IO 錯誤處理
 */
class WorkspaceManager(private val baseDir: File) {

    @Throws(IOException::class)
    fun createWorkspace(): String {
        val workspaceId = UUID.randomUUID().toString()
        val workspaceDir = File(baseDir, workspaceId)
        
        if (!workspaceDir.exists()) {
            if (!workspaceDir.mkdirs()) {
                throw IOException("無法創建 Workspace 目錄: ${workspaceDir.absolutePath}")
            }
        }
        
        if (!workspaceDir.canWrite()) {
            throw IOException("目錄無寫入權限: ${workspaceDir.absolutePath}")
        }
        
        return workspaceId
    }

    fun getWorkspaceDir(workspaceId: String): File {
        return File(baseDir, workspaceId)
    }
}
