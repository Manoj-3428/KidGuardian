package com.example.phonelock.ui.parent

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.phonelock.ui.parent.tabs.ActivityTab
import com.example.phonelock.ui.parent.tabs.AnalyticsTab
import com.example.phonelock.ui.parent.tabs.DashboardTab
import com.example.phonelock.viewmodel.ParentDashboardViewModel
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ParentDashboardScreen(
    viewModel: ParentDashboardViewModel,
    onLogout: () -> Unit
) {
    val parentData = viewModel.parentData.value
    val childData = viewModel.childData.value
    val complaints = viewModel.complaints.value
    val isLoading = viewModel.isLoading.value
    val monitoringEnabled = viewModel.monitoringEnabled.value
    
    LaunchedEffect(Unit) {
        while (true) {
            delay(5000)
            viewModel.refreshDataSilently()
        }
    }
    
    var selectedTab by remember { mutableIntStateOf(0) }
    val tabs = listOf("Dashboard", "Activity", "Analytics")
    
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

