package com.example.phonelock

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.phonelock.models.UserRole
import com.example.phonelock.repository.UserRepository

class RoleSelectionActivity : ComponentActivity() {
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        val repository = UserRepository(this)
        
        setContent {
            MaterialTheme {
                RoleSelectionScreen(
                    onRoleSelected = { role ->
                        repository.saveUserRole(role)
                        when (role) {
                            com.example.phonelock.models.UserRole.CHILD -> {
                                // Child goes to AuthActivity for signup/login
                        val intent = Intent(this, AuthActivity::class.java).apply {
                            putExtra("USER_ROLE", role.name)
                        }
                        startActivity(intent)
                        finish()
                            }
                            com.example.phonelock.models.UserRole.PARENT -> {
                                // Parent goes directly to login (no signup needed)
                                startActivity(Intent(this, ParentLoginActivity::class.java))
                                finish()
                            }
                        }
                    }
                )
            }
        }
    }
}

@Composable
fun RoleSelectionScreen(onRoleSelected: (UserRole) -> Unit) {
    var selectedRole by remember { mutableStateOf<UserRole?>(null) }
    
    // Animated background
    val infiniteTransition = rememberInfiniteTransition(label = "background")
    val backgroundShift by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(15000, easing = LinearEasing),
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
                        Color(0xFF6366F1), // Indigo-500
                        Color(0xFF8B5CF6), // Violet-500
                        Color(0xFFA855F7), // Purple-500
                        Color(0xFFEC4899)  // Pink-500
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
            )
    ) {
        // Animated floating shapes
        repeat(15) { index ->
            val size = (40 + index * 15).dp
            val delay = index * 300
            val floatAnimation by infiniteTransition.animateFloat(
                initialValue = -30f,
                targetValue = 30f,
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
                        x = (30 + index * 80).dp,
                        y = (80 + index * 120 + floatAnimation).dp
                    )
                    .background(
                        color = Color.White.copy(alpha = 0.1f),
                        shape = RoundedCornerShape(50)
                    )
            )
        }
        
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // App branding
            Text(
                text = "KidGuardian",
                fontSize = 36.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                textAlign = TextAlign.Center
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Text(
                text = "Protecting What Matters Most",
                fontSize = 16.sp,
                color = Color.White.copy(alpha = 0.9f),
                textAlign = TextAlign.Center
            )
            
            Spacer(modifier = Modifier.height(48.dp))
            
            Text(
                text = "I am a...",
                fontSize = 24.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color.White,
                textAlign = TextAlign.Center
            )
            
            Spacer(modifier = Modifier.height(32.dp))
            
            // Role selection cards
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                RoleCard(
                    modifier = Modifier.fillMaxWidth(),
                    role = UserRole.CHILD,
                    imageResId = R.drawable.child,
                    title = "Child",
                    description = "Create your safe space",
                    gradientColors = listOf(
                        Color(0xFF667EEA),
                        Color(0xFF764BA2)
                    ),
                    isSelected = selectedRole == UserRole.CHILD,
                    onClick = {
                        selectedRole = UserRole.CHILD
                        onRoleSelected(UserRole.CHILD)
                    }
                )
                
                RoleCard(
                    modifier = Modifier.fillMaxWidth(),
                    role = UserRole.PARENT,
                    imageResId = R.drawable.family,
                    title = "Parent",
                    description = "Monitor & protect your child",
                    gradientColors = listOf(
                        Color(0xFFF093FB),
                        Color(0xFFF5576C)
                    ),
                    isSelected = selectedRole == UserRole.PARENT,
                    onClick = {
                        selectedRole = UserRole.PARENT
                        onRoleSelected(UserRole.PARENT)
                    }
                )
            }
            
            Spacer(modifier = Modifier.height(32.dp))
            
            Text(
                text = "Select your role to continue",
                fontSize = 14.sp,
                color = Color.White.copy(alpha = 0.8f),
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
fun RoleCard(
    modifier: Modifier = Modifier,
    role: UserRole,
    imageResId: Int,
    title: String,
    description: String,
    gradientColors: List<Color>,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val scale by animateFloatAsState(
        targetValue = if (isSelected) 1.02f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "cardScale"
    )
    
    val elevation by animateDpAsState(
        targetValue = if (isSelected) 16.dp else 8.dp,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "cardElevation"
    )
    
    Card(
        modifier = modifier
            .height(200.dp)
            .scale(scale)
            .shadow(
                elevation = elevation,
                shape = RoundedCornerShape(28.dp),
                spotColor = Color.Black.copy(alpha = 0.3f)
            )
            .clickable { onClick() },
        shape = RoundedCornerShape(28.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color.White
        )
    ) {
        Box(
            modifier = Modifier.fillMaxSize()
        ) {
            // Background gradient
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.linearGradient(
                            colors = if (isSelected) gradientColors else listOf(
                                Color(0xFFF5F5F5),
                                Color(0xFFE0E0E0)
                            ),
                            start = androidx.compose.ui.geometry.Offset(0f, 0f),
                            end = androidx.compose.ui.geometry.Offset(1000f, 1000f)
        )
                    )
            )
            
            // Content
            Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
        ) {
                // Left side: Image
            Box(
                modifier = Modifier
                        .weight(0.5f)
                        .fillMaxHeight(),
                contentAlignment = Alignment.Center
            ) {
                    Image(
                        painter = painterResource(id = imageResId),
                    contentDescription = title,
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(8.dp),
                        contentScale = ContentScale.Fit
                )
            }
            
                // Right side: Text content
                Column(
                    modifier = Modifier
                        .weight(0.5f)
                        .fillMaxHeight()
                        .padding(start = 16.dp),
                    horizontalAlignment = Alignment.Start,
                    verticalArrangement = Arrangement.Center
                ) {
            Text(
                text = title,
                        fontSize = 26.sp,
                fontWeight = FontWeight.Bold,
                        color = if (isSelected) Color.White else Color(0xFF1F2937),
                        textAlign = TextAlign.Start
            )
            
                    Spacer(modifier = Modifier.height(12.dp))
            
            Text(
                text = description,
                        fontSize = 16.sp,
                        color = if (isSelected) Color.White.copy(alpha = 0.9f) else Color(0xFF6B7280),
                        textAlign = TextAlign.Start,
                        lineHeight = 22.sp
                    )
                }
            }
            
            // Selection indicator
            if (isSelected) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(16.dp)
                        .size(32.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(Color.White.copy(alpha = 0.3f)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "✓",
                        fontSize = 20.sp,
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

