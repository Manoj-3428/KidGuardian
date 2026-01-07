package com.example.phonelock.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.phonelock.models.Complaint
import com.example.phonelock.models.Parent
import com.example.phonelock.models.Child
import com.example.phonelock.repository.UserRepository
import kotlinx.coroutines.launch
import androidx.compose.runtime.*

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
            val parentResult = repository.getParentData()
            if (parentResult.isSuccess) {
                val parent = parentResult.getOrNull()
                _parentData.value = parent
                _monitoringEnabled.value = parent?.monitoringEnabled ?: true
                
                parent?.linkedChildId?.let { childId ->
                    val childResult = repository.getChildByChildId(childId)
                    if (childResult.isSuccess) {
                        _childData.value = childResult.getOrNull()
                        
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
        viewModelScope.launch {
            val parentResult = repository.getParentData()
            if (parentResult.isSuccess) {
                val parent = parentResult.getOrNull()
                _parentData.value = parent
                _monitoringEnabled.value = parent?.monitoringEnabled ?: true
                
                parent?.linkedChildId?.let { childId ->
                    val childResult = repository.getChildByChildId(childId)
                    if (childResult.isSuccess) {
                        _childData.value = childResult.getOrNull()
                        
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

