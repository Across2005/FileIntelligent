package com.crossk.data

import android.content.Context
import android.content.SharedPreferences

/**
 * Lightweight wrapper around SharedPreferences for app settings.
 * Tracks onboarding completion, theme preference, and other user preferences.
 */
class PreferenceManager(context: Context) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences("crossk_prefs", Context.MODE_PRIVATE)

    var onboardingCompleted: Boolean
        get() = prefs.getBoolean(KEY_ONBOARDING_DONE, false)
        set(value) = prefs.edit().putBoolean(KEY_ONBOARDING_DONE, value).apply()

    companion object {
        private const val KEY_ONBOARDING_DONE = "onboarding_completed"
    }
}
