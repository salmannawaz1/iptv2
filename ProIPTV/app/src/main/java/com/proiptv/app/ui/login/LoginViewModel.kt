package com.proiptv.app.ui.login

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.proiptv.app.data.local.PreferencesManager
import com.proiptv.app.data.model.XtreamCredentials
import com.proiptv.app.data.repository.IPTVRepository
import com.proiptv.app.util.AppConfig
import com.proiptv.app.util.Resource
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed class LoginState {
    object Idle : LoginState()
    object Loading : LoginState()
    object Success : LoginState()
    data class Error(val message: String) : LoginState()
}

@HiltViewModel
class LoginViewModel @Inject constructor(
    private val repository: IPTVRepository,
    private val preferencesManager: PreferencesManager
) : ViewModel() {
    
    private val _loginState = MutableStateFlow<LoginState>(LoginState.Idle)
    val loginState: StateFlow<LoginState> = _loginState.asStateFlow()
    
    val isLoggedIn: Flow<Boolean> = preferencesManager.isLoggedIn
    
    fun loginWithXtream(username: String, password: String, rememberMe: Boolean) {
        viewModelScope.launch {
            _loginState.value = LoginState.Loading
            
            // Use first server URL from AppConfig (repository will try all)
            val serverUrl = AppConfig.getServerUrl()
            val credentials = XtreamCredentials(serverUrl, username, password)
            
            when (val result = repository.authenticateXtream(credentials)) {
                is Resource.Success -> {
                    preferencesManager.saveXtreamCredentials(credentials, rememberMe)
                    _loginState.value = LoginState.Success
                }
                is Resource.Error -> {
                    _loginState.value = LoginState.Error(result.message ?: "Authentication failed")
                }
                is Resource.Loading -> {
                    _loginState.value = LoginState.Loading
                }
            }
        }
    }
    
    fun loginWithM3U(url: String, rememberMe: Boolean) {
        viewModelScope.launch {
            _loginState.value = LoginState.Loading
            
            when (val result = repository.loadM3UPlaylist(url)) {
                is Resource.Success -> {
                    preferencesManager.saveM3UUrl(url, rememberMe)
                    _loginState.value = LoginState.Success
                }
                is Resource.Error -> {
                    _loginState.value = LoginState.Error(result.message ?: "Failed to load playlist")
                }
                is Resource.Loading -> {
                    _loginState.value = LoginState.Loading
                }
            }
        }
    }
}
