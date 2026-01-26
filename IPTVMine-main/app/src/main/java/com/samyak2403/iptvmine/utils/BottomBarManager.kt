package com.samyak2403.iptvmine.utils

import android.content.Context

/**
 * Enum representing different bottom bar styles available in the app
 */
enum class BottomBarType(val id: Int, val displayName: String) {
    SMOOTH_BOTTOM_BAR(0, "Smooth Bottom Bar"),
    ANIMATED_BOTTOM_BAR(1, "Animated Bottom Bar"),
    CHIP_NAVIGATION_BAR(2, "Chip Navigation Bar");

    companion object {
        fun fromId(id: Int): BottomBarType {
            return entries.find { it.id == id } ?: SMOOTH_BOTTOM_BAR
        }
    }
}

/**
 * Manager class to handle bottom bar preference storage and retrieval
 */
object BottomBarManager {
    private const val PREF_NAME = "app_prefs"
    private const val KEY_BOTTOM_BAR_TYPE = "bottom_bar_type"

    /**
     * Get the currently selected bottom bar type
     */
    fun getSelectedBottomBar(context: Context): BottomBarType {
        val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        val id = prefs.getInt(KEY_BOTTOM_BAR_TYPE, BottomBarType.SMOOTH_BOTTOM_BAR.id)
        return BottomBarType.fromId(id)
    }

    /**
     * Save the selected bottom bar type
     */
    fun setSelectedBottomBar(context: Context, type: BottomBarType) {
        val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        prefs.edit().putInt(KEY_BOTTOM_BAR_TYPE, type.id).apply()
    }

    /**
     * Get all available bottom bar types
     */
    fun getAllBottomBarTypes(): List<BottomBarType> {
        return BottomBarType.entries.toList()
    }
}
