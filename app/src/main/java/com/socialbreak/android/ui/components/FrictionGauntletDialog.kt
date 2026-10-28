package com.socialbreak.android.ui.components

import android.os.SystemClock
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.socialbreak.android.ui.theme.*
import kotlinx.coroutines.delay
import kotlin.random.Random

private const val COMMITMENT_PHRASE = "I am deliberately choosing to break my wellness commitment and waste my attention"

@Composable
fun FrictionGauntletDialog(
    onDismiss: () -> Unit,
    onSuccess: () -> Unit
) {
    var stage by remember { mutableIntStateOf(1) } // 1: Commitment, 2: Math (5 questions), 3: Cooldown, 4: Confirm
    var commitmentInput by remember { mutableStateOf("") }

    // Multi-operation Math questions state
    var mathCount by remember { mutableIntStateOf(0) }
    var numA by remember { mutableIntStateOf(Random.nextInt(6, 15)) }
    var numB by remember { mutableIntStateOf(Random.nextInt(4, 9)) }
    var numC by remember { mutableIntStateOf(Random.nextInt(7, 25)) }
    var expectedAnswer by remember { mutableIntStateOf((numA * numB) - numC) }
    var mathAnswerInput by remember { mutableStateOf("") }
    var mathError by remember { mutableStateOf(false) }

    // Monotonic Hardware Cooldown state
    var cooldownRemaining by remember { mutableIntStateOf(60) }
    var cooldownEndRealtime by remember { mutableLongStateOf(0L) }

    LaunchedEffect(stage) {
        if (stage == 3) {
            cooldownEndRealtime = SystemClock.elapsedRealtime() + 60000L
            while (true) {
                val remainingMs = cooldownEndRealtime - SystemClock.elapsedRealtime()
                if (remainingMs <= 0L) {
                    cooldownRemaining = 0
                    stage = 4
                    break
                }
                cooldownRemaining = (remainingMs / 1000L).toInt() + 1
                delay(250L)
            }
        }
    }

    Dialog(onDismissRequest = onDismiss) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(24.dp))
                .background(BgSecondary)
                .border(1.dp, BorderSubtle, RoundedCornerShape(24.dp))
                .padding(24.dp)
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "Hard Mode Gauntlet",
                    style = MaterialTheme.typography.headlineSmall,
                    color = AccentAmber,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Stage $stage of 4: Anti-Impulse Protection",
                    style = MaterialTheme.typography.labelSmall,
                    color = TextMuted,
                    modifier = Modifier.padding(top = 4.dp, bottom = 20.dp)
                )

                when (stage) {
                    1 -> {
                        Text(
                            text = "Type this exact sentence to confirm deliberate intent:",
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextSecondary
                        )
                        Text(
                            text = "\"$COMMITMENT_PHRASE\"",
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextHighlight,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.padding(vertical = 12.dp)
                        )
                        OutlinedTextField(
                            value = commitmentInput,
                            onValueChange = { commitmentInput = it },
                            modifier = Modifier.fillMaxWidth(),
                            placeholder = { Text("Type commitment phrase...", color = TextMuted) },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = AccentBlue,
                                unfocusedBorderColor = BorderSubtle,
                                focusedTextColor = TextPrimary,
                                unfocusedTextColor = TextPrimary
                            ),
                            shape = RoundedCornerShape(12.dp)
                        )
                        Spacer(modifier = Modifier.height(20.dp))
                        Button(
                            onClick = { stage = 2 },
                            enabled = commitmentInput.trim() == COMMITMENT_PHRASE,
                            modifier = Modifier.fillMaxWidth().height(48.dp),
                            shape = RoundedCornerShape(9999.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = AccentAmber)
                        ) {
                            Text("Next: Mental Clarity Check", color = BgPrimary, fontWeight = FontWeight.Bold)
                        }
                    }
                    2 -> {
                        Text(
                            text = "Solve 5 mental math questions (${mathCount}/5)",
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextSecondary
                        )
                        Text(
                            text = "($numA × $numB) - $numC = ?",
                            style = MaterialTheme.typography.headlineMedium,
                            color = TextHighlight,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(vertical = 16.dp)
                        )
                        OutlinedTextField(
                            value = mathAnswerInput,
                            onValueChange = {
                                mathAnswerInput = it
                                mathError = false
                            },
                            isError = mathError,
                            modifier = Modifier.fillMaxWidth(),
                            placeholder = { Text("Answer", color = TextMuted) },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = AccentBlue,
                                unfocusedBorderColor = BorderSubtle,
                                focusedTextColor = TextPrimary,
                                unfocusedTextColor = TextPrimary
                            ),
                            shape = RoundedCornerShape(12.dp)
                        )
                        if (mathError) {
                            Text(
                                text = "Incorrect. Try again.",
                                color = AccentCoral,
                                style = MaterialTheme.typography.bodySmall,
                                modifier = Modifier.padding(top = 4.dp)
                            )
                        }
                        Spacer(modifier = Modifier.height(20.dp))
                        Button(
                            onClick = {
                                val entered = mathAnswerInput.trim().toIntOrNull()
                                if (entered == expectedAnswer) {
                                    mathCount++
                                    mathAnswerInput = ""
                                    mathError = false
                                    if (mathCount >= 5) {
                                        stage = 3
                                    } else {
                                        numA = Random.nextInt(7, 18)
                                        numB = Random.nextInt(4, 9)
                                        numC = Random.nextInt(8, 30)
                                        expectedAnswer = (numA * numB) - numC
                                    }
                                } else {
                                    mathError = true
                                }
                            },
                            modifier = Modifier.fillMaxWidth().height(48.dp),
                            shape = RoundedCornerShape(9999.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = AccentBlue)
                        ) {
                            Text("Submit Answer", color = TextHighlight, fontWeight = FontWeight.Bold)
                        }
                    }
                    3 -> {
                        Text(
                            text = "Cooldown Period",
                            style = MaterialTheme.typography.bodyLarge,
                            color = TextSecondary
                        )
                        Text(
                            text = "${cooldownRemaining}s",
                            fontSize = 48.sp,
                            fontWeight = FontWeight.Bold,
                            color = AccentAmber,
                            modifier = Modifier.padding(vertical = 24.dp)
                        )
                        Text(
                            text = "Take a breath. Waiting out the impulse is part of breaking the dopamine loop. This cooldown is backed by your device's hardware clock.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextMuted,
                            modifier = Modifier.padding(bottom = 12.dp)
                        )
                    }
                    4 -> {
                        Text(
                            text = "Final Confirmation",
                            style = MaterialTheme.typography.headlineSmall,
                            color = AccentCoral,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Are you certain you want to turn off Hard Mode protection?",
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextSecondary,
                            modifier = Modifier.padding(vertical = 16.dp)
                        )
                        Button(
                            onClick = onSuccess,
                            modifier = Modifier.fillMaxWidth().height(48.dp),
                            shape = RoundedCornerShape(9999.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = AccentCoral)
                        ) {
                            Text("Disable Hard Mode", color = TextHighlight, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}
