package com.socialbreak.android.services

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.socialbreak.android.MainActivity
import com.socialbreak.android.ui.theme.*

class BlockedAppActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val platform = intent.getStringExtra("platform") ?: "app"
        val reason = intent.getStringExtra("reason") ?: "sleep_block"

        val platformDisplayName = when (platform.lowercase()) {
            "instagram" -> "Instagram"
            "youtube" -> "YouTube"
            "tiktok" -> "TikTok"
            "snapchat" -> "Snapchat"
            "twitter", "x" -> "X"
            "facebook" -> "Facebook"
            "linkedin" -> "LinkedIn"
            else -> platform.replaceFirstChar { it.uppercase() }
        }

        val reasonText = when (reason) {
            "sleep_block" -> "Sleep Mode is Active"
            "focus_block" -> "Full App Blocking Active"
            "limit_block" -> "Daily App Limit Reached"
            else -> "Paused by SocialLite"
        }

        setContent {
            SocialBreakTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = PitchBlack
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(28.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .size(96.dp)
                                .clip(CircleShape)
                                .background(SlateCardSurface)
                                .border(1.5.dp, SlateBorder, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Lock,
                                contentDescription = null,
                                tint = AmberGold,
                                modifier = Modifier.size(44.dp)
                            )
                        }

                        Spacer(modifier = Modifier.height(24.dp))

                        Text(
                            text = "$platformDisplayName is Paused",
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary,
                            textAlign = TextAlign.Center
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        Surface(
                            shape = RoundedCornerShape(9999.dp),
                            color = SlateCardElevated,
                            border = BorderStroke(1.dp, SlateBorder)
                        ) {
                            Text(
                                text = reasonText,
                                style = MaterialTheme.typography.labelMedium,
                                color = AmberGold,
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)
                            )
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        Text(
                            text = "SocialLite is protecting your attention. You can still use the distraction-free WebApp inside SocialLite to check messages without infinite Reels or Shorts.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextSecondary,
                            textAlign = TextAlign.Center,
                            lineHeight = 22.sp
                        )

                        Spacer(modifier = Modifier.height(36.dp))

                        Button(
                            onClick = {
                                val openIntent = Intent(this@BlockedAppActivity, MainActivity::class.java).apply {
                                    action = Intent.ACTION_VIEW
                                    data = Uri.parse("socialbreak://open?platform=$platform")
                                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                                }
                                startActivity(openIntent)
                                finish()
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(52.dp),
                            shape = RoundedCornerShape(9999.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = IndigoActive)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "Open in SocialLite",
                                    fontWeight = FontWeight.SemiBold,
                                    color = Color.White
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Icon(
                                    Icons.AutoMirrored.Filled.ArrowForward,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        OutlinedButton(
                            onClick = {
                                val homeIntent = Intent(Intent.ACTION_MAIN).apply {
                                    addCategory(Intent.CATEGORY_HOME)
                                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                                }
                                startActivity(homeIntent)
                                finish()
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(50.dp),
                            shape = RoundedCornerShape(9999.dp),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = TextPrimary),
                            border = BorderStroke(1.dp, SlateBorder)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    Icons.Default.Home,
                                    contentDescription = null,
                                    tint = TextMuted,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Return to Home Screen",
                                    color = TextPrimary,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
