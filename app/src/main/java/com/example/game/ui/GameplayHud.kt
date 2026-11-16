package com.example.game.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Backpack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.game.data.MissionCatalog
import com.example.game.engine.Math3D
import com.example.game.model.EnemyState
import com.example.game.model.HudUiState
import com.example.ui.theme.AlertCrimson
import com.example.ui.theme.ElectricCyan
import com.example.ui.theme.ObsidianVoid
import com.example.ui.theme.PanelBorder
import com.example.ui.theme.PanelSteel
import com.example.ui.theme.ShieldViolet
import com.example.ui.theme.TacticalEmerald
import com.example.ui.theme.TacticalSlate
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.WarningAmber
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun GameplayHud(
    state: HudUiState,
    onPause: () -> Unit,
    onOpenInventory: () -> Unit,
    modifier: Modifier = Modifier
) {
    val player = state.player

    Box(modifier = modifier.fillMaxSize()) {
        // Screen-edge feedback vignettes (Damage, Shield, Scanner, Hazard)
        if (player.damageFlash > 0.02f) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.radialGradient(
                            colors = listOf(Color.Transparent, AlertCrimson.copy(alpha = 0.48f * player.damageFlash))
                        )
                    )
            )
        }
        if (player.shieldActiveTimer > 0f) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .border(4.dp, ShieldViolet.copy(alpha = 0.65f))
            )
        }
        if (player.scannerActiveTimer > 0f) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .border(2.dp, ElectricCyan.copy(alpha = 0.55f))
            )
        }

        // Main Safe-Area HUD Layer
        Box(
            modifier = Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.safeDrawing)
                .padding(horizontal = 16.dp, vertical = 10.dp)
        ) {
            // TOP LEFT: Health, Stamina, Room Zone, Telemetry & Objective List
            Column(
                modifier = Modifier.align(Alignment.TopStart),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(ObsidianVoid.copy(alpha = 0.78f))
                        .border(1.dp, PanelBorder, RoundedCornerShape(8.dp))
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Column(modifier = Modifier.width(152.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                "HP ${player.health.toInt()}",
                                style = MaterialTheme.typography.labelSmall,
                                color = if (player.health < 35f) AlertCrimson else TacticalEmerald
                            )
                            Text(
                                if (player.shieldActiveTimer > 0f) "SHIELD ON" else state.currentRoomName,
                                style = MaterialTheme.typography.labelSmall,
                                color = if (player.shieldActiveTimer > 0f) ShieldViolet else ElectricCyan,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        Spacer(Modifier.height(4.dp))
                        LinearProgressIndicator(
                            progress = { (player.health / player.maxHealth).coerceIn(0f, 1f) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(6.dp)
                                .clip(RoundedCornerShape(3.dp)),
                            color = if (player.health < 35f) AlertCrimson else TacticalEmerald,
                            trackColor = PanelSteel
                        )
                        Spacer(Modifier.height(6.dp))
                        LinearProgressIndicator(
                            progress = { (player.stamina / player.maxStamina).coerceIn(0f, 1f) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(4.dp)
                                .clip(RoundedCornerShape(2.dp)),
                            color = if (player.sprintExhaustedLock) WarningAmber else ElectricCyan,
                            trackColor = PanelSteel
                        )
                    }

                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            "${state.fpsEstimate} FPS",
                            style = MaterialTheme.typography.labelSmall,
                            color = TextSecondary
                        )
                        Text(
                            "${state.ramUsageMb} MB",
                            style = MaterialTheme.typography.labelSmall,
                            color = TextMuted
                        )
                    }
                }

                // Objective Tracker Card
                Column(
                    modifier = Modifier
                        .widthIn(max = 260.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(ObsidianVoid.copy(alpha = 0.72f))
                        .border(1.dp, PanelBorder.copy(alpha = 0.7f), RoundedCornerShape(8.dp))
                        .padding(horizontal = 10.dp, vertical = 7.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        "${state.currentMission.code} // OBJECTIVES",
                        style = MaterialTheme.typography.labelSmall,
                        color = ElectricCyan
                    )
                    state.objectives.take(4).forEach { obj ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = if (obj.completed) Icons.Default.CheckCircle else Icons.Default.RadioButtonUnchecked,
                                contentDescription = null,
                                tint = when {
                                    obj.completed -> TacticalEmerald
                                    obj.def.isOptional -> WarningAmber
                                    else -> TextSecondary
                                },
                                modifier = Modifier.size(13.dp)
                            )
                            Text(
                                text = obj.def.description,
                                style = MaterialTheme.typography.bodyMedium.copy(fontSize = 11.5f.let {
                                    MaterialTheme.typography.labelSmall.fontSize
                                }),
                                color = if (obj.completed) TextMuted else TextPrimary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }

            // TOP CENTER: Detection Alert / Hazard Warning
            Column(
                modifier = Modifier.align(Alignment.TopCenter),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                if (state.maxEnemyDetection > 5f || state.anyEnemyAlert) {
                    val isAlert = state.anyEnemyAlert
                    val color = if (isAlert) AlertCrimson else WarningAmber
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(20.dp))
                            .background(ObsidianVoid.copy(alpha = 0.85f))
                            .border(1.5.dp, color, RoundedCornerShape(20.dp))
                            .padding(horizontal = 14.dp, vertical = 5.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Warning,
                            contentDescription = "Detection Alert",
                            tint = color,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = if (isAlert) "HOSTILE ALERT // EVADE" else "DETECTING ${state.maxEnemyDetection.toInt()}%",
                            style = MaterialTheme.typography.labelLarge,
                            color = color
                        )
                    }
                }

                if (player.environmentalHazardActive && player.shieldActiveTimer <= 0f) {
                    Text(
                        text = "HIGH VOLTAGE HAZARD — ACTIVATE SHIELD",
                        style = MaterialTheme.typography.labelSmall,
                        color = WarningAmber,
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(AlertCrimson.copy(alpha = 0.7f))
                            .padding(horizontal = 10.dp, vertical = 3.dp)
                    )
                }

                AnimatedVisibility(
                    visible = state.statusToast != null,
                    enter = fadeIn(),
                    exit = fadeOut()
                ) {
                    state.statusToast?.let { toast ->
                        Text(
                            text = toast,
                            style = MaterialTheme.typography.labelLarge,
                            color = ObsidianVoid,
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(ElectricCyan)
                                .padding(horizontal = 12.dp, vertical = 4.dp)
                        )
                    }
                }
            }

            // TOP RIGHT: Tactical Mini-Radar + Inventory & Pause Buttons
            Row(
                modifier = Modifier.align(Alignment.TopEnd),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.Top
            ) {
                TacticalMiniRadar(state = state)

                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    IconButton(
                        onClick = onPause,
                        modifier = Modifier
                            .size(48.dp)
                            .clip(CircleShape)
                            .background(ObsidianVoid.copy(alpha = 0.8f))
                            .border(1.dp, PanelBorder, CircleShape)
                            .testTag("btn_pause")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Pause,
                            contentDescription = "Pause",
                            tint = TextPrimary
                        )
                    }

                    IconButton(
                        onClick = onOpenInventory,
                        modifier = Modifier
                            .size(48.dp)
                            .clip(CircleShape)
                            .background(ObsidianVoid.copy(alpha = 0.8f))
                            .border(1.dp, ElectricCyan.copy(alpha = 0.65f), CircleShape)
                            .testTag("btn_inventory")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Backpack,
                            contentDescription = "Inventory & Logs",
                            tint = ElectricCyan
                        )
                    }
                }
            }

            // CENTER CROSSHAIR & INTERACT PROMPT
            Column(
                modifier = Modifier.align(Alignment.Center),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(6.dp)
                        .clip(CircleShape)
                        .background(
                            if (state.focusedInteractable != null) TacticalEmerald
                            else ElectricCyan.copy(alpha = 0.75f)
                        )
                )

                state.focusedInteractable?.let { focused ->
                    Column(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(ObsidianVoid.copy(alpha = 0.84f))
                            .border(
                                1.dp,
                                if (focused.isLocked) WarningAmber else TacticalEmerald,
                                RoundedCornerShape(8.dp)
                            )
                            .padding(horizontal = 14.dp, vertical = 6.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = focused.def.title,
                            style = MaterialTheme.typography.labelLarge,
                            color = if (focused.isLocked) WarningAmber else TacticalEmerald
                        )
                        Text(
                            text = if (focused.isLocked) "LOCKED — ACCESS REQUIRED" else "[USE] ${focused.def.promptText}",
                            style = MaterialTheme.typography.labelSmall,
                            color = TextPrimary
                        )
                    }
                }
            }

            // BOTTOM CENTER: Radio Communication & Subtitles
            AnimatedVisibility(
                visible = state.radioSubtitle != null,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 14.dp),
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                state.radioSubtitle?.let { sub ->
                    Box(
                        modifier = Modifier
                            .widthIn(max = 460.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(ObsidianVoid.copy(alpha = 0.88f))
                            .border(1.dp, ElectricCyan.copy(alpha = 0.6f), RoundedCornerShape(8.dp))
                            .padding(horizontal = 14.dp, vertical = 8.dp)
                    ) {
                        Text(
                            text = sub,
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextPrimary
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun TacticalMiniRadar(state: HudUiState) {
    val mission = MissionCatalog.getMission(state.currentMission)
    val player = state.player
    val radarRangeMeters = 18f

    Box(
        modifier = Modifier
            .size(104.dp)
            .clip(CircleShape)
            .background(ObsidianVoid.copy(alpha = 0.82f))
            .border(1.5.dp, ElectricCyan.copy(alpha = 0.65f), CircleShape)
            .testTag("tactical_mini_radar")
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val cx = size.width / 2f
            val cy = size.height / 2f
            val radiusPx = size.minDimension / 2f - 4f
            val pxPerMeter = radiusPx / radarRangeMeters

            // Radar rings
            drawCircle(
                color = PanelBorder.copy(alpha = 0.55f),
                radius = radiusPx * 0.5f,
                center = Offset(cx, cy),
                style = Stroke(width = 1.5f)
            )

            val yawRad = player.yawDeg * Math3D.DEG_TO_RAD
            val cosY = cos(yawRad)
            val sinY = sin(yawRad)

            fun worldToRadar(wx: Float, wz: Float): Offset {
                val dx = wx - player.x
                val dz = wz - player.z
                // Rotate so player's forward (-Z local) points UP on radar
                val rx = dx * cosY + dz * sinY
                val rz = -dx * sinY + dz * cosY
                val px = cx + rx * pxPerMeter
                val py = cy + rz * pxPerMeter
                return Offset(px, py)
            }

            // Objective markers
            val nextObj = state.objectives.firstOrNull { !it.completed && !it.def.isOptional }
            nextObj?.let { obj ->
                val ox = (obj.def.targetGridX + 0.5f) * mission.tileSize
                val oz = (obj.def.targetGridZ + 0.5f) * mission.tileSize
                val pt = worldToRadar(ox, oz)
                val dir = pt - Offset(cx, cy)
                val d = dir.getDistance()
                val clampedPt = if (d > radiusPx - 5f && d > 0.01f) {
                    Offset(cx, cy) + (dir / d) * (radiusPx - 5f)
                } else pt
                drawCircle(color = TacticalEmerald, radius = 5f, center = clampedPt)
            }

            // Enemies (always shown if Alert/Investigate or Scanner Active or within 11m)
            val scannerOn = player.scannerActiveTimer > 0f
            for (e in state.enemies) {
                if (e.state == EnemyState.DISABLED) continue
                val dist = Math3D.distance2D(player.x, player.z, e.x, e.z)
                if (scannerOn || dist <= 12f || e.state == EnemyState.ALERT) {
                    val pt = worldToRadar(e.x, e.z)
                    if ((pt - Offset(cx, cy)).getDistance() <= radiusPx) {
                        val c = when (e.state) {
                            EnemyState.ALERT -> AlertCrimson
                            EnemyState.INVESTIGATE, EnemyState.SEARCH -> WarningAmber
                            else -> ElectricCyan
                        }
                        drawCircle(color = c, radius = 4.5f, center = pt)
                    }
                }
            }

            // Player center chevron
            val tri = Path().apply {
                moveTo(cx, cy - 6f)
                lineTo(cx - 4.5f, cy + 5f)
                lineTo(cx + 4.5f, cy + 5f)
                close()
            }
            drawPath(tri, color = Color.White)
        }
    }
}
