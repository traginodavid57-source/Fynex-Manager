package org.fynex.manager.core.fileops

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.fynex.manager.core.model.FileItem
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.security.MessageDigest
import java.util.zip.CRC32

object FileOperations {

    suspend fun search(
        root: File,
        query: String,
        maxResults: Int = 200,
        isCancelled: () -> Boolean = { false }
    ): List<FileItem> = withContext(Dispatchers.IO) {
        val q = query.trim().lowercase()
        if (q.isEmpty()) return@withContext emptyList()
        val results = mutableListOf<FileItem>()
        val stack = ArrayDeque<File>()
        stack.add(root)
        while (stack.isNotEmpty() && results.size < maxResults && !isCancelled()) {
            val dir = stack.removeLast()
            if (!dir.canRead()) continue
            val children = dir.listFiles() ?: continue
            val dirs = mutableListOf<File>()
            for (f in children) {
                if (isCancelled() || results.size >= maxResults) break
                val name = f.name.lowercase()
                if (name.contains(q)) {
                    results.add(FileItem(file = f))
                }
                if (f.isDirectory) {
                    if (f.canRead()) dirs.add(f)
                }
            }
            // Add subdirectories to stack (reverse order for DFS-like behavior)
            stack.addAll(dirs)
        }
        results.sortedByDescending { it.lastModified }
    }

    suspend fun listDirectory(path: String, showHidden: Boolean = false): List<FileItem> = withContext(Dispatchers.IO) {
        val dir = File(path)
        if (!dir.exists() || !dir.isDirectory) {
            // Check if root has access
            if (RootFileOperations.isRootAvailable()) {
                return@withContext RootFileOperations.listDirectory(path)
            }
            return@withContext emptyList()
        }

        val files = dir.listFiles() ?: return@withContext emptyList()
        files.mapNotNull { f ->
            if (!showHidden && (f.isHidden || f.name.startsWith("."))) {
                null
            } else {
                val childCount = if (f.isDirectory) f.list()?.size else null
                FileItem(
                    file = f,
                    childCount = childCount
                )
            }
        }
    }

    suspend fun copyFileOrDirectory(src: File, destDir: File, onProgress: ((Float) -> Unit)? = null): Boolean = withContext(Dispatchers.IO) {
        try {
            if (!destDir.exists()) destDir.mkdirs()
            val target = File(destDir, src.name)
            if (src.isDirectory) {
                target.mkdirs()
                src.listFiles()?.forEach { child ->
                    copyFileOrDirectory(child, target, onProgress)
                }
            } else {
                val buffer = ByteArray(64 * 1024)
                val totalBytes = src.length()
                var copiedBytes = 0L

                FileInputStream(src).use { input ->
                    FileOutputStream(target).use { output ->
                        var read: Int
                        while (input.read(buffer).also { read = it } != -1) {
                            output.write(buffer, 0, read)
                            copiedBytes += read
                            if (totalBytes > 0) {
                                onProgress?.invoke(copiedBytes.toFloat() / totalBytes)
                            }
                        }
                    }
                }
            }
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    suspend fun moveFileOrDirectory(src: File, destDir: File): Boolean = withContext(Dispatchers.IO) {
        try {
            val target = File(destDir, src.name)
            if (src.renameTo(target)) return@withContext true
            val copied = copyFileOrDirectory(src, destDir)
            if (copied) {
                deleteRecursively(src)
                true
            } else {
                false
            }
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    suspend fun deleteRecursively(file: File): Boolean = withContext(Dispatchers.IO) {
        try {
            if (file.isDirectory) {
                file.listFiles()?.forEach { deleteRecursively(it) }
            }
            file.delete()
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    suspend fun rename(file: File, newName: String): Boolean = withContext(Dispatchers.IO) {
        try {
            val target = File(file.parentFile, newName)
            file.renameTo(target)
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    suspend fun createDirectory(parent: File, name: String): Boolean = withContext(Dispatchers.IO) {
        try {
            val dir = File(parent, name)
            dir.mkdirs()
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    suspend fun createEmptyFile(parent: File, name: String): Boolean = withContext(Dispatchers.IO) {
        try {
            val file = File(parent, name)
            file.createNewFile()
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    // MT Manager Batch Rename Engine
    suspend fun batchRename(
        files: List<File>,
        pattern: String,
        replacement: String,
        useRegex: Boolean = false,
        prefix: String = "",
        suffix: String = "",
        startIndex: Int = 1,
        indexDigits: Int = 2
    ): Int = withContext(Dispatchers.IO) {
        var successCount = 0
        var currentIndex = startIndex

        for (file in files) {
            val oldName = file.name
            val nameWithoutExt = file.nameWithoutExtension
            val ext = if (file.extension.isNotEmpty()) ".${file.extension}" else ""

            var base = if (pattern.isNotEmpty()) {
                if (useRegex) {
                    nameWithoutExt.replace(Regex(pattern), replacement)
                } else {
                    nameWithoutExt.replace(pattern, replacement)
                }
            } else {
                nameWithoutExt
            }

            val indexStr = String.format("%0${indexDigits}d", currentIndex)
            val usesIndex = base.contains("{i}") || base.contains("{index}")
            val withNumber = if (usesIndex) {
                base.replace("{i}", indexStr).replace("{index}", indexStr)
            } else {
                "$base$indexStr"
            }

            val newName = "$prefix$withNumber$suffix$ext"
            val target = File(file.parentFile, newName)
            if (file.renameTo(target)) {
                successCount++
                currentIndex++
            }
        }
        successCount
    }

    // Hash calculation (MD5, SHA-1, SHA-256, CRC32)
    suspend fun calculateChecksums(file: File): Map<String, String> = withContext(Dispatchers.IO) {
        val md5 = MessageDigest.getInstance("MD5")
        val sha1 = MessageDigest.getInstance("SHA-1")
        val sha256 = MessageDigest.getInstance("SHA-256")
        val crc = CRC32()

        val buffer = ByteArray(64 * 1024)
        FileInputStream(file).use { input ->
            var read: Int
            while (input.read(buffer).also { read = it } != -1) {
                md5.update(buffer, 0, read)
                sha1.update(buffer, 0, read)
                sha256.update(buffer, 0, read)
                crc.update(buffer, 0, read)
            }
        }

        mapOf(
            "MD5" to md5.digest().joinToString("") { "%02x".format(it) },
            "SHA-1" to sha1.digest().joinToString("") { "%02x".format(it) },
            "SHA-256" to sha256.digest().joinToString("") { "%02x".format(it) },
            "CRC32" to "%08X".format(crc.value)
        )
    }

    // Permissions (chmod)
    suspend fun setPermissions(file: File, octal: String): Boolean = withContext(Dispatchers.IO) {
        try {
            val process = Runtime.getRuntime().exec(arrayOf("chmod", octal, file.absolutePath))
            process.waitFor() == 0
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }
}
