package com.example.patienttracker.ui.theme

import android.content.Context
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import com.example.patienttracker.data.ThemeManager

// Light Color Scheme (Your current theme)
private val LightColorScheme = lightColorScheme(
    primary = Color(0xFF0EA5B8),
    secondary = Color(0xFF4CB7C2),
    tertiary = Color(0xFF6AA8B0),
    background = Color(0xFFF6F8FC),
    surface = Color(0xFFFFFFFF),
    onPrimary = Color.White,
    onSecondary = Color.White,
    onBackground = Color(0xFF1C3D5A),
    onSurface = Color(0xFF1C3D5A),
)

// Dark Color Scheme
private val DarkColorScheme = darkColorScheme(
    primary = Color(0xFF4FD8E6),
    secondary = Color(0xFF66C2CD),
    tertiary = Color(0xFF88B4BC),
    background = Color(0xFF121212),
    surface = Color(0xFF1E1E1E),
    onPrimary = Color.Black,
    onSecondary = Color.Black,
    onBackground = Color(0xFFE1E1E1),
    onSurface = Color(0xFFE1E1E1),
)

// Theme state management
data class AppThemeState(
    val isDarkMode: Boolean = false,
    val toggleTheme: () -> Unit = {}
)

val LocalAppTheme = staticCompositionLocalOf { AppThemeState() }

@Composable
fun PatientTrackerTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit
) {
    val context = LocalContext.current
    val themeState = rememberThemeState(context)
    
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val dynamicDark = dynamicDarkColorScheme(context)
            val dynamicLight = dynamicLightColorScheme(context)
            if (themeState.isDarkMode) dynamicDark else dynamicLight
        }
        themeState.isDarkMode -> DarkColorScheme
        else -> LightColorScheme
    }

    CompositionLocalProvider(LocalAppTheme provides themeState) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = Typography,
            content = content
        )
    }
}

@Composable
fun rememberThemeState(context: android.content.Context): AppThemeState {
    var isDarkMode by remember { 
        mutableStateOf(ThemeManager.isDarkModeEnabled(context)) 
    }
    
    return remember {
        AppThemeState(
            isDarkMode = isDarkMode,
            toggleTheme = { 
                isDarkMode = !isDarkMode
                ThemeManager.setDarkModeEnabled(context, isDarkMode)
            }
        )
    }
}