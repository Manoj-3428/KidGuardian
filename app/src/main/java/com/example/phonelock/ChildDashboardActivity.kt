package com.example.phonelock

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.phonelock.models.Child
import com.example.phonelock.models.Complaint
import com.example.phonelock.repository.UserRepository
import kotlinx.coroutines.launch
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import java.text.SimpleDateFormat
import java.util.*
import android.app.admin.DevicePolicyManager
import android.content.ComponentName
import android.content.Context
import android.provider.Settings
import android.util.Log
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.window.Dialog

class ChildDashboardActivity : ComponentActivity() {
    private val viewModel: ChildDashboardViewModel by viewModels()
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        val repository = UserRepository(this)
        viewModel.setRepository(repository)
        
        setContent {
            MaterialTheme {
                ChildDashboardScreen(
                    viewModel = viewModel,
                    onLogout = {
                        // Clear logout passcode before logging out
                        val childData = viewModel.childData.value
                        if (childData != null) {
                            CoroutineScope(Dispatchers.IO).launch {
                                repository.clearLogoutPasscode(childData.childId)
                            }
                        }
                        repository.logout()
                        startActivity(Intent(this, RoleSelectionActivity::class.java))
                        finish()
                    },
                    onNavigateToPermissions = {
                        startActivity(Intent(this, PermissionSetupActivity::class.java))
                        finish()
                    }
                )
            }
        }
    }
}

class ChildDashboardViewModel : ViewModel() {
    private lateinit var repository: UserRepository
    
    private val _childData = mutableStateOf<Child?>(null)
    val childData: State<Child?> = _childData
    
    private val _complaints = mutableStateOf<List<Complaint>>(emptyList())
    val complaints: State<List<Complaint>> = _complaints
    
    private val _isLoading = mutableStateOf(true)
    val isLoading: State<Boolean> = _isLoading
    
    
    fun setRepository(repo: UserRepository) {
        repository = repo
        loadData()
    }
    
    fun loadData() {
        _isLoading.value = true
        viewModelScope.launch {
            // Load child data
            val childResult = repository.getChildData()
            if (childResult.isSuccess) {
                _childData.value = childResult.getOrNull()
                
                // Load complaints
                _childData.value?.childId?.let { childId ->
                    val complaintsResult = repository.getComplaintsForChild(childId)
                    if (complaintsResult.isSuccess) {
                        _complaints.value = complaintsResult.getOrNull() ?: emptyList()
                    }
                }
            } else {
                    // Handle error silently for now
            }
            _isLoading.value = false
        }
    }
    
    fun refreshData() {
        loadData()
    }
    
    fun refreshDataSilently() {
        // Refresh without showing loader
        viewModelScope.launch {
            // Load child data
            val childResult = repository.getChildData()
            if (childResult.isSuccess) {
                _childData.value = childResult.getOrNull()
                
                // Load complaints
                _childData.value?.childId?.let { childId ->
                    val complaintsResult = repository.getComplaintsForChild(childId)
                    if (complaintsResult.isSuccess) {
                        _complaints.value = complaintsResult.getOrNull() ?: emptyList()
                    }
                }
            }
        }
    }
    
    fun generateLogoutOTP(childId: String, onResult: (Boolean) -> Unit) {
        viewModelScope.launch {
            try {
                val result = repository.generateAndSaveLogoutPasscode(childId)
                if (result.isSuccess) {
                    Log.d("LOGOUT_OTP", "✅ Logout OTP generated successfully")
                    onResult(true)
                } else {
                    Log.e("LOGOUT_OTP", "Failed to generate logout OTP: ${result.exceptionOrNull()?.message}")
                    onResult(false)
                }
            } catch (e: Exception) {
                Log.e("LOGOUT_OTP", "Error generating logout OTP", e)
                onResult(false)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChildDashboardScreen(
    viewModel: ChildDashboardViewModel,
    onLogout: () -> Unit,
    onNavigateToPermissions: () -> Unit
) {
    val childData = viewModel.childData.value
    val complaints = viewModel.complaints.value
    val isLoading = viewModel.isLoading.value
    val context = androidx.compose.ui.platform.LocalContext.current
    
    // Track if we've already navigated to avoid multiple navigations
    var hasNavigated by remember { mutableStateOf(false) }
    
    // Logout verification dialog state
    var showLogoutDialog by remember { mutableStateOf(false) }
    
    // Continuously refresh child data every 3 seconds without showing loader
    LaunchedEffect(Unit) {
        while (true) {
            delay(3000) // Refresh every 3 seconds
            viewModel.refreshDataSilently()
        }
    }
    
    // Immediately navigate to permissions when parent is linked
    LaunchedEffect(childData?.parentLinked) {
        if (childData?.parentLinked == true && !hasNavigated) {
            // Check if permissions are already granted
            val permissionsGranted = arePermissionsGranted(context)
            if (!permissionsGranted) {
                hasNavigated = true
                // Parent just linked and permissions not granted - navigate immediately
                onNavigateToPermissions()
            }
        }
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("My Dashboard", fontWeight = FontWeight.Bold) },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(0xFF8B5CF6),
                    titleContentColor = Color.White,
                    actionIconContentColor = Color.White
                ),
                actions = {
                    IconButton(onClick = { viewModel.refreshData() }) {
                        Icon(Icons.Default.Refresh, "Refresh")
                    }
                    IconButton(onClick = {
                        // Check if parent is linked before allowing logout
                        if (childData?.parentLinked == true) {
                            // Show dialog immediately for better UX
                            showLogoutDialog = true
                            // Generate logout OTP in background (don't wait for it)
                            viewModel.generateLogoutOTP(childData.childId) { success ->
                                if (success) {
                                    // Refresh data silently to get the new logout passcode
                                    viewModel.refreshDataSilently()
                                }
                            }
                        } else {
                            // No parent linked, allow direct logout
                            onLogout()
                        }
                    }) {
                        Icon(Icons.AutoMirrored.Filled.ExitToApp, "Logout")
                    }
                }
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(
                    Brush.linearGradient(
                        colors = listOf(
                            Color(0xFFF3E8FF),
                            Color(0xFFE9D5FF),
                            Color(0xFFDDD6FE)
                        )
                    )
                )
        ) {
            if (isLoading) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = Color(0xFF8B5CF6))
                }
            } else if (childData != null) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(16.dp)
                ) {
                    // Profile Card with Child ID
                    ProfileCard(childData)
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    // Parent Link Status
                    ParentLinkStatusCard(childData)
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    // Recent Activity
                    RecentActivitySection(complaints)
                }
            } else {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text("Loading data...", color = Color.Gray)
                }
            }
        }
    }
    
    // Logout verification dialog
    if (showLogoutDialog) {
        LogoutVerificationDialog(
            childData = childData,
            onDismiss = { showLogoutDialog = false },
            onSuccess = onLogout
        )
    }
}

@Composable
fun ProfileCard(child: Child) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(8.dp, RoundedCornerShape(20.dp)),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color.White
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(70.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(
                            Brush.linearGradient(
                                colors = listOf(
                                    Color(0xFF8B5CF6),
                                    Color(0xFFA855F7)
                                )
                            )
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Person,
                        contentDescription = "Profile",
                        modifier = Modifier.size(36.dp),
                        tint = Color.White
                    )
                }
                
                Spacer(modifier = Modifier.width(16.dp))
                
                Column {
                    Text(
                        text = child.name,
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF1F2937)
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "${child.age} years old • ${child.gender}",
                        fontSize = 14.sp,
                        color = Color.Gray
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Child ID with status
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Your ID",
                        fontSize = 12.sp,
                        color = Color.Gray,
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        text = child.childId,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (child.parentLinked) Color(0xFF8B5CF6) else Color(0xFFEF4444)
                    )
                }
                
                if (!child.parentLinked) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Warning,
                            contentDescription = "Warning",
                            modifier = Modifier.size(16.dp),
                            tint = Color(0xFFEF4444)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Not Linked",
                            fontSize = 12.sp,
                            color = Color(0xFFEF4444),
                            fontWeight = FontWeight.Bold
                        )
                    }
                } else {
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = "Linked",
                            modifier = Modifier.size(16.dp),
                            tint = Color(0xFF10B981)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Linked",
                            fontSize = 12.sp,
                            color = Color(0xFF10B981),
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun LogoutVerificationDialog(
    childData: Child?,
    onDismiss: () -> Unit,
    onSuccess: () -> Unit
) {
    var enteredPasscode by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf("") }
    var isVerifying by remember { mutableStateOf(false) }
    
    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Icon with gradient background
                Box(
                    modifier = Modifier
                        .size(80.dp)
                        .clip(RoundedCornerShape(40.dp))
                        .background(
                            Brush.linearGradient(
                                colors = listOf(
                                    Color(0xFFEF4444),
                                    Color(0xFFDC2626)
                                )
                            )
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ExitToApp,
                        contentDescription = "Logout",
                        tint = Color.White,
                        modifier = Modifier.size(40.dp)
                    )
                }
                
                Spacer(modifier = Modifier.height(20.dp))
                
                // Title
                Text(
                    text = "Parent Verification Required",
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF1F2937),
                    textAlign = TextAlign.Center
                )
                
                Spacer(modifier = Modifier.height(12.dp))
                
                // Description
                Text(
                    text = "To logout, please enter the 6-digit verification code shown on your parent's dashboard.",
                    fontSize = 14.sp,
                    color = Color(0xFF6B7280),
                    textAlign = TextAlign.Center,
                    lineHeight = 20.sp
                )
                
                // Show loading indicator if passcode is not yet generated
                if (childData?.logoutPasscode == null) {
                    Spacer(modifier = Modifier.height(16.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            color = Color(0xFF8B5CF6),
                            strokeWidth = 2.dp
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Generating verification code...",
                            fontSize = 12.sp,
                            color = Color(0xFF8B5CF6),
                            fontStyle = androidx.compose.ui.text.font.FontStyle.Italic
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(24.dp))
                
                // Passcode input field
                OutlinedTextField(
                    value = enteredPasscode,
                    onValueChange = {
                        if (it.length <= 6 && it.all { char -> char.isDigit() }) {
                            enteredPasscode = it
                            errorMessage = ""
                        }
                    },
                    label = { 
                        Text(
                            "Enter 6-digit code",
                            fontSize = 14.sp
                        ) 
                    },
                    placeholder = { 
                        Text(
                            "123456",
                            fontSize = 18.sp,
                            letterSpacing = 3.sp
                        ) 
                    },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    enabled = !isVerifying,
                    textStyle = androidx.compose.ui.text.TextStyle(
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 4.sp
                    ),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Color(0xFF8B5CF6),
                        unfocusedBorderColor = Color(0xFFE5E7EB),
                        focusedLabelColor = Color(0xFF8B5CF6),
                        unfocusedLabelColor = Color(0xFF6B7280)
                    ),
                    shape = RoundedCornerShape(12.dp)
                )
                
                if (errorMessage.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Warning,
                            contentDescription = "Error",
                            tint = Color(0xFFEF4444),
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = errorMessage,
                            color = Color(0xFFEF4444),
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(24.dp))
                
                // Action buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Cancel button
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f),
                        enabled = !isVerifying,
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = Color(0xFF6B7280)
                        )
                    ) {
                        Text(
                            "Cancel",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                    
                    // Verify & Logout button
                    Button(
                        onClick = {
                            if (enteredPasscode.length != 6) {
                                errorMessage = "Code must be 6 digits"
                                return@Button
                            }
                            
                            isVerifying = true
                            
                            // Verify the code matches logoutPasscode
                            if (childData?.logoutPasscode == enteredPasscode) {
                                onSuccess()
                            } else {
                                errorMessage = "Invalid code. Please check parent's dashboard."
                                isVerifying = false
                            }
                        },
                        enabled = !isVerifying && enteredPasscode.length == 6 && childData?.logoutPasscode != null,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFFEF4444),
                            contentColor = Color.White
                        )
                    ) {
                        if (isVerifying) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                color = Color.White,
                                strokeWidth = 2.dp
                            )
                        } else {
                            Text(
                                "Verify & Logout",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ParentLinkStatusCard(child: Child) {
    // Helper function to check if passcode is expired
    fun isPasscodeExpired(generatedAt: Long?, currentTime: Long): Boolean {
        return if (generatedAt == null) {
            true
        } else {
            (currentTime - generatedAt) > UserRepository.PASSCODE_EXPIRY_MS
        }
    }
    
    // Real-time timer for passcode expiry
    var currentTime by remember { mutableLongStateOf(System.currentTimeMillis()) }
    
    // Update timer every second
    LaunchedEffect(child.passcodeGeneratedAt) {
        while (child.passcodeGeneratedAt != null && !isPasscodeExpired(child.passcodeGeneratedAt, currentTime)) {
            delay(1000) // Update every second
            currentTime = System.currentTimeMillis()
        }
    }
    
    // Calculate passcode expiry
    val isPasscodeExpiredFlag = isPasscodeExpired(child.passcodeGeneratedAt, currentTime)
    
    val isPasscodeActive = child.linkPasscode != null && 
                          child.passcodeGeneratedAt != null &&
                          !isPasscodeExpiredFlag
    
    val timeRemaining = if (child.passcodeGeneratedAt != null && isPasscodeActive) {
        val elapsed = currentTime - child.passcodeGeneratedAt
        val remaining = UserRepository.PASSCODE_EXPIRY_MS - elapsed
        if (remaining <= 0) {
            "Expired"
        } else {
            val minutes = (remaining / 60000).toInt()
            val seconds = ((remaining % 60000) / 1000).toInt()
            "$minutes:${seconds.toString().padStart(2, '0')}"
        }
    } else {
        ""
    }
    
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(8.dp, RoundedCornerShape(20.dp)),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = when {
                child.parentLinked -> Color(0xFFDCFCE7) // Green - linked
                isPasscodeActive -> Color(0xFFDEF7EC) // Light green - passcode active
                else -> Color(0xFFFEF3C7) // Yellow - no passcode
            }
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(60.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(
                            when {
                                child.parentLinked -> Color(0xFF10B981)
                                isPasscodeActive -> Color(0xFF059669)
                                else -> Color(0xFFF59E0B)
                            }
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                        imageVector = when {
                            child.parentLinked -> Icons.Default.CheckCircle
                            isPasscodeActive -> Icons.Default.Build
                            else -> Icons.Default.AddCircle
                        },
                    contentDescription = "Status",
                    modifier = Modifier.size(32.dp),
                    tint = Color.White
                )
            }
            
            Spacer(modifier = Modifier.width(16.dp))
            
            Column(modifier = Modifier.weight(1f)) {
                Text(
                        text = when {
                            child.parentLinked -> "Parent Account Linked"
                            isPasscodeActive -> "Linking Passcode Active"
                            else -> "No Parent Linked"
                        },
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                        color = when {
                            child.parentLinked -> Color(0xFF065F46)
                            isPasscodeActive -> Color(0xFF065F46)
                            else -> Color(0xFF92400E)
                        }
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                        text = when {
                            child.parentLinked -> "Your account is being monitored"
                            isPasscodeActive -> "Passcode expires in $timeRemaining"
                            else -> "Share your ID: ${child.childId}"
                    },
                    fontSize = 13.sp,
                        color = when {
                            child.parentLinked -> Color(0xFF047857)
                            isPasscodeActive -> Color(0xFF047857)
                            else -> Color(0xFF78350F)
                        }
                    )
                }
            }
            
            // Show passcode if active
            if (isPasscodeActive) {
                Spacer(modifier = Modifier.height(16.dp))
                
                HorizontalDivider(color = Color(0xFF10B981).copy(alpha = 0.3f))
                
                Spacer(modifier = Modifier.height(16.dp))
                
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Linking Passcode",
                        fontSize = 14.sp,
                        color = Color(0xFF065F46),
                        fontWeight = FontWeight.Medium
                    )
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    Text(
                        text = child.linkPasscode,
                        fontSize = 36.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF10B981),
                        letterSpacing = 4.sp
                    )
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    Text(
                        text = "⏰ Expires in $timeRemaining",
                        fontSize = 12.sp,
                        color = Color(0xFF047857),
                        fontWeight = FontWeight.Medium
                    )
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    Text(
                        text = "Share this code with your parent to link accounts",
                        fontSize = 11.sp,
                        color = Color(0xFF065F46).copy(alpha = 0.7f),
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    }
}


@Composable
fun RecentActivitySection(complaints: List<Complaint>) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(8.dp, RoundedCornerShape(20.dp)),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color.White
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Info,
                    contentDescription = "Activity",
                    tint = Color(0xFF8B5CF6),
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Recent Detections",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF1F2937)
                )
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            if (complaints.isEmpty()) {
                Text(
                    text = "No detections yet! Keep browsing safely 😊",
                    fontSize = 14.sp,
                    color = Color.Gray,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            } else {
                complaints.take(5).forEach { complaint ->
                    ComplaintItem(complaint)
                    Spacer(modifier = Modifier.height(12.dp))
                }
            }
        }
    }
}

@Composable
fun ComplaintItem(complaint: Complaint) {
    val dateFormat = SimpleDateFormat("MMM dd, hh:mm a", Locale.getDefault())
    val dateStr = dateFormat.format(Date(complaint.timestamp))
    
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (complaint.accessed) Color(0xFFF3F4F6) else Color(0xFFFEE2E2)
        ),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = if (complaint.accessed) Icons.Default.CheckCircle else Icons.Default.Warning,
                contentDescription = "Status",
                tint = if (complaint.accessed) Color(0xFF10B981) else Color(0xFFEF4444),
                modifier = Modifier.size(20.dp)
            )
            
            Spacer(modifier = Modifier.width(12.dp))
            
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = getAppDisplayName(LocalContext.current, complaint.appName),
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF1F2937)
                )
                Text(
                    text = dateStr,
                    fontSize = 12.sp,
                    color = Color.Gray
                )
            }
            
            Text(
                text = if (complaint.accessed) "Unlocked" else "Locked",
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                color = if (complaint.accessed) Color(0xFF10B981) else Color(0xFFEF4444)
            )
        }
    }
}

// Helper function to check if all permissions are granted
// Note: Screenshot permission is NOT checked here as we request it on-demand when needed
fun arePermissionsGranted(context: Context): Boolean {
    return try {
        // Check Device Admin permission
        val dpm = context.getSystemService(Context.DEVICE_POLICY_SERVICE) as DevicePolicyManager
        val componentName = ComponentName(context, MyDeviceAdminReceiver::class.java)
        val isDeviceAdmin = dpm.isAdminActive(componentName)
        
        // Check System Alert Window permission
        val canDrawOverlays = Settings.canDrawOverlays(context)
        
        // Check Accessibility Service
        val accessibilityEnabled = isAccessibilityServiceEnabled(context)
        
        // All 3 main permissions must be granted (screenshot is requested on-demand)
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

