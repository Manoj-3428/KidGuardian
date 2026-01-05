package com.example.phonelock

import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.Spring
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.phonelock.repository.UserRepository
import kotlinx.coroutines.launch

class ParentLoginActivity : ComponentActivity() {
    
    private lateinit var repository: UserRepository
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        repository = UserRepository(this)
        
        setContent {
            MaterialTheme {
            ParentLoginScreen(
                repository = repository,
                onLoginSuccess = {
                    // Navigate to parent dashboard
                    startActivity(Intent(this, ParentDashboardActivity::class.java))
                    finish()
                },
                onBackPressed = {
                    // Go back to role selection instead of exiting app
                    startActivity(Intent(this, RoleSelectionActivity::class.java))
                    finish()
                }
            )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ParentLoginScreen(
    repository: UserRepository,
    onLoginSuccess: () -> Unit,
    onBackPressed: () -> Unit
) {
    var childId by remember { mutableStateOf("") }
    var passcode by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf("") }
    var successMessage by remember { mutableStateOf("") }
    var showPasscodeEntry by remember { mutableStateOf(false) }
    var showRoleSelection by remember { mutableStateOf(false) }
    var selectedRole by remember { mutableStateOf("") }
    var parentName by remember { mutableStateOf("") }
    val scope = rememberCoroutineScope()
    
    // Animated gradient background
    val infiniteTransition = rememberInfiniteTransition(label = "background")
    val gradientOffset by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(10000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "gradient"
    )
    
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF6366F1),
                        Color(0xFF8B5CF6),
                        Color(0xFFA855F7)
                    )
                )
            )
    ) {
        // Animated floating orbs
        repeat(8) { index ->
            val size = (50 + index * 20).dp
            val delay = index * 300
            val floatY by infiniteTransition.animateFloat(
                initialValue = -30f,
                targetValue = 30f,
                animationSpec = infiniteRepeatable(
                    animation = tween(4000 + delay, easing = EaseInOutCubic),
                    repeatMode = RepeatMode.Reverse
                ),
                label = "floatY$index"
            )
            
            Box(
                modifier = Modifier
                    .size(size)
                    .offset(
                        x = (40 + index * 90).dp,
                        y = (80 + index * 130).dp + floatY.dp
                    )
                    .background(
                        color = Color.White.copy(alpha = 0.06f),
                        shape = RoundedCornerShape(50)
                    )
            )
        }
        
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
        ) {
            Spacer(modifier = Modifier.height(16.dp))
            
            // Header with Back Button
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = onBackPressed,
                    modifier = Modifier
                        .size(44.dp)
                        .shadow(
                            elevation = 3.dp,
                            shape = RoundedCornerShape(12.dp)
                        )
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color.White.copy(alpha = 0.2f))
                ) {
                    Icon(
                        Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = Color.White,
                        modifier = Modifier.size(22.dp)
                    )
                }
            }
            
            // Spacer to center card vertically
            Spacer(modifier = Modifier.weight(1f))
            
            // Main Content Card - Centered vertically
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp)
                    .shadow(
                        elevation = 12.dp,
                        shape = RoundedCornerShape(20.dp),
                        spotColor = Color.Black.copy(alpha = 0.15f)
                    ),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(18.dp)
                ) {
                    // Title Section
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "Parent Login",
                            fontSize = 26.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF1F2937),
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Enter child ID to request login code",
                            fontSize = 13.sp,
                            color = Color(0xFF6B7280),
                            textAlign = TextAlign.Center
                        )
                    }
                    
                    if (!showPasscodeEntry && !showRoleSelection) {
                        // Child ID Input Section
                        Column(
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            OutlinedTextField(
                                value = childId,
                                onValueChange = { 
                                    childId = it
                                    errorMessage = ""
                                },
                                label = { Text("Child ID") },
                                placeholder = { Text("Enter child's unique ID") },
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true,
                                enabled = !isLoading,
                                shape = RoundedCornerShape(16.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = Color(0xFF6366F1),
                                    unfocusedBorderColor = Color(0xFFE5E7EB),
                                    focusedLabelColor = Color(0xFF6366F1),
                                    unfocusedLabelColor = Color(0xFF9CA3AF),
                                    cursorColor = Color(0xFF6366F1),
                                    focusedContainerColor = Color.White,
                                    unfocusedContainerColor = Color(0xFFF9FAFB)
                                ),
                                leadingIcon = {
                                    Icon(
                                        Icons.Default.Person,
                                        contentDescription = "Child ID",
                                        tint = Color(0xFF6366F1),
                                        modifier = Modifier.size(24.dp)
                                    )
                                }
                            )
                            
                            // Error Message
                            if (errorMessage.isNotEmpty()) {
                                Card(
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = CardDefaults.cardColors(
                                        containerColor = Color(0xFFFEF2F2)
                                    ),
                                    shape = RoundedCornerShape(12.dp),
                                    border = androidx.compose.foundation.BorderStroke(
                                        width = 1.dp,
                                        color = Color(0xFFFECACA)
                                    )
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(12.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Icon(
                                            Icons.Default.Close,
                                            contentDescription = "Error",
                                            tint = Color(0xFFDC2626),
                                            modifier = Modifier.size(20.dp)
                                        )
                                        Text(
                                            text = errorMessage,
                                            color = Color(0xFFDC2626),
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.Medium,
                                            modifier = Modifier.weight(1f)
                                        )
                                    }
                                }
                            }
                            
                            // Success Message
                            if (successMessage.isNotEmpty()) {
                                Card(
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = CardDefaults.cardColors(
                                        containerColor = Color(0xFFF0FDF4)
                                    ),
                                    shape = RoundedCornerShape(12.dp),
                                    border = androidx.compose.foundation.BorderStroke(
                                        width = 1.dp,
                                        color = Color(0xFFBBF7D0)
                                    )
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(12.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Icon(
                                            Icons.Default.CheckCircle,
                                            contentDescription = "Success",
                                            tint = Color(0xFF10B981),
                                            modifier = Modifier.size(20.dp)
                                        )
                                        Text(
                                            text = successMessage,
                                            color = Color(0xFF059669),
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.Medium,
                                            modifier = Modifier.weight(1f)
                                        )
                                    }
                                }
                            }
                        }
                        
                        Spacer(modifier = Modifier.height(16.dp))
                        
                        // Request Code Button
                        Button(
                            onClick = {
                                if (childId.isBlank()) {
                                    errorMessage = "Please enter Child ID"
                                    return@Button
                                }
                                
                                // Request login code
                                isLoading = true
                                scope.launch {
                                    requestLoginCode(
                                        childId = childId,
                                        repository = repository,
                                        onSuccess = {
                                            isLoading = false
                                            successMessage = "✅ Login code sent to child dashboard (expires in 5 min)"
                                            showPasscodeEntry = true
                                        },
                                        onError = { error ->
                                            isLoading = false
                                            errorMessage = error
                                        }
                                    )
                                }
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(60.dp),
                            shape = RoundedCornerShape(16.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFF6366F1)
                            ),
                            enabled = !isLoading,
                            elevation = ButtonDefaults.buttonElevation(
                                defaultElevation = 8.dp,
                                pressedElevation = 4.dp
                            )
                        ) {
                            if (isLoading) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(28.dp),
                                    color = Color.White,
                                    strokeWidth = 3.dp
                                )
                                Spacer(Modifier.width(12.dp))
                                Text(
                                    "Sending...",
                                    fontSize = 17.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            } else {
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        "Request Login Code",
                                        fontSize = 17.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Icon(
                                        Icons.Default.Send,
                                        contentDescription = "Send",
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                        }
                    } else if (showPasscodeEntry && !showRoleSelection) {
                        // Passcode Entry Screen - Clean Design with Icons
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(20.dp)
                        ) {
                            // Icon with Title
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(56.dp)
                                        .shadow(
                                            elevation = 8.dp,
                                            shape = RoundedCornerShape(16.dp),
                                            spotColor = Color(0xFF10B981).copy(alpha = 0.3f)
                                        )
                                        .clip(RoundedCornerShape(16.dp))
                                        .background(
                                            Brush.linearGradient(
                                                colors = listOf(
                                                    Color(0xFF10B981),
                                                    Color(0xFF059669)
                                                )
                                            )
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        painter = painterResource(id = R.drawable.ic_key),
                                        contentDescription = "Passcode",
                                        modifier = Modifier.size(28.dp),
                                        tint = Color.White
                                    )
                                }
                                Column {
                                    Text(
                                        text = "Parent Login",
                                        fontSize = 24.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF1F2937)
                                    )
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Icon(
                                            Icons.Default.DateRange,
                                            contentDescription = "Timer",
                                            tint = Color(0xFFEF4444),
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Text(
                                            text = "Expires in 5 min",
                                            fontSize = 12.sp,
                                            color = Color(0xFF6B7280),
                                            fontWeight = FontWeight.Medium
                                        )
                                    }
                                }
                            }
                        }
                        
                        Spacer(modifier = Modifier.height(8.dp))
                        
                        // Passcode Field
                        OutlinedTextField(
                            value = passcode,
                            onValueChange = { 
                                if (it.length <= 6 && it.all { char -> char.isDigit() }) {
                                    passcode = it
                                    errorMessage = ""
                                }
                            },
                            label = { Text("6-digit code") },
                            placeholder = { Text("000000") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            enabled = !isLoading,
                            shape = RoundedCornerShape(16.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = Color(0xFF10B981),
                                unfocusedBorderColor = Color(0xFFE5E7EB),
                                focusedLabelColor = Color(0xFF10B981),
                                unfocusedLabelColor = Color(0xFF9CA3AF),
                                cursorColor = Color(0xFF10B981),
                                focusedContainerColor = Color.White,
                                unfocusedContainerColor = Color(0xFFF9FAFB)
                            ),
                            leadingIcon = {
                                Icon(
                                    Icons.Default.Lock,
                                    contentDescription = "Passcode",
                                    tint = Color(0xFF10B981),
                                    modifier = Modifier.size(24.dp)
                                )
                            },
                            trailingIcon = {
                                if (passcode.length == 6) {
                                    Icon(
                                        Icons.Default.CheckCircle,
                                        contentDescription = "Valid",
                                        tint = Color(0xFF10B981),
                                        modifier = Modifier.size(24.dp)
                                    )
                                }
                            },
                            textStyle = androidx.compose.ui.text.TextStyle(
                                fontSize = 24.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 6.sp
                            )
                        )
                        
                        // Error Message
                        if (errorMessage.isNotEmpty()) {
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(
                                    containerColor = Color(0xFFFEF2F2)
                                ),
                                shape = RoundedCornerShape(12.dp),
                                border = BorderStroke(
                                    width = 1.dp,
                                    color = Color(0xFFFECACA)
                                )
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Icon(
                                        Icons.Default.Close,
                                        contentDescription = "Error",
                                        tint = Color(0xFFDC2626),
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Text(
                                        text = errorMessage,
                                        color = Color(0xFFDC2626),
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Medium,
                                        modifier = Modifier.weight(1f)
                                    )
                                }
                            }
                        }
                        
                        // Verify Passcode Button
                        Button(
                            onClick = {
                                if (passcode.isBlank()) {
                                    errorMessage = "Please enter passcode"
                                    return@Button
                                }
                                if (passcode.length != 6) {
                                    errorMessage = "Passcode must be exactly 6 digits"
                                    return@Button
                                }
                                
                                // Verify passcode
                                isLoading = true
                                scope.launch {
                                    verifyPasscode(
                                        childId = childId,
                                        passcode = passcode,
                                        repository = repository,
                                        onSuccess = {
                                            isLoading = false
                                            successMessage = "✅ Passcode verified! Select your role."
                                            showRoleSelection = true
                                        },
                                        onError = { error ->
                                            isLoading = false
                                            errorMessage = error
                                        }
                                    )
                                }
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(60.dp),
                            shape = RoundedCornerShape(16.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981)),
                            enabled = passcode.length == 6 && !isLoading,
                            elevation = ButtonDefaults.buttonElevation(
                                defaultElevation = 8.dp,
                                pressedElevation = 4.dp
                            )
                        ) {
                            if (isLoading) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(28.dp),
                                    color = Color.White,
                                    strokeWidth = 3.dp
                                )
                                Spacer(Modifier.width(12.dp))
                                Text("Verifying...", fontSize = 17.sp, fontWeight = FontWeight.Bold)
                            } else {
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        painter = painterResource(id = R.drawable.ic_key),
                                        contentDescription = null,
                                        modifier = Modifier.size(22.dp)
                                    )
                                    Text(
                                        "Verify Passcode",
                                        fontSize = 17.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                        
                        Spacer(modifier = Modifier.height(8.dp))
                        
                        // Back to Child ID
                        OutlinedButton(
                            onClick = {
                                showPasscodeEntry = false
                                successMessage = ""
                                errorMessage = ""
                                passcode = ""
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(54.dp),
                            shape = RoundedCornerShape(16.dp),
                            colors = ButtonDefaults.outlinedButtonColors(
                                contentColor = Color(0xFF6366F1)
                            ),
                            border = BorderStroke(
                                width = 1.5.dp,
                                color = Color(0xFF6366F1)
                            )
                        ) {
                            Icon(
                                Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = null,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(
                                "Back to Child ID",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    } else {
                        // Role Selection Screen - Clean Design with Icons
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(56.dp)
                                    .shadow(
                                        elevation = 8.dp,
                                        shape = RoundedCornerShape(16.dp),
                                        spotColor = Color(0xFF6366F1).copy(alpha = 0.3f)
                                    )
                                    .clip(RoundedCornerShape(16.dp))
                                    .background(
                                        Brush.linearGradient(
                                            colors = listOf(
                                                Color(0xFF6366F1),
                                                Color(0xFF8B5CF6)
                                            )
                                        )
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    Icons.Default.Person,
                                    contentDescription = "Role",
                                    modifier = Modifier.size(28.dp),
                                    tint = Color.White
                                )
                            }
                            Column {
                                Text(
                                    text = "Select Your Role",
                                    fontSize = 24.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF1F2937)
                                )
                                Text(
                                    text = "Choose relationship",
                                    fontSize = 13.sp,
                                    color = Color(0xFF6B7280),
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                        
                        Spacer(modifier = Modifier.height(20.dp))
                        
                        // Role Options
                        val roles = listOf("Mother", "Father", "Guardian", "Other")
                        
                        Column(
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            roles.forEach { role ->
                                val isSelected = selectedRole == role
                                val scale by animateFloatAsState(
                                    targetValue = if (isSelected) 1.02f else 1f,
                                    animationSpec = spring(
                                        dampingRatio = Spring.DampingRatioMediumBouncy,
                                        stiffness = Spring.StiffnessMedium
                                    ),
                                    label = "roleScale"
                                )
                                
                                val borderColor by animateColorAsState(
                                    targetValue = if (isSelected) Color(0xFF6366F1) else Color(0xFFE5E7EB),
                                    label = "borderColor"
                                )
                                
                                Card(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .graphicsLayer {
                                            scaleX = scale
                                            scaleY = scale
                                        }
                                        .clickable {
                                            selectedRole = role
                                            // Auto-fetch name if this is an existing role
                                            scope.launch {
                                                autoFetchParentName(childId, role, repository) { name ->
                                                    parentName = name
                                                }
                                            }
                                        }
                                        .shadow(
                                            elevation = if (isSelected) 8.dp else 2.dp,
                                            shape = RoundedCornerShape(18.dp),
                                            spotColor = if (isSelected) Color(0xFF6366F1).copy(alpha = 0.3f) else Color.Transparent
                                        ),
                                    shape = RoundedCornerShape(18.dp),
                                    colors = CardDefaults.cardColors(
                                        containerColor = if (isSelected) 
                                            Color(0xFF6366F1).copy(alpha = 0.1f)
                                        else Color(0xFFF9FAFB)
                                    ),
                                    border = BorderStroke(
                                        width = if (isSelected) 2.dp else 1.dp,
                                        color = borderColor
                                    )
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(20.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                                    ) {
                                        if (isSelected) {
                                            Box(
                                                modifier = Modifier
                                                    .size(28.dp)
                                                    .clip(RoundedCornerShape(14.dp))
                                                    .background(Color(0xFF6366F1)),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Icon(
                                                    Icons.Default.Check,
                                                    contentDescription = "Selected",
                                                    tint = Color.White,
                                                    modifier = Modifier.size(18.dp)
                                                )
                                            }
                                        } else {
                                            Box(
                                                modifier = Modifier
                                                    .size(28.dp)
                                                    .clip(RoundedCornerShape(14.dp))
                                                    .background(Color(0xFFE5E7EB))
                                            )
                                        }
                                        Text(
                                            text = role,
                                            fontSize = 17.sp,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                            color = if (isSelected) 
                                                Color(0xFF6366F1) 
                                            else Color(0xFF1F2937)
                                        )
                                    }
                                }
                            }
                        }
                        
                        Spacer(modifier = Modifier.height(12.dp))
                        
                        // Parent Name Field
                        OutlinedTextField(
                            value = parentName,
                            onValueChange = { parentName = it },
                            label = { Text("Your Name") },
                            placeholder = { Text("Enter your name") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            enabled = !isLoading,
                            shape = RoundedCornerShape(16.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = Color(0xFF6366F1),
                                unfocusedBorderColor = Color(0xFFE5E7EB),
                                focusedLabelColor = Color(0xFF6366F1),
                                unfocusedLabelColor = Color(0xFF9CA3AF),
                                cursorColor = Color(0xFF6366F1),
                                focusedContainerColor = Color.White,
                                unfocusedContainerColor = Color(0xFFF9FAFB)
                            ),
                            leadingIcon = {
                                Icon(
                                    Icons.Default.Person,
                                    contentDescription = "Name",
                                    tint = Color(0xFF6366F1),
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                        )
                        
                        // Error Message
                        if (errorMessage.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(
                                    containerColor = Color(0xFFFEF2F2)
                                ),
                                shape = RoundedCornerShape(12.dp),
                                border = BorderStroke(
                                    width = 1.dp,
                                    color = Color(0xFFFECACA)
                                )
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Icon(
                                        Icons.Default.Close,
                                        contentDescription = "Error",
                                        tint = Color(0xFFDC2626),
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Text(
                                        text = errorMessage,
                                        color = Color(0xFFDC2626),
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Medium,
                                        modifier = Modifier.weight(1f)
                                    )
                                }
                            }
                        }
                        
                        // Login Button
                        Button(
                            onClick = {
                                if (selectedRole.isBlank()) {
                                    errorMessage = "Please select your role"
                                    return@Button
                                }
                                if (parentName.isBlank()) {
                                    errorMessage = "Please enter your name"
                                    return@Button
                                }
                                
                                // Complete login
                                isLoading = true
                                scope.launch {
                                    completeLogin(
                                        childId = childId,
                                        role = selectedRole,
                                        name = parentName,
                                        repository = repository,
                                        onSuccess = {
                                            isLoading = false
                                            onLoginSuccess()
                                        },
                                        onError = { error ->
                                            isLoading = false
                                            errorMessage = error
                                        }
                                    )
                                }
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(60.dp),
                            shape = RoundedCornerShape(16.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981)),
                            enabled = selectedRole.isNotEmpty() && parentName.isNotEmpty() && !isLoading,
                            elevation = ButtonDefaults.buttonElevation(
                                defaultElevation = 8.dp,
                                pressedElevation = 4.dp
                            )
                        ) {
                            if (isLoading) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(28.dp),
                                    color = Color.White,
                                    strokeWidth = 3.dp
                                )
                                Spacer(Modifier.width(12.dp))
                                Text(
                                    "Logging in...",
                                    fontSize = 17.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            } else {
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        "Complete Login",
                                        fontSize = 17.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Icon(
                                        Icons.Default.ArrowForward,
                                        contentDescription = "Login",
                                        modifier = Modifier.size(22.dp)
                                    )
                                }
                            }
                        }
                        
                        Spacer(modifier = Modifier.height(8.dp))
                        
                        // Back to Code Request
                        TextButton(
                            onClick = {
                                showRoleSelection = false
                                successMessage = ""
                                errorMessage = ""
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(
                                Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Back",
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(
                                "Back to Code Request",
                                color = Color(0xFF6366F1),
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }
            }
            
            // Spacer to center card vertically
            Spacer(modifier = Modifier.weight(1f))
        }
    }
}

private suspend fun requestLoginCode(
    childId: String,
    repository: UserRepository,
    onSuccess: () -> Unit,
    onError: (String) -> Unit
) {
    try {
        // Generate and save login code for the child
        val result = repository.requestLinkPasscode(childId)
        
        if (result.isSuccess) {
            Log.d("LOGIN_DEBUG", "✅ Login code sent to child dashboard")
            onSuccess()
        } else {
            onError(result.exceptionOrNull()?.message ?: "Failed to send login code")
        }
    } catch (e: Exception) {
        Log.e("LOGIN_DEBUG", "Error requesting login code", e)
        onError("Failed to send login code: ${e.message}")
    }
}

private suspend fun autoFetchParentName(
    _childId: String,
    _role: String,
    _repository: UserRepository,
    onNameFetched: (String) -> Unit
) {
    try {
        // Try to find existing parent with this role for this child
        // For now, we'll leave name empty and let user enter it
        // In future, we can implement logic to fetch existing parent names
        onNameFetched("")
    } catch (e: Exception) {
        Log.e("LOGIN_DEBUG", "Error fetching parent name", e)
        onNameFetched("")
    }
}

private suspend fun verifyPasscode(
    childId: String,
    passcode: String,
    repository: UserRepository,
    onSuccess: () -> Unit,
    onError: (String) -> Unit
) {
    try {
        // Get child data to verify passcode
        val childResult = repository.getUserById(childId)
        if (childResult.isFailure) {
            onError("Child ID not found")
            return
        }
        
        val child = childResult.getOrNull()
        if (child == null) {
            onError("Child account not found")
            return
        }
        
        // Check if passcode exists and matches
        if (child.linkPasscode == null) {
            onError("No passcode found. Please request a new code.")
            return
        }
        
        if (child.linkPasscode != passcode) {
            onError("Invalid passcode. Please check and try again.")
            return
        }
        
        if (repository.isPasscodeExpired(child.passcodeGeneratedAt)) {
            onError("Passcode expired. Please request a new code.")
            return
        }
        
        Log.d("LOGIN_DEBUG", "✅ Passcode verified successfully")
        onSuccess()
    } catch (e: Exception) {
        Log.e("LOGIN_DEBUG", "Error verifying passcode", e)
        onError("Passcode verification failed: ${e.message}")
    }
}

private suspend fun completeLogin(
    childId: String,
    role: String,
    name: String,
    repository: UserRepository,
    onSuccess: () -> Unit,
    onError: (String) -> Unit
) {
    try {
        // Create parent account
        val parentId = "parent_${childId}_${System.currentTimeMillis()}"
        val parent = com.example.phonelock.models.Parent(
            parentId = parentId,
            name = name,
            relationship = role,
            linkedChildId = childId,
            monitoringEnabled = true,
            createdAt = System.currentTimeMillis()
        )
        
        // Save parent to Firestore using repository method
        val saveResult = repository.saveParentToFirestore(parent)
        if (saveResult.isFailure) {
            onError("Failed to save parent account: ${saveResult.exceptionOrNull()?.message}")
            return
        }
        
        // Link parent to child
        val linkResult = repository.linkParentToChild(parentId, childId)
        if (linkResult.isSuccess) {
            // Clear the passcode after successful login
            repository.clearLinkPasscode(childId)
            
            // Store parent ID for session
            repository.saveParentId(parentId)
            
            // Save parent role for session management
            repository.saveUserRole(com.example.phonelock.models.UserRole.PARENT)
            
            Log.d("LOGIN_DEBUG", "✅ Parent login successful - ID: $parentId, passcode cleared")
            onSuccess()
        } else {
            onError("Failed to link parent account: ${linkResult.exceptionOrNull()?.message}")
        }
    } catch (e: Exception) {
        Log.e("LOGIN_DEBUG", "Error during login", e)
        onError("Login failed: ${e.message}")
    }
}