package com.example.phonelock

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.PixelFormat
import android.hardware.display.DisplayManager
import android.hardware.display.VirtualDisplay
import android.media.Image
import android.media.ImageReader
import android.media.projection.MediaProjection
import android.media.projection.MediaProjectionManager
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.util.DisplayMetrics
import android.util.Log
import android.view.WindowManager
import androidx.annotation.RequiresApi
import com.example.phonelock.repository.UserRepository
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.io.ByteArrayOutputStream

/**
 * Helper class for managing screenshot capture using MediaProjection API
 */
@RequiresApi(Build.VERSION_CODES.LOLLIPOP)
object ScreenshotHelper {
    
    private const val TAG = "ScreenshotHelper"
    private const val PREFS_NAME = "ScreenshotPrefs"
    private const val KEY_HAS_PERMISSION = "has_permission"
    
    private var mediaProjection: MediaProjection? = null
    private var resultCode: Int = Activity.RESULT_CANCELED
    private var resultData: Intent? = null
    
    /**
     * Check if screenshot permission has been granted
     */
    fun hasPermission(context: Context): Boolean {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getBoolean(KEY_HAS_PERMISSION, false)
    }
    
    /**
     * Store the MediaProjection permission data
     */
    fun setPermissionData(context: Context, code: Int, data: Intent) {
        resultCode = code
        resultData = data
        
        // Also store in ScreenshotCaptureService (sync both)
        ScreenshotCaptureService.setMediaProjectionData(code, data)
        
        // Save permission granted state
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putBoolean(KEY_HAS_PERMISSION, true)
            .apply()
        
        Log.d(TAG, "✅ MediaProjection permission data stored in both ScreenshotHelper and ScreenshotCaptureService")
    }
    
    /**
     * Request screenshot permission
     */
    fun requestPermission(context: Context, complaintId: String? = null) {
        val intent = Intent(context, ScreenshotPermissionActivity::class.java)
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        
        // Pass complaint ID so screenshot can be retried after permission granted
        if (!complaintId.isNullOrEmpty()) {
            intent.putExtra("COMPLAINT_ID", complaintId)
            Log.d(TAG, "🔄 Requesting permission with complaint ID: $complaintId")
        }
        
        context.startActivity(intent)
    }
    
    /**
     * Capture screenshot and upload to Firebase
     * ALWAYS goes through permission activity to get fresh token
     * This is called from AbuseDetectionService - it will wait for screenshot before locking
     */
    fun captureAndUpload(context: Context, complaintId: String, detectedWord: String, secretCode: String) {
        Log.d(TAG, "📸 Launching screenshot permission flow for complaint: $complaintId")
        
        // Always go through permission activity
        // This ensures fresh token every time and proper flow management
        val intent = Intent(context, ScreenshotPermissionActivity::class.java).apply {
            putExtra("COMPLAINT_ID", complaintId)
            putExtra("DETECTED_WORD", detectedWord)
            putExtra("SECRET_CODE", secretCode)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        
        context.startActivity(intent)
    }
}

