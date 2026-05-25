package com.coreagent.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.coreagent.ui.viewmodel.DashboardViewModel

@Composable
fun DashboardScreen(viewModel: DashboardViewModel, onNavigateToChat: (String) -> Unit) {
    val workspaces by viewModel.workspaces.collectAsState()
    var showDialog by remember { mutableStateOf(false) }
    var newWorkspaceName by remember { mutableStateOf("") }
    
    if (showDialog) {
        AlertDialog(
            onDismissRequest = { showDialog = false },
            title = { Text("新建工作空間") },
            text = { OutlinedTextField(value = newWorkspaceName, onValueChange = { newWorkspaceName = it }, label = { Text("空間名稱") }) },
            confirmButton = {
                Button(onClick = { 
                    viewModel.createNewWorkspace(newWorkspaceName)
                    showDialog = false
                    newWorkspaceName = ""
                }) { Text("建立") }
            }
        )
    }
    
    Scaffold(
        floatingActionButton = {
            FloatingActionButton(onClick = { showDialog = true }) {
                Icon(Icons.Default.Add, contentDescription = "Add")
            }
        }
    ) { padding ->
        LazyColumn(modifier = Modifier.padding(padding).fillMaxSize()) {
            items(workspaces) { workspace ->
                Card(
                    modifier = Modifier.padding(8.dp).fillMaxWidth(),
                    onClick = { onNavigateToChat(workspace.workspaceId) }
                ) {
                    Text(text = "Workspace: ${workspace.globalContext}", modifier = Modifier.padding(16.dp))
                }
            }
        }
    }
}
