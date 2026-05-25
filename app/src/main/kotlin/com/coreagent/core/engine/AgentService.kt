package com.coreagent.core.engine

import com.coreagent.data.tool.ToolDao
import java.io.File
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.JsonObject

class AgentService(
    private val executionEngine: ExecutionEngine,
    private val toolDao: ToolDao
) {
    suspend fun executeTool(toolName: String, arguments: JsonObject): String {
        val tool = toolDao.getToolByName(toolName) ?: throw Exception("找不到工具: $toolName")
        val scriptFile = File(tool.localPath)
        if (!scriptFile.exists()) throw Exception("工具腳本不存在: ${tool.localPath}")
        
        val script = scriptFile.readText()
        val argsJson = kotlinx.serialization.json.Json.encodeToString(arguments)
        return executionEngine.execute(script, argsJson)
    }
}
