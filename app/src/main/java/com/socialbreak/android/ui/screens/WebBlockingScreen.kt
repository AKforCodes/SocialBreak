package com.socialbreak.android.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.socialbreak.android.data.DomainFilterRepository
import com.socialbreak.android.data.PreferencesManager
import com.socialbreak.android.ui.theme.*
import kotlinx.coroutines.launch

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun WebBlockingScreen(prefs: PreferencesManager, onBack: () -> Unit) {
    val coroutineScope = rememberCoroutineScope()

    val adultBlocked by prefs.webBlockingAdult.collectAsState(initial = false)
    val casinosBlocked by prefs.webBlockingCasinos.collectAsState(initial = false)
    val gamingBlocked by prefs.webBlockingGaming.collectAsState(initial = false)
    val bettingBlocked by prefs.webBlockingBetting.collectAsState(initial = false)
    val socialBlocked by prefs.webBlockingSocial.collectAsState(initial = false)
    val reachableChips by prefs.reachableSocialChips.collectAsState(initial = emptySet())

    var socialExpanded by remember { mutableStateOf(false) }
    var gamingExpanded by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BgPrimary)
            .padding(horizontal = 20.dp, vertical = 16.dp)
            .verticalScroll(rememberScrollState())
    ) {
        // Top Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBack) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = TextHighlight
                    )
                }
                Text(
                    text = "Web Blocking",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = TextHighlight
                )
            }
            TextButton(onClick = {}) {
                Text("Edit", color = AccentBlue, fontWeight = FontWeight.SemiBold)
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "Presets",
            style = MaterialTheme.typography.labelMedium,
            color = TextMuted,
            fontWeight = FontWeight.Medium
        )
        Spacer(modifier = Modifier.height(12.dp))

        // Preset 1: Social Apps (16 sites)
        PresetCard(
            title = "Social apps",
            sub = "16 sites",
            icon = Icons.Default.People,
            isChecked = socialBlocked,
            isExpanded = socialExpanded,
            onToggle = { coroutineScope.launch { prefs.setWebFilter("social", it) } },
            onExpandToggle = { socialExpanded = !socialExpanded }
        ) {
            Column(modifier = Modifier.padding(top = 12.dp)) {
                Text(
                    text = "Keep these reachable",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = TextHighlight
                )
                Text(
                    text = "Chips OFF = blocked. Chips ON = still works.",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextMuted,
                    modifier = Modifier.padding(top = 2.dp, bottom = 10.dp)
                )

                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    DomainFilterRepository.SOCIAL_APPS.forEach { app ->
                        val isReachable = reachableChips.contains(app.id)
                        Surface(
                            shape = RoundedCornerShape(9999.dp),
                            color = if (isReachable) AccentBlue.copy(alpha = 0.2f) else BgTertiary,
                            border = androidx.compose.foundation.BorderStroke(
                                1.dp,
                                if (isReachable) AccentBlue else BorderSubtle
                            ),
                            modifier = Modifier.clickable {
                                coroutineScope.launch { prefs.toggleReachableChip(app.id) }
                            }
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                if (isReachable) {
                                    Icon(
                                        Icons.Default.Check,
                                        contentDescription = null,
                                        tint = AccentBlue,
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                }
                                Text(
                                    text = app.displayName,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = if (isReachable) AccentBlue else TextPrimary
                                )
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Preset 2: Adult Content (25 sites)
        PresetCard(
            title = "Adult content",
            sub = "25 sites",
            icon = Icons.Default.Explicit,
            isChecked = adultBlocked,
            isExpanded = false,
            onToggle = { coroutineScope.launch { prefs.setWebFilter("adult", it) } }
        )

        Spacer(modifier = Modifier.height(10.dp))

        // Preset 3: Online Casinos (24 sites)
        PresetCard(
            title = "Online casinos",
            sub = "24 sites",
            icon = Icons.Default.Casino,
            isChecked = casinosBlocked,
            isExpanded = false,
            onToggle = { coroutineScope.launch { prefs.setWebFilter("casinos", it) } }
        )

        Spacer(modifier = Modifier.height(10.dp))

        // Preset 4: Casual Gaming Portals (21 sites)
        PresetCard(
            title = "Casual gaming portals",
            sub = "21 sites",
            icon = Icons.Default.SportsEsports,
            isChecked = gamingBlocked,
            isExpanded = gamingExpanded,
            onToggle = { coroutineScope.launch { prefs.setWebFilter("gaming", it) } },
            onExpandToggle = { gamingExpanded = !gamingExpanded }
        ) {
            Column(modifier = Modifier.padding(top = 12.dp)) {
                Text(
                    text = "Sites in this pack",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = TextHighlight,
                    modifier = Modifier.padding(bottom = 8.dp)
                )

                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    DomainFilterRepository.CASUAL_GAMING_PORTALS.forEach { site ->
                        Surface(
                            shape = RoundedCornerShape(9999.dp),
                            color = BgTertiary,
                            border = androidx.compose.foundation.BorderStroke(1.dp, BorderSubtle)
                        ) {
                            Text(
                                text = site,
                                style = MaterialTheme.typography.bodySmall,
                                color = TextSecondary,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Preset 5: Sports Betting (22 sites)
        PresetCard(
            title = "Sports betting",
            sub = "22 sites",
            icon = Icons.Default.SportsSoccer,
            isChecked = bettingBlocked,
            isExpanded = false,
            onToggle = { coroutineScope.launch { prefs.setWebFilter("betting", it) } }
        )
    }
}

@Composable
fun PresetCard(
    title: String,
    sub: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    isChecked: Boolean,
    isExpanded: Boolean,
    onToggle: (Boolean) -> Unit,
    onExpandToggle: (() -> Unit)? = null,
    expandedContent: (@Composable () -> Unit)? = null
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = BgSecondary,
        border = androidx.compose.foundation.BorderStroke(1.dp, BorderSubtle)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .weight(1f)
                        .clickable(enabled = onExpandToggle != null) { onExpandToggle?.invoke() }
                ) {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(BgTertiary),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(icon, contentDescription = null, tint = TextMuted, modifier = Modifier.size(20.dp))
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(text = title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold, color = TextHighlight)
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(text = sub, style = MaterialTheme.typography.bodySmall, color = TextMuted)
                            if (onExpandToggle != null) {
                                Spacer(modifier = Modifier.width(4.dp))
                                Icon(
                                    imageVector = if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                                    contentDescription = null,
                                    tint = TextMuted,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }
                }

                Switch(
                    checked = isChecked,
                    onCheckedChange = onToggle,
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = Color.White,
                        checkedTrackColor = AccentBlue,
                        uncheckedThumbColor = TextMuted,
                        uncheckedTrackColor = BgTertiary
                    )
                )
            }

            if (isExpanded && expandedContent != null) {
                expandedContent()
            }
        }
    }
}
