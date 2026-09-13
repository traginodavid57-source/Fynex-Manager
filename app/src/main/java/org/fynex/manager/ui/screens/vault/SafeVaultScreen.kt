package org.fynex.manager.ui.screens.vault

import android.widget.Toast
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import kotlinx.coroutines.launch
import org.fynex.manager.core.model.FileItem
import org.fynex.manager.core.vault.SafeVaultManager
import java.io.File

@Composable
fun SafeVaultScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val fragmentActivity = context as? FragmentActivity
    val biometricsAvailable = fragmentActivity != null && SafeVaultManager.canAuthenticate(context)
    var isUnlocked by remember { mutableStateOf(false) }
    var passcode by remember { mutableStateOf("") }
    var vaultFiles by remember { mutableStateOf<List<FileItem>>(emptyList()) }
    var isLoading by remember { mutableStateOf(false) }

    fun refreshFiles() {
        scope.launch {
            isLoading = true
            vaultFiles = SafeVaultManager.listVaultFiles(context)
            isLoading = false
        }
    }

    LaunchedEffect(isUnlocked) {
        if (isUnlocked) {
            refreshFiles()
        }
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
                    text = "Cofre Criptografado (AES-256)",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        if (!isUnlocked) {
            // Lock Screen
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(80.dp)
                        .clip(CircleShape)
                        .background(Color(0xFFEF4444).copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Lock,
                        contentDescription = null,
                        tint = Color(0xFFEF4444),
                        modifier = Modifier.size(40.dp)
                    )
                }

                Spacer(modifier = Modifier.height(20.dp))

                Text(
                    text = "Cofre Protegido",
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "Digite sua senha mestra para desbloquear os arquivos criptografados.",
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )

                Spacer(modifier = Modifier.height(24.dp))

                OutlinedTextField(
                    value = passcode,
                    onValueChange = { passcode = it },
                    label = { Text("Senha do Cofre") },
                    visualTransformation = PasswordVisualTransformation(),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(20.dp))

                Button(
                    onClick = {
                        if (passcode.length >= 4) {
                            isUnlocked = true
                        } else {
                            Toast.makeText(context, "A senha deve ter pelo menos 4 caracteres", Toast.LENGTH_SHORT).show()
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(imageVector = Icons.Default.LockOpen, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Desbloquear Cofre")
                }

                if (biometricsAvailable) {
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedButton(
                        onClick = {
                            try {
                                val cipher = SafeVaultManager.createBiometricDecryptCipher()
                                val executor = ContextCompat.getMainExecutor(context)
                                val prompt = BiometricPrompt(
                                    fragmentActivity!!,
                                    executor,
                                    object : BiometricPrompt.AuthenticationCallback() {
                                        override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                                            val stored = SafeVaultManager.retrievePasscodeFromCipher(context, result.cryptoObject?.cipher)
                                            if (!stored.isNullOrEmpty()) {
                                                passcode = stored
                                                isUnlocked = true
                                            } else {
                                                Toast.makeText(context, "Nenhuma senha salva. Mova um arquivo para o cofre antes.", Toast.LENGTH_LONG).show()
                                            }
                                        }

                                        override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                                            if (errorCode != BiometricPrompt.ERROR_NEGATIVE_BUTTON) {
                                                Toast.makeText(context, errString, Toast.LENGTH_SHORT).show()
                                            }
                                        }
                                    }
                                )
                                val promptInfo = BiometricPrompt.PromptInfo.Builder()
                                    .setTitle("Desbloquear Cofre")
                                    .setSubtitle("Confirme sua identidade para liberar a senha do cofre")
                                    .setNegativeButtonText("Usar senha")
                                    .setAllowedAuthenticators(
                                        BiometricManager.Authenticators.BIOMETRIC_STRONG or
                                            BiometricManager.Authenticators.BIOMETRIC_WEAK
                                    )
                                    .build()
                                prompt.authenticate(promptInfo, BiometricPrompt.CryptoObject(cipher))
                            } catch (e: Exception) {
                                Toast.makeText(context, "Biometria indisponível: ${e.message}", Toast.LENGTH_LONG).show()
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(imageVector = Icons.Default.Fingerprint, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Desbloquear com Biometria")
                    }
                }
            }
        } else {
            // Unlocked Vault Files
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
            ) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(imageVector = Icons.Default.Security, contentDescription = null, tint = Color(0xFF10B981))
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = "Cofre desbloqueado • Criptografia AES-GCM 256 bits ativa",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                if (isLoading) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                } else if (vaultFiles.isEmpty()) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text(
                            text = "Nenhum arquivo no cofre.\nPara adicionar arquivos, use a opção 'Mover para Cofre' no explorador.",
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                            fontSize = 13.sp
                        )
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(vaultFiles) { item ->
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                            ) {
                                Row(
                                    modifier = Modifier.padding(14.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(imageVector = Icons.Default.Lock, contentDescription = null, tint = Color(0xFF10B981))
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(text = item.name, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                                        Text(text = item.formattedSize, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
                                    }
                                    IconButton(
                                        onClick = {
                                            scope.launch {
                                                val dlDir = android.os.Environment.getExternalStoragePublicDirectory(android.os.Environment.DIRECTORY_DOWNLOADS)
                                                val res = SafeVaultManager.decryptAndRestore(context, item.file, dlDir, passcode)
                                                if (res.isSuccess) {
                                                    Toast.makeText(context, "Restaurado em Downloads!", Toast.LENGTH_SHORT).show()
                                                    refreshFiles()
                                                } else {
                                                    Toast.makeText(context, "Erro ao restaurar: ${res.exceptionOrNull()?.message}", Toast.LENGTH_LONG).show()
                                                }
                                            }
                                        }
                                    ) {
                                        Icon(imageVector = Icons.Default.Restore, contentDescription = "Restaurar")
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
