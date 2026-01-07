package com.example.phonelock.ui.parent.charts

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun DailyBarGraph(dailyStats: List<Pair<String, Int>>) {
    val maxCount = dailyStats.maxOfOrNull { it.second } ?: 1
    val barMaxHeight = 140.dp
    val spacing = 4.dp
    val dayLabelHeight = 20.dp
    
    Column(modifier = Modifier.fillMaxWidth()) {
        // Bars with counts positioned directly above each bar
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(barMaxHeight + 24.dp) // Extra space for count labels
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.BottomCenter),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                dailyStats.forEach { (_, count) ->
                    val heightFraction = if (maxCount > 0) count.toFloat() / maxCount.toFloat() else 0f
                    val barHeight = barMaxHeight * heightFraction
                    
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight(),
                        contentAlignment = Alignment.BottomCenter
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Bottom,
                            modifier = Modifier.fillMaxHeight()
                        ) {
                            // Count label positioned immediately above the bar
                            if (count > 0) {
                                Text(
                                    text = count.toString(),
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF3B82F6),
                                    modifier = Modifier.padding(bottom = 2.dp)
                                )
                            }
                            
                            // Bar
                            if (count > 0) {
                                Box(
                                    modifier = Modifier
                                        .width(32.dp)
                                        .height(barHeight)
                                        .clip(RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp))
                                        .background(
                                            Brush.verticalGradient(
                                                colors = listOf(
                                                    Color(0xFF3B82F6),
                                                    Color(0xFF1D4ED8)
                                                )
                                            )
                                        )
                                )
                            } else {
                                Spacer(modifier = Modifier.height(barHeight))
                            }
                        }
                    }
                }
            }
        }
        
        Spacer(modifier = Modifier.height(spacing))
        
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(dayLabelHeight)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                dailyStats.forEach { (day, _) ->
                    Box(
                        modifier = Modifier.weight(1f),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = day,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium,
                            color = Color(0xFF6B7280),
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        }
    }
}

