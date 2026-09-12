package org.fynex.manager.core.model

enum class FileCategory(val displayName: String, val extensions: Set<String>) {
    IMAGES("Imagens", setOf("jpg", "jpeg", "png", "gif", "webp", "bmp", "svg", "heic", "avif")),
    VIDEOS("Vídeos", setOf("mp4", "mkv", "avi", "mov", "webm", "flv", "wmv", "3gp", "m4v")),
    AUDIO("Áudios", setOf("mp3", "wav", "flac", "m4a", "aac", "ogg", "opus", "mid")),
    DOCUMENTS("Documentos", setOf("pdf", "doc", "docx", "xls", "xlsx", "ppt", "pptx", "txt", "md", "csv", "json", "xml", "html", "epub")),
    APKS("APKs & Apps", setOf("apk", "apks", "xapk", "apkm", "dex")),
    ARCHIVES("Compactados", setOf("zip", "rar", "7z", "tar", "gz", "bz2", "xz", "iso")),
    DOWNLOADS("Downloads", emptySet()),
    LARGE_FILES("Arquivos Grandes", emptySet()),
    RECENT("Recentes", emptySet());

    companion object {
        fun fromExtension(ext: String): FileCategory? {
            val lower = ext.lowercase()
            return entries.firstOrNull { it.extensions.contains(lower) }
        }
    }
}
