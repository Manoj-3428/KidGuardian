package com.example.phonelock.ui.parent.components

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
import java.util.Locale

@Composable
fun EnhancedCategoryBar(category: String, count: Int, total: Int) {
    val percentage = if (total > 0) (count.toFloat() / total.toFloat()) else 0f
    
    val barColor = when {
        category.contains("offensive") -> Color(0xFFEF4444)
        category.contains("violence") -> Color(0xFFDC2626)
        category.contains("sexual") -> Color(0xFFEC4899)
        category.contains("substance") -> Color(0xFFF59E0B)
        category.contains("bullying") -> Color(0xFF8B5CF6)
        else -> Color(0xFF3B82F6)
    }
    
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(modifier = Modifier.weight(0.3f)) {
            Text(
                text = category.replace("_", " ").replaceFirstChar { 
                    if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() 
                },
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
                color = Color(0xFF1F2937)
            )
            Text(
                text = "$count detections",
                fontSize = 11.sp,
                color = Color.Gray
            )
        }
        
        Column(modifier = Modifier.weight(0.7f)) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(24.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(barColor.copy(alpha = 0.1f))
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(percentage)
                        .fillMaxHeight()
                        .clip(RoundedCornerShape(12.dp))
                        .background(barColor)
                )
                
                Text(
                    text = "${(percentage * 100).toInt()}%",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (percentage > 0.3f) Color.White else barColor,
                    modifier = Modifier
                        .align(Alignment.CenterEnd)
                        .padding(end = 8.dp)
                )
            }
        }
    }
}

