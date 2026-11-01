package com.marksameh.remote

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.media.projection.MediaProjection
import android.media.projection.MediaProjectionManager
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat

class ScreenShareService : Service() {
    private var projection: MediaProjection? = null
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val channelId = "mark_remote_sharing"
        val manager = getSystemService(NotificationManager::class.java)
        if (Build.VERSION.SDK_INT >= 26) manager.createNotificationChannel(
            NotificationChannel(channelId, "Screen sharing", NotificationManager.IMPORTANCE_LOW)
        )
        val notification: Notification = NotificationCompat.Builder(this, channelId)
            .setSmallIcon(android.R.drawable.ic_menu_view)
            .setContentTitle("Mark Remote is sharing your screen")
            .setContentText("Your screen-sharing session is active. Tap Stop to end it.")
            .setOngoing(true)
            .addAction(0, "Stop", android.app.PendingIntent.getService(
                this, 1, Intent(this, ScreenShareService::class.java).setAction("STOP"),
                android.app.PendingIntent.FLAG_UPDATE_CURRENT or android.app.PendingIntent.FLAG_IMMUTABLE
            ))
            .build()
        startForeground(41, notification)
        if (intent?.action == "STOP") { stopSharing(); return START_NOT_STICKY }
        val resultCode = intent?.getIntExtra("resultCode", 0) ?: 0
        val data = if (Build.VERSION.SDK_INT >= 33) intent?.getParcelableExtra("data", Intent::class.java)
                   else @Suppress("DEPRECATION") intent?.getParcelableExtra("data")
        if (data != null) {
            val mgr = getSystemService(MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
            projection = mgr.getMediaProjection(resultCode, data)
            // Capture pipeline is intentionally the next milestone; permission and visible foreground session are established here.
        }
        return START_NOT_STICKY
    }
    private fun stopSharing() {
        projection?.stop(); projection = null
        stopForeground(STOP_FOREGROUND_REMOVE); stopSelf()
    }
    override fun onDestroy() { projection?.stop(); projection = null; super.onDestroy() }
    override fun onBind(intent: Intent?): IBinder? = null
}
