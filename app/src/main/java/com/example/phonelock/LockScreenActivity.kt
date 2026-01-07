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
import androidx.compose.foundation.Image
import androidx.compose.ui.res.painterResource
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
                        
                        // Prevent duplicate unlocks
                        if (isFinishing) {
                            Log.d("LockScreen", "Already unlocking, ignoring duplicate call")
                            return@LockScreenUI
                        }
                        
                        // Stop kiosk mode
                        try {
                            stopLockTask()
                            Log.d("LockScreen", "Kiosk mode stopped")
                        } catch (e: Exception) {
                            Log.e("LockScreen", "Error stopping kiosk", e)
                        }
                        
                        // Stop monitor service FIRST and mark as unlocked
                        LockMonitorService.stop(this@LockScreenActivity)
                        
                        // Mark accessed
                        complaintId?.let {
                            viewModel.markComplaintAsAccessed(it)
                        }
                        
                        // Finish this activity first to prevent duplicates
                        finish()
                        
                        // Go to dashboard
                        val intent = Intent(this@LockScreenActivity, ChildDashboardActivity::class.java)
                        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                        startActivity(intent)
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
    
    // Modern gradient background
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF0F172A),
                        Color(0xFF1E293B),
                        Color(0xFF334155)
                    )
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth(0.9f)
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // App icon and tagline
            Image(
                painter = painterResource(id = R.drawable.app_icon_modern),
                contentDescription = "App icon",
                modifier = Modifier.size(96.dp)
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "KidGuardian",
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                color = Color(0xFF9CA3AF)
            )
            
            Spacer(modifier = Modifier.height(32.dp))
            
            // Screen is locked message
            Text(
                text = "Screen is Locked",
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                textAlign = TextAlign.Center
            )
            
            Spacer(modifier = Modifier.height(24.dp))
            
            // Detected word section
            if (!detectedWord.isNullOrEmpty()) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = Color(0xFF1F2937)
                    ),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "Detected Word",
                            fontSize = 14.sp,
                            color = Color(0xFF9CA3AF),
                            fontWeight = FontWeight.Medium
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "\"$detectedWord\"",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFEF4444),
                            textAlign = TextAlign.Center
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(32.dp))
            }
            
            // Input field
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
                        "Enter unlock code",
                        color = Color(0xFF9CA3AF)
                    ) 
                },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = if (showError) Color(0xFFEF4444) else Color(0xFF3B82F6),
                    unfocusedBorderColor = if (showError) Color(0xFFEF4444) else Color(0xFF475569),
                    focusedLabelColor = if (showError) Color(0xFFEF4444) else Color(0xFF3B82F6),
                    unfocusedLabelColor = Color(0xFF9CA3AF),
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White,
                    cursorColor = Color(0xFF3B82F6),
                    errorBorderColor = Color(0xFFEF4444),
                    errorLabelColor = Color(0xFFEF4444)
                ),
                shape = RoundedCornerShape(12.dp),
                isError = showError
            )
            
            Spacer(modifier = Modifier.height(24.dp))
            
            // Unlock button
            Button(
                onClick = {
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
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(
                    text = "Unlock",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}