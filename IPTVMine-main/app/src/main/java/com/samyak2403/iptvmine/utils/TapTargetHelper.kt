package com.samyak2403.iptvmine.utils

import android.app.Activity
import android.content.Context
import android.view.View
import androidx.core.content.ContextCompat
import com.getkeepsafe.taptargetview.TapTarget
import com.getkeepsafe.taptargetview.TapTargetSequence
import com.getkeepsafe.taptargetview.TapTargetView
import com.samyak2403.iptvmine.R

/**
 * Helper class for TapTargetView feature discovery/onboarding
 */
object TapTargetHelper {

    private const val PREF_NAME = "tap_target_prefs"
    private const val KEY_MAIN_ONBOARDING_SHOWN = "main_onboarding_shown"
    private const val KEY_SETTINGS_ONBOARDING_SHOWN = "settings_onboarding_shown"
    private const val KEY_BOTTOM_BAR_SELECTION_ONBOARDING_SHOWN = "bottom_bar_selection_onboarding_shown"

    /**
     * Check if main activity onboarding has been shown
     */
    fun isMainOnboardingShown(context: Context): Boolean {
        val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        return prefs.getBoolean(KEY_MAIN_ONBOARDING_SHOWN, false)
    }

    /**
     * Mark main activity onboarding as shown
     */
    fun setMainOnboardingShown(context: Context) {
        val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        prefs.edit().putBoolean(KEY_MAIN_ONBOARDING_SHOWN, true).apply()
    }

    /**
     * Check if settings onboarding has been shown
     */
    fun isSettingsOnboardingShown(context: Context): Boolean {
        val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        return prefs.getBoolean(KEY_SETTINGS_ONBOARDING_SHOWN, false)
    }

    /**
     * Mark settings onboarding as shown
     */
    fun setSettingsOnboardingShown(context: Context) {
        val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        prefs.edit().putBoolean(KEY_SETTINGS_ONBOARDING_SHOWN, true).apply()
    }

    /**
     * Check if bottom bar selection onboarding has been shown
     */
    fun isBottomBarSelectionOnboardingShown(context: Context): Boolean {
        val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        return prefs.getBoolean(KEY_BOTTOM_BAR_SELECTION_ONBOARDING_SHOWN, false)
    }

    /**
     * Mark bottom bar selection onboarding as shown
     */
    fun setBottomBarSelectionOnboardingShown(context: Context) {
        val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        prefs.edit().putBoolean(KEY_BOTTOM_BAR_SELECTION_ONBOARDING_SHOWN, true).apply()
    }

    /**
     * Reset all onboarding flags (for testing)
     */
    fun resetOnboarding(context: Context) {
        val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        prefs.edit().clear().apply()
    }


    /**
     * Show a single TapTarget on a view
     */
    fun showSingleTarget(
        activity: Activity,
        targetView: View,
        title: String,
        description: String,
        outerCircleColor: Int = R.color.colorPrimary,
        targetCircleColor: Int = android.R.color.white,
        onTargetClick: (() -> Unit)? = null,
        onTargetCancel: (() -> Unit)? = null
    ) {
        TapTargetView.showFor(
            activity,
            TapTarget.forView(targetView, title, description)
                .outerCircleColor(outerCircleColor)
                .outerCircleAlpha(0.96f)
                .targetCircleColor(targetCircleColor)
                .titleTextSize(22)
                .titleTextColor(android.R.color.white)
                .descriptionTextSize(16)
                .descriptionTextColor(android.R.color.white)
                .textColor(android.R.color.white)
                .dimColor(android.R.color.black)
                .drawShadow(true)
                .cancelable(true)
                .tintTarget(true)
                .transparentTarget(false)
                .targetRadius(60),
            object : TapTargetView.Listener() {
                override fun onTargetClick(view: TapTargetView) {
                    super.onTargetClick(view)
                    onTargetClick?.invoke()
                }

                override fun onTargetCancel(view: TapTargetView) {
                    super.onTargetCancel(view)
                    onTargetCancel?.invoke()
                }
            }
        )
    }

    /**
     * Show main activity onboarding sequence
     */
    fun showMainOnboardingSequence(
        activity: Activity,
        searchIcon: View,
        micIcon: View,
        bottomBar: View,
        onComplete: () -> Unit
    ) {
        TapTargetSequence(activity)
            .targets(
                TapTarget.forView(searchIcon, "Search Channels", "Tap here to search for your favorite channels")
                    .outerCircleColor(R.color.bg_color)
                    .outerCircleAlpha(0.96f)
                    .targetCircleColor(android.R.color.white)
                    .titleTextSize(22)
                    .titleTextColor(android.R.color.white)
                    .descriptionTextSize(16)
                    .descriptionTextColor(android.R.color.white)
                    .dimColor(android.R.color.black)
                    .drawShadow(true)
                    .cancelable(true)
                    .tintTarget(true)
                    .targetRadius(50)
                    .id(1),

                TapTarget.forView(micIcon, "Voice Search", "Use voice to search channels hands-free")
                    .outerCircleColor(R.color.colorAccent)
                    .outerCircleAlpha(0.96f)
                    .targetCircleColor(android.R.color.white)
                    .titleTextSize(22)
                    .titleTextColor(android.R.color.white)
                    .descriptionTextSize(16)
                    .descriptionTextColor(android.R.color.white)
                    .dimColor(android.R.color.black)
                    .drawShadow(true)
                    .cancelable(true)
                    .tintTarget(true)
                    .targetRadius(50)
                    .id(2),

                TapTarget.forView(bottomBar, "Swipe Navigation", "Swipe left/right or tap to switch between Home and About")
                    .outerCircleColor(R.color.colorPrimary)
                    .outerCircleAlpha(0.96f)
                    .targetCircleColor(android.R.color.white)
                    .titleTextSize(22)
                    .titleTextColor(android.R.color.white)
                    .descriptionTextSize(16)
                    .descriptionTextColor(android.R.color.white)
                    .dimColor(android.R.color.black)
                    .drawShadow(true)
                    .cancelable(true)
                    .tintTarget(false)
                    .transparentTarget(true)
                    .targetRadius(80)
                    .id(3)
            )
            .listener(object : TapTargetSequence.Listener {
                override fun onSequenceFinish() {
                    setMainOnboardingShown(activity)
                    onComplete()
                }

                override fun onSequenceStep(lastTarget: TapTarget, targetClicked: Boolean) {
                    // Handle each step if needed
                }

                override fun onSequenceCanceled(lastTarget: TapTarget) {
                    setMainOnboardingShown(activity)
                    onComplete()
                }
            })
            .start()
    }


    /**
     * Show settings onboarding for bottom bar selection
     */
    fun showSettingsOnboarding(
        activity: Activity,
        bottomBarSelectionView: View,
        onComplete: () -> Unit
    ) {
        TapTargetView.showFor(
            activity,
            TapTarget.forView(
                bottomBarSelectionView,
                "Customize Bottom Bar",
                "Choose from different bottom bar styles to personalize your app experience"
            )
                .outerCircleColor(R.color.colorPrimary)
                .outerCircleAlpha(0.96f)
                .targetCircleColor(android.R.color.white)
                .titleTextSize(22)
                .titleTextColor(android.R.color.white)
                .descriptionTextSize(16)
                .descriptionTextColor(android.R.color.white)
                .dimColor(android.R.color.black)
                .drawShadow(true)
                .cancelable(true)
                .tintTarget(false)
                .transparentTarget(true)
                .targetRadius(70),
            object : TapTargetView.Listener() {
                override fun onTargetClick(view: TapTargetView) {
                    super.onTargetClick(view)
                    setSettingsOnboardingShown(activity)
                    onComplete()
                }

                override fun onTargetCancel(view: TapTargetView) {
                    super.onTargetCancel(view)
                    setSettingsOnboardingShown(activity)
                    onComplete()
                }
            }
        )
    }

    /**
     * Create a custom TapTarget for toolbar menu items
     */
    fun createToolbarTarget(
        activity: Activity,
        toolbar: androidx.appcompat.widget.Toolbar,
        menuItemId: Int,
        title: String,
        description: String
    ): TapTarget {
        return TapTarget.forToolbarMenuItem(toolbar, menuItemId, title, description)
            .outerCircleColor(R.color.colorPrimary)
            .outerCircleAlpha(0.96f)
            .targetCircleColor(android.R.color.white)
            .titleTextSize(22)
            .titleTextColor(android.R.color.white)
            .descriptionTextSize(16)
            .descriptionTextColor(android.R.color.white)
            .dimColor(android.R.color.black)
            .drawShadow(true)
            .cancelable(true)
            .tintTarget(true)
            .targetRadius(50)
    }

    /**
     * Show bottom bar selection onboarding
     */
    fun showBottomBarSelectionOnboarding(
        activity: Activity,
        firstItemView: View,
        onComplete: () -> Unit
    ) {
        TapTargetView.showFor(
            activity,
            TapTarget.forView(
                firstItemView,
                "Choose Your Style",
                "Tap on any style to preview and apply it. The selected style will be highlighted with a checkmark."
            )
                .outerCircleColor(R.color.colorPrimary)
                .outerCircleAlpha(0.96f)
                .targetCircleColor(android.R.color.white)
                .titleTextSize(22)
                .titleTextColor(android.R.color.white)
                .descriptionTextSize(16)
                .descriptionTextColor(android.R.color.white)
                .dimColor(android.R.color.black)
                .drawShadow(true)
                .cancelable(true)
                .tintTarget(false)
                .transparentTarget(true)
                .targetRadius(80),
            object : TapTargetView.Listener() {
                override fun onTargetClick(view: TapTargetView) {
                    super.onTargetClick(view)
                    setBottomBarSelectionOnboardingShown(activity)
                    onComplete()
                }

                override fun onTargetCancel(view: TapTargetView) {
                    super.onTargetCancel(view)
                    setBottomBarSelectionOnboardingShown(activity)
                    onComplete()
                }
            }
        )
    }

    /**
     * Show feature highlight for new features
     */
    fun showNewFeatureHighlight(
        activity: Activity,
        targetView: View,
        featureName: String,
        featureDescription: String,
        onDismiss: () -> Unit
    ) {
        TapTargetView.showFor(
            activity,
            TapTarget.forView(targetView, "New: $featureName", featureDescription)
                .outerCircleColor(R.color.colorAccent)
                .outerCircleAlpha(0.96f)
                .targetCircleColor(android.R.color.white)
                .titleTextSize(24)
                .titleTextColor(android.R.color.white)
                .descriptionTextSize(16)
                .descriptionTextColor(android.R.color.white)
                .dimColor(android.R.color.black)
                .drawShadow(true)
                .cancelable(true)
                .tintTarget(true)
                .targetRadius(60),
            object : TapTargetView.Listener() {
                override fun onTargetClick(view: TapTargetView) {
                    super.onTargetClick(view)
                    onDismiss()
                }

                override fun onTargetCancel(view: TapTargetView) {
                    super.onTargetCancel(view)
                    onDismiss()
                }
            }
        )
    }
}
