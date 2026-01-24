package com.proiptv.app.ui.main

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.proiptv.app.data.local.PreferencesManager
import com.proiptv.app.data.model.XtreamCredentials
import com.proiptv.app.data.repository.IPTVRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class MainViewModel @Inject constructor(
    private val repository: IPTVRepository,
    private val preferencesManager: PreferencesManager
) : ViewModel() {
    
    private val _isLoggedOut = MutableStateFlow(false)
    val isLoggedOut: StateFlow<Boolean> = _isLoggedOut.asStateFlow()
    
    private val _refreshTrigger = MutableSharedFlow<Unit>()
    val refreshTrigger: SharedFlow<Unit> = _refreshTrigger.asSharedFlow()
    
    val credentials: Flow<XtreamCredentials?> = preferencesManager.xtreamCredentials
    val loginType: Flow<String> = preferencesManager.loginType
    val m3uUrl: Flow<String?> = preferencesManager.m3uUrl
    
    fun refreshData() {
        viewModelScope.launch {
            _refreshTrigger.emit(Unit)
        }
    }
    
    fun logout() {
        viewModelScope.launch {
            preferencesManager.logout()
            _isLoggedOut.value = true
        }
    }
}
