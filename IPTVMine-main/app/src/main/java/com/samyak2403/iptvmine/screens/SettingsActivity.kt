package com.samyak2403.iptvmine.screens

import android.content.Intent
import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updateLayoutParams
import com.samyak2403.iptvmine.databinding.ActivitySettingsBinding
import com.samyak2403.iptvmine.utils.BottomBarManager
import com.samyak2403.iptvmine.utils.TapTargetHelper
import com.samyak2403.iptvmine.utils.ThemeManager

class SettingsActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySettingsBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        ThemeManager.applyTheme(this)
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        
        binding = ActivitySettingsBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        setupEdgeToEdge()
        setupToolbar()
        setupViews()
        setupClickListeners()
    }

    private fun setupEdgeToEdge() {
        ViewCompat.setOnApplyWindowInsetsListener(binding.main) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, 0, systemBars.right, systemBars.bottom)
            binding.statusBarBackground.updateLayoutParams {
                height = systemBars.top
            }
            insets
        }
    }

    private fun setupToolbar() {
        setSupportActionBar(binding.toolbar)
        supportActionBar?.apply {
            setDisplayHomeAsUpEnabled(true)
            setDisplayShowHomeEnabled(true)
            title = "Settings"
        }
        binding.toolbar.setNavigationOnClickListener { onBackPressed() }
    }

    private fun setupViews() {
        val prefs = getSharedPreferences("app_prefs", MODE_PRIVATE)
        
        // Swipe gesture switch
        val swipeEnabled = prefs.getBoolean("swipe_gesture_enabled", true)
        binding.switchSwipeGesture.setChecked(swipeEnabled)
        
        // Notifications switch
        val notificationsEnabled = prefs.getBoolean("notifications_enabled", true)
        binding.switchNotifications.setChecked(notificationsEnabled)
        
        // Data saving mode switch
        val dataSavingEnabled = prefs.getBoolean("data_saving_enabled", false)
        binding.switchDataSaving.setChecked(dataSavingEnabled)
        
        // Update bottom bar selection text
        updateBottomBarSelectionText()
        
        // Show onboarding for bottom bar selection (delayed)
        binding.root.post {
            showOnboardingIfNeeded()
        }
    }

    /**
     * Show onboarding for settings features
     */
    private fun showOnboardingIfNeeded() {
        if (!TapTargetHelper.isSettingsOnboardingShown(this)) {
            TapTargetHelper.showSettingsOnboarding(
                activity = this,
                bottomBarSelectionView = binding.cardBottomBarSelection,
                onComplete = {
                    // Onboarding completed
                }
            )
        }
    }
    
    override fun onResume() {
        super.onResume()
        updateBottomBarSelectionText()
    }
    
    private fun updateBottomBarSelectionText() {
        val selectedBar = BottomBarManager.getSelectedBottomBar(this)
        binding.bottomBarSelectedText.text = selectedBar.displayName
    }

    private fun setupClickListeners() {
        // Swipe gesture switch
        binding.switchSwipeGesture.setOnCheckChangeListener(
            object : com.samyak.custom_switch.MaterialCustomSwitch.OnCheckChangeListener {
                override fun onCheckChanged(isChecked: Boolean) {
                    val prefs = getSharedPreferences("app_prefs", MODE_PRIVATE)
                    prefs.edit().putBoolean("swipe_gesture_enabled", isChecked).apply()
                    
                    val message = if (isChecked) "Swipe navigation enabled" else "Swipe navigation disabled"
                    android.widget.Toast.makeText(this@SettingsActivity, message, android.widget.Toast.LENGTH_SHORT).show()
                }
            }
        )

        // Notifications switch
        binding.switchNotifications.setOnCheckChangeListener(
            object : com.samyak.custom_switch.MaterialCustomSwitch.OnCheckChangeListener {
                override fun onCheckChanged(isChecked: Boolean) {
                    val prefs = getSharedPreferences("app_prefs", MODE_PRIVATE)
                    prefs.edit().putBoolean("notifications_enabled", isChecked).apply()
                    
                    val message = if (isChecked) "Channel notifications enabled" else "Channel notifications disabled"
                    android.widget.Toast.makeText(this@SettingsActivity, message, android.widget.Toast.LENGTH_SHORT).show()
                    
                    if (isChecked) {
                        com.samyak2403.iptvmine.notification.ChannelMonitorScheduler.scheduleMonitoring(this@SettingsActivity)
                    } else {
                        com.samyak2403.iptvmine.notification.ChannelMonitorScheduler.cancelMonitoring(this@SettingsActivity)
                    }
                }
            }
        )

        // Data saving mode switch
        binding.switchDataSaving.setOnCheckChangeListener(
            object : com.samyak.custom_switch.MaterialCustomSwitch.OnCheckChangeListener {
                override fun onCheckChanged(isChecked: Boolean) {
                    val prefs = getSharedPreferences("app_prefs", MODE_PRIVATE)
                    prefs.edit().putBoolean("data_saving_enabled", isChecked).apply()
                    
                    val message = if (isChecked) "Data saving mode enabled - Lower picture quality" else "Data saving mode disabled - Normal quality"
                    android.widget.Toast.makeText(this@SettingsActivity, message, android.widget.Toast.LENGTH_SHORT).show()
                }
            }
        )
        
        // Bottom bar selection - open selection activity
        binding.cardBottomBarSelection.setOnClickListener {
            startActivity(Intent(this, BottomBarSelectionActivity::class.java))
        }
        
        // Feedback - open feedback form
        binding.cardFeedback.setOnClickListener {
            startActivity(Intent(this, FeedbackActivity::class.java))
        }
    }
}
