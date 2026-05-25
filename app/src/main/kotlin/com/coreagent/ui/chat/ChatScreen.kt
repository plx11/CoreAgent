package com.coreagent.ui.chat

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.coreagent.ui.viewmodel.ChatViewModel
import kotlinx.serialization.json.Json

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(viewModel: ChatViewModel) {
    val memoryBank by viewModel.memoryBank.collectAsState()
    val messages by viewModel.messages.collectAsState()
    var isAccordionExpanded by remember { mutableStateOf(false) }
    var text by remember { mutableStateOf("") }
    val showPaywall by viewModel.showPaywallDialog.collectAsState()

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

import com.coreagent.core.tool.ToolManager
import androidx.compose.material3.CircularProgressIndicator

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(viewModel: ChatViewModel, toolManager: ToolManager) { // 注入 toolManager
    // ...
    val pausedStep = memoryBank.find { it.status == "PAUSED_FOR_TOOL" }
    var isDownloading by remember { mutableStateOf(false) }

    // ...
    if (pausedStep != null) {
        ModalBottomSheet(onDismissRequest = { /* ... */ }) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("缺失工具: ${pausedStep.description}")
                Button(
                    onClick = {
                        isDownloading = true
                        scope.launch {
                            try {
                                // 假設 schemaUrl 可以從某處獲取，這裡暫用模擬
                                toolManager.installTool("https://api.coreagent.com/tools/ops.json")
                                isDownloading = false
                                // 重新刷新界面 logic
                            } catch (e: Exception) {
                                isDownloading = false
                            }
                        }
                    },
                    enabled = !isDownloading
                ) {
                    if (isDownloading) CircularProgressIndicator(modifier = Modifier.size(20.dp))
                    else Text("下載")
                }
            }
        }
    }
}
