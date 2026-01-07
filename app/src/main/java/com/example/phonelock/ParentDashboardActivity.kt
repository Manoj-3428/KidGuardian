package com.example.phonelock

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.material3.MaterialTheme
import com.example.phonelock.repository.UserRepository
import com.example.phonelock.ui.parent.ParentDashboardScreen
import com.example.phonelock.viewmodel.ParentDashboardViewModel

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
