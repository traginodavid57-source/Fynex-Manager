package org.fynex.manager.ui.screens.tools

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.fynex.manager.core.apk.ApkDetails
import org.fynex.manager.core.apk.ApkInspector
import org.fynex.manager.core.apk.BinaryXmlDecoder
import org.fynex.manager.core.fileops.ArchiveManager
import java.io.File

@Composable
fun ApkInspectorScreen(
    apkPath: String,
    onBack: () -> Unit,
    onOpenManifest: (String, String) -> Unit, // path, content
    onSignApk: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var details by remember { mutableStateOf<ApkDetails?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    var manifestContent by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(apkPath) {
        val file = File(apkPath)
        if (file.exists()) {
            details = ApkInspector.inspect(context, file)
            // Pre-decode AndroidManifest.xml
            val manifestBytes = ArchiveManager.readArchiveEntryBytes(file, "AndroidManifest.xml")
            if (manifestBytes != null) {
                manifestContent = BinaryXmlDecoder.decode(manifestBytes)
            }
        }
        isLoading = false
    }

    Column(modifier = modifier.fillMaxSize()) {
        // App Bar
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
                    text = "Inspeção de APK (MT Suite)",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        if (isLoading) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else if (details == null) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("Não foi possível inspecionar o arquivo APK selecionado.")
            }
        } else {
            val d = details!!
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Main App Info Card
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh)
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(54.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(Color(0xFF22C55E).copy(alpha = 0.2f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Android,
                                    contentDescription = null,
                                    tint = Color(0xFF22C55E),
                                    modifier = Modifier.size(32.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(16.dp))
                            Column {
                                Text(text = d.appName, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                                Text(text = d.packageName, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f))
                                Text(
                                    text = "v${d.versionName} (${d.versionCode}) • Min: ${d.minSdk} | Target: ${d.targetSdk}",
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }
                }

                // Action Buttons (MT Style)
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Button(
                            onClick = {
                                if (manifestContent != null) {
                                    onOpenManifest(apkPath, manifestContent!!)
                                }
                            },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Icon(imageVector = Icons.Default.Code, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Ver Manifest")
                        }

                        OutlinedButton(
                            onClick = { onSignApk(apkPath) },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Icon(imageVector = Icons.Default.Key, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Assinar APK")
                        }
                    }
                }

                // Signature & Certificate Info Card
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(imageVector = Icons.Default.Security, contentDescription = null, tint = MaterialTheme.colorScheme.secondary)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(text = "Assinatura & Certificado X.509", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                            }
                            Spacer(modifier = Modifier.height(10.dp))
                            Text("Assinatura v1 (JAR): ${if (d.hasV1Signature) "✅ Presente" else "❌ Ausente"}", fontSize = 13.sp)
                            Text("Assinatura v2/v3 (APK): ${if (d.hasV2Signature) "✅ Presente" else "ℹ️ Verificável"}", fontSize = 13.sp)
                            d.certIssuer?.let {
                                Text("Emissor: $it", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f))
                            }
                            d.certSha256?.let {
                                Text("SHA-256 Fingerprint:\n$it", fontSize = 11.sp, fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace, color = MaterialTheme.colorScheme.primary)
                            }
                        }
                    }
                }

                // Components & Architecture breakdown
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(text = "Estatísticas de Componentes", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                            Spacer(modifier = Modifier.height(8.dp))
                            Text("• Arquivos DEX: ${d.dexCount}", fontSize = 13.sp)
                            Text("• Atividades (Activities): ${d.activities.size}", fontSize = 13.sp)
                            Text("• Serviços (Services): ${d.services.size}", fontSize = 13.sp)
                            Text("• Receptores (Receivers): ${d.receivers.size}", fontSize = 13.sp)
                            Text("• Arquiteturas Nativas: ${if (d.nativeArchitectures.isEmpty()) "Nenhuma (Java/Kotlin puro)" else d.nativeArchitectures.joinToString(", ")}", fontSize = 13.sp)
                        }
                    }
                }

                // Permissions List
                item {
                    Text(text = "Permissões Requisitadas (${d.permissions.size})", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                }

                items(d.permissions.size) { index ->
                    val perm = d.permissions[index]
                    val isDangerous = perm.contains("CAMERA") || perm.contains("LOCATION") || perm.contains("STORAGE") || perm.contains("AUDIO") || perm.contains("PACKAGE")
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp),
                        color = if (isDangerous) Color(0xFFEF4444).copy(alpha = 0.1f) else MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.5f)
                    ) {
                        Text(
                            text = perm,
                            fontSize = 12.sp,
                            fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                            color = if (isDangerous) Color(0xFFEF4444) else MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.padding(10.dp)
                        )
                    }
                }
            }
        }
    }
}
