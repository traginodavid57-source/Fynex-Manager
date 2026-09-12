package org.fynex.manager.core.vault

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.fynex.manager.core.model.FileItem
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.security.MessageDigest
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec

object SafeVaultManager {

    private const val ALGO = "AES/GCM/NoPadding"
    private const val TAG_LENGTH_BIT = 128
    private const val IV_LENGTH_BYTE = 12

    private fun getVaultDirectory(context: Context): File {
        val dir = File(context.filesDir, ".vault_encrypted")
        if (!dir.exists()) dir.mkdirs()
        return dir
    }

    private fun deriveKey(passcode: String): SecretKey {
        val md = MessageDigest.getInstance("SHA-256")
        val keyBytes = md.digest(passcode.toByteArray(Charsets.UTF_8))
        return SecretKeySpec(keyBytes, "AES")
    }

    suspend fun listVaultFiles(context: Context): List<FileItem> = withContext(Dispatchers.IO) {
        val dir = getVaultDirectory(context)
        dir.listFiles()?.map { file ->
            // In vault, file names are stored as originalName.fynexenc
            val originalName = file.name.removeSuffix(".fynexenc")
            FileItem(
                file = file,
                name = originalName,
                path = file.absolutePath,
                isDirectory = false,
                size = file.length(),
                lastModified = file.lastModified()
            )
        } ?: emptyList()
    }

    suspend fun encryptAndMoveToVault(
        context: Context,
        sourceFile: File,
        passcode: String
    ): Result<File> = withContext(Dispatchers.IO) {
        try {
            val key = deriveKey(passcode)
            val iv = ByteArray(IV_LENGTH_BYTE)
            SecureRandom().nextBytes(iv)

            val cipher = Cipher.getInstance(ALGO)
            cipher.init(Cipher.ENCRYPT_MODE, key, GCMParameterSpec(TAG_LENGTH_BIT, iv))

            val vaultDir = getVaultDirectory(context)
            val destFile = File(vaultDir, "${sourceFile.name}.fynexenc")

            FileOutputStream(destFile).use { out ->
                // Write IV first
                out.write(iv)
                FileInputStream(sourceFile).use { input ->
                    val buffer = ByteArray(8192)
                    var read: Int
                    while (input.read(buffer).also { read = it } != -1) {
                        val encryptedChunk = cipher.update(buffer, 0, read)
                        if (encryptedChunk != null) out.write(encryptedChunk)
                    }
                    val finalChunk = cipher.doFinal()
                    if (finalChunk != null) out.write(finalChunk)
                }
            }

            // Remove original file for privacy
            sourceFile.delete()
            Result.success(destFile)
        } catch (e: Exception) {
            e.printStackTrace()
            Result.failure(e)
        }
    }

    suspend fun decryptAndRestore(
        context: Context,
        vaultFile: File,
        destinationDir: File,
        passcode: String
    ): Result<File> = withContext(Dispatchers.IO) {
        try {
            val key = deriveKey(passcode)
            val originalName = vaultFile.name.removeSuffix(".fynexenc")
            val targetFile = File(destinationDir, originalName)

            FileInputStream(vaultFile).use { input ->
                val iv = ByteArray(IV_LENGTH_BYTE)
                val ivRead = input.read(iv)
                if (ivRead != IV_LENGTH_BYTE) throw IllegalStateException("Arquivo criptografado corrompido")

                val cipher = Cipher.getInstance(ALGO)
                cipher.init(Cipher.DECRYPT_MODE, key, GCMParameterSpec(TAG_LENGTH_BIT, iv))

                FileOutputStream(targetFile).use { out ->
                    val buffer = ByteArray(8192)
                    var read: Int
                    while (input.read(buffer).also { read = it } != -1) {
                        val decryptedChunk = cipher.update(buffer, 0, read)
                        if (decryptedChunk != null) out.write(decryptedChunk)
                    }
                    val finalChunk = cipher.doFinal()
                    if (finalChunk != null) out.write(finalChunk)
                }
            }

            vaultFile.delete()
            Result.success(targetFile)
        } catch (e: Exception) {
            e.printStackTrace()
            Result.failure(e)
        }
    }
}
