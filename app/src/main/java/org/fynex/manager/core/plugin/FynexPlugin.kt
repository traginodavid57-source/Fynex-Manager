package org.fynex.manager.core.plugin

import android.content.Context
import org.fynex.manager.core.model.FileItem

interface FynexPlugin {
    val manifest: PluginManifest

    fun onInit(context: Context)
    fun onDestroy()

    fun canHandleFile(fileItem: FileItem): Boolean {
        return manifest.supportedExtensions.contains(fileItem.extension)
    }

    suspend fun executeAction(action: String, targetFile: FileItem?, params: Map<String, Any>): PluginResult
}

data class PluginResult(
    val success: Boolean,
    val message: String,
    val outputData: Any? = null
)
