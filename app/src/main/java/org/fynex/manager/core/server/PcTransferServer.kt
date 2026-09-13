package org.fynex.manager.core.server

import android.os.Environment
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import java.io.BufferedReader
import java.io.File
import java.io.FileInputStream
import java.io.InputStreamReader
import java.io.OutputStream
import java.net.Inet4Address
import java.net.NetworkInterface
import java.net.ServerSocket
import java.net.Socket
import java.net.URLDecoder

class PcTransferServer(val port: Int = 8080) {

    private val token: String = prepareToken()

    private var serverSocket: ServerSocket? = null
    var isRunning = false
        private set

    fun start() {
        if (isRunning) return
        try {
            serverSocket = ServerSocket(port)
            isRunning = true
            GlobalScope.launch(Dispatchers.IO) {
                while (isRunning) {
                    try {
                        val client = serverSocket?.accept() ?: break
                        handleClient(client)
                    } catch (e: Exception) {
                        break
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
            isRunning = false
        }
    }

    fun stop() {
        isRunning = false
        try {
            serverSocket?.close()
        } catch (e: Exception) {
            e.printStackTrace()
        }
        serverSocket = null
    }

    private fun handleClient(socket: Socket) {
        GlobalScope.launch(Dispatchers.IO) {
            try {
                val input = socket.getInputStream()
                val output = socket.getOutputStream()
                val reader = BufferedReader(InputStreamReader(input))

                val requestLine = reader.readLine() ?: return@launch
                val parts = requestLine.split(" ")
                if (parts.size < 2) return@launch

                val method = parts[0]
                val rawPath = parts[1]
                val decodedPath = URLDecoder.decode(rawPath, "UTF-8")

                if (method == "GET") {
                    handleGet(decodedPath, output)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                try { socket.close() } catch (_: Exception) {}
            }
        }
    }

    private fun isAuthorized(query: String): Boolean {
        if (query.isBlank()) return false
        val param = query.split("&").firstOrNull { it.startsWith("t=") } ?: return false
        return param.substring(2) == token
    }

    private fun withToken(relativePath: String): String {
        val sep = if (relativePath.contains("?")) "&" else "?"
        return "$relativePath$sep" + "t=$token"
    }

    private fun handleGet(rawPath: String, output: OutputStream) {
        val querySplit = rawPath.split("?", limit = 2)
        val path = querySplit[0]
        val query = querySplit.getOrElse(1) { "" }

        if (!isAuthorized(query)) {
            val body = "403 Forbidden - Token inválido. Use a URL completa exibida no app."
            val header = "HTTP/1.1 403 Forbidden\r\nContent-Type: text/plain; charset=utf-8\r\nContent-Length: ${body.length}\r\nConnection: close\r\n\r\n"
            output.write(header.toByteArray(Charsets.UTF_8))
            output.write(body.toByteArray(Charsets.UTF_8))
            output.flush()
            return
        }

        val rootDir = Environment.getExternalStorageDirectory()
        val targetFile = if (path == "/" || path.isBlank()) {
            rootDir
        } else {
            File(rootDir, path.removePrefix("/"))
        }

        if (targetFile.isDirectory) {
            val html = renderDirectoryHtml(targetFile, path)
            val bytes = html.toByteArray(Charsets.UTF_8)
            val header = "HTTP/1.1 200 OK\r\nContent-Type: text/html; charset=utf-8\r\nContent-Length: ${bytes.size}\r\nConnection: close\r\n\r\n"
            output.write(header.toByteArray(Charsets.UTF_8))
            output.write(bytes)
            output.flush()
        } else if (targetFile.isFile && targetFile.exists()) {
            val length = targetFile.length()
            val header = "HTTP/1.1 200 OK\r\nContent-Type: application/octet-stream\r\nContent-Disposition: attachment; filename=\"${targetFile.name}\"\r\nContent-Length: $length\r\nConnection: close\r\n\r\n"
            output.write(header.toByteArray(Charsets.UTF_8))
            FileInputStream(targetFile).use { fileIn ->
                fileIn.copyTo(output, 64 * 1024)
            }
            output.flush()
        } else {
            val notFound = "HTTP/1.1 404 Not Found\r\nContent-Type: text/plain\r\nContent-Length: 9\r\n\r\nNot Found"
            output.write(notFound.toByteArray(Charsets.UTF_8))
            output.flush()
        }
    }

    private fun renderDirectoryHtml(dir: File, currentUrlPath: String): String {
        val files = dir.listFiles()?.sortedWith(compareBy({ !it.isDirectory }, { it.name.lowercase() })) ?: emptyList()
        val parentPath = if (currentUrlPath == "/" || currentUrlPath.isBlank()) "" else {
            val clean = currentUrlPath.trimEnd('/')
            val idx = clean.lastIndexOf('/')
            if (idx <= 0) "/" else clean.substring(0, idx)
        }

        val rows = StringBuilder()
        if (parentPath.isNotEmpty()) {
            rows.append("""<tr><td>📁</td><td><a href="${withToken(parentPath)}">.. (Voltar)</a></td><td>-</td><td>-</td></tr>""")
        }

        for (f in files) {
            val icon = if (f.isDirectory) "📁" else "📄"
            val subPath = if (currentUrlPath.endsWith("/")) "$currentUrlPath${f.name}" else "$currentUrlPath/${f.name}"
            val size = if (f.isDirectory) "Pasta" else "${f.length() / 1024} KB"
            val link = withToken(subPath)
            val downloadBtn = if (f.isDirectory) "" else """<a href="$link" class="dl-btn">Baixar</a>"""
            rows.append("""
                <tr>
                    <td>$icon</td>
                    <td><a href="$link">${f.name}</a></td>
                    <td>$size</td>
                    <td>$downloadBtn</td>
                </tr>
            """.trimIndent())
        }

        return """
            <!DOCTYPE html>
            <html lang="pt-BR">
            <head>
                <meta charset="UTF-8">
                <meta name="viewport" content="width=device-width, initial-scale=1.0">
                <title>Fynex PC Web Transfer</title>
                <style>
                    body { font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif; background: #0f172a; color: #f8fafc; margin: 0; padding: 24px; }
                    .header { display: flex; align-items: center; gap: 16px; margin-bottom: 24px; }
                    .logo { font-size: 24px; font-weight: bold; color: #3b82f6; }
                    .badge { background: #10b981; color: #000; font-size: 12px; font-weight: 600; padding: 4px 8px; border-radius: 6px; }
                    .card { background: #1e293b; border-radius: 12px; padding: 20px; box-shadow: 0 4px 6px -1px rgba(0,0,0,0.3); }
                    table { width: 100%; border-collapse: collapse; margin-top: 12px; }
                    th, td { text-align: left; padding: 12px; border-bottom: 1px solid #334155; }
                    th { color: #94a3b8; font-size: 14px; text-transform: uppercase; }
                    a { color: #60a5fa; text-decoration: none; font-weight: 500; }
                    a:hover { text-decoration: underline; }
                    .dl-btn { background: #2563eb; color: #fff; padding: 6px 12px; border-radius: 6px; font-size: 13px; text-decoration: none; }
                    .dl-btn:hover { background: #1d4ed8; text-decoration: none; }
                    .path-bar { background: #334155; padding: 10px 14px; border-radius: 8px; font-family: monospace; color: #38bdf8; margin-bottom: 16px; }
                </style>
            </head>
            <body>
                <div class="header">
                    <span class="logo">⚡ Fynex Web Transfer</span>
                    <span class="badge">Open-Source</span>
                </div>
                <div class="card">
                    <div class="path-bar">Local: ${dir.absolutePath}</div>
                    <table>
                        <thead>
                            <tr>
                                <th style="width: 40px"></th>
                                <th>Nome</th>
                                <th style="width: 120px">Tamanho</th>
                                <th style="width: 100px">Ação</th>
                            </tr>
                        </thead>
                        <tbody>
                            $rows
                        </tbody>
                    </table>
                </div>
            </body>
            </html>
        """.trimIndent()
    }

    companion object {
        @Volatile
        var activeToken: String? = null
            private set

        private fun generateToken(): String = (100_000..999_999).random().toString()

        fun prepareToken(): String {
            if (activeToken == null) {
                activeToken = generateToken()
            }
            return activeToken!!
        }

        fun getLocalIpAddress(): String? {
            try {
                val interfaces = NetworkInterface.getNetworkInterfaces()
                while (interfaces.hasMoreElements()) {
                    val intf = interfaces.nextElement()
                    val addrs = intf.inetAddresses
                    while (addrs.hasMoreElements()) {
                        val addr = addrs.nextElement()
                        if (!addr.isLoopbackAddress && addr is Inet4Address) {
                            return addr.hostAddress
                        }
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
            return null
        }
    }
}
