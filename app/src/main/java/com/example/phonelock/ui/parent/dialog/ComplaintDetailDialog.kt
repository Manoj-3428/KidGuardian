package com.example.phonelock.ui.parent.dialog

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import coil.compose.AsyncImage
import com.example.phonelock.R
import com.example.phonelock.models.Complaint
import com.example.phonelock.utils.getAppDisplayName
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun ComplaintDetailDialog(
    complaint: Complaint,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val dateFormat = SimpleDateFormat("MMM dd, yyyy 'at' HH:mm", Locale.getDefault())
    val dateStr = dateFormat.format(Date(complaint.timestamp))
    
    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .fillMaxHeight(0.9f),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
            ) {
                // Header with close button
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Complaint Details",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF1F2937)
                    )
                    IconButton(onClick = onDismiss) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close",
                            tint = Color(0xFF6B7280)
                        )
                    }
                }
                
                HorizontalDivider(color = Color(0xFFE5E7EB))
                
                // Full-size image
                if (!complaint.screenshotUrl.isNullOrEmpty()) {
                    if (complaint.screenshotUrl == "screenshot_placeholder") {
                        Image(
                            painter = painterResource(id = R.drawable.placeholder),
                            contentDescription = "Screenshot placeholder",
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(400.dp)
                                .clip(RoundedCornerShape(0.dp)),
                            contentScale = ContentScale.Fit
                        )
                    } else {
                        AsyncImage(
                            model = complaint.screenshotUrl,
                            contentDescription = "Screenshot",
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(400.dp)
                                .clip(RoundedCornerShape(0.dp)),
                            contentScale = ContentScale.Fit
                        )
                    }
                } else {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(400.dp)
                            .background(Color(0xFFF3F4F6)),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Warning,
                                contentDescription = "No screenshot",
                                modifier = Modifier.size(64.dp),
                                tint = Color(0xFF9CA3AF)
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = "No screenshot available",
                                fontSize = 16.sp,
                                color = Color(0xFF6B7280)
                            )
                        }
                    }
                }
                
                // Details section
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Detected Word
                    DetailRow(
                        icon = Icons.Default.Warning,
                        iconColor = Color(0xFFEF4444),
                        label = "Detected Word",
                        value = "\"${complaint.detectedWord}\"",
                        valueColor = Color(0xFFEF4444),
                        valueFontWeight = FontWeight.Bold
                    )
                    
                    // App Name
                    DetailRow(
                        icon = Icons.Default.Phone,
                        iconColor = Color(0xFF3B82F6),
                        label = "Application",
                        value = getAppDisplayName(context, complaint.appName)
                    )
                    
                    // Category
                    if (complaint.category.isNotEmpty()) {
                        DetailRowWithPainter(
                            iconPainter = painterResource(id = R.drawable.ic_tag),
                            iconColor = Color(0xFF8B5CF6),
                            label = "Category",
                            value = complaint.category.replace("_", " ").replaceFirstChar { 
                                if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() 
                            }
                        )
                    }
                    
                    // Timestamp
                    DetailRowWithPainter(
                        iconPainter = painterResource(id = R.drawable.ic_watch),
                        iconColor = Color(0xFF6B7280),
                        label = "Detected At",
                        value = dateStr
                    )
                    
                    // Status
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = if (complaint.accessed) Icons.Default.CheckCircle else Icons.Default.Lock,
                                contentDescription = "Status",
                                tint = if (complaint.accessed) Color(0xFF10B981) else Color(0xFFEF4444),
                                modifier = Modifier.size(20.dp)
                            )
                            Text(
                                text = "Status",
                                fontSize = 14.sp,
                                color = Color(0xFF6B7280),
                                fontWeight = FontWeight.Medium
                            )
                        }
                        
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(
                                    if (complaint.accessed) Color(0xFFD1FAE5) else Color(0xFFFEE2E2)
                                )
                                .padding(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            Text(
                                text = if (complaint.accessed) "✓ Unlocked" else "🔒 Locked",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (complaint.accessed) Color(0xFF065F46) else Color(0xFF991B1B)
                            )
                        }
                    }
                    
                    // Unlock Code (if locked)
                    if (!complaint.accessed) {
                        HorizontalDivider(color = Color(0xFFE5E7EB))
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(Color(0xFFF0F9FF))
                                .padding(16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "Unlock Code",
                                fontSize = 12.sp,
                                color = Color(0xFF6B7280),
                                fontWeight = FontWeight.Medium
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = complaint.secretCode,
                                fontSize = 32.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF3B82F6),
                                letterSpacing = 4.sp
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DetailRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    iconColor: Color,
    label: String,
    value: String,
    valueColor: Color = Color(0xFF1F2937),
    valueFontWeight: FontWeight = FontWeight.Normal
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.weight(1f)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = iconColor,
                modifier = Modifier.size(20.dp)
            )
            Text(
                text = label,
                fontSize = 14.sp,
                color = Color(0xFF6B7280),
                fontWeight = FontWeight.Medium
            )
        }
        Text(
            text = value,
            fontSize = 14.sp,
            color = valueColor,
            fontWeight = valueFontWeight,
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun DetailRowWithPainter(
    iconPainter: androidx.compose.ui.graphics.painter.Painter,
    iconColor: Color,
    label: String,
    value: String,
    valueColor: Color = Color(0xFF1F2937),
    valueFontWeight: FontWeight = FontWeight.Normal
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.weight(1f)
        ) {
            Icon(
                painter = iconPainter,
                contentDescription = label,
                tint = iconColor,
                modifier = Modifier.size(20.dp)
            )
            Text(
                text = label,
                fontSize = 14.sp,
                color = Color(0xFF6B7280),
                fontWeight = FontWeight.Medium
            )
        }
        Text(
            text = value,
            fontSize = 14.sp,
            color = valueColor,
            fontWeight = valueFontWeight,
            modifier = Modifier.weight(1f)
        )
    }
}

