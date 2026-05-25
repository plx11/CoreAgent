package com.coreagent.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.coreagent.core.tool.ToolManager
import com.coreagent.ui.viewmodel.ChatViewModel
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(viewModel: ChatViewModel, toolManager: ToolManager) {
    val memoryBank by viewModel.memoryBank.collectAsState()
    val messages by viewModel.messages.collectAsState()
    var isAccordionExpanded by remember { mutableStateOf(false) }
    var text by remember { mutableStateOf("") }
    val showPaywall by viewModel.showPaywallDialog.collectAsState()
    val scope = rememberCoroutineScope()

    if (showPaywall) AlertDialog(onDismissRequest = { viewModel.dismissPaywall() }, title = { Text("付費") }, confirmButton = { Button(onClick = { viewModel.dismissPaywall() }) { Text("OK") } })

    val pausedStep = memoryBank.find { it.status == "PAUSED_FOR_TOOL" }
    
    Column(modifier = Modifier.fillMaxSize()) {
        Card(modifier = Modifier.fillMaxWidth().padding(8.dp), onClick = { isAccordionExpanded = !isAccordionExpanded }) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("記憶庫進度看板", style = MaterialTheme.typography.titleMedium)
                AnimatedVisibility(visible = isAccordionExpanded) {
                    LazyColumn(modifier = Modifier.heightIn(max = 200.dp)) {
                        items(memoryBank) { step ->
                            Text("- ${step.description} (${step.status} | Agent: ${step.assignedAgent})")
                        }
                    }
                }
            }
        }
        
        LazyColumn(modifier = Modifier.weight(1f)) {
            items(messages) { msg ->
                Card(modifier = Modifier.padding(8.dp)) {
                    Column(modifier = Modifier.padding(8.dp)) {
                        Text(msg.role, style = MaterialTheme.typography.labelSmall)
                        Text(msg.content)
                        Text("來源: ${msg.citationsJson}", style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
        }
        
        OutlinedTextField(value = text, onValueChange = { text = it }, modifier = Modifier.fillMaxWidth().padding(8.dp), trailingIcon = { Button(onClick = { viewModel.sendMessage(text); text = "" }) { Text("發送") } })
    }

    if (pausedStep != null) {
        ModalBottomSheet(onDismissRequest = {}) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("缺失工具: ${pausedStep.description}")
                Button(onClick = {
                    scope.launch {
                        // 這裡假設我們有 SchemaUrl，真實開發中應該從工具詳細資料獲取
                        val url = "https://api.coreagent.com/tools/${pausedStep.description}.json"
                        try {
                            toolManager.installTool(url)
                        } catch (e: Exception) {
                            // 錯誤處理
                        }
                    }
                }) { Text("下載") }
            }
        }
    }
}
