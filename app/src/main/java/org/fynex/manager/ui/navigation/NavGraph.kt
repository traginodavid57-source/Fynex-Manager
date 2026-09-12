package org.fynex.manager.ui.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Extension
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Home
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.navArgument
import org.fynex.manager.ui.screens.ai.AiChatScreen
import org.fynex.manager.ui.screens.ai.AiSettingsScreen
import org.fynex.manager.ui.screens.donate.DonateScreen
import org.fynex.manager.ui.screens.editor.CodeEditorScreen
import org.fynex.manager.ui.screens.editor.HexEditorScreen
import org.fynex.manager.ui.screens.explorer.ExplorerScreen
import org.fynex.manager.ui.screens.explorer.ExplorerViewModel
import org.fynex.manager.ui.screens.home.HomeScreen
import org.fynex.manager.ui.screens.plugins.PluginMarketScreen
import org.fynex.manager.ui.screens.settings.SettingsScreen
import org.fynex.manager.ui.screens.tools.ApkInspectorScreen
import org.fynex.manager.ui.screens.tools.ApkSignerScreen
import org.fynex.manager.ui.screens.tools.BatchRenameScreen
import org.fynex.manager.ui.screens.tools.DiffViewerScreen
import org.fynex.manager.ui.screens.tools.HashToolScreen
import org.fynex.manager.ui.screens.tools.PcTransferScreen
import org.fynex.manager.ui.screens.tools.ToolsScreen
import org.fynex.manager.ui.screens.vault.SafeVaultScreen

@Composable
fun MainNavGraph(
    navController: NavHostController,
    explorerViewModel: ExplorerViewModel = viewModel()
) {
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    val bottomNavItems = listOf(
        Pair(Screen.Home, Icons.Default.Home),
        Pair(Screen.Explorer, Icons.Default.Folder),
        Pair(Screen.Tools, Icons.Default.Build),
        Pair(Screen.Plugins, Icons.Default.Extension),
        Pair(Screen.Donate, Icons.Default.Favorite)
    )

    val showBottomBar = bottomNavItems.any { it.first.route == currentRoute }

    Scaffold(
        bottomBar = {
            if (showBottomBar) {
                NavigationBar(
                    containerColor = MaterialTheme.colorScheme.surface,
                    contentColor = MaterialTheme.colorScheme.onSurface
                ) {
                    bottomNavItems.forEach { (screen, icon) ->
                        val selected = currentRoute == screen.route
                        val isDonate = screen == Screen.Donate
                        NavigationBarItem(
                            icon = {
                                Icon(
                                    imageVector = icon,
                                    contentDescription = screen.title,
                                    tint = if (isDonate) Color(0xFFEF4444) else if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                                )
                            },
                            label = { Text(screen.title) },
                            selected = selected,
                            onClick = {
                                if (currentRoute != screen.route) {
                                    navController.navigate(screen.route) {
                                        popUpTo(Screen.Home.route) { saveState = true }
                                        launchSingleTop = true
                                        restoreState = true
                                    }
                                }
                            }
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Screen.Home.route,
            modifier = Modifier.padding(innerPadding)
        ) {
            composable(Screen.Home.route) {
                HomeScreen(
                    onNavigateToCategory = { cat ->
                        navController.navigate(Screen.Explorer.route)
                    },
                    onNavigateToExplorer = { navController.navigate(Screen.Explorer.route) },
                    onNavigateToPcTransfer = { navController.navigate(Screen.PcTransfer.route) },
                    onNavigateToVault = { navController.navigate(Screen.Vault.route) },
                    onNavigateToAiChat = { navController.navigate(Screen.AiChat.createRoute()) },
                    onNavigateToDonate = { navController.navigate(Screen.Donate.route) }
                )
            }

            composable(Screen.Explorer.route) {
                ExplorerScreen(
                    viewModel = explorerViewModel,
                    onInspectApk = { path -> navController.navigate(Screen.ApkInspector.createRoute(path)) },
                    onEditCode = { path -> navController.navigate(Screen.CodeEditor.createRoute(path)) },
                    onEditHex = { path -> navController.navigate(Screen.HexEditor.createRoute(path)) },
                    onCheckHash = { path -> navController.navigate(Screen.HashTool.createRoute(path)) },
                    onAiChat = { path -> navController.navigate(Screen.AiChat.createRoute(path)) }
                )
            }

            composable(Screen.Tools.route) {
                ToolsScreen(
                    onNavigateToApkSigner = { navController.navigate("apk_signer") },
                    onNavigateToBatchRename = {
                        val currentPath = explorerViewModel.leftPane.value.currentPath
                        navController.navigate(Screen.BatchRename.createRoute(currentPath))
                    },
                    onNavigateToHashTool = {
                        val currentPath = explorerViewModel.leftPane.value.items.firstOrNull { !it.isDirectory }?.path ?: ""
                        navController.navigate(Screen.HashTool.createRoute(currentPath))
                    },
                    onNavigateToDiffViewer = {
                        navController.navigate(Screen.DiffViewer.createRoute("", ""))
                    },
                    onNavigateToPcTransfer = { navController.navigate(Screen.PcTransfer.route) },
                    onNavigateToAiChat = { navController.navigate(Screen.AiChat.createRoute()) },
                    onNavigateToPlugins = { navController.navigate(Screen.Plugins.route) },
                    onNavigateToVault = { navController.navigate(Screen.Vault.route) }
                )
            }

            composable(Screen.Plugins.route) {
                PluginMarketScreen(onBack = { navController.popBackStack() })
            }

            composable(Screen.Vault.route) {
                SafeVaultScreen(onBack = { navController.popBackStack() })
            }

            composable(Screen.Donate.route) {
                DonateScreen(onBack = { navController.popBackStack() })
            }

            composable(Screen.Settings.route) {
                SettingsScreen(
                    onBack = { navController.popBackStack() },
                    onNavigateToDonate = { navController.navigate(Screen.Donate.route) }
                )
            }

            composable(
                route = Screen.ApkInspector.route,
                arguments = listOf(navArgument("path") { type = NavType.StringType })
            ) { backStack ->
                val path = backStack.arguments?.getString("path") ?: ""
                ApkInspectorScreen(
                    apkPath = path,
                    onBack = { navController.popBackStack() },
                    onOpenManifest = { apkP, content ->
                        navController.navigate(Screen.CodeEditor.createRoute("$apkP!/AndroidManifest.xml"))
                    },
                    onSignApk = { apkP ->
                        navController.navigate("apk_signer?path=${android.net.Uri.encode(apkP)}")
                    }
                )
            }

            composable(
                route = "apk_signer?path={path}",
                arguments = listOf(navArgument("path") { type = NavType.StringType; defaultValue = "" })
            ) { backStack ->
                val path = backStack.arguments?.getString("path") ?: ""
                ApkSignerScreen(
                    initialApkPath = path,
                    onBack = { navController.popBackStack() }
                )
            }

            composable(
                route = Screen.CodeEditor.route,
                arguments = listOf(navArgument("path") { type = NavType.StringType })
            ) { backStack ->
                val path = backStack.arguments?.getString("path") ?: ""
                CodeEditorScreen(
                    filePath = path,
                    onBack = { navController.popBackStack() }
                )
            }

            composable(
                route = Screen.HexEditor.route,
                arguments = listOf(navArgument("path") { type = NavType.StringType })
            ) { backStack ->
                val path = backStack.arguments?.getString("path") ?: ""
                HexEditorScreen(
                    filePath = path,
                    onBack = { navController.popBackStack() }
                )
            }

            composable(
                route = Screen.DiffViewer.route,
                arguments = listOf(
                    navArgument("path1") { type = NavType.StringType; defaultValue = "" },
                    navArgument("path2") { type = NavType.StringType; defaultValue = "" }
                )
            ) { backStack ->
                val p1 = backStack.arguments?.getString("path1") ?: ""
                val p2 = backStack.arguments?.getString("path2") ?: ""
                DiffViewerScreen(
                    filePath1 = p1,
                    filePath2 = p2,
                    onBack = { navController.popBackStack() }
                )
            }

            composable(
                route = Screen.AiChat.route,
                arguments = listOf(navArgument("path") { type = NavType.StringType; defaultValue = "" })
            ) { backStack ->
                val path = backStack.arguments?.getString("path")
                AiChatScreen(
                    contextFilePath = path,
                    onBack = { navController.popBackStack() },
                    onOpenSettings = { navController.navigate("ai_settings") }
                )
            }

            composable("ai_settings") {
                AiSettingsScreen(onBack = { navController.popBackStack() })
            }

            composable(Screen.PcTransfer.route) {
                PcTransferScreen(onBack = { navController.popBackStack() })
            }

            composable(
                route = Screen.BatchRename.route,
                arguments = listOf(navArgument("path") { type = NavType.StringType })
            ) { backStack ->
                val path = backStack.arguments?.getString("path") ?: ""
                BatchRenameScreen(
                    directoryPath = path,
                    onBack = { navController.popBackStack() }
                )
            }

            composable(
                route = Screen.HashTool.route,
                arguments = listOf(navArgument("path") { type = NavType.StringType })
            ) { backStack ->
                val path = backStack.arguments?.getString("path") ?: ""
                HashToolScreen(
                    filePath = path,
                    onBack = { navController.popBackStack() }
                )
            }
        }
    }
}
