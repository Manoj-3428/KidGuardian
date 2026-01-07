package com.example.phonelock

import android.accessibilityservice.AccessibilityService
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Intent
import android.os.Build
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import androidx.annotation.RequiresApi
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import com.example.phonelock.repository.UserRepository
import com.example.phonelock.models.UserRole

@RequiresApi(Build.VERSION_CODES.DONUT)
class AbuseDetectionService : AccessibilityService() {

    // Use comprehensive word database with categories
    private val abusiveWords = AbusiveWordsDatabase.allWords
    private lateinit var repository: UserRepository
    
    // Debouncing mechanism to prevent multiple detections
    private var lastDetectedWord: String? = null
    private var lastDetectionTime: Long = 0
    private val DETECTION_COOLDOWN_MS = 60000L // 1 minute cooldown to prevent loops
    
    // Parent link status cache
    private var isParentLinked = false
    private var lastParentCheckTime = 0L
    private val PARENT_CHECK_INTERVAL = 5000L // Check every 5 seconds for more responsive detection
    
    // Track the actual foreground app (not keyboard)
    private var lastForegroundApp: String? = null
    private var lastForegroundAppTime: Long = 0

    override fun onCreate() {
        super.onCreate()
        repository = UserRepository(this)
        checkParentLinkStatus()
        Log.d("ABUSE_DETECT", "🚀 AbuseDetectionService started - monitoring enabled")
    }
    
    private fun checkParentLinkStatus() {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val result = repository.getChildData()
                if (result.isSuccess) {
                    val child = result.getOrNull()
                    val wasLinked = isParentLinked
                    isParentLinked = child?.parentLinked == true
                    lastParentCheckTime = System.currentTimeMillis()
                    
                    if (wasLinked != isParentLinked) {
                        Log.d("ABUSE_DETECT", "🔗 Parent link status changed: $isParentLinked")
                    }
                    
                    // Log current status for debugging
                    Log.d("ABUSE_DETECT", "📊 Current parent link status: $isParentLinked (child: ${child?.name}, childId: ${child?.childId})")
                } else {
                    Log.w("ABUSE_DETECT", "⚠️ Failed to get child data: ${result.exceptionOrNull()?.message}")
                }
            } catch (e: Exception) {
                Log.e("ABUSE_DETECT", "❌ Error checking parent link status", e)
            }
        }
    }
    
    /**
     * Synchronously check parent link status (used when we need immediate result)
     * This blocks the current thread, so use sparingly
     */
    private suspend fun checkParentLinkStatusSync(): Boolean {
        return try {
            val result = repository.getChildData()
            if (result.isSuccess) {
                val child = result.getOrNull()
                val linked = child?.parentLinked == true
                val wasLinked = isParentLinked
                isParentLinked = linked
                lastParentCheckTime = System.currentTimeMillis()
                
                if (wasLinked != linked) {
                    Log.d("ABUSE_DETECT", "🔗 Parent link status changed (sync): $linked")
                }
                linked
            } else {
                Log.w("ABUSE_DETECT", "⚠️ Failed to get child data (sync): ${result.exceptionOrNull()?.message}")
                isParentLinked // Return cached value
            }
        } catch (e: Exception) {
            Log.e("ABUSE_DETECT", "❌ Error checking parent link status (sync)", e)
            isParentLinked // Return cached value
        }
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event == null) return

        // Check if monitoring is enabled and user is a child
        if (!isMonitoringEnabled()) {
            return
        }
        
        // Periodically refresh parent link status (async)
        val currentTime = System.currentTimeMillis()
        if (currentTime - lastParentCheckTime > PARENT_CHECK_INTERVAL) {
            checkParentLinkStatus()
        }
        
        // IMPORTANT: Only detect if parent account is linked
        // If status is unknown or stale, do a quick sync check
        var shouldProceed = isParentLinked
        if (!isParentLinked) {
            // If we haven't checked recently or status is false, do a synchronous check
            if (currentTime - lastParentCheckTime > PARENT_CHECK_INTERVAL || lastParentCheckTime == 0L) {
                // Use runBlocking to get immediate result (only blocks this thread briefly)
                shouldProceed = kotlinx.coroutines.runBlocking {
                    checkParentLinkStatusSync()
                }
                if (!shouldProceed) {
                    // Still not linked - skip detection
                    return
                }
            } else {
                // Recently checked and still not linked - skip detection
                return
            }
        }

        // CRITICAL: Don't detect on our own app (prevents infinite loops)
        val packageName = event.packageName?.toString() ?: ""
        if (packageName == "com.example.phonelock") {
            // Skip all PhoneLock app screens: lock screen, permission, dashboard, etc.
            return
        }
        
        // Track foreground app changes (when window state changes)
        if (event.eventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) {
            // This is a real app window, not a keyboard
            if (!isKeyboardPackage(packageName)) {
                lastForegroundApp = packageName
                lastForegroundAppTime = System.currentTimeMillis()
                Log.d("ABUSE_DETECT", "📱 Foreground app changed to: $packageName")
            }
        }

        // Check for ANY content changes - capture entire screen
        if (event.eventType == AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED ||
            event.eventType == AccessibilityEvent.TYPE_VIEW_TEXT_CHANGED ||
            event.eventType == AccessibilityEvent.TYPE_VIEW_TEXT_SELECTION_CHANGED ||
            event.eventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED
        ) {
            // Log that we're processing events (only occasionally to avoid spam)
            if (System.currentTimeMillis() % 10000 < 100) { // Log roughly every 10 seconds
                Log.d("ABUSE_DETECT", "📝 Processing event: type=${event.eventType}, package=$packageName, parentLinked=$shouldProceed")
            }
            
            // Get text from multiple sources
            val allText = mutableListOf<String>()
            
            // 1. Get text from event.text
            event.text?.forEach { text ->
                if (text.toString().isNotEmpty()) {
                allText.add(text.toString())
                }
            }
            
            // 2. Get text from event.contentDescription
            event.contentDescription?.let { desc ->
                if (desc.toString().isNotEmpty()) {
                allText.add(desc.toString())
            }
            }
            
            // 3. Get text from the entire window hierarchy (FULL SCREEN)
            event.source?.let { source ->
                extractTextFromNode(source, allText)
            }
            
            // Log text extraction results (only occasionally)
            if (allText.isNotEmpty() && System.currentTimeMillis() % 10000 < 100) {
                val sampleText = allText.take(3).joinToString(", ")
                Log.d("ABUSE_DETECT", "📄 Extracted text samples: $sampleText... (total: ${allText.size} items)")
            }
            
            // Check all collected text
            for (text in allText) {
                val lowerText = text.toString().lowercase().trim()
                if (lowerText.isEmpty() || lowerText.length < 3) continue
                
                for (word in abusiveWords) {
                    // Use word boundary regex to match WHOLE WORDS ONLY
                    // \b ensures we don't match "ass" in "compass", "glass", "class", etc.
                    val wordPattern = "\\b${Regex.escape(word)}\\b".toRegex()
                    if (wordPattern.containsMatchIn(lowerText)) {
                        // Check if ANY detection happened within last 1 minute
                        val currentTime = System.currentTimeMillis()
                        if ((currentTime - lastDetectionTime) < DETECTION_COOLDOWN_MS) {
                            // Still in cooldown period - ignore ALL detections
                            return
                        }
                        
                        // Update last detection
                        lastDetectedWord = word
                        lastDetectionTime = currentTime

                        // Get the actual app name (not keyboard)
                        val actualAppName = getActualAppName(packageName)
                        
                        Log.e("ABUSE_DETECT", "🚨 DETECTED: $word in: $lowerText (detected package: $packageName, actual app: $actualAppName)")
                        Log.d("ABUSE_DETECT", "⏰ Detection cooldown started - no detections for 1 minute")

                        // Create complaint and launch screenshot flow with actual app name
                        handleAbuseDetection(word, lowerText, actualAppName)
                        return
                    }
                }
            }
        }
    }
    
    private fun extractTextFromNode(node: android.view.accessibility.AccessibilityNodeInfo, textList: MutableList<String>) {
        try {
            // Get text from current node
            node.text?.let { text ->
                if (text.toString().isNotEmpty()) {
                    textList.add(text.toString())
                }
            }
            
            // Get content description
            node.contentDescription?.let { desc ->
                if (desc.toString().isNotEmpty()) {
                    textList.add(desc.toString())
                }
            }
            
            // Recursively get text from child nodes (limit depth to avoid performance issues)
            if (node.childCount > 0 && textList.size < 50) {
                for (i in 0 until node.childCount) {
                    node.getChild(i)?.let { child ->
                        extractTextFromNode(child, textList)
                        child.recycle()
                    }
                }
            }
        } catch (e: Exception) {
            // Silently catch - some nodes may not be accessible
        }
    }

    /**
     * Check if a package is a keyboard/input method
     */
    private fun isKeyboardPackage(packageName: String): Boolean {
        val packageNameLower = packageName.lowercase()
        return packageNameLower.contains("inputmethod") ||
               packageNameLower.contains("keyboard") ||
               packageNameLower.contains(".ime") ||
               packageNameLower.contains("com.google.android.inputmethod") ||
               packageNameLower.contains("com.samsung.android.honeyboard") ||
               packageNameLower.contains("com.swiftkey") ||
               packageNameLower.contains("com.touchtype.swiftkey") ||
               packageNameLower.contains("com.gboard") ||
               packageNameLower.contains("com.sec.android.inputmethod")
    }
    
    /**
     * Get the actual app name, not the keyboard package
     * If the detected package is a keyboard, use the last known foreground app
     */
    private fun getActualAppName(detectedPackage: String): String {
        // If it's not a keyboard, return as is
        if (!isKeyboardPackage(detectedPackage)) {
            return detectedPackage
        }
        
        // If it's a keyboard, try to get the last foreground app
        // Check if we have a recent foreground app (within last 5 seconds)
        val currentTime = System.currentTimeMillis()
        if (lastForegroundApp != null && 
            (currentTime - lastForegroundAppTime) < 5000) {
            Log.d("ABUSE_DETECT", "🔍 Keyboard detected, using last foreground app: $lastForegroundApp")
            return lastForegroundApp!!
        }
        
        // Fallback: Try to get current foreground app using ActivityManager
        return try {
            val activityManager = getSystemService(android.app.ActivityManager::class.java)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                // Use getRunningAppProcesses to find foreground app
                val runningProcesses = activityManager.runningAppProcesses
                runningProcesses?.firstOrNull { processInfo ->
                    processInfo.importance == android.app.ActivityManager.RunningAppProcessInfo.IMPORTANCE_FOREGROUND
                }?.let { processInfo ->
                    // Get the first package name from the process
                    processInfo.pkgList?.firstOrNull()?.let { pkg ->
                        if (!isKeyboardPackage(pkg)) {
                            return pkg
                        }
                    }
                }
            }
            
            // If we still don't have a valid app, return detected package
            // (This should rarely happen since we track foreground apps)
            Log.w("ABUSE_DETECT", "Could not determine foreground app, using detected package: $detectedPackage")
            detectedPackage
        } catch (e: Exception) {
            Log.e("ABUSE_DETECT", "Error getting foreground app", e)
            // Last resort: return detected package (keyboard)
            detectedPackage
        }
    }
    
    private fun isMonitoringEnabled(): Boolean {
        return try {
            val userRole = repository.getUserRole()
            // Only monitor if user is a child
            if (userRole != UserRole.CHILD) {
                Log.d("ABUSE_DETECT", "⏸️ Monitoring disabled: user is not a child (role: $userRole)")
                return false
            }
            
            // For now, monitoring is enabled by default for all child accounts
            // In the future, you can check parent's monitoring preference here
            true
        } catch (e: Exception) {
            Log.e("ABUSE_DETECT", "❌ Error checking monitoring status", e)
            false
        }
    }

    private fun handleAbuseDetection(detectedWord: String, fullText: String, appName: String) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                // Get child data
                val childResult = repository.getChildData()
                if (childResult.isFailure) {
                    Log.e("ABUSE_DETECT", "Failed to get child data")
                    return@launch
                }
                
                val child = childResult.getOrNull() ?: return@launch
                
                // Determine category from detected word
                val category = AbusiveWordsDatabase.getCategoryForWord(detectedWord)
                val categoryName = category.displayName.lowercase().replace(" ", "_")
                
                Log.d("ABUSE_DETECT", "📊 Category: ${category.displayName} for word: $detectedWord")
                
                // Create complaint with screenshot placeholder
                val complaintResult = repository.createComplaint(
                    childId = child.childId,
                    detectedWord = detectedWord,
                    screenshotUrl = "screenshot_placeholder", // Will be updated later
                    appName = appName,
                    category = categoryName
                )
                
                if (complaintResult.isSuccess) {
                    val complaintId = complaintResult.getOrNull() ?: return@launch
                    
                    // Get the complaint to retrieve the secret code
                    val complaint = repository.getComplaintById(complaintId).getOrNull()
                    val secretCode = complaint?.secretCode ?: "000000"
                    
                    Log.d("ABUSE_DETECT", "📸 Initiating screenshot capture flow...")
                    
                    // Send notification to parent with secret code
                    sendParentNotification(child.childId, detectedWord, secretCode, appName)
                    
                    // IMPORTANT: Launch screenshot permission flow
                    // ScreenshotPermissionActivity will handle complete flow:
                    // - Request permission (every time - Android auto-approves after first)
                    // - If granted: Captures screenshot → Uploads → Launches lock screen
                    // - If denied: Launches lock screen directly without screenshot
                    try {
                        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP) {
                            ScreenshotHelper.captureAndUpload(
                                this@AbuseDetectionService,
                                complaintId,
                                detectedWord,
                                secretCode
                            )
                            Log.d("ABUSE_DETECT", "✅ Screenshot flow initiated for complaint: $complaintId")
                        }
                    } catch (e: Exception) {
                        Log.e("ABUSE_DETECT", "❌ Screenshot flow failed", e)
                        // Fallback: Launch lock screen directly
                        launchLockScreen(complaintId, detectedWord, secretCode)
                    }
                    
                    Log.d("ABUSE_DETECT", "Complaint created (ID: $complaintId), screenshot flow started")
                } else {
                    Log.e("ABUSE_DETECT", "Failed to create complaint")
                }
            } catch (e: Exception) {
                Log.e("ABUSE_DETECT", "Error handling abuse detection", e)
            }
        }
    }
    
    private fun launchLockScreen(complaintId: String, detectedWord: String, secretCode: String) {
        CoroutineScope(Dispatchers.Main).launch {
            try {
                    // Start lock monitor service (keeps phone locked)
                    LockMonitorService.start(
                        this@AbuseDetectionService,
                        complaintId,
                        detectedWord,
                        secretCode
                    )
                    
                    // Launch lock screen with complaint details
                        val intent = Intent(this@AbuseDetectionService, LockScreenActivity::class.java).apply {
                            putExtra("COMPLAINT_ID", complaintId)
                            putExtra("DETECTED_WORD", detectedWord)
                            putExtra("SECRET_CODE", secretCode)
                            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
                        }
                        startActivity(intent)
                    
                Log.d("ABUSE_DETECT", "🔒 Lock screen launched for complaint: $complaintId")
            } catch (e: Exception) {
                Log.e("ABUSE_DETECT", "❌ Error launching lock screen", e)
            }
        }
    }
    
    private fun sendParentNotification(childId: String, detectedWord: String, secretCode: String, appName: String) {
        CoroutineScope(Dispatchers.IO).launch @androidx.annotation.RequiresPermission(android.Manifest.permission.POST_NOTIFICATIONS) {
            try {
                // Get parent data to send notification
                val childResult = repository.getChildByChildId(childId)
                if (childResult.isSuccess) {
                    val child = childResult.getOrNull()
                    
                    // For now, show a high-priority notification
                    // In production, you would:
                    // 1. Get parent's phone number from Firestore
                    // 2. Send SMS using SMS API or Firebase Cloud Messaging
                    // 3. Send push notification to parent's app
                    
                    val channelId = "abuse_parent_alert"
                    val notificationId = (System.currentTimeMillis() % Int.MAX_VALUE).toInt()
                    
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        val channel = NotificationChannel(
                            channelId,
                            "Parent Alerts",
                            NotificationManager.IMPORTANCE_HIGH
                        ).apply {
                            description = "Critical alerts sent to parents about abuse detection"
                        }
                        val notificationManager = getSystemService(NotificationManager::class.java)
                        notificationManager?.createNotificationChannel(channel)
                    }
                    
                    val notification = NotificationCompat.Builder(this@AbuseDetectionService, channelId)
                        .setSmallIcon(R.drawable.ic_launcher_foreground)
                        .setContentTitle("🔒 Child's Device Locked")
                        .setContentText("Inappropriate content detected: \"$detectedWord\"")
                        .setStyle(NotificationCompat.BigTextStyle()
                            .bigText("Your child's device has been locked due to inappropriate content.\n\n" +
                                    "⚠️ Detected word: \"$detectedWord\"\n" +
                                    "📱 App: $appName\n" +
                                    "🕐 Time: ${java.text.SimpleDateFormat("HH:mm", java.util.Locale.getDefault()).format(java.util.Date())}\n\n" +
                                    "🔑 UNLOCK CODE: $secretCode\n\n" +
                                    "Share this code with your child to unlock the device."))
                        .setPriority(NotificationCompat.PRIORITY_MAX)
                        .setCategory(NotificationCompat.CATEGORY_ALARM)
                        .setAutoCancel(false)
                        .setOngoing(true)
                        .build()
                    
                    NotificationManagerCompat.from(this@AbuseDetectionService).notify(notificationId, notification)
                    
                    Log.d("ABUSE_DETECT", "Parent notification sent with code: $secretCode")
                }
            } catch (e: Exception) {
                Log.e("ABUSE_DETECT", "Error sending parent notification", e)
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

}
