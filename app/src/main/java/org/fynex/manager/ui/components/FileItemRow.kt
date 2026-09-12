package org.fynex.manager.ui.components

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Android
import androidx.compose.material.icons.filled.Archive
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.fynex.manager.core.model.FileCategory
import org.fynex.manager.core.model.FileItem

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun FileItemRow(
    item: FileItem,
    isSelectionMode: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    onInspectApk: (() -> Unit)? = null,
    onEditCode: (() -> Unit)? = null,
    onEditHex: (() -> Unit)? = null,
    onRename: (() -> Unit)? = null,
    onDelete: (() -> Unit)? = null,
    onCheckHash: (() -> Unit)? = null,
    onCopyToOppositePane: (() -> Unit)? = null,
    onMoveToOppositePane: (() -> Unit)? = null,
    onAiSummarize: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    var showMenu by remember { mutableStateOf(false) }

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick
            ),
        color = if (item.isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.15f) else Color.Transparent,
        shape = RoundedCornerShape(8.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (isSelectionMode) {
                Checkbox(
                    checked = item.isSelected,
                    onCheckedChange = { onClick() },
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
            }

            // File Type Icon with custom color
            val (iconVector, iconTint) = getFileIconAndColor(item)
            Icon(
                imageVector = iconVector,
                contentDescription = null,
                tint = iconTint,
                modifier = Modifier.size(36.dp)
            )

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = item.name,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = item.formattedSize,
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                    if (item.formattedDate.isNotEmpty()) {
                        Text(
                            text = " • ${item.formattedDate}",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        )
                    }
                    if (item.permissions.isNotEmpty()) {
                        Text(
                            text = " • ${item.permissions}",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.8f)
                        )
                    }
                }
            }

            Box {
                IconButton(onClick = { showMenu = true }) {
                    Icon(
                        imageVector = Icons.Default.MoreVert,
                        contentDescription = "Menu",
                        tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                    )
                }

                DropdownMenu(
                    expanded = showMenu,
                    onDismissRequest = { showMenu = false }
                ) {
                    if (item.isApk) {
                        DropdownMenuItem(
                            text = { Text("⚡ Inspecionar APK (MT Suite)") },
                            onClick = { showMenu = false; onInspectApk?.invoke() }
                        )
                    }
                    if (!item.isDirectory) {
                        DropdownMenuItem(
                            text = { Text("📝 Editar Código / Texto") },
                            onClick = { showMenu = false; onEditCode?.invoke() }
                        )
                        DropdownMenuItem(
                            text = { Text("🔢 Visualizador Hexadecimal") },
                            onClick = { showMenu = false; onEditHex?.invoke() }
                        )
                        DropdownMenuItem(
                            text = { Text("🤖 IA: Resumir / Explicar") },
                            onClick = { showMenu = false; onAiSummarize?.invoke() }
                        )
                        DropdownMenuItem(
                            text = { Text("#️⃣ Calcular Hash / Checksum") },
                            onClick = { showMenu = false; onCheckHash?.invoke() }
                        )
                    }
                    if (onCopyToOppositePane != null) {
                        DropdownMenuItem(
                            text = { Text("➡️ Copiar para o outro painel") },
                            onClick = { showMenu = false; onCopyToOppositePane.invoke() }
                        )
                    }
                    if (onMoveToOppositePane != null) {
                        DropdownMenuItem(
                            text = { Text("➡️ Mover para o outro painel") },
                            onClick = { showMenu = false; onMoveToOppositePane.invoke() }
                        )
                    }
                    DropdownMenuItem(
                        text = { Text("✏️ Renomear") },
                        onClick = { showMenu = false; onRename?.invoke() }
                    )
                    DropdownMenuItem(
                        text = { Text("🗑️ Excluir", color = MaterialTheme.colorScheme.error) },
                        onClick = { showMenu = false; onDelete?.invoke() }
                    )
                }
            }
        }
    }
}

private fun getFileIconAndColor(item: FileItem): Pair<ImageVector, Color> {
    if (item.isDirectory) return Pair(Icons.Default.Folder, Color(0xFFFFB300))
    if (item.isApk) return Pair(Icons.Default.Android, Color(0xFF4CAF50))
    if (item.isArchive) return Pair(Icons.Default.Archive, Color(0xFFFF7043))

    val cat = FileCategory.fromExtension(item.extension)
    return when (cat) {
        FileCategory.IMAGES -> Pair(Icons.Default.Image, Color(0xFF42A5F5))
        FileCategory.VIDEOS -> Pair(Icons.Default.Movie, Color(0xFFAB47BC))
        FileCategory.AUDIO -> Pair(Icons.Default.MusicNote, Color(0xFFEC407A))
        FileCategory.DOCUMENTS -> {
            if (listOf("kt", "java", "py", "xml", "json", "smali", "js", "html", "c", "cpp", "sh").contains(item.extension)) {
                Pair(Icons.Default.Code, Color(0xFF26A69A))
            } else {
                Pair(Icons.Default.Description, Color(0xFF7E57C2))
            }
        }
        else -> Pair(Icons.Default.Description, Color(0xFF78909C))
    }
}
