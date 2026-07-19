package com.localforge.app

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.Uri
import android.os.Build
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
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

        // Register receiver safely for all API levels
        val filter = IntentFilter("LOCALFORGE_LOG")
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(logReceiver, filter, RECEIVER_NOT_EXPORTED)
        } else {
            registerReceiver(logReceiver, filter)
        }

        val settings = SettingsRepository(this)

        setContent {
            MaterialTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    MainScreen(settings, logs)
                }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        try {
            unregisterReceiver(logReceiver)
        } catch (_: IllegalArgumentException) {
            // Receiver not registered
        }
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

    val launcher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocumentTree()
    ) { uri ->
        if (uri != null) {
            // Take persistable permissions
            try {
                context.contentResolver.takePersistableUriPermission(
                    uri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION or
                            Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                )
            } catch (_: SecurityException) {
                // Some providers don't support persistable permissions
            }
            rootUri = uri
        }
    }

    // Load saved settings
    LaunchedEffect(Unit) {
        settings.port.collect { port = it.toString() }
        settings.rootUri.collect { uriStr ->
            uriStr?.let { rootUri = Uri.parse(it) }
        }
        settings.autoStart.collect { autoStart = it }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text(
            "LocalForge",
            style = MaterialTheme.typography.headlineMedium
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            "Local AI Server",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(16.dp))

        // Port input
        OutlinedTextField(
            value = port,
            onValueChange = { newValue ->
                // Only allow digits
                if (newValue.all { it.isDigit() } && newValue.length <= 5) {
                    port = newValue
                }
            },
            label = { Text("Puerto") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )

        Spacer(modifier = Modifier.height(8.dp))

        // Folder picker
        Button(
            onClick = { launcher.launch(null) },
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.outlinedButtonColors()
        ) {
            Text(
                if (rootUri == null) "Elegir Carpeta Raíz"
                else "Carpeta: ${rootUri?.lastPathSegment ?: rootUri?.path}"
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Auto-start checkbox
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            Checkbox(
                checked = autoStart,
                onCheckedChange = {
                    autoStart = it
                    scope.launch { settings.saveAutoStart(it) }
                }
            )
            Text("Auto-start on boot")
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Start/Stop button
        Button(
            onClick = {
                if (isRunning) {
                    // STOP
                    val stopIntent = Intent(context, LocalForgeService::class.java).apply {
                        action = LocalForgeService.ACTION_STOP
                    }
                    context.startService(stopIntent)
                    isRunning = false
                } else {
                    // START
                    val selectedPort = port.toIntOrNull() ?: 8080
                    val selectedUri = rootUri?.toString()
                    if (selectedUri == null) {
                        logs.add("ERROR: Selecciona una carpeta primero")
                        return@Button
                    }
                    val startIntent = Intent(context, LocalForgeService::class.java).apply {
                        action = LocalForgeService.ACTION_START
                        putExtra(LocalForgeService.EXTRA_PORT, selectedPort)
                        putExtra(LocalForgeService.EXTRA_ROOT_URI, selectedUri)
                    }
                    context.startForegroundService(startIntent)
                    isRunning = true
                    // Save settings
                    scope.launch {
                        settings.saveSettings(selectedPort, selectedUri, autoStart)
                    }
                }
            },
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(
                containerColor = if (isRunning)
                    MaterialTheme.colorScheme.error
                else
                    MaterialTheme.colorScheme.primary
            ),
            enabled = rootUri != null || isRunning
        ) {
            Text(if (isRunning) "Stop Server" else "Start Server")
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Logs section
        Text("Logs:", style = MaterialTheme.typography.titleMedium)
        Spacer(modifier = Modifier.height(4.dp))

        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
        ) {
            items(logs) { log ->
                Text(
                    log,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(vertical = 1.dp)
                )
            }
        }
    }
}
