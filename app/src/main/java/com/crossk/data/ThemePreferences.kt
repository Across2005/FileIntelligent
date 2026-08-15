package com.crossk.data

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * v2.0 主题持久化 — 替代 v1 的 `object ThemeState` 全局可变单例。
 *
 * 关键对齐：
 * - 写入：DataStore 异步 fsync
 * - 读取：Flow → UI 用 collectAsStateWithLifecycle 订阅
 * - 缺省：跟随系统 (`useSystemTheme = true`)
 *
 * 暴露给 UI 的 `darkModeFlow` 并不"兜底"塞值，使用者要先决定 fallback。
 */
private val Context.themeDataStore by preferencesDataStore(name = "theme_prefs")

class ThemePreferences(private val context: Context) {

    private val useSystemKey = booleanPreferencesKey("use_system_theme")
    private val darkModeKey = booleanPreferencesKey("dark_mode")

    val useSystemTheme: Flow<Boolean> = context.themeDataStore.data
        .map { it[useSystemKey] ?: true }

    val darkMode: Flow<Boolean> = context.themeDataStore.data
        .map { it[darkModeKey] ?: true }

    suspend fun setUseSystemTheme(useSystem: Boolean) {
        context.themeDataStore.edit { it[useSystemKey] = useSystem }
    }

    suspend fun setDarkMode(dark: Boolean) {
        context.themeDataStore.edit { it[darkModeKey] = dark }
    }
}
