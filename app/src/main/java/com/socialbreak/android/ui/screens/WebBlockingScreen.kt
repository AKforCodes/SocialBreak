package com.socialbreak.android.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.socialbreak.android.data.DomainFilterRepository
import com.socialbreak.android.data.PreferencesManager
import com.socialbreak.android.ui.theme.*
import kotlinx.coroutines.launch

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
            .background(Color(0xFF000000))
            .padding(horizontal = 20.dp, vertical = 16.dp)
            .verticalScroll(rememberScrollState())
    ) {
        // Top Header (Screenshot 15: <- Web Blocking, Edit)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp, bottom = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBack, modifier = Modifier.size(36.dp)) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = Color.White
                    )
                }
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "Web Blocking",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }
            Text(
                text = "Edit",
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color(0xFF3B82F6),
                modifier = Modifier
                    .clickable { }
                    .padding(8.dp)
            )
        }

        Spacer(modifier = Modifier.height(14.dp))
        Text(
            text = "Presets",
            fontSize = 13.sp,
            color = Color(0xFF64748B),
            fontWeight = FontWeight.Medium
        )
        Spacer(modifier = Modifier.height(10.dp))

        // Preset 1: Social apps (16 sites) - Expandable (Screenshot 15, 16)
        PresetRow(
            title = "Social apps",
            siteCount = "16 sites",
            icon = Icons.Outlined.People,
            isChecked = socialBlocked,
            isExpanded = socialExpanded,
            onToggle = { coroutineScope.launch { prefs.setWebFilter("social", it) } },
            onRowClick = { socialExpanded = !socialExpanded }
        ) {
            Column(modifier = Modifier.padding(top = 16.dp)) {
                Text(
                    text = "Keep these reachable",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color.White
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "Chips OFF = blocked. Chips ON = still works.",
                    fontSize = 12.sp,
                    color = Color(0xFF64748B)
                )
                Spacer(modifier = Modifier.height(14.dp))

                // 2-Column Exact Grid of Chips (Screenshot 16)
                val appPairs = DomainFilterRepository.SOCIAL_APPS.chunked(2)
                for (pair in appPairs) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        for (app in pair) {
                            val isReachable = reachableChips.contains(app.id)
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .height(44.dp)
                                    .clip(RoundedCornerShape(9999.dp))
                                    .background(if (isReachable) Color(0xFF1E293B) else Color(0xFF111522))
                                    .border(
                                        1.dp,
                                        if (isReachable) Color(0xFF3B82F6) else Color(0xFF1E2538),
                                        RoundedCornerShape(9999.dp)
                                    )
                                    .clickable {
                                        coroutineScope.launch { prefs.toggleReachableChip(app.id) }
                                    }
                                    .padding(horizontal = 14.dp),
                                contentAlignment = Alignment.CenterStart
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = if (isReachable) Icons.Default.CheckCircle else Icons.Outlined.Circle,
                                        contentDescription = null,
                                        tint = if (isReachable) Color(0xFF3B82F6) else Color(0xFF64748B),
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = app.displayName,
                                        fontSize = 13.sp,
                                        color = if (isReachable) Color.White else Color(0xFF94A3B8)
                                    )
                                }
                            }
                        }
                        if (pair.size < 2) {
                            Spacer(modifier = Modifier.weight(1f))
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Preset 2: Adult content (25 sites) (Screenshot 15)
        PresetRow(
            title = "Adult content",
            siteCount = "25 sites",
            icon = Icons.Outlined.Explicit,
            isChecked = adultBlocked,
            isExpanded = false,
            onToggle = { coroutineScope.launch { prefs.setWebFilter("adult", it) } }
        )

        Spacer(modifier = Modifier.height(10.dp))

        // Preset 3: Online casinos (24 sites) (Screenshot 15)
        PresetRow(
            title = "Online casinos",
            siteCount = "24 sites",
            icon = Icons.Outlined.Casino,
            isChecked = casinosBlocked,
            isExpanded = false,
            onToggle = { coroutineScope.launch { prefs.setWebFilter("casinos", it) } }
        )

        Spacer(modifier = Modifier.height(10.dp))

        // Preset 4: Casual gaming portals (21 sites) - Expandable (Screenshot 15, 17)
        PresetRow(
            title = "Casual gaming portals",
            siteCount = "21 sites",
            icon = Icons.Outlined.SportsEsports,
            isChecked = gamingBlocked,
            isExpanded = gamingExpanded,
            onToggle = { coroutineScope.launch { prefs.setWebFilter("gaming", it) } },
            onRowClick = { gamingExpanded = !gamingExpanded }
        ) {
            Column(modifier = Modifier.padding(top = 16.dp)) {
                Text(
                    text = "Sites in this pack",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color.White
                )
                Spacer(modifier = Modifier.height(12.dp))

                // 2-Column Exact Grid of 21 Gaming Domains (Screenshot 17)
                val domainPairs = DomainFilterRepository.CASUAL_GAMING_PORTALS.chunked(2)
                for (pair in domainPairs) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        for (domain in pair) {
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .height(44.dp)
                                    .clip(RoundedCornerShape(9999.dp))
                                    .background(Color(0xFF111522))
                                    .border(1.dp, Color(0xFF1E2538), RoundedCornerShape(9999.dp))
                                    .padding(horizontal = 14.dp),
                                contentAlignment = Alignment.CenterStart
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Outlined.Language,
                                        contentDescription = null,
                                        tint = Color(0xFF64748B),
                                        modifier = Modifier.size(15.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = domain,
                                        fontSize = 12.sp,
                                        color = Color(0xFF94A3B8)
                                    )
                                }
                            }
                        }
                        if (pair.size < 2) {
                            Spacer(modifier = Modifier.weight(1f))
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Preset 5: Sports betting (22 sites) (Screenshot 15)
        PresetRow(
            title = "Sports betting",
            siteCount = "22 sites",
            icon = Icons.Outlined.SportsSoccer,
            isChecked = bettingBlocked,
            isExpanded = false,
            onToggle = { coroutineScope.launch { prefs.setWebFilter("betting", it) } }
        )

        Spacer(modifier = Modifier.height(24.dp))
    }
}

@Composable
fun PresetRow(
    title: String,
    siteCount: String,
    icon: ImageVector,
    isChecked: Boolean,
    isExpanded: Boolean = false,
    onToggle: (Boolean) -> Unit,
    onRowClick: (() -> Unit)? = null,
    expandedContent: (@Composable () -> Unit)? = null
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .border(1.dp, Color(0xFF1E2538), RoundedCornerShape(18.dp)),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF111522)),
        shape = RoundedCornerShape(18.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(enabled = onRowClick != null) { onRowClick?.invoke() },
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF1A2234)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = icon,
                            contentDescription = null,
                            tint = Color(0xFF94A3B8),
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = title,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color.White
                        )
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = siteCount,
                                fontSize = 12.sp,
                                color = Color(0xFF64748B)
                            )
                            if (onRowClick != null) {
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = if (isExpanded) "▲" else "▼",
                                    fontSize = 8.sp,
                                    color = Color(0xFF64748B)
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
                        checkedTrackColor = Color(0xFF3B82F6),
                        uncheckedThumbColor = Color(0xFF94A3B8),
                        uncheckedTrackColor = Color(0xFF1E2538)
                    )
                )
            }

            if (expandedContent != null) {
                AnimatedVisibility(visible = isExpanded) {
                    expandedContent()
                }
            }
        }
    }
}
