package org.fynex.manager.core.apk

import java.nio.ByteBuffer
import java.nio.ByteOrder

/**
 * High-performance pure Kotlin Android Binary XML to Text XML Decoder.
 * Decodes AndroidManifest.xml and binary layout XMLs found inside APK files.
 */
object BinaryXmlDecoder {

    private const val CHUNK_AXML_FILE = 0x00080003
    private const val CHUNK_STRING_POOL = 0x001C0001
    private const val CHUNK_RESOURCE_MAP = 0x00080180
    private const val CHUNK_START_NAMESPACE = 0x00100100
    private const val CHUNK_END_NAMESPACE = 0x00100101
    private const val CHUNK_START_TAG = 0x00100102
    private const val CHUNK_END_TAG = 0x00100103
    private const val CHUNK_TEXT = 0x00100104

    fun decode(bytes: ByteArray): String {
        try {
            val buffer = ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN)
            val magic = buffer.int
            if (magic != CHUNK_AXML_FILE) {
                // If not binary XML, return as plain text (might already be decoded or plain XML)
                return String(bytes, Charsets.UTF_8)
            }

            val fileSize = buffer.int
            val stringTable = mutableListOf<String>()
            val xmlBuilder = StringBuilder()
            xmlBuilder.appendLine("<?xml version=\"1.0\" encoding=\"utf-8\"?>")

            var indent = 0
            val namespaces = mutableMapOf<String, String>() // uri -> prefix

            while (buffer.hasRemaining()) {
                val chunkType = buffer.int
                val chunkSize = buffer.int

                if (chunkSize <= 0) break
                val chunkStart = buffer.position() - 8

                when (chunkType) {
                    CHUNK_STRING_POOL -> {
                        parseStringPool(buffer, stringTable, chunkStart, chunkSize)
                        buffer.position(chunkStart + chunkSize)
                    }
                    CHUNK_RESOURCE_MAP -> {
                        // Skip resource map ids
                        buffer.position(chunkStart + chunkSize)
                    }
                    CHUNK_START_NAMESPACE -> {
                        val lineNumber = buffer.int
                        val comment = buffer.int
                        val prefixIdx = buffer.int
                        val uriIdx = buffer.int
                        val prefix = getString(stringTable, prefixIdx)
                        val uri = getString(stringTable, uriIdx)
                        if (prefix.isNotEmpty() && uri.isNotEmpty()) {
                            namespaces[uri] = prefix
                        }
                        buffer.position(chunkStart + chunkSize)
                    }
                    CHUNK_END_NAMESPACE -> {
                        buffer.position(chunkStart + chunkSize)
                    }
                    CHUNK_START_TAG -> {
                        val lineNumber = buffer.int
                        val comment = buffer.int
                        val nsIdx = buffer.int
                        val nameIdx = buffer.int
                        val attrStart = buffer.short
                        val attrSize = buffer.short
                        val attrCount = buffer.short.toInt() and 0xFFFF
                        val idIndex = buffer.short
                        val classIndex = buffer.short
                        val styleIndex = buffer.short

                        val tagName = getString(stringTable, nameIdx)
                        val indentStr = "  ".repeat(indent)
                        xmlBuilder.append("$indentStr<$tagName")

                        // Add namespace attributes on root element
                        if (indent == 0) {
                            namespaces.forEach { (uri, prefix) ->
                                xmlBuilder.append(" xmlns:$prefix=\"$uri\"")
                            }
                        }

                        // Attributes
                        for (i in 0 until attrCount) {
                            val attrNsIdx = buffer.int
                            val attrNameIdx = buffer.int
                            val attrRawValIdx = buffer.int
                            val attrType = buffer.int shr 24
                            val attrData = buffer.int

                            val attrNs = getString(stringTable, attrNsIdx)
                            val attrName = getString(stringTable, attrNameIdx)
                            val rawVal = getString(stringTable, attrRawValIdx)

                            val prefix = if (attrNs.isNotEmpty()) "${namespaces[attrNs] ?: "android"}:" else ""
                            val valStr = if (rawVal.isNotEmpty()) {
                                rawVal
                            } else {
                                formatAttrValue(attrType, attrData)
                            }
                            xmlBuilder.append(" $prefix$attrName=\"$valStr\"")
                        }

                        xmlBuilder.appendLine(">")
                        indent++
                        buffer.position(chunkStart + chunkSize)
                    }
                    CHUNK_END_TAG -> {
                        indent = (indent - 1).coerceAtLeast(0)
                        val lineNumber = buffer.int
                        val comment = buffer.int
                        val nsIdx = buffer.int
                        val nameIdx = buffer.int
                        val tagName = getString(stringTable, nameIdx)
                        val indentStr = "  ".repeat(indent)
                        xmlBuilder.appendLine("$indentStr</$tagName>")
                        buffer.position(chunkStart + chunkSize)
                    }
                    CHUNK_TEXT -> {
                        val lineNumber = buffer.int
                        val comment = buffer.int
                        val nameIdx = buffer.int
                        val text = getString(stringTable, nameIdx)
                        xmlBuilder.append("  ".repeat(indent)).appendLine(text)
                        buffer.position(chunkStart + chunkSize)
                    }
                    else -> {
                        // Unknown chunk, advance
                        if (chunkStart + chunkSize <= bytes.size) {
                            buffer.position(chunkStart + chunkSize)
                        } else {
                            break
                        }
                    }
                }
            }
            return xmlBuilder.toString()
        } catch (e: Exception) {
            return "<!-- Erro ao decodificar Binary XML: ${e.message} -->\n" + String(bytes, Charsets.ISO_8859_1)
        }
    }

    private fun parseStringPool(buffer: ByteBuffer, stringTable: MutableList<String>, chunkStart: Int, chunkSize: Int) {
        val stringCount = buffer.int
        val styleCount = buffer.int
        val flags = buffer.int
        val stringsStart = buffer.int
        val stylesStart = buffer.int

        val isUtf8 = (flags and (1 shl 8)) != 0
        val offsets = IntArray(stringCount)
        for (i in 0 until stringCount) {
            offsets[i] = buffer.int
        }

        val poolStart = chunkStart + stringsStart
        for (i in 0 until stringCount) {
            val strPos = poolStart + offsets[i]
            if (strPos >= chunkStart + chunkSize) {
                stringTable.add("")
                continue
            }
            buffer.position(strPos)
            if (isUtf8) {
                // UTF-8 string: length is 1 or 2 bytes
                var len = buffer.get().toInt() and 0xFF
                if ((len and 0x80) != 0) {
                    len = ((len and 0x7F) shl 8) or (buffer.get().toInt() and 0xFF)
                }
                var byteLen = buffer.get().toInt() and 0xFF
                if ((byteLen and 0x80) != 0) {
                    byteLen = ((byteLen and 0x7F) shl 8) or (buffer.get().toInt() and 0xFF)
                }
                val strBytes = ByteArray(byteLen)
                buffer.get(strBytes)
                stringTable.add(String(strBytes, Charsets.UTF_8))
            } else {
                // UTF-16 string
                var charLen = buffer.short.toInt() and 0xFFFF
                if ((charLen and 0x8000) != 0) {
                    charLen = ((charLen and 0x7FFF) shl 16) or (buffer.short.toInt() and 0xFFFF)
                }
                val chars = CharArray(charLen)
                for (c in 0 until charLen) {
                    chars[c] = buffer.short.toInt().toChar()
                }
                stringTable.add(String(chars))
            }
        }
    }

    private fun getString(stringTable: List<String>, index: Int): String {
        return if (index in stringTable.indices) stringTable[index] else ""
    }

    private fun formatAttrValue(type: Int, data: Int): String {
        return when (type) {
            3 -> "@0x${Integer.toHexString(data)}" // reference
            16 -> data.toString() // int dec
            17 -> "0x${Integer.toHexString(data)}" // int hex
            18 -> if (data != 0) "true" else "false" // boolean
            28, 29, 30, 31 -> "#${Integer.toHexString(data)}" // color
            else -> data.toString()
        }
    }
}
