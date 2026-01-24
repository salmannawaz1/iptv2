package com.proiptv.app.ui.livetv

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.proiptv.app.data.local.PreferencesManager
import com.proiptv.app.data.model.*
import com.proiptv.app.data.repository.IPTVRepository
import com.proiptv.app.util.Resource
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class LiveTvViewModel @Inject constructor(
    private val repository: IPTVRepository,
    private val preferencesManager: PreferencesManager
) : ViewModel() {
    
    private val _categories = MutableStateFlow<Resource<List<LiveCategory>>>(Resource.Loading())
    val categories: StateFlow<Resource<List<LiveCategory>>> = _categories.asStateFlow()
    
    private val _channels = MutableStateFlow<Resource<List<LiveStream>>>(Resource.Loading())
    val channels: StateFlow<Resource<List<LiveStream>>> = _channels.asStateFlow()
    
    private val _selectedCategory = MutableStateFlow<LiveCategory?>(null)
    val selectedCategory: StateFlow<LiveCategory?> = _selectedCategory.asStateFlow()
    
    private var credentials: XtreamCredentials? = null
    private var m3uPlaylist: M3UPlaylist? = null
    private var isM3UMode = false
    
    init {
        viewModelScope.launch {
            preferencesManager.xtreamCredentials.collect { creds ->
                credentials = creds
            }
        }
    }
    
    fun loadData() {
        viewModelScope.launch {
            val loginType = preferencesManager.loginType.first()
            
            if (loginType == "m3u") {
                isM3UMode = true
                preferencesManager.m3uUrl.first()?.let { url ->
                    loadM3UData(url)
                }
            } else {
                isM3UMode = false
                preferencesManager.xtreamCredentials.first()?.let { creds ->
                    credentials = creds
                    loadCategories(creds)
                    loadChannels(creds, null)
                }
            }
        }
    }
    
    private suspend fun loadM3UData(url: String) {
        _categories.value = Resource.Loading()
        _channels.value = Resource.Loading()
        
        when (val result = repository.loadM3UPlaylist(url)) {
            is Resource.Success -> {
                m3uPlaylist = result.data
                val playlist = result.data!!
                
                // Convert M3U categories to LiveCategory
                val categoryList = playlist.getCategories().map { cat ->
                    LiveCategory(cat.name, cat.name, null)
                }
                _categories.value = Resource.Success(categoryList)
                
                // Convert M3U channels to LiveStream
                val streams = playlist.channels.map { channel ->
                    LiveStream(
                        num = channel.id,
                        name = channel.name,
                        streamType = "live",
                        streamId = channel.id,
                        streamIcon = channel.logo,
                        epgChannelId = channel.tvgId,
                        added = null,
                        categoryId = channel.group ?: "Uncategorized",
                        customSid = null,
                        tvArchive = 0,
                        directSource = channel.url,
                        tvArchiveDuration = 0,
                        isFavorite = false
                    )
                }
                _channels.value = Resource.Success(streams)
            }
            is Resource.Error -> {
                _categories.value = Resource.Error(result.message ?: "Failed to load")
                _channels.value = Resource.Error(result.message ?: "Failed to load")
            }
            is Resource.Loading -> {
                _categories.value = Resource.Loading()
                _channels.value = Resource.Loading()
            }
        }
    }
    
    private fun loadCategories(credentials: XtreamCredentials) {
        viewModelScope.launch {
            repository.getLiveCategories(credentials).collect { resource ->
                _categories.value = resource
            }
        }
    }
    
    private fun loadChannels(credentials: XtreamCredentials, categoryId: String?) {
        viewModelScope.launch {
            repository.getLiveStreams(credentials, categoryId).collect { resource ->
                _channels.value = resource
            }
        }
    }
    
    fun selectCategory(category: LiveCategory) {
        _selectedCategory.value = category
        
        if (isM3UMode) {
            // Filter M3U channels by category
            m3uPlaylist?.let { playlist ->
                val filteredChannels = if (category.categoryId == "all") {
                    playlist.channels
                } else {
                    playlist.channels.filter { it.group == category.categoryId }
                }
                
                val streams = filteredChannels.map { channel ->
                    LiveStream(
                        num = channel.id,
                        name = channel.name,
                        streamType = "live",
                        streamId = channel.id,
                        streamIcon = channel.logo,
                        epgChannelId = channel.tvgId,
                        added = null,
                        categoryId = channel.group ?: "Uncategorized",
                        customSid = null,
                        tvArchive = 0,
                        directSource = channel.url,
                        tvArchiveDuration = 0,
                        isFavorite = false
                    )
                }
                _channels.value = Resource.Success(streams)
            }
        } else {
            credentials?.let { creds ->
                val categoryId = if (category.categoryId == "all") null else category.categoryId
                loadChannels(creds, categoryId)
            }
        }
    }
    
    fun toggleFavorite(channel: LiveStream) {
        viewModelScope.launch {
            val favoriteId = "live_${channel.streamId}"
            if (channel.isFavorite) {
                repository.removeFavorite(favoriteId)
            } else {
                repository.addFavorite(favoriteId, "live", channel.name ?: "", channel.streamIcon)
            }
            
            // Update the channel in the list
            val currentChannels = (_channels.value as? Resource.Success)?.data?.toMutableList()
            currentChannels?.let { list ->
                val index = list.indexOfFirst { it.streamId == channel.streamId }
                if (index != -1) {
                    list[index] = channel.copy(isFavorite = !channel.isFavorite)
                    _channels.value = Resource.Success(list)
                }
            }
        }
    }
}
