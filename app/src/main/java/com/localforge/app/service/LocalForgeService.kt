package com.localforge.app.service

import android.app.*
import android.content.Intent
import android.net.Uri
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.localforge.app.MainActivity
import com.localforge.app.server.LocalServer
import com.localforge.app.tools.WorkspaceManager

class LocalForgeService : Service() {

    private var server: LocalServer? = null
    private val CHANNEL_ID = "LocalForgeChannel"

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val port = intent?.getIntExtra("port", 8080) ?: 8080
        val uriString = intent?.getStringExtra("rootUri")
        
        if (uriString != null) {
            val rootUri = Uri.parse(uriString)
            val workspaceManager = WorkspaceManager(this, rootUri)
            
            createNotificationChannel()
            val notification = NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("LocalForge Server")
                .setContentText("Servidor corriendo en puerto $port")
                .setSmallIcon(android.R.drawable.ic_dialog_info)
                .build()

            startForeground(1, notification)
            
            server = LocalServer(port, workspaceManager) { log ->
                // Broadcast log to Activity if needed
                val logIntent = Intent("LOCALFORGE_LOG").apply {
                    putExtra("message", log)
                }
                sendBroadcast(logIntent)
            }
            server?.start()
        }

        return START_STICKY
    }

    override fun onDestroy() {
        server?.stop()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun createNotificationChannel() {
        val serviceChannel = NotificationChannel(
            CHANNEL_ID,
            "LocalForge Service Channel",
            NotificationManager.IMPORTANCE_DEFAULT
        )
        val manager = getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(serviceChannel)
    }
}
