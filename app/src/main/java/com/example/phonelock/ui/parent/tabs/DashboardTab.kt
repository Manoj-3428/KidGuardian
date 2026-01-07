package com.example.phonelock.ui.parent.tabs

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.phonelock.models.Child
import com.example.phonelock.models.Complaint
import com.example.phonelock.models.Parent
import com.example.phonelock.ui.parent.components.LogoutOTPCard
import com.example.phonelock.ui.parent.components.ModernComplaintsCard
import com.example.phonelock.ui.parent.components.ModernStatCard
import com.example.phonelock.ui.parent.dialog.ComplaintDetailDialog
import com.example.phonelock.viewmodel.ParentDashboardViewModel

@Composable
fun DashboardTab(
    parent: Parent,
    child: Child,
    complaints: List<Complaint>,
    monitoringEnabled: Boolean,
    viewModel: ParentDashboardViewModel
) {
    var selectedComplaint by remember { mutableStateOf<Complaint?>(null) }
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            ModernStatCard(
                title = "Total Detections",
                value = complaints.size.toString(),
                icon = null,
                color = Color(0xFF3B82F6),
                modifier = Modifier.weight(1f)
            )
            
            ModernStatCard(
                title = "Active Locks",
                value = complaints.count { !it.accessed }.toString(),
                icon = null,
                color = Color(0xFFEF4444),
                modifier = Modifier.weight(1f)
            )
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .shadow(12.dp, RoundedCornerShape(24.dp)),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(60.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .background(
                                Brush.radialGradient(
                                    colors = listOf(
                                        Color(0xFF10B981),
                                        Color(0xFF059669)
                                    )
                                )
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Person,
                            contentDescription = "Child",
                            tint = Color.White,
                            modifier = Modifier.size(28.dp)
                        )
                    }
                    
                    Spacer(modifier = Modifier.width(16.dp))
                    
                    Column {
                        Text(
                            text = "Monitoring Child",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium,
                            color = Color(0xFF6B7280)
                        )
                        Text(
                            text = child.name,
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF1F2937)
                        )
                        Text(
                            text = "${child.age} years old • ${child.gender}",
                            fontSize = 14.sp,
                            color = Color(0xFF6B7280)
                        )
                    }
                }
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        if (child.logoutPasscode != null) {
            LogoutOTPCard(child)
            Spacer(modifier = Modifier.height(16.dp))
        }
        
        if (complaints.isNotEmpty()) {
            ModernComplaintsCard(
                complaints.sortedByDescending { it.timestamp },
                onComplaintClick = { complaint ->
                    selectedComplaint = complaint
                }
            )
        }
    }
    
    // Show detail dialog when a complaint is selected
    selectedComplaint?.let { complaint ->
        ComplaintDetailDialog(
            complaint = complaint,
            onDismiss = { selectedComplaint = null }
        )
    }
}

