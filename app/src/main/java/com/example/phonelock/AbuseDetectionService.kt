package com.example.phonelock

import android.accessibilityservice.AccessibilityService
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Intent
import android.os.Build
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat

class AbuseDetectionService : AccessibilityService() {

    private val abusiveWords = listOf("fuck", "sex", "porn", "nude","bokka","mingi")

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event == null) return

        if (event.eventType == AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED ||
            event.eventType == AccessibilityEvent.TYPE_VIEW_TEXT_CHANGED
        ) {
            val textList = event.text
            for (text in textList) {
                val lowerText = text.toString().lowercase()
                for (word in abusiveWords) {
                    if (lowerText.contains(word)) {
                        Log.e("ABUSE_DETECT", "Detected abusive word: $word in: $lowerText")

                        // 🟢 Optional notification (for visual debug)
                        showNotification(word, lowerText)

                        // 🚀 Launch screen capture permission (one time)
                        launchScreenshotCapture()

                        return
                    }
                }
            }
        }
    }

    override fun onInterrupt() {
        Log.d("ABUSE_DETECT", "Accessibility Service Interrupted")
    }

    // 🟢 Optional: Show alert notification
    private fun showNotification(word: String, fullText: String) {
        val channelId = "abuse_alerts"
        val notificationId = (System.currentTimeMillis() % Int.MAX_VALUE).toInt()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "Abusive Word Alerts",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Notifies when abusive words are detected on screen"
            }

            val notificationManager = getSystemService(NotificationManager::class.java)
            notificationManager?.createNotificationChannel(channel)
        }

        val notification = NotificationCompat.Builder(this, channelId)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle("Abusive Word Detected")
            .setContentText("Detected \"$word\" in: \"$fullText\"")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .build()

        NotificationManagerCompat.from(this).notify(notificationId, notification)
    }

    // 🔥 Launch permission activity to request screen capture
    private fun launchScreenshotCapture() {
        val intent = Intent(this, ScreenCapturePermissionActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        startActivity(intent)
    }
}
