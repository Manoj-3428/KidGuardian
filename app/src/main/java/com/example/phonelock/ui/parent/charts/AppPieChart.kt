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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.phonelock.ui.parent.models.SliceData

@Composable
fun AppPieChart(appCategories: Map<String, Int>, total: Int) {
    val categoryColors = mapOf(
        "WhatsApp" to Color(0xFF25D366),
        "Instagram" to Color(0xFFE4405F),
        "Chrome" to Color(0xFF4285F4),
        "Facebook" to Color(0xFF1877F2),
        "LinkedIn" to Color(0xFF0077B5),
        "Others" to Color(0xFF6B7280)
    )
    
    val slices = appCategories.map { (category, count) ->
        val percentage = if (total > 0) (count.toFloat() / total.toFloat()) * 100f else 0f
        val color = categoryColors[category] ?: Color(0xFF8B5CF6)
        SliceData(category, count, percentage, color)
    }.sortedByDescending { it.percentage }
    
    Column(modifier = Modifier.fillMaxWidth()) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 16.dp),
            contentAlignment = Alignment.Center
        ) {
            CustomPieChartWithLabels(
                slices = slices,
                modifier = Modifier.size(220.dp)
            )
        }
        
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            appCategories.forEach { (category, count) ->
                val percentage = if (total > 0) (count.toFloat() / total.toFloat()) * 100f else 0f
                val color = categoryColors[category] ?: Color(0xFF8B5CF6)
                Column(
                    modifier = Modifier.padding(horizontal = 2.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(14.dp)
                            .clip(RoundedCornerShape(3.dp))
                            .background(color)
                    )
                    Text(
                        text = category,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFF1F2937),
                        textAlign = TextAlign.Center,
                        maxLines = 1,
                        minLines = 1
                    )
                    Text(
                        text = "${percentage.toInt()}%",
                        fontSize = 9.sp,
                        color = Color(0xFF6B7280),
                        textAlign = TextAlign.Center,
                        maxLines = 1
                    )
                }
            }
        }
    }
}

