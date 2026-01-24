package com.proiptv.app.ui.series

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
class SeriesViewModel @Inject constructor(
    private val repository: IPTVRepository,
    private val preferencesManager: PreferencesManager
) : ViewModel() {
    
    private val _categories = MutableStateFlow<Resource<List<SeriesCategory>>>(Resource.Loading())
    val categories: StateFlow<Resource<List<SeriesCategory>>> = _categories.asStateFlow()
    
    private val _series = MutableStateFlow<Resource<List<Series>>>(Resource.Loading())
    val series: StateFlow<Resource<List<Series>>> = _series.asStateFlow()
    
    private val _seriesInfo = MutableStateFlow<Resource<SeriesInfo>?>(null)
    val seriesInfo: StateFlow<Resource<SeriesInfo>?> = _seriesInfo.asStateFlow()
    
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
                    loadSeries(creds, null)
                }
            }
        }
    }
    
    private suspend fun loadM3UData(url: String) {
        _categories.value = Resource.Loading()
        _series.value = Resource.Loading()
        
        when (val result = repository.loadM3UPlaylist(url)) {
            is Resource.Success -> {
                m3uPlaylist = result.data
                val playlist = result.data!!
                
                // Convert M3U series categories to SeriesCategory
                val categoryList = playlist.getSeriesCategories().map { cat ->
                    SeriesCategory(cat.name, cat.name, null)
                }
                _categories.value = Resource.Success(categoryList)
                
                // Convert M3U series to Series
                val seriesList = playlist.series.map { channel ->
                    Series(
                        num = channel.id,
                        name = channel.name,
                        seriesId = channel.id,
                        cover = channel.logo,
                        plot = null,
                        cast = null,
                        director = null,
                        genre = channel.group,
                        releaseDate = null,
                        rating = null,
                        rating5Based = null,
                        youtubeTrailer = null,
                        categoryId = channel.group ?: "Uncategorized",
                        backdropPath = null,
                        isFavorite = false
                    )
                }
                _series.value = Resource.Success(seriesList)
            }
            is Resource.Error -> {
                _categories.value = Resource.Error(result.message ?: "Failed to load")
                _series.value = Resource.Error(result.message ?: "Failed to load")
            }
            is Resource.Loading -> {
                _categories.value = Resource.Loading()
                _series.value = Resource.Loading()
            }
        }
    }
    
    private fun loadCategories(credentials: XtreamCredentials) {
        viewModelScope.launch {
            repository.getSeriesCategories(credentials).collect { resource ->
                _categories.value = resource
            }
        }
    }
    
    private fun loadSeries(credentials: XtreamCredentials, categoryId: String?) {
        viewModelScope.launch {
            repository.getSeries(credentials, categoryId).collect { resource ->
                _series.value = resource
            }
        }
    }
    
    fun selectCategory(category: SeriesCategory) {
        if (isM3UMode) {
            m3uPlaylist?.let { playlist ->
                val filteredSeries = if (category.categoryId == "all") {
                    playlist.series
                } else {
                    playlist.series.filter { it.group == category.categoryId }
                }
                
                val seriesList = filteredSeries.map { channel ->
                    Series(
                        num = channel.id,
                        name = channel.name,
                        seriesId = channel.id,
                        cover = channel.logo,
                        plot = null,
                        cast = null,
                        director = null,
                        genre = channel.group,
                        releaseDate = null,
                        rating = null,
                        rating5Based = null,
                        youtubeTrailer = null,
                        categoryId = channel.group ?: "Uncategorized",
                        backdropPath = null,
                        isFavorite = false
                    )
                }
                _series.value = Resource.Success(seriesList)
            }
        } else {
            credentials?.let { creds ->
                val categoryId = if (category.categoryId == "all") null else category.categoryId
                loadSeries(creds, categoryId)
            }
        }
    }
    
    fun loadSeriesInfo(seriesId: Int) {
        viewModelScope.launch {
            _seriesInfo.value = Resource.Loading()
            credentials?.let { creds ->
                _seriesInfo.value = repository.getSeriesInfo(creds, seriesId)
            }
        }
    }
    
    fun toggleFavorite(series: Series) {
        viewModelScope.launch {
            val favoriteId = "series_${series.seriesId}"
            if (series.isFavorite) {
                repository.removeFavorite(favoriteId)
            } else {
                repository.addFavorite(favoriteId, "series", series.name ?: "", series.cover)
            }
            
            val currentSeries = (_series.value as? Resource.Success)?.data?.toMutableList()
            currentSeries?.let { list ->
                val index = list.indexOfFirst { it.seriesId == series.seriesId }
                if (index != -1) {
                    list[index] = series.copy(isFavorite = !series.isFavorite)
                    _series.value = Resource.Success(list)
                }
            }
        }
    }
}
