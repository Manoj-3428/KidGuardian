package com.example.phonelock

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.media.projection.MediaProjectionManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi

/**
 * Activity to request MediaProjection permission for screenshot capture
 */
@RequiresApi(Build.VERSION_CODES.LOLLIPOP)
class ScreenshotPermissionActivity : ComponentActivity() {
    
    private val TAG = "ScreenshotPermission"
    private var isWaitingForLockScreen = false
    
    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        val complaintId = intent.getStringExtra("COMPLAINT_ID")
        val detectedWord = intent.getStringExtra("DETECTED_WORD")
        val secretCode = intent.getStringExtra("SECRET_CODE")
        
        if (result.resultCode == Activity.RESULT_OK && result.data != null) {
            // Permission granted - store the data in both places
            ScreenshotHelper.setPermissionData(this, result.resultCode, result.data!!)
            ScreenshotCaptureService.setMediaProjectionData(result.resultCode, result.data!!)
            
            Log.d(TAG, "✅ MediaProjection permission granted and stored")
            
            // Capture screenshot for this complaint
            if (!complaintId.isNullOrEmpty()) {
                Log.d(TAG, "📸 Taking screenshot of THIS screen, then going to kiosk mode...")
                
                // Set flag to indicate we're waiting for lock screen
                isWaitingForLockScreen = true
                
                // Start capture service with lock screen launch callback
                val intent = Intent(this, ScreenshotCaptureService::class.java).apply {
                    putExtra("COMPLAINT_ID", complaintId)
                    putExtra("DETECTED_WORD", detectedWord)
                    putExtra("SECRET_CODE", secretCode)
                    putExtra("LAUNCH_LOCK_SCREEN", true)
                }
                
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    startForegroundService(intent)
                } else {
                    startService(intent)
                }
            }
            
            // DON'T call finish() - stay on this screen for screenshot
            // The ScreenshotCaptureService will launch lock screen after capture
            Log.d(TAG, "⏳ Waiting for screenshot capture to complete...")
        } else {
            Log.e(TAG, "❌ MediaProjection permission denied - going directly to kiosk mode")
            
            // Permission denied - launch lock screen directly without screenshot
            if (!complaintId.isNullOrEmpty() && !detectedWord.isNullOrEmpty() && !secretCode.isNullOrEmpty()) {
                // Set flag - we don't need onStop() to finish since launchLockScreenDirect already does
                // But we'll remove the finish() from there and let onStop() handle it
                isWaitingForLockScreen = true
                launchLockScreenDirect(complaintId, detectedWord, secretCode)
            }
            
            // Lock screen will cover this activity, onStop() will finish it
            Log.d(TAG, "⏳ Lock screen will cover this activity...")
        }
    }
    
    private fun launchLockScreenDirect(complaintId: String, detectedWord: String, secretCode: String) {
        // Start lock monitor service
        LockMonitorService.start(this, complaintId, detectedWord, secretCode)
        
        // Launch lock screen
        val intent = Intent(this, LockScreenActivity::class.java).apply {
            putExtra("COMPLAINT_ID", complaintId)
            putExtra("DETECTED_WORD", detectedWord)
            putExtra("SECRET_CODE", secretCode)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
        }
        startActivity(intent)
        
        Log.d(TAG, "🔒 Lock screen launched directly (no screenshot)")
        // Don't call finish() here - let onStop() handle it when lock screen covers this activity
    }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Activity is TRANSPARENT (see AndroidManifest.xml) so WhatsApp/Instagram stays visible
        // This allows screenshot to capture the original app where word was detected
        
        Log.d(TAG, "📸 Requesting MediaProjection permission (transparent activity - WhatsApp visible underneath)...")
        
        val mediaProjectionManager = getSystemService(Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
        val intent = mediaProjectionManager.createScreenCaptureIntent()
        
        permissionLauncher.launch(intent)
    }
    
    override fun onStop() {
        super.onStop()
        // Only finish if we're waiting for lock screen
        // This prevents finishing when permission dialog shows
        if (isWaitingForLockScreen) {
            Log.d(TAG, "📱 Activity stopped (lock screen is on top) - finishing activity")
            finish()
        } else {
            Log.d(TAG, "📱 Activity stopped (permission dialog shown) - NOT finishing yet")
        }
    }
}

