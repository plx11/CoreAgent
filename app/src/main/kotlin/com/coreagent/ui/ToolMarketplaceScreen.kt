package com.coreagent.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.coreagent.core.tool.ToolManager
import com.coreagent.data.tool.ToolDao
import kotlinx.coroutines.launch

@Composable
fun ToolMarketplaceScreen(toolManager: ToolManager, toolDao: ToolDao) {
    val scope = rememberCoroutineScope()
    val tools by toolDao.getAllToolsFlow().collectAsState(initial = emptyList())
    
    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Text("外掛工具市場", style = MaterialTheme.typography.headlineMedium)
        
        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(8.dp)
        ) {
            items(tools) { tool ->
                Card(modifier = Modifier.padding(8.dp).fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(text = tool.name, style = MaterialTheme.typography.titleMedium)
                        
                        val statusText = when (tool.status) {
                            "DOWNLOADING" -> "下載中: ${tool.downloadProgress}%"
                            "INSTALLED" -> "狀態: 已激活"
                            else -> "狀態: 未安裝"
                        }
                        
                        Text(text = statusText, style = MaterialTheme.typography.bodySmall)
                        
                        if (tool.status == "DOWNLOADING") {
                            LinearProgressIndicator(
                                progress = tool.downloadProgress / 100f,
                                modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)
                            )
                        }
                        
                        Button(
                            onClick = {
                                scope.launch {
                                    toolManager.installTool(tool.downloadUrl)
                                }
                            },
                            enabled = tool.status == "NOT_INSTALLED"
                        ) {
                            Text(when (tool.status) {
                                "DOWNLOADING" -> "請稍候"
                                "INSTALLED" -> "已啟用"
                                else -> "下載"
                            })
                        }
                    }
                }
            }
        }
    }
}
