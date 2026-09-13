package org.fynex.manager.core.vault

import android.content.Context
import android.os.Build
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import androidx.biometric.BiometricManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.fynex.manager.core.model.FileItem
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.security.KeyPairGenerator
import java.security.KeyStore
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.SecretKey
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.PBEKeySpec
import javax.crypto.spec.SecretKeySpec

object SafeVaultManager {

    private const val ALGO = "AES/GCM/NoPadding"
    private const val TAG_LENGTH_BIT = 128
    private const val IV_LENGTH_BYTE = 12
    private const val SALT_LENGTH_BYTE = 16
    private const val PBKDF2_ITERATIONS = 210_000

    // New file header: magic + salt + IV. Older vault files (no header) are still supported.
    private val VAULT_MAGIC = "FYNE1".toByteArray(Charsets.US_ASCII)

    // Android Keystore key used to keep the passcode for biometric unlock.
    private const val KEYSTORE_ALIAS = "fynex_vault_biometric"
    private const val AUTH_CIPHER_TRANSFORM = "RSA/ECB/PKCS1Padding"
    private const val KEY_STORED_PASSCODE = "vault_biometric_passcode"
    private const val PREFS_NAME = "fynex_vault"

    private fun getVaultDirectory(context: Context): File {
        val dir = File(context.filesDir, ".vault_encrypted")
        if (!dir.exists()) dir.mkdirs()
        return dir
    }

    /**
     * v1 (legacy): SHA-256 of the passcode. v2 (new): PBKDF2WithHmacSHA256 + random salt.
     */
    private fun deriveKey(passcode: String, salt: ByteArray?): SecretKey = if (salt == null) {
        val md = java.security.MessageDigest.getInstance("SHA-256")
        SecretKeySpec(md.digest(passcode.toByteArray(Charsets.UTF_8)), "AES")
    } else {
        val spec = PBEKeySpec(passcode.toCharArray(), salt, PBKDF2_ITERATIONS, 256)
        val factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
        SecretKeySpec(factory.generateSecret(spec).encoded, "AES")
    }

    private class VaultStream(val key: SecretKey, val iv: ByteArray, val input: FileInputStream)

    private fun openVaultStream(file: File, passcode: String): VaultStream {
        val raw = FileInputStream(file)
        val magicBytes = ByteArray(VAULT_MAGIC.size)
        val read = raw.read(magicBytes)
        if (read == VAULT_MAGIC.size && magicBytes.contentEquals(VAULT_MAGIC)) {
            val salt = ByteArray(SALT_LENGTH_BYTE)
            if (raw.read(salt) != SALT_LENGTH_BYTE) {
                raw.close()
                throw IllegalStateException("Arquivo criptografado corrompido")
            }
            val iv = ByteArray(IV_LENGTH_BYTE)
            if (raw.read(iv) != IV_LENGTH_BYTE) {
                raw.close()
                throw IllegalStateException("Arquivo criptografado corrompido")
            }
            return VaultStream(deriveKey(passcode, salt), iv, raw)
        }
        raw.close()

        // Legacy v1 format: file starts directly with the IV.
        val legacy = FileInputStream(file)
        val iv = ByteArray(IV_LENGTH_BYTE)
        if (legacy.read(iv) != IV_LENGTH_BYTE) {
            legacy.close()
            throw IllegalStateException("Arquivo criptografado corrompido")
        }
        return VaultStream(deriveKey(passcode, null), iv, legacy)
    }

    suspend fun listVaultFiles(context: Context): List<FileItem> = withContext(Dispatchers.IO) {
        val dir = getVaultDirectory(context)
        dir.listFiles()?.map { file ->
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
            val salt = ByteArray(SALT_LENGTH_BYTE)
            SecureRandom().nextBytes(salt)
            val iv = ByteArray(IV_LENGTH_BYTE)
            SecureRandom().nextBytes(iv)
            val key = deriveKey(passcode, salt)

            val cipher = Cipher.getInstance(ALGO)
            cipher.init(Cipher.ENCRYPT_MODE, key, GCMParameterSpec(TAG_LENGTH_BIT, iv))

            val vaultDir = getVaultDirectory(context)
            val destFile = File(vaultDir, "${sourceFile.name}.fynexenc")

            FileOutputStream(destFile).use { out ->
                out.write(VAULT_MAGIC)
                out.write(salt)
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

            // Keep an encrypted copy of the passcode (biometric unlock).
            storePasscodeForBiometric(context, passcode)

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
            if (!destinationDir.exists()) destinationDir.mkdirs()
            val originalName = vaultFile.name.removeSuffix(".fynexenc")
            val targetFile = File(destinationDir, originalName)

            val stream = openVaultStream(vaultFile, passcode)
            try {
                val cipher = Cipher.getInstance(ALGO)
                cipher.init(Cipher.DECRYPT_MODE, stream.key, GCMParameterSpec(TAG_LENGTH_BIT, stream.iv))

                FileOutputStream(targetFile).use { out ->
                    val buffer = ByteArray(8192)
                    var read: Int
                    while (stream.input.read(buffer).also { read = it } != -1) {
                        val decryptedChunk = cipher.update(buffer, 0, read)
                        if (decryptedChunk != null) out.write(decryptedChunk)
                    }
                    stream.input.close()
                    val finalChunk = cipher.doFinal()
                    if (finalChunk != null) out.write(finalChunk)
                }
            } finally {
                stream.input.close()
            }

            vaultFile.delete()
            Result.success(targetFile)
        } catch (e: Exception) {
            e.printStackTrace()
            try { targetFile.delete() } catch (_: Exception) {}
            Result.failure(e)
        }
    }

    // ------------------------------------------------------------------
    // Biometric unlock support (passcode protected by Android Keystore)
    // ------------------------------------------------------------------

    fun canAuthenticate(context: Context): Boolean {
        return try {
            val result = BiometricManager.from(context).canAuthenticate(
                BiometricManager.Authenticators.BIOMETRIC_STRONG or
                    BiometricManager.Authenticators.BIOMETRIC_WEAK
            )
            result == BiometricManager.BIOMETRIC_SUCCESS
        } catch (e: Exception) {
            false
        }
    }

    fun storePasscodeForBiometric(context: Context, passcode: String) {
        try {
            if (!canAuthenticate(context)) return
            ensureAuthKey(context)

            val keyStore = KeyStore.getInstance("AndroidKeyStore")
            keyStore.load(null)
            val publicKey = keyStore.getCertificate(KEYSTORE_ALIAS)?.publicKey ?: return

            val cipher = Cipher.getInstance(AUTH_CIPHER_TRANSFORM)
            cipher.init(Cipher.ENCRYPT_MODE, publicKey)
            val encrypted = cipher.doFinal(passcode.toByteArray(Charsets.UTF_8))

            context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                .edit()
                .putString(KEY_STORED_PASSCODE, Base64.encodeToString(encrypted, Base64.NO_WRAP))
                .apply()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    /**
     * Creates a keystore Cipher set up for decryption. This Cipher must be passed to
     * BiometricPrompt.authenticate(cryptoObject) so the key is released after auth.
     */
    fun createBiometricDecryptCipher(): Cipher {
        val keyStore = KeyStore.getInstance("AndroidKeyStore")
        keyStore.load(null)
        if (!keyStore.containsAlias(KEYSTORE_ALIAS)) {
            throw IllegalStateException("Nenhuma senha salva para biometria ainda")
        }
        val privateKey = keyStore.getKey(KEYSTORE_ALIAS, null)
        val cipher = Cipher.getInstance(AUTH_CIPHER_TRANSFORM)
        cipher.init(Cipher.DECRYPT_MODE, privateKey)
        return cipher
    }

    fun retrievePasscodeFromCipher(context: Context, cipher: Cipher?): String? {
        try {
            if (cipher == null) return null
            val base64 = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                .getString(KEY_STORED_PASSCODE, null) ?: return null
            val encrypted = Base64.decode(base64, Base64.NO_WRAP)
            return String(cipher.doFinal(encrypted), Charsets.UTF_8)
        } catch (e: Exception) {
            e.printStackTrace()
            return null
        }
    }

    private fun ensureAuthKey(context: Context) {
        val keyStore = KeyStore.getInstance("AndroidKeyStore")
        keyStore.load(null)
        if (keyStore.containsAlias(KEYSTORE_ALIAS)) return

        val builder = KeyGenParameterSpec.Builder(
            KEYSTORE_ALIAS,
            KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
        )
            .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_RSA_PKCS1)
            .setDigests(KeyProperties.DIGEST_SHA256, KeyProperties.DIGEST_SHA512)
            .setKeySize(2048)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            builder.setUserAuthenticationParameters(0, KeyProperties.AUTH_BIOMETRIC)
        } else {
            @Suppress("DEPRECATION")
            builder.setUserAuthenticationRequired(true)
        }

        val generator = KeyPairGenerator.getInstance(
            KeyProperties.KEY_ALGORITHM_RSA,
            "AndroidKeyStore"
        )
        generator.initialize(builder.build())
        generator.generateKeyPair()
    }
}