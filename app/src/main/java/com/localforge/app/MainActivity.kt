package com.localforge.app

import android.content.*
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.localforge.app.service.LocalForgeService
import com.localforge.app.storage.SettingsRepository
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    
    private val logs = mutableStateListOf<String>()
    private val logReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            intent?.getStringExtra("message")?.let { logs.add(it) }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        registerReceiver(logReceiver, IntentFilter("LOCALFORGE_LOG"), RECEIVER_NOT_EXPORTED)
        
        val settings = SettingsRepository(this)

        setContent {
            MaterialTheme {
                Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                    MainScreen(settings, logs)
                }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        unregisterReceiver(logReceiver)
    }
}

@Composable
fun MainScreen(settings: SettingsRepository, logs: SnapshotStateList<String>) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    
    var port by remember { mutableStateOf("8080") }
    var rootUri by remember { mutableStateOf<Uri?>(null) }
    var isRunning by remember { mutableStateOf(false) }
    var autoStart by remember { mutableStateOf(false) }

    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocumentTree()) { uri ->
        if (uri != null) {
            context.contentResolver.takePersistableUriPermission(
                uri, Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
            )
            rootUri = uri
        }
    }

    LaunchedEffect(Unit) {
        settings.port.collect { port = it.toString() }
        settings.rootUri.collect { it?.let { rootUri = Uri.parse(it) } }
        settings.autoStart.collect { autoStart = it }
    }

    Column(modifier = Modifier.padding(16.dp)) {
        Text("LocalForge", style = MaterialTheme.typography.headlineMedium)
        Spacer(modifier = Modifier.height(16.dp))

        OutlinedTextField(
            value = port,
            onValueChange = { port = it },
            label = { Text("Puerto") },
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(8.dp))

        Button(onClick = { launcher.launch(null) }, modifier = Modifier.fillMaxWidth()) {
            Text(if (rootUri == null) "Elegir Carpeta Raíz" else "Carpeta: ${rootUri?.path}")
        }

        Spacer(modifier = Modifier.height(8.dp))

        Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
            Checkbox(checked = autoStart, onCheckedChange = { autoStart = it })
            Text("Auto-start")
        }

        Spacer(modifier = Modifier.height(16.dp))

        Button(
            onClick = {
                if (isRunning) {
                    context.stopService(Intent(context, LocalForgeService::class.java))
                    isRunning = false
                } else {
                    val intent = Intent(context, LocalForgeService::class.java).apply {
                        putExtra("port", port.toIntOrNull() ?: 8080)
                        putExtra("rootUri", rootUri.toString())
                    }
                    context.startForegroundService(intent)
                    isRunning = true
                    scope.launch {
                        settings.saveSettings(port.toIntOrNull() ?: 8080, rootUri.toString(), autoStart)
                    }
                }
            },
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(
                containerColor = if (isRunning) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
            )
        ) {
            Text(if (isRunning) "Stop Server" else "Start Server")
        }

        Spacer(modifier = Modifier.height(16.dp))
        Text("Logs:", style = MaterialTheme.typography.titleMedium)
        LazyColumn(modifier = Modifier.fillMaxSize().weight(1f)) {
            items(logs) { log ->
                Text(log, style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}
