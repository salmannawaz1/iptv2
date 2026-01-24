package com.proiptv.app.ui.movies

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
class MoviesViewModel @Inject constructor(
    private val repository: IPTVRepository,
    private val preferencesManager: PreferencesManager
) : ViewModel() {
    
    private val _categories = MutableStateFlow<Resource<List<VodCategory>>>(Resource.Loading())
    val categories: StateFlow<Resource<List<VodCategory>>> = _categories.asStateFlow()
    
    private val _movies = MutableStateFlow<Resource<List<VodStream>>>(Resource.Loading())
    val movies: StateFlow<Resource<List<VodStream>>> = _movies.asStateFlow()
    
    private val _selectedCategory = MutableStateFlow<VodCategory?>(null)
    val selectedCategory: StateFlow<VodCategory?> = _selectedCategory.asStateFlow()
    
    private var credentials: XtreamCredentials? = null
    private var m3uPlaylist: M3UPlaylist? = null
    private var isM3UMode = false
    
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
                    loadMovies(creds, null)
                }
            }
        }
    }
    
    private suspend fun loadM3UData(url: String) {
        _categories.value = Resource.Loading()
        _movies.value = Resource.Loading()
        
        when (val result = repository.loadM3UPlaylist(url)) {
            is Resource.Success -> {
                m3uPlaylist = result.data
                val playlist = result.data!!
                
                // Convert M3U movie categories to VodCategory
                val categoryList = playlist.getMovieCategories().map { cat ->
                    VodCategory(cat.name, cat.name, null)
                }
                _categories.value = Resource.Success(categoryList)
                
                // Convert M3U movies to VodStream
                val streams = playlist.movies.map { channel ->
                    VodStream(
                        num = channel.id,
                        name = channel.name,
                        streamType = "movie",
                        streamId = channel.id,
                        streamIcon = channel.logo,
                        rating = null,
                        rating5Based = null,
                        added = null,
                        categoryId = channel.group ?: "Uncategorized",
                        containerExtension = "mp4",
                        customSid = null,
                        directSource = channel.url,
                        isFavorite = false
                    )
                }
                _movies.value = Resource.Success(streams)
            }
            is Resource.Error -> {
                _categories.value = Resource.Error(result.message ?: "Failed to load")
                _movies.value = Resource.Error(result.message ?: "Failed to load")
            }
            is Resource.Loading -> {
                _categories.value = Resource.Loading()
                _movies.value = Resource.Loading()
            }
        }
    }
    
    private fun loadCategories(credentials: XtreamCredentials) {
        viewModelScope.launch {
            repository.getVodCategories(credentials).collect { resource ->
                _categories.value = resource
            }
        }
    }
    
    private fun loadMovies(credentials: XtreamCredentials, categoryId: String?) {
        viewModelScope.launch {
            repository.getVodStreams(credentials, categoryId).collect { resource ->
                _movies.value = resource
            }
        }
    }
    
    fun selectCategory(category: VodCategory) {
        _selectedCategory.value = category
        
        if (isM3UMode) {
            m3uPlaylist?.let { playlist ->
                val filteredMovies = if (category.categoryId == "all") {
                    playlist.movies
                } else {
                    playlist.movies.filter { it.group == category.categoryId }
                }
                
                val streams = filteredMovies.map { channel ->
                    VodStream(
                        num = channel.id,
                        name = channel.name,
                        streamType = "movie",
                        streamId = channel.id,
                        streamIcon = channel.logo,
                        rating = null,
                        rating5Based = null,
                        added = null,
                        categoryId = channel.group ?: "Uncategorized",
                        containerExtension = "mp4",
                        customSid = null,
                        directSource = channel.url,
                        isFavorite = false
                    )
                }
                _movies.value = Resource.Success(streams)
            }
        } else {
            credentials?.let { creds ->
                val categoryId = if (category.categoryId == "all") null else category.categoryId
                loadMovies(creds, categoryId)
            }
        }
    }
    
    fun toggleFavorite(movie: VodStream) {
        viewModelScope.launch {
            val favoriteId = "vod_${movie.streamId}"
            if (movie.isFavorite) {
                repository.removeFavorite(favoriteId)
            } else {
                repository.addFavorite(favoriteId, "vod", movie.name ?: "", movie.streamIcon)
            }
            
            val currentMovies = (_movies.value as? Resource.Success)?.data?.toMutableList()
            currentMovies?.let { list ->
                val index = list.indexOfFirst { it.streamId == movie.streamId }
                if (index != -1) {
                    list[index] = movie.copy(isFavorite = !movie.isFavorite)
                    _movies.value = Resource.Success(list)
                }
            }
        }
    }
}
