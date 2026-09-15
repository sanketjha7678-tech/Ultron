package com.ultron.ai.ui

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ultron.ai.domain.HudMessage
import com.ultron.ai.domain.UltronState

@Composable
fun UltronHudScreen(
    currentState: UltronState,
    hudMessages: List<HudMessage>,
    rmsLevel: Float,
    onMicClick: () -> Unit,
    onVisionClick: () -> Unit
) {
    val darkBackground = Color(0xFF0A0C10)
    val graphiteSurface = Color(0xFF141820)
    val cyanAccent = Color(0xFF00F0FF)
    val warningRed = Color(0xFFFF2A55)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(darkBackground)
            .padding(16.dp)
    ) {
        // Top HUD Status Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 24.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "ULTRON OS v1.0",
                    color = cyanAccent,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 2.sp
                )
                Text(
                    text = "STATUS: ${currentState.name}",
                    color = if (currentState == UltronState.ERROR) warningRed else Color.White.copy(alpha = 0.7f),
                    fontSize = 10.sp,
                    letterSpacing = 1.sp
                )
            }
            Box(
                modifier = Modifier
                    .size(10.dp)
                    .clip(CircleShape)
                    .background(if (currentState == UltronState.ERROR) warningRed else cyanAccent)
            )
        }

        // Central AI Core Visualizer
        Box(
            modifier = Modifier
                .size(320.dp)
                .align(Alignment.Center)
        ) {
            UltronCoreCanvas(currentState = currentState, rmsLevel = rmsLevel, cyanAccent = cyanAccent)
        }

        // Translucent Command Overlay
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter)
                .padding(bottom = 80.dp)
        ) {
            hudMessages.takeLast(2).forEach { msg ->
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp)
                        .border(1.dp, cyanAccent.copy(alpha = 0.3f), RoundedCornerShape(12.dp)),
                    color = graphiteSurface.copy(alpha = 0.65f),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(
                            text = if (msg.isUser) "COMMAND RECEIVED" else msg.title,
                            color = cyanAccent,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.SemiBold,
                            letterSpacing = 1.sp
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = msg.detail,
                            color = Color.White,
                            fontSize = 14.sp
                        )
                    }
                }
            }
        }

        // Bottom Controls
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter)
                .padding(bottom = 8.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Button(
                onClick = onVisionClick,
                colors = ButtonDefaults.buttonColors(containerColor = graphiteSurface),
                modifier = Modifier.border(1.dp, cyanAccent.copy(alpha = 0.4f), RoundedCornerShape(20.dp))
            ) {
                Text("VISION", color = Color.White, fontSize = 12.sp)
            }

            IconButton(
                onClick = onMicClick,
                modifier = Modifier
                    .size(64.dp)
                    .clip(CircleShape)
                    .background(
                        Brush.radialGradient(
                            colors = listOf(cyanAccent, graphiteSurface)
                        )
                    )
            ) {
                Text("MIC", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 12.sp)
            }

            Button(
                onClick = { },
                colors = ButtonDefaults.buttonColors(containerColor = graphiteSurface),
                modifier = Modifier.border(1.dp, cyanAccent.copy(alpha = 0.4f), RoundedCornerShape(20.dp))
            ) {
                Text("SETTINGS", color = Color.White, fontSize = 12.sp)
            }
        }
    }
}

@Composable
fun UltronCoreCanvas(currentState: UltronState, rmsLevel: Float, cyanAccent: Color) {
    val infiniteTransition = rememberInfiniteTransition(label = "CoreRotation")
    val rotationAngle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = when (currentState) {
                    UltronState.THINKING -> 1200
                    UltronState.EXECUTING -> 2000
                    else -> 6000
                },
                easing = LinearEasing
            ),
            repeatMode = RepeatMode.Restart
        ),
        label = "Rotation"
    )

    Canvas(modifier = Modifier.fillMaxSize()) {
        val centerPoint = center
        val baseRadius = size.minDimension / 3f
        val pulseAddition = if (currentState == UltronState.LISTENING) rmsLevel * 4f else 0f

        rotate(rotationAngle) {
            drawCircle(
                color = cyanAccent.copy(alpha = 0.4f),
                radius = baseRadius + 20.dp.toPx() + pulseAddition,
                style = Stroke(width = 3.dp.toPx())
            )
            drawArc(
                color = cyanAccent,
                startAngle = 0f,
                sweepAngle = 90f,
                useCenter = false,
                style = Stroke(width = 6.dp.toPx())
            )
        }

        rotate(-rotationAngle * 1.5f) {
            drawCircle(
                color = Color.White.copy(alpha = 0.2f),
                radius = baseRadius - 15.dp.toPx(),
                style = Stroke(width = 2.dp.toPx())
            )
            drawArc(
                color = if (currentState == UltronState.ERROR) Color.Red else cyanAccent,
                startAngle = 180f,
                sweepAngle = 60f,
                useCenter = false,
                style = Stroke(width = 4.dp.toPx())
            )
        }

        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(cyanAccent.copy(alpha = 0.8f), Color.Transparent),
                center = centerPoint,
                radius = baseRadius
            ),
            radius = baseRadius / 2f + pulseAddition
        )
    }
}
