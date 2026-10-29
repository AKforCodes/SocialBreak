package com.socialbreak.android.ui.screens

import android.app.Activity
import android.content.Intent
import android.net.VpnService
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.outlined.Language
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
import com.socialbreak.android.data.DomainFilterRepository
import com.socialbreak.android.data.PreferencesManager
import com.socialbreak.android.data.SocialBreakPreferences
import com.socialbreak.android.services.WebBlockerVpnService
import com.socialbreak.android.ui.theme.*
import kotlinx.coroutines.launch

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun WebBlockingScreen(
    prefs: SocialBreakPreferences,
    prefsManager: PreferencesManager,
    onBack: () -> Unit
) {
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val palette = SocialTheme.colors

    var expandedCategory by remember { mutableStateOf<String?>("social") }
    var isEditMode by remember { mutableStateOf(false) }
    var newCustomDomain by remember { mutableStateOf("") }
    var domainError by remember { mutableStateOf<String?>(null) }

    val vpnPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            try {
                context.startService(Intent(context, WebBlockerVpnService::class.java))
            } catch (_: Exception) {}
        }
    }

    fun syncVpnServiceState(anyActive: Boolean) {
        if (anyActive) {
            val prepareIntent = VpnService.prepare(context)
            if (prepareIntent != null) {
                vpnPermissionLauncher.launch(prepareIntent)
            } else {
                try {
                    context.startService(Intent(context, WebBlockerVpnService::class.java))
                } catch (_: Exception) {}
            }
        } else {
            try {
                context.startService(
                    Intent(context, WebBlockerVpnService::class.java).apply {
                        action = WebBlockerVpnService.ACTION_STOP_VPN
                    }
                )
            } catch (_: Exception) {}
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(palette.background)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 14.dp)
    ) {
        // Header Row: Back Arrow + "Web Blocking" | "Edit" / "Done"
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
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
                    text = "Web Blocking",
                    color = palette.textPrimary,
                    fontSize = 19.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            Text(
                text = if (isEditMode) "Done" else "Edit",
                color = Color(0xFF818CF8),
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.clickable { isEditMode = !isEditMode }
            )
        }

        // Custom Domains Section ("Your domains") shown when Edit is tapped or custom domains exist
        if (isEditMode || prefs.customBlockedDomains.isNotEmpty()) {
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "Your domains",
                color = palette.textSecondary,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(modifier = Modifier.height(8.dp))

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(18.dp))
                    .background(palette.cardBackground)
                    .padding(16.dp)
            ) {
                if (isEditMode) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = newCustomDomain,
                            onValueChange = {
                                newCustomDomain = it
                                domainError = null
                            },
                            placeholder = {
                                Text(
                                    text = "Add a domain (e.g. example.com)",
                                    color = Color(0xFF6E7185),
                                    fontSize = 13.sp
                                )
                            },
                            singleLine = true,
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = palette.textPrimary,
                                unfocusedTextColor = palette.textPrimary,
                                focusedBorderColor = Color(0xFF6366F1),
                                unfocusedBorderColor = palette.border
                            )
                        )
                        Box(
                            modifier = Modifier
                                .height(48.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(Color(0xFF6366F1))
                                .clickable {
                                    val clean = newCustomDomain.trim().lowercase()
                                        .removePrefix("https://")
                                        .removePrefix("http://")
                                        .removePrefix("www.")
                                        .substringBefore("/")
                                    if (clean.contains(".") && clean.length >= 4) {
                                        scope.launch {
                                            prefsManager.addCustomBlockedDomain(clean)
                                            syncVpnServiceState(true)
                                        }
                                        newCustomDomain = ""
                                    } else {
                                        domainError = "That doesn't look like a valid domain."
                                    }
                                }
                                .padding(horizontal = 16.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("Add", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        }
                    }
                    if (domainError != null) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(domainError!!, color = CrimsonDanger, fontSize = 12.sp)
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                }

                if (prefs.customBlockedDomains.isEmpty()) {
                    Text(
                        text = "No custom domains yet. Add one above to block it everywhere.",
                        color = palette.textSecondary,
                        fontSize = 12.sp
                    )
                } else {
                    FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        prefs.customBlockedDomains.sorted().forEach { domain ->
                            Row(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(Color(0xFF1D1E2C))
                                    .border(0.75.dp, Color(0xFF2D2E42), RoundedCornerShape(10.dp))
                                    .clickable(enabled = isEditMode) {
                                        scope.launch { prefsManager.removeCustomBlockedDomain(domain) }
                                    }
                                    .padding(horizontal = 10.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.Language,
                                    contentDescription = null,
                                    tint = Color(0xFF8E91A6),
                                    modifier = Modifier.size(13.dp)
                                )
                                Text(
                                    text = domain,
                                    color = Color(0xFFD1D5DB),
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Medium
                                )
                                if (isEditMode) {
                                    Icon(
                                        imageVector = Icons.Filled.Close,
                                        contentDescription = "Remove",
                                        tint = CrimsonDanger,
                                        modifier = Modifier.size(13.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(18.dp))

        Text(
            text = "Presets",
            color = palette.textSecondary,
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium
        )

        Spacer(modifier = Modifier.height(10.dp))

        // 1. Social apps — 16 sites (Views 15, 16, 17)
        WebPresetCategoryCard(
            title = "Social apps",
            siteCount = DomainFilterRepository.SOCIAL_SITES.size,
            description = "Blocks Instagram, TikTok, Facebook and friends in Chrome and other apps. Your SocialLite tabs keep working.",
            checked = prefs.webBlockSocialApps,
            expanded = expandedCategory == "social",
            domains = DomainFilterRepository.SOCIAL_SITES,
            whitelistedDomains = prefs.webWhitelistedDomains,
            onToggleExpand = {
                expandedCategory = if (expandedCategory == "social") null else "social"
            },
            onCheckedChange = { enabled ->
                scope.launch {
                    prefsManager.setBooleanPreference(PreferencesManager.WEB_BLOCK_SOCIAL, enabled)
                    syncVpnServiceState(
                        enabled || prefs.webBlockAdultContent || prefs.webBlockOnlineCasinos ||
                            prefs.webBlockCasualGaming || prefs.webBlockSportsBetting ||
                            prefs.customBlockedDomains.isNotEmpty()
                    )
                }
            },
            onToggleDomainWhitelist = { domain ->
                scope.launch { prefsManager.toggleWhitelistedDomain(domain) }
            }
        )

        Spacer(modifier = Modifier.height(10.dp))

        // 2. Adult content — 25 sites
        WebPresetCategoryCard(
            title = "Adult content",
            siteCount = DomainFilterRepository.ADULT_CONTENT.size,
            description = "Tube sites, cam sites, adult creator platforms",
            checked = prefs.webBlockAdultContent,
            expanded = expandedCategory == "adult",
            domains = DomainFilterRepository.ADULT_CONTENT,
            whitelistedDomains = prefs.webWhitelistedDomains,
            onToggleExpand = {
                expandedCategory = if (expandedCategory == "adult") null else "adult"
            },
            onCheckedChange = { enabled ->
                scope.launch {
                    prefsManager.setBooleanPreference(PreferencesManager.WEB_BLOCK_ADULT, enabled)
                    syncVpnServiceState(
                        prefs.webBlockSocialApps || enabled || prefs.webBlockOnlineCasinos ||
                            prefs.webBlockCasualGaming || prefs.webBlockSportsBetting ||
                            prefs.customBlockedDomains.isNotEmpty()
                    )
                }
            },
            onToggleDomainWhitelist = { domain ->
                scope.launch { prefsManager.toggleWhitelistedDomain(domain) }
            }
        )

        Spacer(modifier = Modifier.height(10.dp))

        // 3. Online casinos — 24 sites
        WebPresetCategoryCard(
            title = "Online casinos",
            siteCount = DomainFilterRepository.ONLINE_CASINOS.size,
            description = "Slots, poker, casino operators",
            checked = prefs.webBlockOnlineCasinos,
            expanded = expandedCategory == "casinos",
            domains = DomainFilterRepository.ONLINE_CASINOS,
            whitelistedDomains = prefs.webWhitelistedDomains,
            onToggleExpand = {
                expandedCategory = if (expandedCategory == "casinos") null else "casinos"
            },
            onCheckedChange = { enabled ->
                scope.launch {
                    prefsManager.setBooleanPreference(PreferencesManager.WEB_BLOCK_CASINOS, enabled)
                    syncVpnServiceState(
                        prefs.webBlockSocialApps || prefs.webBlockAdultContent || enabled ||
                            prefs.webBlockCasualGaming || prefs.webBlockSportsBetting ||
                            prefs.customBlockedDomains.isNotEmpty()
                    )
                }
            },
            onToggleDomainWhitelist = { domain ->
                scope.launch { prefsManager.toggleWhitelistedDomain(domain) }
            }
        )

        Spacer(modifier = Modifier.height(10.dp))

        // 4. Casual gaming portals — 21 sites
        WebPresetCategoryCard(
            title = "Casual gaming portals",
            siteCount = DomainFilterRepository.CASUAL_GAMING_PORTALS.size,
            description = "Browser games, streaming, chess portals",
            checked = prefs.webBlockCasualGaming,
            expanded = expandedCategory == "gaming",
            domains = DomainFilterRepository.CASUAL_GAMING_PORTALS,
            whitelistedDomains = prefs.webWhitelistedDomains,
            onToggleExpand = {
                expandedCategory = if (expandedCategory == "gaming") null else "gaming"
            },
            onCheckedChange = { enabled ->
                scope.launch {
                    prefsManager.setBooleanPreference(PreferencesManager.WEB_BLOCK_GAMING, enabled)
                    syncVpnServiceState(
                        prefs.webBlockSocialApps || prefs.webBlockAdultContent || prefs.webBlockOnlineCasinos ||
                            enabled || prefs.webBlockSportsBetting ||
                            prefs.customBlockedDomains.isNotEmpty()
                    )
                }
            },
            onToggleDomainWhitelist = { domain ->
                scope.launch { prefsManager.toggleWhitelistedDomain(domain) }
            }
        )

        Spacer(modifier = Modifier.height(10.dp))

        // 5. Sports betting — 22 sites
        WebPresetCategoryCard(
            title = "Sports betting",
            siteCount = DomainFilterRepository.SPORTS_BETTING.size,
            description = "Sportsbooks, fantasy, prop pick sites",
            checked = prefs.webBlockSportsBetting,
            expanded = expandedCategory == "sports",
            domains = DomainFilterRepository.SPORTS_BETTING,
            whitelistedDomains = prefs.webWhitelistedDomains,
            onToggleExpand = {
                expandedCategory = if (expandedCategory == "sports") null else "sports"
            },
            onCheckedChange = { enabled ->
                scope.launch {
                    prefsManager.setBooleanPreference(PreferencesManager.WEB_BLOCK_SPORTS, enabled)
                    syncVpnServiceState(
                        prefs.webBlockSocialApps || prefs.webBlockAdultContent || prefs.webBlockOnlineCasinos ||
                            prefs.webBlockCasualGaming || enabled ||
                            prefs.customBlockedDomains.isNotEmpty()
                    )
                }
            },
            onToggleDomainWhitelist = { domain ->
                scope.launch { prefsManager.toggleWhitelistedDomain(domain) }
            }
        )

        Spacer(modifier = Modifier.height(24.dp))
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun WebPresetCategoryCard(
    title: String,
    siteCount: Int,
    description: String,
    checked: Boolean,
    expanded: Boolean,
    domains: List<String>,
    whitelistedDomains: Set<String>,
    onToggleExpand: () -> Unit,
    onCheckedChange: (Boolean) -> Unit,
    onToggleDomainWhitelist: (String) -> Unit
) {
    val palette = SocialTheme.colors
    val allowedInPack = domains.count { whitelistedDomains.contains(it) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(palette.cardBackground)
            .clickable { onToggleExpand() }
            .padding(horizontal = 16.dp, vertical = 14.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f).padding(end = 12.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = title,
                        color = palette.textPrimary,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = if (allowedInPack > 0) {
                            "$siteCount sites · $allowedInPack allowed"
                        } else {
                            "$siteCount sites"
                        },
                        color = palette.textSecondary,
                        fontSize = 11.sp
                    )
                }
                Spacer(modifier = Modifier.height(3.dp))
                Text(
                    text = description,
                    color = palette.textSecondary,
                    fontSize = 12.sp,
                    lineHeight = 16.sp
                )
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Switch(
                    checked = checked,
                    onCheckedChange = onCheckedChange,
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = Color.White,
                        checkedTrackColor = Color(0xFF6366F1),
                        uncheckedThumbColor = Color(0xFF8E91A6),
                        uncheckedTrackColor = palette.switchTrackOff
                    )
                )
                Icon(
                    imageVector = if (expanded) Icons.Filled.KeyboardArrowUp else Icons.Filled.KeyboardArrowDown,
                    contentDescription = null,
                    tint = palette.textSecondary,
                    modifier = Modifier.size(20.dp)
                )
            }
        }

        if (expanded && domains.isNotEmpty()) {
            Spacer(modifier = Modifier.height(14.dp))
            Divider(color = Color(0xFF222330), thickness = 1.dp)
            Spacer(modifier = Modifier.height(12.dp))

            if (checked) {
                // View 17: Active Preset -> "Keep these reachable"
                Text(
                    text = "Keep these reachable",
                    color = palette.textPrimary,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = "Chips OFF = blocked. Chips ON = still works.",
                    color = palette.textSecondary,
                    fontSize = 11.sp
                )
            } else {
                // View 16: Inactive Preset -> "Sites in this pack"
                Text(
                    text = "Sites in this pack",
                    color = palette.textSecondary,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                domains.forEach { domain ->
                    val isWhitelisted = whitelistedDomains.contains(domain)
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(10.dp))
                            .background(
                                if (isWhitelisted) Color(0xFF282B48) else Color(0xFF1D1E2C)
                            )
                            .border(
                                width = 0.75.dp,
                                color = if (isWhitelisted) Color(0xFF818CF8) else Color(0xFF2D2E42),
                                shape = RoundedCornerShape(10.dp)
                            )
                            .clickable { onToggleDomainWhitelist(domain) }
                            .padding(horizontal = 10.dp, vertical = 6.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(5.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Language,
                                contentDescription = null,
                                tint = if (isWhitelisted) Color(0xFF818CF8) else Color(0xFF8E91A6),
                                modifier = Modifier.size(13.dp)
                            )
                            Text(
                                text = domain,
                                color = if (isWhitelisted) Color.White else Color(0xFFD1D5DB),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }
            }
        }
    }
}
