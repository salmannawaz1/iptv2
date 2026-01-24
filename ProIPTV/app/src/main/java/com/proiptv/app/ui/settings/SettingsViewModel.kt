package com.proiptv.app.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.proiptv.app.data.local.PreferencesManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class UserInfoDisplay(
    val username: String,
    val serverUrl: String,
    val expiry: String,
    val maxConnections: String
)

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val preferencesManager: PreferencesManager
) : ViewModel() {
    
    private val _isLoggedOut = MutableStateFlow(false)
    val isLoggedOut: StateFlow<Boolean> = _isLoggedOut.asStateFlow()
    
    val autoPlay: StateFlow<Boolean> = preferencesManager.autoPlay
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)
    
    val defaultQuality: StateFlow<String> = preferencesManager.defaultQuality
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "auto")
    
    val userInfo: StateFlow<UserInfoDisplay?> = preferencesManager.xtreamCredentials
        .map { credentials ->
            credentials?.let {
                UserInfoDisplay(
                    username = it.username,
                    serverUrl = it.serverUrl,
                    expiry = "N/A",
                    maxConnections = "N/A"
                )
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)
    
    fun setAutoPlay(enabled: Boolean) {
        viewModelScope.launch {
            preferencesManager.setAutoPlay(enabled)
        }
    }
    
    fun setDefaultQuality(quality: String) {
        viewModelScope.launch {
            preferencesManager.setDefaultQuality(quality)
        }
    }
    
    fun clearCache() {
        viewModelScope.launch {
            // Clear cache implementation
        }
    }
    
    fun logout() {
        viewModelScope.launch {
            preferencesManager.logout()
            _isLoggedOut.value = true
        }
    }
}
