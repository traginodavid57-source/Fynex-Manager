package org.fynex.manager.core.settings

import android.content.Context
import android.content.SharedPreferences
import com.google.gson.Gson
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.fynex.manager.core.ai.AiProvider
import org.fynex.manager.core.ai.AiSettings
import org.fynex.manager.core.model.SortField
import org.fynex.manager.core.model.SortSpec
import org.fynex.manager.core.model.ViewMode

enum class AppThemeMode(val title: String) {
    SYSTEM("Padrão do Sistema"),
    LIGHT("Claro"),
    DARK("Escuro (Material 3)"),
    AMOLED("AMOLED Preto Absoluto")
}

class PreferencesManager(context: Context) {

    private val prefs: SharedPreferences = context.getSharedPreferences("fynex_preferences", Context.MODE_PRIVATE)
    private val gson = Gson()

    private val _themeMode = MutableStateFlow(loadThemeMode())
    val themeMode: StateFlow<AppThemeMode> = _themeMode.asStateFlow()

    private val _showHiddenFiles = MutableStateFlow(prefs.getBoolean(KEY_SHOW_HIDDEN, false))
    val showHiddenFiles: StateFlow<Boolean> = _showHiddenFiles.asStateFlow()

    private val _isDualPaneEnabled = MutableStateFlow(prefs.getBoolean(KEY_DUAL_PANE, true))
    val isDualPaneEnabled: StateFlow<Boolean> = _isDualPaneEnabled.asStateFlow()

    private val _viewMode = MutableStateFlow(loadViewMode())
    val viewMode: StateFlow<ViewMode> = _viewMode.asStateFlow()

    private val _sortSpec = MutableStateFlow(loadSortSpec())
    val sortSpec: StateFlow<SortSpec> = _sortSpec.asStateFlow()

    private val _aiSettings = MutableStateFlow(loadAiSettings())
    val aiSettings: StateFlow<AiSettings> = _aiSettings.asStateFlow()

    fun setThemeMode(mode: AppThemeMode) {
        prefs.edit().putString(KEY_THEME, mode.name).apply()
        _themeMode.value = mode
    }

    fun setShowHiddenFiles(show: Boolean) {
        prefs.edit().putBoolean(KEY_SHOW_HIDDEN, show).apply()
        _showHiddenFiles.value = show
    }

    fun setDualPaneEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_DUAL_PANE, enabled).apply()
        _isDualPaneEnabled.value = enabled
    }

    fun setViewMode(mode: ViewMode) {
        prefs.edit().putString(KEY_VIEW_MODE, mode.name).apply()
        _viewMode.value = mode
    }

    fun setSortSpec(spec: SortSpec) {
        prefs.edit()
            .putString(KEY_SORT_FIELD, spec.field.name)
            .putBoolean(KEY_SORT_ASC, spec.ascending)
            .putBoolean(KEY_SORT_FOLDERS_FIRST, spec.foldersFirst)
            .apply()
        _sortSpec.value = spec
    }

    fun saveAiSettings(settings: AiSettings) {
        val json = gson.toJson(settings)
        prefs.edit().putString(KEY_AI_SETTINGS, json).apply()
        _aiSettings.value = settings
    }

    private fun loadThemeMode(): AppThemeMode {
        val name = prefs.getString(KEY_THEME, AppThemeMode.DARK.name) ?: AppThemeMode.DARK.name
        return try { AppThemeMode.valueOf(name) } catch (_: Exception) { AppThemeMode.DARK }
    }

    private fun loadViewMode(): ViewMode {
        val name = prefs.getString(KEY_VIEW_MODE, ViewMode.LIST.name) ?: ViewMode.LIST.name
        return try { ViewMode.valueOf(name) } catch (_: Exception) { ViewMode.LIST }
    }

    private fun loadSortSpec(): SortSpec {
        val fieldName = prefs.getString(KEY_SORT_FIELD, SortField.NAME.name) ?: SortField.NAME.name
        val field = try { SortField.valueOf(fieldName) } catch (_: Exception) { SortField.NAME }
        val asc = prefs.getBoolean(KEY_SORT_ASC, true)
        val foldersFirst = prefs.getBoolean(KEY_SORT_FOLDERS_FIRST, true)
        return SortSpec(field, asc, foldersFirst)
    }

    private fun loadAiSettings(): AiSettings {
        val json = prefs.getString(KEY_AI_SETTINGS, null) ?: return AiSettings()
        return try {
            gson.fromJson(json, AiSettings::class.java)
        } catch (_: Exception) {
            AiSettings()
        }
    }

    companion object {
        private const val KEY_THEME = "pref_theme"
        private const val KEY_SHOW_HIDDEN = "pref_show_hidden"
        private const val KEY_DUAL_PANE = "pref_dual_pane"
        private const val KEY_VIEW_MODE = "pref_view_mode"
        private const val KEY_SORT_FIELD = "pref_sort_field"
        private const val KEY_SORT_ASC = "pref_sort_asc"
        private const val KEY_SORT_FOLDERS_FIRST = "pref_sort_folders_first"
        private const val KEY_AI_SETTINGS = "pref_ai_settings"
    }
}
