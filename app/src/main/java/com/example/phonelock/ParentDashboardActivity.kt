package com.example.phonelock

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.foundation.Canvas
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Offset
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.PI
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.phonelock.models.Child
import com.example.phonelock.models.Complaint
import com.example.phonelock.models.Parent
import com.example.phonelock.repository.UserRepository
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*
import coil.compose.AsyncImage
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import android.util.Log
import android.content.pm.PackageManager
import co.yml.charts.ui.piechart.charts.PieChart
import co.yml.charts.ui.piechart.models.PieChartData
import co.yml.charts.ui.piechart.models.PieChartConfig
import co.yml.charts.common.model.PlotType
import java.util.Locale

class ParentDashboardActivity : ComponentActivity() {
    private val viewModel: ParentDashboardViewModel by viewModels()
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        val repository = UserRepository(this)
        viewModel.setRepository(repository)
        
        setContent {
            MaterialTheme {
                ParentDashboardScreen(
                    viewModel = viewModel,
                    onLogout = {
                        repository.logout()
                        startActivity(Intent(this, RoleSelectionActivity::class.java))
                        finish()
                    }
                )
            }
        }
    }
}

class ParentDashboardViewModel : ViewModel() {
    private lateinit var repository: UserRepository
    
    private val _parentData = mutableStateOf<Parent?>(null)
    val parentData: State<Parent?> = _parentData
    
    private val _childData = mutableStateOf<Child?>(null)
    val childData: State<Child?> = _childData
    
    private val _complaints = mutableStateOf<List<Complaint>>(emptyList())
    val complaints: State<List<Complaint>> = _complaints
    
    private val _isLoading = mutableStateOf(true)
    val isLoading: State<Boolean> = _isLoading
    
    private val _errorMessage = mutableStateOf("")
    val errorMessage: State<String> = _errorMessage
    
    private val _monitoringEnabled = mutableStateOf(true)
    val monitoringEnabled: State<Boolean> = _monitoringEnabled
    
    fun setRepository(repo: UserRepository) {
        repository = repo
        loadData()
    }
    
    fun loadData() {
        _isLoading.value = true
        viewModelScope.launch {
            // Load parent data
            val parentResult = repository.getParentData()
            if (parentResult.isSuccess) {
                val parent = parentResult.getOrNull()
                _parentData.value = parent
                _monitoringEnabled.value = parent?.monitoringEnabled ?: true
                
                // Load child data
                parent?.linkedChildId?.let { childId ->
                    val childResult = repository.getChildByChildId(childId)
                    if (childResult.isSuccess) {
                        _childData.value = childResult.getOrNull()
                        
                        // Load complaints
                        val complaintsResult = repository.getComplaintsForChild(childId)
                        if (complaintsResult.isSuccess) {
                            _complaints.value = complaintsResult.getOrNull() ?: emptyList()
                        }
                    }
                }
            } else {
                _errorMessage.value = "Failed to load data"
            }
            _isLoading.value = false
        }
    }
    
    fun refreshDataSilently() {
        // Refresh without showing loader
        viewModelScope.launch {
            // Load parent data
            val parentResult = repository.getParentData()
            if (parentResult.isSuccess) {
                val parent = parentResult.getOrNull()
                _parentData.value = parent
                _monitoringEnabled.value = parent?.monitoringEnabled ?: true
                
                // Load child data
                parent?.linkedChildId?.let { childId ->
                    val childResult = repository.getChildByChildId(childId)
                    if (childResult.isSuccess) {
                        _childData.value = childResult.getOrNull()
                        
                        // Load complaints
                        val complaintsResult = repository.getComplaintsForChild(childId)
                        if (complaintsResult.isSuccess) {
                            _complaints.value = complaintsResult.getOrNull() ?: emptyList()
                        }
                    }
                }
            }
        }
    }
    
    fun toggleMonitoring(enabled: Boolean) {
        viewModelScope.launch {
            val result = repository.updateMonitoringStatus(enabled)
            if (result.isSuccess) {
                _monitoringEnabled.value = enabled
            }
        }
    }
    
    fun refreshData() {
        loadData()
    }
    
    fun getCategoryStats(): Map<String, Int> {
        val stats = mutableMapOf<String, Int>()
        _complaints.value.forEach { complaint ->
            val category = complaint.category.ifEmpty { "Other" }
            stats[category] = (stats[category] ?: 0) + 1
        }
        return stats
    }
    
    fun getAccessedCount(): Pair<Int, Int> {
        val accessed = _complaints.value.count { it.accessed }
        val total = _complaints.value.size
        return Pair(accessed, total)
    }
    
    fun getMostFrequentWords(): List<Pair<String, Int>> {
        val wordCounts = mutableMapOf<String, Int>()
        _complaints.value.forEach { complaint ->
            val word = complaint.detectedWord
            wordCounts[word] = (wordCounts[word] ?: 0) + 1
        }
        return wordCounts.toList().sortedByDescending { it.second }.take(5)
    }
    
    fun getMostProblematicApps(): List<Pair<String, Int>> {
        val appCounts = mutableMapOf<String, Int>()
        _complaints.value.forEach { complaint ->
            // Use full package name for grouping to avoid conflicts
            val packageName = complaint.appName
            appCounts[packageName] = (appCounts[packageName] ?: 0) + 1
        }
        return appCounts.toList().sortedByDescending { it.second }.take(5)
    }
    
    fun getTodayDetections(): Int {
        val todayStart = java.util.Calendar.getInstance().apply {
            set(java.util.Calendar.HOUR_OF_DAY, 0)
            set(java.util.Calendar.MINUTE, 0)
            set(java.util.Calendar.SECOND, 0)
        }.timeInMillis
        
        return _complaints.value.count { it.timestamp >= todayStart }
    }
    
    fun getWeekDetections(): Int {
        val weekStart = System.currentTimeMillis() - (7 * 24 * 60 * 60 * 1000)
        return _complaints.value.count { it.timestamp >= weekStart }
    }
    
    fun getDailyStats(): List<Pair<String, Int>> {
        val calendar = java.util.Calendar.getInstance()
        val dailyStats = mutableListOf<Pair<String, Int>>()
        val dayFormatter = java.text.SimpleDateFormat("EEE", java.util.Locale.getDefault())
        
        // Get stats for last 7 days
        for (i in 6 downTo 0) {
            calendar.timeInMillis = System.currentTimeMillis()
            calendar.add(java.util.Calendar.DAY_OF_YEAR, -i)
            
            val dayStart = calendar.apply {
                set(java.util.Calendar.HOUR_OF_DAY, 0)
                set(java.util.Calendar.MINUTE, 0)
                set(java.util.Calendar.SECOND, 0)
            }.timeInMillis
            
            val dayEnd = calendar.apply {
                set(java.util.Calendar.HOUR_OF_DAY, 23)
                set(java.util.Calendar.MINUTE, 59)
                set(java.util.Calendar.SECOND, 59)
            }.timeInMillis
            
            val count = _complaints.value.count { it.timestamp in dayStart..dayEnd }
            val dayName = dayFormatter.format(calendar.time)
            
            dailyStats.add(Pair(dayName, count))
        }
        
        return dailyStats
    }
    
    fun getHourlyStats(): Map<Int, Int> {
        val hourlyStats = mutableMapOf<Int, Int>()
        
        _complaints.value.forEach { complaint ->
            val calendar = java.util.Calendar.getInstance()
            calendar.timeInMillis = complaint.timestamp
            val hour = calendar.get(java.util.Calendar.HOUR_OF_DAY)
            hourlyStats[hour] = (hourlyStats[hour] ?: 0) + 1
        }
        
        return hourlyStats
    }
    
    fun getStatusBreakdown(): Map<String, Int> {
        return mapOf(
            "Locked" to _complaints.value.count { !it.accessed },
            "Unlocked" to _complaints.value.count { it.accessed }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ParentDashboardScreen(
    viewModel: ParentDashboardViewModel,
    onLogout: () -> Unit
) {
    val context = LocalContext.current
    val parentData = viewModel.parentData.value
    val childData = viewModel.childData.value
    val complaints = viewModel.complaints.value
    val isLoading = viewModel.isLoading.value
    val monitoringEnabled = viewModel.monitoringEnabled.value
    
    // Continuously refresh parent dashboard data every 5 seconds without showing loader
    LaunchedEffect(Unit) {
        while (true) {
            kotlinx.coroutines.delay(5000) // Refresh every 5 seconds
            viewModel.refreshDataSilently()
        }
    }
    
    var selectedTab by remember { mutableIntStateOf(0) }
    val tabs = listOf("Dashboard", "Activity", "Analytics")
    
    // Manual refresh only - user must click refresh button
    // Auto-refresh removed as per user request
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Parent Dashboard", fontWeight = FontWeight.Bold) },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(0xFF3B82F6),
                    titleContentColor = Color.White,
                    actionIconContentColor = Color.White
                ),
                actions = {
                    IconButton(onClick = { viewModel.refreshData() }) {
                        Icon(Icons.Default.Refresh, "Refresh")
                    }
                    IconButton(onClick = onLogout) {
                        Icon(Icons.AutoMirrored.Filled.ExitToApp, "Logout")
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(
                    Brush.linearGradient(
                        colors = listOf(
                            Color(0xFFEFF6FF),
                            Color(0xFFDBEAFE),
                            Color(0xFFBFDBFE)
                        )
                    )
                )
        ) {
            if (isLoading) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = Color(0xFF3B82F6))
                }
            } else if (parentData != null && childData != null) {
                // Tab Row
                TabRow(
                    selectedTabIndex = selectedTab,
                    containerColor = Color.White,
                    contentColor = Color(0xFF3B82F6)
                ) {
                    tabs.forEachIndexed { index, title ->
                        Tab(
                            selected = selectedTab == index,
                            onClick = { selectedTab = index },
                            text = { Text(title, fontWeight = FontWeight.Medium) }
                        )
                    }
                }
                
                // Tab Content
                when (selectedTab) {
                    0 -> DashboardTab(parentData, childData, complaints, monitoringEnabled, viewModel)
                    1 -> ActivityTab(complaints, childData)
                    2 -> AnalyticsTab(viewModel, complaints)
                }
            } else {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = "No data",
                            modifier = Modifier.size(48.dp),
                            tint = Color(0xFF3B82F6)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = if (childData?.name?.isNotEmpty() == true) {
                                "No complaints from ${childData?.name}"
                            } else {
                                "No complaints yet"
                            },
                            color = Color(0xFF3B82F6),
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Medium
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Monitoring is active and ready",
                            color = Color.Gray,
                            fontSize = 14.sp
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun DashboardTab(
    parent: Parent,
    child: Child,
    complaints: List<Complaint>,
    monitoringEnabled: Boolean,
    viewModel: ParentDashboardViewModel
) {
    val context = LocalContext.current
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        // Modern Stats Cards Row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Total Detections Card
            ModernStatCard(
                title = "Total Detections",
                value = complaints.size.toString(),
                icon = null,
                color = Color(0xFF3B82F6),
                modifier = Modifier.weight(1f)
            )
            
            // Active Locks Card
            ModernStatCard(
                title = "Active Locks",
                value = complaints.count { !it.accessed }.toString(),
                icon = null,
                color = Color(0xFFEF4444),
                modifier = Modifier.weight(1f)
            )
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Child Info Card - Modern Design
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
        
        // Monitoring Status Card - Modern Design
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .shadow(12.dp, RoundedCornerShape(24.dp)),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(
                containerColor = if (monitoringEnabled) Color(0xFFF0FDF4) else Color(0xFFF9FAFB)
            )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(
                                    if (monitoringEnabled) Color(0xFF10B981) else Color(0xFF6B7280)
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = if (monitoringEnabled) Icons.Default.Lock else Icons.Default.Lock,
                                contentDescription = "Monitoring",
                                tint = Color.White,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                        
                        Spacer(modifier = Modifier.width(16.dp))
                        
                        Column {
                            Text(
                                text = "Content Monitoring",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF1F2937)
                            )
                            Text(
                                text = if (monitoringEnabled) "Active Protection" else "Disabled",
                                fontSize = 14.sp,
                                color = if (monitoringEnabled) Color(0xFF10B981) else Color(0xFF6B7280),
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                    
                    Switch(
                        checked = monitoringEnabled,
                        onCheckedChange = { enabled ->
                            viewModel.toggleMonitoring(enabled)
                            Toast.makeText(
                                context,
                                if (enabled) "Monitoring enabled" else "Monitoring disabled",
                                Toast.LENGTH_SHORT
                            ).show()
                        },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.White,
                            checkedTrackColor = Color(0xFF10B981),
                            uncheckedThumbColor = Color.White,
                            uncheckedTrackColor = Color(0xFFD1D5DB)
                        )
                    )
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                Text(
                    text = if (monitoringEnabled) {
                        "🔒 Your child's device is protected. Inappropriate content will be detected and the device will be locked automatically."
                    } else {
                        "⚠️ Monitoring is disabled. Your child can browse without restrictions."
                    },
                    fontSize = 14.sp,
                    color = Color(0xFF6B7280),
                    lineHeight = 20.sp
                )
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Logout OTP Card - Show if child has requested logout
        if (child.logoutPasscode != null) {
            LogoutOTPCard(child)
            Spacer(modifier = Modifier.height(16.dp))
        }
        
        // Recent Detections with Modern Design
        if (complaints.isNotEmpty()) {
            ModernComplaintsCard(complaints)
        }
    }
}

@Composable
fun ActivityTab(complaints: List<Complaint>, child: Child) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 8.dp, vertical = 12.dp)
    ) {
        if (complaints.isEmpty()) {
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
                items(complaints) { complaint ->
                    ComplaintCard(complaint)
                }
            }
        }
    }
}

@Composable
fun ModernStatCard(
    title: String,
    value: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector?,
    color: Color,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .height(140.dp)
            .shadow(8.dp, RoundedCornerShape(20.dp)),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Title at the top
            Text(
                text = title,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color(0xFF6B7280),
                textAlign = TextAlign.Center
            )
            
            Spacer(modifier = Modifier.height(12.dp))
            
            // Value in the center - large and prominent
            Text(
                text = value,
                fontSize = 42.sp,
                fontWeight = FontWeight.Bold,
                color = color
            )
        }
    }
}

@Composable
fun ModernComplaintsCard(complaints: List<Complaint>) {
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
                .padding(16.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(
                            Brush.radialGradient(
                                colors = listOf(
                                    Color(0xFF3B82F6),
                                    Color(0xFF1D4ED8)
                                )
                            )
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Lock,
                        contentDescription = "Detections",
                        tint = Color.White,
                        modifier = Modifier.size(24.dp)
                    )
                }
                
                Spacer(modifier = Modifier.width(16.dp))
                
                Column {
                    Text(
                        text = "Recent Detections",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF1F2937)
                    )
                    Text(
                        text = "Latest security alerts",
                        fontSize = 14.sp,
                        color = Color(0xFF6B7280)
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            complaints.take(3).forEach { complaint ->
                ModernComplaintItem(complaint)
                Spacer(modifier = Modifier.height(12.dp))
            }
            
            if (complaints.size > 3) {
                Text(
                    text = "View all in Activity tab →",
                    fontSize = 14.sp,
                    color = Color(0xFF3B82F6),
                    fontWeight = FontWeight.Medium,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
} 

@Composable
fun ModernComplaintItem(complaint: Complaint) {
    val context = LocalContext.current
    val dateFormat = SimpleDateFormat("MMM dd, HH:mm", Locale.getDefault())
    val dateStr = dateFormat.format(Date(complaint.timestamp))
    
    // Debug: Log screenshot URL
    Log.d("ParentDashboard", "ModernComplaintItem - Screenshot URL: ${complaint.screenshotUrl}")
    
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = Color.White
        ),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth()
        ) {
            // Screenshot as main image (large)
            if (!complaint.screenshotUrl.isNullOrEmpty()) {
                if (complaint.screenshotUrl == "screenshot_placeholder") {
                    // Placeholder image when screenshot is not captured
                    Image(
                        painter = painterResource(id = R.drawable.placeholder),
                        contentDescription = "Screenshot placeholder",
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(320.dp)
                            .clip(RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp)),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    // Actual screenshot
                    AsyncImage(
                        model = complaint.screenshotUrl,
                        contentDescription = "Screenshot",
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(320.dp)
                            .clip(RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp)),
                        contentScale = ContentScale.Crop
                    )
                }
            } else {
                // No screenshot available
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(300.dp)
                        .background(Color(0xFFF3F4F6)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Warning,
                        contentDescription = "No screenshot",
                        modifier = Modifier.size(48.dp),
                        tint = Color(0xFF9CA3AF)
                    )
                }
            }
            
            // Bottom section with detected word (left) and app name (right)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Bottom
            ) {
                // Left: Detected word
                Text(
                    text = "\"${complaint.detectedWord}\"",
                    fontSize = 13.sp,
                    color = Color(0xFFEF4444),
                    fontWeight = FontWeight.Bold
                )
                
                // Right: App name
                Text(
                    text = getAppDisplayName(context, complaint.appName),
                    fontSize = 11.sp,
                    color = Color(0xFF6B7280),
                    fontWeight = FontWeight.Medium
                )
            }
            
            // Status badge and unlock code (if locked)
            if (!complaint.accessed) {
                Divider(color = Color(0xFFE5E7EB))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Unlock Code",
                            fontSize = 11.sp,
                            color = Color(0xFF6B7280)
                        )
                        Text(
                            text = complaint.secretCode,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF3B82F6),
                            letterSpacing = 2.sp
                        )
                    }
                    
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color(0xFFEF4444))
                            .padding(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = "🔒 LOCKED",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                }
            } else {
                Divider(color = Color(0xFFE5E7EB))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = "Unlocked",
                            tint = Color(0xFF10B981),
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Unlocked at $dateStr",
                            fontSize = 13.sp,
                            color = Color(0xFF10B981),
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun ComplaintCard(complaint: Complaint) {
    val context = LocalContext.current
    val dateFormat = SimpleDateFormat("MMM dd, HH:mm", Locale.getDefault())
    val dateStr = dateFormat.format(Date(complaint.timestamp))
    
    // Debug: Log screenshot URL
    Log.d("ParentDashboard", "ComplaintCard - Screenshot URL: ${complaint.screenshotUrl}")
    
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = Color.White
        ),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth()
        ) {
            // Screenshot as main image (large, at top)
            if (!complaint.screenshotUrl.isNullOrEmpty()) {
                if (complaint.screenshotUrl == "screenshot_placeholder") {
                    // Placeholder image when screenshot is not captured
                    Image(
                        painter = painterResource(id = R.drawable.placeholder),
                        contentDescription = "Screenshot placeholder",
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(320.dp)
                            .clip(RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp)),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    // Actual screenshot
                    AsyncImage(
                        model = complaint.screenshotUrl,
                        contentDescription = "Screenshot",
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(320.dp)
                            .clip(RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp)),
                        contentScale = ContentScale.Crop
                    )
                }
            } else {
                // No screenshot
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(300.dp)
                        .background(Color(0xFFF3F4F6)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Warning,
                        contentDescription = "No screenshot",
                        modifier = Modifier.size(48.dp),
                        tint = Color(0xFF9CA3AF)
                    )
                }
            }
            
            // Bottom row: Detected word (left) and App name (right)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFFFAFAFA))
                    .padding(12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Left: Detected word (small text)
                Text(
                    text = "\"${complaint.detectedWord}\"",
                    fontSize = 12.sp,
                    color = Color(0xFFEF4444),
                    fontWeight = FontWeight.Bold
                )
                
                // Right: App name (small text)
                Text(
                    text = getAppDisplayName(context, complaint.appName),
                    fontSize = 11.sp,
                    color = Color(0xFF6B7280),
                    fontWeight = FontWeight.Medium
                )
            }
            
            // Unlock code section (if locked)
            if (!complaint.accessed) {
                Divider(color = Color(0xFFE5E7EB))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Unlock Code",
                            fontSize = 11.sp,
                            color = Color(0xFF6B7280)
                        )
                        Text(
                            text = complaint.secretCode,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF3B82F6),
                            letterSpacing = 2.sp
                        )
                    }
                    
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color(0xFFEF4444))
                            .padding(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = "🔒 LOCKED",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                }
            } else {
                Divider(color = Color(0xFFE5E7EB))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = "Unlocked",
                            tint = Color(0xFF10B981),
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Unlocked • $dateStr",
                            fontSize = 13.sp,
                            color = Color(0xFF10B981),
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun RecentComplaintsCard(complaints: List<Complaint>) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(8.dp, RoundedCornerShape(20.dp)),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color.White
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Lock,
                    contentDescription = "Security",
                    tint = Color(0xFF3B82F6),
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Recent Detections",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF1F2937)
                )
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            complaints.take(3).forEach { complaint ->
                ComplaintWithCodeItem(complaint)
                Spacer(modifier = Modifier.height(12.dp))
            }
            
            if (complaints.size > 3) {
                Text(
                    text = "View all in Activity tab",
                    fontSize = 12.sp,
                    color = Color(0xFF3B82F6),
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

@Composable
fun ComplaintWithCodeItem(complaint: Complaint) {
    val dateFormat = SimpleDateFormat("MMM dd, hh:mm a", Locale.getDefault())
    val dateStr = dateFormat.format(Date(complaint.timestamp))
    
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (complaint.accessed) Color(0xFFF3F4F6) else Color(0xFFFEE2E2)
        ),
        shape = RoundedCornerShape(12.dp)
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
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(RoundedCornerShape(6.dp))
                            .background(
                                if (complaint.accessed) Color(0xFFDCFCE7) else Color(0xFFFEE2E2)
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (complaint.accessed) Icons.Default.CheckCircle else Icons.Default.Warning,
                            contentDescription = "Status",
                            tint = if (complaint.accessed) Color(0xFF10B981) else Color(0xFFEF4444),
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    
                    Spacer(modifier = Modifier.width(12.dp))
                    
                    Column {
                        Text(
                            text = complaint.appName,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF1F2937)
                        )
                        Text(
                            text = dateStr,
                            fontSize = 11.sp,
                            color = Color.Gray
                        )
                    }
                }
                
                Chip(
                    text = if (complaint.accessed) "Unlocked" else "Locked",
                    color = if (complaint.accessed) Color(0xFF10B981) else Color(0xFFEF4444)
                )
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = Color(0xFFF3F4F6)
                ),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text(
                    text = "Detected: \"${complaint.detectedWord}\"",
                    fontSize = 12.sp,
                    color = Color(0xFF1F2937),
                    modifier = Modifier.padding(12.dp),
                    fontWeight = FontWeight.Medium
                )
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            // Secret Code Section
            if (!complaint.accessed) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Secret Code",
                            fontSize = 11.sp,
                            color = Color.Gray,
                            fontWeight = FontWeight.Medium
                        )
                        Text(
                            text = complaint.secretCode,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF3B82F6),
                            letterSpacing = 2.sp
                        )
                    }
                    
                    Text(
                        text = "Share with child",
                        fontSize = 10.sp,
                        color = Color(0xFF3B82F6),
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
    }
}

@Composable
fun Chip(text: String, color: Color) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .background(color.copy(alpha = 0.15f))
            .padding(horizontal = 12.dp, vertical = 6.dp)
    ) {
        Text(
            text = text,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            color = color
        )
    }
}

fun groupAppsIntoCategories(appList: List<Pair<String, Int>>, context: Context): Map<String, Int> {
    val categoryCounts = mutableMapOf<String, Int>()
    
    appList.forEach { (packageName, count) ->
        val appName = getAppDisplayName(context, packageName).lowercase()
        val category = when {
            appName.contains("whatsapp", ignoreCase = true) -> "WhatsApp"
            appName.contains("instagram", ignoreCase = true) -> "Instagram"
            appName.contains("chrome", ignoreCase = true) || packageName.contains("chrome", ignoreCase = true) -> "Chrome"
            appName.contains("facebook", ignoreCase = true) -> "Facebook"
            else -> "Others"
        }
        categoryCounts[category] = (categoryCounts[category] ?: 0) + count
    }
    
    return categoryCounts
}

fun getAppDisplayName(context: Context, packageName: String): String {
    // Common package name mappings
    val packageMappings = mapOf(
        "com.whatsapp" to "WhatsApp",
        "com.whatsapp.w4b" to "WhatsApp Business",
        "com.instagram.android" to "Instagram",
        "com.facebook.katana" to "Facebook",
        "com.twitter.android" to "Twitter",
        "com.google.android.youtube" to "YouTube",
        "com.snapchat.android" to "Snapchat",
        "com.tiktok.android" to "TikTok",
        "com.google.android.apps.messaging" to "Messages",
        "com.google.android.inputmethod.latin" to "WhatsApp", // Keyboard package - commonly used with WhatsApp
        "com.google.android.inputmethod.gboard" to "WhatsApp", // Gboard keyboard
        "com.samsung.android.honeyboard" to "WhatsApp" // Samsung keyboard
    )
    
    // Check mappings first
    packageMappings[packageName.lowercase()]?.let { return it }
    
    // Handle keyboard/input method packages - default to WhatsApp since most detections happen there
    if (packageName.contains("inputmethod", ignoreCase = true) || 
        packageName.contains("keyboard", ignoreCase = true)) {
        return "WhatsApp"
    }
    
    // Try PackageManager
    return try {
        val pm = context.packageManager
        val appInfo = pm.getApplicationInfo(packageName, 0)
        pm.getApplicationLabel(appInfo).toString()
    } catch (e: Exception) {
        // If package not found, try to extract from package name
        val lastPart = packageName.substringAfterLast('.', packageName)
        // Handle cases like "com.whatsapp.latin" -> "WhatsApp"
        if (lastPart.lowercase() == "latin" && packageName.contains("whatsapp")) {
            "WhatsApp"
        } else if (lastPart.lowercase() == "latin") {
            // Default latin to WhatsApp since it's commonly used there
            "WhatsApp"
        } else {
            lastPart.replaceFirstChar { 
                if (it.isLowerCase()) it.titlecase(java.util.Locale.getDefault()) else it.toString() 
            }
        }
    }
}

@Composable
fun AnalyticsTab(viewModel: ParentDashboardViewModel, complaints: List<Complaint>) {
    val context = LocalContext.current
    val categoryStats = viewModel.getCategoryStats()
    val (accessed, total) = viewModel.getAccessedCount()
    val mostFrequentWords = viewModel.getMostFrequentWords()
    // Group apps into categories for pie chart
    val appCategories = groupAppsIntoCategories(viewModel.getMostProblematicApps(), context)
    val dailyStats = viewModel.getDailyStats()
    val hourlyStats = viewModel.getHourlyStats()
    val statusBreakdown = viewModel.getStatusBreakdown()
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        // Behavior Summary Card
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
        
        // Category Breakdown
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
        
        // Most Frequent Words
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
        
        // Most Problematic Apps Pie Chart
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
        
        // 7-Day Activity Trend
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
        
        // Peak Activity Hours
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

@Composable
fun StatBox(title: String, value: String, color: Color, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = value,
            fontSize = 32.sp,
            fontWeight = FontWeight.Bold,
            color = color
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = title,
            fontSize = 12.sp,
            color = Color.Gray,
            textAlign = TextAlign.Center,
            maxLines = 2,
            lineHeight = 14.sp
        )
    }
}

@Composable
fun AnalyticsTimeCard(
    title: String,
    value: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    color: Color,
    gradientColors: List<Color>,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxHeight()
            .shadow(8.dp, RoundedCornerShape(16.dp)),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            gradientColors[0].copy(alpha = 0.1f),
                            gradientColors[1].copy(alpha = 0.05f)
                        )
                    )
                )
                .padding(16.dp)
        ) {
            Column(
                modifier = Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                // Time period label at top with background
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(color.copy(alpha = 0.2f))
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = title,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = color
                    )
                }
                
                Spacer(modifier = Modifier.height(8.dp))
                
                // Icon
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(color.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = title,
                        tint = color,
                        modifier = Modifier.size(24.dp)
                    )
                }
                
                Spacer(modifier = Modifier.height(8.dp))
                
                // Value
                Text(
                    text = value,
                    fontSize = 36.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF1F2937)
                )
            }
        }
    }
}

@Composable
fun AnalyticsStatBox(
    title: String,
    value: String,
    color: Color,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .shadow(4.dp, RoundedCornerShape(12.dp)),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = color.copy(alpha = 0.05f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = icon,
                contentDescription = title,
                tint = color,
                modifier = Modifier.size(28.dp)
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Text(
                text = value,
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                color = color
            )
            
            Spacer(modifier = Modifier.height(4.dp))
            
            Text(
                text = title,
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                color = Color(0xFF6B7280),
                textAlign = TextAlign.Center,
                maxLines = 2
            )
        }
    }
}

@Composable
fun HorizontalStatBox(
    title: String,
    value: String,
    color: Color,
    icon: androidx.compose.ui.graphics.vector.ImageVector
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(4.dp, RoundedCornerShape(16.dp)),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.horizontalGradient(
                        colors = listOf(
                            color.copy(alpha = 0.08f),
                            color.copy(alpha = 0.03f)
                        )
                    )
                )
                .padding(20.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Start
        ) {
            Row(
                modifier = Modifier.weight(1f),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Icon with colored background
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(
                            Brush.linearGradient(
                                colors = listOf(
                                    color.copy(alpha = 0.2f),
                                    color.copy(alpha = 0.15f)
                                )
                            )
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = title,
                        tint = color,
                        modifier = Modifier.size(28.dp)
                    )
                }
                
                // Title and Value
                Column {
                    Text(
                        text = title,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFF6B7280),
                        letterSpacing = 0.2.sp
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = value,
                        fontSize = 28.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF1F2937),
                        letterSpacing = (-0.5).sp
                    )
                }
            }
        }
    }
}

@Composable
fun CategoryBar(category: String, count: Int, total: Int) {
    val percentage = if (total > 0) (count.toFloat() / total.toFloat()) else 0f
    
    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = category,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                color = Color(0xFF1F2937)
            )
            Text(
                text = "$count (${(percentage * 100).toInt()}%)",
                fontSize = 14.sp,
                color = Color.Gray
            )
        }
        
        Spacer(modifier = Modifier.height(6.dp))
        
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(8.dp)
                .clip(RoundedCornerShape(4.dp))
                .background(Color(0xFFF3F4F6))
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(percentage)
                    .fillMaxHeight()
                    .clip(RoundedCornerShape(4.dp))
                    .background(
                        Brush.horizontalGradient(
                            colors = listOf(
                                Color(0xFF3B82F6),
                                Color(0xFF8B5CF6)
                            )
                        )
                    )
            )
        }
    }
}

@Composable
fun EnhancedCategoryBar(category: String, count: Int, total: Int) {
    val percentage = if (total > 0) (count.toFloat() / total.toFloat()) else 0f
    
    // Color based on category
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
                text = category.replace("_", " ").capitalize(),
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
                
                // Percentage text inside bar
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

@Composable
fun WordFrequencyItem(word: String, count: Int, rank: Int, total: Int) {
    val percentage = if (total > 0) (count.toFloat() / total.toFloat()) else 0f
    
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(Color(0xFFF9FAFB))
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            // Rank badge
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(
                        when (rank) {
                            1 -> Color(0xFFEF4444)
                            2 -> Color(0xFFF59E0B)
                            3 -> Color(0xFFFBBF24)
                            else -> Color(0xFF6B7280)
                        }
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "#$rank",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }
            
            Spacer(modifier = Modifier.width(12.dp))
            
            Column {
                Text(
                    text = "\"$word\"",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF1F2937)
                )
                Text(
                    text = "$count times (${(percentage * 100).toInt()}%)",
                    fontSize = 12.sp,
                    color = Color(0xFF6B7280)
                )
            }
        }
        
        // Bar visualization
        Box(
            modifier = Modifier
                .width(60.dp)
                .height(8.dp)
                .clip(RoundedCornerShape(4.dp))
                .background(Color(0xFFE5E7EB))
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(percentage)
                    .fillMaxHeight()
                    .clip(RoundedCornerShape(4.dp))
                    .background(Color(0xFFEF4444))
            )
        }
    }
}

@Composable
fun AppFrequencyItem(app: String, count: Int, rank: Int, total: Int) {
    val percentage = if (total > 0) (count.toFloat() / total.toFloat()) else 0f
    
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(Color(0xFFF9FAFB))
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            // Rank badge
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(
                        when (rank) {
                            1 -> Color(0xFFF59E0B)
                            2 -> Color(0xFFFBBF24)
                            3 -> Color(0xFF84CC16)
                            else -> Color(0xFF6B7280)
                        }
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "#$rank",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }
            
            Spacer(modifier = Modifier.width(12.dp))
            
            Column {
                Text(
                    text = app.capitalize(),
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF1F2937)
                )
                Text(
                    text = "$count detections (${(percentage * 100).toInt()}%)",
                    fontSize = 12.sp,
                    color = Color(0xFF6B7280)
                )
            }
        }
        
        // Bar visualization
        Box(
            modifier = Modifier
                .width(60.dp)
                .height(8.dp)
                .clip(RoundedCornerShape(4.dp))
                .background(Color(0xFFE5E7EB))
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(percentage)
                    .fillMaxHeight()
                    .clip(RoundedCornerShape(4.dp))
                    .background(Color(0xFFF59E0B))
            )
        }
    }
}

@Composable
fun DailyBarGraph(dailyStats: List<Pair<String, Int>>) {
    val maxCount = dailyStats.maxOfOrNull { it.second } ?: 1
    
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(200.dp),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.Bottom
    ) {
        dailyStats.forEach { (day, count) ->
            Column(
                modifier = Modifier.weight(1f),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Count label on top
                if (count > 0) {
                    Text(
                        text = count.toString(),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF3B82F6)
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                }
                
                // Bar
                val heightFraction = if (maxCount > 0) count.toFloat() / maxCount.toFloat() else 0f
                Box(
                    modifier = Modifier
                        .width(32.dp)
                        .height(160.dp * heightFraction)
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
                
                Spacer(modifier = Modifier.height(8.dp))
                
                // Day label
                Text(
                    text = day,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color(0xFF6B7280)
                )
            }
        }
    }
}

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
        
        // Create 4 rows of 6 hours each (0-23 hours)
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
                                        intensity >= 0.7f -> Color(0xFFDC2626) // High - Dark Red
                                        intensity >= 0.4f -> Color(0xFFF59E0B) // Medium - Orange
                                        intensity > 0f -> Color(0xFFFBBF24)    // Low - Yellow
                                        else -> Color(0xFFF3F4F6)              // None - Gray
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
        
        // Legend
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

@Composable
fun AppPieChart(appCategories: Map<String, Int>, total: Int) {
    val categoryColors = mapOf(
        "WhatsApp" to Color(0xFF25D366),
        "Instagram" to Color(0xFFE4405F),
        "Chrome" to Color(0xFF4285F4),
        "Facebook" to Color(0xFF1877F2),
        "Others" to Color(0xFF6B7280)
    )
    
    val slices = appCategories.map { (category, count) ->
        val percentage = if (total > 0) (count.toFloat() / total.toFloat()) * 100f else 0f
        val color = categoryColors[category] ?: Color(0xFF8B5CF6)
        // Create custom label with percentage and detection count
        val label = "${percentage.toInt()}% ($count detection${if (count != 1) "s" else ""})"
        PieChartData.Slice(label, percentage, color)
    }
    
    val pieChartConfig = PieChartConfig(
        showSliceLabels = true,
        sliceLabelTextSize = 12.sp,
        isClickOnSliceEnabled = false
    )
    val pieChartData = PieChartData(slices = slices, plotType = PlotType.Pie)
    
    Column(
        modifier = Modifier.fillMaxWidth()
    ) {
        // Pie Chart - Centered
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 16.dp),
            contentAlignment = Alignment.Center
        ) {
            PieChart(
                modifier = Modifier.size(200.dp),
                pieChartData = pieChartData,
                pieChartConfig = pieChartConfig
            )
        }
        
        // Legend - Horizontal arrangement
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
                    modifier = Modifier
                        .padding(horizontal = 2.dp),
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

@Composable
fun CategoryPieChart(categoryStats: Map<String, Int>, total: Int) {
    // Define colors for different categories
    val categoryColors = mapOf(
        "offensive" to Color(0xFFEF4444),
        "violence" to Color(0xFFDC2626),
        "sexual" to Color(0xFFEC4899),
        "harassment" to Color(0xFFF59E0B),
        "other" to Color(0xFF6B7280)
    )
    
    // Convert category stats to list with percentages
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
                    colors = CardDefaults.cardColors(
                        containerColor = Color.White
                    )
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
                                // Color indicator
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
                            
                            // Percentage display
                            Text(
                                text = "${(percentage * 100).toInt()}%",
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Bold,
                                color = color
                            )
                        }
                        
                        Spacer(modifier = Modifier.height(12.dp))
                        
                        // Progress bar
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(8.dp)
                                .clip(RoundedCornerShape(4.dp))
                                .background(Color(0xFFF3F4F6))
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth(percentage)
                                    .fillMaxHeight()
                                    .background(
                                        color = color,
                                        shape = RoundedCornerShape(4.dp)
                                    )
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun StatusDistribution(statusBreakdown: Map<String, Int>, total: Int) {
    val locked = statusBreakdown["Locked"] ?: 0
    val unlocked = statusBreakdown["Unlocked"] ?: 0
    // Ensure weight is never 0 - use 0.5f for equal split when no data
    val lockedPercentage = if (total > 0) locked.toFloat() / total.toFloat() else 0.5f
    val unlockedPercentage = if (total > 0) unlocked.toFloat() / total.toFloat() else 0.5f
    
    Column {
        // Visual bar showing locked vs unlocked
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(60.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(Color(0xFFF3F4F6))
        ) {
            // Locked portion
            Box(
                modifier = Modifier
                    .weight(lockedPercentage.coerceAtLeast(0.01f)) // Minimum weight
                    .fillMaxHeight()
                    .background(
                        Brush.horizontalGradient(
                            colors = listOf(
                                Color(0xFFEF4444),
                                Color(0xFFDC2626)
                            )
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                if (lockedPercentage > 0.15f) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "${(lockedPercentage * 100).toInt()}%",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Text(
                            text = "Locked",
                            fontSize = 11.sp,
                            color = Color.White.copy(alpha = 0.9f)
                        )
                    }
                }
            }
            
            // Unlocked portion
            Box(
                modifier = Modifier
                    .weight(unlockedPercentage.coerceAtLeast(0.01f)) // Minimum weight
                    .fillMaxHeight()
                    .background(
                        Brush.horizontalGradient(
                            colors = listOf(
                                Color(0xFF10B981),
                                Color(0xFF059669)
                            )
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                if (unlockedPercentage > 0.15f) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "${(unlockedPercentage * 100).toInt()}%",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Text(
                            text = "Resolved",
                            fontSize = 11.sp,
                            color = Color.White.copy(alpha = 0.9f)
                        )
                    }
                }
            }
        }
        
        Spacer(modifier = Modifier.height(20.dp))
        
        // Stats boxes
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(0xFFFEE2E2))
                    .padding(16.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Lock,
                    contentDescription = "Locked",
                    tint = Color(0xFFEF4444),
                    modifier = Modifier.size(28.dp)
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = locked.toString(),
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFFEF4444)
                )
                Text(
                    text = "Active Locks",
                    fontSize = 12.sp,
                    color = Color(0xFF6B7280)
                )
            }
            
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(0xFFDCFCE7))
                    .padding(16.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = "Unlocked",
                    tint = Color(0xFF10B981),
                    modifier = Modifier.size(28.dp)
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = unlocked.toString(),
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF10B981)
                )
                Text(
                    text = "Resolved",
                    fontSize = 12.sp,
                    color = Color(0xFF6B7280)
                )
            }
        }
    }
}

@Composable
fun LogoutOTPCard(child: Child) {
    // Check if logout passcode is expired
    fun isLogoutPasscodeExpired(generatedAt: Long?): Boolean {
        return if (generatedAt == null) {
            true
        } else {
            (System.currentTimeMillis() - generatedAt) > UserRepository.PASSCODE_EXPIRY_MS
        }
    }
    
    val isExpired = isLogoutPasscodeExpired(child.logoutPasscodeGeneratedAt)
    
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(12.dp, RoundedCornerShape(24.dp)),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isExpired) Color(0xFFFEF2F2) else Color(0xFFF0F9FF)
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(50.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(
                                if (isExpired) Color(0xFFEF4444) else Color(0xFF3B82F6)
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.ExitToApp,
                            contentDescription = "Logout OTP",
                            tint = Color.White,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                    
                    Spacer(modifier = Modifier.width(16.dp))
                    
                    Column {
                        Text(
                            text = "Child Logout Request",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF1F2937)
                        )
                        Text(
                            text = if (isExpired) "Code Expired" else "Active Request",
                            fontSize = 14.sp,
                            color = if (isExpired) Color(0xFFEF4444) else Color(0xFF3B82F6),
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            if (!isExpired && child.logoutPasscode != null) {
                // Show the OTP code
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "Give this code to your child:",
                            fontSize = 14.sp,
                            color = Color(0xFF6B7280),
                            textAlign = TextAlign.Center
                        )
                        
                        Spacer(modifier = Modifier.height(12.dp))
                        
                        Text(
                            text = child.logoutPasscode,
                            fontSize = 36.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF1F2937),
                            letterSpacing = 4.sp
                        )
                        
                        Spacer(modifier = Modifier.height(8.dp))
                        
                        Text(
                            text = "Expires in 5 minutes",
                            fontSize = 12.sp,
                            color = Color(0xFFEF4444),
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            } else if (isExpired) {
                Text(
                    text = "The logout code has expired. Child will need to request a new code to logout.",
                    fontSize = 14.sp,
                    color = Color(0xFF6B7280),
                    lineHeight = 20.sp
                )
            }
        }
    }
}


