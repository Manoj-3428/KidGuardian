package com.example.phonelock.ui.parent.charts

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.util.Locale

@Composable
fun CategoryPieChart(categoryStats: Map<String, Int>, total: Int) {
    val categoryColors = mapOf(
        "offensive" to Color(0xFFEF4444),
        "violence" to Color(0xFFDC2626),
        "sexual" to Color(0xFFEC4899),
        "harassment" to Color(0xFFF59E0B),
        "other" to Color(0xFF6B7280)
    )
    
    val data = categoryStats.map { (category, count) ->
        val percentage = if (total > 0) count.toFloat() / total.toFloat() else 0f
        val color = categoryColors[category.lowercase()] ?: Color(0xFF8B5CF6)
        Triple(category, percentage, color)
    }.sortedByDescending { it.second }
    
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        data.forEach { (category, percentage, color) ->
            if (percentage > 0) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .shadow(2.dp, RoundedCornerShape(12.dp)),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                modifier = Modifier.weight(1f),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(12.dp)
                                        .clip(RoundedCornerShape(3.dp))
                                        .background(color)
                                )
                                
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = category.replace("_", " ")
                                            .split(" ")
                                            .joinToString(" ") { word ->
                                                word.replaceFirstChar { 
                                                    if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() 
                                                }
                                            },
                                        fontSize = 15.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF1F2937)
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = "${categoryStats[category]} detection${if ((categoryStats[category] ?: 0) != 1) "s" else ""}",
                                        fontSize = 12.sp,
                                        color = Color(0xFF6B7280)
                                    )
                                }
                            }
                            
                            Text(
                                text = "${(percentage * 100).toInt()}%",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = color
                            )
                        }
                    }
                }
            }
        }
    }
}

