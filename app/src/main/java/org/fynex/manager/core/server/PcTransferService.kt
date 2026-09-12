package org.fynex.manager.core.server

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat

class PcTransferService : Service() {

    private var server: PcTransferServer? = null

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val action = intent?.action
        if (action == ACTION_STOP) {
            stopServer()
            stopSelf()
            return START_NOT_STICKY
        }

        val port = intent?.getIntExtra(EXTRA_PORT, 8080) ?: 8080
        startServer(port)
        return START_STICKY
    }

    private fun startServer(port: Int) {
        if (server == null) {
            server = PcTransferServer(port).apply { start() }
            val ip = PcTransferServer.getLocalIpAddress() ?: "127.0.0.1"
            val notification = buildNotification("Servidor Fynex Ativo em http://$ip:$port")
            startForeground(NOTIFICATION_ID, notification)
        }
    }

    private fun stopServer() {
        server?.stop()
        server = null
    }

    override fun onDestroy() {
        stopServer()
        super.onDestroy()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Fynex PC Transfer",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Notificação de transferência de arquivos Wi-Fi do Fynex"
            }
            val nm = getSystemService(NotificationManager::class.java)
            nm.createNotificationChannel(channel)
        }
    }

    private fun buildNotification(text: String): Notification {
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Fynex Web Transfer")
            .setContentText(text)
            .setSmallIcon(android.R.drawable.stat_sys_upload)
            .setOngoing(true)
            .build()
    }

    companion object {
        const val CHANNEL_ID = "fynex_pc_transfer_channel"
        const val NOTIFICATION_ID = 1001
        const val ACTION_STOP = "org.fynex.manager.STOP_SERVER"
        const val EXTRA_PORT = "extra_port"
    }
}
