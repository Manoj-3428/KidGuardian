package com.example.phonelock

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.UserProfileChangeRequest

class FlipAuthActivity : ComponentActivity() {
    private val viewModel: FlipAuthViewModel by viewModels()
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        setContent {
            MaterialTheme {
                FlipAuthScreen(
                    viewModel = viewModel,
                    onNavigateToLockScreen = {
                        val intent = Intent(this, LockScreenActivity::class.java)
                        startActivity(intent)
                        finish()
                    }
                )
            }
        }
    }
}

class FlipAuthViewModel : ViewModel() {
    private val auth = FirebaseAuth.getInstance()
    
    private val _isLoginMode = mutableStateOf(true)
    val isLoginMode: State<Boolean> = _isLoginMode
    
    private val _email = mutableStateOf("")
    val email: State<String> = _email
    
    private val _password = mutableStateOf("")
    val password: State<String> = _password
    
    private val _name = mutableStateOf("")
    val name: State<String> = _name
    
    private val _isLoading = mutableStateOf(false)
    val isLoading: State<Boolean> = _isLoading
    
    private val _isParentMode = mutableStateOf(false)
    val isParentMode: State<Boolean> = _isParentMode
    
    private val _errorMessage = mutableStateOf("")
    val errorMessage: State<String> = _errorMessage
    
    private val _currentUser = mutableStateOf<FirebaseUser?>(null)
    val currentUser: State<FirebaseUser?> = _currentUser
    
    fun toggleMode() {
        _isLoginMode.value = !_isLoginMode.value
        _errorMessage.value = "" // Clear error when switching modes
    }
    
    fun toggleParentMode() {
        _isParentMode.value = !_isParentMode.value
    }
    
    fun updateEmail(value: String) {
        _email.value = value
        _errorMessage.value = "" // Clear error when user types
    }
    
    fun updatePassword(value: String) {
        _password.value = value
        _errorMessage.value = "" // Clear error when user types
    }
    
    fun updateName(value: String) {
        _name.value = value
        _errorMessage.value = "" // Clear error when user types
    }
    
    fun login(onSuccess: () -> Unit, onError: (String) -> Unit) {
        if (email.value.isEmpty() || password.value.isEmpty()) {
            _errorMessage.value = "Please fill all fields"
            onError("Please fill all fields")
            return
        }
        
        _isLoading.value = true
        _errorMessage.value = ""
        
        viewModelScope.launch {
            try {
                val result = auth.signInWithEmailAndPassword(
                    email.value.trim(),
                    password.value
                ).await()
                
                result.user?.let { user ->
                    _currentUser.value = user
                    _isLoading.value = false
                    onSuccess()
                } ?: run {
                    _errorMessage.value = "Login failed"
                    _isLoading.value = false
                    onError("Login failed")
                }
            } catch (e: Exception) {
                _errorMessage.value = when {
                    e.message?.contains("password") == true -> "Invalid password"
                    e.message?.contains("email") == true -> "Invalid email address"
                    e.message?.contains("network") == true -> "Network error"
                    else -> "Login failed: ${e.message}"
                }
                _isLoading.value = false
                onError(_errorMessage.value)
            }
        }
    }
    
    fun signup(onSuccess: () -> Unit, onError: (String) -> Unit) {
        if (name.value.isEmpty() || email.value.isEmpty() || password.value.isEmpty()) {
            _errorMessage.value = "Please fill all fields"
            onError("Please fill all fields")
            return
        }
        
        if (password.value.length < 6) {
            _errorMessage.value = "Password must be at least 6 characters"
            onError("Password must be at least 6 characters")
            return
        }
        
        _isLoading.value = true
        _errorMessage.value = ""
        
        viewModelScope.launch {
            try {
                val result = auth.createUserWithEmailAndPassword(
                    email.value.trim(),
                    password.value
                ).await()
                
                result.user?.let { user ->
                    // Update user profile with name
                    val profileUpdates = UserProfileChangeRequest.Builder()
                        .setDisplayName(name.value.trim())
                        .build()
                    
                    user.updateProfile(profileUpdates).await()
                    
                    _currentUser.value = user
                    _isLoading.value = false
                    onSuccess()
                } ?: run {
                    _errorMessage.value = "Signup failed"
                    _isLoading.value = false
                    onError("Signup failed")
                }
            } catch (e: Exception) {
                _errorMessage.value = when {
                    e.message?.contains("email") == true -> "Email already in use"
                    e.message?.contains("password") == true -> "Password is too weak"
                    e.message?.contains("network") == true -> "Network error"
                    else -> "Signup failed: ${e.message}"
                }
                _isLoading.value = false
                onError(_errorMessage.value)
            }
        }
    }
    
    fun clearError() {
        _errorMessage.value = ""
    }
    
    fun isUserLoggedIn(): Boolean {
        return auth.currentUser != null
    }
    
    fun getCurrentUser(): FirebaseUser? {
        return auth.currentUser
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FlipAuthScreen(
    viewModel: FlipAuthViewModel,
    onNavigateToLockScreen: () -> Unit
) {
    // Animation states
    val context=LocalContext.current
    val rotation by animateFloatAsState(
        targetValue = if (viewModel.isLoginMode.value) 0f else 180f,
        animationSpec = tween(
            durationMillis = 600,
            easing = EaseInOutCubic
        ),
        label = "cardRotation"
    )
    
    val infiniteTransition = rememberInfiniteTransition(label = "background")
    val backgroundShift by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(10000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "backgroundShift"
    )
    
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.linearGradient(
                    colors = listOf(
                        Color(0xFF1E3A8A), // Blue-800
                        Color(0xFF3B82F6), // Blue-500
                        Color(0xFF60A5FA), // Blue-400
                        Color(0xFF93C5FD)  // Blue-300
                    ),
                    start = androidx.compose.ui.geometry.Offset(
                        x = backgroundShift * 1000f,
                        y = 0f
                    ),
                    end = androidx.compose.ui.geometry.Offset(
                        x = (1f - backgroundShift) * 1000f,
                        y = 1000f
                    )
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        // Background circles - More visible and properly positioned
        repeat(20) { index ->
            val circleSize = (30 + index % 60).dp
            val delay = index * 200
            val floatAnimation by infiniteTransition.animateFloat(
                initialValue = -40f,
                targetValue = 40f,
                animationSpec = infiniteRepeatable(
                    animation = tween(3000 + delay, easing = EaseInOutCubic),
                    repeatMode = RepeatMode.Reverse
                ),
                label = "circle$index"
            )
            
            Box(
                modifier = Modifier
                    .size(circleSize)
                    .offset(
                        x = (20 + index * 60).dp,
                        y = (50 + index * 80 + floatAnimation).dp
                    )
                    .background(
                        color = Color.White.copy(alpha = 0.15f),
                        shape = RoundedCornerShape(50)
                    )
            )
        }
        
        // Additional larger circles
        repeat(12) { index ->
            val circleSize = (80 + index % 40).dp
            val delay = index * 400
            val floatAnimation by infiniteTransition.animateFloat(
                initialValue = -25f,
                targetValue = 25f,
                animationSpec = infiniteRepeatable(
                    animation = tween(5000 + delay, easing = EaseInOutCubic),
                    repeatMode = RepeatMode.Reverse
                ),
                label = "largeCircle$index"
            )
            
            Box(
                modifier = Modifier
                    .size(circleSize)
                    .offset(
                        x = (150 + index * 120).dp,
                        y = (200 + index * 150 + floatAnimation).dp
                    )
                    .background(
                        color = Color.White.copy(alpha = 0.1f),
                        shape = RoundedCornerShape(50)
                    )
            )
        }
        
        // Main card with flip animation
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(32.dp)
                .graphicsLayer(
                    rotationY = rotation,
                    cameraDistance = 8 * 340f
                )
        ) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .shadow(
                        elevation = 24.dp,
                        shape = RoundedCornerShape(24.dp),
                        spotColor = Color.Black.copy(alpha = 0.3f)
                    ),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(
                    containerColor = Color.White
                )
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp)
                        .graphicsLayer(
                            rotationY = if (rotation > 90f) 180f else 0f,
                            cameraDistance = 8 * 340f
                        )
                ) {
                        // Login Form (Front)
                        AnimatedVisibility(
                            visible = viewModel.isLoginMode.value,
                            enter = fadeIn(
                                animationSpec = tween(300, delayMillis = 300)
                            ) + slideInVertically(
                                initialOffsetY = { 50 },
                                animationSpec = tween(300, delayMillis = 300)
                            ),
                            exit = fadeOut(
                                animationSpec = tween(300)
                            ) + slideOutVertically(
                                targetOffsetY = { -50 },
                                animationSpec = tween(300)
                            )
                        ) {
                            LoginForm(
                                email = viewModel.email.value,
                                password = viewModel.password.value,
                                isLoading = viewModel.isLoading.value,
                                onEmailChange = { viewModel.updateEmail(it) },
                                onPasswordChange = { viewModel.updatePassword(it) },
                                onToggleMode = { viewModel.toggleMode() },
                                onSubmit = {
                                    viewModel.login(
                                        onSuccess = {
                                            Toast.makeText(context, "Login successful!", Toast.LENGTH_SHORT).show()
                                            onNavigateToLockScreen()
                                        },
                                        onError = { errorMessage ->
                                            Toast.makeText(context, errorMessage, Toast.LENGTH_LONG).show()
                                        }
                                    )
                                },
                                viewModel = viewModel
                            )
                        }
                        
                        // Signup Form (Back)
                        AnimatedVisibility(
                            visible = !viewModel.isLoginMode.value,
                            enter = fadeIn(
                                animationSpec = tween(300, delayMillis = 300)
                            ) + slideInVertically(
                                initialOffsetY = { 50 },
                                animationSpec = tween(300, delayMillis = 300)
                            ),
                            exit = fadeOut(
                                animationSpec = tween(300)
                            ) + slideOutVertically(
                                targetOffsetY = { -50 },
                                animationSpec = tween(300)
                            )
                        ) {
                            SignupForm(
                                name = viewModel.name.value,
                                email = viewModel.email.value,
                                password = viewModel.password.value,
                                isLoading = viewModel.isLoading.value,
                                onNameChange = { viewModel.updateName(it) },
                                onEmailChange = { viewModel.updateEmail(it) },
                                onPasswordChange = { viewModel.updatePassword(it) },
                                onToggleMode = { viewModel.toggleMode() },
                                onSubmit = {
                                    viewModel.signup(
                                        onSuccess = {
                                            Toast.makeText(context, "Account created successfully!", Toast.LENGTH_SHORT).show()
                                            onNavigateToLockScreen()
                                        },
                                        onError = { errorMessage ->
                                            Toast.makeText(context, errorMessage, Toast.LENGTH_LONG).show()
                                        }
                                    )
                                },
                                viewModel = viewModel
                            )
                        }
                    }
                }
            }
        }
    }


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoginForm(
    email: String,
    password: String,
    isLoading: Boolean,
    onEmailChange: (String) -> Unit,
    onPasswordChange: (String) -> Unit,
    onToggleMode: () -> Unit,
    onSubmit: () -> Unit,
    viewModel: FlipAuthViewModel
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Header
        Text(
            text = "Welcome Back",
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF1E3A8A),
            textAlign = TextAlign.Center,
            fontFamily = FontFamily.Default
        )
        
        Spacer(modifier = Modifier.height(8.dp))
        
        Text(
            text = "Sign in to continue your journey",
            fontSize = 14.sp,
            color = Color.Gray,
            textAlign = TextAlign.Center,
            fontFamily = FontFamily.Default
        )
        
        Spacer(modifier = Modifier.height(24.dp))
        
        // Email field
        OutlinedTextField(
            value = email,
            onValueChange = onEmailChange,
            label = { Text("Email", fontSize = 14.sp, fontFamily = FontFamily.Default) },
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 56.dp, max = 120.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = Color(0xFF3B82F6),
                unfocusedBorderColor = Color.Gray.copy(alpha = 0.5f),
                focusedLabelColor = Color(0xFF3B82F6),
                unfocusedLabelColor = Color.Gray,
                focusedTextColor = Color.Black,
                unfocusedTextColor = Color.Black
            ),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
            shape = RoundedCornerShape(12.dp),
            textStyle = androidx.compose.ui.text.TextStyle(
                fontSize = 14.sp,
                fontFamily = FontFamily.Default
            ),
            maxLines = 3
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Password field
        OutlinedTextField(
            value = password,
            onValueChange = onPasswordChange,
            label = { Text("Password", fontSize = 14.sp, fontFamily = FontFamily.Default) },
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 56.dp, max = 120.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = Color(0xFF3B82F6),
                unfocusedBorderColor = Color.Gray.copy(alpha = 0.5f),
                focusedLabelColor = Color(0xFF3B82F6),
                unfocusedLabelColor = Color.Gray,
                focusedTextColor = Color.Black,
                unfocusedTextColor = Color.Black
            ),
            visualTransformation = PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
            shape = RoundedCornerShape(12.dp),
            textStyle = androidx.compose.ui.text.TextStyle(
                fontSize = 14.sp,
                fontFamily = FontFamily.Default
            ),
            maxLines = 3
        )
        
        Spacer(modifier = Modifier.height(8.dp))
        
        // Parent/Children Toggle
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = if (viewModel.isParentMode.value) "Parent Mode" else "Child Mode",
                fontSize = 12.sp,
                color = Color(0xFF3B82F6),
                fontFamily = FontFamily.Default,
                fontWeight = FontWeight.Medium
            )
            
            Switch(
                checked = viewModel.isParentMode.value,
                onCheckedChange = { viewModel.toggleParentMode() },
                modifier = Modifier.scale(0.8f),
                colors = SwitchDefaults.colors(
                    checkedThumbColor = Color.White,
                    checkedTrackColor = Color(0xFF3B82F6),
                    uncheckedThumbColor = Color.White,
                    uncheckedTrackColor = Color.Gray.copy(alpha = 0.5f)
                )
            )
        }
        
        // Error message
        if (viewModel.errorMessage.value.isNotEmpty()) {
            Text(
                text = viewModel.errorMessage.value,
                color = Color.Red,
                fontSize = 12.sp,
                fontFamily = FontFamily.Default,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(8.dp))
        }
        
        // Login button
        Button(
            onClick = { onSubmit() },
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = Color(0xFF3B82F6)
            ),
            shape = RoundedCornerShape(16.dp),
            enabled = !isLoading
        ) {
            if (isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(24.dp),
                    color = Color.White
                )
            } else {
                Text(
                    text = "Login",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.SemiBold,
                    fontFamily = FontFamily.Default
                )
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Toggle to signup
        TextButton(
            onClick = onToggleMode,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = "Don't have an account? Sign up",
                color = Color(0xFF3B82F6),
                fontSize = 14.sp,
                fontFamily = FontFamily.Default
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SignupForm(
    name: String,
    email: String,
    password: String,
    isLoading: Boolean,
    onNameChange: (String) -> Unit,
    onEmailChange: (String) -> Unit,
    onPasswordChange: (String) -> Unit,
    onToggleMode: () -> Unit,
    onSubmit: () -> Unit,
    viewModel: FlipAuthViewModel
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Header
        Text(
            text = "Create Account",
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF1E3A8A),
            textAlign = TextAlign.Center,
            fontFamily = FontFamily.Default
        )
        
        Spacer(modifier = Modifier.height(8.dp))
        
        Text(
            text = "Join us and start your journey",
            fontSize = 14.sp,
            color = Color.Gray,
            textAlign = TextAlign.Center,
            fontFamily = FontFamily.Default
        )
        
        Spacer(modifier = Modifier.height(24.dp))
        
        // Name field
        OutlinedTextField(
            value = name,
            onValueChange = onNameChange,
            label = { Text("Full Name", fontSize = 14.sp, fontFamily = FontFamily.Default) },
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 56.dp, max = 120.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = Color(0xFF3B82F6),
                unfocusedBorderColor = Color.Gray.copy(alpha = 0.5f),
                focusedLabelColor = Color(0xFF3B82F6),
                unfocusedLabelColor = Color.Gray,
                focusedTextColor = Color.Black,
                unfocusedTextColor = Color.Black
            ),
            shape = RoundedCornerShape(12.dp),
            textStyle = androidx.compose.ui.text.TextStyle(
                fontSize = 14.sp,
                fontFamily = FontFamily.Default
            ),
            maxLines = 3
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Email field
        OutlinedTextField(
            value = email,
            onValueChange = onEmailChange,
            label = { Text("Email", fontSize = 14.sp, fontFamily = FontFamily.Default) },
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 56.dp, max = 120.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = Color(0xFF3B82F6),
                unfocusedBorderColor = Color.Gray.copy(alpha = 0.5f),
                focusedLabelColor = Color(0xFF3B82F6),
                unfocusedLabelColor = Color.Gray,
                focusedTextColor = Color.Black,
                unfocusedTextColor = Color.Black
            ),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
            shape = RoundedCornerShape(12.dp),
            textStyle = androidx.compose.ui.text.TextStyle(
                fontSize = 14.sp,
                fontFamily = FontFamily.Default
            ),
            maxLines = 3
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Password field
        OutlinedTextField(
            value = password,
            onValueChange = onPasswordChange,
            label = { Text("Password", fontSize = 14.sp, fontFamily = FontFamily.Default) },
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 56.dp, max = 120.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = Color(0xFF3B82F6),
                unfocusedBorderColor = Color.Gray.copy(alpha = 0.5f),
                focusedLabelColor = Color(0xFF3B82F6),
                unfocusedLabelColor = Color.Gray,
                focusedTextColor = Color.Black,
                unfocusedTextColor = Color.Black
            ),
            visualTransformation = PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
            shape = RoundedCornerShape(12.dp),
            textStyle = androidx.compose.ui.text.TextStyle(
                fontSize = 14.sp,
                fontFamily = FontFamily.Default
            ),
            maxLines = 3
        )
        
        Spacer(modifier = Modifier.height(8.dp))
        
        // Parent/Children Toggle
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = if (viewModel.isParentMode.value) "Parent Mode" else "Child Mode",
                fontSize = 12.sp,
                color = Color(0xFF3B82F6),
                fontFamily = FontFamily.Default,
                fontWeight = FontWeight.Medium
            )
            
            Switch(
                checked = viewModel.isParentMode.value,
                onCheckedChange = { viewModel.toggleParentMode() },
                modifier = Modifier.scale(0.8f),
                colors = SwitchDefaults.colors(
                    checkedThumbColor = Color.White,
                    checkedTrackColor = Color(0xFF3B82F6),
                    uncheckedThumbColor = Color.White,
                    uncheckedTrackColor = Color.Gray.copy(alpha = 0.5f)
                )
            )
        }
        
        // Error message
        if (viewModel.errorMessage.value.isNotEmpty()) {
            Text(
                text = viewModel.errorMessage.value,
                color = Color.Red,
                fontSize = 12.sp,
                fontFamily = FontFamily.Default,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(8.dp))
        }
        
        // Signup button
        Button(
            onClick = { onSubmit() },
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = Color(0xFF3B82F6)
            ),
            shape = RoundedCornerShape(16.dp),
            enabled = !isLoading
        ) {
            if (isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(24.dp),
                    color = Color.White
                )
            } else {
                Text(
                    text = "Create Account",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.SemiBold,
                    fontFamily = FontFamily.Default
                )
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Toggle to login
        TextButton(
            onClick = onToggleMode,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = "Already have an account? Login",
                color = Color(0xFF3B82F6),
                fontSize = 14.sp,
                fontFamily = FontFamily.Default
            )
        }
    }
}

