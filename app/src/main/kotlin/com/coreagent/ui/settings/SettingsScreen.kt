package com.coreagent.ui.settings

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.coreagent.settings.SettingsManager
import kotlinx.coroutines.launch

@Composable
fun SettingsScreen(settingsManager: SettingsManager) {
    val roles = listOf("scheduler", "critic", "refiner")
    val scope = rememberCoroutineScope()
    
    var settings by remember { mutableStateOf(mapOf<String, Pair<String, String>>()) }
    var criticLimit by remember { mutableFloatStateOf(3f) }
    var repoUrl by remember { mutableStateOf("") }
    
    LaunchedEffect(Unit) {
        val loaded = roles.associateWith { settingsManager.getModelSettings(it) }
        settings = loaded
        criticLimit = settingsManager.getCriticLimit().toFloat()
    }

    LazyColumn(modifier = Modifier.padding(16.dp)) {
        item { Text("系統設定", style = MaterialTheme.typography.headlineMedium) }
        items(roles.size) { index ->
            val role = roles[index]
            val (type, key) = settings[role] ?: Pair("OpenAI", "")
            Text(text = "角色: ${role.uppercase()}", style = MaterialTheme.typography.titleMedium)
            OutlinedTextField(value = type, onValueChange = { settings = settings + (role to Pair(it, key)) }, label = { Text("Model Type") })
            OutlinedTextField(value = key, onValueChange = { settings = settings + (role to Pair(type, it)) }, label = { Text("API Key") })
        }
        item {
            Text("反思循環上限: ${criticLimit.toInt()}")
            Slider(value = criticLimit, onValueChange = { criticLimit = it }, valueRange = 1f..5f, steps = 3)
            OutlinedTextField(value = repoUrl, onValueChange = { repoUrl = it }, label = { Text("自定義工具源網址") })
            Text("免責聲明: 本 App 為純本地工具，不收集用戶隱私數據。", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
            Button(onClick = {
                scope.launch {
                    settings.forEach { (role, pair) -> settingsManager.updateModelSettings(role, pair.first, pair.second) }
                    settingsManager.updateCriticLimit(criticLimit.toInt())
                }
            }) { Text("儲存設定") }
        }
    }
}
