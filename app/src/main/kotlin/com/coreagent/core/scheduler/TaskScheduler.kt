package com.coreagent.core.scheduler

import com.coreagent.core.memory.MemoryBank
import com.coreagent.core.memory.TaskStep
import com.coreagent.data.api.LLMClient
import kotlinx.serialization.json.*
import kotlinx.serialization.Serializable
import java.util.UUID

@Serializable
data class LLMTaskPlan(val steps: List<String>)

/**
 * 任務規劃引擎，對接 LLM 進行智能拆解
 */
class TaskScheduler(private val llmClient: LLMClient) {

    @Throws(Exception::class)
    suspend fun scheduleTask(memoryBank: MemoryBank, userInstruction: String) {
        val prompt = """
            請將以下用戶需求拆解為一系列邏輯步驟，並以 JSON 格式回傳，格式範例：{"steps": ["步驟1", "步驟2"]}
            需求: $userInstruction
        """.trimIndent()

        val response = try {
            llmClient.sendMessage(prompt)
        } catch (e: Exception) {
            throw Exception("任務拆解網路請求失敗: ${e.message}", e)
        }
        
        val plan = try {
            val start = response.indexOf("{")
            val end = response.lastIndexOf("}")
            if (start == -1 || end == -1) throw Exception("AI 返回內容不符合 JSON 格式")
            Json.decodeFromString<LLMTaskPlan>(response.substring(start, end + 1))
        } catch (e: Exception) {
            throw Exception("無法解析 AI 任務計畫: ${e.message}", e)
        }

        memoryBank.taskQueue.clear()
        plan.steps.forEach { description ->
            memoryBank.taskQueue.add(TaskStep(UUID.randomUUID().toString(), description))
        }
        memoryBank.globalContext = userInstruction
    }
}
