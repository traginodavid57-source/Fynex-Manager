package org.fynex.manager.ui.screens.explorer

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Archive
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.CompareArrows
import androidx.compose.material.icons.filled.CreateNewFolder
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DriveFileRenameOutline
import androidx.compose.material.icons.filled.Flip
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.SelectAll
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.filled.ViewColumn
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.fynex.manager.core.model.FileItem
import org.fynex.manager.ui.components.BreadcrumbBar
import org.fynex.manager.ui.components.CreateArchiveDialog
import org.fynex.manager.ui.components.CreateItemDialog
import org.fynex.manager.ui.components.DeleteConfirmDialog
import org.fynex.manager.ui.components.FileItemRow
import org.fynex.manager.ui.components.RenameDialog
import java.io.File

@Composable
fun ExplorerScreen(
    viewModel: ExplorerViewModel,
    onInspectApk: (String) -> Unit,
    onEditCode: (String) -> Unit,
    onEditHex: (String) -> Unit,
    onCheckHash: (String) -> Unit,
    onAiChat: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val leftState by viewModel.leftPane.collectAsState()
    val rightState by viewModel.rightPane.collectAsState()
    val activePane by viewModel.activePane.collectAsState()
    val isSplit by viewModel.isDualPaneSplit.collectAsState()
    val opMessage by viewModel.operationMessage.collectAsState()

    var showCreateDialog by remember { mutableStateOf(false) }
    var createIsFolder by remember { mutableStateOf(true) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    var showRenameDialog by remember { mutableStateOf(false) }
    var renameTargetFile by remember { mutableStateOf<File?>(null) }
    var showZipDialog by remember { mutableStateOf(false) }
    var showMoreMenu by remember { mutableStateOf(false) }

    LaunchedEffect(opMessage) {
        opMessage?.let {
            Toast.makeText(context, it, Toast.LENGTH_SHORT).show()
            viewModel.clearOperationMessage()
        }
    }

    val currentPaneState = if (activePane == ActivePane.LEFT) leftState else rightState
    val isSelectionMode = currentPaneState.selectedItems.isNotEmpty()

    Column(modifier = modifier.fillMaxSize()) {
        // Dual Pane Header / Tab Switcher (MT Manager Style)
        Surface(
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 2.dp
        ) {
            Column {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Pane Selector Tabs
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            color = if (activePane == ActivePane.LEFT) MaterialTheme.colorScheme.primary else Color.Transparent,
                            modifier = Modifier
                                .clickable { viewModel.setActivePane(ActivePane.LEFT) }
                                .padding(horizontal = 14.dp, vertical = 6.dp)
                        ) {
                            Text(
                                text = "Painel 1 (Esq)",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (activePane == ActivePane.LEFT) Color.White else MaterialTheme.colorScheme.onSurface
                            )
                        }
                        Surface(
                            color = if (activePane == ActivePane.RIGHT) MaterialTheme.colorScheme.primary else Color.Transparent,
                            modifier = Modifier
                                .clickable { viewModel.setActivePane(ActivePane.RIGHT) }
                                .padding(horizontal = 14.dp, vertical = 6.dp)
                        ) {
                            Text(
                                text = "Painel 2 (Dir)",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (activePane == ActivePane.RIGHT) Color.White else MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }

                    // Power Tool Action Buttons
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(onClick = { viewModel.toggleDualPaneSplit() }) {
                            Icon(
                                imageVector = Icons.Default.ViewColumn,
                                contentDescription = "Dividir tela",
                                tint = if (isSplit) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                            )
                        }
                        IconButton(onClick = { viewModel.swapPanes() }) {
                            Icon(imageVector = Icons.Default.Flip, contentDescription = "Inverter lados")
                        }
                        IconButton(onClick = { viewModel.syncOppositePanePath() }) {
                            Icon(imageVector = Icons.Default.Sync, contentDescription = "Sincronizar caminhos")
                        }
                        Box {
                            IconButton(onClick = { showMoreMenu = true }) {
                                Icon(imageVector = Icons.Default.MoreVert, contentDescription = "Mais opções")
                            }
                            DropdownMenu(
                                expanded = showMoreMenu,
                                onDismissRequest = { showMoreMenu = false }
                            ) {
                                DropdownMenuItem(
                                    text = { Text("Selecionar Todos") },
                                    onClick = { showMoreMenu = false; viewModel.selectAll(activePane) }
                                )
                                DropdownMenuItem(
                                    text = { Text("Inverter Seleção") },
                                    onClick = { showMoreMenu = false; viewModel.invertSelection(activePane) }
                                )
                                DropdownMenuItem(
                                    text = { Text("Nova Pasta") },
                                    onClick = { showMoreMenu = false; createIsFolder = true; showCreateDialog = true }
                                )
                                DropdownMenuItem(
                                    text = { Text("Novo Arquivo") },
                                    onClick = { showMoreMenu = false; createIsFolder = false; showCreateDialog = true }
                                )
                            }
                        }
                    }
                }

                // Dual Pane Quick Transfer Bar (MT Manager Signature Feature)
                if (isSelectionMode) {
                    Surface(
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 6.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "${currentPaneState.selectedItems.size} selecionados",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Row {
                                Surface(
                                    shape = RoundedCornerShape(6.dp),
                                    color = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier
                                        .clickable { viewModel.copySelectedToOppositePane() }
                                        .padding(horizontal = 10.dp, vertical = 5.dp)
                                ) {
                                    Text("Copiar p/ outro lado ➔", fontSize = 12.sp, color = Color.White)
                                }
                                Spacer(modifier = Modifier.width(8.dp))
                                Surface(
                                    shape = RoundedCornerShape(6.dp),
                                    color = MaterialTheme.colorScheme.secondary,
                                    modifier = Modifier
                                        .clickable { viewModel.moveSelectedToOppositePane() }
                                        .padding(horizontal = 10.dp, vertical = 5.dp)
                                ) {
                                    Text("Mover ➔", fontSize = 12.sp, color = Color.White)
                                }
                                Spacer(modifier = Modifier.width(8.dp))
                                IconButton(
                                    onClick = { showZipDialog = true },
                                    modifier = Modifier.size(28.dp)
                                ) {
                                    Icon(imageVector = Icons.Default.Archive, contentDescription = "ZIP")
                                }
                                Spacer(modifier = Modifier.width(4.dp))
                                IconButton(
                                    onClick = { showDeleteDialog = true },
                                    modifier = Modifier.size(28.dp)
                                ) {
                                    Icon(imageVector = Icons.Default.Delete, contentDescription = "Excluir", tint = MaterialTheme.colorScheme.error)
                                }
                            }
                        }
                    }
                }
            }
        }

        // Main Explorer Content: Split or Single
        if (isSplit) {
            Row(modifier = Modifier.weight(1f)) {
                Box(modifier = Modifier.weight(1f)) {
                    PaneContent(
                        pane = ActivePane.LEFT,
                        state = leftState,
                        isActive = activePane == ActivePane.LEFT,
                        onActivate = { viewModel.setActivePane(ActivePane.LEFT) },
                        onNavigateTo = { viewModel.loadDirectory(ActivePane.LEFT, it) },
                        onItemClick = { item -> handleItemClick(viewModel, ActivePane.LEFT, item, onInspectApk, onEditCode) },
                        onItemLongClick = { item -> viewModel.toggleItemSelection(ActivePane.LEFT, item.path) },
                        onInspectApk = onInspectApk,
                        onEditCode = onEditCode,
                        onEditHex = onEditHex,
                        onCheckHash = onCheckHash,
                        onAiChat = onAiChat,
                        onRename = { renameTargetFile = it.file; showRenameDialog = true },
                        onDelete = { showDeleteDialog = true },
                        onCopyToOther = { viewModel.copySelectedToOppositePane() },
                        onMoveToOther = { viewModel.moveSelectedToOppositePane() }
                    )
                }
                Box(
                    modifier = Modifier
                        .width(2.dp)
                        .fillMaxHeight()
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                )
                Box(modifier = Modifier.weight(1f)) {
                    PaneContent(
                        pane = ActivePane.RIGHT,
                        state = rightState,
                        isActive = activePane == ActivePane.RIGHT,
                        onActivate = { viewModel.setActivePane(ActivePane.RIGHT) },
                        onNavigateTo = { viewModel.loadDirectory(ActivePane.RIGHT, it) },
                        onItemClick = { item -> handleItemClick(viewModel, ActivePane.RIGHT, item, onInspectApk, onEditCode) },
                        onItemLongClick = { item -> viewModel.toggleItemSelection(ActivePane.RIGHT, item.path) },
                        onInspectApk = onInspectApk,
                        onEditCode = onEditCode,
                        onEditHex = onEditHex,
                        onCheckHash = onCheckHash,
                        onAiChat = onAiChat,
                        onRename = { renameTargetFile = it.file; showRenameDialog = true },
                        onDelete = { showDeleteDialog = true },
                        onCopyToOther = { viewModel.copySelectedToOppositePane() },
                        onMoveToOther = { viewModel.moveSelectedToOppositePane() }
                    )
                }
            }
        } else {
            PaneContent(
                pane = activePane,
                state = currentPaneState,
                isActive = true,
                onActivate = {},
                onNavigateTo = { viewModel.loadDirectory(activePane, it) },
                onItemClick = { item -> handleItemClick(viewModel, activePane, item, onInspectApk, onEditCode) },
                onItemLongClick = { item -> viewModel.toggleItemSelection(activePane, item.path) },
                onInspectApk = onInspectApk,
                onEditCode = onEditCode,
                onEditHex = onEditHex,
                onCheckHash = onCheckHash,
                onAiChat = onAiChat,
                onRename = { renameTargetFile = it.file; showRenameDialog = true },
                onDelete = { showDeleteDialog = true },
                onCopyToOther = { viewModel.copySelectedToOppositePane() },
                onMoveToOther = { viewModel.moveSelectedToOppositePane() },
                modifier = Modifier.weight(1f)
            )
        }
    }

    // Dialogs
    if (showCreateDialog) {
        CreateItemDialog(
            isFolder = createIsFolder,
            onDismiss = { showCreateDialog = false },
            onConfirm = {
                viewModel.createNewItem(activePane, it, createIsFolder)
                showCreateDialog = false
            }
        )
    }

    if (showDeleteDialog) {
        DeleteConfirmDialog(
            itemsCount = currentPaneState.selectedItems.size,
            itemName = null,
            onDismiss = { showDeleteDialog = false },
            onConfirm = {
                viewModel.deleteSelected(activePane)
                showDeleteDialog = false
            }
        )
    }

    if (showRenameDialog && renameTargetFile != null) {
        RenameDialog(
            initialName = renameTargetFile!!.name,
            onDismiss = { showRenameDialog = false },
            onConfirm = {
                viewModel.renameItem(activePane, renameTargetFile!!, it)
                showRenameDialog = false
            }
        )
    }

    if (showZipDialog) {
        CreateArchiveDialog(
            selectedCount = currentPaneState.selectedItems.size,
            defaultName = "fynex_archive_${System.currentTimeMillis() / 1000}.zip",
            onDismiss = { showZipDialog = false },
            onConfirm = { name, pwd ->
                viewModel.createZipFromSelected(activePane, name, pwd)
                showZipDialog = false
            }
        )
    }
}

@Composable
fun PaneContent(
    pane: ActivePane,
    state: PaneState,
    isActive: Boolean,
    onActivate: () -> Unit,
    onNavigateTo: (String) -> Unit,
    onItemClick: (FileItem) -> Unit,
    onItemLongClick: (FileItem) -> Unit,
    onInspectApk: (String) -> Unit,
    onEditCode: (String) -> Unit,
    onEditHex: (String) -> Unit,
    onCheckHash: (String) -> Unit,
    onAiChat: (String) -> Unit,
    onRename: (FileItem) -> Unit,
    onDelete: (FileItem) -> Unit,
    onCopyToOther: () -> Unit,
    onMoveToOther: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .clickable { onActivate() }
    ) {
        // Breadcrumb
        BreadcrumbBar(
            currentPath = state.currentPath,
            onNavigateTo = onNavigateTo,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
        )

        if (state.isLoading) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else if (state.items.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(
                    text = "Pasta vazia",
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                )
            }
        } else {
            LazyColumn(modifier = Modifier.fillMaxSize()) {
                items(state.items, key = { it.path }) { item ->
                    FileItemRow(
                        item = item,
                        isSelectionMode = state.selectedItems.isNotEmpty(),
                        onClick = { onItemClick(item) },
                        onLongClick = { onItemLongClick(item) },
                        onInspectApk = if (item.isApk) { { onInspectApk(item.path) } } else null,
                        onEditCode = { onEditCode(item.path) },
                        onEditHex = { onEditHex(item.path) },
                        onCheckHash = { onCheckHash(item.path) },
                        onAiSummarize = { onAiChat(item.path) },
                        onRename = { onRename(item) },
                        onDelete = { onDelete(item) },
                        onCopyToOppositePane = onCopyToOther,
                        onMoveToOppositePane = onMoveToOther
                    )
                }
            }
        }
    }
}

private fun handleItemClick(
    viewModel: ExplorerViewModel,
    pane: ActivePane,
    item: FileItem,
    onInspectApk: (String) -> Unit,
    onEditCode: (String) -> Unit
) {
    if (viewModel.getPaneSelectedCount(pane) > 0) {
        viewModel.toggleItemSelection(pane, item.path)
        return
    }

    if (item.isDirectory) {
        if (item.archiveInternalPath != null && item.file.parentFile != null) {
            // inside archive directory
            viewModel.openArchive(pane, item.file.parentFile!!, item.archiveInternalPath)
        } else {
            viewModel.loadDirectory(pane, item.path)
        }
    } else if (item.isArchive) {
        // Open ZIP/APK directly as a folder (MT Manager Style!)
        viewModel.openArchive(pane, item.file)
    } else if (item.isApk) {
        onInspectApk(item.path)
    } else {
        // Text / Code file
        val textExtensions = setOf("txt", "md", "json", "xml", "kt", "java", "py", "sh", "smali", "html", "css", "js", "properties", "gradle", "c", "cpp", "h")
        if (textExtensions.contains(item.extension)) {
            onEditCode(item.path)
        }
    }
}

private fun ExplorerViewModel.getPaneSelectedCount(pane: ActivePane): Int {
    return if (pane == ActivePane.LEFT) leftPane.value.selectedItems.size else rightPane.value.selectedItems.size
}
