package com.example.phonelock

import android.app.*
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.util.Log
import androidx.core.app.NotificationCompat

class LockMonitorService : Service() {
    
    private val handler = Handler(Looper.getMainLooper())
    private var isLocked = true
    private var complaintId: String? = null
    private var detectedWord: String? = null
    private var secretCode: String? = null
    
    private val checkRunnable = object : Runnable {
        override fun run() {
            if (isLocked) {
                // Always try to bring lock screen to front
                // This ensures child cannot escape
                bringLockScreenToFront()
                
                // Check again every 1 second (more aggressive)
                handler.postDelayed(this, 1000)
            }
        }
    }
    
    companion object {
        private const val CHANNEL_ID = "lock_monitor_channel"
        private const val NOTIFICATION_ID = 12345
        
        private var isServiceRunning = false
        
        fun start(context: Context, complaintId: String, detectedWord: String, secretCode: String) {
            // Prevent multiple instances
            if (isServiceRunning) {
                Log.d("LockMonitor", "Service already running - skipping duplicate start")
                return
            }
            
            val intent = Intent(context, LockMonitorService::class.java).apply {
                putExtra("COMPLAINT_ID", complaintId)
                putExtra("DETECTED_WORD", detectedWord)
                putExtra("SECRET_CODE", secretCode)
            }
            
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
            
            isServiceRunning = true
        }
        
        fun stop(context: Context) {
            val intent = Intent(context, LockMonitorService::class.java)
            context.stopService(intent)
            isServiceRunning = false
        }
    }
    
    override fun onCreate() {
        super.onCreate()
        Log.d("LockMonitor", "Service created")
        createNotificationChannel()
    }
    
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d("LockMonitor", "Service started - ENFORCING LOCK MODE")
        
        complaintId = intent?.getStringExtra("COMPLAINT_ID")
        detectedWord = intent?.getStringExtra("DETECTED_WORD")
        secretCode = intent?.getStringExtra("SECRET_CODE")
        
        isLocked = true
        
        // Start as foreground service
        startForeground(NOTIFICATION_ID, createNotification())
        
        // Start monitoring IMMEDIATELY and continuously
        handler.post(checkRunnable)
        
        // Also bring lock screen to front immediately
        bringLockScreenToFront()
        
        return START_STICKY
    }
    
    override fun onDestroy() {
        super.onDestroy()
        Log.d("LockMonitor", "Service destroyed")
        handler.removeCallbacks(checkRunnable)
        isLocked = false
        isServiceRunning = false
    }
    
    override fun onBind(intent: Intent?): IBinder? {
        return null
    }
    
    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Phone Lock Monitor",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Monitoring phone lock status"
                setShowBadge(false)
            }
            
            val notificationManager = getSystemService(NotificationManager::class.java)
            notificationManager?.createNotificationChannel(channel)
        }
    }
    
    private fun createNotification(): Notification {
        val unlockIntent = Intent(this, LockScreenActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            putExtra("COMPLAINT_ID", complaintId)
            putExtra("DETECTED_WORD", detectedWord)
            putExtra("SECRET_CODE", secretCode)
        }
        
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            unlockIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle("Phone Locked")
            .setContentText("Enter passcode to unlock")
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setOngoing(true)
            .setContentIntent(pendingIntent)
            .build()
    }
    
    private fun bringLockScreenToFront() {
        try {
            Log.d("LockMonitor", "Bringing lock screen to front...")
            val intent = Intent(this, LockScreenActivity::class.java).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or 
                         Intent.FLAG_ACTIVITY_CLEAR_TOP or
                         Intent.FLAG_ACTIVITY_SINGLE_TOP or
                         Intent.FLAG_ACTIVITY_NO_ANIMATION)
                putExtra("COMPLAINT_ID", complaintId)
                putExtra("DETECTED_WORD", detectedWord)
                putExtra("SECRET_CODE", secretCode)
            }
            startActivity(intent)
        } catch (e: Exception) {
            Log.e("LockMonitor", "Failed to bring lock screen to front", e)
        }
    }
}

