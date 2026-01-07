package com.example.phonelock.ui.parent.tabs

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.phonelock.models.Child
import com.example.phonelock.models.Complaint
import com.example.phonelock.ui.parent.components.ComplaintCard
import com.example.phonelock.ui.parent.dialog.ComplaintDetailDialog

@Composable
fun ActivityTab(complaints: List<Complaint>, child: Child) {
    val sortedComplaints = complaints.sortedByDescending { it.timestamp }
    var selectedComplaint by remember { mutableStateOf<Complaint?>(null) }
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 8.dp, vertical = 12.dp)
    ) {
        if (sortedComplaints.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = "No activity",
                        modifier = Modifier.size(64.dp),
                        tint = Color(0xFF10B981)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "No detections yet!",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF1F2937)
                    )
                    Text(
                        text = "${child.name} is browsing safely",
                        fontSize = 14.sp,
                        color = Color.Gray
                    )
                }
            }
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(sortedComplaints) { complaint ->
                    ComplaintCard(
                        complaint = complaint,
                        onClick = { selectedComplaint = it }
                    )
                }
            }
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

