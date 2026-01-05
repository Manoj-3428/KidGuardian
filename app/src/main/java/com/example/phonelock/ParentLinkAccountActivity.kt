package com.example.phonelock

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.phonelock.repository.UserRepository
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class ParentLinkAccountActivity : ComponentActivity() {
    
    private lateinit var repository: UserRepository
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        repository = UserRepository(this)
        
        setContent {
            MaterialTheme {
                ParentLinkScreen(
                    onLinkSuccess = {
                        // Navigate to parent dashboard after successful linking
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
    
    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    fun ParentLinkScreen(
        onLinkSuccess: () -> Unit,
        onBackPressed: () -> Unit
    ) {
        var childId by remember { mutableStateOf("") }
        var passcode by remember { mutableStateOf("") }
        var isLoading by remember { mutableStateOf(false) }
        var errorMessage by remember { mutableStateOf("") }
        var passcodeRequested by remember { mutableStateOf(false) }
        var successMessage by remember { mutableStateOf("") }
        var showRoleSelection by remember { mutableStateOf(false) }
        var selectedRole by remember { mutableStateOf("") }
        var parentName by remember { mutableStateOf("") }
        val scope = rememberCoroutineScope()
        
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color(0xFF667eea),
                            Color(0xFF764ba2)
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
                // Top Bar
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onBackPressed) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = Color.White
                        )
                    }
                    Spacer(Modifier.weight(1f))
                }
                
                Spacer(Modifier.height(20.dp))
                
                // Title
                Icon(
                    painter = painterResource(id = R.drawable.ic_link),
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(80.dp)
                )
                
                Spacer(Modifier.height(16.dp))
                
                Text(
                    text = "Link with Child Account",
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    textAlign = TextAlign.Center
                )
                
                Spacer(Modifier.height(8.dp))
                
                Text(
                    text = if (!passcodeRequested) {
                        "Enter Child ID and request a passcode.\nCode will appear in child's dashboard."
                    } else {
                        "Passcode sent! Check child's dashboard\nand enter the code within 5 minutes."
                    },
                    fontSize = 14.sp,
                    color = Color.White.copy(alpha = 0.9f),
                    textAlign = TextAlign.Center
                )
                
                Spacer(Modifier.height(24.dp))
                
                // Input Card
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = Color.White
                    )
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        // Child ID Field
                        OutlinedTextField(
                            value = childId,
                            onValueChange = { 
                                childId = it
                                errorMessage = ""
                                successMessage = ""
                            },
                            label = { Text("Child ID") },
                            placeholder = { Text("Enter child's unique ID") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            enabled = !isLoading && !passcodeRequested
                        )
                        
                        // Success Message
                        if (successMessage.isNotEmpty()) {
                            Spacer(Modifier.height(8.dp))
                            Text(
                                text = successMessage,
                                color = Color(0xFF10B981),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                        
                        // Show passcode field only after request
                        if (passcodeRequested) {
                            Spacer(Modifier.height(16.dp))
                            
                            // Passcode Field
                            OutlinedTextField(
                                value = passcode,
                                onValueChange = { 
                                    passcode = it
                                    errorMessage = ""
                                },
                                label = { Text("Passcode") },
                                placeholder = { Text("6-digit code from child dashboard") },
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true,
                                visualTransformation = PasswordVisualTransformation(),
                                enabled = !isLoading
                            )
                        }
                        
                        // Error Message
                        if (errorMessage.isNotEmpty()) {
                            Spacer(Modifier.height(8.dp))
                            Text(
                                text = errorMessage,
                                color = Color.Red,
                                fontSize = 12.sp
                            )
                        }
                        
                        Spacer(Modifier.height(24.dp))
                        
                        // Request Passcode or Link Button
                        Button(
                            onClick = {
                                if (!passcodeRequested) {
                                    // Step 1: Request Passcode
                                    if (childId.isBlank()) {
                                        errorMessage = "Please enter Child ID"
                                        return@Button
                                    }
                                    
                                    isLoading = true
                                    scope.launch {
                                        requestPasscode(
                                            childId = childId,
                                            onSuccess = {
                                                isLoading = false
                                                passcodeRequested = true
                                                successMessage = "✅ Passcode sent to child's dashboard!"
                                            },
                                            onError = { error ->
                                                isLoading = false
                                                errorMessage = error
                                            }
                                        )
                                    }
                                } else {
                                    // Step 2: Link Accounts
                                    if (passcode.isBlank()) {
                                        errorMessage = "Please enter passcode"
                                        return@Button
                                    }
                                    if (passcode.length != 6) {
                                        errorMessage = "Passcode must be 6 digits"
                                        return@Button
                                    }
                                    
                                    // Link accounts
                                    isLoading = true
                                    scope.launch {
                                        linkAccounts(
                                            childId = childId,
                                            passcode = passcode,
                                            onSuccess = {
                                                isLoading = false
                                                Toast.makeText(
                                                    this@ParentLinkAccountActivity,
                                                    "✅ Passcode verified! Please select your role.",
                                                    Toast.LENGTH_SHORT
                                                ).show()
                                                showRoleSelection = true
                                            },
                                            onError = { error ->
                                                isLoading = false
                                                errorMessage = error
                                            }
                                        )
                                    }
                                }
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(56.dp),
                            enabled = !isLoading,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (passcodeRequested) Color(0xFF10B981) else Color(0xFF667eea)
                            )
                        ) {
                            if (isLoading) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(24.dp),
                                    color = Color.White
                                )
                            } else {
                                Text(
                                    if (passcodeRequested) "Link Accounts" else "Request Passcode",
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
                
                // Role Selection Screen
                if (showRoleSelection) {
                    Spacer(Modifier.height(24.dp))
                    
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.95f))
                    ) {
                        Column(
                            modifier = Modifier.padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "👨‍👩‍👧‍👦 Select Your Role",
                                fontSize = 24.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF1F2937),
                                textAlign = TextAlign.Center
                            )
                            
                            Spacer(Modifier.height(16.dp))
                            
                            Text(
                                text = "Choose your relationship to the child",
                                fontSize = 14.sp,
                                color = Color(0xFF6B7280),
                                textAlign = TextAlign.Center
                            )
                            
                            Spacer(Modifier.height(24.dp))
                            
                            // Role Options
                            val roles = listOf("Mother", "Father", "Guardian", "Other")
                            
                            roles.forEach { role ->
                                Card(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 4.dp),
                                    colors = CardDefaults.cardColors(
                                        containerColor = if (selectedRole == role) 
                                            Color(0xFF667eea).copy(alpha = 0.1f) 
                                        else Color.Transparent
                                    ),
                                    onClick = {
                                        selectedRole = role
                                        // Auto-fetch name if this is an existing role
                                        scope.launch {
                                            autoFetchParentName(childId, role, repository) { name ->
                                                parentName = name
                                            }
                                        }
                                    }
                                ) {
                                    Text(
                                        text = role,
                                        modifier = Modifier.padding(16.dp),
                                        fontSize = 16.sp,
                                        color = if (selectedRole == role) 
                                            Color(0xFF667eea) 
                                        else Color(0xFF1F2937)
                                    )
                                }
                            }
                            
                            Spacer(Modifier.height(16.dp))
                            
                            // Parent Name Field (auto-filled if existing role)
                            OutlinedTextField(
                                value = parentName,
                                onValueChange = { parentName = it },
                                label = { Text("Your Name") },
                                placeholder = { Text("Enter your name") },
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true,
                                enabled = !isLoading
                            )
                            
                            Spacer(Modifier.height(24.dp))
                            
                            // Complete Setup Button
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
                                    
                                    // Complete account setup
                                    isLoading = true
                                    scope.launch {
                                        completeAccountSetup(
                                            childId = childId,
                                            role = selectedRole,
                                            name = parentName,
                                            onSuccess = {
                                                isLoading = false
                                                Toast.makeText(
                                                    this@ParentLinkAccountActivity,
                                                    "✅ Account setup complete!",
                                                    Toast.LENGTH_SHORT
                                                ).show()
                                                onLinkSuccess()
                                            },
                                            onError = { error ->
                                                isLoading = false
                                                errorMessage = error
                                            }
                                        )
                                    }
                                },
                                modifier = Modifier.fillMaxWidth(),
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981)),
                                enabled = selectedRole.isNotEmpty() && parentName.isNotEmpty()
                            ) {
                                if (isLoading) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(24.dp),
                                        color = Color.White
                                    )
                                } else {
                                    Text(
                                        "Complete Setup",
                                        fontSize = 16.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                            
                            Spacer(Modifier.height(16.dp))
                            
                            // Back to Passcode
                            TextButton(
                                onClick = {
                                    showRoleSelection = false
                                    successMessage = ""
                                    errorMessage = ""
                                }
                            ) {
                                Text(
                                    "← Back to Passcode Entry",
                                    color = Color(0xFF667eea)
                                )
                            }
                        }
                    }
                } else {
                    Spacer(Modifier.height(32.dp))
                }
                
                Spacer(Modifier.height(32.dp))
            }
        }
    }
    
    private suspend fun requestPasscode(
        childId: String,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        // Request passcode generation for child
        val result = repository.requestLinkPasscode(childId)
        if (result.isSuccess) {
            onSuccess()
        } else {
            onError(result.exceptionOrNull()?.message ?: "Failed to request passcode")
        }
    }
    
    private suspend fun linkAccounts(
        childId: String,
        passcode: String,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        // Check Firebase authentication status
        val firebaseUser = FirebaseAuth.getInstance().currentUser
        val currentUserId = repository.getCurrentUserId()
        
        Log.d("LINK_DEBUG", "Firebase user: ${firebaseUser?.uid}")
        Log.d("LINK_DEBUG", "Repository user: $currentUserId")
        Log.d("LINK_DEBUG", "Child ID: $childId")
        Log.d("LINK_DEBUG", "Passcode: $passcode")
        
        // If no Firebase user, we need to create a parent account first
        if (firebaseUser == null) {
            Log.e("LINK_DEBUG", "No Firebase user found - creating parent account")
            // For now, we'll use the childId as a temporary parent ID
            // In a real app, you'd want proper parent authentication
            val tempParentId = "parent_${childId}_${System.currentTimeMillis()}"
            Log.d("LINK_DEBUG", "Using temporary parent ID: $tempParentId")
            
            // Continue with linking using temporary ID
            linkWithTemporaryParent(tempParentId, childId, passcode, onSuccess, onError)
            return
        }
        
        if (currentUserId == null) {
            Log.e("LINK_DEBUG", "No current user ID found")
            onError("Not logged in. Please sign in again.")
            return
        }
        
        // Get child user data
        val childResult = repository.getUserById(childId)
        if (childResult.isFailure) {
            Log.e("LINK_DEBUG", "Failed to get child data: ${childResult.exceptionOrNull()?.message}")
            onError("Child ID not found")
            return
        }
        
        val childUser = childResult.getOrNull()
        if (childUser == null) {
            Log.e("LINK_DEBUG", "Child user is null")
            onError("Child account not found")
            return
        }
        
        Log.d("LINK_DEBUG", "Child user found: ${childUser.childId}, passcode: ${childUser.linkPasscode}, generatedAt: ${childUser.passcodeGeneratedAt}")
        
        // Check if passcode exists
        if (childUser.linkPasscode == null) {
            onError("No passcode found. Please request a new passcode.")
            return
        }
        
        // Check if passcode is expired (5 minutes)
        if (repository.isPasscodeExpired(childUser.passcodeGeneratedAt)) {
            onError("Passcode expired. Please request a new passcode.")
            return
        }
        
        // Verify passcode
        if (childUser.linkPasscode != passcode) {
            onError("Invalid passcode")
            return
        }
        
        // Check if already linked
        if (childUser.parentId != null && childUser.parentId.isNotEmpty()) {
            onError("This child is already linked to another parent")
            return
        }
        
        // Link accounts
        val linkResult = repository.linkParentToChild(currentUserId, childId)
        if (linkResult.isSuccess) {
            // Clear the passcode after successful linking
            repository.clearLinkPasscode(childId)
            onSuccess()
        } else {
            onError("Failed to link accounts: ${linkResult.exceptionOrNull()?.message}")
        }
    }
    
    private suspend fun linkWithTemporaryParent(
        tempParentId: String,
        childId: String,
        passcode: String,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        // Get child user data
        val childResult = repository.getUserById(childId)
        if (childResult.isFailure) {
            Log.e("LINK_DEBUG", "Failed to get child data: ${childResult.exceptionOrNull()?.message}")
            onError("Child ID not found")
            return
        }
        
        val childUser = childResult.getOrNull()
        if (childUser == null) {
            Log.e("LINK_DEBUG", "Child user is null")
            onError("Child account not found")
            return
        }
        
        Log.d("LINK_DEBUG", "Child user found: ${childUser.childId}, passcode: ${childUser.linkPasscode}, generatedAt: ${childUser.passcodeGeneratedAt}")
        
        // Check if passcode exists
        if (childUser.linkPasscode == null) {
            onError("No passcode found. Please request a new passcode.")
            return
        }
        
        // Check if passcode is expired (5 minutes)
        if (repository.isPasscodeExpired(childUser.passcodeGeneratedAt)) {
            onError("Passcode expired. Please request a new passcode.")
            return
        }
        
        // Verify passcode
        if (childUser.linkPasscode != passcode) {
            onError("Invalid passcode")
            return
        }
        
        // Check if already linked
        if (childUser.parentId != null && childUser.parentId.isNotEmpty()) {
            onError("This child is already linked to another parent")
            return
        }
        
        // Link accounts using temporary parent ID
        val linkResult = repository.linkParentToChild(tempParentId, childId)
        if (linkResult.isSuccess) {
            // Clear the passcode after successful linking
            repository.clearLinkPasscode(childId)
            // Store the parent ID for future use
            repository.saveParentId(tempParentId)
            Log.d("LINK_DEBUG", "Successfully linked with temporary parent ID: $tempParentId")
            onSuccess()
        } else {
            onError("Failed to link accounts: ${linkResult.exceptionOrNull()?.message}")
        }
    }
    
    private suspend fun completeAccountSetup(
        childId: String,
        role: String,
        name: String,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        try {
            // Get the parent ID that was used for linking
            val parentId = repository.getParentId()
            if (parentId == null) {
                onError("Parent ID not found. Please try linking again.")
                return
            }
            
            // Create parent account
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
            
            // Save parent role for session management
            repository.saveUserRole(com.example.phonelock.models.UserRole.PARENT)
            
            Log.d("LINK_DEBUG", "✅ Parent account setup completed successfully - ID: $parentId")
            onSuccess()
        } catch (e: Exception) {
            Log.e("LINK_DEBUG", "Error completing account setup", e)
            onError("Failed to complete account setup: ${e.message}")
        }
    }
    
    private suspend fun autoFetchParentName(
        childId: String,
        role: String,
        repository: UserRepository,
        onNameFetched: (String) -> Unit
    ) {
        try {
            // Try to find existing parent with this role for this child
            // For now, we'll leave name empty and let user enter it
            // In future, we can implement logic to fetch existing parent names
            onNameFetched("")
        } catch (e: Exception) {
            Log.e("LINK_DEBUG", "Error fetching parent name", e)
            onNameFetched("")
        }
    }
}

