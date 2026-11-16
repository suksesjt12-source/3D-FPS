package com.example.game.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessibilityNew
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.automirrored.filled.DirectionsRun
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Radar
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Sensors
import androidx.compose.material.icons.filled.TouchApp
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.PointerId
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.game.model.GameSettings
import com.example.game.model.InteractableRuntime
import com.example.game.model.PlayerState
import com.example.ui.theme.ElectricCyan
import com.example.ui.theme.ObsidianVoid
import com.example.ui.theme.PanelSteel
import com.example.ui.theme.ShieldViolet
import com.example.ui.theme.TacticalEmerald
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.WarningAmber
import kotlin.math.hypot

@Composable
fun TouchControlsOverlay(
    player: PlayerState,
    focusedInteractable: InteractableRuntime?,
    settings: GameSettings,
    onMoveInput: (Float, Float) -> Unit,
    onSprintHeld: (Boolean) -> Unit,
    onLookDelta: (Float, Float) -> Unit,
    onToggleCrouch: () -> Unit,
    onJump: () -> Unit,
    onInteract: () -> Unit,
    onScanner: () -> Unit,
    onDecoy: () -> Unit,
    onShield: () -> Unit,
    modifier: Modifier = Modifier
) {
    val scale = settings.buttonSizeScale
    val opacity = settings.buttonOpacity

    Box(modifier = modifier.fillMaxSize()) {
        // Right half: Touch-Look Area (placed behind buttons so buttons receive taps cleanly)
        Box(
            modifier = Modifier
                .fillMaxHeight()
                .fillMaxSize(0.55f)
                .align(Alignment.CenterEnd)
                .testTag("touch_look_area")
                .pointerInput(Unit) {
                    awaitEachGesture {
                        val down = awaitFirstDown(requireUnconsumed = false)
                        val activeId: PointerId = down.id
                        var lastPos = down.position
                        while (true) {
                            val event = awaitPointerEvent()
                            val change = event.changes.firstOrNull { it.id == activeId } ?: break
                            if (!change.pressed) {
                                change.consume()
                                break
                            }
                            val delta = change.position - lastPos
                            lastPos = change.position
                            if (delta.x != 0f || delta.y != 0f) {
                                onLookDelta(delta.x, delta.y)
                            }
                            change.consume()
                        }
                    }
                }
        )

        // Left side: Virtual Joystick + Sprint button
        Column(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(start = 22.dp, bottom = 20.dp)
                .alpha(opacity),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            var sprintActive by remember { mutableStateOf(false) }
            ActionCircleButton(
                icon = Icons.AutoMirrored.Filled.DirectionsRun,
                label = if (sprintActive) "RUN ON" else "SPRINT",
                size = (52f * scale).dp,
                accentColor = if (player.isSprinting || sprintActive) TacticalEmerald else ElectricCyan,
                active = sprintActive || player.isSprinting,
                testTag = "btn_sprint",
                onClick = {
                    sprintActive = !sprintActive
                    onSprintHeld(sprintActive)
                }
            )

            VirtualJoystickPad(
                size = (132f * scale).dp,
                onVectorChanged = onMoveInput
            )
        }

        // Right side: Action & Gadget Cluster
        Column(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = 22.dp, bottom = 20.dp)
                .alpha(opacity),
            horizontalAlignment = Alignment.End,
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Top gadget row: Scanner, Decoy, Shield
            Row(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                ActionCircleButton(
                    icon = Icons.Default.Radar,
                    label = if (player.scannerActiveTimer > 0f) {
                        "${player.scannerActiveTimer.toInt() + 1}s"
                    } else {
                        "${player.scannerCharge.toInt()}%"
                    },
                    size = (54f * scale).dp,
                    accentColor = ElectricCyan,
                    active = player.scannerActiveTimer > 0f,
                    testTag = "btn_scanner",
                    onClick = onScanner
                )
                ActionCircleButton(
                    icon = Icons.Default.Sensors,
                    label = if (player.decoyCooldown > 0f) {
                        "${player.decoyCooldown.toInt() + 1}s"
                    } else "DECOY",
                    size = (54f * scale).dp,
                    accentColor = WarningAmber,
                    active = player.decoyCooldown <= 0f,
                    testTag = "btn_decoy",
                    onClick = onDecoy
                )
                ActionCircleButton(
                    icon = Icons.Default.Security,
                    label = when {
                        player.shieldActiveTimer > 0f -> "ON ${player.shieldActiveTimer.toInt() + 1}s"
                        player.shieldCooldown > 0f -> "${player.shieldCooldown.toInt() + 1}s"
                        else -> "SHIELD"
                    },
                    size = (54f * scale).dp,
                    accentColor = ShieldViolet,
                    active = player.shieldActiveTimer > 0f,
                    testTag = "btn_shield",
                    onClick = onShield
                )
            }

            // Bottom movement & interact row: Crouch, Jump, Interact
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                ActionCircleButton(
                    icon = Icons.Default.AccessibilityNew,
                    label = if (player.isCrouching) "LOW" else "CROUCH",
                    size = (56f * scale).dp,
                    accentColor = if (player.isCrouching) TacticalEmerald else ElectricCyan,
                    active = player.isCrouching,
                    testTag = "btn_crouch",
                    onClick = onToggleCrouch
                )
                ActionCircleButton(
                    icon = Icons.Default.ArrowUpward,
                    label = "JUMP",
                    size = (56f * scale).dp,
                    accentColor = ElectricCyan,
                    active = !player.isGrounded,
                    testTag = "btn_jump",
                    onClick = onJump
                )
                ActionCircleButton(
                    icon = if (focusedInteractable != null) Icons.Default.Bolt else Icons.Default.TouchApp,
                    label = if (focusedInteractable != null) "USE" else "ACT",
                    size = (66f * scale).dp,
                    accentColor = if (focusedInteractable != null) TacticalEmerald else TextPrimary.copy(alpha = 0.55f),
                    active = focusedInteractable != null,
                    testTag = "btn_interact",
                    onClick = onInteract
                )
            }
        }
    }
}

@Composable
private fun VirtualJoystickPad(
    size: Dp,
    onVectorChanged: (Float, Float) -> Unit
) {
    var knobOffset by remember { mutableStateOf(Offset.Zero) }

    Box(
        modifier = Modifier
            .size(size)
            .testTag("virtual_joystick")
            .pointerInput(Unit) {
                val maxRadius = size.toPx() * 0.36f
                awaitEachGesture {
                    val down = awaitFirstDown(requireUnconsumed = false)
                    val activeId = down.id
                    val center = Offset(size.toPx() / 2f, size.toPx() / 2f)

                    fun updateFromPos(pos: Offset) {
                        val raw = pos - center
                        val dist = hypot(raw.x, raw.y)
                        val clamped = if (dist > maxRadius) {
                            Offset(raw.x / dist * maxRadius, raw.y / dist * maxRadius)
                        } else raw
                        knobOffset = clamped
                        // Screen Y down is negative forward; invert Y so pushing up = +1 forward
                        val nx = (clamped.x / maxRadius).coerceIn(-1f, 1f)
                        val ny = (-clamped.y / maxRadius).coerceIn(-1f, 1f)
                        onVectorChanged(nx, ny)
                    }

                    updateFromPos(down.position)
                    while (true) {
                        val event = awaitPointerEvent()
                        val change = event.changes.firstOrNull { it.id == activeId } ?: break
                        if (!change.pressed) {
                            change.consume()
                            break
                        }
                        updateFromPos(change.position)
                        change.consume()
                    }
                    knobOffset = Offset.Zero
                    onVectorChanged(0f, 0f)
                }
            }
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val c = Offset(this.size.width / 2f, this.size.height / 2f)
            val outerR = this.size.minDimension * 0.45f
            drawCircle(
                color = PanelSteel.copy(alpha = 0.55f),
                radius = outerR,
                center = c
            )
            drawCircle(
                color = ElectricCyan.copy(alpha = 0.65f),
                radius = outerR,
                center = c,
                style = Stroke(width = 3f)
            )
            val knobCenter = c + knobOffset
            drawCircle(
                color = ElectricCyan.copy(alpha = 0.85f),
                radius = outerR * 0.36f,
                center = knobCenter
            )
            drawCircle(
                color = Color.White.copy(alpha = 0.45f),
                radius = outerR * 0.22f,
                center = knobCenter,
                style = Stroke(width = 2f)
            )
        }
    }
}

@Composable
private fun ActionCircleButton(
    icon: ImageVector,
    label: String,
    size: Dp,
    accentColor: Color,
    active: Boolean,
    testTag: String,
    onClick: () -> Unit
) {
    val bgColor = if (active) accentColor.copy(alpha = 0.26f) else ObsidianVoid.copy(alpha = 0.68f)
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .size(size.coerceAtLeast(48.dp))
            .clip(CircleShape)
            .background(bgColor)
            .border(1.5.dp, accentColor.copy(alpha = if (active) 0.95f else 0.55f), CircleShape)
            .clickable(onClick = onClick)
            .testTag(testTag),
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = accentColor,
            modifier = Modifier.size(size * 0.38f)
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall.copy(
                fontSize = 9.sp,
                fontWeight = FontWeight.Bold
            ),
            color = TextPrimary,
            maxLines = 1
        )
    }
}
