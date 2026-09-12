package org.fynex.manager.core.ai

enum class AiProvider(val displayName: String, val defaultModel: String, val defaultUrl: String) {
    GEMINI("Google Gemini (Recomendado / Grátis)", "gemini-1.5-flash", "https://generativelanguage.googleapis.com/v1beta/models/"),
    OPENAI("OpenAI (ChatGPT)", "gpt-4o-mini", "https://api.openai.com/v1/chat/completions"),
    CLAUDE("Anthropic Claude", "claude-3-5-haiku-latest", "https://api.anthropic.com/v1/messages"),
    GROQ("Groq (Ultra-Rápido)", "llama-3.3-70b-versatile", "https://api.groq.com/openai/v1/chat/completions"),
    OLLAMA("Ollama (Local / Termux)", "llama3.2", "http://127.0.0.1:11434/api/generate")
}

data class AiSettings(
    val provider: AiProvider = AiProvider.GEMINI,
    val apiKey: String = "",
    val customModel: String = "",
    val customEndpoint: String = "",
    val temperature: Float = 0.7f,
    val systemPrompt: String = "Você é o assistente inteligente do Fynex, um gerenciador de arquivos avançado e open-source para Android. Ajude o usuário de forma concisa, direta e útil."
)
