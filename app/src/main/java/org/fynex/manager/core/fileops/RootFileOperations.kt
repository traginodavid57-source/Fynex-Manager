package org.fynex.manager.core.fileops

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.fynex.manager.core.model.FileItem
import java.io.BufferedReader
import java.io.DataOutputStream
import java.io.File
import java.io.InputStreamReader

object RootFileOperations {

    private var rootAvailable: Boolean? = null

    suspend fun isRootAvailable(): Boolean = withContext(Dispatchers.IO) {
        if (rootAvailable != null) return@withContext rootAvailable!!
        try {
            val process = Runtime.getRuntime().exec(arrayOf("su", "-c", "id"))
            val reader = BufferedReader(InputStreamReader(process.inputStream))
            val line = reader.readLine()
            val available = line != null && line.contains("uid=0")
            process.waitFor()
            rootAvailable = available
            available
        } catch (e: Exception) {
            rootAvailable = false
            false
        }
    }

    suspend fun executeCommand(cmd: String): Pair<Int, String> = withContext(Dispatchers.IO) {
        try {
            val process = Runtime.getRuntime().exec("su")
            val os = DataOutputStream(process.outputStream)
            os.writeBytes("$cmd\n")
            os.writeBytes("exit\n")
            os.flush()

            val reader = BufferedReader(InputStreamReader(process.inputStream))
            val errReader = BufferedReader(InputStreamReader(process.errorStream))
            val output = StringBuilder()
            var line: String?

            while (reader.readLine().also { line = it } != null) {
                output.appendLine(line)
            }
            while (errReader.readLine().also { line = it } != null) {
                output.appendLine(line)
            }

            val code = process.waitFor()
            Pair(code, output.toString())
        } catch (e: Exception) {
            Pair(-1, e.message ?: "Erro ao executar comando root")
        }
    }

    suspend fun listDirectory(path: String): List<FileItem> = withContext(Dispatchers.IO) {
        val result = mutableListOf<FileItem>()
        val (_, output) = executeCommand("ls -la \"$path\"")
        val lines = output.lines()

        for (line in lines) {
            val parts = line.trim().split(Regex("\\s+"))
            if (parts.size >= 8 && !parts[0].startsWith("total")) {
                val perms = parts[0]
                val isDir = perms.startsWith("d")
                val size = parts[4].toLongOrNull() ?: 0L
                val name = parts.subList(7, parts.size).joinToString(" ")
                if (name == "." || name == "..") continue

                val file = File(path, name)
                result.add(
                    FileItem(
                        file = file,
                        name = name,
                        path = file.absolutePath,
                        isDirectory = isDir,
                        size = size,
                        permissions = perms.take(4)
                    )
                )
            }
        }
        result
    }

    suspend fun remountSystemRw(): Boolean = withContext(Dispatchers.IO) {
        val (code, _) = executeCommand("mount -o remount,rw /")
        code == 0
    }
}
