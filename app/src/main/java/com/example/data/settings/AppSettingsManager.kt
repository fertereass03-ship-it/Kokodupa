package com.example.data.settings

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

enum class AppLanguage(val code: String, val displayName: String) {
    UKRAINIAN("uk", "Українська"),
    RUSSIAN("ru", "Русский")
}

enum class AppThemeMode(val code: String, val titleUk: String, val titleRu: String) {
    ORIGINAL_DARK("original", "Оригінальна (ANIWERTI)", "Оригинальная (ANIWERTI)"),
    LIGHT("light", "Світла тема", "Светлая тема"),
    DARK("dark", "Темно-фіолетова тема", "Тёмно-фиолетовая тема")
}

data class AppSettings(
    val language: AppLanguage = AppLanguage.RUSSIAN,
    val themeMode: AppThemeMode = AppThemeMode.ORIGINAL_DARK
)

object AppSettingsManager {
    private const val PREFS_NAME = "aniwerti_app_settings_prefs"
    private const val KEY_LANG = "app_language"
    private const val KEY_THEME = "app_theme_mode"

    private lateinit var prefs: SharedPreferences
    private val _settingsState = MutableStateFlow(AppSettings())
    val settingsState: StateFlow<AppSettings> = _settingsState.asStateFlow()

    fun init(context: Context) {
        if (::prefs.isInitialized) return
        prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

        val savedLangCode = prefs.getString(KEY_LANG, AppLanguage.RUSSIAN.code) ?: AppLanguage.RUSSIAN.code
        val lang = if (savedLangCode == AppLanguage.UKRAINIAN.code) AppLanguage.UKRAINIAN else AppLanguage.RUSSIAN

        val savedThemeCode = prefs.getString(KEY_THEME, AppThemeMode.ORIGINAL_DARK.code) ?: AppThemeMode.ORIGINAL_DARK.code
        val theme = when (savedThemeCode) {
            AppThemeMode.LIGHT.code -> AppThemeMode.LIGHT
            AppThemeMode.DARK.code -> AppThemeMode.DARK
            else -> AppThemeMode.ORIGINAL_DARK
        }

        _settingsState.value = AppSettings(language = lang, themeMode = theme)
    }

    fun setLanguage(language: AppLanguage) {
        _settingsState.value = _settingsState.value.copy(language = language)
        if (::prefs.isInitialized) {
            prefs.edit().putString(KEY_LANG, language.code).apply()
        }
    }

    fun setThemeMode(themeMode: AppThemeMode) {
        _settingsState.value = _settingsState.value.copy(themeMode = themeMode)
        if (::prefs.isInitialized) {
            prefs.edit().putString(KEY_THEME, themeMode.code).apply()
        }
    }

    fun isUkrainian(): Boolean = _settingsState.value.language == AppLanguage.UKRAINIAN
}
