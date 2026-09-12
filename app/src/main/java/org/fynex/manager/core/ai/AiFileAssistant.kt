package org.fynex.manager.core.ai

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

object AiFileAssistant {

    suspend fun summarizeFile(file: File, settings: AiSettings): Result<String> = withContext(Dispatchers.IO) {
        val content = try {
            file.readText().take(15000)
        } catch (e: Exception) {
            return@withContext Result.failure(e)
        }
        val prompt = "Por favor, resuma detalhadamente o conteúdo deste arquivo (${file.name}), destacando pontos principais, estrutura e finalidade."
        AiService.chat(prompt, content, settings)
    }

    suspend fun explainCode(code: String, language: String, settings: AiSettings): Result<String> = withContext(Dispatchers.IO) {
        val prompt = "Explique detalhadamente o que este código $language faz, identificando a lógica, métodos e possíveis melhorias ou riscos de segurança."
        AiService.chat(prompt, code, settings)
    }

    suspend fun analyzeApkManifest(manifestXml: String, settings: AiSettings): Result<String> = withContext(Dispatchers.IO) {
        val prompt = "Analise este AndroidManifest.xml de APK: identifique as permissões sensíveis/perigosas, componentes exportados, possíveis vulnerabilidades de segurança e propósito aparente do app."
        AiService.chat(prompt, manifestXml, settings)
    }
}
