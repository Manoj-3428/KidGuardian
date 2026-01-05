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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.phonelock.models.UserRole
import com.example.phonelock.repository.UserRepository
import kotlinx.coroutines.launch

class AuthActivity : ComponentActivity() {
    private val viewModel: AuthViewModel by viewModels()
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        val userRole = intent.getStringExtra("USER_ROLE")?.let { 
            UserRole.valueOf(it) 
        } ?: UserRole.CHILD
        
        val repository = UserRepository(this)
        viewModel.setRole(userRole, repository)
        
        setContent {
            MaterialTheme {
                AuthScreen(
                    viewModel = viewModel,
                    onNavigateToProfile = {
                        // After SIGNUP: Show profile edit for both roles
                        if (userRole == UserRole.CHILD) {
                            startActivity(Intent(this, ChildProfileActivity::class.java))
                        } else {
                            // Parent after signup -> Go to link account screen (not profile)
                            startActivity(Intent(this, ParentLinkAccountActivity::class.java))
                        }
                        finish()
                    },
                    onNavigateToDashboard = {
                        // After LOGIN: Skip profile edit, go directly to dashboard
                        if (userRole == UserRole.CHILD) {
                            // Child login -> Go to MainActivity for permission check, then dashboard
                        startActivity(Intent(this, MainActivity::class.java))
                        } else {
                            // Parent login -> Go directly to login screen (enter Child ID)
                            startActivity(Intent(this, ParentLoginActivity::class.java))
                        }
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

class AuthViewModel : ViewModel() {
    private lateinit var repository: UserRepository
    
    private val _userRole = mutableStateOf(UserRole.CHILD)
    val userRole: State<UserRole> = _userRole
    
    private val _isLoginMode = mutableStateOf(true)
    val isLoginMode: State<Boolean> = _isLoginMode
    
    private val _email = mutableStateOf("")
    val email: State<String> = _email
    
    private val _password = mutableStateOf("")
    val password: State<String> = _password
    
    private val _name = mutableStateOf("")
    val name: State<String> = _name
    
    private val _childId = mutableStateOf("")
    val childId: State<String> = _childId
    
    private val _relationship = mutableStateOf("Mother")
    val relationship: State<String> = _relationship
    
    private val _isLoading = mutableStateOf(false)
    val isLoading: State<Boolean> = _isLoading
    
    private val _errorMessage = mutableStateOf("")
    val errorMessage: State<String> = _errorMessage
    
    fun setRole(role: UserRole, repo: UserRepository) {
        _userRole.value = role
        repository = repo
    }
    
    fun toggleMode() {
        _isLoginMode.value = !_isLoginMode.value
        _errorMessage.value = ""
    }
    
    fun updateEmail(value: String) {
        _email.value = value
        _errorMessage.value = ""
    }
    
    fun updatePassword(value: String) {
        _password.value = value
        _errorMessage.value = ""
    }
    
    fun updateName(value: String) {
        _name.value = value
        _errorMessage.value = ""
    }
    
    fun updateChildId(value: String) {
        _childId.value = value.uppercase()
        _errorMessage.value = ""
    }
    
    fun updateRelationship(value: String) {
        _relationship.value = value
    }
    
    fun login(onSuccess: () -> Unit, onError: (String) -> Unit) {
        if (_userRole.value == UserRole.PARENT) {
            // Parent login with Child ID only
            if (_childId.value.isEmpty()) {
                _errorMessage.value = "Please enter Child ID"
                onError("Please enter Child ID")
                return
            }
            
            _isLoading.value = true
            _errorMessage.value = ""
            
            viewModelScope.launch {
                val result = repository.linkParentWithChild(_childId.value.trim())
                
                if (result.isSuccess) {
                    _isLoading.value = false
                    onSuccess()
                } else {
                    _errorMessage.value = result.exceptionOrNull()?.message ?: "Failed to link account"
                    _isLoading.value = false
                    onError(_errorMessage.value)
                }
            }
        } else {
            // Child login with email and password
            if (_email.value.isEmpty() || _password.value.isEmpty()) {
                _errorMessage.value = "Please fill all fields"
                onError("Please fill all fields")
                return
            }
            
            _isLoading.value = true
            _errorMessage.value = ""
            
            viewModelScope.launch {
                val result = repository.loginUser(_email.value.trim(), _password.value)
                
                if (result.isSuccess) {
                    _isLoading.value = false
                    onSuccess()
                } else {
                    _errorMessage.value = result.exceptionOrNull()?.message ?: "Login failed"
                    _isLoading.value = false
                    onError(_errorMessage.value)
                }
            }
        }
    }
    
    fun signupChild(onSuccess: (String) -> Unit, onError: (String) -> Unit) {
        if (_name.value.isEmpty() || _email.value.isEmpty() || _password.value.isEmpty()) {
            _errorMessage.value = "Please fill all fields"
            onError("Please fill all fields")
            return
        }
        
        if (_password.value.length < 6) {
            _errorMessage.value = "Password must be at least 6 characters"
            onError("Password must be at least 6 characters")
            return
        }
        
        _isLoading.value = true
        _errorMessage.value = ""
        
        viewModelScope.launch {
            val result = repository.createChildAccount(
                name = _name.value.trim(),
                email = _email.value.trim(),
                password = _password.value
            )
            
            if (result.isSuccess) {
                val childId = result.getOrNull() ?: ""
                _isLoading.value = false
                onSuccess(childId)
            } else {
                _errorMessage.value = result.exceptionOrNull()?.message ?: "Signup failed"
                _isLoading.value = false
                onError(_errorMessage.value)
            }
        }
    }
    
}

@Composable
fun AuthScreen(
    viewModel: AuthViewModel,
    onNavigateToProfile: () -> Unit,
    onNavigateToDashboard: () -> Unit,
    onBackPressed: () -> Unit
) {
    val context = LocalContext.current
    
    // Animation - Only for child (parent has no signup)
    val rotation by animateFloatAsState(
        targetValue = if (viewModel.isLoginMode.value) 0f else 180f,
        animationSpec = tween(600, easing = EaseInOutCubic),
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
                        Color(0xFF1E3A8A),
                        Color(0xFF3B82F6),
                        Color(0xFF60A5FA),
                        Color(0xFF93C5FD)
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
        // Background shapes
        repeat(20) { index ->
            val size = (30 + index % 60).dp
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
                    .size(size)
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
        
        // Back button at top left
        IconButton(
            onClick = onBackPressed,
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(16.dp)
        ) {
            Icon(
                Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = "Back",
                tint = Color.White,
                modifier = Modifier.size(28.dp)
            )
        }
        
        // Main card
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(32.dp)
                .graphicsLayer(
                    // Only rotate for child (who has signup)
                    rotationY = if (viewModel.userRole.value == UserRole.CHILD) rotation else 0f,
                    cameraDistance = 8 * 340f
                )
        ) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .shadow(
                        elevation = 24.dp,
                        shape = RoundedCornerShape(24.dp)
                    ),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp)
                        .graphicsLayer(
                            rotationY = if (viewModel.userRole.value == UserRole.CHILD && rotation > 90f) 180f else 0f,
                            cameraDistance = 8 * 340f
                        )
                ) {
                    // For PARENT: Only show login (no signup)
                    if (viewModel.userRole.value == UserRole.PARENT) {
                        ParentLoginForm(
                            viewModel = viewModel,
                            onSubmit = {
                                viewModel.login(
                                    onSuccess = {
                                        Toast.makeText(context, "Login successful!", Toast.LENGTH_SHORT).show()
                                        onNavigateToDashboard()
                                    },
                                    onError = { error ->
                                        Toast.makeText(context, error, Toast.LENGTH_LONG).show()
                                    }
                                )
                            }
                        )
                    } else {
                        // For CHILD: Show login/signup toggle
                        AnimatedVisibility(
                            visible = viewModel.isLoginMode.value,
                            enter = fadeIn(tween(300, delayMillis = 300)) +
                                    slideInVertically(initialOffsetY = { 50 }, animationSpec = tween(300, delayMillis = 300)),
                            exit = fadeOut(tween(300)) +
                                    slideOutVertically(targetOffsetY = { -50 }, animationSpec = tween(300))
                        ) {
                            ChildLoginForm(
                                viewModel = viewModel,
                                onSubmit = {
                                    viewModel.login(
                                        onSuccess = {
                                            Toast.makeText(context, "Login successful!", Toast.LENGTH_SHORT).show()
                                            onNavigateToDashboard()
                                        },
                                        onError = { error ->
                                            Toast.makeText(context, error, Toast.LENGTH_LONG).show()
                                        }
                                    )
                                }
                            )
                        }
                        
                        AnimatedVisibility(
                            visible = !viewModel.isLoginMode.value,
                            enter = fadeIn(tween(300, delayMillis = 300)) +
                                    slideInVertically(initialOffsetY = { 50 }, animationSpec = tween(300, delayMillis = 300)),
                            exit = fadeOut(tween(300)) +
                                    slideOutVertically(targetOffsetY = { -50 }, animationSpec = tween(300))
                        ) {
                            ChildSignupForm(
                                viewModel = viewModel,
                                onSubmit = { childId ->
                                    Toast.makeText(context, "Account created! Your Child ID: $childId", Toast.LENGTH_LONG).show()
                                    onNavigateToProfile()
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChildLoginForm(viewModel: AuthViewModel, onSubmit: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Welcome Back",
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF8B5CF6)
        )
        
        Spacer(modifier = Modifier.height(8.dp))
        
        Text(
            text = "Child Login",
            fontSize = 14.sp,
            color = Color.Gray
        )
        
        Spacer(modifier = Modifier.height(24.dp))
        
        OutlinedTextField(
            value = viewModel.email.value,
            onValueChange = { viewModel.updateEmail(it) },
            label = { Text("Email") },
            modifier = Modifier.fillMaxWidth(),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = Color(0xFF8B5CF6),
                focusedLabelColor = Color(0xFF8B5CF6)
            ),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
            shape = RoundedCornerShape(12.dp)
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        OutlinedTextField(
            value = viewModel.password.value,
            onValueChange = { viewModel.updatePassword(it) },
            label = { Text("Password") },
            modifier = Modifier.fillMaxWidth(),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = Color(0xFF8B5CF6),
                focusedLabelColor = Color(0xFF8B5CF6)
            ),
            visualTransformation = PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
            shape = RoundedCornerShape(12.dp)
        )
        
        if (viewModel.errorMessage.value.isNotEmpty()) {
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = viewModel.errorMessage.value,
                color = Color.Red,
                fontSize = 12.sp,
                textAlign = TextAlign.Center
            )
        }
        
        Spacer(modifier = Modifier.height(24.dp))
        
        Button(
            onClick = onSubmit,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF8B5CF6)),
            shape = RoundedCornerShape(16.dp),
            enabled = !viewModel.isLoading.value
        ) {
            if (viewModel.isLoading.value) {
                CircularProgressIndicator(modifier = Modifier.size(24.dp), color = Color.White)
            } else {
                Text("Login", fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        TextButton(onClick = { viewModel.toggleMode() }) {
            Text(
                text = "Don't have an account? Sign up",
                color = Color(0xFF8B5CF6),
                fontSize = 13.sp,
                fontWeight = FontWeight.Normal
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ParentLoginForm(viewModel: AuthViewModel, onSubmit: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Parent Access",
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF3B82F6)
        )
        
        Spacer(modifier = Modifier.height(8.dp))
        
        Text(
            text = "Enter your child's ID to link accounts",
            fontSize = 14.sp,
            color = Color.Gray,
            textAlign = TextAlign.Center
        )
        
        Spacer(modifier = Modifier.height(24.dp))
        
        OutlinedTextField(
            value = viewModel.childId.value,
            onValueChange = { viewModel.updateChildId(it) },
            label = { Text("Child ID") },
            placeholder = { Text("Enter your child's unique ID") },
            modifier = Modifier.fillMaxWidth(),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = Color(0xFF3B82F6),
                focusedLabelColor = Color(0xFF3B82F6)
            ),
            shape = RoundedCornerShape(12.dp),
            leadingIcon = {
                Icon(
                    imageVector = Icons.Default.Person,
                    contentDescription = "Child ID",
                    tint = Color(0xFF3B82F6)
                )
            }
        )
        
        if (viewModel.errorMessage.value.isNotEmpty()) {
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = viewModel.errorMessage.value,
                color = Color.Red,
                fontSize = 12.sp,
                textAlign = TextAlign.Center
            )
        }
        
        Spacer(modifier = Modifier.height(24.dp))
        
        Button(
            onClick = onSubmit,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF3B82F6)),
            shape = RoundedCornerShape(16.dp),
            enabled = !viewModel.isLoading.value
        ) {
            if (viewModel.isLoading.value) {
                CircularProgressIndicator(modifier = Modifier.size(24.dp), color = Color.White)
            } else {
                Text("Link Account", fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        Text(
            text = "Get the Child ID from your child's app",
            fontSize = 12.sp,
            color = Color.Gray,
            textAlign = TextAlign.Center
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChildSignupForm(viewModel: AuthViewModel, onSubmit: (String) -> Unit) {
    val context = LocalContext.current
    
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Create Child Account",
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF8B5CF6),
            maxLines = 1
        )
        
        Spacer(modifier = Modifier.height(24.dp))
        
        OutlinedTextField(
            value = viewModel.name.value,
            onValueChange = { viewModel.updateName(it) },
            label = { Text("Name") },
            placeholder = { Text("Enter your name") },
            modifier = Modifier.fillMaxWidth(),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = Color(0xFF8B5CF6),
                focusedLabelColor = Color(0xFF8B5CF6)
            ),
            shape = RoundedCornerShape(12.dp)
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        OutlinedTextField(
            value = viewModel.email.value,
            onValueChange = { viewModel.updateEmail(it) },
            label = { Text("Email") },
            placeholder = { Text("Enter your email") },
            modifier = Modifier.fillMaxWidth(),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = Color(0xFF8B5CF6),
                focusedLabelColor = Color(0xFF8B5CF6)
            ),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
            shape = RoundedCornerShape(12.dp)
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        OutlinedTextField(
            value = viewModel.password.value,
            onValueChange = { viewModel.updatePassword(it) },
            label = { Text("Password (min 6 characters)") },
            modifier = Modifier.fillMaxWidth(),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = Color(0xFF8B5CF6),
                focusedLabelColor = Color(0xFF8B5CF6)
            ),
            visualTransformation = PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
            shape = RoundedCornerShape(12.dp)
        )
        
        if (viewModel.errorMessage.value.isNotEmpty()) {
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = viewModel.errorMessage.value,
                color = Color.Red,
                fontSize = 12.sp,
                textAlign = TextAlign.Center
            )
        }
        
        Spacer(modifier = Modifier.height(24.dp))
        
        Button(
            onClick = {
                viewModel.signupChild(
                    onSuccess = { childId -> onSubmit(childId) },
                    onError = { error ->
                        Toast.makeText(context, error, Toast.LENGTH_LONG).show()
                    }
                )
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF8B5CF6)),
            shape = RoundedCornerShape(16.dp),
            enabled = !viewModel.isLoading.value
        ) {
            if (viewModel.isLoading.value) {
                CircularProgressIndicator(modifier = Modifier.size(24.dp), color = Color.White)
            } else {
                Text("Create Account", fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        TextButton(onClick = { viewModel.toggleMode() }) {
            Text(
                text = "Already have an account? Login",
                color = Color(0xFF8B5CF6),
                fontSize = 13.sp,
                fontWeight = FontWeight.Normal
            )
        }
    }
}

// Removed unused ParentSignupForm function

