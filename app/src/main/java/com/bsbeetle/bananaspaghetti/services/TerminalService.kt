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
import java.util.concurrent.atomic.AtomicInteger

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
        const val EXTRA_NAVIGATE_TO_SESSIONS = "navigate_to_sessions"
        
        private var instance: TerminalService? = null
        private val activeSessionCount = AtomicInteger(0)
        @Volatile
        private var installationInProgress: String? = null

        fun isRunning(): Boolean = instance != null

        fun getSessionCount(): Int = activeSessionCount.get()

        fun startService(context: Context) {
            val intent = Intent(context, TerminalService::class.java)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        fun stopServiceIfNoSessions(context: Context) {
            if (activeSessionCount.get() <= 0 && installationInProgress == null) {
                val intent = Intent(context, TerminalService::class.java)
                context.stopService(intent)
            }
        }

        fun incrementSessionCount() {
            val count = activeSessionCount.incrementAndGet()
            instance?.updateNotification()
            Log.d(TAG, "Session count increased to $count")
        }

        fun decrementSessionCount(context: Context) {
            val count = activeSessionCount.decrementAndGet().coerceAtLeast(0)
            // If decrementAndGet went below 0, reset to 0
            activeSessionCount.compareAndSet(count, count.coerceAtLeast(0))
            instance?.updateNotification()
            Log.d(TAG, "Session count decreased to $count")
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
                NotificationManager.IMPORTANCE_LOW // Low importance = silent, no popup
            ).apply {
                description = "Keeps terminal sessions running in background"
                setShowBadge(false)
                lockscreenVisibility = Notification.VISIBILITY_SECRET // Hide on lock screen
                enableVibration(false)
                setSound(null, null)
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
                // SINGLE_TOP + CLEAR_TOP: bring existing MainActivity to front and call
                // onNewIntent instead of recreating the activity (which would crash because
                // the new EditorViewModel would try to bind AssetServer port 8080 while the
                // old instance is still running)
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or
                        Intent.FLAG_ACTIVITY_CLEAR_TOP or
                        Intent.FLAG_ACTIVITY_SINGLE_TOP
                putExtra(EXTRA_NAVIGATE_TO_SESSIONS, true)
            },
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        
        val title: String
        val text: String
        
        val sessionCount = activeSessionCount.get()
        when {
            installationInProgress != null -> {
                title = "Installing $installationInProgress"
                text = "Extension installation in progress..."
            }
            sessionCount > 0 -> {
                title = "Beetle Terminal"
                text = "$sessionCount session${if (sessionCount > 1) "s" else ""} running"
            }
            else -> {
                title = "Beetle Terminal"
                text = "Running in background"
            }
        }
        
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(title)
            .setContentText(text)
            .setSmallIcon(R.drawable.ic_notification_beetle)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .setAutoCancel(false)
            .setPriority(NotificationCompat.PRIORITY_LOW) // Low priority = minimal visibility
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .setForegroundServiceBehavior(NotificationCompat.FOREGROUND_SERVICE_DEFERRED) // Defer showing
            .setVisibility(NotificationCompat.VISIBILITY_SECRET) // Hide from lock screen
            .setSilent(true) // No sound
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
            acquire() // No timeout — foreground service lifecycle manages wake lock
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
