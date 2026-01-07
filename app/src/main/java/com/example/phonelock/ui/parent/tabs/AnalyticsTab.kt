package com.example.phonelock.ui.parent.tabs

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
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.phonelock.models.Complaint
import com.example.phonelock.ui.parent.charts.AppPieChart
import com.example.phonelock.ui.parent.charts.DailyBarGraph
import com.example.phonelock.ui.parent.charts.HourlyActivityGrid
import com.example.phonelock.ui.parent.components.EnhancedCategoryBar
import com.example.phonelock.ui.parent.components.HorizontalStatBox
import com.example.phonelock.ui.parent.components.WordFrequencyItem
import com.example.phonelock.utils.groupAppsIntoCategories
import com.example.phonelock.viewmodel.ParentDashboardViewModel

@Composable
fun AnalyticsTab(viewModel: ParentDashboardViewModel, complaints: List<Complaint>) {
    val context = LocalContext.current
    val categoryStats = viewModel.getCategoryStats()
    val (accessed, total) = viewModel.getAccessedCount()
    val mostFrequentWords = viewModel.getMostFrequentWords()
    val appCategories = groupAppsIntoCategories(viewModel.getMostProblematicApps(), context)
    val dailyStats = viewModel.getDailyStats()
    val hourlyStats = viewModel.getHourlyStats()
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .shadow(6.dp, RoundedCornerShape(16.dp)),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                HorizontalStatBox(
                    title = "Total Detections",
                    value = total.toString(),
                    color = Color(0xFF3B82F6),
                    icon = Icons.Default.Lock
                )
                
                HorizontalStatBox(
                    title = "Resolved",
                    value = accessed.toString(),
                    color = Color(0xFF10B981),
                    icon = Icons.Default.CheckCircle
                )
                
                HorizontalStatBox(
                    title = "Active",
                    value = (total - accessed).toString(),
                    color = Color(0xFFEF4444),
                    icon = Icons.Default.Warning
                )
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        if (categoryStats.isNotEmpty()) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .shadow(6.dp, RoundedCornerShape(16.dp)),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.List,
                            contentDescription = "Categories",
                            tint = Color(0xFF3B82F6),
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "Category Breakdown",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF1F2937)
                        )
                    }
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    categoryStats.forEach { (category, count) ->
                        EnhancedCategoryBar(category, count, total)
                        Spacer(modifier = Modifier.height(12.dp))
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
        }
        
        if (mostFrequentWords.isNotEmpty()) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .shadow(6.dp, RoundedCornerShape(16.dp)),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Warning,
                            contentDescription = "Words",
                            tint = Color(0xFFEF4444),
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "Most Frequent Words",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF1F2937)
                        )
                    }
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    mostFrequentWords.forEachIndexed { index, (word, count) ->
                        WordFrequencyItem(word, count, index + 1, total)
                        if (index < mostFrequentWords.size - 1) {
                            Spacer(modifier = Modifier.height(10.dp))
                        }
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
        }
        
        if (appCategories.isNotEmpty()) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .shadow(6.dp, RoundedCornerShape(16.dp)),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = "Apps",
                            tint = Color(0xFFF59E0B),
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "Most Problematic Apps",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF1F2937)
                        )
                    }
                    
                    Spacer(modifier = Modifier.height(20.dp))
                    
                    AppPieChart(appCategories, total)
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
        }
        
        if (dailyStats.isNotEmpty()) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .shadow(6.dp, RoundedCornerShape(16.dp)),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = "Daily",
                            tint = Color(0xFF3B82F6),
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "7-Day Activity Trend",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF1F2937)
                        )
                    }
                    
                    Spacer(modifier = Modifier.height(20.dp))
                    
                    DailyBarGraph(dailyStats)
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
        }
        
        if (hourlyStats.isNotEmpty()) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .shadow(6.dp, RoundedCornerShape(16.dp)),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = "Hourly",
                            tint = Color(0xFF8B5CF6),
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "Peak Activity Hours",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF1F2937)
                        )
                    }
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    HourlyActivityGrid(hourlyStats)
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
        }
        
        Spacer(modifier = Modifier.height(20.dp))
    }
}

