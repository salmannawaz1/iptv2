package com.proiptv.app.ui.admin

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.*
import javax.inject.Inject

data class AdminUser(
    val id: String = UUID.randomUUID().toString(),
    val username: String,
    val password: String,
    val maxConnections: Int = 1,
    val createdAt: Long = System.currentTimeMillis(),
    val expiryDate: Long = System.currentTimeMillis() + (30L * 24 * 60 * 60 * 1000),
    val isActive: Boolean = true,
    val isReseller: Boolean = false,
    val createdBy: String? = null
)

data class AdminStatistics(
    val totalUsers: Int = 0,
    val activeUsers: Int = 0,
    val expiredUsers: Int = 0,
    val totalResellers: Int = 0,
    val onlineUsers: Int = 0
)

@HiltViewModel
class AdminViewModel @Inject constructor() : ViewModel() {
    
    private val _users = MutableStateFlow<List<AdminUser>>(emptyList())
    val users: StateFlow<List<AdminUser>> = _users.asStateFlow()
    
    private val _statistics = MutableStateFlow<AdminStatistics?>(null)
    val statistics: StateFlow<AdminStatistics?> = _statistics.asStateFlow()
    
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()
    
    private val _message = MutableSharedFlow<String?>()
    val message: SharedFlow<String?> = _message.asSharedFlow()
    
    private val allUsers = mutableListOf<AdminUser>()
    
    init {
        // Initialize with demo data
        allUsers.addAll(listOf(
            AdminUser(
                username = "demo_user1",
                password = "pass123",
                maxConnections = 2,
                isActive = true
            ),
            AdminUser(
                username = "demo_user2", 
                password = "pass456",
                maxConnections = 1,
                isActive = true
            ),
            AdminUser(
                username = "reseller1",
                password = "reseller123",
                maxConnections = 5,
                isActive = true,
                isReseller = true
            ),
            AdminUser(
                username = "expired_user",
                password = "expired123",
                maxConnections = 1,
                isActive = false,
                expiryDate = System.currentTimeMillis() - (24 * 60 * 60 * 1000)
            )
        ))
    }
    
    fun loadUsers() {
        viewModelScope.launch {
            _isLoading.value = true
            kotlinx.coroutines.delay(500)
            _users.value = allUsers.filter { !it.isReseller }
            updateStatistics()
            _isLoading.value = false
        }
    }
    
    fun loadResellers() {
        viewModelScope.launch {
            _isLoading.value = true
            kotlinx.coroutines.delay(500)
            _users.value = allUsers.filter { it.isReseller }
            _isLoading.value = false
        }
    }
    
    fun loadStatistics() {
        viewModelScope.launch {
            _isLoading.value = true
            kotlinx.coroutines.delay(500)
            updateStatistics()
            _isLoading.value = false
        }
    }
    
    private fun updateStatistics() {
        val now = System.currentTimeMillis()
        _statistics.value = AdminStatistics(
            totalUsers = allUsers.count { !it.isReseller },
            activeUsers = allUsers.count { !it.isReseller && it.isActive && it.expiryDate > now },
            expiredUsers = allUsers.count { !it.isReseller && it.expiryDate <= now },
            totalResellers = allUsers.count { it.isReseller },
            onlineUsers = (0..allUsers.size / 2).random()
        )
    }
    
    fun addUser(username: String, password: String, maxConnections: Int, expiryDays: Int, isReseller: Boolean) {
        viewModelScope.launch {
            val newUser = AdminUser(
                username = username,
                password = password,
                maxConnections = maxConnections,
                expiryDate = System.currentTimeMillis() + (expiryDays.toLong() * 24 * 60 * 60 * 1000),
                isReseller = isReseller
            )
            allUsers.add(newUser)
            
            if (isReseller) {
                loadResellers()
            } else {
                loadUsers()
            }
            
            _message.emit("User '$username' added successfully")
        }
    }
    
    fun updateUser(user: AdminUser) {
        viewModelScope.launch {
            val index = allUsers.indexOfFirst { it.id == user.id }
            if (index != -1) {
                allUsers[index] = user
                
                if (user.isReseller) {
                    loadResellers()
                } else {
                    loadUsers()
                }
                
                _message.emit("User '${user.username}' updated successfully")
            }
        }
    }
    
    fun deleteUser(user: AdminUser) {
        viewModelScope.launch {
            allUsers.removeAll { it.id == user.id }
            
            if (user.isReseller) {
                loadResellers()
            } else {
                loadUsers()
            }
            
            _message.emit("User '${user.username}' deleted")
        }
    }
    
    fun toggleUserStatus(user: AdminUser) {
        viewModelScope.launch {
            val index = allUsers.indexOfFirst { it.id == user.id }
            if (index != -1) {
                allUsers[index] = user.copy(isActive = !user.isActive)
                
                if (user.isReseller) {
                    loadResellers()
                } else {
                    loadUsers()
                }
                
                val status = if (!user.isActive) "activated" else "deactivated"
                _message.emit("User '${user.username}' $status")
            }
        }
    }
}
