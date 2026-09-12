package org.fynex.manager.core.plugin

enum class PluginType(val displayName: String) {
    TOOL("Ferramenta de Utilidade"),
    VIEWER("Visualizador / Editor"),
    AI_MODEL("Modelo de IA Offline"),
    THEME("Tema & Personalização")
}

data class PluginManifest(
    val id: String,
    val name: String,
    val version: String,
    val versionCode: Int,
    val author: String,
    val description: String,
    val type: PluginType,
    val iconUrl: String? = null,
    val supportedExtensions: List<String> = emptyList(),
    val isEnabled: Boolean = true,
    val isBuiltIn: Boolean = false,
    val installPath: String? = null
)
