package org.fynex.manager.ui.screens.tools

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.io.File

data class DiffLine(
    val lineNumberA: Int?,
    val lineTextA: String?,
    val lineNumberB: Int?,
    val lineTextB: String?,
    val status: DiffStatus
)

enum class DiffStatus {
    SAME, ADDED, REMOVED, MODIFIED
}

@Composable
fun DiffViewerScreen(
    filePath1: String,
    filePath2: String,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    var diffLines by remember { mutableStateOf<List<DiffLine>>(emptyList()) }
    var fileAName by remember { mutableStateOf("") }
    var fileBName by remember { mutableStateOf("") }

    LaunchedEffect(filePath1, filePath2) {
        val f1 = File(filePath1)
        val f2 = File(filePath2)
        fileAName = f1.name
        fileBName = f2.name

        val lines1 = if (f1.exists()) f1.readLines() else emptyList()
        val lines2 = if (f2.exists()) f2.readLines() else emptyList()

        val maxLines = maxOf(lines1.size, lines2.size)
        val result = mutableListOf<DiffLine>()

        for (i in 0 until maxLines) {
            val textA = lines1.getOrNull(i)
            val textB = lines2.getOrNull(i)

            val status = when {
                textA == null -> DiffStatus.ADDED
                textB == null -> DiffStatus.REMOVED
                textA == textB -> DiffStatus.SAME
                else -> DiffStatus.MODIFIED
            }

            result.add(
                DiffLine(
                    lineNumberA = if (textA != null) i + 1 else null,
                    lineTextA = textA,
                    lineNumberB = if (textB != null) i + 1 else null,
                    lineTextB = textB,
                    status = status
                )
            )
        }
        diffLines = result
    }

    Column(modifier = modifier.fillMaxSize()) {
        Surface(color = MaterialTheme.colorScheme.surface, tonalElevation = 2.dp) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBack) {
                    Icon(imageVector = Icons.Default.ArrowBack, contentDescription = "Voltar")
                }
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Comparador Diff (MT Manager)",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        // Header showing file names
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.surfaceVariant)
                .padding(horizontal = 12.dp, vertical = 8.dp)
        ) {
            Text(
                text = "Arquivo 1: $fileAName",
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFFEF4444),
                modifier = Modifier.weight(1f)
            )
            Text(
                text = "Arquivo 2: $fileBName",
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF10B981),
                modifier = Modifier.weight(1f)
            )
        }

        LazyColumn(modifier = Modifier.fillMaxSize()) {
            itemsIndexed(diffLines) { _, item ->
                val bgColor = when (item.status) {
                    DiffStatus.SAME -> Color.Transparent
                    DiffStatus.ADDED -> Color(0xFF10B981).copy(alpha = 0.15f)
                    DiffStatus.REMOVED -> Color(0xFFEF4444).copy(alpha = 0.15f)
                    DiffStatus.MODIFIED -> Color(0xFFF59E0B).copy(alpha = 0.15f)
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(bgColor)
                        .padding(vertical = 2.dp, horizontal = 4.dp)
                ) {
                    // Side A
                    Row(modifier = Modifier.weight(1f)) {
                        Text(
                            text = item.lineNumberA?.toString() ?: "",
                            fontSize = 11.sp,
                            fontFamily = FontFamily.Monospace,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
                            modifier = Modifier.width(32.dp)
                        )
                        Text(
                            text = item.lineTextA ?: "",
                            fontSize = 11.sp,
                            fontFamily = FontFamily.Monospace,
                            color = if (item.status == DiffStatus.REMOVED || item.status == DiffStatus.MODIFIED) Color(0xFFEF4444) else MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.weight(1f)
                        )
                    }

                    Box(modifier = Modifier.width(1.dp).background(MaterialTheme.colorScheme.surfaceVariant))

                    // Side B
                    Row(modifier = Modifier.weight(1f).padding(start = 4.dp)) {
                        Text(
                            text = item.lineNumberB?.toString() ?: "",
                            fontSize = 11.sp,
                            fontFamily = FontFamily.Monospace,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
                            modifier = Modifier.width(32.dp)
                        )
                        Text(
                            text = item.lineTextB ?: "",
                            fontSize = 11.sp,
                            fontFamily = FontFamily.Monospace,
                            color = if (item.status == DiffStatus.ADDED || item.status == DiffStatus.MODIFIED) Color(0xFF10B981) else MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }
    }
}
