package com.socialbreak.android.ui.components

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

private const val COMMITMENT_PHRASE = "I am breaking my digital wellness commitment"

@Composable
fun FrictionGauntletDialog(
    onDismiss: () -> Unit,
    onSuccess: () -> Unit
) {
    var stage by remember { mutableIntStateOf(1) } // 1: Commitment, 2: Math (5 questions), 3: Cooldown, 4: Confirm
    var commitmentInput by remember { mutableStateOf("") }
    
    // Math questions state
    var mathCount by remember { mutableIntStateOf(0) }
    var numA by remember { mutableIntStateOf(Random.nextInt(12, 49)) }
    var numB by remember { mutableIntStateOf(Random.nextInt(11, 38)) }
    var mathAnswerInput by remember { mutableStateOf("") }

    // Cooldown state
    var cooldownSeconds by remember { mutableIntStateOf(60) }

    LaunchedEffect(stage) {
        if (stage == 3) {
            while (cooldownSeconds > 0) {
                delay(1000L)
                cooldownSeconds--
            }
            stage = 4
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
                    color = AccentAmber
                )
                Text(
                    text = "Stage $stage of 4: Anti-Impulse Protection",
                    style = MaterialTheme.typography.labelSmall,
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
                            text = "$numA + $numB = ?",
                            style = MaterialTheme.typography.headlineMedium,
                            color = TextHighlight,
                            modifier = Modifier.padding(vertical = 16.dp)
                        )
                        OutlinedTextField(
                            value = mathAnswerInput,
                            onValueChange = { mathAnswerInput = it },
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
                        Spacer(modifier = Modifier.height(20.dp))
                        Button(
                            onClick = {
                                if (mathAnswerInput.trim().toIntOrNull() == (numA + numB)) {
                                    mathCount++
                                    mathAnswerInput = ""
                                    numA = Random.nextInt(15, 65)
                                    numB = Random.nextInt(12, 45)
                                    if (mathCount >= 5) {
                                        stage = 3
                                    }
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
                            text = "${cooldownSeconds}s",
                            fontSize = 48.sp,
                            fontWeight = FontWeight.Bold,
                            color = AccentAmber,
                            modifier = Modifier.padding(vertical = 24.dp)
                        )
                        Text(
                            text = "Take a breath. Waiting out the impulse is part of breaking the dopamine loop.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextMuted,
                            modifier = Modifier.padding(bottom = 12.dp)
                        )
                    }
                    4 -> {
                        Text(
                            text = "Final Confirmation",
                            style = MaterialTheme.typography.headlineSmall,
                            color = AccentCoral
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
