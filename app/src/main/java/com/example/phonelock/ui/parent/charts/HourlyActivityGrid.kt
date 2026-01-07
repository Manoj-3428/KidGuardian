package com.example.phonelock.ui.parent.charts

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun HourlyActivityGrid(hourlyStats: Map<Int, Int>) {
    val maxCount = hourlyStats.values.maxOrNull() ?: 1
    
    Column {
        Text(
            text = "Activity levels throughout the day",
            fontSize = 13.sp,
            color = Color(0xFF6B7280),
            modifier = Modifier.padding(bottom = 12.dp)
        )
        
        for (row in 0..3) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                for (col in 0..5) {
                    val hour = row * 6 + col
                    val count = hourlyStats[hour] ?: 0
                    val intensity = if (maxCount > 0) count.toFloat() / maxCount.toFloat() else 0f
                    
                    Column(
                        modifier = Modifier.weight(1f),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(
                                    when {
                                        intensity >= 0.7f -> Color(0xFFDC2626)
                                        intensity >= 0.4f -> Color(0xFFF59E0B)
                                        intensity > 0f -> Color(0xFFFBBF24)
                                        else -> Color(0xFFF3F4F6)
                                    }
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            if (count > 0) {
                                Text(
                                    text = count.toString(),
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (intensity > 0.4f) Color.White else Color(0xFF1F2937)
                                )
                            }
                        }
                        
                        Spacer(modifier = Modifier.height(4.dp))
                        
                        Text(
                            text = "${hour}h",
                            fontSize = 9.sp,
                            color = Color(0xFF9CA3AF)
                        )
                    }
                }
            }
            
            if (row < 3) {
                Spacer(modifier = Modifier.height(8.dp))
            }
        }
        
        Spacer(modifier = Modifier.height(12.dp))
        
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            LegendItem("High", Color(0xFFDC2626))
            Spacer(modifier = Modifier.width(12.dp))
            LegendItem("Medium", Color(0xFFF59E0B))
            Spacer(modifier = Modifier.width(12.dp))
            LegendItem("Low", Color(0xFFFBBF24))
            Spacer(modifier = Modifier.width(12.dp))
            LegendItem("None", Color(0xFFF3F4F6))
        }
    }
}

@Composable
fun LegendItem(label: String, color: Color) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(12.dp)
                .clip(RoundedCornerShape(3.dp))
                .background(color)
        )
        Spacer(modifier = Modifier.width(4.dp))
        Text(
            text = label,
            fontSize = 10.sp,
            color = Color(0xFF6B7280)
        )
    }
}

