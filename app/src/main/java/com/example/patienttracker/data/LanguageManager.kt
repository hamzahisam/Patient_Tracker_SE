package com.example.patienttracker.data

import android.content.Context
import android.content.SharedPreferences
import android.content.res.Configuration
import android.content.res.Resources
import android.os.Build
import java.util.*

object LanguageManager {
    private const val PREFS_NAME = "language_prefs"
    private const val KEY_LANGUAGE = "app_language"
    
    private fun getSharedPreferences(context: Context): SharedPreferences {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }
    
    fun setAppLanguage(context: Context, languageCode: String): Context {
        saveLanguage(context, languageCode)
        return updateResources(context, languageCode)
    }
    
    fun getSavedLanguage(context: Context): String {
        val prefs = getSharedPreferences(context)
        return prefs.getString(KEY_LANGUAGE, "en") ?: "en"
    }
    
    private fun saveLanguage(context: Context, languageCode: String) {
        val prefs = getSharedPreferences(context)
        prefs.edit().putString(KEY_LANGUAGE, languageCode).apply()
    }
    
    private fun updateResources(context: Context, languageCode: String): Context {
        val locale = Locale(languageCode)
        Locale.setDefault(locale)
        
        val resources: Resources = context.resources
        val configuration: Configuration = resources.configuration
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            configuration.setLocale(locale)
        } else {
            configuration.locale = locale
        }
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N_MR1) {
            return context.createConfigurationContext(configuration)
        } else {
            resources.updateConfiguration(configuration, resources.displayMetrics)
            return context
        }
    }
}