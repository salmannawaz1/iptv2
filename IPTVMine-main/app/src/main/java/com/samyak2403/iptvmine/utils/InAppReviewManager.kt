package com.samyak2403.iptvmine.utils

import android.app.Activity
import android.content.Context
import android.util.Log
import com.google.android.play.core.review.ReviewInfo
import com.google.android.play.core.review.ReviewManager
import com.google.android.play.core.review.ReviewManagerFactory

/**
 * Manager class for handling Google Play In-App Reviews
 * 
 * Best practices:
 * - Don't prompt too frequently (Google may not show the dialog)
 * - Trigger after positive user experiences (e.g., completing a task, watching content)
 * - Don't interrupt critical user flows
 */
class InAppReviewManager(private val activity: Activity) {

    private val reviewManager: ReviewManager = ReviewManagerFactory.create(activity)
    private var reviewInfo: ReviewInfo? = null
    
    companion object {
        private const val TAG = "InAppReviewManager"
        private const val PREFS_NAME = "in_app_review_prefs"
        private const val KEY_LAST_REVIEW_TIME = "last_review_time"
        private const val KEY_LAUNCH_COUNT = "launch_count"
        private const val KEY_REVIEW_REQUESTED = "review_requested"
        
        // Minimum days between review prompts
        private const val MIN_DAYS_BETWEEN_REVIEWS = 30
        // Minimum app launches before showing review
        private const val MIN_LAUNCHES_BEFORE_REVIEW = 5
    }

    private val prefs by lazy {
        activity.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    /**
     * Pre-cache the review info for faster display later
     * Call this early (e.g., in onCreate) so the review dialog loads quickly when needed
     */
    fun preWarmReview() {
        reviewManager.requestReviewFlow().addOnCompleteListener { task ->
            if (task.isSuccessful) {
                reviewInfo = task.result
                Log.d(TAG, "Review info pre-cached successfully")
            } else {
                Log.e(TAG, "Failed to pre-cache review info", task.exception)
            }
        }
    }

    /**
     * Request and launch the in-app review flow
     * Note: Google controls when the review dialog actually appears
     * 
     * @param onComplete Callback when the flow completes (doesn't indicate if review was submitted)
     */
    fun requestReview(onComplete: (() -> Unit)? = null) {
        if (reviewInfo != null) {
            launchReviewFlow(reviewInfo!!, onComplete)
        } else {
            // Request review info if not pre-cached
            reviewManager.requestReviewFlow().addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    launchReviewFlow(task.result, onComplete)
                } else {
                    Log.e(TAG, "Failed to request review flow", task.exception)
                    onComplete?.invoke()
                }
            }
        }
    }

    /**
     * Launch the review flow with the given ReviewInfo
     */
    private fun launchReviewFlow(info: ReviewInfo, onComplete: (() -> Unit)?) {
        reviewManager.launchReviewFlow(activity, info).addOnCompleteListener { task ->
            // The flow has finished. The API does not indicate whether the user
            // reviewed or not, or even whether the review dialog was shown.
            if (task.isSuccessful) {
                Log.d(TAG, "Review flow completed")
                markReviewRequested()
            } else {
                Log.e(TAG, "Review flow failed", task.exception)
            }
            // Clear cached info as it can only be used once
            reviewInfo = null
            onComplete?.invoke()
        }
    }

    /**
     * Check if it's appropriate to request a review based on usage patterns
     * Call this to determine if you should trigger the review flow
     */
    fun shouldRequestReview(): Boolean {
        val launchCount = prefs.getInt(KEY_LAUNCH_COUNT, 0)
        val lastReviewTime = prefs.getLong(KEY_LAST_REVIEW_TIME, 0)
        val hasRequestedBefore = prefs.getBoolean(KEY_REVIEW_REQUESTED, false)
        
        val daysSinceLastReview = if (lastReviewTime > 0) {
            (System.currentTimeMillis() - lastReviewTime) / (1000 * 60 * 60 * 24)
        } else {
            Long.MAX_VALUE
        }
        
        return when {
            // First time: wait for minimum launches
            !hasRequestedBefore && launchCount >= MIN_LAUNCHES_BEFORE_REVIEW -> true
            // Subsequent times: respect minimum days between reviews
            hasRequestedBefore && daysSinceLastReview >= MIN_DAYS_BETWEEN_REVIEWS -> true
            else -> false
        }
    }

    /**
     * Increment the app launch counter
     * Call this in your main activity's onCreate
     */
    fun incrementLaunchCount() {
        val currentCount = prefs.getInt(KEY_LAUNCH_COUNT, 0)
        prefs.edit().putInt(KEY_LAUNCH_COUNT, currentCount + 1).apply()
        Log.d(TAG, "Launch count: ${currentCount + 1}")
    }

    /**
     * Mark that a review has been requested
     */
    private fun markReviewRequested() {
        prefs.edit()
            .putBoolean(KEY_REVIEW_REQUESTED, true)
            .putLong(KEY_LAST_REVIEW_TIME, System.currentTimeMillis())
            .apply()
    }

    /**
     * Request review only if conditions are met
     * Combines shouldRequestReview() check with requestReview()
     * 
     * @param onComplete Callback when complete (or skipped)
     */
    fun requestReviewIfAppropriate(onComplete: (() -> Unit)? = null) {
        if (shouldRequestReview()) {
            Log.d(TAG, "Conditions met, requesting review")
            requestReview(onComplete)
        } else {
            Log.d(TAG, "Conditions not met for review request")
            onComplete?.invoke()
        }
    }

    /**
     * Force request review regardless of conditions
     * Use sparingly - for testing or special occasions
     */
    fun forceRequestReview(onComplete: (() -> Unit)? = null) {
        Log.d(TAG, "Force requesting review")
        requestReview(onComplete)
    }

    /**
     * Reset all review tracking data
     * Useful for testing
     */
    fun resetReviewData() {
        prefs.edit().clear().apply()
        Log.d(TAG, "Review data reset")
    }
}
