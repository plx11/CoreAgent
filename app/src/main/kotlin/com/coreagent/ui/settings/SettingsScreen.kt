package com.coreagent.ui.settings

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.coreagent.settings.SettingsManager
import kotlinx.coroutines.launch

/**
 * 設定頁面 (用戶可修改區)
 */
@Composable
fun SettingsScreen(settingsManager: SettingsManager) {
    val scope = rememberCoroutineScope()
    val apiKey by settingsManager.apiKey.collectAsState(initial = "")
    val criticLimit by settingsManager.criticLimit.collectAsState(initial = 3)
    
    var keyInput by remember(apiKey) { mutableStateOf(apiKey ?: "") }
    var limitInput by remember(criticLimit) { mutableStateOf(criticLimit.toString()) }

    Column(modifier = Modifier.padding(16.dp)) {
        Text("用戶設定", style = MaterialTheme.typography.headlineMedium)
        Spacer(modifier = Modifier.height(16.dp))
        
        OutlinedTextField(
            value = keyInput,
            onValueChange = { keyInput = it },
            label = { Text("大模型 API Key") },
            modifier = Modifier.fillMaxWidth()
        )
        
        OutlinedTextField(
            value = limitInput,
            onValueChange = { limitInput = it },
            label = { Text("反思循環上限 (1-5)") },
            modifier = Modifier.fillMaxWidth()
        )
        
        Button(
            onClick = { 
                scope.launch { 
                    settingsManager.updateApiKey(keyInput)
                    settingsManager.updateCriticLimit(limitInput.toIntOrNull() ?: 3)
                } 
            },
            modifier = Modifier.padding(top = 8.dp)
        ) {
            Text("儲存設定")
        }
    }
}
