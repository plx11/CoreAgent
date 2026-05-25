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
    // 動態觀察資料庫中的所有工具狀態
    val tools by toolDao.getAllToolsFlow().collectAsState(initial = emptyList())
    
    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Text("外掛工具市場", style = MaterialTheme.typography.headlineMedium)
        
        LazyVerticalGrid(columns = GridCells.Fixed(2), modifier = Modifier.fillMaxSize()) {
            items(tools) { tool ->
                Card(modifier = Modifier.padding(8.dp)) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(tool.name, style = MaterialTheme.typography.titleMedium)
                        Text("狀態: 已安裝", style = MaterialTheme.typography.bodySmall)
                        // 進度條與狀態綁定
                        LinearProgressIndicator(progress = 1f, modifier = Modifier.fillMaxWidth())
                        
                        Button(onClick = { /* 工具已安裝，顯示配置或更新 */ }) {
                            Text("已安裝")
                        }
                    }
                }
            }
        }
    }
}
