package com.coreagent.core.memory

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.encodeToString
import java.io.*

@Serializable
enum class TaskStatus { PENDING, RUNNING, COMPLETED, FAILED }

@Serializable
data class TaskStep(
    val stepId: String,
    val description: String,
    var status: TaskStatus = TaskStatus.PENDING
)

@Serializable
data class MemoryBank(
    var globalContext: String = "",
    val taskQueue: MutableList<TaskStep> = mutableListOf()
) {

    fun save(directory: File) {
        val file = File(directory, "memory.json")
        val jsonString = Json { prettyPrint = true }.encodeToString(this)
        
        // 使用暫存檔實現原子寫入，防止檔案損壞
        val tempFile = File(directory, "memory.json.tmp")
        tempFile.writeText(jsonString)
        
        if (!tempFile.renameTo(file)) {
            throw IOException("無法將 MemoryBank 原子寫入至: ${file.absolutePath}")
        }
    }

    companion object {
        fun load(directory: File): MemoryBank {
            val file = File(directory, "memory.json")
            if (!file.exists()) return MemoryBank()
            
            return try {
                Json.decodeFromString<MemoryBank>(file.readText())
            } catch (e: Exception) {
                throw IOException("記憶檔格式錯誤或已損壞: ${file.absolutePath}", e)
            }
        }
    }
}
