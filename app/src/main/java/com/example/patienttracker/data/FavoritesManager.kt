package com.example.patienttracker.data

import android.content.Context
import android.content.SharedPreferences
import com.example.patienttracker.auth.AuthManager
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.runBlocking

object FavoritesManager {
    private const val PREFS_NAME = "favorites_prefs"
    private const val KEY_FAVORITES = "favorite_doctors"
    
    private fun getSharedPreferences(context: Context): SharedPreferences {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }
    
    private fun getCurrentUserId(): String? {
        return runBlocking {
            AuthManager.getCurrentUserProfile()?.humanId
        }
    }
    
    fun addFavorite(context: Context, doctorId: String) {
        val userId = getCurrentUserId() ?: return
        
        val favorites = getFavorites(context, userId).toMutableSet()
        favorites.add(doctorId)
        saveFavorites(context, userId, favorites)
    }
    
    fun removeFavorite(context: Context, doctorId: String) {
        val userId = getCurrentUserId() ?: return
        
        val favorites = getFavorites(context, userId).toMutableSet()
        favorites.remove(doctorId)
        saveFavorites(context, userId, favorites)
    }
    
    fun isFavorite(context: Context, doctorId: String): Boolean {
        val userId = getCurrentUserId() ?: return false
        
        return getFavorites(context, userId).contains(doctorId)
    }
    
    fun getFavorites(context: Context, userId: String): Set<String> {
        val prefs = getSharedPreferences(context)
        val favoritesJson = prefs.getString("${KEY_FAVORITES}_$userId", null)
        
        return if (favoritesJson != null) {
            val type = object : TypeToken<Set<String>>() {}.type
            Gson().fromJson(favoritesJson, type) ?: emptySet()
        } else {
            emptySet()
        }
    }
    
    private fun saveFavorites(context: Context, userId: String, favorites: Set<String>) {
        val prefs = getSharedPreferences(context)
        val editor = prefs.edit()
        val favoritesJson = Gson().toJson(favorites)
        editor.putString("${KEY_FAVORITES}_$userId", favoritesJson)
        editor.apply()
    }
}