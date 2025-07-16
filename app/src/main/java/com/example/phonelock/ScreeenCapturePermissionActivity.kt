package com.example.phonelock

import android.app.Activity
import android.content.Intent
import android.media.projection.MediaProjectionManager
import android.os.Bundle

class ScreenCapturePermissionActivity : Activity() {

    private val REQUEST_SCREENSHOT = 1001

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val projectionManager = getSystemService(MediaProjectionManager::class.java)
        val permissionIntent = projectionManager.createScreenCaptureIntent()
        startActivityForResult(permissionIntent, REQUEST_SCREENSHOT)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (requestCode == REQUEST_SCREENSHOT && resultCode == RESULT_OK && data != null) {
            // Store in companion object
            ScreenshotService.resultCode = resultCode
            ScreenshotService.dataIntent = data

            // Start the screenshot service
            val intent = Intent(this, ScreenshotService::class.java)
            startService(intent)
        }

        finish()
    }
}
