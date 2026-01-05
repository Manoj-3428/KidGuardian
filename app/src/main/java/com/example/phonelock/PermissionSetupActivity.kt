package com.example.phonelock

import android.app.Activity
import android.app.admin.DevicePolicyManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import android.widget.Toast
import androidx.compose.runtime.Composable
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.phonelock.repository.UserRepository
import kotlinx.coroutines.launch
import androidx.compose.runtime.DisposableEffect

class PermissionSetupActivity : ComponentActivity() {
    private val viewModel: PermissionSetupViewModel by viewModels()
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        val repository = UserRepository(this)
        viewModel.setRepository(repository, this)
        
        setContent {
            MaterialTheme {
                PermissionSetupScreen(
                    viewModel = viewModel,
                    activity = this@PermissionSetupActivity,
                    onNavigateToDashboard = {
                        val intent = Intent(this, ChildDashboardActivity::class.java)
                        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                        startActivity(intent)
                        finish()
                    }
                )
            }
        }
        
        // Check permissions after content is set and activity is ready
        window.decorView.post {
            viewModel.refreshPermissions(this)
            // Check again after a short delay to ensure all checks complete
            android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
                viewModel.refreshPermissions(this)
            }, 200)
        }
    }
    
    override fun onResume() {
        super.onResume()
        // Refresh permissions when user returns from settings
        // Use post to ensure activity is fully resumed
        window.decorView.post {
            viewModel.refreshPermissions(this)
            // Check again after a delay to catch any delayed updates
            android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
                viewModel.refreshPermissions(this)
            }, 300)
        }
    }
    
    override fun onStart() {
        super.onStart()
        // Check permissions when activity starts
        window.decorView.post {
            viewModel.refreshPermissions(this)
        }
    }
}

class PermissionSetupViewModel : ViewModel() {
    private lateinit var repository: UserRepository
    private var activityContext: Context? = null
    
    private val _permissionsGranted = mutableStateOf(0)
    val permissionsGranted = _permissionsGranted
    
    private val _totalPermissions = mutableStateOf(3)
    val totalPermissions = _totalPermissions
    
    // Reactive permission states
    private val _isAccessibilityGranted = mutableStateOf(false)
    val isAccessibilityGranted: State<Boolean> = _isAccessibilityGranted
    
    private val _isDeviceAdminGranted = mutableStateOf(false)
    val isDeviceAdminGranted: State<Boolean> = _isDeviceAdminGranted
    
    private val _isSystemAlertGranted = mutableStateOf(false)
    val isSystemAlertGranted: State<Boolean> = _isSystemAlertGranted
    
    fun setRepository(repo: UserRepository, context: Context) {
        repository = repo
        activityContext = context
        // Don't check here - wait for activity to be fully ready
        // Permissions will be checked in onCreate after content is set
    }
    
    fun refreshPermissions(context: Context? = null) {
        // Use provided context or fall back to activity context or repository context
        val ctx = context ?: activityContext ?: repository.context
        
        if (ctx == null) {
            android.util.Log.w("PermissionSetup", "refreshPermissions called but context is null")
            return
        }
        
        android.util.Log.d("PermissionSetup", "refreshPermissions called with context: ${ctx.javaClass.simpleName}")
        
        // Always run permission checks and update state on main thread
        val updateState: () -> Unit = {
            try {
                // Update individual permission states
                val accessibility = isAccessibilityGranted(ctx)
                val deviceAdmin = isDeviceAdminGranted(ctx)
                val systemAlert = isSystemAlertGranted(ctx)
                
                android.util.Log.d("PermissionSetup", "Permission check results - Accessibility: $accessibility, DeviceAdmin: $deviceAdmin, SystemAlert: $systemAlert")
                
                // Update state
                _isAccessibilityGranted.value = accessibility
                _isDeviceAdminGranted.value = deviceAdmin
                _isSystemAlertGranted.value = systemAlert
                
                // Count granted permissions
                val granted = listOf(accessibility, deviceAdmin, systemAlert)
                _permissionsGranted.value = granted.count { it }
                
                android.util.Log.d("PermissionSetup", "State updated - Accessibility: $accessibility, DeviceAdmin: $deviceAdmin, SystemAlert: $systemAlert, Total: ${_permissionsGranted.value}")
            } catch (e: Exception) {
                android.util.Log.e("PermissionSetup", "Error refreshing permissions", e)
            }
        }
        
        // Ensure we're on main thread
        if (android.os.Looper.myLooper() == android.os.Looper.getMainLooper()) {
            updateState()
        } else {
            android.os.Handler(android.os.Looper.getMainLooper()).post(updateState)
        }
    }
    
    private fun isAccessibilityGranted(context: Context): Boolean {
        return try {
            var accessibilityEnabled = 0
            
            try {
                accessibilityEnabled = Settings.Secure.getInt(
                    context.contentResolver,
                    Settings.Secure.ACCESSIBILITY_ENABLED
                )
            } catch (e: Settings.SettingNotFoundException) {
                android.util.Log.d("PermissionSetup", "Accessibility setting not found")
                return false
            }
            
            if (accessibilityEnabled == 1) {
                val settingValue = Settings.Secure.getString(
                    context.contentResolver,
                    Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
                )
                
                if (settingValue != null && settingValue.isNotEmpty()) {
                    val packageName = context.packageName
                    val serviceName = AbuseDetectionService::class.java.canonicalName
                    val fullServicePath = "$packageName/$serviceName"
                    
                    android.util.Log.d("PermissionSetup", "Checking accessibility - package: $packageName")
                    android.util.Log.d("PermissionSetup", "Service name: $serviceName")
                    android.util.Log.d("PermissionSetup", "Full service path: $fullServicePath")
                    android.util.Log.d("PermissionSetup", "Enabled services: $settingValue")
                    
                    // Check for various possible formats - be more thorough
                    val isGranted = settingValue.contains(fullServicePath) ||
                           settingValue.contains("$packageName/") ||
                           settingValue.contains("AbuseDetectionService") ||
                           settingValue.lowercase().contains("phonelock") ||
                           settingValue.contains(packageName)
                    
                    android.util.Log.d("PermissionSetup", "Accessibility granted: $isGranted")
                    return isGranted
                } else {
                    android.util.Log.d("PermissionSetup", "Accessibility services string is null or empty")
                }
            } else {
                android.util.Log.d("PermissionSetup", "Accessibility is not enabled (value: $accessibilityEnabled)")
            }
            
            false
        } catch (e: Exception) {
            android.util.Log.e("PermissionSetup", "Error checking accessibility", e)
            false
        }
    }
    
    private fun isDeviceAdminGranted(context: Context): Boolean {
        return try {
            val dpm = context.getSystemService(Context.DEVICE_POLICY_SERVICE) as DevicePolicyManager
            val componentName = ComponentName(context, MyDeviceAdminReceiver::class.java)
            val isGranted = dpm.isAdminActive(componentName)
            android.util.Log.d("PermissionSetup", "Device Admin granted: $isGranted, component: $componentName")
            return isGranted
        } catch (e: Exception) {
            android.util.Log.e("PermissionSetup", "Error checking device admin", e)
            false
        }
    }
    
    private fun isSystemAlertGranted(context: Context): Boolean {
        return try {
            val isGranted = Settings.canDrawOverlays(context)
            android.util.Log.d("PermissionSetup", "System Alert granted: $isGranted")
            return isGranted
        } catch (e: Exception) {
            android.util.Log.e("PermissionSetup", "Error checking system alert", e)
            false
        }
    }
}

@Composable
fun PermissionSetupScreen(
    viewModel: PermissionSetupViewModel,
    activity: Activity,
    onNavigateToDashboard: () -> Unit
) {
    val context = LocalContext.current
    
    // Check permissions when composable is displayed and continuously
    LaunchedEffect(Unit) {
        android.util.Log.d("PermissionSetup", "LaunchedEffect started - checking permissions")
        // Check immediately on first load with activity context
        viewModel.refreshPermissions(activity)
        kotlinx.coroutines.delay(100) // Small delay to ensure activity is ready
        viewModel.refreshPermissions(activity)
        kotlinx.coroutines.delay(200) // Another check
        viewModel.refreshPermissions(activity)
        
        // Then check continuously every 500ms for updates
        while (true) {
            kotlinx.coroutines.delay(500) // Check every 500ms
            viewModel.refreshPermissions(activity)
        }
    }
    
    // Animated background
    val gradientColors = listOf(
        Color(0xFF3B82F6),
        Color(0xFF1E40AF),
        Color(0xFF1E3A8A)
    )
    
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.linearGradient(
                    colors = gradientColors,
                    start = androidx.compose.ui.geometry.Offset(0f, 0f),
                    end = androidx.compose.ui.geometry.Offset(
                        androidx.compose.ui.geometry.Offset.Infinite.x,
                        androidx.compose.ui.geometry.Offset.Infinite.y
                    )
                )
            )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(40.dp))
            
            // Header
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .shadow(8.dp, RoundedCornerShape(16.dp)),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        imageVector = Icons.Default.Lock,
                        contentDescription = "Security Setup",
                        modifier = Modifier.size(48.dp),
                        tint = Color(0xFF3B82F6)
                    )
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    Text(
                        text = "Permission Setup",
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF1F2937)
                    )
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    Text(
                        text = "Grant required permissions for app to work",
                        fontSize = 14.sp,
                        color = Color.Gray,
                        textAlign = TextAlign.Center
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(24.dp))
            
            // Progress Card
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .shadow(4.dp, RoundedCornerShape(12.dp)),
                colors = CardDefaults.cardColors(containerColor = Color(0xFFF0F9FF)),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(
                    modifier = Modifier.padding(20.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Setup Progress",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF3B82F6)
                        )
                        Text(
                            text = "${viewModel.permissionsGranted.value}/${viewModel.totalPermissions.value}",
                            fontSize = 14.sp,
                            color = Color.Gray
                        )
                    }
                    
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    LinearProgressIndicator(
                        progress = { viewModel.permissionsGranted.value.toFloat() / viewModel.totalPermissions.value.toFloat() },
                        modifier = Modifier.fillMaxWidth(),
                        color = Color(0xFF3B82F6),
                        trackColor = Color(0xFFE5E7EB)
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Device Admin Activity Result Launcher
            val deviceAdminLauncher = rememberLauncherForActivityResult(
                contract = ActivityResultContracts.StartActivityForResult()
            ) { result ->
                // Refresh permissions after user returns from device admin settings
                viewModel.refreshPermissions(activity)
            }
            
            // Permission Cards
            PermissionCard(
                title = "Accessibility Service",
                description = "Required to detect inappropriate words on screen",
                icon = Icons.Default.FavoriteBorder,
                onGrant = {
                    val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
                    context.startActivity(intent)
                },
                viewModel = viewModel,
                activity = activity,
                isGranted = viewModel.isAccessibilityGranted.value
            )
            
            Spacer(modifier = Modifier.height(12.dp))
            
            PermissionCard(
                title = "Device Admin",
                description = "Required to block screen and enter kiosk mode",
                icon = Icons.Default.Lock,
                onGrant = {
                    val intent = Intent(DevicePolicyManager.ACTION_ADD_DEVICE_ADMIN).apply {
                        putExtra(DevicePolicyManager.EXTRA_DEVICE_ADMIN, ComponentName(context, MyDeviceAdminReceiver::class.java))
                        putExtra(DevicePolicyManager.EXTRA_ADD_EXPLANATION, "Required for screen blocking and kiosk mode")
                    }
                    deviceAdminLauncher.launch(intent)
                },
                viewModel = viewModel,
                activity = activity,
                isGranted = viewModel.isDeviceAdminGranted.value
            )
            
            Spacer(modifier = Modifier.height(12.dp))
            
            PermissionCard(
                title = "System Alert Window",
                description = "Required to display lock screen over other apps",
                icon = Icons.Default.Warning,
                onGrant = {
                    val intent = Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION).apply {
                        data = android.net.Uri.parse("package:${context.packageName}")
                    }
                    context.startActivity(intent)
                },
                viewModel = viewModel,
                activity = activity,
                isGranted = viewModel.isSystemAlertGranted.value
            )
            
            Spacer(modifier = Modifier.height(24.dp))
            
            // Continue Button
            Button(
                onClick = {
                    if (viewModel.permissionsGranted.value >= viewModel.totalPermissions.value) {
                        onNavigateToDashboard()
                    } else {
                        Toast.makeText(context, "Please grant all permissions first", Toast.LENGTH_SHORT).show()
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF3B82F6)),
                shape = RoundedCornerShape(12.dp),
                enabled = viewModel.permissionsGranted.value >= viewModel.totalPermissions.value
            ) {
                Text(
                    text = if (viewModel.permissionsGranted.value >= viewModel.totalPermissions.value) {
                        "Continue to Dashboard"
                    } else {
                        "Grant All Permissions First"
                    },
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Text(
                text = "All permissions are required for the app to function properly",
                fontSize = 12.sp,
                color = Color.White.copy(alpha = 0.8f),
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
fun PermissionCard(
    title: String,
    description: String,
    icon: ImageVector,
    onGrant: () -> Unit,
    viewModel: PermissionSetupViewModel? = null,
    activity: Activity? = null,
    isGranted: Boolean = false
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(2.dp, RoundedCornerShape(8.dp)),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        shape = RoundedCornerShape(8.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = title,
                modifier = Modifier.size(32.dp),
                tint = Color(0xFF3B82F6)
            )
            
            Spacer(modifier = Modifier.width(16.dp))
            
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = title,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF1F2937)
                )
                
                Spacer(modifier = Modifier.height(4.dp))
                
                Text(
                    text = description,
                    fontSize = 12.sp,
                    color = Color.Gray
                )
            }
            
            if (isGranted) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = "Granted",
                        tint = Color(0xFF10B981),
                        modifier = Modifier.size(20.dp)
                    )
                    Text(
                        text = "Granted",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color(0xFF10B981)
                    )
                }
            } else {
                Button(
                    onClick = {
                        onGrant()
                        // Refresh permissions after a delay to allow user to grant them
                        viewModel?.let { vm ->
                            activity?.let { act ->
                                kotlinx.coroutines.GlobalScope.launch {
                                    kotlinx.coroutines.delay(2000) // Wait 2 seconds
                                    vm.refreshPermissions(act)
                                }
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF3B82F6)),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("Grant", fontSize = 12.sp, fontWeight = FontWeight.Medium)
                }
            }
        }
    }
}