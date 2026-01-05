package com.example.phonelock

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import android.content.Intent
import android.content.ComponentName
import android.app.admin.DevicePolicyManager
import android.content.Context
import android.provider.Settings
import androidx.compose.runtime.Composable
import com.example.phonelock.models.UserRole
import com.example.phonelock.repository.UserRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import android.util.Log

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        val repository = UserRepository(this)
        
        // Check if user is already logged in
        if (repository.isUserLoggedIn()) {
            // Check user role and navigate accordingly
            val userRole = repository.getUserRole()
            when (userRole) {
                UserRole.CHILD -> {
                    // Child user - check if parent linked, if yes go to permissions, else dashboard
                    CoroutineScope(Dispatchers.Main).launch {
                        val childResult = repository.getChildData()
                        if (childResult.isSuccess) {
                            val child = childResult.getOrNull()
                            if (child?.parentLinked == true && !arePermissionsGranted()) {
                                // Parent linked but permissions not granted - ask for permissions
                                startActivity(Intent(this@MainActivity, PermissionSetupActivity::class.java))
                            } else {
                                // Go to dashboard
                                startActivity(Intent(this@MainActivity, ChildDashboardActivity::class.java))
                            }
                        } else {
                            // Error loading data - go to dashboard anyway
                            startActivity(Intent(this@MainActivity, ChildDashboardActivity::class.java))
                        }
                        finish()
                    }
                    return // Exit function early, finish() is called in the coroutine
                }
                UserRole.PARENT -> {
                    // Parent user - go to parent dashboard
                    startActivity(Intent(this, ParentDashboardActivity::class.java))
                }
                null -> {
                    // No role stored - go to role selection
                    startActivity(Intent(this, RoleSelectionActivity::class.java))
                }
            }
        } else {
            // Not logged in - go to role selection
            startActivity(Intent(this, RoleSelectionActivity::class.java))
        }
        finish()
    }
    
    private fun arePermissionsGranted(): Boolean {
        // Check Device Admin permission
        val dpm = getSystemService(Context.DEVICE_POLICY_SERVICE) as DevicePolicyManager
        val componentName = ComponentName(this, MyDeviceAdminReceiver::class.java)
        val isDeviceAdmin = dpm.isAdminActive(componentName)
        
        // Check System Alert Window permission
        val canDrawOverlays = Settings.canDrawOverlays(this)
        
        // Check Accessibility Service (simplified check)
        val accessibilityEnabled = isAccessibilityServiceEnabled()
        
        return isDeviceAdmin && canDrawOverlays && accessibilityEnabled
    }
    
    private fun isAccessibilityServiceEnabled(): Boolean {
        var accessibilityEnabled = 0
        val service = packageName + "/" + AbuseDetectionService::class.java.canonicalName
        
        try {
            accessibilityEnabled = Settings.Secure.getInt(
                contentResolver,
                Settings.Secure.ACCESSIBILITY_ENABLED
            )
        } catch (e: Settings.SettingNotFoundException) {
            Log.e("MainActivity", "Accessibility setting not found", e)
        }
        
        if (accessibilityEnabled == 1) {
            val settingValue = Settings.Secure.getString(
                contentResolver,
                Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
            )
            
            if (settingValue != null) {
                // Check for various possible formats
                return settingValue.contains(packageName) || 
                       settingValue.contains("AbuseDetectionService") ||
                       settingValue.lowercase().contains("phonelock")
            }
        }
        
        return false
    }
}