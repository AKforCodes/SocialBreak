package com.socialbreak.android.ui.components

import android.app.AppOpsManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Process
import android.provider.Settings
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material.icons.outlined.Shield
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.socialbreak.android.R

@Composable
fun SocialAppIcon(
    appId: String,
    iconTheme: String,
    size: Dp = 56.dp,
    cornerRadius: Dp = 15.dp,
    modifier: Modifier = Modifier
) {
    val cleanId = appId.lowercase()
    val drawableRes: Int? = when (iconTheme.lowercase()) {
        "classic" -> when (cleanId) {
            "instagram" -> R.drawable.ic_instagram_classic
            "youtube" -> R.drawable.ic_youtube_classic
            "tiktok" -> R.drawable.ic_tiktok_classic
            "snapchat" -> R.drawable.ic_snapchat_classic
            "facebook" -> R.drawable.ic_facebook_classic
            "twitter", "x" -> R.drawable.ic_twitter_classic
            "linkedin" -> R.drawable.ic_linkedin_classic
            else -> null
        }
        "painted" -> when (cleanId) {
            "instagram" -> R.drawable.ic_instagram_painted
            "youtube" -> R.drawable.ic_youtube_painted
            "tiktok" -> R.drawable.ic_tiktok_painted
            "snapchat" -> R.drawable.ic_snapchat_painted
            "facebook" -> R.drawable.ic_facebook_painted
            "twitter", "x" -> R.drawable.ic_twitter_painted
            "linkedin" -> R.drawable.ic_linkedin_painted
            else -> null
        }
        else -> null // "modern" uses sleek vector/gradient tile
    }

    if (drawableRes != null) {
        Image(
            painter = painterResource(id = drawableRes),
            contentDescription = appId,
            contentScale = ContentScale.Crop,
            modifier = modifier
                .size(size)
                .clip(RoundedCornerShape(cornerRadius))
        )
    } else {
        // Modern / fallback stylized app tile
        val bgBrush = when (cleanId) {
            "instagram" -> Brush.linearGradient(
                listOf(Color(0xFF833AB4), Color(0xFFFD1D1D), Color(0xFFFCAF45))
            )
            "youtube" -> Brush.linearGradient(
                listOf(Color(0xFFFF0000), Color(0xFFB31217))
            )
            "tiktok" -> Brush.linearGradient(
                listOf(Color(0xFF0F0F14), Color(0xFF1F2029))
            )
            "snapchat" -> Brush.linearGradient(
                listOf(Color(0xFFFFFC00), Color(0xFFE6C200))
            )
            "facebook" -> Brush.linearGradient(
                listOf(Color(0xFF1877F2), Color(0xFF0D5BD9))
            )
            "twitter" -> Brush.linearGradient(
                listOf(Color(0xFF1DA1F2), Color(0xFF0C85D0))
            )
            "x" -> Brush.linearGradient(
                listOf(Color(0xFF14141C), Color(0xFF232433))
            )
            "reddit" -> Brush.linearGradient(
                listOf(Color(0xFFFF4500), Color(0xFFD93A00))
            )
            "threads" -> Brush.linearGradient(
                listOf(Color(0xFF181820), Color(0xFF2A2B38))
            )
            "pinterest" -> Brush.linearGradient(
                listOf(Color(0xFFE60023), Color(0xFFAD081B))
            )
            "linkedin" -> Brush.linearGradient(
                listOf(Color(0xFF0A66C2), Color(0xFF004182))
            )
            "discord" -> Brush.linearGradient(
                listOf(Color(0xFF5865F2), Color(0xFF404EED))
            )
            "messenger" -> Brush.linearGradient(
                listOf(Color(0xFF00B2FF), Color(0xFF006AFF))
            )
            "bereal" -> Brush.linearGradient(
                listOf(Color(0xFF1A1A22), Color(0xFF2E2F3E))
            )
            "vsco" -> Brush.linearGradient(
                listOf(Color(0xFF22232E), Color(0xFF343646))
            )
            "tumblr" -> Brush.linearGradient(
                listOf(Color(0xFF36465D), Color(0xFF222E3F))
            )
            else -> Brush.linearGradient(
                listOf(Color(0xFF282A3A), Color(0xFF1C1D29))
            )
        }

        Box(
            modifier = modifier
                .size(size)
                .clip(RoundedCornerShape(cornerRadius))
                .background(bgBrush)
                .border(0.75.dp, Color.White.copy(alpha = 0.14f), RoundedCornerShape(cornerRadius)),
            contentAlignment = Alignment.Center
        ) {
            when (cleanId) {
                "instagram" -> {
                    Canvas(modifier = Modifier.size(size * 0.52f)) {
                        drawRoundRect(
                            color = Color.White,
                            cornerRadius = CornerRadius(size.toPx() * 0.14f),
                            style = Stroke(width = size.toPx() * 0.055f)
                        )
                        drawCircle(
                            color = Color.White,
                            radius = size.toPx() * 0.12f,
                            style = Stroke(width = size.toPx() * 0.055f)
                        )
                        drawCircle(
                            color = Color.White,
                            radius = size.toPx() * 0.03f,
                            center = Offset(this.size.width * 0.76f, this.size.height * 0.24f)
                        )
                    }
                }
                "youtube" -> {
                    Canvas(modifier = Modifier.size(size * 0.45f)) {
                        val path = Path().apply {
                            moveTo(this@Canvas.size.width * 0.25f, this@Canvas.size.height * 0.18f)
                            lineTo(this@Canvas.size.width * 0.82f, this@Canvas.size.height * 0.5f)
                            lineTo(this@Canvas.size.width * 0.25f, this@Canvas.size.height * 0.82f)
                            close()
                        }
                        drawPath(path, Color.White)
                    }
                }
                else -> {
                    val textColor = if (cleanId == "snapchat") Color(0xFF111118) else Color.White
                    val glyph = when (cleanId) {
                        "tiktok" -> "♪"
                        "snapchat" -> "👻"
                        "facebook" -> "f"
                        "twitter" -> "🐦"
                        "x" -> "𝕏"
                        "threads" -> "@"
                        "linkedin" -> "in"
                        "bereal" -> "Be."
                        else -> appId.take(1).uppercase()
                    }
                    Text(
                        text = glyph,
                        color = textColor,
                        fontWeight = FontWeight.Bold,
                        fontSize = (size.value * 0.40f).sp
                    )
                }
            }
        }
    }
}

@Composable
fun SocialBreakRibbonIcon(
    size: Dp = 24.dp,
    tint: Color = Color.White,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier.size(size)) {
        val w = this.size.width
        val h = this.size.height
        val strokeWidth = w * 0.125f

        // Upper ribbon loop of the stylized 'S'
        val upperPath = Path().apply {
            moveTo(w * 0.74f, h * 0.24f)
            cubicTo(
                w * 0.42f, h * 0.08f,
                w * 0.16f, h * 0.26f,
                w * 0.30f, h * 0.46f
            )
            lineTo(w * 0.68f, h * 0.62f)
        }
        // Lower ribbon loop of the stylized 'S'
        val lowerPath = Path().apply {
            moveTo(w * 0.32f, h * 0.38f)
            lineTo(w * 0.70f, h * 0.54f)
            cubicTo(
                w * 0.84f, h * 0.74f,
                w * 0.58f, h * 0.92f,
                w * 0.26f, h * 0.76f
            )
        }

        drawPath(upperPath, color = tint, style = Stroke(width = strokeWidth, cap = StrokeCap.Round))
        drawPath(lowerPath, color = tint, style = Stroke(width = strokeWidth, cap = StrokeCap.Round))
    }
}

object ProtectionPermissionHelper {
    fun isAccessibilityEnabled(context: Context): Boolean {
        return try {
            val enabledServices = Settings.Secure.getString(
                context.contentResolver,
                Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
            ) ?: return false
            enabledServices.contains(context.packageName, ignoreCase = true)
        } catch (e: Exception) {
            false
        }
    }

    fun isUsageAccessGranted(context: Context): Boolean {
        return try {
            val appOps = context.getSystemService(Context.APP_OPS_SERVICE) as AppOpsManager
            val mode = appOps.checkOpNoThrow(
                AppOpsManager.OPSTR_GET_USAGE_STATS,
                Process.myUid(),
                context.packageName
            )
            mode == AppOpsManager.MODE_ALLOWED
        } catch (e: Exception) {
            false
        }
    }

    fun isNotificationListenerEnabled(context: Context): Boolean {
        return try {
            val flat = Settings.Secure.getString(
                context.contentResolver,
                "enabled_notification_listeners"
            ) ?: return false
            flat.contains(context.packageName, ignoreCase = true)
        } catch (e: Exception) {
            false
        }
    }

    fun isOverlayGranted(context: Context): Boolean {
        return Settings.canDrawOverlays(context)
    }

    fun areAllProtectionsActive(context: Context): Boolean {
        return isAccessibilityEnabled(context) &&
            isUsageAccessGranted(context) &&
            isNotificationListenerEnabled(context)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProtectionSetupBottomSheet(
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    var a11yGranted by remember { mutableStateOf(ProtectionPermissionHelper.isAccessibilityEnabled(context)) }
    var overlayGranted by remember { mutableStateOf(ProtectionPermissionHelper.isOverlayGranted(context)) }
    var notifGranted by remember { mutableStateOf(ProtectionPermissionHelper.isNotificationListenerEnabled(context)) }
    var usageGranted by remember { mutableStateOf(ProtectionPermissionHelper.isUsageAccessGranted(context)) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = Color(0xFF14141C),
        contentColor = Color.White,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 22.dp)
                .padding(bottom = 32.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Icon(
                    imageVector = Icons.Outlined.Shield,
                    contentDescription = null,
                    tint = Color(0xFFF59E0B),
                    modifier = Modifier.size(24.dp)
                )
                Text(
                    text = "Finish protection setup",
                    color = Color.White,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold
                )
            }
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "Grant these on-device permissions so SocialBreak can enforce Sleep Mode, Full App Blocking, and Reels/Shorts stripping reliably.",
                color = Color(0xFF9CA3AF),
                fontSize = 13.sp,
                lineHeight = 19.sp
            )

            Spacer(modifier = Modifier.height(18.dp))

            PermissionSetupRow(
                title = "App and sleep blocking",
                subtitle = "Accessibility service to lock blocked apps & strip Reels/Shorts",
                granted = a11yGranted,
                onClick = {
                    try {
                        context.startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS).apply {
                            flags = Intent.FLAG_ACTIVITY_NEW_TASK
                        })
                    } catch (_: Exception) {}
                }
            )
            Spacer(modifier = Modifier.height(10.dp))

            PermissionSetupRow(
                title = "Display over other apps",
                subtitle = "Required so SocialBreak can show the friendly stop overlay",
                granted = overlayGranted,
                onClick = {
                    try {
                        context.startActivity(
                            Intent(
                                Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                                Uri.parse("package:${context.packageName}")
                            ).apply { flags = Intent.FLAG_ACTIVITY_NEW_TASK }
                        )
                    } catch (_: Exception) {}
                }
            )
            Spacer(modifier = Modifier.height(10.dp))

            PermissionSetupRow(
                title = "Notification access",
                subtitle = "Redirects social media notifications into clean SocialBreak tabs",
                granted = notifGranted,
                onClick = {
                    try {
                        context.startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS).apply {
                            flags = Intent.FLAG_ACTIVITY_NEW_TASK
                        })
                    } catch (_: Exception) {}
                }
            )
            Spacer(modifier = Modifier.height(10.dp))

            PermissionSetupRow(
                title = "Approve Usage Access",
                subtitle = "Reads social app screen time on this phone only",
                granted = usageGranted,
                onClick = {
                    try {
                        context.startActivity(Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS).apply {
                            flags = Intent.FLAG_ACTIVITY_NEW_TASK
                        })
                    } catch (_: Exception) {}
                }
            )

            Spacer(modifier = Modifier.height(22.dp))

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(Color(0xFF6366F1))
                    .clickable {
                        a11yGranted = ProtectionPermissionHelper.isAccessibilityEnabled(context)
                        overlayGranted = ProtectionPermissionHelper.isOverlayGranted(context)
                        notifGranted = ProtectionPermissionHelper.isNotificationListenerEnabled(context)
                        usageGranted = ProtectionPermissionHelper.isUsageAccessGranted(context)
                        onDismiss()
                    },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Done",
                    color = Color.White,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
private fun PermissionSetupRow(
    title: String,
    subtitle: String,
    granted: Boolean,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(Color(0xFF1D1E2B))
            .clickable { onClick() }
            .padding(horizontal = 16.dp, vertical = 14.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f).padding(end = 12.dp)) {
            Text(
                text = title,
                color = Color.White,
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = subtitle,
                color = Color(0xFF8E91A6),
                fontSize = 12.sp,
                lineHeight = 16.sp
            )
        }
        if (granted) {
            Icon(
                imageVector = Icons.Outlined.CheckCircle,
                contentDescription = "Granted",
                tint = Color(0xFF22C55E),
                modifier = Modifier.size(22.dp)
            )
        } else {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "Enable",
                    color = Color(0xFFF59E0B),
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Icon(
                    imageVector = Icons.Outlined.ChevronRight,
                    contentDescription = null,
                    tint = Color(0xFFF59E0B),
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}

