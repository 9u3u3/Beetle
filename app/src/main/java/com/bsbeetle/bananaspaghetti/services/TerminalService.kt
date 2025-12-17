package com.bsbeetle.bananaspaghetti.services

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Binder
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import android.util.Log
import androidx.core.app.NotificationCompat
import com.bsbeetle.bananaspaghetti.MainActivity
import com.bsbeetle.bananaspaghetti.R

/**
 * Foreground service that keeps terminal sessions alive when the app is in background.
 * Similar to how Termux handles background processes.
 */
class TerminalService : Service() {

    companion object {
        private const val TAG = "TerminalService"
        private const val NOTIFICATION_ID = 1001
        const val CHANNEL_ID = "beetle_terminal_channel"
        const val CHANNEL_NAME = "Terminal Sessions"
        
        private var instance: TerminalService? = null
        private var activeSessionCount = 0
        private var installationInProgress: String? = null
        
        fun isRunning(): Boolean = instance != null
        
        fun startService(context: Context) {
            val intent = Intent(context, TerminalService::class.java)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }
        
        fun stopServiceIfNoSessions(context: Context) {
            if (activeSessionCount <= 0 && installationInProgress == null) {
                val intent = Intent(context, TerminalService::class.java)
                context.stopService(intent)
            }
        }
        
        fun incrementSessionCount() {
            activeSessionCount++
            instance?.updateNotification()
            Log.d(TAG, "Session count increased to $activeSessionCount")
        }
        
        fun decrementSessionCount(context: Context) {
            activeSessionCount = maxOf(0, activeSessionCount - 1)
            instance?.updateNotification()
            Log.d(TAG, "Session count decreased to $activeSessionCount")
            stopServiceIfNoSessions(context)
        }
        
        fun setInstallationInProgress(extensionName: String?) {
            installationInProgress = extensionName
            instance?.updateNotification()
        }
    }
    
    private val binder = LocalBinder()
    private var wakeLock: PowerManager.WakeLock? = null
    
    inner class LocalBinder : Binder() {
        fun getService(): TerminalService = this@TerminalService
    }
    
    override fun onCreate() {
        super.onCreate()
        instance = this
        Log.d(TAG, "TerminalService created")
        
        createNotificationChannel()
        acquireWakeLock()
        
        startForeground(NOTIFICATION_ID, createNotification())
    }
    
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d(TAG, "TerminalService started")
        return START_STICKY
    }
    
    override fun onBind(intent: Intent?): IBinder {
        return binder
    }
    
    override fun onDestroy() {
        Log.d(TAG, "TerminalService destroyed")
        releaseWakeLock()
        instance = null
        super.onDestroy()
    }
    
    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_DEFAULT // Default importance for foreground services
            ).apply {
                description = "Keeps terminal sessions running in background"
                setShowBadge(false)
                lockscreenVisibility = Notification.VISIBILITY_PUBLIC
                // Foreground service notifications cannot be dismissed anyway
            }
            
            val notificationManager = getSystemService(NotificationManager::class.java)
            notificationManager.createNotificationChannel(channel)
            Log.d(TAG, "Notification channel created")
        }
    }
    
    private fun createNotification(): Notification {
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            },
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        
        val title: String
        val text: String
        
        when {
            installationInProgress != null -> {
                title = "Installing $installationInProgress"
                text = "Extension installation in progress..."
            }
            activeSessionCount > 0 -> {
                title = "Beetle Terminal"
                text = "$activeSessionCount session${if (activeSessionCount > 1) "s" else ""} running"
            }
            else -> {
                title = "Beetle Terminal"
                text = "Running in background"
            }
        }
        
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(title)
            .setContentText(text)
            .setSmallIcon(R.drawable.ic_notification_beetle) // Minimal "B" icon
            .setContentIntent(pendingIntent)
            .setOngoing(true) // Cannot be dismissed by user
            .setAutoCancel(false) // Don't auto-cancel
            .setPriority(NotificationCompat.PRIORITY_DEFAULT) // Default priority for persistent notifications
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .setForegroundServiceBehavior(NotificationCompat.FOREGROUND_SERVICE_IMMEDIATE) // Show immediately
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC) // Show on lock screen
            .setColor(0xFF4EC9B0.toInt()) // Beetle teal accent color
            .build()
    }
    
    fun updateNotification() {
        val notificationManager = getSystemService(NotificationManager::class.java)
        notificationManager.notify(NOTIFICATION_ID, createNotification())
        Log.d(TAG, "Notification updated")
    }
    
    private fun acquireWakeLock() {
        val powerManager = getSystemService(Context.POWER_SERVICE) as PowerManager
        wakeLock = powerManager.newWakeLock(
            PowerManager.PARTIAL_WAKE_LOCK,
            "Beetle::TerminalWakeLock"
        ).apply {
            acquire(10 * 60 * 1000L) // 10 minutes max, will be re-acquired if needed
        }
        Log.d(TAG, "WakeLock acquired")
    }
    
    private fun releaseWakeLock() {
        wakeLock?.let {
            if (it.isHeld) {
                it.release()
                Log.d(TAG, "WakeLock released")
            }
        }
        wakeLock = null
    }
    
    /**
     * Refresh wakelock for long-running operations like installations
     */
    fun refreshWakeLock() {
        releaseWakeLock()
        acquireWakeLock()
    }
}
