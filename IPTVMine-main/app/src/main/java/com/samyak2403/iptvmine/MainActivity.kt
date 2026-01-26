package com.samyak2403.iptvmine

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.speech.RecognizerIntent
import android.util.Log
import android.view.KeyEvent
import android.view.View
import android.widget.EditText
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updateLayoutParams
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.viewpager2.adapter.FragmentStateAdapter
import androidx.viewpager2.widget.ViewPager2
import com.samyak2403.iptvmine.databinding.ActivityMainBinding
import com.samyak2403.iptvmine.notification.ChannelMonitorScheduler
import com.samyak2403.iptvmine.screens.AboutFragment
import com.samyak2403.iptvmine.screens.HomeFragment
import com.samyak2403.iptvmine.utils.BottomBarManager
import com.samyak2403.iptvmine.utils.BottomBarType
import com.samyak2403.iptvmine.utils.InAppReviewManager
import com.samyak2403.iptvmine.utils.InAppUpdateManager
import com.samyak2403.iptvmine.utils.TapTargetHelper
import com.samyak2403.iptvmine.utils.ThemeManager
import com.samyak2403.iptvmine.utils.TvUtils
import java.util.Locale


class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private var currentFragmentIndex = 0
    private var isSearchVisible = false
    private lateinit var inAppUpdateManager: InAppUpdateManager
    private lateinit var inAppReviewManager: InAppReviewManager
    private lateinit var viewPagerAdapter: ViewPagerAdapter
    private var currentBottomBarType: BottomBarType = BottomBarType.SMOOTH_BOTTOM_BAR

    companion object {
        private const val TAG = "MainActivity"
        private const val NOTIFICATION_PERMISSION_REQUEST_CODE = 1001
    }

    // Voice search launcher
    private val voiceSearchLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == RESULT_OK) {
            val matches = result.data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
            if (!matches.isNullOrEmpty()) {
                val voiceText = matches[0]
                binding.searchEditText.setText(voiceText)
            }
        }
    }

    // In-app update launcher
    private val updateLauncher: ActivityResultLauncher<IntentSenderRequest> = registerForActivityResult(
        ActivityResultContracts.StartIntentSenderForResult()
    ) { result ->
        inAppUpdateManager.handleUpdateResult(result.resultCode)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        ThemeManager.applyTheme(this)
        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        // Initialize in-app update manager
        inAppUpdateManager = InAppUpdateManager(this)
        
        // Initialize in-app review manager
        inAppReviewManager = InAppReviewManager(this)
        

        inAppReviewManager.incrementLaunchCount()
        inAppReviewManager.preWarmReview() // Pre-cache for faster display
        
        // Check for updates on app start
        checkForAppUpdates()
        
        // Setup custom status bar color
        setupStatusBar()
        
        // Handle window insets for edge-to-edge display
        ViewCompat.setOnApplyWindowInsetsListener(binding.main) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            
            // Only apply left, right, and bottom padding to main layout
            v.setPadding(systemBars.left, 0, systemBars.right, systemBars.bottom)
            
            // Set status bar background height to match system status bar
            // This prevents toolbar from overlapping with status bar
            binding.statusBarBackground.updateLayoutParams {
                height = systemBars.top
            }
            
            insets
        }

        setSupportActionBar(binding.toolbar)
        
        // Setup for TV mode
        if (TvUtils.isTvMode(this)) {
            // Make icons more accessible for D-pad navigation
            binding.searchIcon.isFocusable = true
            binding.micIcon.isFocusable = true
            
            // Add scale animation on focus for better UX
            binding.searchIcon.setOnFocusChangeListener { view, hasFocus ->
                if (hasFocus) {
                    view.animate().scaleX(1.1f).scaleY(1.1f).setDuration(200).start()
                } else {
                    view.animate().scaleX(1.0f).scaleY(1.0f).setDuration(200).start()
                }
            }
            
            binding.micIcon.setOnFocusChangeListener { view, hasFocus ->
                if (hasFocus) {
                    view.animate().scaleX(1.1f).scaleY(1.1f).setDuration(200).start()
                } else {
                    view.animate().scaleX(1.0f).scaleY(1.0f).setDuration(200).start()
                }
            }
        }
        
        // Setup search icon click
        binding.searchIcon.setOnClickListener {
            toggleSearch()
        }

        // Setup mic icon click for voice search
        binding.micIcon.setOnClickListener {
            startVoiceSearch()
        }

        // Setup close icon click
        binding.closeIcon.setOnClickListener {
            toggleSearch()
        }

        // Setup ViewPager2 with adapter
        setupViewPager()

        // Start automatic channel monitoring
        startAutomaticNotifications()

        // Setup bottom bar based on saved preference
        setupBottomBar()
        
        // Enhanced TV navigation for bottom bar
        if (TvUtils.isTvMode(this)) {
            setupTvBottomBarNavigation()
        }
        
        // Show onboarding for first-time users (delayed to ensure views are ready)
        binding.root.post {
            showOnboardingIfNeeded()
        }
    }

    /**
     * Show onboarding tutorial for first-time users
     */
    private fun showOnboardingIfNeeded() {
        if (!TvUtils.isTvMode(this) && !TapTargetHelper.isMainOnboardingShown(this)) {
            val bottomBarView = when (currentBottomBarType) {
                BottomBarType.SMOOTH_BOTTOM_BAR -> binding.smoothBottomBar
                BottomBarType.ANIMATED_BOTTOM_BAR -> binding.animatedBottomBar
                BottomBarType.CHIP_NAVIGATION_BAR -> binding.chipNavigationBar
            }
            
            bottomBarView?.let { bar ->
                TapTargetHelper.showMainOnboardingSequence(
                    activity = this,
                    searchIcon = binding.searchIcon,
                    micIcon = binding.micIcon,
                    bottomBar = bar,
                    onComplete = {
                        Log.d(TAG, "Onboarding completed")
                    }
                )
            }
        }
    }

    /**
     * Setup status bar with custom color
     * Sets status bar color to bg_color and handles light/dark status bar icons
     */
    private fun setupStatusBar() {
        window.apply {
            // Enable drawing behind the status bar
            statusBarColor = getColor(R.color.bg_color)
            
            // For API 30+, use WindowInsetsController for better control
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                // This will be handled by the theme's windowLightStatusBar attribute
                // which automatically adjusts icon colors based on status bar color
            } else {
                // For older APIs, manually set light status bar if needed
                @Suppress("DEPRECATION")
                decorView.systemUiVisibility = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    // Check if bg_color is light, then use dark icons
                    // For now, assuming dark background, so use light icons (default)
                    decorView.systemUiVisibility and View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR.inv()
                } else {
                    decorView.systemUiVisibility
                }
            }
        }
    }

    private fun toggleSearch() {
        isSearchVisible = !isSearchVisible
        if (isSearchVisible) {
            // Show search mode
            binding.searchEditText.visibility = View.VISIBLE
            binding.closeIcon.visibility = View.VISIBLE
            binding.toolbarTitle.visibility = View.GONE
            binding.searchIcon.visibility = View.GONE
            binding.micIcon.visibility = View.GONE
            binding.searchEditText.requestFocus()
        } else {
            // Show normal mode
            binding.searchEditText.visibility = View.GONE
            binding.closeIcon.visibility = View.GONE
            binding.toolbarTitle.visibility = View.VISIBLE
            binding.searchIcon.visibility = View.VISIBLE
            binding.micIcon.visibility = View.VISIBLE
            binding.searchEditText.text.clear()
        }
    }

    /**
     * Start voice search using Android's speech recognition
     */
    private fun startVoiceSearch() {
        try {
            val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault())
                putExtra(RecognizerIntent.EXTRA_PROMPT, "Speak to search...")
            }
            voiceSearchLauncher.launch(intent)
        } catch (e: Exception) {
            Toast.makeText(this, "Voice search not available", Toast.LENGTH_SHORT).show()
        }
    }

    private fun updateToolbarForFragment(index: Int) {
        when (index) {
            0 -> {
                binding.toolbarTitle.text = "IPTVmine"
                binding.searchIcon.visibility = View.VISIBLE
                binding.micIcon.visibility = View.VISIBLE
                binding.closeIcon.visibility = View.GONE
                binding.searchEditText.visibility = View.GONE
                isSearchVisible = false
            }
            1 -> {
                binding.toolbarTitle.text = "About"
                binding.searchIcon.visibility = View.GONE
                binding.micIcon.visibility = View.GONE
                binding.closeIcon.visibility = View.GONE
                binding.searchEditText.visibility = View.GONE
                isSearchVisible = false
            }
        }
    }

    fun getSearchEditText(): EditText = binding.searchEditText

    /**
     * Setup ViewPager2 with fragments and sync with bottom bar
     */
    private fun setupViewPager() {
        viewPagerAdapter = ViewPagerAdapter(this)
        binding.viewPager?.adapter = viewPagerAdapter
        
        // Update swipe enabled state based on preference
        updateSwipeEnabled()
        
        // Sync ViewPager with bottom bar
        binding.viewPager?.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                super.onPageSelected(position)
                if (currentFragmentIndex != position) {
                    currentFragmentIndex = position
                    updateBottomBarSelection(position)
                    updateToolbarForFragment(position)
                }
            }
        })
    }

    /**
     * Check if swipe gesture is enabled in preferences
     */
    private fun isSwipeGestureEnabled(): Boolean {
        val prefs = getSharedPreferences("app_prefs", MODE_PRIVATE)
        return prefs.getBoolean("swipe_gesture_enabled", true)
    }

    /**
     * Update ViewPager swipe enabled state based on preference
     */
    private fun updateSwipeEnabled() {
        binding.viewPager?.isUserInputEnabled = isSwipeGestureEnabled()
    }

    /**
     * ViewPager2 Adapter for fragments
     */
    private inner class ViewPagerAdapter(activity: FragmentActivity) : FragmentStateAdapter(activity) {
        override fun getItemCount(): Int = 2

        override fun createFragment(position: Int): Fragment {
            return when (position) {
                0 -> HomeFragment()
                1 -> AboutFragment()
                else -> HomeFragment()
            }
        }
    }

    /**
     * Setup bottom bar based on saved preference
     */
    private fun setupBottomBar() {
        currentBottomBarType = BottomBarManager.getSelectedBottomBar(this)
        showSelectedBottomBar(currentBottomBarType)
        setupBottomBarListeners()
    }

    /**
     * Show only the selected bottom bar and hide others
     */
    private fun showSelectedBottomBar(type: BottomBarType) {
        // Hide all bottom bars
        binding.smoothBottomBar?.visibility = View.GONE
        binding.animatedBottomBar?.visibility = View.GONE
        binding.chipNavigationBar?.visibility = View.GONE

        // Show selected bottom bar
        when (type) {
            BottomBarType.SMOOTH_BOTTOM_BAR -> {
                binding.smoothBottomBar?.visibility = View.VISIBLE
                binding.smoothBottomBar?.itemActiveIndex = currentFragmentIndex
            }
            BottomBarType.ANIMATED_BOTTOM_BAR -> {
                binding.animatedBottomBar?.visibility = View.VISIBLE
                binding.animatedBottomBar?.selectTabAt(currentFragmentIndex)
            }
            BottomBarType.CHIP_NAVIGATION_BAR -> {
                binding.chipNavigationBar?.visibility = View.VISIBLE
                binding.chipNavigationBar?.setItemSelected(
                    if (currentFragmentIndex == 0) R.id.home else R.id.about
                )
            }
        }
    }

    /**
     * Setup listeners for all bottom bars
     */
    private fun setupBottomBarListeners() {
        // Smooth Bottom Bar
        binding.smoothBottomBar?.setOnItemSelectedListener { position ->
            if (currentFragmentIndex != position) {
                binding.viewPager?.currentItem = position
            }
        }

        // Animated Bottom Bar
        binding.animatedBottomBar?.setOnTabSelectListener(
            object : nl.joery.animatedbottombar.AnimatedBottomBar.OnTabSelectListener {
                override fun onTabSelected(
                    lastIndex: Int,
                    lastTab: nl.joery.animatedbottombar.AnimatedBottomBar.Tab?,
                    newIndex: Int,
                    newTab: nl.joery.animatedbottombar.AnimatedBottomBar.Tab
                ) {
                    if (currentFragmentIndex != newIndex) {
                        binding.viewPager?.currentItem = newIndex
                    }
                }

                override fun onTabReselected(
                    index: Int,
                    tab: nl.joery.animatedbottombar.AnimatedBottomBar.Tab
                ) {}
            }
        )

        // Chip Navigation Bar
        binding.chipNavigationBar?.setOnItemSelectedListener { id ->
            val position = if (id == R.id.home) 0 else 1
            if (currentFragmentIndex != position) {
                binding.viewPager?.currentItem = position
            }
        }
    }

    /**
     * Update all bottom bars to show the current selected index
     */
    private fun updateBottomBarSelection(position: Int) {
        when (currentBottomBarType) {
            BottomBarType.SMOOTH_BOTTOM_BAR -> binding.smoothBottomBar?.itemActiveIndex = position
            BottomBarType.ANIMATED_BOTTOM_BAR -> binding.animatedBottomBar?.selectTabAt(position)
            BottomBarType.CHIP_NAVIGATION_BAR -> binding.chipNavigationBar?.setItemSelected(
                if (position == 0) R.id.home else R.id.about
            )
        }
    }



    /**
     * Start automatic channel monitoring and notifications
     * This runs automatically when app starts - no user action needed!
     */
    private fun startAutomaticNotifications() {
        Log.d(TAG, "Starting automatic channel notifications...")

        // Request notification permission for Android 13+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                ActivityCompat.requestPermissions(
                    this,
                    arrayOf(Manifest.permission.POST_NOTIFICATIONS),
                    NOTIFICATION_PERMISSION_REQUEST_CODE
                )
            } else {
                scheduleChannelMonitoring()
            }
        } else {
            scheduleChannelMonitoring()
        }
    }

    private fun scheduleChannelMonitoring() {
        // Schedule automatic monitoring
        ChannelMonitorScheduler.scheduleMonitoring(this)
        Log.d(TAG, "Automatic channel monitoring started successfully!")
        
        // Check if this is first time showing notification message
        val prefs = getSharedPreferences("app_prefs", MODE_PRIVATE)
        val hasShownNotificationMessage = prefs.getBoolean("notification_message_shown", false)
        
        if (!hasShownNotificationMessage) {
            // Show toast only first time
            Toast.makeText(
                this,
                "Automatic channel notifications enabled",
                Toast.LENGTH_SHORT
            ).show()
            
            // Mark as shown
            prefs.edit().putBoolean("notification_message_shown", true).apply()
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        
        if (requestCode == NOTIFICATION_PERMISSION_REQUEST_CODE) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                scheduleChannelMonitoring()
            } else {
                Toast.makeText(
                    this,
                    "Notification permission denied. You won't receive channel alerts.",
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }

    override fun onResume() {
        super.onResume()
        // Check if there's a downloaded update waiting to be installed
        inAppUpdateManager.checkForDownloadedUpdate()
        
        // Request in-app review if conditions are met
        inAppReviewManager.requestReviewIfAppropriate()
        
        // Update swipe enabled state (in case changed in Settings)
        updateSwipeEnabled()
        
        // Check if bottom bar type changed in Settings
        val newBottomBarType = BottomBarManager.getSelectedBottomBar(this)
        if (newBottomBarType != currentBottomBarType) {
            currentBottomBarType = newBottomBarType
            showSelectedBottomBar(newBottomBarType)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        // Unregister update listener to prevent memory leaks
        inAppUpdateManager.unregisterListener()
    }

    /**
     * Check for app updates using Google Play In-App Updates API
     * Supports both immediate and flexible update flows
     */
    private fun checkForAppUpdates() {
        Log.d(TAG, "Checking for app updates...")
        inAppUpdateManager.checkForUpdate(
            updateResultLauncher = updateLauncher,
            preferImmediate = false // Set to true for immediate updates
        )
    }

    /**
     * Setup TV-specific bottom bar navigation with D-pad support
     */
    private fun setupTvBottomBarNavigation() {
        binding.smoothBottomBar?.isFocusable = true
        binding.smoothBottomBar?.isFocusableInTouchMode = false
        
        // Add focus change listener for visual feedback
        binding.bottomBarContainer?.setOnFocusChangeListener { view, hasFocus ->
            if (hasFocus) {
                view.animate().scaleY(1.05f).setDuration(200).start()
                view.elevation = 8f
            } else {
                view.animate().scaleY(1.0f).setDuration(200).start()
                view.elevation = 0f
            }
        }
        
        // Handle D-pad left/right navigation
        binding.bottomBarContainer?.setOnKeyListener { _, keyCode, event ->
            if (event.action == android.view.KeyEvent.ACTION_DOWN) {
                when (keyCode) {
                    android.view.KeyEvent.KEYCODE_DPAD_LEFT -> {
                        if (currentFragmentIndex > 0) {
                            binding.viewPager?.currentItem = currentFragmentIndex - 1
                            true
                        } else false
                    }
                    android.view.KeyEvent.KEYCODE_DPAD_RIGHT -> {
                        if (currentFragmentIndex < 1) {
                            binding.viewPager?.currentItem = currentFragmentIndex + 1
                            true
                        } else false
                    }
                    else -> false
                }
            } else false
        }
    }
    
    /**
     * Handle key events for TV navigation
     */
    override fun dispatchKeyEvent(event: KeyEvent): Boolean {
        if (TvUtils.isTvMode(this) && event.action == KeyEvent.ACTION_DOWN) {
            when (event.keyCode) {
                // Menu key shows/hides search on TV
                KeyEvent.KEYCODE_MENU -> {
                    if (currentFragmentIndex == 0) {
                        toggleSearch()
                        return true
                    }
                }
            }
        }
        return super.dispatchKeyEvent(event)
    }

    override fun onBackPressed() {
        if (isSearchVisible) {
            toggleSearch()
        } else if (currentFragmentIndex != 0) {
            // If not on home, go back to home
            binding.viewPager?.currentItem = 0
        } else {
            // If on home, exit app
            super.onBackPressed()
        }
    }
}
