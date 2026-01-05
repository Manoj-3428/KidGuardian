package com.example.phonelock

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
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
import android.os.IBinder
import android.os.Looper
import android.util.DisplayMetrics
import android.util.Log
import android.view.WindowManager
import androidx.annotation.RequiresApi
import com.example.phonelock.repository.UserRepository
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.io.ByteArrayOutputStream

/**
 * Foreground service for capturing screenshots using MediaProjection API
 * This service is required because MediaProjection must be used within a foreground service
 */
@RequiresApi(Build.VERSION_CODES.LOLLIPOP)
class ScreenshotCaptureService : Service() {
    
    private val TAG = "ScreenshotCapture"
    
    // Service-scoped coroutine scope that lives with the service
    private val serviceScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    
    companion object {
        private const val CHANNEL_ID = "screenshot_capture_channel"
        private const val NOTIFICATION_ID = 9876
        
        // Don't store MediaProjection instance - create fresh one each time
        private var resultCode: Int = 0
        private var resultData: Intent? = null
        
        fun setMediaProjectionData(code: Int, data: Intent) {
            resultCode = code
            resultData = data
            Log.d("ScreenshotCapture", "✅ MediaProjection data stored")
        }
        
        fun startCapture(context: Context, complaintId: String) {
            val intent = Intent(context, ScreenshotCaptureService::class.java).apply {
                putExtra("COMPLAINT_ID", complaintId)
            }
            
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }
    }
    
    override fun onBind(intent: Intent?): IBinder? = null
    
    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "Service created")
    }
    
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d(TAG, "🚀 onStartCommand called")
        
        // Create notification channel and start foreground
        createNotificationChannel()
        val notification = createNotification()
        startForeground(NOTIFICATION_ID, notification)
        Log.d(TAG, "✅ Started as foreground service")
        
        val complaintId = intent?.getStringExtra("COMPLAINT_ID")
        val detectedWord = intent?.getStringExtra("DETECTED_WORD")
        val secretCode = intent?.getStringExtra("SECRET_CODE")
        val shouldLaunchLockScreen = intent?.getBooleanExtra("LAUNCH_LOCK_SCREEN", false) ?: false
        
        if (complaintId != null) {
            Log.d(TAG, "📸 Starting screenshot capture for complaint: $complaintId")
            Log.d(TAG, "🔑 resultData is ${if (resultData != null) "available" else "NULL"}")
            Log.d(TAG, "🔒 Will launch lock screen after capture: $shouldLaunchLockScreen")
            captureScreenshot(complaintId, detectedWord, secretCode, shouldLaunchLockScreen)
        } else {
            Log.e(TAG, "❌ No complaint ID provided")
            stopSelf()
        }
        
        return START_NOT_STICKY
    }
    
    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Screenshot Capture",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Captures screenshots when inappropriate content is detected"
            }
            
            val notificationManager = getSystemService(NotificationManager::class.java)
            notificationManager?.createNotificationChannel(channel)
        }
    }
    
    private fun createNotification(): Notification {
        val builder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            Notification.Builder(this, CHANNEL_ID)
        } else {
            @Suppress("DEPRECATION")
            Notification.Builder(this)
        }
        
        return builder
            .setContentTitle("Capturing Screenshot")
            .setContentText("Taking screenshot for monitoring...")
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .build()
    }
    
    private fun captureScreenshot(
        complaintId: String, 
        detectedWord: String?, 
        secretCode: String?, 
        shouldLaunchLockScreen: Boolean
    ) {
        var localMediaProjection: MediaProjection? = null
        var virtualDisplay: VirtualDisplay? = null
        var imageReader: ImageReader? = null
        
        try {
            if (resultData == null) {
                Log.e(TAG, "❌ MediaProjection data not available")
                stopSelf()
                return
            }
            
            // Create FRESH MediaProjection instance for this capture
            val mediaProjectionManager = getSystemService(Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
            localMediaProjection = mediaProjectionManager.getMediaProjection(resultCode, resultData!!)
            
            if (localMediaProjection == null) {
                Log.e(TAG, "❌ Failed to get MediaProjection")
                stopSelf()
                return
            }
            
            // CRITICAL: Register callback BEFORE createVirtualDisplay (required for Android 14+)
            localMediaProjection.registerCallback(object : MediaProjection.Callback() {
                override fun onStop() {
                    Log.d(TAG, "MediaProjection stopped")
                }
            }, Handler(Looper.getMainLooper()))
            
            val windowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager
            val displayMetrics = DisplayMetrics()
            windowManager.defaultDisplay.getRealMetrics(displayMetrics)
            
            val width = displayMetrics.widthPixels
            val height = displayMetrics.heightPixels
            val density = displayMetrics.densityDpi
            
            Log.d(TAG, "📏 Display: ${width}x${height} @ ${density}dpi")
            
            imageReader = ImageReader.newInstance(width, height, PixelFormat.RGBA_8888, 2)
            
            virtualDisplay = localMediaProjection.createVirtualDisplay(
                "ScreenCapture",
                width, height, density,
                DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
                imageReader.surface,
                null, null
            )
            
            // Capture references for cleanup
            val captureMediaProjection = localMediaProjection
            val captureVirtualDisplay = virtualDisplay
            val captureImageReader = imageReader
            
            // Small delay to ensure screen is rendered (reduced for speed)
            Handler(Looper.getMainLooper()).postDelayed({
                try {
                    val image: Image? = captureImageReader.acquireLatestImage()
                    if (image != null) {
                        val bitmap = imageToBitmap(image, width, height)
                        image.close()
                        
                        Log.d(TAG, "✅ Screenshot captured successfully")
                        
                        // Upload to Firebase
                        uploadToFirebase(bitmap, complaintId)
                        
                        // Cleanup immediately after capture
                        captureVirtualDisplay?.release()
                        captureImageReader.close()
                        captureMediaProjection.stop()
                        
                        Log.d(TAG, "🧹 Resources cleaned up")
                        
                        // Launch lock screen IMMEDIATELY after screenshot captured
                        if (shouldLaunchLockScreen && !detectedWord.isNullOrEmpty() && !secretCode.isNullOrEmpty()) {
                            Handler(Looper.getMainLooper()).postDelayed({
                                launchLockScreen(complaintId, detectedWord, secretCode)
                            }, 300) // Minimal delay - just enough for upload to start
                        }
                        
                        // Stop service after 30 seconds (give upload time)
                        Handler(Looper.getMainLooper()).postDelayed({
                            stopSelf()
                        }, 30000)
                    } else {
                        Log.e(TAG, "❌ Failed to acquire image")
                        captureVirtualDisplay?.release()
                        captureImageReader.close()
                        captureMediaProjection.stop()
                        
                        // Launch lock screen even if screenshot failed
                        if (shouldLaunchLockScreen && !detectedWord.isNullOrEmpty() && !secretCode.isNullOrEmpty()) {
                            launchLockScreen(complaintId, detectedWord, secretCode)
                        }
                        
                        stopSelf()
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "❌ Error processing screenshot", e)
                    captureVirtualDisplay?.release()
                    captureImageReader?.close()
                    captureMediaProjection.stop()
                    
                    // Launch lock screen even on error
                    if (shouldLaunchLockScreen && !detectedWord.isNullOrEmpty() && !secretCode.isNullOrEmpty()) {
                        launchLockScreen(complaintId, detectedWord, secretCode)
                    }
                    
                    stopSelf()
                }
            }, 500)
            
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error in screenshot capture", e)
            virtualDisplay?.release()
            imageReader?.close()
            localMediaProjection?.stop()
            
            // Launch lock screen even on error
            if (shouldLaunchLockScreen && !detectedWord.isNullOrEmpty() && !secretCode.isNullOrEmpty()) {
                launchLockScreen(complaintId, detectedWord, secretCode)
            }
            
            stopSelf()
        }
    }
    
    private fun launchLockScreen(complaintId: String, detectedWord: String, secretCode: String) {
        try {
            Log.d(TAG, "🔒 Launching lock screen for complaint: $complaintId")
            
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
            
            Log.d(TAG, "✅ Lock screen launched successfully")
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error launching lock screen", e)
        }
    }
    
    private fun imageToBitmap(image: Image, width: Int, height: Int): Bitmap {
        val planes = image.planes
        val buffer = planes[0].buffer
        val pixelStride = planes[0].pixelStride
        val rowStride = planes[0].rowStride
        val rowPadding = rowStride - pixelStride * width
        
        val bitmap = Bitmap.createBitmap(
            width + rowPadding / pixelStride,
            height,
            Bitmap.Config.ARGB_8888
        )
        bitmap.copyPixelsFromBuffer(buffer)
        
        // Crop to actual size
        return Bitmap.createBitmap(bitmap, 0, 0, width, height)
    }
    
    private fun uploadToFirebase(bitmap: Bitmap, complaintId: String) {
        serviceScope.launch {
            try {
                Log.d(TAG, "🔄 Starting upload process for complaint: $complaintId")
                
                // Compress to JPEG
                val baos = ByteArrayOutputStream()
                bitmap.compress(Bitmap.CompressFormat.JPEG, 80, baos)
                val data = baos.toByteArray()
                
                Log.d(TAG, "⬆️ Uploading screenshot (${data.size} bytes) to Firebase Storage...")
                
                // Upload to Firebase Storage
                val storage = FirebaseStorage.getInstance()
                val storageRef = storage.reference
                val screenshotRef = storageRef.child("screenshots/${complaintId}.jpg")
                
                Log.d(TAG, "📤 Starting putBytes to ${screenshotRef.path}")
                screenshotRef.putBytes(data).await()
                Log.d(TAG, "✅ putBytes completed successfully")
                
                Log.d(TAG, "🔗 Getting download URL...")
                val downloadUrl = screenshotRef.downloadUrl.await()
                Log.d(TAG, "✅ Download URL obtained: $downloadUrl")
                
                // Update complaint with screenshot URL
                Log.d(TAG, "💾 Updating Firestore complaint document...")
                val repository = UserRepository(this@ScreenshotCaptureService)
                val updateResult = repository.updateComplaintScreenshot(complaintId, downloadUrl.toString())
                
                if (updateResult.isSuccess) {
                    Log.d(TAG, "✅✅✅ SUCCESS! Complaint $complaintId updated with screenshot URL: $downloadUrl")
                } else {
                    Log.e(TAG, "❌ Failed to update Firestore: ${updateResult.exceptionOrNull()}")
                }
                
                // Recycle bitmap
                bitmap.recycle()
                Log.d(TAG, "🧹 Bitmap recycled, upload complete")
                
                // Stop service immediately after successful upload
                Handler(Looper.getMainLooper()).post {
                    stopSelf()
                }
                
            } catch (e: Exception) {
                Log.e(TAG, "❌❌❌ CRITICAL ERROR uploading screenshot for $complaintId", e)
                e.printStackTrace()
                
                // Stop service even on error
                Handler(Looper.getMainLooper()).post {
                    stopSelf()
                }
            }
        }
    }
    
    override fun onDestroy() {
        super.onDestroy()
        // Cancel any ongoing uploads
        serviceScope.cancel()
        Log.d(TAG, "Service destroyed, coroutine scope cancelled")
    }
}

