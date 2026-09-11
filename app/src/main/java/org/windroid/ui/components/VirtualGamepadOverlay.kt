package org.windroid.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * High-performance on-screen virtual touch controller overlay for WinDroid.
 * Features customizable opacity, analog stick tracking, D-Pad, and ABXY buttons.
 */
@Composable
fun VirtualGamepadOverlay(
    modifier: Modifier = Modifier,
    opacity: Float = 0.65f,
    onButtonEvent: (button: String, isDown: Boolean) -> Unit = { _, _ -> },
    onLeftStickMove: (x: Float, y: Float) -> Unit = { _, _ -> },
    onRightStickMove: (x: Float, y: Float) -> Unit = { _, _ -> }
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .alpha(opacity)
    ) {
        // Left Side: D-Pad & Left Analog Stick
        Column(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(start = 24.dp, bottom = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            VirtualJoystick(
                radiusDp = 64,
                onStickMove = onLeftStickMove
            )
            Spacer(modifier = Modifier.height(16.dp))
            VirtualDPad(onButtonEvent = onButtonEvent)
        }

        // Top Triggers: L1/L2 and R1/R2
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 32.dp, vertical = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                TriggerButton("L2", onButtonEvent)
                TriggerButton("L1", onButtonEvent)
            }
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                TriggerButton("R1", onButtonEvent)
                TriggerButton("R2", onButtonEvent)
            }
        }

        // Right Side: Action Buttons (ABXY) & Right Analog Stick
        Column(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = 24.dp, bottom = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            ActionButtonsCluster(onButtonEvent = onButtonEvent)
            Spacer(modifier = Modifier.height(16.dp))
            VirtualJoystick(
                radiusDp = 64,
                onStickMove = onRightStickMove
            )
        }
    }
}

@Composable
fun VirtualJoystick(
    radiusDp: Int = 64,
    onStickMove: (Float, Float) -> Unit
) {
    var thumbOffset by remember { mutableStateOf(Offset.Zero) }
    val maxRadiusPx = radiusDp * 2.5f

    Box(
        modifier = Modifier
            .size((radiusDp * 2).dp)
            .pointerInput(Unit) {
                detectDragGestures(
                    onDragStart = { offset ->
                        val center = Offset(size.width / 2f, size.height / 2f)
                        val delta = offset - center
                        val dist = sqrt(delta.x * delta.x + delta.y * delta.y)
                        val clampedDist = minOf(dist, maxRadiusPx)
                        val angle = atan2(delta.y, delta.x)
                        thumbOffset = Offset(cos(angle) * clampedDist, sin(angle) * clampedDist)
                        onStickMove(thumbOffset.x / maxRadiusPx, thumbOffset.y / maxRadiusPx)
                    },
                    onDrag = { change, dragAmount ->
                        change.consume()
                        val newPos = thumbOffset + dragAmount
                        val dist = sqrt(newPos.x * newPos.x + newPos.y * newPos.y)
                        val clampedDist = minOf(dist, maxRadiusPx)
                        val angle = atan2(newPos.y, newPos.x)
                        thumbOffset = Offset(cos(angle) * clampedDist, sin(angle) * clampedDist)
                        onStickMove(thumbOffset.x / maxRadiusPx, thumbOffset.y / maxRadiusPx)
                    },
                    onDragEnd = {
                        thumbOffset = Offset.Zero
                        onStickMove(0f, 0f)
                    },
                    onDragCancel = {
                        thumbOffset = Offset.Zero
                        onStickMove(0f, 0f)
                    }
                )
            },
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val center = Offset(size.width / 2f, size.height / 2f)
            // Outer Ring
            drawCircle(
                color = Color(0xFF64B5F6).copy(alpha = 0.4f),
                radius = size.width / 2f,
                center = center,
                style = Stroke(width = 3.dp.toPx())
            )
            // Inner Stick Thumb
            drawCircle(
                color = Color(0xFF00E5FF).copy(alpha = 0.8f),
                radius = 24.dp.toPx(),
                center = center + thumbOffset
            )
        }
    }
}

@Composable
fun VirtualDPad(onButtonEvent: (String, Boolean) -> Unit) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        PadButton("▲", "DPAD_UP", onButtonEvent)
        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            PadButton("◀", "DPAD_LEFT", onButtonEvent)
            Box(modifier = Modifier.size(36.dp)) // Center spacer
            PadButton("▶", "DPAD_RIGHT", onButtonEvent)
        }
        PadButton("▼", "DPAD_DOWN", onButtonEvent)
    }
}

@Composable
fun ActionButtonsCluster(onButtonEvent: (String, Boolean) -> Unit) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        PadButton("Y", "BTN_Y", onButtonEvent, Color(0xFFFFD54F))
        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            PadButton("X", "BTN_X", onButtonEvent, Color(0xFF42A5F5))
            Box(modifier = Modifier.size(36.dp))
            PadButton("B", "BTN_B", onButtonEvent, Color(0xFFEF5350))
        }
        PadButton("A", "BTN_A", onButtonEvent, Color(0xFF66BB6A))
    }
}

@Composable
fun PadButton(
    label: String,
    code: String,
    onEvent: (String, Boolean) -> Unit,
    accentColor: Color = Color.White
) {
    Box(
        modifier = Modifier
            .size(44.dp)
            .background(Color(0xFF212121).copy(alpha = 0.8f), shape = CircleShape)
            .pointerInput(code) {
                awaitPointerEventScope {
                    while (true) {
                        val event = awaitPointerEvent()
                        val isPressed = event.changes.any { it.pressed }
                        onEvent(code, isPressed)
                    }
                }
            },
        contentAlignment = Alignment.Center
    ) {
        Text(
            label,
            color = accentColor,
            fontWeight = FontWeight.Bold,
            fontSize = 16.sp
        )
    }
}

@Composable
fun TriggerButton(
    label: String,
    onEvent: (String, Boolean) -> Unit
) {
    Box(
        modifier = Modifier
            .size(width = 60.dp, height = 36.dp)
            .background(Color(0xFF2C2C2C).copy(alpha = 0.85f), shape = CircleShape)
            .pointerInput(label) {
                awaitPointerEventScope {
                    while (true) {
                        val event = awaitPointerEvent()
                        onEvent(label, event.changes.any { it.pressed })
                    }
                }
            },
        contentAlignment = Alignment.Center
    ) {
        Text(label, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
    }
}
