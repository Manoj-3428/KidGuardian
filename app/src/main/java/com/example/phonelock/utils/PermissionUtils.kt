package com.example.phonelock.utils

import android.app.admin.DevicePolicyManager
import android.content.ComponentName
import android.content.Context
import android.provider.Settings
import com.example.phonelock.MyDeviceAdminReceiver

fun arePermissionsGranted(context: Context): Boolean {
    return try {
        val dpm = context.getSystemService(Context.DEVICE_POLICY_SERVICE) as DevicePolicyManager
        val componentName = ComponentName(context, MyDeviceAdminReceiver::class.java)
        val isDeviceAdmin = dpm.isAdminActive(componentName)
        
        val canDrawOverlays = Settings.canDrawOverlays(context)
        val accessibilityEnabled = isAccessibilityServiceEnabled(context)
        
        isDeviceAdmin && canDrawOverlays && accessibilityEnabled
    } catch (_: Exception) {
        false
    }
}

fun isAccessibilityServiceEnabled(context: Context): Boolean {
    var accessibilityEnabled = 0
    
    try {
        accessibilityEnabled = Settings.Secure.getInt(
            context.contentResolver,
            Settings.Secure.ACCESSIBILITY_ENABLED
        )
    } catch (_: Settings.SettingNotFoundException) {
        return false
    }
    
    if (accessibilityEnabled == 1) {
        val settingValue = Settings.Secure.getString(
            context.contentResolver,
            Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
        )
        
        if (settingValue != null) {
            return settingValue.contains(context.packageName) || 
                   settingValue.contains("AbuseDetectionService") ||
                   settingValue.lowercase().contains("phonelock")
        }
    }
    
    return false
}

