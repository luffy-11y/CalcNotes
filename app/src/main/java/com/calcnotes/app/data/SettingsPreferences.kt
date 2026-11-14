package com.calcnotes.app.data

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.calcnotes.app.calc.MultiNumberMode
import com.calcnotes.app.calc.ThousandsSeparatorStyle
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "calcnotes_settings")

enum class AppThemeMode {
    DARK,
    LIGHT,
    AMOLED,
    DYNAMIC
}

enum class NoteSortOrder {
    DATE_EDITED,
    DATE_CREATED,
    TITLE,
    MANUAL
}

data class UserSettings(
    val themeMode: AppThemeMode = AppThemeMode.DARK,
    val accentColorIndex: Int = 0,
    val blurIntensityDp: Float = 24f,
    val animatedBackground: Boolean = true,
    val decimalPlaces: Int = 2,
    val separatorStyle: ThousandsSeparatorStyle = ThousandsSeparatorStyle.COMMA,
    val currencySymbol: String = "",
    val multiNumberMode: MultiNumberMode = MultiNumberMode.SUM,
    val fontSizeSp: Float = 16f,
    val lineSpacingMultiplier: Float = 1.35f,
    val showLineNumbers: Boolean = true,
    val hapticsEnabled: Boolean = true,
    val appLockEnabled: Boolean = false,
    val isGridView: Boolean = false,
    val sortOrder: NoteSortOrder = NoteSortOrder.DATE_EDITED,
    val hasSeededSampleNote: Boolean = false
)

class SettingsPreferences(private val context: Context) {

    private object Keys {
        val THEME_MODE = stringPreferencesKey("theme_mode")
        val ACCENT_INDEX = intPreferencesKey("accent_index")
        val BLUR_INTENSITY = floatPreferencesKey("blur_intensity")
        val ANIMATED_BG = booleanPreferencesKey("animated_bg")
        val DECIMAL_PLACES = intPreferencesKey("decimal_places")
        val SEPARATOR_STYLE = stringPreferencesKey("separator_style")
        val CURRENCY_SYMBOL = stringPreferencesKey("currency_symbol")
        val MULTI_NUMBER_MODE = stringPreferencesKey("multi_number_mode")
        val FONT_SIZE = floatPreferencesKey("font_size")
        val LINE_SPACING = floatPreferencesKey("line_spacing")
        val SHOW_LINE_NUMBERS = booleanPreferencesKey("show_line_numbers")
        val HAPTICS = booleanPreferencesKey("haptics")
        val APP_LOCK = booleanPreferencesKey("app_lock")
        val IS_GRID_VIEW = booleanPreferencesKey("is_grid_view")
        val SORT_ORDER = stringPreferencesKey("sort_order")
        val HAS_SEEDED = booleanPreferencesKey("has_seeded_sample")
    }

    val settingsFlow: Flow<UserSettings> = context.dataStore.data.map { prefs ->
        UserSettings(
            themeMode = runCatching {
                AppThemeMode.valueOf(prefs[Keys.THEME_MODE] ?: AppThemeMode.DARK.name)
            }.getOrDefault(AppThemeMode.DARK),
            accentColorIndex = prefs[Keys.ACCENT_INDEX] ?: 0,
            blurIntensityDp = prefs[Keys.BLUR_INTENSITY] ?: 24f,
            animatedBackground = prefs[Keys.ANIMATED_BG] ?: true,
            decimalPlaces = prefs[Keys.DECIMAL_PLACES] ?: 2,
            separatorStyle = runCatching {
                ThousandsSeparatorStyle.valueOf(
                    prefs[Keys.SEPARATOR_STYLE] ?: ThousandsSeparatorStyle.COMMA.name
                )
            }.getOrDefault(ThousandsSeparatorStyle.COMMA),
            currencySymbol = prefs[Keys.CURRENCY_SYMBOL] ?: "",
            multiNumberMode = runCatching {
                MultiNumberMode.valueOf(
                    prefs[Keys.MULTI_NUMBER_MODE] ?: MultiNumberMode.SUM.name
                )
            }.getOrDefault(MultiNumberMode.SUM),
            fontSizeSp = prefs[Keys.FONT_SIZE] ?: 16f,
            lineSpacingMultiplier = prefs[Keys.LINE_SPACING] ?: 1.35f,
            showLineNumbers = prefs[Keys.SHOW_LINE_NUMBERS] ?: true,
            hapticsEnabled = prefs[Keys.HAPTICS] ?: true,
            appLockEnabled = prefs[Keys.APP_LOCK] ?: false,
            isGridView = prefs[Keys.IS_GRID_VIEW] ?: false,
            sortOrder = runCatching {
                NoteSortOrder.valueOf(prefs[Keys.SORT_ORDER] ?: NoteSortOrder.DATE_EDITED.name)
            }.getOrDefault(NoteSortOrder.DATE_EDITED),
            hasSeededSampleNote = prefs[Keys.HAS_SEEDED] ?: false
        )
    }

    suspend fun updateThemeMode(mode: AppThemeMode) {
        context.dataStore.edit { it[Keys.THEME_MODE] = mode.name }
    }

    suspend fun updateAccentColorIndex(index: Int) {
        context.dataStore.edit { it[Keys.ACCENT_INDEX] = index }
    }

    suspend fun updateBlurIntensity(dp: Float) {
        context.dataStore.edit { it[Keys.BLUR_INTENSITY] = dp }
    }

    suspend fun updateAnimatedBackground(enabled: Boolean) {
        context.dataStore.edit { it[Keys.ANIMATED_BG] = enabled }
    }

    suspend fun updateDecimalPlaces(places: Int) {
        context.dataStore.edit { it[Keys.DECIMAL_PLACES] = places.coerceIn(0, 6) }
    }

    suspend fun updateSeparatorStyle(style: ThousandsSeparatorStyle) {
        context.dataStore.edit { it[Keys.SEPARATOR_STYLE] = style.name }
    }

    suspend fun updateCurrencySymbol(symbol: String) {
        context.dataStore.edit { it[Keys.CURRENCY_SYMBOL] = symbol }
    }

    suspend fun updateMultiNumberMode(mode: MultiNumberMode) {
        context.dataStore.edit { it[Keys.MULTI_NUMBER_MODE] = mode.name }
    }

    suspend fun updateFontSize(sp: Float) {
        context.dataStore.edit { it[Keys.FONT_SIZE] = sp.coerceIn(12f, 24f) }
    }

    suspend fun updateLineSpacing(multiplier: Float) {
        context.dataStore.edit { it[Keys.LINE_SPACING] = multiplier.coerceIn(1.1f, 1.8f) }
    }

    suspend fun updateShowLineNumbers(show: Boolean) {
        context.dataStore.edit { it[Keys.SHOW_LINE_NUMBERS] = show }
    }

    suspend fun updateHaptics(enabled: Boolean) {
        context.dataStore.edit { it[Keys.HAPTICS] = enabled }
    }

    suspend fun updateAppLock(enabled: Boolean) {
        context.dataStore.edit { it[Keys.APP_LOCK] = enabled }
    }

    suspend fun updateGridView(isGrid: Boolean) {
        context.dataStore.edit { it[Keys.IS_GRID_VIEW] = isGrid }
    }

    suspend fun updateSortOrder(order: NoteSortOrder) {
        context.dataStore.edit { it[Keys.SORT_ORDER] = order.name }
    }

    suspend fun markSampleSeeded() {
        context.dataStore.edit { it[Keys.HAS_SEEDED] = true }
    }
}
