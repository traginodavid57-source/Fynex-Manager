package org.fynex.manager.ui.screens.explorer

import android.os.Environment
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.fynex.manager.core.fileops.ArchiveManager
import org.fynex.manager.core.fileops.FileOperations
import org.fynex.manager.core.model.FileItem
import org.fynex.manager.core.model.SortSpec
import java.io.File

enum class ActivePane { LEFT, RIGHT }

data class PaneState(
    val currentPath: String = Environment.getExternalStorageDirectory().absolutePath,
    val items: List<FileItem> = emptyList(),
    val isLoading: Boolean = false,
    val selectedItems: Set<String> = emptySet(),
    val sortSpec: SortSpec = SortSpec(),
    val searchQuery: String = "",
    val archiveFile: File? = null,
    val archiveInternalSubPath: String = ""
)

class ExplorerViewModel : ViewModel() {

    private val defaultRoot = Environment.getExternalStorageDirectory().absolutePath

    private val _leftPane = MutableStateFlow(PaneState(currentPath = defaultRoot))
    val leftPane: StateFlow<PaneState> = _leftPane.asStateFlow()

    private val _rightPane = MutableStateFlow(PaneState(currentPath = defaultRoot))
    val rightPane: StateFlow<PaneState> = _rightPane.asStateFlow()

    private val _activePane = MutableStateFlow(ActivePane.LEFT)
    val activePane: StateFlow<ActivePane> = _activePane.asStateFlow()

    private val _isDualPaneSplit = MutableStateFlow(false) // toggle between split screen and single-pane-swipe
    val isDualPaneSplit: StateFlow<Boolean> = _isDualPaneSplit.asStateFlow()

    private val _operationMessage = MutableStateFlow<String?>(null)
    val operationMessage: StateFlow<String?> = _operationMessage.asStateFlow()

    init {
        loadDirectory(ActivePane.LEFT, defaultRoot)
        loadDirectory(ActivePane.RIGHT, defaultRoot)
    }

    fun setActivePane(pane: ActivePane) {
        _activePane.value = pane
    }

    fun toggleDualPaneSplit() {
        _isDualPaneSplit.value = !_isDualPaneSplit.value
    }

    fun clearOperationMessage() {
        _operationMessage.value = null
    }

    fun loadDirectory(pane: ActivePane, path: String) {
        viewModelScope.launch {
            updatePane(pane) { it.copy(isLoading = true, currentPath = path, archiveFile = null, archiveInternalSubPath = "") }
            val rawItems = FileOperations.listDirectory(path, showHidden = true)
            val sorted = getPaneState(pane).sortSpec.sort(rawItems)
            updatePane(pane) { it.copy(items = sorted, isLoading = false, selectedItems = emptySet()) }
        }
    }

    fun openArchive(pane: ActivePane, archiveFile: File, internalSubPath: String = "") {
        viewModelScope.launch {
            updatePane(pane) { it.copy(isLoading = true, archiveFile = archiveFile, archiveInternalSubPath = internalSubPath) }
            val rawItems = ArchiveManager.listArchiveContents(archiveFile, internalSubPath)
            val sorted = getPaneState(pane).sortSpec.sort(rawItems)
            updatePane(pane) { it.copy(items = sorted, isLoading = false, selectedItems = emptySet()) }
        }
    }

    fun navigateUp(pane: ActivePane) {
        val state = getPaneState(pane)
        if (state.archiveFile != null) {
            // inside archive
            if (state.archiveInternalSubPath.isEmpty()) {
                // Exit archive back to normal directory
                loadDirectory(pane, state.archiveFile.parent ?: defaultRoot)
            } else {
                val clean = state.archiveInternalSubPath.trimEnd('/')
                val lastSlash = clean.lastIndexOf('/')
                val parentSub = if (lastSlash == -1) "" else clean.substring(0, lastSlash + 1)
                openArchive(pane, state.archiveFile, parentSub)
            }
            return
        }

        val current = File(state.currentPath)
        val parent = current.parentFile
        if (parent != null && parent.canRead()) {
            loadDirectory(pane, parent.absolutePath)
        }
    }

    fun toggleItemSelection(pane: ActivePane, path: String) {
        updatePane(pane) { state ->
            val set = state.selectedItems.toMutableSet()
            if (set.contains(path)) set.remove(path) else set.add(path)
            val updatedList = state.items.map { it.copy(isSelected = set.contains(it.path)) }
            state.copy(selectedItems = set, items = updatedList)
        }
    }

    fun selectAll(pane: ActivePane) {
        updatePane(pane) { state ->
            val allPaths = state.items.map { it.path }.toSet()
            val updatedList = state.items.map { it.copy(isSelected = true) }
            state.copy(selectedItems = allPaths, items = updatedList)
        }
    }

    fun clearSelection(pane: ActivePane) {
        updatePane(pane) { state ->
            val updatedList = state.items.map { it.copy(isSelected = false) }
            state.copy(selectedItems = emptySet(), items = updatedList)
        }
    }

    fun invertSelection(pane: ActivePane) {
        updatePane(pane) { state ->
            val newSet = mutableSetOf<String>()
            val updatedList = state.items.map {
                val selected = !state.selectedItems.contains(it.path)
                if (selected) newSet.add(it.path)
                it.copy(isSelected = selected)
            }
            state.copy(selectedItems = newSet, items = updatedList)
        }
    }

    // MT Manager Dual-Pane Power Actions:
    fun copySelectedToOppositePane() {
        val srcPane = _activePane.value
        val destPane = if (srcPane == ActivePane.LEFT) ActivePane.RIGHT else ActivePane.LEFT
        val srcState = getPaneState(srcPane)
        val destDir = File(getPaneState(destPane).currentPath)

        viewModelScope.launch {
            var count = 0
            for (path in srcState.selectedItems) {
                val file = File(path)
                if (file.exists()) {
                    FileOperations.copyFileOrDirectory(file, destDir)
                    count++
                }
            }
            _operationMessage.value = "$count itens copiados com sucesso para o outro painel!"
            refreshPane(destPane)
            clearSelection(srcPane)
        }
    }

    fun moveSelectedToOppositePane() {
        val srcPane = _activePane.value
        val destPane = if (srcPane == ActivePane.LEFT) ActivePane.RIGHT else ActivePane.LEFT
        val srcState = getPaneState(srcPane)
        val destDir = File(getPaneState(destPane).currentPath)

        viewModelScope.launch {
            var count = 0
            for (path in srcState.selectedItems) {
                val file = File(path)
                if (file.exists()) {
                    FileOperations.moveFileOrDirectory(file, destDir)
                    count++
                }
            }
            _operationMessage.value = "$count itens movidos para o outro painel!"
            refreshPane(srcPane)
            refreshPane(destPane)
        }
    }

    fun syncOppositePanePath() {
        val srcPane = _activePane.value
        val destPane = if (srcPane == ActivePane.LEFT) ActivePane.RIGHT else ActivePane.LEFT
        val currentPath = getPaneState(srcPane).currentPath
        loadDirectory(destPane, currentPath)
        _operationMessage.value = "Caminho sincronizado entre painéis"
    }

    fun swapPanes() {
        val left = _leftPane.value
        val right = _rightPane.value
        _leftPane.value = right
        _rightPane.value = left
        _operationMessage.value = "Painéis invertidos"
    }

    fun deleteSelected(pane: ActivePane) {
        val state = getPaneState(pane)
        viewModelScope.launch {
            for (path in state.selectedItems) {
                FileOperations.deleteRecursively(File(path))
            }
            _operationMessage.value = "${state.selectedItems.size} itens excluídos"
            refreshPane(pane)
        }
    }

    fun createNewItem(pane: ActivePane, name: String, isFolder: Boolean) {
        val state = getPaneState(pane)
        viewModelScope.launch {
            val parent = File(state.currentPath)
            val success = if (isFolder) {
                FileOperations.createDirectory(parent, name)
            } else {
                FileOperations.createEmptyFile(parent, name)
            }
            if (success) {
                _operationMessage.value = "${if (isFolder) "Pasta" else "Arquivo"} criado com sucesso"
                refreshPane(pane)
            }
        }
    }

    fun renameItem(pane: ActivePane, oldFile: File, newName: String) {
        viewModelScope.launch {
            if (FileOperations.rename(oldFile, newName)) {
                _operationMessage.value = "Renomeado com sucesso"
                refreshPane(pane)
            }
        }
    }

    fun createZipFromSelected(pane: ActivePane, zipName: String, password: String?) {
        val state = getPaneState(pane)
        viewModelScope.launch {
            val destFile = File(state.currentPath, zipName)
            val files = state.selectedItems.map { File(it) }.filter { it.exists() }
            val success = ArchiveManager.createArchive(destFile, files, password)
            if (success) {
                _operationMessage.value = "Arquivo $zipName criado com sucesso!"
                refreshPane(pane)
                clearSelection(pane)
            }
        }
    }

    fun refreshPane(pane: ActivePane) {
        val state = getPaneState(pane)
        if (state.archiveFile != null) {
            openArchive(pane, state.archiveFile, state.archiveInternalSubPath)
        } else {
            loadDirectory(pane, state.currentPath)
        }
    }

    private fun getPaneState(pane: ActivePane): PaneState = if (pane == ActivePane.LEFT) _leftPane.value else _rightPane.value

    private fun updatePane(pane: ActivePane, transform: (PaneState) -> PaneState) {
        if (pane == ActivePane.LEFT) {
            _leftPane.value = transform(_leftPane.value)
        } else {
            _rightPane.value = transform(_rightPane.value)
        }
    }
}
