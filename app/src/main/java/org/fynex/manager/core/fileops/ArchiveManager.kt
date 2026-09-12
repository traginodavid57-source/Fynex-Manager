package org.fynex.manager.core.fileops

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import net.lingala.zip4j.ZipFile
import net.lingala.zip4j.model.FileHeader
import net.lingala.zip4j.model.ZipParameters
import net.lingala.zip4j.model.enums.AesKeyStrength
import net.lingala.zip4j.model.enums.CompressionLevel
import net.lingala.zip4j.model.enums.CompressionMethod
import net.lingala.zip4j.model.enums.EncryptionMethod
import org.fynex.manager.core.model.FileItem
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.InputStream

object ArchiveManager {

    /**
     * Lists files inside a ZIP or APK archive at a given internal subfolder path.
     * E.g. internalPath = "" lists root of archive; internalPath = "res/" lists res folder.
     */
    suspend fun listArchiveContents(
        archiveFile: File,
        internalPath: String = "",
        password: String? = null
    ): List<FileItem> = withContext(Dispatchers.IO) {
        val result = mutableListOf<FileItem>()
        try {
            val zip = ZipFile(archiveFile)
            if (password != null && zip.isEncrypted) {
                zip.setPassword(password.toCharArray())
            }

            val prefix = if (internalPath.isEmpty() || internalPath.endsWith("/")) internalPath else "$internalPath/"
            val seenDirs = mutableSetOf<String>()

            val fileHeaders = zip.fileHeaders
            for (header in fileHeaders) {
                val fullPath = header.fileName
                if (!fullPath.startsWith(prefix)) continue

                val relative = fullPath.removePrefix(prefix)
                if (relative.isEmpty()) continue

                val slashIndex = relative.indexOf('/')
                if (slashIndex != -1) {
                    // It's a directory or inside a directory
                    val dirName = relative.substring(0, slashIndex)
                    if (seenDirs.add(dirName)) {
                        result.add(
                            FileItem(
                                file = File(archiveFile, "$prefix$dirName"),
                                name = dirName,
                                path = "${archiveFile.absolutePath}!/$prefix$dirName",
                                isDirectory = true,
                                size = 0L,
                                lastModified = header.lastModifiedTimeEpoch,
                                archiveInternalPath = "$prefix$dirName/"
                            )
                        )
                    }
                } else {
                    // Direct file in this directory
                    val isDir = header.isDirectory
                    result.add(
                        FileItem(
                            file = File(archiveFile, fullPath),
                            name = relative,
                            path = "${archiveFile.absolutePath}!/$fullPath",
                            isDirectory = isDir,
                            size = header.uncompressedSize,
                            lastModified = header.lastModifiedTimeEpoch,
                            archiveInternalPath = fullPath
                        )
                    )
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        result
    }

    suspend fun readArchiveEntryBytes(
        archiveFile: File,
        entryPath: String,
        password: String? = null
    ): ByteArray? = withContext(Dispatchers.IO) {
        try {
            val zip = ZipFile(archiveFile)
            if (password != null && zip.isEncrypted) {
                zip.setPassword(password.toCharArray())
            }
            val header = zip.getFileHeader(entryPath) ?: return@withContext null
            zip.getInputStream(header).use { input: InputStream ->
                val buffer = ByteArray(8192)
                val out = ByteArrayOutputStream()
                var read: Int
                while (input.read(buffer).also { read = it } != -1) {
                    out.write(buffer, 0, read)
                }
                out.toByteArray()
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    suspend fun extractAll(
        archiveFile: File,
        destinationDir: File,
        password: String? = null
    ): Boolean = withContext(Dispatchers.IO) {
        try {
            val zip = ZipFile(archiveFile)
            if (password != null && zip.isEncrypted) {
                zip.setPassword(password.toCharArray())
            }
            zip.extractAll(destinationDir.absolutePath)
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    suspend fun extractEntry(
        archiveFile: File,
        entryPath: String,
        destinationDir: File,
        password: String? = null
    ): Boolean = withContext(Dispatchers.IO) {
        try {
            val zip = ZipFile(archiveFile)
            if (password != null && zip.isEncrypted) {
                zip.setPassword(password.toCharArray())
            }
            zip.extractFile(entryPath, destinationDir.absolutePath)
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    suspend fun addOrReplaceFileInArchive(
        archiveFile: File,
        fileToAdd: File,
        internalPath: String = ""
    ): Boolean = withContext(Dispatchers.IO) {
        try {
            val zip = ZipFile(archiveFile)
            val params = ZipParameters().apply {
                compressionMethod = CompressionMethod.DEFLATE
                compressionLevel = CompressionLevel.NORMAL
                if (internalPath.isNotEmpty()) {
                    rootFolderNameInZip = internalPath
                }
            }
            zip.addFile(fileToAdd, params)
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    suspend fun createArchive(
        destFile: File,
        filesToCompress: List<File>,
        password: String? = null
    ): Boolean = withContext(Dispatchers.IO) {
        try {
            val zip = if (password != null && password.isNotEmpty()) {
                ZipFile(destFile, password.toCharArray())
            } else {
                ZipFile(destFile)
            }
            val params = ZipParameters().apply {
                compressionMethod = CompressionMethod.DEFLATE
                compressionLevel = CompressionLevel.NORMAL
                if (password != null && password.isNotEmpty()) {
                    isEncryptFiles = true
                    encryptionMethod = EncryptionMethod.AES
                    aesKeyStrength = AesKeyStrength.KEY_STRENGTH_256
                }
            }
            for (file in filesToCompress) {
                if (file.isDirectory) {
                    zip.addFolder(file, params)
                } else {
                    zip.addFile(file, params)
                }
            }
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }
}
