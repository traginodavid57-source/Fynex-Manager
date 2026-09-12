package org.fynex.manager.core.plugin

import android.content.Context
import com.google.gson.Gson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import net.lingala.zip4j.ZipFile
import org.fynex.manager.core.model.FileItem
import java.io.File

object PluginManager {

    private val _installedPlugins = MutableStateFlow<List<PluginManifest>>(emptyList())
    val installedPlugins: StateFlow<List<PluginManifest>> = _installedPlugins.asStateFlow()

    private val gson = Gson()

    fun init(context: Context) {
        val pluginsDir = File(context.filesDir, "plugins")
        if (!pluginsDir.exists()) pluginsDir.mkdirs()
        loadPlugins(pluginsDir)
    }

    private fun loadPlugins(pluginsDir: File) {
        val list = mutableListOf<PluginManifest>()

        // 1. Add built-in core plugins (from MT Manager & Fylo concepts)
        list.add(
            PluginManifest(
                id = "org.fynex.plugin.apkmod",
                name = "APK Modder & Smali Helper",
                version = "1.2.0",
                versionCode = 12,
                author = "Fynex Core Team",
                description = "Ferramenta avançada para inspeção rápida de métodos Smali, patch de strings e verificação de assinatura APK.",
                type = PluginType.TOOL,
                supportedExtensions = listOf("apk", "dex", "smali"),
                isEnabled = true,
                isBuiltIn = true
            )
        )
        list.add(
            PluginManifest(
                id = "org.fynex.plugin.whisper",
                name = "Whisper Mini AI Transcriber",
                version = "2.0.1",
                versionCode = 20,
                author = "Fylo Labs & Fynex",
                description = "Mecanismo de transcrição inteligente de arquivos de áudio para texto offline usando modelos de inteligência artificial.",
                type = PluginType.AI_MODEL,
                supportedExtensions = listOf("mp3", "wav", "m4a", "ogg", "opus"),
                isEnabled = true,
                isBuiltIn = true
            )
        )
        list.add(
            PluginManifest(
                id = "org.fynex.plugin.hexpro",
                name = "Hex Professional Inspector",
                version = "1.0.0",
                versionCode = 10,
                author = "MT Tools Community",
                description = "Visualizador e editor hexadecimal com suporte a endianness, realce de cabeçalhos de arquivos e busca de bytes binários.",
                type = PluginType.VIEWER,
                supportedExtensions = listOf("bin", "dat", "so", "dex", "img"),
                isEnabled = true,
                isBuiltIn = true
            )
        )
        list.add(
            PluginManifest(
                id = "org.fynex.plugin.sqlitebrowser",
                name = "SQLite Database Viewer",
                version = "1.1.0",
                versionCode = 11,
                author = "Open FOSS Developer",
                description = "Navegador de tabelas e registros de bancos de dados SQLite (.db, .sqlite).",
                type = PluginType.VIEWER,
                supportedExtensions = listOf("db", "sqlite", "sqlite3"),
                isEnabled = true,
                isBuiltIn = true
            )
        )

        // 2. Load custom user-installed plugins from disk
        pluginsDir.listFiles()?.forEach { dir ->
            if (dir.isDirectory) {
                val manifestFile = File(dir, "plugin.json")
                if (manifestFile.exists()) {
                    try {
                        val manifest = gson.fromJson(manifestFile.readText(), PluginManifest::class.java)
                        list.add(manifest.copy(installPath = dir.absolutePath, isBuiltIn = false))
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
            }
        }

        _installedPlugins.value = list
    }

    suspend fun installPluginPackage(context: Context, pluginArchiveFile: File): Result<PluginManifest> = withContext(Dispatchers.IO) {
        try {
            val pluginsDir = File(context.filesDir, "plugins")
            val tempDir = File(context.cacheDir, "plugin_temp_${System.currentTimeMillis()}")
            tempDir.mkdirs()

            ZipFile(pluginArchiveFile).extractAll(tempDir.absolutePath)
            val manifestFile = File(tempDir, "plugin.json")
            if (!manifestFile.exists()) {
                return@withContext Result.failure(IllegalArgumentException("Arquivo plugin.json não encontrado no pacote do plugin!"))
            }

            val manifest = gson.fromJson(manifestFile.readText(), PluginManifest::class.java)
            val targetDir = File(pluginsDir, manifest.id)
            if (targetDir.exists()) targetDir.deleteRecursively()
            tempDir.renameTo(targetDir)

            loadPlugins(pluginsDir)
            Result.success(manifest)
        } catch (e: Exception) {
            e.printStackTrace()
            Result.failure(e)
        }
    }

    fun togglePlugin(pluginId: String, enable: Boolean) {
        _installedPlugins.value = _installedPlugins.value.map {
            if (it.id == pluginId) it.copy(isEnabled = enable) else it
        }
    }

    fun deletePlugin(context: Context, pluginId: String) {
        val plugin = _installedPlugins.value.firstOrNull { it.id == pluginId } ?: return
        if (plugin.isBuiltIn) return
        plugin.installPath?.let { File(it).deleteRecursively() }
        val pluginsDir = File(context.filesDir, "plugins")
        loadPlugins(pluginsDir)
    }
}
