/*
 * Created for TV compatibility
 * Utility class to detect TV mode and provide TV-specific configurations
 */

package com.samyak2403.iptvmine.utils

import android.app.Activity
import android.app.UiModeManager
import android.content.Context
import android.content.res.Configuration
import android.graphics.Rect
import android.view.KeyEvent
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import androidx.recyclerview.widget.RecyclerView

object TvUtils {
    
    // D-pad key codes for easy reference
    const val DPAD_UP = KeyEvent.KEYCODE_DPAD_UP
    const val DPAD_DOWN = KeyEvent.KEYCODE_DPAD_DOWN
    const val DPAD_LEFT = KeyEvent.KEYCODE_DPAD_LEFT
    const val DPAD_RIGHT = KeyEvent.KEYCODE_DPAD_RIGHT
    const val DPAD_CENTER = KeyEvent.KEYCODE_DPAD_CENTER
    const val ENTER = KeyEvent.KEYCODE_ENTER
    const val BACK = KeyEvent.KEYCODE_BACK
    const val MEDIA_PLAY_PAUSE = KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE
    const val MEDIA_PLAY = KeyEvent.KEYCODE_MEDIA_PLAY
    const val MEDIA_PAUSE = KeyEvent.KEYCODE_MEDIA_PAUSE
    const val MEDIA_FAST_FORWARD = KeyEvent.KEYCODE_MEDIA_FAST_FORWARD
    const val MEDIA_REWIND = KeyEvent.KEYCODE_MEDIA_REWIND
    
    /**
     * Check if the app is running on Android TV
     */
    fun isTvMode(context: Context): Boolean {
        val uiModeManager = context.getSystemService(Context.UI_MODE_SERVICE) as UiModeManager
        return uiModeManager.currentModeType == Configuration.UI_MODE_TYPE_TELEVISION
    }
    
    /**
     * Get the appropriate span count for grid layout based on device type
     */
    fun getGridSpanCount(context: Context): Int {
        return if (isTvMode(context)) {
            // TV: 4 columns for better viewing on large screens
            4
        } else {
            // Mobile: 2 columns
            2
        }
    }
    
    /**
     * Check if device has D-pad navigation
     */
    fun hasDpadNavigation(context: Context): Boolean {
        val config = context.resources.configuration
        return config.navigation == Configuration.NAVIGATION_DPAD ||
               config.navigation == Configuration.NAVIGATION_TRACKBALL ||
               isTvMode(context)
    }
    
    /**
     * Check if the key event is a selection key (Enter or D-pad center)
     */
    fun isSelectKey(keyCode: Int): Boolean {
        return keyCode == DPAD_CENTER || keyCode == ENTER
    }
    
    /**
     * Check if the key event is a media control key
     */
    fun isMediaKey(keyCode: Int): Boolean {
        return keyCode == MEDIA_PLAY_PAUSE ||
               keyCode == MEDIA_PLAY ||
               keyCode == MEDIA_PAUSE ||
               keyCode == MEDIA_FAST_FORWARD ||
               keyCode == MEDIA_REWIND
    }
    
    /**
     * Check if the key event is a navigation key
     */
    fun isNavigationKey(keyCode: Int): Boolean {
        return keyCode == DPAD_UP ||
               keyCode == DPAD_DOWN ||
               keyCode == DPAD_LEFT ||
               keyCode == DPAD_RIGHT
    }
    
    /**
     * Setup focus handling for a view with scale animation
     */
    fun setupFocusAnimation(view: View, scaleFactor: Float = 1.1f, duration: Long = 200) {
        view.isFocusable = true
        view.isFocusableInTouchMode = false
        
        view.setOnFocusChangeListener { v, hasFocus ->
            if (hasFocus) {
                v.animate()
                    .scaleX(scaleFactor)
                    .scaleY(scaleFactor)
                    .setDuration(duration)
                    .start()
                v.elevation = 8f
            } else {
                v.animate()
                    .scaleX(1.0f)
                    .scaleY(1.0f)
                    .setDuration(duration)
                    .start()
                v.elevation = 0f
            }
        }
    }
    
    /**
     * Setup focus handling for ImageButton with tint change
     */
    fun setupButtonFocusAnimation(button: ImageButton, context: Context, duration: Long = 200) {
        button.isFocusable = true
        button.isFocusableInTouchMode = false
        
        button.setOnFocusChangeListener { v, hasFocus ->
            val btn = v as ImageButton
            if (hasFocus) {
                btn.animate()
                    .scaleX(1.2f)
                    .scaleY(1.2f)
                    .setDuration(duration)
                    .start()
                btn.alpha = 1.0f
            } else {
                btn.animate()
                    .scaleX(1.0f)
                    .scaleY(1.0f)
                    .setDuration(duration)
                    .start()
                btn.alpha = 0.8f
            }
        }
    }
    
    /**
     * Setup RecyclerView for TV navigation
     */
    fun setupRecyclerViewForTv(recyclerView: RecyclerView) {
        recyclerView.isFocusable = true
        recyclerView.isFocusableInTouchMode = false
        recyclerView.descendantFocusability = ViewGroup.FOCUS_AFTER_DESCENDANTS
        
        // Ensure smooth scrolling when navigating
        recyclerView.setItemViewCacheSize(20)
        recyclerView.isNestedScrollingEnabled = false
    }
    
    /**
     * Request focus on the first focusable child of a ViewGroup
     */
    fun requestFocusOnFirstChild(viewGroup: ViewGroup): Boolean {
        for (i in 0 until viewGroup.childCount) {
            val child = viewGroup.getChildAt(i)
            if (child.isFocusable && child.visibility == View.VISIBLE) {
                return child.requestFocus()
            }
            if (child is ViewGroup) {
                if (requestFocusOnFirstChild(child)) {
                    return true
                }
            }
        }
        return false
    }
    
    /**
     * Get overscan-safe margins for TV displays
     * Returns margins in pixels [left, top, right, bottom]
     */
    fun getOverscanSafeMargins(context: Context): IntArray {
        if (!isTvMode(context)) {
            return intArrayOf(0, 0, 0, 0)
        }
        
        // Standard 5% overscan margin for TV
        val displayMetrics = context.resources.displayMetrics
        val horizontalMargin = (displayMetrics.widthPixels * 0.05).toInt()
        val verticalMargin = (displayMetrics.heightPixels * 0.05).toInt()
        
        return intArrayOf(horizontalMargin, verticalMargin, horizontalMargin, verticalMargin)
    }
    
    /**
     * Apply overscan-safe padding to a view
     */
    fun applyOverscanSafePadding(view: View, context: Context) {
        if (!isTvMode(context)) return
        
        val margins = getOverscanSafeMargins(context)
        view.setPadding(
            view.paddingLeft + margins[0],
            view.paddingTop + margins[1],
            view.paddingRight + margins[2],
            view.paddingBottom + margins[3]
        )
    }
    
    /**
     * Get recommended button size for TV in dp
     */
    fun getTvButtonSizeDp(): Int = 56
    
    /**
     * Get recommended icon size for TV in dp
     */
    fun getTvIconSizeDp(): Int = 32
    
    /**
     * Get recommended text size for TV in sp
     */
    fun getTvTextSizeSp(): Float = 18f
    
    /**
     * Get recommended title text size for TV in sp
     */
    fun getTvTitleTextSizeSp(): Float = 24f
}
