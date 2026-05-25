package com.coreagent.core.scheduler

import com.coreagent.core.engine.AgentService
import com.coreagent.core.memory.MemoryDao
import com.coreagent.core.memory.TaskStepEntity
import com.coreagent.core.router.AgentRole
import com.coreagent.core.router.AgentRouter
import com.coreagent.data.tool.ToolDao
import com.coreagent.settings.SettingsManager
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.*
import java.util.UUID

@Serializable
data class SerializableStep(val description: String, val assignedAgent: String, val requiredTools: List<String>)

@Serializable
data class LLMTaskPlan(val steps: List<SerializableStep>)

@Serializable
data class ToolCall(val toolName: String, val arguments: JsonObject)

class TaskScheduler(
    private val agentRouter: AgentRouter,
    private val settingsManager: SettingsManager,
    private val agentService: AgentService,
    private val toolDao: ToolDao
) {

import com.coreagent.core.memory.ProducedArtifactEntity
// ...

class TaskSchedulingException(message: String, cause: Throwable? = null) : Exception(message, cause)

// ... inside TaskScheduler class
    suspend fun scheduleTask(workspaceId: String, memoryDao: MemoryDao, userInstruction: String) {
        var retries = 0
        while (true) {
            try {
                val schedulerClient = agentRouter.getClientForRole(AgentRole.SCHEDULER)
                val prompt = """
                    請將以下用戶需求拆解為一系列邏輯步驟。
                    必須為每個步驟指派適合的 Agent Persona (如: CHEF, STORE_MANAGER) 並列出所需工具。
                    需求: $userInstruction
                    回傳格式範例: {"steps": [{"description": "...", "assignedAgent": "...", "requiredTools": ["..."]}]}
                """.trimIndent()

                val response = schedulerClient.sendMessage(prompt)
                
                val plan = try {
                    val start = response.indexOf("{")
                    val end = response.lastIndexOf("}")
                    Json.decodeFromString<LLMTaskPlan>(response.substring(start, end + 1))
                } catch (e: Exception) {
                    throw Exception("解析失敗")
                }

                val stepEntities = plan.steps.map { step ->
                    TaskStepEntity(
                        stepId = UUID.randomUUID().toString(),
                        workspaceId = workspaceId,
                        description = step.description,
                        status = "PENDING",
                        result = null,
                        dependenciesJson = "[]",
                        assignedAgent = step.assignedAgent,
                        requiredToolsJson = Json.encodeToString(step.requiredTools)
                    )
                }
                memoryDao.insertTaskSteps(stepEntities)
                return // Success
            } catch (e: Exception) {
                if (retries >= 2) throw TaskSchedulingException("任務拆解失敗，重試次數超過上限", e)
                retries++
            }
        }
    }

    suspend fun runStep(workspaceId: String, stepId: String, memoryDao: MemoryDao) {
        // ... (existing tool check and execution)
        if (match != null) {
            try {
                val jsonElement = Json.parseToJsonElement(match.value)
                if (jsonElement is JsonObject && jsonElement.containsKey("tool_call")) {
                    val toolCall = Json.decodeFromJsonElement<ToolCall>(jsonElement["tool_call"]!!)
                    val toolResult = agentService.executeTool(toolCall.toolName)
                    currentOutput = "工具 ${toolCall.toolName} 執行結果: $toolResult"
                    
                    // 保存產出物
                    memoryDao.insertArtifact(ProducedArtifactEntity(
                        workspaceId = workspaceId,
                        stepId = stepId,
                        artifactName = toolCall.toolName,
                        content = toolResult
                    ))
                }
            } catch (e: Exception) { /* 降級為純文字 */ }
        }
        // ...
}
