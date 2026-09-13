package org.fynex.manager.core.apk

import android.content.Context
import android.content.pm.PackageManager
import android.graphics.drawable.Drawable
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayInputStream
import java.io.File
import java.io.RandomAccessFile
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.security.MessageDigest
import java.security.cert.CertificateFactory
import java.security.cert.X509Certificate
import java.util.zip.ZipFile

data class ApkDetails(
    val fileName: String,
    val filePath: String,
    val fileSize: Long,
    val appName: String,
    val packageName: String,
    val versionName: String,
    val versionCode: Long,
    val minSdk: Int,
    val targetSdk: Int,
    val icon: Drawable? = null,
    val permissions: List<String> = emptyList(),
    val activities: List<String> = emptyList(),
    val services: List<String> = emptyList(),
    val receivers: List<String> = emptyList(),
    val nativeArchitectures: List<String> = emptyList(),
    val dexCount: Int = 0,
    val certIssuer: String? = null,
    val certSubject: String? = null,
    val certSha256: String? = null,
    val certValidUntil: String? = null,
    val hasV1Signature: Boolean = false,
    val hasV2Signature: Boolean = false
)

object ApkInspector {

    private const val APK_SIGNING_BLOCK_MAGIC = "APK Sig Block 42"

    suspend fun inspect(context: Context, apkFile: File): ApkDetails = withContext(Dispatchers.IO) {
        var appName = apkFile.nameWithoutExtension
        var packageName = "Desconhecido"
        var versionName = "1.0"
        var versionCode = 1L
        var minSdk = 21
        var targetSdk = 34
        var icon: Drawable? = null
        val permissions = mutableListOf<String>()
        val activities = mutableListOf<String>()
        val services = mutableListOf<String>()
        val receivers = mutableListOf<String>()
        val nativeArchs = mutableSetOf<String>()
        var dexCount = 0
        var certIssuer: String? = null
        var certSubject: String? = null
        var certSha256: String? = null
        var certValidUntil: String? = null
        var hasV1 = false
        var hasV2 = false

        try {
            // Use Android PackageManager if possible
            val pm = context.packageManager
            val archiveInfo = pm.getPackageArchiveInfo(
                apkFile.absolutePath,
                PackageManager.GET_PERMISSIONS or
                PackageManager.GET_ACTIVITIES or
                PackageManager.GET_SERVICES or
                PackageManager.GET_RECEIVERS or
                PackageManager.GET_SIGNATURES
            )

            if (archiveInfo != null) {
                packageName = archiveInfo.packageName ?: "Desconhecido"
                versionName = archiveInfo.versionName ?: "1.0"
                versionCode = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.P) {
                    archiveInfo.longVersionCode
                } else {
                    @Suppress("DEPRECATION")
                    archiveInfo.versionCode.toLong()
                }

                archiveInfo.applicationInfo?.let { appInfo ->
                    appInfo.sourceDir = apkFile.absolutePath
                    appInfo.publicSourceDir = apkFile.absolutePath
                    appName = pm.getApplicationLabel(appInfo).toString()
                    icon = pm.getApplicationIcon(appInfo)
                    minSdk = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N) {
                        appInfo.minSdkVersion
                    } else 21
                    targetSdk = appInfo.targetSdkVersion
                }

                archiveInfo.requestedPermissions?.forEach { permissions.add(it) }
                archiveInfo.activities?.forEach { activities.add(it.name) }
                archiveInfo.services?.forEach { services.add(it.name) }
                archiveInfo.receivers?.forEach { receivers.add(it.name) }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        // Inspect ZIP structure for DEX files, native libs and certs
        try {
            ZipFile(apkFile).use { zip ->
                val entries = zip.entries()
                while (entries.hasMoreElements()) {
                    val entry = entries.nextElement()
                    val name = entry.name
                    if (name.endsWith(".dex") && name.startsWith("classes")) {
                        dexCount++
                    } else if (name.startsWith("lib/")) {
                        val parts = name.split("/")
                        if (parts.size > 1 && parts[1].isNotEmpty()) {
                            nativeArchs.add(parts[1])
                        }
                    } else if (name.startsWith("META-INF/") && (name.endsWith(".RSA") || name.endsWith(".DSA") || name.endsWith(".EC"))) {
                        hasV1 = true
                        try {
                            val certBytes = zip.getInputStream(entry).readBytes()
                            val cf = CertificateFactory.getInstance("X.509")
                            val cert = cf.generateCertificate(ByteArrayInputStream(certBytes)) as? X509Certificate
                            if (cert != null) {
                                certIssuer = cert.issuerX500Principal.name
                                certSubject = cert.subjectX500Principal.name
                                certValidUntil = cert.notAfter.toString()

                                val md = MessageDigest.getInstance("SHA-256")
                                certSha256 = md.digest(cert.encoded).joinToString(":") { "%02X".format(it) }
                            }
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        // Detect APK Signing Block (v2/v3) located right before the Central Directory.
        // Its last 24 bytes are: 8-byte block size (LE) + 16-byte magic "APK Sig Block 42".
        try {
            RandomAccessFile(apkFile, "r").use { raf ->
                val len = raf.length()
                if (len >= 22) {
                    raf.seek(len - 22)
                    val eocd = ByteArray(22)
                    raf.readFully(eocd)
                    val isEocd = (eocd[0].toInt() and 0xFF == 0x50) && (eocd[1].toInt() and 0xFF == 0x4B)
                    if (isEocd) {
                        val centralDirOffset = ByteBuffer.wrap(eocd, 16, 4)
                            .order(ByteOrder.LITTLE_ENDIAN).int.toLong() and 0xFFFFFFFFL
                        if (centralDirOffset >= 24) {
                            raf.seek(centralDirOffset - 24)
                            val tail = ByteArray(24)
                            raf.readFully(tail)
                            val magic = String(tail, 8, 16, Charsets.UTF_8)
                            hasV2 = magic == APK_SIGNING_BLOCK_MAGIC
                        }
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        ApkDetails(
            fileName = apkFile.name,
            filePath = apkFile.absolutePath,
            fileSize = apkFile.length(),
            appName = appName,
            packageName = packageName,
            versionName = versionName,
            versionCode = versionCode,
            minSdk = minSdk,
            targetSdk = targetSdk,
            icon = icon,
            permissions = permissions.distinct(),
            activities = activities,
            services = services,
            receivers = receivers,
            nativeArchitectures = nativeArchs.toList(),
            dexCount = dexCount,
            certIssuer = certIssuer,
            certSubject = certSubject,
            certSha256 = certSha256,
            certValidUntil = certValidUntil,
            hasV1Signature = hasV1,
            hasV2Signature = hasV2
        )
    }
}
