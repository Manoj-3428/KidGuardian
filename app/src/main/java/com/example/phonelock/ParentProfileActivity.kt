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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.phonelock.models.Child
import com.example.phonelock.models.Parent
import com.example.phonelock.repository.UserRepository
import kotlinx.coroutines.launch

class ParentProfileActivity : ComponentActivity() {
    private val viewModel: ParentProfileViewModel by viewModels()
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        val repository = UserRepository(this)
        viewModel.setRepository(repository)
        
        setContent {
            MaterialTheme {
                ParentProfileScreen(
                    viewModel = viewModel,
                    onNavigateToDashboard = {
                        // After profile edit, go to Link Account screen (not dashboard)
                        startActivity(Intent(this, ParentLinkAccountActivity::class.java))
                        finish()
                    }
                )
            }
        }
    }
}

class ParentProfileViewModel : ViewModel() {
    private lateinit var repository: UserRepository
    
    private val _name = mutableStateOf("")
    val name: State<String> = _name
    
    private val _relationship = mutableStateOf("Parent")
    val relationship: State<String> = _relationship
    
    private val _childName = mutableStateOf("")
    val childName: State<String> = _childName
    
    private val _isLoading = mutableStateOf(true)
    val isLoading: State<Boolean> = _isLoading
    
    private val _isSaving = mutableStateOf(false)
    val isSaving: State<Boolean> = _isSaving
    
    fun setRepository(repo: UserRepository) {
        repository = repo
        loadParentData()
    }
    
    private fun loadParentData() {
        viewModelScope.launch {
            val parentResult = repository.getParentData()
            
            if (parentResult.isSuccess) {
                val parent = parentResult.getOrNull()
                _name.value = parent?.name ?: ""
                _relationship.value = parent?.relationship ?: "Parent"
                
                // Load child data using parent's linkedChildId
                parent?.linkedChildId?.let { childId ->
                    val childResult = repository.getChildByChildId(childId)
                    if (childResult.isSuccess) {
                        val child = childResult.getOrNull()
                        _childName.value = child?.name ?: ""
                    }
                }
            }
            
            _isLoading.value = false
        }
    }
    
    fun updateName(value: String) {
        _name.value = value
    }
    
    fun updateRelationship(value: String) {
        _relationship.value = value
    }
    
    fun saveProfile(onSuccess: () -> Unit, onError: (String) -> Unit) {
        if (_name.value.isEmpty()) {
            onError("Please enter your name")
            return
        }
        
        _isSaving.value = true
        
        viewModelScope.launch {
            val result = repository.updateParentProfile(
                name = _name.value.trim(),
                relationship = _relationship.value
            )
            
            if (result.isSuccess) {
                _isSaving.value = false
                onSuccess()
            } else {
                _isSaving.value = false
                onError(result.exceptionOrNull()?.message ?: "Failed to save profile")
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ParentProfileScreen(
    viewModel: ParentProfileViewModel,
    onNavigateToDashboard: () -> Unit
) {
    val context = LocalContext.current
    
    // Animated background
    val infiniteTransition = rememberInfiniteTransition(label = "background")
    val animatedGradient by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(3000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "gradient"
    )
    
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
        if (viewModel.isLoading.value) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = Color.White)
            }
        } else {
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
                            imageVector = Icons.Default.Person,
                            contentDescription = "Parent Profile",
                            modifier = Modifier.size(48.dp),
                            tint = Color(0xFF3B82F6)
                        )
                        
                        Spacer(modifier = Modifier.height(16.dp))
                        
                        Text(
                            text = "Parent Profile",
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF1F2937)
                        )
                        
                        Spacer(modifier = Modifier.height(8.dp))
                        
                        Text(
                            text = "Complete your profile to start monitoring",
                            fontSize = 14.sp,
                            color = Color.Gray,
                            textAlign = TextAlign.Center
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(24.dp))
                
                // Child Info Card
                if (viewModel.childName.value.isNotEmpty()) {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .shadow(4.dp, RoundedCornerShape(12.dp)),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFF0F9FF)),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.AccountCircle,
                                    contentDescription = "Child",
                                    tint = Color(0xFF3B82F6),
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Linked Child",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF3B82F6)
                                )
                            }
                            
                            Spacer(modifier = Modifier.height(8.dp))
                            
                            Text(
                                text = viewModel.childName.value,
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF1F2937)
                            )
                            
                            Text(
                                text = "Your child is successfully linked",
                                fontSize = 12.sp,
                                color = Color.Gray
                            )
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(16.dp))
                }
                
                // Profile Form
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .shadow(4.dp, RoundedCornerShape(12.dp)),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp)
                    ) {
                        Text(
                            text = "Your Information",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF1F2937)
                        )
                        
                        Spacer(modifier = Modifier.height(16.dp))
                        
                        OutlinedTextField(
                            value = viewModel.name.value,
                            onValueChange = { viewModel.updateName(it) },
                            label = { Text("Your Name") },
                            modifier = Modifier.fillMaxWidth(),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = Color(0xFF3B82F6),
                                focusedLabelColor = Color(0xFF3B82F6)
                            ),
                            shape = RoundedCornerShape(8.dp)
                        )
                        
                        Spacer(modifier = Modifier.height(16.dp))
                        
                        var expanded by remember { mutableStateOf(false) }
                        val relationships = listOf("Mother", "Father", "Guardian")
                        
                        ExposedDropdownMenuBox(
                            expanded = expanded,
                            onExpandedChange = { expanded = !expanded }
                        ) {
                            OutlinedTextField(
                                value = viewModel.relationship.value,
                                onValueChange = { },
                                readOnly = true,
                                label = { Text("Relationship to Child") },
                                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .menuAnchor(),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = Color(0xFF3B82F6),
                                    focusedLabelColor = Color(0xFF3B82F6)
                                ),
                                shape = RoundedCornerShape(8.dp)
                            )
                            
                            ExposedDropdownMenu(
                                expanded = expanded,
                                onDismissRequest = { expanded = false }
                            ) {
                                relationships.forEach { relationship ->
                                    DropdownMenuItem(
                                        text = { Text(relationship) },
                                        onClick = {
                                            viewModel.updateRelationship(relationship)
                                            expanded = false
                                        }
                                    )
                                }
                            }
                        }
                        
                        Spacer(modifier = Modifier.height(24.dp))
                        
                        Button(
                            onClick = {
                                viewModel.saveProfile(
                                    onSuccess = {
                                        Toast.makeText(context, "Profile saved successfully!", Toast.LENGTH_SHORT).show()
                                        onNavigateToDashboard()
                                    },
                                    onError = { error ->
                                        Toast.makeText(context, error, Toast.LENGTH_LONG).show()
                                    }
                                )
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(56.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF3B82F6)),
                            shape = RoundedCornerShape(12.dp),
                            enabled = !viewModel.isSaving.value
                        ) {
                            if (viewModel.isSaving.value) {
                                CircularProgressIndicator(modifier = Modifier.size(24.dp), color = Color.White)
                            } else {
                                Text("Save Profile", fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                            }
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(24.dp))
                
                Text(
                    text = "You can update your profile anytime from settings",
                    fontSize = 12.sp,
                    color = Color.White.copy(alpha = 0.8f),
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}
