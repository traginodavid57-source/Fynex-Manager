package org.fynex.manager.ui.screens.tools

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Android
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Calculate
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Compare
import androidx.compose.material.icons.filled.DriveFileRenameOutline
import androidx.compose.material.icons.filled.Extension
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Numbers
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

data class ToolItemData(
    val title: String,
    val description: String,
    val icon: ImageVector,
    val color: Color,
    val tag: String,
    val onClick: () -> Unit
)

@Composable
fun ToolsScreen(
    onNavigateToApkSigner: () -> Unit,
    onNavigateToBatchRename: () -> Unit,
    onNavigateToHashTool: () -> Unit,
    onNavigateToDiffViewer: () -> Unit,
    onNavigateToPcTransfer: () -> Unit,
    onNavigateToAiChat: () -> Unit,
    onNavigateToPlugins: () -> Unit,
    onNavigateToVault: () -> Unit,
    modifier: Modifier = Modifier
) {
    val mtTools = listOf(
        ToolItemData(
            title = "Assinador de APK",
            description = "Assine qualquer APK com chave AOSP TestKey ou Keystore personalizada (v1/v2).",
            icon = Icons.Default.Key,
            color = Color(0xFF10B981),
            tag = "MT Suite",
            onClick = onNavigateToApkSigner
        ),
        ToolItemData(
            title = "Renomeador em Lote",
            description = "Renomeie centenas de arquivos usando expressões regulares (Regex) e indexadores.",
            icon = Icons.Default.DriveFileRenameOutline,
            color = Color(0xFF3B82F6),
            tag = "MT Suite",
            onClick = onNavigateToBatchRename
        ),
        ToolItemData(
            title = "Checksums & Hashes",
            description = "Calcule e valide MD5, SHA-1, SHA-256 e CRC32 para verificar integridade.",
            icon = Icons.Default.Calculate,
            color = Color(0xFFF59E0B),
            tag = "Segurança",
            onClick = onNavigateToHashTool
        ),
        ToolItemData(
            title = "Comparador de Arquivos (Diff)",
            description = "Compare dois arquivos lado a lado com realce visual de alterações linha a linha.",
            icon = Icons.Default.Compare,
            color = Color(0xFF8B5CF6),
            tag = "Desenvolvedor",
            onClick = onNavigateToDiffViewer
        )
    )

    val fyloTools = listOf(
        ToolItemData(
            title = "Assistente de IA & Resumos",
            description = "Converse com seus arquivos, resuma documentos e explique códigos usando Gemini ou Ollama.",
            icon = Icons.Default.AutoAwesome,
            color = Color(0xFFA855F7),
            tag = "Fylo AI",
            onClick = onNavigateToAiChat
        ),
        ToolItemData(
            title = "Transferência PC Web",
            description = "Envie e baixe arquivos do computador via Wi-Fi pelo navegador sem cabos.",
            icon = Icons.Default.Wifi,
            color = Color(0xFF06B6D4),
            tag = "Rede",
            onClick = onNavigateToPcTransfer
        ),
        ToolItemData(
            title = "Cofre Criptografado",
            description = "Proteja fotos e documentos com criptografia militar AES-256 e biometria.",
            icon = Icons.Default.Lock,
            color = Color(0xFFEF4444),
            tag = "Privacidade",
            onClick = onNavigateToVault
        ),
        ToolItemData(
            title = "Gerenciador de Plugins",
            description = "Instale e gerencie módulos adicionais de utilidades, modelos de IA e temas.",
            icon = Icons.Default.Extension,
            color = Color(0xFF14B8A6),
            tag = "Extensões",
            onClick = onNavigateToPlugins
        )
    )

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        contentPadding = PaddingValues(top = 16.dp, bottom = 32.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Text(
                text = "Central de Ferramentas",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = "Recursos combinados do MT Manager e Fylo — 100% gratuitos",
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
            )
        }

        item {
            Text(
                text = "Ferramentas Avançadas (MT Style)",
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.primary
            )
        }

        items(mtTools.size) { index ->
            ToolItemCard(mtTools[index])
        }

        item {
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "IA, Rede & Segurança (Fylo Style)",
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.secondary
            )
        }

        items(fyloTools.size) { index ->
            ToolItemCard(fyloTools[index])
        }
    }
}

@Composable
fun ToolItemCard(data: ToolItemData) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { data.onClick() },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(46.dp)
                    .clip(CircleShape)
                    .background(data.color.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = data.icon,
                    contentDescription = null,
                    tint = data.color,
                    modifier = Modifier.size(24.dp)
                )
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(text = data.title, fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = data.color.copy(alpha = 0.15f)
                    ) {
                        Text(
                            text = data.tag,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = data.color,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = data.description,
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.65f),
                    lineHeight = 16.sp
                )
            }
        }
    }
}
