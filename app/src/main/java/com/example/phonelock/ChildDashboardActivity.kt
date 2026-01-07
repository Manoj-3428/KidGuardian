package com.example.phonelock

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.material3.MaterialTheme
import com.example.phonelock.repository.UserRepository
import com.example.phonelock.ui.child.ChildDashboardScreen
import com.example.phonelock.viewmodel.ChildDashboardViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class ChildDashboardActivity : ComponentActivity() {
    private val viewModel: ChildDashboardViewModel by viewModels()
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        val repository = UserRepository(this)
        viewModel.setRepository(repository)
        
        setContent {
            MaterialTheme {
                ChildDashboardScreen(
                    viewModel = viewModel,
                    onLogout = {
                        val childData = viewModel.childData.value
                        if (childData != null) {
                            CoroutineScope(Dispatchers.IO).launch {
                                repository.clearLogoutPasscode(childData.childId)
                            }
                        }
                        repository.logout()
                        startActivity(Intent(this, RoleSelectionActivity::class.java))
                        finish()
                    },
                    onNavigateToPermissions = {
                        startActivity(Intent(this, PermissionSetupActivity::class.java))
                        finish()
                    }
                )
            }
        }
    }
}
