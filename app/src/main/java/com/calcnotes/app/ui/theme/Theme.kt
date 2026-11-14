package com.calcnotes.app.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.calcnotes.app.data.AppThemeMode

data class AccentPalette(
    val name: String,
    val primary: Color,
    val secondary: Color,
    val glow: Color
)

val AccentPalettes = listOf(
    AccentPalette("Cyber Cyan", Color(0xFF38BDF8), Color(0xFF818CF8), Color(0xFF0284C7)),
    AccentPalette("Electric Violet", Color(0xFFA855F7), Color(0xFFF472B6), Color(0xFF7E22CE)),
    AccentPalette("Neon Emerald", Color(0xFF34D399), Color(0xFF2DD4BF), Color(0xFF059669)),
    AccentPalette("Coral Rose", Color(0xFFFB7185), Color(0xFFFBBF24), Color(0xFFE11D48)),
    AccentPalette("Solar Amber", Color(0xFFFBBF24), Color(0xFFFB923C), Color(0xFFD97706))
)

val NoteTagColors = listOf(
    Color(0xFF38BDF8),
    Color(0xFFA855F7),
    Color(0xFF34D399),
    Color(0xFFFB7185),
    Color(0xFFFBBF24),
    Color(0xFF60A5FA)
)

data class GlassColors(
    val isDark: Boolean,
    val isAmoled: Boolean,
    val surfaceTranslucent: Color,
    val surfaceFallback: Color,
    val borderLight: Color,
    val textPrimary: Color,
    val textDimmed: Color,
    val accent: Color,
    val accentSecondary: Color,
    val resultChipBg: Color
)

val LocalGlassColors = staticCompositionLocalOf {
    GlassColors(
        isDark = true,
        isAmoled = false,
        surfaceTranslucent = Color(0x28FFFFFF),
        surfaceFallback = Color(0xE6151C2C),
        borderLight = Color(0x38FFFFFF),
        textPrimary = Color(0xFFF8FAFC),
        textDimmed = Color(0xFF94A3B8),
        accent = Color(0xFF38BDF8),
        accentSecondary = Color(0xFF818CF8),
        resultChipBg = Color(0x2638BDF8)
    )
}

val CalcNotesTypography = Typography(
    headlineMedium = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Bold,
        fontSize = 24.sp
    ),
    titleMedium = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.SemiBold,
        fontSize = 17.sp
    ),
    bodyLarge = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp
    ),
    labelMedium = TextStyle(
        fontFamily = FontFamily.Monospace,
        fontWeight = FontWeight.Medium,
        fontSize = 14.sp,
        fontFeatureSettings = "tnum"
    )
)

@Composable
fun CalcNotesTheme(
    themeMode: AppThemeMode = AppThemeMode.DARK,
    accentIndex: Int = 0,
    content: @Composable () -> Unit
) {
    val context = LocalContext.current
    val systemDark = isSystemInDarkTheme()
    val palette = AccentPalettes.getOrElse(accentIndex) { AccentPalettes.first() }

    val isDark = when (themeMode) {
        AppThemeMode.DARK, AppThemeMode.AMOLED -> true
        AppThemeMode.LIGHT -> false
        AppThemeMode.DYNAMIC -> systemDark
    }
    val isAmoled = (themeMode == AppThemeMode.AMOLED)

    val colorScheme = when {
        themeMode == AppThemeMode.DYNAMIC && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            if (isDark) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        isDark -> darkColorScheme(
            primary = palette.primary,
            secondary = palette.secondary,
            background = if (isAmoled) Color.Black else Color(0xFF0B101E),
            surface = if (isAmoled) Color(0xFF08080C) else Color(0xFF131B2E)
        )
        else -> lightColorScheme(
            primary = palette.glow,
            secondary = palette.secondary,
            background = Color(0xFFF1F5F9),
            surface = Color(0xFFFFFFFF)
        )
    }

    val primaryAccent = if (themeMode == AppThemeMode.DYNAMIC) colorScheme.primary else palette.primary
    val secondaryAccent = if (themeMode == AppThemeMode.DYNAMIC) colorScheme.secondary else palette.secondary

    val glassColors = if (isDark) {
        GlassColors(
            isDark = true,
            isAmoled = isAmoled,
            surfaceTranslucent = if (isAmoled) Color(0x22FFFFFF) else Color(0x24FFFFFF),
            surfaceFallback = if (isAmoled) Color(0xF0090D16) else Color(0xEB151E32),
            borderLight = Color(0x36FFFFFF),
            textPrimary = Color(0xFFF8FAFC),
            textDimmed = Color(0xFF94A3B8),
            accent = primaryAccent,
            accentSecondary = secondaryAccent,
            resultChipBg = primaryAccent.copy(alpha = 0.16f)
        )
    } else {
        GlassColors(
            isDark = false,
            isAmoled = false,
            surfaceTranslucent = Color(0xA6FFFFFF),
            surfaceFallback = Color(0xF5FFFFFF),
            borderLight = Color(0x80FFFFFF),
            textPrimary = Color(0xFF0F172A),
            textDimmed = Color(0xFF64748B),
            accent = palette.glow,
            accentSecondary = secondaryAccent,
            resultChipBg = palette.glow.copy(alpha = 0.12f)
        )
    }

    CompositionLocalProvider(LocalGlassColors provides glassColors) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = CalcNotesTypography,
            content = content
        )
    }
}
