package com.socialbreak.android.ui.screens

import android.app.admin.DevicePolicyManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.view.HapticFeedbackConstants
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import com.socialbreak.android.data.PreferencesManager
import com.socialbreak.android.data.SocialBreakPreferences
import com.socialbreak.android.data.UsageStatsHelper
import com.socialbreak.android.receivers.UninstallProtectionReceiver
import com.socialbreak.android.ui.components.FrictionGauntletDialog
import com.socialbreak.android.ui.components.ProtectionPermissionHelper
import com.socialbreak.android.ui.components.ProtectionSetupBottomSheet
import com.socialbreak.android.ui.components.SocialAppIcon
import com.socialbreak.android.ui.components.SocialBreakRibbonIcon
import com.socialbreak.android.ui.theme.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.Random
import kotlin.math.*

private data class StarParticle(
    val xFrac: Float,
    val yFrac: Float,
    val radiusDp: Float,
    val baseAlpha: Float,
    val phaseOffset: Float
)

private val CONSTELLATION_STARS: List<StarParticle> = run {
    val rng = Random(11404L)
    List(140) {
        StarParticle(
            xFrac = rng.nextFloat(),
            yFrac = rng.nextFloat(),
            radiusDp = 0.65f + rng.nextFloat() * 1.35f,
            baseAlpha = 0.14f + rng.nextFloat() * 0.42f,
            phaseOffset = rng.nextFloat() * (2f * PI.toFloat())
        )
    }
}

private data class ConfettiParticle(
    val startXFrac: Float,
    val vx: Float,
    val vy: Float,
    val sizeDp: Float,
    val color: Color,
    val rotationSpeed: Float,
    val isCircle: Boolean
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeDashboardScreen(
    prefs: SocialBreakPreferences,
    prefsManager: PreferencesManager,
    onOpenCleanWebApp: (String) -> Unit,
    onNavigateWebBlocking: () -> Unit,
    onNavigateTodayStats: () -> Unit
) {
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val view = LocalView.current
    val density = LocalDensity.current
    val palette = SocialTheme.colors

    var showHardModeConfirmScreen by remember { mutableStateOf(false) }
    var preventUninstallChecked by remember { mutableStateOf(true) }
    var showDisableHardModeGauntlet by remember { mutableStateOf(false) }
    var showProtectionSheet by remember { mutableStateOf(false) }
    var showCustomizeAppsSheet by remember { mutableStateOf(false) }
    var selectedAppForAllowance by remember { mutableStateOf<String?>(null) }
    var showConfettiCelebration by remember { mutableStateOf(false) }
    var showAllTodayApps by remember { mutableStateOf(false) }

    val allProtectionsReady = remember(showProtectionSheet) {
        ProtectionPermissionHelper.areAllProtectionsActive(context)
    }

    val usageList = remember(prefs) {
        UsageStatsHelper.getTodaySocialUsage(context)
    }
    val hourlyBuckets = remember(prefs) {
        UsageStatsHelper.getTodayHourlyBuckets(context)
    }

    // View 6: Full-Screen "Turn on Hard Mode?" Confirmation View
    if (showHardModeConfirmScreen) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFF000000))
                .padding(horizontal = 28.dp, vertical = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.weight(1f))

            Icon(
                imageVector = Icons.Outlined.Lock,
                contentDescription = "Hard Mode Lock",
                tint = PeachOrange,
                modifier = Modifier.size(36.dp)
            )

            Spacer(modifier = Modifier.height(20.dp))

            Text(
                text = "Turn on Hard Mode?",
                color = Color.White,
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = "Locks your settings behind a typing challenge, 5 mental math problems, and a 60-second wait. You won't be able to impulsively turn off blocking or lower limits.",
                color = Color(0xFF9CA3AF),
                fontSize = 14.sp,
                lineHeight = 20.sp,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(24.dp))

            // "Prevent easy uninstall" checkbox card
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(Color(0xFF14141C))
                    .clickable {
                        view.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
                        preventUninstallChecked = !preventUninstallChecked
                    }
                    .padding(horizontal = 16.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(20.dp)
                        .clip(RoundedCornerShape(5.dp))
                        .background(if (preventUninstallChecked) PeachOrange else Color(0xFF282A3A)),
                    contentAlignment = Alignment.Center
                ) {
                    if (preventUninstallChecked) {
                        Icon(
                            imageVector = Icons.Filled.Check,
                            contentDescription = null,
                            tint = Color.Black,
                            modifier = Modifier.size(14.dp)
                        )
                    }
                }
                Text(
                    text = "Prevent easy uninstall",
                    color = Color.White,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Peach-orange "Continue" pill button
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .clip(RoundedCornerShape(999.dp))
                    .background(PeachOrange)
                    .clickable {
                        view.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
                        scope.launch {
                            prefsManager.setHardMode(true, preventUninstallChecked)
                            if (preventUninstallChecked) {
                                prefsManager.setUninstallProtection(true)
                                try {
                                    val dpm = context.getSystemService(Context.DEVICE_POLICY_SERVICE) as DevicePolicyManager
                                    val adminComp = ComponentName(context, UninstallProtectionReceiver::class.java)
                                    if (!dpm.isAdminActive(adminComp)) {
                                        val intent = Intent(DevicePolicyManager.ACTION_ADD_DEVICE_ADMIN).apply {
                                            putExtra(DevicePolicyManager.EXTRA_DEVICE_ADMIN, adminComp)
                                            putExtra(
                                                DevicePolicyManager.EXTRA_ADD_EXPLANATION,
                                                "Keeps SocialBreak on this phone so blocking can't be removed by deleting the app."
                                            )
                                        }
                                        context.startActivity(intent)
                                    }
                                } catch (_: Exception) {}
                            }
                        }
                        showHardModeConfirmScreen = false
                        showConfettiCelebration = true
                    },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Continue",
                    color = Color(0xFF1A140E),
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "Not yet",
                color = Color(0xFF8E91A6),
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.clickable { showHardModeConfirmScreen = false }
            )

            Spacer(modifier = Modifier.weight(1.2f))

            Text(
                text = "SocialBreak",
                color = Color(0xFF555768),
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold
            )
        }
        return
    }

    // All Home-eligible apps ordered by constellationOrder and filtered by hiddenHomeApps
    val orderedVisibleApps = remember(prefs.constellationOrder, prefs.hiddenHomeApps) {
        val base = (prefs.constellationOrder + SocialBreakPreferences.DEFAULT_CONSTELLATION_ORDER).distinct()
        base.filter { it !in prefs.hiddenHomeApps }
    }
    val protectedOrbitApps = remember(orderedVisibleApps, prefs.protectedHomeApps) {
        orderedVisibleApps.filter { it in prefs.protectedHomeApps }
    }
    val unprotectedBankApps = remember(orderedVisibleApps, prefs.protectedHomeApps) {
        orderedVisibleApps.filter { it !in prefs.protectedHomeApps }
    }

    // Interactive Constellation Drag State (matching oe.c7)
    var draggingOrbitAppId by remember { mutableStateOf<String?>(null) }
    var orbitDragOffset by remember { mutableStateOf(Offset.Zero) }
    var isOverUnprotectZone by remember { mutableStateOf(false) }

    var draggingBankAppId by remember { mutableStateOf<String?>(null) }
    var bankDragOffset by remember { mutableStateOf(Offset.Zero) }
    var isBankDragOverHub by remember { mutableStateOf(false) }

    var hubTopInRoot by remember { mutableFloatStateOf(0f) }
    var hubBottomInRoot by remember { mutableFloatStateOf(0f) }
    var bankTopInRoot by remember { mutableFloatStateOf(0f) }

    // Shake animation when hovering over unprotect zone (oe.n5: 320ms infinite reverse)
    val infiniteTransition = rememberInfiniteTransition(label = "constellation-infinite")
    val unprotectShakePhase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 320, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "unprotect-shake"
    )
    val starTwinklePhase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = (2f * PI).toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 4200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "star-twinkle"
    )
    val hardModeHubGlow by animateFloatAsState(
        targetValue = if (prefs.hardModeEnabled) 1f else 0f,
        animationSpec = tween(durationMillis = 350, easing = FastOutSlowInEasing),
        label = "hub-hard-mode-glow"
    )

    Box(modifier = Modifier.fillMaxSize().background(palette.background)) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 14.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Top Header: "SocialBreak [S]" | "Customize apps"
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "SocialBreak",
                        color = palette.textPrimary,
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold
                    )
                    SocialBreakRibbonIcon(
                        size = 22.dp,
                        tint = palette.textPrimary
                    )
                }

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(10.dp))
                        .background(palette.cardBackground)
                        .border(0.5.dp, palette.border, RoundedCornerShape(10.dp))
                        .clickable {
                            view.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
                            showCustomizeAppsSheet = true
                        }
                        .padding(horizontal = 12.dp, vertical = 7.dp)
                ) {
                    Text(
                        text = "Customize apps",
                        color = palette.textSecondary,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            // View 5: Protection alerts banner (shown if permissions aren't all granted)
            if (!allProtectionsReady) {
                Spacer(modifier = Modifier.height(12.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(palette.cardBackground)
                        .clickable { showProtectionSheet = true }
                        .padding(horizontal = 14.dp, vertical = 11.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(AmberGold)
                        )
                        Column {
                            Text(
                                text = "Protection alerts",
                                color = palette.textPrimary,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = "App and sleep blocking · Content blocking · Notification access",
                                color = palette.textSecondary,
                                fontSize = 11.sp,
                                maxLines = 1
                            )
                        }
                    }
                    Text(
                        text = "Fix",
                        color = AmberGold,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(start = 8.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Centered "Hard Mode" Pill Switch
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(14.dp))
                    .background(palette.cardBackground)
                    .border(0.5.dp, palette.border, RoundedCornerShape(14.dp))
                    .clickable {
                        view.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
                        if (!prefs.hardModeEnabled) {
                            showHardModeConfirmScreen = true
                        } else {
                            showDisableHardModeGauntlet = true
                        }
                    }
                    .padding(horizontal = 14.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Icon(
                    imageVector = Icons.Outlined.Lock,
                    contentDescription = null,
                    tint = if (prefs.hardModeEnabled) PeachOrange else palette.textSecondary,
                    modifier = Modifier.size(16.dp)
                )
                Text(
                    text = "Hard Mode",
                    color = palette.textPrimary,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Switch(
                    checked = prefs.hardModeEnabled,
                    onCheckedChange = { checked ->
                        view.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
                        if (checked) {
                            showHardModeConfirmScreen = true
                        } else {
                            showDisableHardModeGauntlet = true
                        }
                    },
                    modifier = Modifier.height(24.dp),
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = Color.White,
                        checkedTrackColor = PeachOrange,
                        uncheckedThumbColor = Color(0xFF8E91A6),
                        uncheckedTrackColor = palette.switchTrackOff
                    )
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Interactive Constellation Hub (oe.c7.d: 384.dp height, RoundedCornerShape(28.dp))
            BoxWithConstraints(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(384.dp)
                    .onGloballyPositioned { coords ->
                        val pos = coords.positionInRoot()
                        hubTopInRoot = pos.y
                        hubBottomInRoot = pos.y + coords.size.height
                    }
                    .clip(RoundedCornerShape(28.dp))
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                Color(0xFF0A0B12),
                                Color(0xFF0E101B),
                                Color(0xFF0A0B12)
                            )
                        )
                    )
                    .border(
                        width = if (isBankDragOverHub) 1.5.dp else 0.75.dp,
                        color = if (isBankDragOverHub) Color(0xFF4DB86B) else Color(0xFF1F2133),
                        shape = RoundedCornerShape(28.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                val widthPx = constraints.maxWidth.toFloat()
                val heightPx = constraints.maxHeight.toFloat()
                val centerPx = Offset(widthPx / 2f, heightPx / 2f)
                val orbitRadiusPx = min(widthPx * 0.36f, heightPx * 0.32f)

                // Compute animated target offsets for each orbiting app (c7.D: angle = 2*PI*i/N - PI/2)
                val count = protectedOrbitApps.size.coerceAtLeast(1)
                val targetOffsets = remember(protectedOrbitApps, widthPx, heightPx) {
                    protectedOrbitApps.mapIndexed { idx, appId ->
                        val angle = (2.0 * PI * idx.toDouble() / count.toDouble()) - (PI / 2.0)
                        appId to Offset(
                            x = (orbitRadiusPx * cos(angle)).toFloat(),
                            y = (orbitRadiusPx * sin(angle)).toFloat()
                        )
                    }.toMap()
                }

                // Smoothly animate each app's orbital position when reordered
                val animatedOrbitalOffsets = protectedOrbitApps.associateWith { appId ->
                    val target = targetOffsets[appId] ?: Offset.Zero
                    val animX by animateFloatAsState(
                        targetValue = target.x,
                        animationSpec = spring(
                            dampingRatio = 0.74f,
                            stiffness = 380f
                        ),
                        label = "orbit-x-$appId"
                    )
                    val animY by animateFloatAsState(
                        targetValue = target.y,
                        animationSpec = spring(
                            dampingRatio = 0.74f,
                            stiffness = 380f
                        ),
                        label = "orbit-y-$appId"
                    )
                    if (draggingOrbitAppId == appId) {
                        target + orbitDragOffset
                    } else {
                        Offset(animX, animY)
                    }
                }

                // 140 Twinkling Starfield + Center Radial Glow + Gradient Tether Lines (c7.H)
                Canvas(modifier = Modifier.fillMaxSize()) {
                    // Starfield
                    CONSTELLATION_STARS.forEach { star ->
                        val twinkle = 0.65f + 0.35f * sin(starTwinklePhase + star.phaseOffset)
                        drawCircle(
                            color = Color.White.copy(alpha = (star.baseAlpha * twinkle).coerceIn(0.05f, 0.85f)),
                            radius = star.radiusDp.dp.toPx(),
                            center = Offset(size.width * star.xFrac, size.height * star.yFrac)
                        )
                    }

                    // Subtle radial nebula glow around center hub
                    val glowColor = if (prefs.hardModeEnabled) {
                        Color(0xFFE5A467).copy(alpha = 0.14f + 0.08f * hardModeHubGlow)
                    } else {
                        Color(0xFF6366F1).copy(alpha = 0.12f)
                    }
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(glowColor, Color.Transparent),
                            center = centerPx,
                            radius = orbitRadiusPx * 1.15f
                        ),
                        radius = orbitRadiusPx * 1.15f,
                        center = centerPx
                    )

                    // Gradient tether lines from center Hub to each orbiting app (c7.H)
                    protectedOrbitApps.forEach { appId ->
                        val rel = animatedOrbitalOffsets[appId] ?: Offset.Zero
                        val nodeCenter = centerPx + rel
                        val isDraggingThis = draggingOrbitAppId == appId
                        val tetherColorStart = if (isDraggingThis && isOverUnprotectZone) {
                            Color(0xFFFF7070).copy(alpha = 0.85f)
                        } else if (prefs.hardModeEnabled) {
                            Color(0xFFE5A467).copy(alpha = 0.55f)
                        } else {
                            Color(0xFF818CF8).copy(alpha = 0.55f)
                        }
                        val tetherColorEnd = if (isDraggingThis && isOverUnprotectZone) {
                            Color(0xFFFF7070).copy(alpha = 0.35f)
                        } else {
                            Color(0xFF3E4264).copy(alpha = 0.30f)
                        }
                        drawLine(
                            brush = Brush.linearGradient(
                                colors = listOf(tetherColorStart, tetherColorEnd),
                                start = centerPx,
                                end = nodeCenter
                            ),
                            start = centerPx,
                            end = nodeCenter,
                            strokeWidth = if (isDraggingThis) 2.4.dp.toPx() else 1.75.dp.toPx(),
                            cap = StrokeCap.Round
                        )
                    }
                }

                // Center SocialBreak Hub (c7.v: 86.dp rounded square inside 116.dp glow box)
                Box(
                    modifier = Modifier.size(116.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier
                            .size(72.dp)
                            .shadow(
                                elevation = if (prefs.hardModeEnabled) 18.dp else 10.dp,
                                shape = RoundedCornerShape(22.dp),
                                ambientColor = if (prefs.hardModeEnabled) PeachOrange else IndigoActive,
                                spotColor = if (prefs.hardModeEnabled) PeachOrange else IndigoActive
                            )
                            .clip(RoundedCornerShape(22.dp))
                            .background(
                                Brush.linearGradient(
                                    colors = listOf(Color(0xFF1A1C2C), Color(0xFF12131E))
                                )
                            )
                            .border(
                                width = 1.5.dp,
                                color = if (prefs.hardModeEnabled) {
                                    PeachOrange.copy(alpha = 0.7f)
                                } else {
                                    Color(0xFF343752)
                                },
                                shape = RoundedCornerShape(22.dp)
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        SocialBreakRibbonIcon(size = 32.dp, tint = Color.White)
                    }
                }

                // Orbiting Protected Apps (c7.u) - Draggable around orbit & down to unprotect bank!
                protectedOrbitApps.forEach { appId ->
                    key(appId) {
                        val currentRel = animatedOrbitalOffsets[appId] ?: Offset.Zero
                        val isDraggingThis = draggingOrbitAppId == appId
                        val limit = prefs.getAppDailyLimitMinutes(appId)
                        val usedMins = usageList.firstOrNull { it.id == appId }?.minutesToday ?: 0
                        val timerLabel = if (limit > 0) {
                            "${usedMins}m / ${limit}m"
                        } else {
                            String.format(Locale.US, "%d:%02d", usedMins / 60, usedMins % 60)
                        }

                        val shakePx = if (isDraggingThis && isOverUnprotectZone) {
                            (sin(unprotectShakePhase.toDouble() * 4.0 * PI) * 5.0).toFloat()
                        } else {
                            0f
                        }

                        Box(
                            modifier = Modifier
                                .offset {
                                    IntOffset(
                                        x = (currentRel.x + shakePx).roundToInt(),
                                        y = currentRel.y.roundToInt()
                                    )
                                }
                                .zIndex(if (isDraggingThis) 10f else 1f)
                                .graphicsLayer {
                                    scaleX = if (isDraggingThis) 1.12f else 1f
                                    scaleY = if (isDraggingThis) 1.12f else 1f
                                    alpha = if (isDraggingThis && isOverUnprotectZone) 0.75f else 1f
                                }
                                .pointerInput(appId, protectedOrbitApps, orderedVisibleApps) {
                                    detectDragGestures(
                                        onDragStart = {
                                            view.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
                                            draggingOrbitAppId = appId
                                            orbitDragOffset = Offset.Zero
                                            isOverUnprotectZone = false
                                        },
                                        onDrag = { change, dragAmount ->
                                            change.consume()
                                            orbitDragOffset += dragAmount
                                            val basePos = targetOffsets[appId] ?: Offset.Zero
                                            val liveRel = basePos + orbitDragOffset

                                            // Check if dragged down toward bottom of hub / into bank (c7.d unprotect zone)
                                            val unprotectThresholdY = heightPx * 0.36f
                                            val nowOverUnprotect = liveRel.y > unprotectThresholdY
                                            if (nowOverUnprotect != isOverUnprotectZone) {
                                                isOverUnprotectZone = nowOverUnprotect
                                                view.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
                                            }

                                            // If not in unprotect zone and >= 2 orbiting apps, check angle for live orbit reordering (c7.p & c7.e)
                                            if (!nowOverUnprotect && protectedOrbitApps.size >= 2) {
                                                val rawAngle = atan2(liveRel.y.toDouble(), liveRel.x.toDouble())
                                                val normalizedAngle = ((rawAngle + PI / 2.0) % (2.0 * PI) + (2.0 * PI)) % (2.0 * PI)
                                                val slotSpan = (2.0 * PI) / protectedOrbitApps.size.toDouble()
                                                val targetSlot = ((normalizedAngle + slotSpan / 2.0) / slotSpan).toInt() % protectedOrbitApps.size
                                                val currentSlot = protectedOrbitApps.indexOf(appId)
                                                if (currentSlot != -1 && targetSlot != currentSlot) {
                                                    val targetAppAtSlot = protectedOrbitApps[targetSlot]
                                                    val fullOrder = orderedVisibleApps.toMutableList()
                                                    val idxA = fullOrder.indexOf(appId)
                                                    val idxB = fullOrder.indexOf(targetAppAtSlot)
                                                    if (idxA != -1 && idxB != -1) {
                                                        // Adjust drag offset so dragged icon stays under finger when target slot updates
                                                        val oldTargetOffset = targetOffsets[appId] ?: Offset.Zero
                                                        val newAngle = (2.0 * PI * targetSlot.toDouble() / protectedOrbitApps.size.toDouble()) - (PI / 2.0)
                                                        val newTargetOffset = Offset(
                                                            x = (orbitRadiusPx * cos(newAngle)).toFloat(),
                                                            y = (orbitRadiusPx * sin(newAngle)).toFloat()
                                                        )
                                                        orbitDragOffset += (oldTargetOffset - newTargetOffset)
                                                        fullOrder.removeAt(idxA)
                                                        fullOrder.add(idxB, appId)
                                                        view.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
                                                        scope.launch {
                                                            prefsManager.setConstellationOrder(fullOrder)
                                                        }
                                                    }
                                                }
                                            }
                                        },
                                        onDragEnd = {
                                            if (isOverUnprotectZone) {
                                                view.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
                                                scope.launch {
                                                    prefsManager.setAppProtectedOnHome(appId, false)
                                                }
                                            }
                                            draggingOrbitAppId = null
                                            orbitDragOffset = Offset.Zero
                                            isOverUnprotectZone = false
                                        },
                                        onDragCancel = {
                                            draggingOrbitAppId = null
                                            orbitDragOffset = Offset.Zero
                                            isOverUnprotectZone = false
                                        }
                                    )
                                }
                        ) {
                            ConnectedAppNode(
                                appId = appId,
                                iconTheme = prefs.iconTheme,
                                timerText = timerLabel,
                                isDangerHighlight = isDraggingThis && isOverUnprotectZone,
                                onClick = {
                                    view.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
                                    selectedAppForAllowance = appId
                                }
                            )
                        }
                    }
                }

                // When 0 apps are protected, show green "Protect all apps" pill button (oe_c7_fb.java lines 4454-4508)
                if (protectedOrbitApps.isEmpty() && orderedVisibleApps.isNotEmpty()) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .padding(bottom = 26.dp)
                            .clip(RoundedCornerShape(999.dp))
                            .background(Color(0xFF4DB86B).copy(alpha = 0.18f))
                            .border(1.dp, Color(0xFF4DB86B), RoundedCornerShape(999.dp))
                            .clickable {
                                view.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
                                scope.launch {
                                    prefsManager.protectAllHomeApps(orderedVisibleApps)
                                }
                                showConfettiCelebration = true
                            }
                            .padding(horizontal = 20.dp, vertical = 10.dp)
                    ) {
                        Text(
                            text = "Protect all apps",
                            color = Color(0xFF4DB86B),
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                // Bottom overlay inside Hub when dragging an orbiting app toward unprotect bank
                if (draggingOrbitAppId != null && isOverUnprotectZone) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .padding(bottom = 14.dp)
                            .clip(RoundedCornerShape(999.dp))
                            .background(Color(0xFFFF7070).copy(alpha = 0.18f))
                            .border(1.dp, Color(0xFFFF7070), RoundedCornerShape(999.dp))
                            .padding(horizontal = 16.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = "Release to unprotect",
                            color = Color(0xFFFF7070),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                } else if (isBankDragOverHub) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .padding(bottom = 14.dp)
                            .clip(RoundedCornerShape(999.dp))
                            .background(Color(0xFF4DB86B).copy(alpha = 0.18f))
                            .border(1.dp, Color(0xFF4DB86B), RoundedCornerShape(999.dp))
                            .padding(horizontal = 16.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = "Release to protect",
                            color = Color(0xFF4DB86B),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Bottom App Bank Card (c7.b: Unprotected apps dock, supports drag-up or tap to protect, and drop-to-unprotect)
            val bankBorderColor by animateColorAsState(
                targetValue = when {
                    draggingOrbitAppId != null && isOverUnprotectZone -> Color(0xFFFF7070)
                    draggingOrbitAppId != null -> Color(0xFF3E4056)
                    else -> Color.Transparent
                },
                label = "bank-border-color"
            )
            val bankBgColor by animateColorAsState(
                targetValue = if (draggingOrbitAppId != null && isOverUnprotectZone) {
                    Color(0xFFFF7070).copy(alpha = 0.14f)
                } else {
                    palette.cardBackground
                },
                label = "bank-bg-color"
            )

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .onGloballyPositioned { coords ->
                        bankTopInRoot = coords.positionInRoot().y
                    }
                    .clip(RoundedCornerShape(22.dp))
                    .background(bankBgColor)
                    .border(
                        width = if (draggingOrbitAppId != null) 1.5.dp else 0.5.dp,
                        color = if (draggingOrbitAppId != null) bankBorderColor else palette.border,
                        shape = RoundedCornerShape(22.dp)
                    )
                    .padding(horizontal = 16.dp, vertical = 14.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                if (unprotectedBankApps.isEmpty()) {
                    Text(
                        text = if (draggingOrbitAppId != null) {
                            "Release here to unprotect"
                        } else {
                            "All visible apps are protected in orbit · Drag an app here to unprotect"
                        },
                        color = if (draggingOrbitAppId != null && isOverUnprotectZone) Color(0xFFFF7070) else palette.textSecondary,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(vertical = 8.dp)
                    )
                } else {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        unprotectedBankApps.forEach { appId ->
                            key(appId) {
                                val limit = prefs.getAppDailyLimitMinutes(appId)
                                val isDraggingBankItem = draggingBankAppId == appId
                                Box(
                                    modifier = Modifier
                                        .offset {
                                            if (isDraggingBankItem) {
                                                IntOffset(
                                                    bankDragOffset.x.roundToInt(),
                                                    bankDragOffset.y.roundToInt()
                                                )
                                            } else {
                                                IntOffset.Zero
                                            }
                                        }
                                        .zIndex(if (isDraggingBankItem) 20f else 1f)
                                        .graphicsLayer {
                                            scaleX = if (isDraggingBankItem) 1.12f else 1f
                                            scaleY = if (isDraggingBankItem) 1.12f else 1f
                                        }
                                        .pointerInput(appId) {
                                            detectDragGestures(
                                                onDragStart = {
                                                    view.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
                                                    draggingBankAppId = appId
                                                    bankDragOffset = Offset.Zero
                                                    isBankDragOverHub = false
                                                },
                                                onDrag = { change, dragAmount ->
                                                    change.consume()
                                                    bankDragOffset += dragAmount
                                                    val overHub = bankDragOffset.y < -48.dp.toPx()
                                                    if (overHub != isBankDragOverHub) {
                                                        isBankDragOverHub = overHub
                                                        view.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
                                                    }
                                                },
                                                onDragEnd = {
                                                    if (isBankDragOverHub) {
                                                        view.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
                                                        scope.launch {
                                                            prefsManager.setAppProtectedOnHome(appId, true)
                                                        }
                                                    }
                                                    draggingBankAppId = null
                                                    bankDragOffset = Offset.Zero
                                                    isBankDragOverHub = false
                                                },
                                                onDragCancel = {
                                                    draggingBankAppId = null
                                                    bankDragOffset = Offset.Zero
                                                    isBankDragOverHub = false
                                                }
                                            )
                                        }
                                ) {
                                    DockAppItem(
                                        appId = appId,
                                        iconTheme = prefs.iconTheme,
                                        limitText = if (limit > 0) "${limit}m limit" else "No limit",
                                        onClick = {
                                            view.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
                                            selectedAppForAllowance = appId
                                        },
                                        onProtectClick = {
                                            view.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
                                            scope.launch {
                                                prefsManager.setAppProtectedOnHome(appId, true)
                                            }
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Scrollable Home "Today" Activity & Blocked Distractions Card (ng.A0 in oe_ng.java lines 903-1420)
            HomeTodayActivityCard(
                prefs = prefs,
                usageList = usageList,
                hourlyBuckets = hourlyBuckets,
                showAllApps = showAllTodayApps,
                onToggleShowAll = { showAllTodayApps = !showAllTodayApps },
                onOpenTodayStats = onNavigateTodayStats,
                onSelectApp = { appId -> selectedAppForAllowance = appId }
            )

            Spacer(modifier = Modifier.height(84.dp))
        }

        // Confetti Celebration Overlay (db.k in oe_db.java)
        if (showConfettiCelebration) {
            ConfettiCelebrationOverlay(
                onFinished = { showConfettiCelebration = false }
            )
        }
    }

    // Per-App Daily Scrolling Allowance & Native Settings Bottom Sheet
    selectedAppForAllowance?.let { appId ->
        val displayTitle = when (appId) {
            "instagram" -> "Instagram"
            "youtube" -> "YouTube"
            "tiktok" -> "TikTok"
            "snapchat" -> "Snapchat"
            "facebook" -> "Facebook"
            "x" -> "X"
            "linkedin" -> "LinkedIn"
            "reddit" -> "Reddit"
            else -> appId.replaceFirstChar { it.uppercase() }
        }
        var limitMinutes by remember(appId) { mutableIntStateOf(prefs.getAppDailyLimitMinutes(appId)) }
        val isProtectedInOrbit = appId in prefs.protectedHomeApps

        ModalBottomSheet(
            onDismissRequest = { selectedAppForAllowance = null },
            containerColor = Color(0xFF14141C),
            contentColor = Color.White,
            shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 22.dp)
                    .padding(bottom = 30.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        SocialAppIcon(
                            appId = appId,
                            iconTheme = prefs.iconTheme,
                            size = 40.dp,
                            cornerRadius = 11.dp
                        )
                        Column {
                            Text(
                                text = "$displayTitle Settings",
                                color = Color.White,
                                fontSize = 19.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Daily scrolling allowance & native protection",
                                color = Color(0xFF8E91A6),
                                fontSize = 12.sp
                            )
                        }
                    }
                    Text(
                        text = "Save",
                        color = Color(0xFF818CF8),
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.clickable {
                            scope.launch { prefsManager.setAppDailyLimitMinutes(appId, limitMinutes) }
                            selectedAppForAllowance = null
                        }
                    )
                }

                Spacer(modifier = Modifier.height(18.dp))

                // Orbit Protection toggle row
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(Color(0xFF1D1E2B))
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f).padding(end = 12.dp)) {
                        Text(
                            text = "Protect on Home Orbit",
                            color = Color.White,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = if (isProtectedInOrbit) {
                                "Active in Constellation Orbit"
                            } else {
                                "Currently in Unprotected Bank"
                            },
                            color = Color(0xFF8E91A6),
                            fontSize = 12.sp
                        )
                    }
                    Switch(
                        checked = isProtectedInOrbit,
                        onCheckedChange = { enabled ->
                            view.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
                            scope.launch {
                                prefsManager.setAppProtectedOnHome(appId, enabled)
                            }
                        },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.White,
                            checkedTrackColor = Color(0xFF4DB86B)
                        )
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "Daily scrolling allowance",
                    color = Color.White,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Set a daily short-form limit, or keep Reels/Shorts/Spotlight completely blocked (0 min).",
                    color = Color(0xFF8E91A6),
                    fontSize = 12.sp,
                    lineHeight = 17.sp
                )

                Spacer(modifier = Modifier.height(14.dp))

                // Presets: No limit (0), 5 min, 15 min, 30 min
                val presets = listOf(0 to "No limit", 5 to "5 min", 15 to "15 min", 30 to "30 min")
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    presets.forEach { (mins, label) ->
                        val isSelected = limitMinutes == mins
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(40.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(if (isSelected) Color(0xFF6366F1) else Color(0xFF222330))
                                .clickable {
                                    view.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
                                    limitMinutes = mins
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = label,
                                color = Color.White,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Custom minute stepper
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(Color(0xFF1D1E2B))
                        .padding(horizontal = 16.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = {
                        view.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
                        limitMinutes = (limitMinutes - 5).coerceAtLeast(0)
                    }) {
                        Icon(Icons.Filled.Remove, contentDescription = "Less", tint = Color.White)
                    }
                    Text(
                        text = if (limitMinutes == 0) "No daily limit" else "$limitMinutes min / day",
                        color = Color.White,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                    IconButton(onClick = {
                        view.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
                        limitMinutes = (limitMinutes + 5).coerceAtMost(180)
                    }) {
                        Icon(Icons.Filled.Add, contentDescription = "More", tint = Color.White)
                    }
                }

                Spacer(modifier = Modifier.height(18.dp))

                // Open inside SocialBreak button
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(Color(0xFF252738))
                        .clickable {
                            scope.launch { prefsManager.setAppDailyLimitMinutes(appId, limitMinutes) }
                            selectedAppForAllowance = null
                            onOpenCleanWebApp(appId)
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Open $displayTitle inside SocialBreak",
                        color = Color.White,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }
    }

    // Customize / Rearrange Apps Sheet (ng.f0 in oe_ng.java lines 17997-18730)
    if (showCustomizeAppsSheet) {
        val appLabels = mapOf(
            "instagram" to "Instagram",
            "youtube" to "YouTube",
            "tiktok" to "TikTok",
            "snapchat" to "Snapchat",
            "facebook" to "Facebook",
            "x" to "X",
            "linkedin" to "LinkedIn",
            "reddit" to "Reddit"
        )
        var tempOrder by remember(prefs.platformOrder) {
            val base = (prefs.platformOrder + SocialBreakPreferences.DEFAULT_PLATFORM_ORDER).distinct()
            mutableStateOf(base)
        }
        var tempHidden by remember(prefs.hiddenHomeApps) { mutableStateOf(prefs.hiddenHomeApps) }
        var draggingListAppId by remember { mutableStateOf<String?>(null) }
        var draggingListOffsetY by remember { mutableFloatStateOf(0f) }

        ModalBottomSheet(
            onDismissRequest = {
                scope.launch {
                    prefsManager.setPlatformOrder(tempOrder)
                    prefsManager.setHiddenHomeApps(tempHidden)
                }
                showCustomizeAppsSheet = false
            },
            containerColor = Color(0xFF14141C),
            contentColor = Color.White,
            shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 22.dp)
                    .padding(bottom = 28.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Customize Apps",
                            color = Color.White,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Drag or use arrows to reorder apps across Home & Inside SocialBreak. Tap the eye to hide.",
                            color = Color(0xFF8E91A6),
                            fontSize = 12.sp,
                            lineHeight = 16.sp
                        )
                    }
                    Text(
                        text = "Done",
                        color = Color(0xFF818CF8),
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier
                            .padding(start = 12.dp)
                            .clickable {
                                scope.launch {
                                    prefsManager.setPlatformOrder(tempOrder)
                                    prefsManager.setHiddenHomeApps(tempHidden)
                                }
                                showCustomizeAppsSheet = false
                            }
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                val rowHeightPx = with(density) { 62.dp.toPx() }
                val visibleCount = tempOrder.count { it !in tempHidden }

                tempOrder.forEachIndexed { index, appId ->
                    key(appId) {
                        val label = appLabels[appId] ?: appId.replaceFirstChar { it.uppercase() }
                        val isHidden = tempHidden.contains(appId)
                        val canToggleVisibility = isHidden || visibleCount > 1
                        val isDraggingRow = draggingListAppId == appId

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp)
                                .offset {
                                    IntOffset(
                                        x = 0,
                                        y = if (isDraggingRow) draggingListOffsetY.roundToInt() else 0
                                    )
                                }
                                .zIndex(if (isDraggingRow) 10f else 1f)
                                .graphicsLayer {
                                    scaleX = if (isDraggingRow) 1.03f else 1f
                                    scaleY = if (isDraggingRow) 1.03f else 1f
                                }
                                .clip(RoundedCornerShape(14.dp))
                                .background(if (isDraggingRow) Color(0xFF26283B) else Color(0xFF1D1E2B))
                                .border(
                                    width = if (isDraggingRow) 1.dp else 0.dp,
                                    color = if (isDraggingRow) Color(0xFF818CF8) else Color.Transparent,
                                    shape = RoundedCornerShape(14.dp)
                                )
                                .pointerInput(appId, tempOrder) {
                                    detectDragGestures(
                                        onDragStart = {
                                            view.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
                                            draggingListAppId = appId
                                            draggingListOffsetY = 0f
                                        },
                                        onDrag = { change, dragAmount ->
                                            change.consume()
                                            draggingListOffsetY += dragAmount.y
                                            val currentIndex = tempOrder.indexOf(appId)
                                            if (currentIndex != -1) {
                                                val steps = (draggingListOffsetY / rowHeightPx).roundToInt()
                                                val targetIndex = (currentIndex + steps).coerceIn(0, tempOrder.lastIndex)
                                                if (targetIndex != currentIndex) {
                                                    val mutable = tempOrder.toMutableList()
                                                    mutable.removeAt(currentIndex)
                                                    mutable.add(targetIndex, appId)
                                                    tempOrder = mutable
                                                    draggingListOffsetY -= (targetIndex - currentIndex) * rowHeightPx
                                                    view.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
                                                }
                                            }
                                        },
                                        onDragEnd = {
                                            draggingListAppId = null
                                            draggingListOffsetY = 0f
                                            scope.launch { prefsManager.setPlatformOrder(tempOrder) }
                                        },
                                        onDragCancel = {
                                            draggingListAppId = null
                                            draggingListOffsetY = 0f
                                        }
                                    )
                                }
                                .padding(horizontal = 12.dp, vertical = 9.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.DragIndicator,
                                    contentDescription = "Drag to reorder",
                                    tint = Color(0xFF6E7185),
                                    modifier = Modifier.size(20.dp)
                                )
                                SocialAppIcon(
                                    appId = appId,
                                    iconTheme = prefs.iconTheme,
                                    size = 34.dp,
                                    cornerRadius = 9.dp
                                )
                                Text(
                                    text = label,
                                    color = if (isHidden) Color(0xFF6E7185) else Color.White,
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }

                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(2.dp)
                            ) {
                                // Eye visibility toggle (prevents hiding last visible app, matching ng.f0)
                                IconButton(
                                    enabled = canToggleVisibility,
                                    onClick = {
                                        view.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
                                        tempHidden = if (isHidden) tempHidden - appId else tempHidden + appId
                                        scope.launch { prefsManager.setHiddenHomeApps(tempHidden) }
                                    },
                                    modifier = Modifier.size(34.dp)
                                ) {
                                    Icon(
                                        imageVector = if (isHidden) Icons.Outlined.VisibilityOff else Icons.Outlined.Visibility,
                                        contentDescription = "Toggle visibility",
                                        tint = if (!canToggleVisibility) {
                                            Color(0xFF3E4052)
                                        } else if (isHidden) {
                                            Color(0xFF6E7185)
                                        } else {
                                            Color(0xFF818CF8)
                                        },
                                        modifier = Modifier.size(19.dp)
                                    )
                                }

                                // Move Up arrow button (ng.f0 KeyboardArrowUp)
                                IconButton(
                                    enabled = index > 0,
                                    onClick = {
                                        if (index > 0) {
                                            view.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
                                            val mutable = tempOrder.toMutableList()
                                            val item = mutable.removeAt(index)
                                            mutable.add(index - 1, item)
                                            tempOrder = mutable
                                            scope.launch { prefsManager.setPlatformOrder(tempOrder) }
                                        }
                                    },
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.KeyboardArrowUp,
                                        contentDescription = "Move up",
                                        tint = if (index > 0) Color.White else Color(0xFF3E4052),
                                        modifier = Modifier.size(20.dp)
                                    )
                                }

                                // Move Down arrow button (ng.f0 KeyboardArrowDown)
                                IconButton(
                                    enabled = index < tempOrder.lastIndex,
                                    onClick = {
                                        if (index < tempOrder.lastIndex) {
                                            view.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
                                            val mutable = tempOrder.toMutableList()
                                            val item = mutable.removeAt(index)
                                            mutable.add(index + 1, item)
                                            tempOrder = mutable
                                            scope.launch { prefsManager.setPlatformOrder(tempOrder) }
                                        }
                                    },
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.KeyboardArrowDown,
                                        contentDescription = "Move down",
                                        tint = if (index < tempOrder.lastIndex) Color.White else Color(0xFF3E4052),
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showProtectionSheet) {
        ProtectionSetupBottomSheet(onDismiss = { showProtectionSheet = false })
    }

    if (showDisableHardModeGauntlet) {
        FrictionGauntletDialog(
            actionLabel = "Hard Mode",
            onDismiss = { showDisableHardModeGauntlet = false },
            onPassed = {
                showDisableHardModeGauntlet = false
                scope.launch { prefsManager.setHardMode(false, false) }
            }
        )
    }
}

@Composable
private fun HomeTodayActivityCard(
    prefs: SocialBreakPreferences,
    usageList: List<UsageStatsHelper.AppUsageEntry>,
    hourlyBuckets: List<Int>,
    showAllApps: Boolean,
    onToggleShowAll: () -> Unit,
    onOpenTodayStats: () -> Unit,
    onSelectApp: (String) -> Unit
) {
    val palette = SocialTheme.colors
    val todayDateStr = remember {
        SimpleDateFormat("EEEE, MMM d", Locale.US).format(Date())
    }
    val totalSocialMinutes = remember(usageList, prefs.inSocialBreakMinutesToday) {
        usageList.sumOf { it.minutesToday } + prefs.inSocialBreakMinutesToday
    }
    val totalAllowanceMinutes = remember(prefs.appDailyLimitsRaw, prefs.dailyGoalMinutes) {
        val explicitSum = SocialBreakPreferences.DEFAULT_CONSTELLATION_ORDER.sumOf {
            prefs.getAppDailyLimitMinutes(it)
        }
        if (explicitSum > 0) explicitSum else prefs.dailyGoalMinutes
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(22.dp))
            .background(palette.cardBackground)
            .border(0.5.dp, palette.border, RoundedCornerShape(22.dp))
            .padding(horizontal = 18.dp, vertical = 16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "Today",
                    color = palette.textPrimary,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = todayDateStr,
                    color = palette.textSecondary,
                    fontSize = 12.sp
                )
            }
            Text(
                text = "See all",
                color = Color(0xFF818CF8),
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.clickable { onOpenTodayStats() }
            )
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Big metric row: Short-form time used vs allowance + distractions blocked badge
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Bottom
        ) {
            Row(verticalAlignment = Alignment.Bottom) {
                Text(
                    text = "${totalSocialMinutes}m",
                    color = palette.textPrimary,
                    fontSize = 36.sp,
                    fontWeight = FontWeight.ExtraBold
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "/ ${totalAllowanceMinutes}m goal",
                    color = palette.textSecondary,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.padding(bottom = 5.dp)
                )
            }

            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(999.dp))
                    .background(Color(0xFF4DB86B).copy(alpha = 0.14f))
                    .padding(horizontal = 10.dp, vertical = 5.dp)
            ) {
                Text(
                    text = "${prefs.blocksLandedToday} blocked today",
                    color = Color(0xFF4DB86B),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // 24-hour mini activity bar chart (ng.t in oe_ng.java)
        val maxBucket = (hourlyBuckets.maxOrNull() ?: 1).coerceAtLeast(1)
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
        ) {
            val barCount = 24
            val gap = 3.dp.toPx()
            val barWidth = ((size.width - gap * (barCount - 1)) / barCount).coerceAtLeast(2f)
            hourlyBuckets.take(24).forEachIndexed { hr, mins ->
                val frac = (mins.toFloat() / maxBucket.toFloat()).coerceIn(0.08f, 1f)
                val barHeight = size.height * frac
                val x = hr * (barWidth + gap)
                drawRoundRect(
                    color = if (mins > 0) Color(0xFF818CF8) else Color(0xFF232536),
                    topLeft = Offset(x, size.height - barHeight),
                    size = Size(barWidth, barHeight),
                    cornerRadius = CornerRadius(3.dp.toPx(), 3.dp.toPx())
                )
            }
        }

        Spacer(modifier = Modifier.height(4.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text("12a", color = palette.textSecondary, fontSize = 10.sp)
            Text("6a", color = palette.textSecondary, fontSize = 10.sp)
            Text("12p", color = palette.textSecondary, fontSize = 10.sp)
            Text("6p", color = palette.textSecondary, fontSize = 10.sp)
            Text("Now", color = palette.textSecondary, fontSize = 10.sp)
        }

        Spacer(modifier = Modifier.height(14.dp))
        HorizontalDivider(color = palette.border, thickness = 0.5.dp)
        Spacer(modifier = Modifier.height(10.dp))

        val displayedApps = if (showAllApps) usageList else usageList.take(3)
        displayedApps.forEach { entry ->
            val limit = prefs.getAppDailyLimitMinutes(entry.id)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .clickable { onSelectApp(entry.id) }
                    .padding(vertical = 7.dp, horizontal = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    SocialAppIcon(
                        appId = entry.id,
                        iconTheme = prefs.iconTheme,
                        size = 30.dp,
                        cornerRadius = 8.dp
                    )
                    Column {
                        Text(
                            text = entry.name,
                            color = palette.textPrimary,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = if (entry.id in prefs.protectedHomeApps) "Protected in Orbit" else "Unprotected",
                            color = if (entry.id in prefs.protectedHomeApps) Color(0xFF4DB86B) else palette.textSecondary,
                            fontSize = 11.sp
                        )
                    }
                }
                Text(
                    text = if (limit > 0) "${entry.minutesToday}m / ${limit}m" else "${entry.minutesToday}m",
                    color = palette.textPrimary,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        if (usageList.size > 3) {
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = if (showAllApps) "Show fewer" else "Show all (${usageList.size})",
                color = Color(0xFF818CF8),
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier
                    .align(Alignment.CenterHorizontally)
                    .clickable { onToggleShowAll() }
                    .padding(vertical = 4.dp)
            )
        }
    }
}

@Composable
private fun ConfettiCelebrationOverlay(
    onFinished: () -> Unit
) {
    val particles = remember {
        val rng = Random(System.currentTimeMillis())
        val colors = listOf(
            Color(0xFF4DB86B),
            Color(0xFF818CF8),
            Color(0xFFF59E0B),
            Color(0xFFEC4899),
            Color(0xFF38BDF8),
            Color(0xFFA855F7)
        )
        List(70) {
            ConfettiParticle(
                startXFrac = 0.1f + rng.nextFloat() * 0.8f,
                vx = (rng.nextFloat() - 0.5f) * 420f,
                vy = 320f + rng.nextFloat() * 540f,
                sizeDp = 5f + rng.nextFloat() * 6f,
                color = colors[rng.nextInt(colors.size)],
                rotationSpeed = (rng.nextFloat() - 0.5f) * 720f,
                isCircle = rng.nextBoolean()
            )
        }
    }

    val progress = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        progress.animateTo(
            targetValue = 1f,
            animationSpec = tween(durationMillis = 2200, easing = LinearOutSlowInEasing)
        )
        onFinished()
    }

    Canvas(modifier = Modifier.fillMaxSize().zIndex(50f)) {
        val t = progress.value
        val alpha = (1f - (t - 0.65f).coerceAtLeast(0f) / 0.35f).coerceIn(0f, 1f)
        particles.forEach { p ->
            val x = size.width * p.startXFrac + p.vx * t
            val y = -20.dp.toPx() + p.vy * t + 0.5f * 680f * t * t
            val sz = p.sizeDp.dp.toPx()
            if (p.isCircle) {
                drawCircle(
                    color = p.color.copy(alpha = alpha),
                    radius = sz / 2f,
                    center = Offset(x, y)
                )
            } else {
                rotate(degrees = p.rotationSpeed * t, pivot = Offset(x, y)) {
                    drawRect(
                        color = p.color.copy(alpha = alpha),
                        topLeft = Offset(x - sz / 2f, y - sz / 2f),
                        size = Size(sz, sz * 0.65f)
                    )
                }
            }
        }
    }
}

@Composable
private fun ConnectedAppNode(
    appId: String,
    iconTheme: String,
    timerText: String,
    isDangerHighlight: Boolean = false,
    onClick: () -> Unit
) {
    val palette = SocialTheme.colors
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.clickable { onClick() }
    ) {
        Box(modifier = Modifier.size(66.dp)) {
            Box(
                modifier = Modifier
                    .size(58.dp)
                    .align(Alignment.Center)
                    .then(
                        if (isDangerHighlight) {
                            Modifier.border(
                                width = 2.5.dp,
                                color = Color(0xFFFF7070),
                                shape = RoundedCornerShape(16.dp)
                            )
                        } else {
                            Modifier
                        }
                    ),
                contentAlignment = Alignment.Center
            ) {
                SocialAppIcon(
                    appId = appId,
                    iconTheme = iconTheme,
                    size = 56.dp,
                    cornerRadius = 15.dp
                )
            }
            // Top-right 22.dp circular settings gear badge (c7.a in oe_c7.java)
            Box(
                modifier = Modifier
                    .size(22.dp)
                    .align(Alignment.TopEnd)
                    .clip(CircleShape)
                    .background(Color(0xFF232433))
                    .border(1.dp, Color(0xFF3E4056), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Outlined.Settings,
                    contentDescription = "Settings",
                    tint = Color(0xFFB4B6C8),
                    modifier = Modifier.size(12.dp)
                )
            }
        }
        Spacer(modifier = Modifier.height(3.dp))
        Text(
            text = timerText,
            color = if (isDangerHighlight) Color(0xFFFF7070) else palette.textSecondary,
            fontSize = 11.sp,
            fontWeight = FontWeight.Medium
        )
    }
}

@Composable
private fun DockAppItem(
    appId: String,
    iconTheme: String,
    limitText: String,
    onClick: () -> Unit,
    onProtectClick: () -> Unit = {}
) {
    val palette = SocialTheme.colors
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.clickable { onClick() }
    ) {
        Box(modifier = Modifier.size(58.dp)) {
            SocialAppIcon(
                appId = appId,
                iconTheme = iconTheme,
                size = 50.dp,
                cornerRadius = 14.dp,
                modifier = Modifier.align(Alignment.Center)
            )
            // Small "+" badge in top-right so tapping "+" or dragging up moves app into Orbit
            Box(
                modifier = Modifier
                    .size(20.dp)
                    .align(Alignment.TopEnd)
                    .clip(CircleShape)
                    .background(Color(0xFF232536))
                    .border(1.dp, Color(0xFF4DB86B), CircleShape)
                    .clickable { onProtectClick() },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Filled.Add,
                    contentDescription = "Protect in Orbit",
                    tint = Color(0xFF4DB86B),
                    modifier = Modifier.size(12.dp)
                )
            }
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = limitText,
            color = palette.textSecondary,
            fontSize = 11.sp,
            fontWeight = FontWeight.Medium
        )
    }
}
