package com.socialbreak.android.ui.screens

import android.webkit.CookieManager
import android.webkit.WebStorage
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.ExitToApp
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.PersonRemove
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.socialbreak.android.data.LocalBackupManager
import com.socialbreak.android.data.PreferencesManager
import com.socialbreak.android.data.SocialBreakPreferences
import com.socialbreak.android.ui.theme.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun AccountScreen(
    prefs: SocialBreakPreferences,
    prefsManager: PreferencesManager,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val palette = SocialTheme.colors

    var showDataInfoDialog by remember { mutableStateOf(false) }
    var showClearAllCookiesDialog by remember { mutableStateOf(false) }
    var showLogoutDialog by remember { mutableStateOf(false) }
    var showDeleteAccountDialog by remember { mutableStateOf(false) }
    var showEditEmailDialog by remember { mutableStateOf(false) }
    var showBackupPanel by remember { mutableStateOf(false) }
    var backupPassphrase by remember { mutableStateOf("") }
    var backupPayload by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(palette.background)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 14.dp)
    ) {
        // Header Row: Back Chevron + "Account" (View 23)
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(
                imageVector = Icons.Filled.ArrowBack,
                contentDescription = "Back",
                tint = palette.textPrimary,
                modifier = Modifier
                    .size(22.dp)
                    .clickable { onBack() }
            )
            Text(
                text = "Account",
                color = palette.textPrimary,
                fontSize = 19.sp,
                fontWeight = FontWeight.Bold
            )
        }

        Spacer(modifier = Modifier.height(18.dp))

        // Main 4-Row Account Card matching View 23 1:1
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(18.dp))
                .background(palette.cardBackground)
                .padding(horizontal = 16.dp, vertical = 6.dp)
        ) {
            // Row 1: Account | akinphil2005@gmail.com
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { showEditEmailDialog = true }
                    .padding(vertical = 14.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Account",
                    color = palette.textSecondary,
                    fontSize = 14.sp
                )
                Text(
                    text = prefs.accountEmail,
                    color = palette.textPrimary,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }

            Divider(color = Color(0xFF1F202D), thickness = 0.75.dp)

            // Row 2: Log out of all accounts (blue trash icon + info icon)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { showClearAllCookiesDialog = true }
                    .padding(vertical = 14.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Icon(
                        imageVector = Icons.Outlined.DeleteOutline,
                        contentDescription = null,
                        tint = Color(0xFF60A5FA),
                        modifier = Modifier.size(19.dp)
                    )
                    Text(
                        text = "Log out of all accounts",
                        color = palette.textPrimary,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
                Icon(
                    imageVector = Icons.Outlined.Info,
                    contentDescription = "Info",
                    tint = palette.textSecondary,
                    modifier = Modifier
                        .size(18.dp)
                        .clickable { showDataInfoDialog = true }
                )
            }

            Divider(color = Color(0xFF1F202D), thickness = 0.75.dp)

            // Row 3: Log Out (amber exit icon)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { showLogoutDialog = true }
                    .padding(vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Icon(
                    imageVector = Icons.Outlined.ExitToApp,
                    contentDescription = null,
                    tint = Color(0xFFFBBF24),
                    modifier = Modifier.size(19.dp)
                )
                Text(
                    text = "Log Out",
                    color = palette.textPrimary,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium
                )
            }

            Divider(color = Color(0xFF1F202D), thickness = 0.75.dp)

            // Row 4: Delete Account — Permanently remove all data (red icon)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { showDeleteAccountDialog = true }
                    .padding(vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Icon(
                    imageVector = Icons.Outlined.PersonRemove,
                    contentDescription = null,
                    tint = Color(0xFFF87171),
                    modifier = Modifier.size(19.dp)
                )
                Column {
                    Text(
                        text = "Delete Account",
                        color = Color(0xFFF87171),
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = "Permanently remove all data",
                        color = palette.textSecondary,
                        fontSize = 11.sp
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(18.dp))

        // Subtle On-Device Encrypted Backup Toggle
        Text(
            text = if (showBackupPanel) "Hide local encrypted backup" else "Local encrypted backup (Export / Import)",
            color = palette.textSecondary,
            fontSize = 12.sp,
            modifier = Modifier
                .align(Alignment.CenterHorizontally)
                .clickable { showBackupPanel = !showBackupPanel }
                .padding(vertical = 6.dp)
        )

        if (showBackupPanel) {
            Spacer(modifier = Modifier.height(8.dp))
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(18.dp))
                    .background(palette.cardBackground)
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = "AES-256-GCM Local Backup",
                    color = palette.textPrimary,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold
                )
                OutlinedTextField(
                    value = backupPassphrase,
                    onValueChange = { backupPassphrase = it },
                    label = { Text("Passphrase") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = backupPayload,
                    onValueChange = { backupPayload = it },
                    label = { Text("Encrypted Payload") },
                    maxLines = 4,
                    modifier = Modifier.fillMaxWidth()
                )
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Button(
                        onClick = {
                            if (backupPassphrase.isNotBlank()) {
                                backupPayload = LocalBackupManager.exportEncryptedBackup(prefs, backupPassphrase)
                                Toast.makeText(context, "Encrypted backup generated", Toast.LENGTH_SHORT).show()
                            }
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Export")
                    }
                    OutlinedButton(
                        onClick = {
                            if (backupPassphrase.isNotBlank() && backupPayload.isNotBlank()) {
                                scope.launch {
                                    val ok = LocalBackupManager.importEncryptedBackup(
                                        backupPayload.trim(),
                                        backupPassphrase,
                                        prefsManager
                                    )
                                    Toast.makeText(
                                        context,
                                        if (ok) "Backup restored!" else "Wrong passphrase or payload",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                }
                            }
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Import")
                    }
                }
            }
        }
    }

    // "About Your Data" Info Dialog (from strings.xml line 1440)
    if (showDataInfoDialog) {
        Dialog(onDismissRequest = { showDataInfoDialog = false }) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(24.dp))
                    .background(Color(0xFF171822))
                    .padding(24.dp)
            ) {
                Text("About Your Data", color = Color.White, fontSize = 19.sp, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = "Your data stays on this device and is never shared off it. Clearing it logs you out of every platform inside SocialLite.",
                    color = Color(0xFFA0A3B5),
                    fontSize = 14.sp,
                    lineHeight = 20.sp
                )
                Spacer(modifier = Modifier.height(20.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(46.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0xFF6366F1))
                        .clickable { showDataInfoDialog = false },
                    contentAlignment = Alignment.Center
                ) {
                    Text("OK", color = Color.White, fontWeight = FontWeight.Bold)
                }
            }
        }
    }

    // "Clear All Data?" Dialog (from strings.xml line 1438)
    if (showClearAllCookiesDialog) {
        Dialog(onDismissRequest = { showClearAllCookiesDialog = false }) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(24.dp))
                    .background(Color(0xFF171822))
                    .padding(24.dp)
            ) {
                Text("Clear All Data?", color = Color.White, fontSize = 19.sp, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "This will log you out of all platforms and clear your browsing data.",
                    color = Color(0xFFA0A3B5),
                    fontSize = 14.sp,
                    lineHeight = 20.sp
                )
                Spacer(modifier = Modifier.height(20.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedButton(
                        onClick = { showClearAllCookiesDialog = false },
                        modifier = Modifier.weight(1f).height(46.dp),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Cancel", color = Color.White)
                    }
                    Button(
                        onClick = {
                            try {
                                CookieManager.getInstance().removeAllCookies(null)
                                CookieManager.getInstance().flush()
                                WebStorage.getInstance().deleteAllData()
                            } catch (_: Exception) {}
                            showClearAllCookiesDialog = false
                            Toast.makeText(context, "Logged out of all platforms", Toast.LENGTH_SHORT).show()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF60A5FA)),
                        modifier = Modifier.weight(1f).height(46.dp),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Clear", color = Color.Black, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }

    // "Log Out?" Dialog (from strings.xml line 1478)
    if (showLogoutDialog) {
        Dialog(onDismissRequest = { showLogoutDialog = false }) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(24.dp))
                    .background(Color(0xFF171822))
                    .padding(24.dp)
            ) {
                Text("Log Out?", color = Color.White, fontSize = 19.sp, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "This clears your active WebView sessions while keeping your blocking rules and schedules intact on this phone.",
                    color = Color(0xFFA0A3B5),
                    fontSize = 14.sp,
                    lineHeight = 20.sp
                )
                Spacer(modifier = Modifier.height(20.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedButton(
                        onClick = { showLogoutDialog = false },
                        modifier = Modifier.weight(1f).height(46.dp),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Cancel", color = Color.White)
                    }
                    Button(
                        onClick = {
                            try {
                                CookieManager.getInstance().removeAllCookies(null)
                                CookieManager.getInstance().flush()
                            } catch (_: Exception) {}
                            showLogoutDialog = false
                            Toast.makeText(context, "Logged out", Toast.LENGTH_SHORT).show()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFBBF24)),
                        modifier = Modifier.weight(1f).height(46.dp),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Log Out", color = Color.Black, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }

    // "Delete Account?" Dialog with 5-second safety countdown (from strings.xml lines 1449-1453)
    if (showDeleteAccountDialog) {
        var deleteCountdown by remember { mutableIntStateOf(5) }
        LaunchedEffect(Unit) {
            while (deleteCountdown > 0) {
                delay(1000L)
                deleteCountdown--
            }
        }

        Dialog(onDismissRequest = { showDeleteAccountDialog = false }) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(24.dp))
                    .background(Color(0xFF171822))
                    .padding(24.dp)
            ) {
                Text("Delete Account?", color = Color.White, fontSize = 19.sp, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "This will permanently delete your account and all associated data on this device. This action cannot be undone.",
                    color = Color(0xFFA0A3B5),
                    fontSize = 14.sp,
                    lineHeight = 20.sp
                )
                Spacer(modifier = Modifier.height(20.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedButton(
                        onClick = { showDeleteAccountDialog = false },
                        modifier = Modifier.weight(1f).height(46.dp),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Cancel", color = Color.White)
                    }
                    Button(
                        enabled = deleteCountdown == 0,
                        onClick = {
                            scope.launch {
                                prefsManager.clearAllLocalData()
                                try {
                                    CookieManager.getInstance().removeAllCookies(null)
                                    CookieManager.getInstance().flush()
                                    WebStorage.getInstance().deleteAllData()
                                } catch (_: Exception) {}
                                showDeleteAccountDialog = false
                                Toast.makeText(context, "All local data deleted", Toast.LENGTH_SHORT).show()
                                onBack()
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = CrimsonDanger),
                        modifier = Modifier.weight(1f).height(46.dp),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(
                            text = if (deleteCountdown > 0) "Delete ($deleteCountdown)" else "Delete Everything",
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp
                        )
                    }
                }
            }
        }
    }

    // Edit Local Account Label Dialog
    if (showEditEmailDialog) {
        var tempEmail by remember(prefs.accountEmail) { mutableStateOf(prefs.accountEmail) }
        Dialog(onDismissRequest = { showEditEmailDialog = false }) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(24.dp))
                    .background(Color(0xFF171822))
                    .padding(24.dp)
            ) {
                Text("Account Identifier", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(12.dp))
                OutlinedTextField(
                    value = tempEmail,
                    onValueChange = { tempEmail = it },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White
                    )
                )
                Spacer(modifier = Modifier.height(16.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedButton(
                        onClick = { showEditEmailDialog = false },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Cancel", color = Color.White)
                    }
                    Button(
                        onClick = {
                            if (tempEmail.isNotBlank()) {
                                scope.launch { prefsManager.setAccountEmail(tempEmail.trim()) }
                            }
                            showEditEmailDialog = false
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Save")
                    }
                }
            }
        }
    }
}
