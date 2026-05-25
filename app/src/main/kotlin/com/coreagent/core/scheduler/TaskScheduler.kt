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

    suspend fun scheduleTask(workspaceId: String, memoryDao: MemoryDao, userInstruction: String) {
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
            throw Exception("無法解析任務計畫: ${e.message}")
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
    }

    suspend fun runStep(workspaceId: String, stepId: String, memoryDao: MemoryDao) {
        val step = memoryDao.getTaskStep(stepId) ?: throw Exception("步驟不存在")
        
        val requiredTools = Json.decodeFromString<List<String>>(step.requiredToolsJson)
        for (toolName in requiredTools) {
            if (toolDao.getToolByName(toolName) == null) {
                memoryDao.updateStepStatus(stepId, "PAUSED_FOR_TOOL")
                return
            }
        }

        memoryDao.updateStepStatus(stepId, "RUNNING")

        val workspace = memoryDao.getWorkspace(workspaceId)
        val artifacts = memoryDao.getArtifacts(workspaceId)
        
        val contextPrompt = """
            ### Global Context
            ${workspace?.globalContext ?: ""}
            
            ### Artifacts
            ${artifacts.joinToString("\n") { "${it.artifactName}: ${it.content}" }}
            
            ### Current Task
            ${step.description}
        """.trimIndent()

        // 選擇對應 Agent
        val agentClient = agentRouter.getClientForRole(AgentRole.valueOf(step.assignedAgent.uppercase()))
        var currentOutput = agentClient.sendMessage(contextPrompt)
        
        // 健壯的 JSON 提取
        val start = currentOutput.indexOf("{")
        val end = currentOutput.lastIndexOf("}")
        if (start != -1 && end != -1 && end > start) {
            try {
                val jsonString = currentOutput.substring(start, end + 1)
                val jsonElement = Json.parseToJsonElement(jsonString)
                if (jsonElement is JsonObject && jsonElement.containsKey("tool_call")) {
                    val toolCall = Json.decodeFromJsonElement<ToolCall>(jsonElement["tool_call"]!!)
                    val toolResult = agentService.executeTool(toolCall.toolName)
                    currentOutput = "工具 ${toolCall.toolName} 執行結果: $toolResult"
                }
            } catch (e: Exception) { /* 降級為純文字 */ }
        }
        
        val criticLimit = settingsManager.getCriticLimit()
        repeat(criticLimit) {
            val criticResponse = agentRouter.getClientForRole(AgentRole.CRITIC).sendMessage("審查: $currentOutput")
            if (criticResponse.contains("[PASS]")) return@repeat
            currentOutput = agentRouter.getClientForRole(AgentRole.REFINER).sendMessage("優化: $currentOutput\n意見: $criticResponse")
        }
        
        memoryDao.updateStepResult(stepId, currentOutput)
        memoryDao.updateStepStatus(stepId, "COMPLETED")
    }
}
