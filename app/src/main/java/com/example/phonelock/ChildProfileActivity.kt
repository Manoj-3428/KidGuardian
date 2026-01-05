package com.example.phonelock

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.phonelock.repository.UserRepository
import kotlinx.coroutines.launch

class ChildProfileActivity : ComponentActivity() {
    private val viewModel: ChildProfileViewModel by viewModels()
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        val repository = UserRepository(this)
        viewModel.setRepository(repository)
        
        // Get child ID if passed from signup
        val childId = intent.getStringExtra("CHILD_ID")
        
        setContent {
            MaterialTheme {
                ChildProfileScreen(
                    viewModel = viewModel,
                    childId = childId,
                    onProfileComplete = {
                        startActivity(Intent(this, ChildDashboardActivity::class.java))
                        finish()
                    }
                )
            }
        }
    }
}

class ChildProfileViewModel : ViewModel() {
    private lateinit var repository: UserRepository
    
    private val _name = mutableStateOf("")
    val name: State<String> = _name
    
    private val _email = mutableStateOf("")
    val email: State<String> = _email
    
    private val _age = mutableStateOf("")
    val age: State<String> = _age
    
    private val _gender = mutableStateOf("Boy")
    val gender: State<String> = _gender
    
    private val _isLoading = mutableStateOf(true)
    val isLoading: State<Boolean> = _isLoading
    
    private val _errorMessage = mutableStateOf("")
    val errorMessage: State<String> = _errorMessage
    
    fun setRepository(repo: UserRepository) {
        repository = repo
        loadChildData()
    }
    
    private fun loadChildData() {
        viewModelScope.launch {
            val result = repository.getChildData()
            if (result.isSuccess) {
                val child = result.getOrNull()
                _name.value = child?.name ?: ""
                _email.value = child?.email ?: ""
                _age.value = if (child?.age != null && child.age > 0) child.age.toString() else ""
                _gender.value = child?.gender?.ifEmpty { "Boy" } ?: "Boy"
            }
            _isLoading.value = false
        }
    }
    
    fun updateAge(value: String) {
        if (value.isEmpty() || value.all { it.isDigit() }) {
            _age.value = value
            _errorMessage.value = ""
        }
    }
    
    fun updateGender(value: String) {
        _gender.value = value
    }
    
    fun saveProfile(onSuccess: () -> Unit, onError: (String) -> Unit) {
        if (age.value.isEmpty()) {
            _errorMessage.value = "Please enter your age"
            onError("Please enter your age")
            return
        }
        
        val ageInt = age.value.toIntOrNull()
        if (ageInt == null || ageInt < 1 || ageInt > 18) {
            _errorMessage.value = "Please enter a valid age (1-18)"
            onError("Please enter a valid age (1-18)")
            return
        }
        
        _isLoading.value = true
        _errorMessage.value = ""
        
        viewModelScope.launch {
            val result = repository.updateChildProfile(
                name = name.value.trim(), // Use existing name from Firestore
                age = ageInt,
                gender = gender.value
            )
            
            if (result.isSuccess) {
                _isLoading.value = false
                onSuccess()
            } else {
                _errorMessage.value = result.exceptionOrNull()?.message ?: "Failed to save profile"
                _isLoading.value = false
                onError(_errorMessage.value)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChildProfileScreen(
    viewModel: ChildProfileViewModel,
    childId: String?,
    onProfileComplete: () -> Unit
) {
    val context = LocalContext.current
    val genders = listOf("Boy", "Girl", "Other")
    val scrollState = rememberScrollState()
    
    // Loading state
    if (viewModel.isLoading.value) {
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
                ),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                CircularProgressIndicator(
                    modifier = Modifier.size(48.dp),
                    color = Color.White,
                    strokeWidth = 4.dp
                )
                Text(
                    text = "Loading your profile...",
                    color = Color.White,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        }
        return
    }
    
    // Animated gradient background
    val infiniteTransition = rememberInfiniteTransition(label = "background")
    val gradientOffset by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(8000, easing = LinearEasing),
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
                        Color(0xFF6366F1).copy(alpha = 0.9f + gradientOffset * 0.1f),
                        Color(0xFF8B5CF6),
                        Color(0xFFA855F7),
                        Color(0xFFC084FC)
                    )
                )
            )
    ) {
        // Animated floating orbs
        repeat(8) { index ->
            val size = (60 + index * 20).dp
            val delay = index * 400
            val floatY by infiniteTransition.animateFloat(
                initialValue = -50f,
                targetValue = 50f,
                animationSpec = infiniteRepeatable(
                    animation = tween(4000 + delay, easing = EaseInOutCubic),
                    repeatMode = RepeatMode.Reverse
                ),
                label = "floatY$index"
            )
            val floatX by infiniteTransition.animateFloat(
                initialValue = -30f,
                targetValue = 30f,
                animationSpec = infiniteRepeatable(
                    animation = tween(5000 + delay, easing = EaseInOutCubic),
                    repeatMode = RepeatMode.Reverse
                ),
                label = "floatX$index"
            )
            
            Box(
                modifier = Modifier
                    .size(size)
                    .offset(
                        x = (50 + index * 120).dp + floatX.dp,
                        y = (80 + index * 150).dp + floatY.dp
                    )
                    .background(
                        color = Color.White.copy(alpha = 0.08f),
                        shape = RoundedCornerShape(50)
                    )
            )
        }
        
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
        ) {
            Spacer(modifier = Modifier.height(40.dp))
            
            // Header Section
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Profile Icon with animated glow
                val iconScale by infiniteTransition.animateFloat(
                    initialValue = 1f,
                    targetValue = 1.05f,
                    animationSpec = infiniteRepeatable(
                        animation = tween(2000, easing = EaseInOutCubic),
                        repeatMode = RepeatMode.Reverse
                    ),
                    label = "iconScale"
                )
                
                Box(
                    modifier = Modifier
                        .size(120.dp)
                        .graphicsLayer {
                            scaleX = iconScale
                            scaleY = iconScale
                        }
                        .shadow(
                            elevation = 20.dp,
                            shape = RoundedCornerShape(30.dp),
                            spotColor = Color.White.copy(alpha = 0.3f)
                        )
                        .clip(RoundedCornerShape(30.dp))
                        .background(
                            Brush.linearGradient(
                                colors = listOf(
                                    Color.White.copy(alpha = 0.25f),
                                    Color.White.copy(alpha = 0.15f)
                                )
                            )
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.AccountCircle,
                        contentDescription = "Profile",
                        modifier = Modifier.size(70.dp),
                        tint = Color.White
                    )
                }
                
                Spacer(modifier = Modifier.height(24.dp))
                
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "Complete Your",
                        fontSize = 32.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        textAlign = TextAlign.Center,
                        letterSpacing = 0.5.sp
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Profile",
                        fontSize = 32.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        textAlign = TextAlign.Center,
                        letterSpacing = 0.5.sp
                    )
                }
                
                Spacer(modifier = Modifier.height(8.dp))
                
                Text(
                    text = "Let's personalize your experience",
                    fontSize = 16.sp,
                    color = Color.White.copy(alpha = 0.9f),
                    textAlign = TextAlign.Center
                )
            }
            
            Spacer(modifier = Modifier.height(32.dp))
            
            // Main Content Card
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp)
                    .shadow(
                        elevation = 16.dp,
                        shape = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp),
                        spotColor = Color.Black.copy(alpha = 0.2f)
                    ),
                shape = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(28.dp),
                    verticalArrangement = Arrangement.spacedBy(24.dp)
                ) {
                    // Child ID Card (if available)
                    if (!childId.isNullOrEmpty()) {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(
                                containerColor = Color(0xFFF0F4FF)
                            ),
                            shape = RoundedCornerShape(20.dp)
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(20.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Info,
                                        contentDescription = "Info",
                                        tint = Color(0xFF6366F1),
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Text(
                                        text = "Your Child ID",
                                        fontSize = 13.sp,
                                        color = Color(0xFF6366F1),
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                                
                                Spacer(modifier = Modifier.height(12.dp))
                                
                                Text(
                                    text = childId,
                                    fontSize = 28.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF1F2937),
                                    letterSpacing = 3.sp,
                                    fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
                                )
                                
                                Spacer(modifier = Modifier.height(8.dp))
                                
                                Text(
                                    text = "Share this ID with your parent to link accounts",
                                    fontSize = 12.sp,
                                    color = Color(0xFF6B7280),
                                    textAlign = TextAlign.Center,
                                    lineHeight = 16.sp
                                )
                            }
                        }
                    }
                    
                    // Account Info Section (Name & Email)
                    if (viewModel.name.value.isNotEmpty()) {
                        Column(
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Text(
                                text = "Account Information",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF1F2937)
                            )
                            
                            // Name Card
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(
                                    containerColor = Color(0xFFF9FAFB)
                                ),
                                shape = RoundedCornerShape(16.dp)
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(16.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(40.dp)
                                            .background(
                                                Color(0xFF6366F1).copy(alpha = 0.1f),
                                                RoundedCornerShape(10.dp)
                                            ),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Person,
                                            contentDescription = "Name",
                                            tint = Color(0xFF6366F1),
                                            modifier = Modifier.size(22.dp)
                                        )
                                    }
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = "Name",
                                            fontSize = 12.sp,
                                            color = Color(0xFF6B7280),
                                            fontWeight = FontWeight.Medium
                                        )
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(
                                            text = viewModel.name.value,
                                            fontSize = 16.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color(0xFF1F2937)
                                        )
                                    }
                                }
                            }
                            
                            // Email Card
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(
                                    containerColor = Color(0xFFF9FAFB)
                                ),
                                shape = RoundedCornerShape(16.dp)
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(16.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(40.dp)
                                            .background(
                                                Color(0xFF6366F1).copy(alpha = 0.1f),
                                                RoundedCornerShape(10.dp)
                                            ),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Email,
                                            contentDescription = "Email",
                                            tint = Color(0xFF6366F1),
                                            modifier = Modifier.size(22.dp)
                                        )
                                    }
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = "Email",
                                            fontSize = 12.sp,
                                            color = Color(0xFF6B7280),
                                            fontWeight = FontWeight.Medium
                                        )
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(
                                            text = viewModel.email.value,
                                            fontSize = 14.sp,
                                            fontWeight = FontWeight.Medium,
                                            color = Color(0xFF1F2937)
                                        )
                                    }
                                }
                            }
                        }
                    }
                    
                    // Age Input Section
                    Column(
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "Your Age",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color(0xFF1F2937)
                        )
                        
                        OutlinedTextField(
                            value = viewModel.age.value,
                            onValueChange = { viewModel.updateAge(it) },
                            label = { Text("Enter your age") },
                            placeholder = { Text("e.g., 12") },
                            modifier = Modifier.fillMaxWidth(),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = Color(0xFF6366F1),
                                unfocusedBorderColor = Color(0xFFE5E7EB),
                                focusedLabelColor = Color(0xFF6366F1),
                                unfocusedLabelColor = Color(0xFF9CA3AF),
                                cursorColor = Color(0xFF6366F1),
                                focusedContainerColor = Color.White,
                                unfocusedContainerColor = Color(0xFFF9FAFB)
                            ),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            shape = RoundedCornerShape(16.dp),
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Default.DateRange,
                                    contentDescription = "Age",
                                    tint = Color(0xFF6366F1),
                                    modifier = Modifier.size(24.dp)
                                )
                            },
                            singleLine = true,
                            supportingText = {
                                Text(
                                    text = "Must be between 1 and 18 years",
                                    fontSize = 12.sp,
                                    color = Color(0xFF6B7280)
                                )
                            }
                        )
                    }
                    
                    // Gender Selection Section
                    Column(
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            text = "Gender",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color(0xFF1F2937)
                        )
                        
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            genders.forEach { gender ->
                                val isSelected = viewModel.gender.value == gender
                                val scale by animateFloatAsState(
                                    targetValue = if (isSelected) 1.02f else 1f,
                                    animationSpec = spring(
                                        dampingRatio = Spring.DampingRatioMediumBouncy,
                                        stiffness = Spring.StiffnessMedium
                                    ),
                                    label = "genderScale"
                                )
                                
                                val borderColor by animateColorAsState(
                                    targetValue = if (isSelected) Color(0xFF6366F1) else Color(0xFFE5E7EB),
                                    label = "borderColor"
                                )
                                
                                Card(
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(100.dp)
                                        .graphicsLayer {
                                            scaleX = scale
                                            scaleY = scale
                                        }
                                        .clickable {
                                            viewModel.updateGender(gender)
                                        }
                                        .shadow(
                                            elevation = if (isSelected) 8.dp else 2.dp,
                                            shape = RoundedCornerShape(20.dp),
                                            spotColor = if (isSelected) Color(0xFF6366F1).copy(alpha = 0.3f) else Color.Transparent
                                        ),
                                    shape = RoundedCornerShape(20.dp),
                                    colors = CardDefaults.cardColors(
                                        containerColor = if (isSelected) {
                                            Color(0xFF6366F1)
                                        } else {
                                            Color(0xFFF9FAFB)
                                        }
                                    ),
                                    border = androidx.compose.foundation.BorderStroke(
                                        width = if (isSelected) 2.dp else 1.dp,
                                        color = borderColor
                                    )
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .padding(12.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Column(
                                            horizontalAlignment = Alignment.CenterHorizontally,
                                            verticalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            Icon(
                                                imageVector = when (gender) {
                                                    "Boy" -> Icons.Default.Person
                                                    "Girl" -> Icons.Default.Person
                                                    else -> Icons.Default.Person
                                                },
                                                contentDescription = gender,
                                                modifier = Modifier.size(32.dp),
                                                tint = if (isSelected) Color.White else Color(0xFF6366F1)
                                            )
                                            Text(
                                                text = gender,
                                                fontSize = 15.sp,
                                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                                color = if (isSelected) Color.White else Color(0xFF1F2937)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                    
                    // Error Message
                    if (viewModel.errorMessage.value.isNotEmpty()) {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(
                                containerColor = Color(0xFFFEF2F2)
                            ),
                            shape = RoundedCornerShape(16.dp),
                            border = androidx.compose.foundation.BorderStroke(
                                width = 1.dp,
                                color = Color(0xFFFECACA)
                            )
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Clear,
                                    contentDescription = "Error",
                                    tint = Color(0xFFDC2626),
                                    modifier = Modifier.size(24.dp)
                                )
                                Text(
                                    text = viewModel.errorMessage.value,
                                    color = Color(0xFFDC2626),
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Medium,
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    // Save Button
                    Button(
                        onClick = {
                            viewModel.saveProfile(
                                onSuccess = {
                                    Toast.makeText(context, "Profile saved successfully!", Toast.LENGTH_SHORT).show()
                                    onProfileComplete()
                                },
                                onError = { error ->
                                    Toast.makeText(context, error, Toast.LENGTH_LONG).show()
                                }
                            )
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(60.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF6366F1)
                        ),
                        shape = RoundedCornerShape(16.dp),
                        enabled = !viewModel.isLoading.value,
                        elevation = ButtonDefaults.buttonElevation(
                            defaultElevation = 8.dp,
                            pressedElevation = 4.dp
                        )
                    ) {
                        if (viewModel.isLoading.value) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(28.dp),
                                color = Color.White,
                                strokeWidth = 3.dp
                            )
                        } else {
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Complete Profile",
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Icon(
                                    imageVector = Icons.Default.ArrowForward,
                                    contentDescription = "Save",
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(8.dp))
                }
            }
            
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

