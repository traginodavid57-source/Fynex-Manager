package org.fynex.manager.core.model

import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class FileItem(
    val file: File,
    val name: String = file.name,
    val path: String = file.absolutePath,
    val isDirectory: Boolean = file.isDirectory,
    val size: Long = if (file.isDirectory) 0L else file.length(),
    val lastModified: Long = file.lastModified(),
    val isHidden: Boolean = file.isHidden || file.name.startsWith("."),
    val extension: String = file.extension.lowercase(),
    val permissions: String = getFilePermissions(file),
    val isArchive: Boolean = isArchiveFile(file.name),
    val isApk: Boolean = file.name.endsWith(".apk", ignoreCase = true) || file.name.endsWith(".apks", ignoreCase = true) || file.name.endsWith(".xapk", ignoreCase = true),
    val isSelected: Boolean = false,
    val childCount: Int? = null,
    val archiveInternalPath: String? = null // if browsing inside a zip/apk
) {
    val formattedSize: String
        get() {
            if (isDirectory) {
                return childCount?.let { "$it itens" } ?: "Pasta"
            }
            if (size <= 0) return "0 B"
            val units = arrayOf("B", "KB", "MB", "GB", "TB")
            var s = size.toDouble()
            var idx = 0
            while (s >= 1024 && idx < units.size - 1) {
                s /= 1024
                idx++
            }
            return String.format(Locale.US, "%.1f %s", s, units[idx])
        }

    val formattedDate: String
        get() {
            if (lastModified <= 0) return ""
            val sdf = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
            return sdf.format(Date(lastModified))
        }

    companion object {
        fun isArchiveFile(name: String): Boolean {
            val lower = name.lowercase()
            return lower.endsWith(".zip") || lower.endsWith(".apk") ||
                   lower.endsWith(".apks") || lower.endsWith(".xapk") ||
                   lower.endsWith(".jar") || lower.endsWith(".tar") ||
                   lower.endsWith(".gz") || lower.endsWith(".7z") ||
                   lower.endsWith(".rar")
        }

        private fun getFilePermissions(file: File): String {
            val r = if (file.canRead()) "r" else "-"
            val w = if (file.canWrite()) "w" else "-"
            val x = if (file.canExecute()) "x" else "-"
            return "$r$w$x"
        }
    }
}
