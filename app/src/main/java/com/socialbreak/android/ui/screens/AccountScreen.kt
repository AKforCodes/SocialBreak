package com.socialbreak.android.ui.screens

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Upload
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.socialbreak.android.data.LocalBackupManager
import com.socialbreak.android.data.PreferencesManager
import com.socialbreak.android.ui.theme.*
import kotlinx.coroutines.launch

@Composable
fun AccountScreen(prefs: PreferencesManager, onBack: () -> Unit) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    var showPinDialog by remember { mutableStateOf(false) }
    var pinDialogMode by remember { mutableStateOf("export") } // "export" or "restore"
    var pinInput by remember { mutableStateOf("") }

    if (showPinDialog) {
        AlertDialog(
            onDismissRequest = { showPinDialog = false },
            title = {
                Text(
                    text = if (pinDialogMode == "export") "Create Encrypted Backup" else "Restore Encrypted Backup",
                    color = TextHighlight,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column {
                    Text(
                        text = "Enter a 4-digit PIN to secure your local AES-256 backup file:",
                        color = TextSecondary,
                        style = MaterialTheme.typography.bodySmall
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(
                        value = pinInput,
                        onValueChange = { if (it.length <= 8) pinInput = it },
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = { Text("Enter PIN", color = TextMuted) },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = TextHighlight,
                            unfocusedTextColor = TextHighlight,
                            focusedBorderColor = AccentBlue,
                            unfocusedBorderColor = BorderSubtle
                        )
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val pin = pinInput.trim()
                        if (pin.isNotEmpty()) {
                            showPinDialog = false
                            coroutineScope.launch {
                                if (pinDialogMode == "export") {
                                    val file = LocalBackupManager.createEncryptedBackup(context, prefs, pin)
                                    Toast.makeText(context, "Backup saved: ${file.name}", Toast.LENGTH_LONG).show()
                                } else {
                                    val success = LocalBackupManager.restoreEncryptedBackup(context, prefs, pin)
                                    val msg = if (success) "Backup restored successfully!" else "Restore failed: Invalid PIN or file"
                                    Toast.makeText(context, msg, Toast.LENGTH_LONG).show()
                                }
                                pinInput = ""
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = AccentBlue)
                ) {
                    Text("Proceed", color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { showPinDialog = false; pinInput = "" }) {
                    Text("Cancel", color = TextMuted)
                }
            },
            containerColor = BgSecondary
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BgPrimary)
            .padding(horizontal = 20.dp, vertical = 16.dp)
    ) {
        // Top Bar
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = TextHighlight)
            }
            Text(
                text = "Account",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = TextHighlight
            )
        }

        Spacer(modifier = Modifier.height(20.dp))

        Text(
            text = "ACCOUNT & DATA",
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.SemiBold,
            color = TextMuted
        )

        Spacer(modifier = Modifier.height(10.dp))

        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            color = BgSecondary,
            border = androidx.compose.foundation.BorderStroke(1.dp, BorderSubtle)
        ) {
            Column {
                // Email
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Account", style = MaterialTheme.typography.bodyMedium, color = TextHighlight)
                    Text("local.device@socialbreak.app", style = MaterialTheme.typography.bodyMedium, color = TextMuted)
                }

                HorizontalDivider(color = BorderSubtle, thickness = 0.5.dp)

                // Export Backup
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            pinDialogMode = "export"
                            showPinDialog = true
                        }
                        .padding(horizontal = 16.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.Upload, contentDescription = null, tint = AccentBlue, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text("Export Encrypted Backup", style = MaterialTheme.typography.bodyMedium, color = TextHighlight)
                        Text("Save AES-256 backup file to device", style = MaterialTheme.typography.bodySmall, color = TextMuted)
                    }
                }

                HorizontalDivider(color = BorderSubtle, thickness = 0.5.dp)

                // Restore Backup
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            pinDialogMode = "restore"
                            showPinDialog = true
                        }
                        .padding(horizontal = 16.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.Download, contentDescription = null, tint = AccentEmerald, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text("Restore Encrypted Backup", style = MaterialTheme.typography.bodyMedium, color = TextHighlight)
                        Text("Import preferences and blocked schedules", style = MaterialTheme.typography.bodySmall, color = TextMuted)
                    }
                }

                HorizontalDivider(color = BorderSubtle, thickness = 0.5.dp)

                // Log Out
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {}
                        .padding(horizontal = 16.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.AutoMirrored.Filled.ExitToApp, contentDescription = null, tint = AccentAmber, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(12.dp))
                    Text("Log Out", style = MaterialTheme.typography.bodyMedium, color = TextHighlight)
                }

                HorizontalDivider(color = BorderSubtle, thickness = 0.5.dp)

                // Delete Account
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {}
                        .padding(horizontal = 16.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.DeleteForever, contentDescription = null, tint = AccentCoral, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text("Delete Account", style = MaterialTheme.typography.bodyMedium, color = AccentCoral, fontWeight = FontWeight.SemiBold)
                        Text("Permanently remove all local data", style = MaterialTheme.typography.bodySmall, color = TextMuted)
                    }
                }
            }
        }
    }
}
