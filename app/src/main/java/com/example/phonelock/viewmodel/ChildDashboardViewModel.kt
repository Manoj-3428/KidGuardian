package com.example.phonelock.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.phonelock.models.Child
import com.example.phonelock.models.Complaint
import com.example.phonelock.repository.UserRepository
import kotlinx.coroutines.launch
import androidx.compose.runtime.*
import android.util.Log

class ChildDashboardViewModel : ViewModel() {
    private lateinit var repository: UserRepository
    
    private val _childData = mutableStateOf<Child?>(null)
    val childData: State<Child?> = _childData
    
    private val _complaints = mutableStateOf<List<Complaint>>(emptyList())
    val complaints: State<List<Complaint>> = _complaints
    
    private val _isLoading = mutableStateOf(true)
    val isLoading: State<Boolean> = _isLoading
    
    fun setRepository(repo: UserRepository) {
        repository = repo
        loadData()
    }
    
    fun loadData() {
        _isLoading.value = true
        viewModelScope.launch {
            val childResult = repository.getChildData()
            if (childResult.isSuccess) {
                _childData.value = childResult.getOrNull()
                
                _childData.value?.childId?.let { childId ->
                    val complaintsResult = repository.getComplaintsForChild(childId)
                    if (complaintsResult.isSuccess) {
                        _complaints.value = complaintsResult.getOrNull() ?: emptyList()
                    }
                }
            }
            _isLoading.value = false
        }
    }
    
    fun refreshData() {
        loadData()
    }
    
    fun refreshDataSilently() {
        viewModelScope.launch {
            val childResult = repository.getChildData()
            if (childResult.isSuccess) {
                _childData.value = childResult.getOrNull()
                
                _childData.value?.childId?.let { childId ->
                    val complaintsResult = repository.getComplaintsForChild(childId)
                    if (complaintsResult.isSuccess) {
                        _complaints.value = complaintsResult.getOrNull() ?: emptyList()
                    }
                }
            }
        }
    }
    
    fun generateLogoutOTP(childId: String, onResult: (Boolean) -> Unit) {
        viewModelScope.launch {
            try {
                val result = repository.generateAndSaveLogoutPasscode(childId)
                if (result.isSuccess) {
                    Log.d("LOGOUT_OTP", "Logout OTP generated successfully")
                    onResult(true)
                } else {
                    Log.e("LOGOUT_OTP", "Failed to generate logout OTP: ${result.exceptionOrNull()?.message}")
                    onResult(false)
                }
            } catch (e: Exception) {
                Log.e("LOGOUT_OTP", "Error generating logout OTP", e)
                onResult(false)
            }
        }
    }
}

