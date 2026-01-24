package com.proiptv.app.ui.player

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.proiptv.app.data.local.PreferencesManager
import com.proiptv.app.data.model.XtreamCredentials
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class PlayerViewModel @Inject constructor(
    private val preferencesManager: PreferencesManager
) : ViewModel() {
    
    private val _streamUrl = MutableStateFlow<String?>(null)
    val streamUrl: StateFlow<String?> = _streamUrl.asStateFlow()
    
    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()
    
    fun setDirectUrl(url: String) {
        _streamUrl.value = url
    }
    
    fun getStreamUrl(streamId: Int, streamType: String, containerExt: String?) {
        viewModelScope.launch {
            preferencesManager.xtreamCredentials.first()?.let { credentials ->
                val url = when (streamType) {
                    "live" -> credentials.getLiveStreamUrl(streamId)
                    "vod" -> credentials.getVodStreamUrl(streamId, containerExt ?: "mp4")
                    "series" -> credentials.getSeriesStreamUrl(streamId, containerExt ?: "mp4")
                    else -> null
                }
                
                if (url != null) {
                    _streamUrl.value = url
                } else {
                    _error.value = "Unable to get stream URL"
                }
            } ?: run {
                _error.value = "No credentials found"
            }
        }
    }
}
