package org.fynex.manager.core.apk

import java.io.File
import java.io.RandomAccessFile
import java.nio.ByteBuffer
import java.nio.ByteOrder

data class DexInfo(
    val fileName: String,
    val fileSize: Long,
    val version: String,
    val checksum: Long,
    val signatureHex: String,
    val stringIdsSize: Int,
    val typeIdsSize: Int,
    val protoIdsSize: Int,
    val fieldIdsSize: Int,
    val methodIdsSize: Int,
    val classDefsSize: Int,
    val isValid: Boolean = true,
    val error: String? = null
)

object DexInspector {

    fun inspect(bytes: ByteArray, name: String = "classes.dex"): DexInfo {
        if (bytes.size < 0x70) {
            return DexInfo(name, bytes.size.toLong(), "", 0L, "", 0, 0, 0, 0, 0, 0, false, "Arquivo muito pequeno para ser DEX válido")
        }

        try {
            val buf = ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN)

            // Magic: dex\n035\0 or dex\n038\0 or dex\n039\0
            val magicBytes = ByteArray(8)
            buf.get(magicBytes)
            val magicStr = String(magicBytes)
            if (!magicStr.startsWith("dex\n")) {
                return DexInfo(name, bytes.size.toLong(), "", 0L, "", 0, 0, 0, 0, 0, 0, false, "Assinatura DEX inválida: $magicStr")
            }
            val version = magicStr.substring(4, 7)

            val checksum = buf.int.toLong() and 0xFFFFFFFFL
            val sigBytes = ByteArray(20)
            buf.get(sigBytes)
            val signatureHex = sigBytes.joinToString("") { "%02x".format(it) }

            val fileSize = buf.int.toLong() and 0xFFFFFFFFL
            val headerSize = buf.int
            val endianTag = buf.int

            val linkSize = buf.int
            val linkOff = buf.int
            val mapOff = buf.int

            val stringIdsSize = buf.int
            val stringIdsOff = buf.int

            val typeIdsSize = buf.int
            val typeIdsOff = buf.int

            val protoIdsSize = buf.int
            val protoIdsOff = buf.int

            val fieldIdsSize = buf.int
            val fieldIdsOff = buf.int

            val methodIdsSize = buf.int
            val methodIdsOff = buf.int

            val classDefsSize = buf.int

            return DexInfo(
                fileName = name,
                fileSize = fileSize,
                version = version,
                checksum = checksum,
                signatureHex = signatureHex,
                stringIdsSize = stringIdsSize,
                typeIdsSize = typeIdsSize,
                protoIdsSize = protoIdsSize,
                fieldIdsSize = fieldIdsSize,
                methodIdsSize = methodIdsSize,
                classDefsSize = classDefsSize,
                isValid = true
            )
        } catch (e: Exception) {
            return DexInfo(name, bytes.size.toLong(), "", 0L, "", 0, 0, 0, 0, 0, 0, false, e.message)
        }
    }

    fun inspectFile(file: File): DexInfo {
        return try {
            val bytes = file.readBytes()
            inspect(bytes, file.name)
        } catch (e: Exception) {
            DexInfo(file.name, file.length(), "", 0L, "", 0, 0, 0, 0, 0, 0, false, e.message)
        }
    }
}
