package com.example.phonelock.ui.child.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.phonelock.models.Child
import com.example.phonelock.repository.UserRepository
import kotlinx.coroutines.delay

@Composable
fun ParentLinkStatusCard(child: Child) {
    fun isPasscodeExpired(generatedAt: Long?, currentTime: Long): Boolean {
        return if (generatedAt == null) {
            true
        } else {
            (currentTime - generatedAt) > UserRepository.PASSCODE_EXPIRY_MS
        }
    }
    
    var currentTime by remember { mutableLongStateOf(System.currentTimeMillis()) }
    
    LaunchedEffect(child.passcodeGeneratedAt) {
        while (child.passcodeGeneratedAt != null && !isPasscodeExpired(child.passcodeGeneratedAt, currentTime)) {
            delay(1000)
            currentTime = System.currentTimeMillis()
        }
    }
    
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
                child.parentLinked -> Color(0xFFDCFCE7)
                isPasscodeActive -> Color(0xFFDEF7EC)
                else -> Color(0xFFFEF3C7)
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

