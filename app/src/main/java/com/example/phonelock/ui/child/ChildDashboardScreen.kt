package com.example.phonelock.ui.child

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.phonelock.ui.child.components.ParentLinkStatusCard
import com.example.phonelock.ui.child.components.ProfileCard
import com.example.phonelock.ui.child.components.RecentActivitySection
import com.example.phonelock.ui.child.dialog.LogoutVerificationDialog
import com.example.phonelock.utils.arePermissionsGranted
import com.example.phonelock.viewmodel.ChildDashboardViewModel
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChildDashboardScreen(
    viewModel: ChildDashboardViewModel,
    onLogout: () -> Unit,
    onNavigateToPermissions: () -> Unit
) {
    val childData = viewModel.childData.value
    val complaints = viewModel.complaints.value
    val isLoading = viewModel.isLoading.value
    val context = LocalContext.current
    
    var hasNavigated by remember { mutableStateOf(false) }
    var showLogoutDialog by remember { mutableStateOf(false) }
    
    LaunchedEffect(Unit) {
        while (true) {
            delay(3000)
            viewModel.refreshDataSilently()
        }
    }
    
    LaunchedEffect(childData?.parentLinked) {
        if (childData?.parentLinked == true && !hasNavigated) {
            val permissionsGranted = arePermissionsGranted(context)
            if (!permissionsGranted) {
                hasNavigated = true
                onNavigateToPermissions()
            }
        }
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("My Dashboard", fontWeight = FontWeight.Bold) },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(0xFF8B5CF6),
                    titleContentColor = Color.White,
                    actionIconContentColor = Color.White
                ),
                actions = {
                    IconButton(onClick = { viewModel.refreshData() }) {
                        Icon(Icons.Default.Refresh, "Refresh")
                    }
                    IconButton(onClick = {
                        if (childData?.parentLinked == true) {
                            showLogoutDialog = true
                            viewModel.generateLogoutOTP(childData.childId) { success ->
                                if (success) {
                                    viewModel.refreshDataSilently()
                                }
                            }
                        } else {
                            onLogout()
                        }
                    }) {
                        Icon(Icons.AutoMirrored.Filled.ExitToApp, "Logout")
                    }
                }
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(
                    Brush.linearGradient(
                        colors = listOf(
                            Color(0xFFF3E8FF),
                            Color(0xFFE9D5FF),
                            Color(0xFFDDD6FE)
                        )
                    )
                )
        ) {
            if (isLoading) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = Color(0xFF8B5CF6))
                }
            } else if (childData != null) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(16.dp)
                ) {
                    ProfileCard(childData)
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    ParentLinkStatusCard(childData)
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    RecentActivitySection(complaints)
                }
            } else {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text("Loading data...", color = Color.Gray)
                }
            }
        }
    }
    
    if (showLogoutDialog) {
        LogoutVerificationDialog(
            childData = childData,
            onDismiss = { showLogoutDialog = false },
            onSuccess = onLogout
        )
    }
}

