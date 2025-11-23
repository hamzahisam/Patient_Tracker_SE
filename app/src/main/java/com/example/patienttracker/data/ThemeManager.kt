package com.example.patienttracker.data

import android.content.Context
import androidx.core.content.edit

object ThemeManager {

    private const val PREFS_NAME = "theme_prefs"
    private const val KEY_DARK_MODE = "dark_mode_enabled"

    /**
     * Returns true if dark mode is enabled.
     * Default = true (your app launches in dark mode for first-time users).
     */
    fun isDarkModeEnabled(context: Context): Boolean {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getBoolean(KEY_DARK_MODE, true)
    }

    /**
     * Stores the chosen theme mode.
     */
    fun setDarkModeEnabled(context: Context, enabled: Boolean) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit { putBoolean(KEY_DARK_MODE, enabled) }
    }

    /**
     * Optional helper: flips between dark and light mode.
     */
    fun toggleTheme(context: Context): Boolean {
        val current = isDarkModeEnabled(context)
        setDarkModeEnabled(context, !current)
        return !current
    }
}