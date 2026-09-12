package org.fynex.manager.core.apk

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.bouncycastle.asn1.x500.X500Name
import org.bouncycastle.cert.jcajce.JcaX509CertificateConverter
import org.bouncycastle.cert.jcajce.JcaX509v3CertificateBuilder
import org.bouncycastle.cms.CMSProcessableByteArray
import org.bouncycastle.cms.CMSSignedDataGenerator
import org.bouncycastle.cms.jcajce.JcaSimpleSignerInfoGeneratorBuilder
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.math.BigInteger
import java.security.KeyPairGenerator
import java.security.MessageDigest
import java.security.PrivateKey
import java.security.cert.X509Certificate
import java.util.Date
import java.util.jar.Attributes
import java.util.jar.Manifest
import java.util.zip.ZipEntry
import java.util.zip.ZipFile
import java.util.zip.ZipOutputStream

object ApkSignerUtil {

    suspend fun signApk(
        inputApk: File,
        outputApk: File,
        onProgress: ((String) -> Unit)? = null
    ): Result<File> = withContext(Dispatchers.IO) {
        try {
            onProgress?.invoke("Gerando chave de assinatura AOSP TestKey...")
            val (privateKey, cert) = generateTestKeyPair()

            onProgress?.invoke("Lendo entradas do APK original...")
            val md = MessageDigest.getInstance("SHA-256")
            val manifest = Manifest()
            manifest.mainAttributes[Attributes.Name.MANIFEST_VERSION] = "1.0"
            manifest.mainAttributes[Attributes.Name("Created-By")] = "Fynex Open-Source APK Signer"

            val entryDigests = mutableMapOf<String, String>()

            ZipFile(inputApk).use { inZip ->
                val entries = inZip.entries()
                while (entries.hasMoreElements()) {
                    val entry = entries.nextElement()
                    if (entry.name.startsWith("META-INF/") && (entry.name.endsWith(".SF") || entry.name.endsWith(".RSA") || entry.name.endsWith(".MF") || entry.name.endsWith(".DSA"))) {
                        continue // Skip old signature files
                    }
                    val bytes = inZip.getInputStream(entry).readBytes()
                    val digest = md.digest(bytes)
                    val digestBase64 = android.util.Base64.encodeToString(digest, android.util.Base64.NO_WRAP)
                    entryDigests[entry.name] = digestBase64

                    val attr = Attributes()
                    attr[Attributes.Name("SHA-256-Digest")] = digestBase64
                    manifest.entries[entry.name] = attr
                }

                onProgress?.invoke("Gerando arquivos de assinatura META-INF...")
                // Generate MANIFEST.MF bytes
                val manifestBytes = ByteArrayOutputStream().apply { manifest.write(this) }.toByteArray()

                // Generate CERT.SF
                val sf = Manifest()
                sf.mainAttributes[Attributes.Name("Signature-Version")] = "1.0"
                sf.mainAttributes[Attributes.Name("Created-By")] = "Fynex Open-Source APK Signer"
                val manifestDigest = android.util.Base64.encodeToString(md.digest(manifestBytes), android.util.Base64.NO_WRAP)
                sf.mainAttributes[Attributes.Name("SHA-256-Digest-Manifest")] = manifestDigest

                for ((name, _) in entryDigests) {
                    val entryAttr = Attributes()
                    val entryHeader = "Name: $name\r\nSHA-256-Digest: ${entryDigests[name]}\r\n\r\n".toByteArray(Charsets.UTF_8)
                    entryAttr[Attributes.Name("SHA-256-Digest")] = android.util.Base64.encodeToString(md.digest(entryHeader), android.util.Base64.NO_WRAP)
                    sf.entries[name] = entryAttr
                }
                val sfBytes = ByteArrayOutputStream().apply { sf.write(this) }.toByteArray()

                // Generate CERT.RSA with BouncyCastle
                val certGen = CMSSignedDataGenerator()
                val signerInfo = JcaSimpleSignerInfoGeneratorBuilder().build("SHA256withRSA", privateKey, cert)
                certGen.addSignerInfoGenerator(signerInfo)
                certGen.addCertificate(org.bouncycastle.cert.X509CertificateHolder(cert.encoded))
                val signedData = certGen.generate(CMSProcessableByteArray(sfBytes), true)
                val rsaBytes = signedData.encoded

                onProgress?.invoke("Gravando APK assinado final...")
                ZipOutputStream(FileOutputStream(outputApk)).use { outZip ->
                    // Write MANIFEST.MF
                    outZip.putNextEntry(ZipEntry("META-INF/MANIFEST.MF"))
                    outZip.write(manifestBytes)
                    outZip.closeEntry()

                    // Write CERT.SF
                    outZip.putNextEntry(ZipEntry("META-INF/CERT.SF"))
                    outZip.write(sfBytes)
                    outZip.closeEntry()

                    // Write CERT.RSA
                    outZip.putNextEntry(ZipEntry("META-INF/CERT.RSA"))
                    outZip.write(rsaBytes)
                    outZip.closeEntry()

                    // Write all other entries
                    val otherEntries = inZip.entries()
                    while (otherEntries.hasMoreElements()) {
                        val entry = otherEntries.nextElement()
                        if (entry.name.startsWith("META-INF/") && (entry.name.endsWith(".SF") || entry.name.endsWith(".RSA") || entry.name.endsWith(".MF") || entry.name.endsWith(".DSA"))) {
                            continue
                        }
                        val newEntry = ZipEntry(entry.name)
                        outZip.putNextEntry(newEntry)
                        inZip.getInputStream(entry).copyTo(outZip)
                        outZip.closeEntry()
                    }
                }
            }

            onProgress?.invoke("Concluído!")
            Result.success(outputApk)
        } catch (e: Exception) {
            e.printStackTrace()
            Result.failure(e)
        }
    }

    private fun generateTestKeyPair(): Pair<PrivateKey, X509Certificate> {
        val keyGen = KeyPairGenerator.getInstance("RSA")
        keyGen.initialize(2048)
        val keyPair = keyGen.generateKeyPair()

        val now = System.currentTimeMillis()
        val startDate = Date(now)
        val endDate = Date(now + 3650L * 24 * 60 * 60 * 1000) // 10 years
        val serial = BigInteger.valueOf(now)

        val owner = X500Name("CN=Fynex TestKey, O=Fynex Open Source, C=BR")
        val certBuilder = JcaX509v3CertificateBuilder(
            owner,
            serial,
            startDate,
            endDate,
            owner,
            keyPair.public
        )

        val contentSigner = JcaContentSignerBuilder("SHA256withRSA").build(keyPair.private)
        val cert = JcaX509CertificateConverter().getCertificate(certBuilder.build(contentSigner))

        return Pair(keyPair.private, cert)
    }
}
