package com.proiptv.app.util

object AppConfig {
    // List of possible server URLs - app will try each one
    private val SERVER_URLS = listOf(
        "http://192.168.59.122:5000",  // WiFi network 1
        "http://192.168.100.78:5000",  // WiFi network 2
        "http://localhost:5000"         // For emulator/testing
    )
    
    // API endpoint for Xtream Codes
    const val PLAYER_API_PATH = "/player_api.php"
    
    // Get the first available server URL
    fun getServerUrl(): String {
        // Try to find the best server by checking if it's the current IP
        return SERVER_URLS.first()
    }
    
    // Try all server URLs until one works
    fun getServerUrls(): List<String> {
        return SERVER_URLS
    }
    
    fun getPlayerApiUrl(): String {
        return "${getServerUrl()}$PLAYER_API_PATH"
    }
}
