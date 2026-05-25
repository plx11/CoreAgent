package com.coreagent.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.coreagent.core.tool.ToolManager
import kotlinx.coroutines.launch

@Composable
fun ToolMarketplaceScreen(toolManager: ToolManager) {
    val scope = rememberCoroutineScope()
    // 應從ToolDao讀取實際狀態，這裡簡化
    val tools = listOf(mapOf("name" to "自動運維", "url" to "https://api.coreagent.com/tools/ops.json"))
    
    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Text("外掛工具市場", style = MaterialTheme.typography.headlineMedium)
        LazyVerticalGrid(columns = GridCells.Fixed(2), modifier = Modifier.fillMaxSize()) {
            items(tools) { tool ->
                Card(modifier = Modifier.padding(8.dp)) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(tool["name"]!!)
                        Text("狀態: 未安裝", style = MaterialTheme.typography.bodySmall)
                        LinearProgressIndicator(progress = 0f, modifier = Modifier.fillMaxWidth())
                        Button(onClick = { scope.launch { toolManager.installTool(tool["url"]!!) } }) { Text("下載") }
                    }
                }
            }
        }
    }
}
