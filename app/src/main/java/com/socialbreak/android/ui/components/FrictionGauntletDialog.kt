package com.socialbreak.android.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.socialbreak.android.ui.theme.*
import kotlinx.coroutines.delay
import kotlin.random.Random

@Composable
fun FrictionGauntletDialog(
    actionLabel: String,
    onDismiss: () -> Unit,
    onPassed: () -> Unit
) {
    var stage by remember { mutableIntStateOf(1) }
    val targetPhrase = "I turned this on when I was thinking straight. Nothing has changed since then except that I want to scroll."
    var typedText by remember { mutableStateOf("") }

    // Stage 2: 5 mental math problems (a wrong answer resets to Question 1)
    var mathCount by remember { mutableIntStateOf(0) }
    var numA by remember { mutableIntStateOf(Random.nextInt(14, 48)) }
    var numB by remember { mutableIntStateOf(Random.nextInt(12, 39)) }
    var mathInput by remember { mutableStateOf("") }
    var mathError by remember { mutableStateOf(false) }

    // Stage 3: 60-second unskippable pause (resets if user leaves app)
    var countdown by remember { mutableIntStateOf(60) }
    val lifecycleOwner = LocalLifecycleOwner.current

    DisposableEffect(lifecycleOwner, stage) {
        val observer = LifecycleEventObserver { _, event ->
            if (stage == 3 && event == Lifecycle.Event.ON_PAUSE) {
                countdown = 60
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    LaunchedEffect(stage, countdown) {
        if (stage == 3) {
            if (countdown > 0) {
                delay(1000L)
                countdown -= 1
            } else {
                stage = 4
            }
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            dismissOnBackPress = true,
            dismissOnClickOutside = false,
            usePlatformDefaultWidth = false
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .clip(RoundedCornerShape(24.dp))
                .background(Color(0xFF14141C))
                .border(1.dp, Color(0xFF282A3A), RoundedCornerShape(24.dp))
                .padding(24.dp)
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Step indicator pills
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    for (s in 1..4) {
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(4.dp)
                                .clip(RoundedCornerShape(999.dp))
                                .background(
                                    if (s <= stage) Color(0xFFEDA158) else Color(0xFF282A3A)
                                )
                        )
                    }
                }

                when (stage) {
                    1 -> {
                        Text(
                            text = "Type this out, word for word",
                            color = Color.White,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center
                        )
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(14.dp))
                                .background(Color(0xFF1D1E2B))
                                .padding(16.dp)
                        ) {
                            Text(
                                text = targetPhrase,
                                style = MaterialTheme.typography.bodyMedium,
                                color = Color(0xFFE5E7EB),
                                lineHeight = 20.sp
                            )
                        }
                        OutlinedTextField(
                            value = typedText,
                            onValueChange = { typedText = it },
                            placeholder = { Text("Start typing…", color = Color(0xFF6E7185)) },
                            modifier = Modifier.fillMaxWidth(),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White,
                                focusedBorderColor = Color(0xFFEDA158),
                                unfocusedBorderColor = Color(0xFF2E3042)
                            )
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            OutlinedButton(
                                onClick = onDismiss,
                                modifier = Modifier.weight(1f).height(48.dp),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Text("Keep blocking", color = Color.White)
                            }
                            Button(
                                onClick = { if (typedText.trim() == targetPhrase) stage = 2 },
                                enabled = typedText.trim() == targetPhrase,
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color(0xFFEDA158),
                                    contentColor = Color(0xFF1A140E)
                                ),
                                modifier = Modifier.weight(1f).height(48.dp),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Text("Continue", fontWeight = FontWeight.Bold)
                            }
                        }
                    }

                    2 -> {
                        Text(
                            text = "Five questions, no calculator",
                            color = Color.White,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center
                        )
                        Text(
                            text = "Question ${mathCount + 1} of 5. A wrong answer starts them over.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color(0xFF9CA3AF),
                            textAlign = TextAlign.Center
                        )
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(16.dp))
                                .background(Color(0xFF1D1E2B))
                                .padding(vertical = 20.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "$numA + $numB = ?",
                                fontSize = 32.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }
                        if (mathError) {
                            Text(
                                text = "Not right. Back to the first one.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = CrimsonDanger,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                        OutlinedTextField(
                            value = mathInput,
                            onValueChange = {
                                mathInput = it.filter { ch -> ch.isDigit() }
                                mathError = false
                            },
                            placeholder = { Text("Your answer", color = Color(0xFF6E7185)) },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White,
                                focusedBorderColor = Color(0xFFEDA158),
                                unfocusedBorderColor = Color(0xFF2E3042)
                            )
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            OutlinedButton(
                                onClick = onDismiss,
                                modifier = Modifier.weight(1f).height(48.dp),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Text("Keep blocking", color = Color.White)
                            }
                            Button(
                                onClick = {
                                    val ans = mathInput.trim().toIntOrNull()
                                    if (ans == numA + numB) {
                                        val nextCount = mathCount + 1
                                        if (nextCount >= 5) {
                                            stage = 3
                                        } else {
                                            mathCount = nextCount
                                            numA = Random.nextInt(14, 48)
                                            numB = Random.nextInt(12, 39)
                                            mathInput = ""
                                            mathError = false
                                        }
                                    } else {
                                        // Wrong answer resets back to Question 1
                                        mathCount = 0
                                        numA = Random.nextInt(14, 48)
                                        numB = Random.nextInt(12, 39)
                                        mathInput = ""
                                        mathError = true
                                    }
                                },
                                enabled = mathInput.isNotBlank(),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color(0xFFEDA158),
                                    contentColor = Color(0xFF1A140E)
                                ),
                                modifier = Modifier.weight(1f).height(48.dp),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Text("Submit", fontWeight = FontWeight.Bold)
                            }
                        }
                    }

                    3 -> {
                        Text(
                            text = "Wait a minute",
                            color = Color.White,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Leaving the app starts it over.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color(0xFF9CA3AF),
                            textAlign = TextAlign.Center
                        )
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(18.dp))
                                .background(Color(0xFF1D1E2B))
                                .padding(vertical = 26.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = "$countdown",
                                    fontSize = 48.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFFEDA158)
                                )
                                Text(
                                    text = "seconds left",
                                    color = Color(0xFF8E91A6),
                                    fontSize = 13.sp
                                )
                            }
                        }
                        OutlinedButton(
                            onClick = onDismiss,
                            modifier = Modifier.fillMaxWidth().height(48.dp),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text("Keep blocking", color = Color.White, fontWeight = FontWeight.SemiBold)
                        }
                    }

                    4 -> {
                        Text(
                            text = "This turns blocking off",
                            color = Color.White,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center
                        )
                        Text(
                            text = "If you continue, $actionLabel turns off and stays off until you turn it back on yourself.",
                            style = MaterialTheme.typography.bodyLarge,
                            color = Color(0xFF9CA3AF),
                            textAlign = TextAlign.Center
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Button(
                                onClick = onDismiss,
                                colors = ButtonDefaults.buttonColors(containerColor = EmeraldSuccess),
                                modifier = Modifier.weight(1f).height(50.dp),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Text("Keep blocking", color = Color.Black, fontWeight = FontWeight.Bold)
                            }
                            OutlinedButton(
                                onClick = onPassed,
                                modifier = Modifier.weight(1f).height(50.dp),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Text("Turn blocking off", color = CrimsonDanger, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }
    }
}
