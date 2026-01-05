package com.example.phonelock

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import android.view.View
import android.view.WindowManager
import android.app.admin.DevicePolicyManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.phonelock.repository.UserRepository
import kotlinx.coroutines.launch

class LockScreenActivity : ComponentActivity() {
    private val viewModel: LockScreenViewModel by viewModels()
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        Log.d("LockScreen", "LOCK SCREEN STARTING...")

        // Fullscreen window flags
        window.addFlags(
            WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
        )
        
        // Modern fullscreen approach
        window.setFlags(
            WindowManager.LayoutParams.FLAG_FULLSCREEN,
            WindowManager.LayoutParams.FLAG_FULLSCREEN
        )

        // Get data from intent FIRST
        val complaintId = intent.getStringExtra("COMPLAINT_ID")
        val detectedWord = intent.getStringExtra("DETECTED_WORD")
        val secretCode = intent.getStringExtra("SECRET_CODE") ?: "000000"
        
        Log.d("LockScreen", "Secret Code: $secretCode")
        Log.d("LockScreen", "Detected Word: $detectedWord")
        
        // Setup repository and viewmodel
        val repository = UserRepository(this)
        viewModel.setRepository(repository)
        viewModel.setSecretCode(secretCode)
        viewModel.setLoading(false)

        // START TRUE KIOSK MODE (requires Device Owner)
        try {
            val dpm = getSystemService(Context.DEVICE_POLICY_SERVICE) as DevicePolicyManager
            val componentName = ComponentName(this, MyDeviceAdminReceiver::class.java)
            
            if (dpm.isDeviceOwnerApp(packageName)) {
                Log.d("LockScreen", "Device Owner confirmed - starting kiosk mode")
                dpm.setLockTaskPackages(componentName, arrayOf(packageName))
                startLockTask()
                Log.d("LockScreen", "✅ KIOSK MODE ACTIVE")
            } else {
                Log.w("LockScreen", "Not device owner - kiosk mode unavailable")
            }
        } catch (e: Exception) {
            Log.e("LockScreen", "Kiosk mode error", e)
        }

        // Set UI
        setContent {
            MaterialTheme {
                LockScreenUI(
                    secretCode = secretCode,
                    detectedWord = detectedWord,
                    onUnlock = {
                        Log.d("LockScreen", "Unlocking...")
                        
                        // Stop kiosk mode
                        try {
                            stopLockTask()
                            Log.d("LockScreen", "Kiosk mode stopped")
                        } catch (e: Exception) {
                            Log.e("LockScreen", "Error stopping kiosk", e)
                        }
                        
                        // Stop monitor service
                        LockMonitorService.stop(this@LockScreenActivity)
                        
                        // Mark accessed
                        complaintId?.let {
                            viewModel.markComplaintAsAccessed(it)
                        }
                        
                        // Go to dashboard
                        val intent = Intent(this@LockScreenActivity, ChildDashboardActivity::class.java)
                        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                        startActivity(intent)
                        finishAffinity()
                    }
                )
            }
        }
    }

    override fun onResume() {
        super.onResume()
        hideSystemUI()
    }

    private fun hideSystemUI() {
        // Modern approach using WindowInsetsController
        window.decorView.systemUiVisibility = (
            View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
            or View.SYSTEM_UI_FLAG_LAYOUT_STABLE
            or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
            or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
            or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
            or View.SYSTEM_UI_FLAG_FULLSCREEN
        )
    }

    // Use modern back handling instead of deprecated onBackPressed
    override fun onBackPressed() {
        // Block back button - do nothing
        Log.d("LockScreen", "Back button blocked")
        // Don't call super.onBackPressed() to prevent default behavior
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus) {
            hideSystemUI()
        } else {
            // If user tries to leave, bring them back
            Log.d("LockScreen", "Lost focus - attempting to regain")
        }
    }
    
}

class LockScreenViewModel : ViewModel() {
    private lateinit var repository: UserRepository
    
    private val _secretCode = mutableStateOf("")
    val secretCode: State<String> = _secretCode
    
    private val _isLoading = mutableStateOf(true)
    val isLoading: State<Boolean> = _isLoading
    
    fun setRepository(repo: UserRepository) {
        repository = repo
    }
    
    fun setSecretCode(code: String) {
        _secretCode.value = code
    }
    
    fun loadSecretCode(complaintId: String) {
        viewModelScope.launch {
            val result = repository.getComplaintById(complaintId)
            if (result.isSuccess) {
                val complaint = result.getOrNull()
                _secretCode.value = complaint?.secretCode ?: ""
            } else {
                _secretCode.value = ""
            }
            _isLoading.value = false
        }
    }
    
    fun setLoading(loading: Boolean) {
        _isLoading.value = loading
    }
    
    fun markComplaintAsAccessed(complaintId: String) {
        viewModelScope.launch {
            repository.markComplaintAsAccessed(complaintId)
        }
    }
}

@Composable
fun LockScreenUI(
    secretCode: String,
    detectedWord: String?,
    onUnlock: () -> Unit
) {
    val context = LocalContext.current
    var input by remember { mutableStateOf("") }
    var showError by remember { mutableStateOf(false) }
    
    // Beautiful gradient background
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF1E3A8A), // Deep blue
                        Color(0xFF3B82F6), // Blue
                        Color(0xFF60A5FA)  // Light blue
                    )
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        // Subtle floating particles
        repeat(8) { index ->
            val size = (20 + index * 8).dp
            val delay = index * 500
            val floatAnimation by rememberInfiniteTransition(label = "float$index").animateFloat(
                initialValue = -20f,
                targetValue = 20f,
                animationSpec = infiniteRepeatable(
                    animation = tween(4000 + delay, easing = EaseInOutCubic),
                    repeatMode = RepeatMode.Reverse
                ),
                label = "float$index"
            )
            
            Box(
                modifier = Modifier
                    .size(size)
                    .offset(
                        x = (50 + index * 120).dp,
                        y = (100 + index * 150 + floatAnimation).dp
                    )
                    .background(
                        color = Color.White.copy(alpha = 0.1f),
                        shape = RoundedCornerShape(50)
                    )
            )
        }
        
        // Main content card
        Card(
            modifier = Modifier
                .fillMaxWidth(0.9f)
                .padding(16.dp),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(
                containerColor = Color.White
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 12.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Lock icon with beautiful styling
                Box(
                    modifier = Modifier
                        .size(80.dp)
                        .background(
                            brush = Brush.radialGradient(
                                colors = listOf(
                                    Color(0xFF3B82F6),
                                    Color(0xFF1E40AF)
                                )
                            ),
                            shape = RoundedCornerShape(20.dp)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Lock,
                        contentDescription = "Locked",
                        modifier = Modifier.size(40.dp),
                        tint = Color.White
                    )
                }
                
                Spacer(modifier = Modifier.height(24.dp))
                
                // Title
                Text(
                    text = "Device Locked",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF1F2937),
                    textAlign = TextAlign.Center
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                // Subtitle
                Text(
                    text = "Your device has been locked for your safety",
                    fontSize = 14.sp,
                    color = Color(0xFF6B7280),
                    textAlign = TextAlign.Center,
                    lineHeight = 20.sp
                )
                
                // Show detected word if available
                if (!detectedWord.isNullOrEmpty()) {
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = Color(0xFFFEF2F2)
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                imageVector = Icons.Default.Warning,
                                contentDescription = "Warning",
                                modifier = Modifier.size(20.dp),
                                tint = Color(0xFFDC2626)
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "Inappropriate content detected",
                                fontSize = 12.sp,
                                color = Color(0xFF991B1B),
                                textAlign = TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "\"$detectedWord\"",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFFDC2626),
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(24.dp))
                
                // Instructions
                Text(
                    text = "Ask your parent for the unlock code",
                    fontSize = 16.sp,
                    color = Color(0xFF374151),
                    textAlign = TextAlign.Center,
                    fontWeight = FontWeight.Medium
                )
                
                Spacer(modifier = Modifier.height(24.dp))
                
                // Code input field
                OutlinedTextField(
                    value = input,
                    onValueChange = { 
                        if (it.length <= 6) {
                            input = it
                            showError = false
                        }
                    },
                    label = { 
                        Text(
                            "Enter 6-digit code",
                            color = Color(0xFF6B7280)
                        ) 
                    },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = if (showError) Color(0xFFDC2626) else Color(0xFF3B82F6),
                        focusedLabelColor = if (showError) Color(0xFFDC2626) else Color(0xFF3B82F6),
                        cursorColor = Color(0xFF3B82F6),
                        errorBorderColor = Color(0xFFDC2626),
                        errorLabelColor = Color(0xFFDC2626)
                    ),
                    shape = RoundedCornerShape(12.dp),
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Lock,
                            contentDescription = "Code",
                            tint = if (showError) Color(0xFFDC2626) else Color(0xFF3B82F6)
                        )
                    },
                    isError = showError
                )
                
                Spacer(modifier = Modifier.height(24.dp))
                
                // Unlock button
                Button(
                    onClick = {
                        // Allow unlock if: correct OTP OR empty input (for testing)
                        if (input.isEmpty() || (input == secretCode && secretCode.isNotEmpty())) {
                            Toast.makeText(context, "✅ Device Unlocked!", Toast.LENGTH_SHORT).show()
                            onUnlock()
                        } else {
                            showError = true
                            Toast.makeText(context, "❌ Incorrect code. Please try again.", Toast.LENGTH_SHORT).show()
                            input = ""
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF3B82F6)
                    ),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Row(
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Lock,
                            contentDescription = "Unlock",
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Unlock Device",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // Footer
                Text(
                    text = "Protected by KidGuardian",
                    fontSize = 12.sp,
                    color = Color(0xFF9CA3AF),
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}