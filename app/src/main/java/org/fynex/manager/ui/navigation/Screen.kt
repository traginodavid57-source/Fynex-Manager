package org.fynex.manager.ui.navigation

sealed class Screen(val route: String, val title: String) {
    object Home : Screen("home", "Início")
    object Explorer : Screen("explorer", "Arquivos")
    object Search : Screen("search", "Busca")
    object Tools : Screen("tools", "Ferramentas")
    object Plugins : Screen("plugins", "Plugins")
    object Vault : Screen("vault", "Cofre Seguro")
    object Donate : Screen("donate", "Apoiar Projeto")
    object Settings : Screen("settings", "Configurações")

    // Sub-screens
    object ApkInspector : Screen("apk_inspector?path={path}", "Inspecionar APK") {
        fun createRoute(path: String) = "apk_inspector?path=${android.net.Uri.encode(path)}"
    }
    object CodeEditor : Screen("code_editor?path={path}", "Editor de Código") {
        fun createRoute(path: String) = "code_editor?path=${android.net.Uri.encode(path)}"
    }
    object HexEditor : Screen("hex_editor?path={path}", "Editor Hexadecimal") {
        fun createRoute(path: String) = "hex_editor?path=${android.net.Uri.encode(path)}"
    }
    object DiffViewer : Screen("diff_viewer?path1={path1}&path2={path2}", "Comparador Diff") {
        fun createRoute(path1: String, path2: String) = "diff_viewer?path1=${android.net.Uri.encode(path1)}&path2=${android.net.Uri.encode(path2)}"
    }
    object AiChat : Screen("ai_chat?path={path}", "Assistente de IA") {
        fun createRoute(path: String = "") = "ai_chat?path=${android.net.Uri.encode(path)}"
    }
    object PcTransfer : Screen("pc_transfer", "Transferência PC Web")
    object BatchRename : Screen("batch_rename?path={path}", "Renomear em Lote") {
        fun createRoute(path: String) = "batch_rename?path=${android.net.Uri.encode(path)}"
    }
    object HashTool : Screen("hash_tool?path={path}", "Checksum & Hashes") {
        fun createRoute(path: String) = "hash_tool?path=${android.net.Uri.encode(path)}"
    }
    object SearchResults : Screen("search_results?query={query}", "Resultados da Busca") {
        fun createRoute(query: String) = "search_results?query=${android.net.Uri.encode(query)}"
    }
}