package com.localforge.app.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.content.pm.ServiceInfo
import android.net.Uri
import android.os.Binder
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.localforge.app.MainActivity
import com.localforge.app.R
import com.localforge.app.server.LocalServer
import com.localforge.app.tools.WorkspaceManager
import java.util.concurrent.atomic.AtomicBoolean

class LocalForgeService : Service() {

    companion object {
        const val CHANNEL_ID = "localforge_service"
        const val NOTIFICATION_ID = 1
        const val ACTION_START = "com.localforge.app.START"
        const val ACTION_STOP = "com.localforge.app.STOP"
        const val EXTRA_PORT = "port"
        const val EXTRA_ROOT_URI = "root_uri"

        private val _isRunning = AtomicBoolean(false)
        val isRunning: Boolean get() = _isRunning.get()
    }

    private var server: LocalServer? = null
    private var currentPort: Int = 8080
    private val binder = LocalBinder()

    inner class LocalBinder : Binder() {
        fun getService(): LocalForgeService = this@LocalForgeService
    }

    override fun onBind(intent: Intent?): IBinder = binder

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_STOP -> {
                stopServer()
                stopForeground(STOP_FOREGROUND_REMOVE)
                stopSelf()
                return START_NOT_STICKY
            }
            ACTION_START -> {
                val port = intent.getIntExtra(EXTRA_PORT, 8080)
                val uriString = intent.getStringExtra(EXTRA_ROOT_URI)
                if (uriString != null) {
                    startServer(port, Uri.parse(uriString))
                } else {
                    log("ERROR: No root URI provided")
                    stopSelf()
                }
            }
            else -> {
                // Direct start (no action set) — read extras directly
                val port = intent?.getIntExtra(EXTRA_PORT, 8080) ?: 8080
                val uriString = intent?.getStringExtra(EXTRA_ROOT_URI)
                if (uriString != null) {
                    startServer(port, Uri.parse(uriString))
                } else {
                    log("ERROR: No root URI provided")
                    stopSelf()
                }
            }
        }
        return START_STICKY
    }

    override fun onDestroy() {
        stopServer()
        super.onDestroy()
    }

    private fun startServer(port: Int, rootUri: Uri) {
        try {
            val workspaceManager = WorkspaceManager(this, rootUri)

            if (!workspaceManager.isWorkspaceValid()) {
                log("ERROR: Invalid workspace at $rootUri")
                stopSelf()
                return
            }

            currentPort = port

            // Build notification
            val notification = buildNotification(port, running = true)

            // Start foreground — handle API differences
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                // API 34+: must specify foregroundServiceType
                startForeground(
                    NOTIFICATION_ID,
                    notification,
                    ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE
                )
            } else {
                startForeground(NOTIFICATION_ID, notification)
            }

            // Create and start server
            server = LocalServer(port, workspaceManager) { logMessage ->
                log(logMessage)
                updateNotification(port, running = true)
            }
            server?.start()

            _isRunning.set(true)
            log("✓ Service started - Server on port $port")
        } catch (e: Exception) {
            log("ERROR starting server: ${e.message}")
            _isRunning.set(false)
            stopForeground(STOP_FOREGROUND_REMOVE)
            stopSelf()
        }
    }

    private fun stopServer() {
        try {
            server?.stop()
            server = null
            _isRunning.set(false)
            log("Server stopped")
        } catch (e: Exception) {
            log("ERROR stopping server: ${e.message}")
        }
    }

    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            CHANNEL_ID,
            "LocalForge Server",
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = "LocalForge HTTP server notifications"
            setShowBadge(false)
        }
        val manager = getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(channel)
    }

    private fun buildNotification(port: Int, running: Boolean): Notification {
        val openAppIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        val openAppPendingIntent = PendingIntent.getActivity(
            this, 0, openAppIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val stopIntent = Intent(this, LocalForgeService::class.java).apply {
            action = ACTION_STOP
        }
        val stopPendingIntent = PendingIntent.getService(
            this, 1, stopIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val statusText = if (running) "Running on port $port" else "Stopped"

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("LocalForge Server")
            .setContentText(statusText)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setOngoing(true)
            .setContentIntent(openAppPendingIntent)
            .addAction(android.R.drawable.ic_media_pause, "Stop", stopPendingIntent)
            .build()
    }

    private fun updateNotification(port: Int, running: Boolean) {
        val notification = buildNotification(port, running)
        val manager = getSystemService(NotificationManager::class.java)
        manager.notify(NOTIFICATION_ID, notification)
    }

    private fun log(message: String) {
        val intent = Intent("LOCALFORGE_LOG").apply {
            putExtra("message", message)
        }
        sendBroadcast(intent)
    }
}
