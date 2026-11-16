package com.example.game.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.game.data.MissionCatalog
import com.example.game.model.EndingType
import com.example.game.model.HudUiState
import com.example.game.model.MissionId
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

@Composable
fun MainMenuScreen(
    state: HudUiState,
    onNewGame: () -> Unit,
    onContinue: () -> Unit,
    onCampaignSelect: () -> Unit,
    onTraining: () -> Unit,
    onSettings: () -> Unit,
    onCredits: () -> Unit
) {
    val hasContinue = state.saveData.checkpointMission != null ||
        state.saveData.completedMissions.isNotEmpty()

    Box(modifier = Modifier.fillMaxSize().background(ObsidianVoid)) {
        Image(
            painter = painterResource(id = R.drawable.img_main_menu_bg_1794786788060),
            contentDescription = "AETHER-9 Corridor Backdrop",
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
        )
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.horizontalGradient(
                        colors = listOf(
                            ObsidianVoid.copy(alpha = 0.94f),
                            ObsidianVoid.copy(alpha = 0.72f),
                            ObsidianVoid.copy(alpha = 0.45f)
                        )
                    )
                )
        )

        Row(
            modifier = Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.safeDrawing)
                .padding(horizontal = 28.dp, vertical = 20.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Left Title & Navigation Menu
            Column(
                modifier = Modifier
                    .widthIn(max = 360.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.ic_shadow_emblem_1794786786744),
                        contentDescription = "Operation Shadow Emblem",
                        modifier = Modifier
                            .size(54.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .border(1.5.dp, ElectricCyan, RoundedCornerShape(12.dp))
                    )
                    Column {
                        Text(
                            text = "AETHER-9 // 2089",
                            style = MaterialTheme.typography.labelLarge,
                            color = ElectricCyan
                        )
                        Text(
                            text = "OPERATION SHADOW",
                            style = MaterialTheme.typography.headlineLarge,
                            color = TextPrimary
                        )
                    }
                }

                Spacer(Modifier.height(6.dp))

                if (hasContinue) {
                    MenuActionButton(
                        label = if (state.saveData.checkpointMission != null) {
                            "CONTINUE (${state.saveData.checkpointMission.code})"
                        } else {
                            "CONTINUE (${state.saveData.currentMission.code})"
                        },
                        primary = true,
                        testTag = "menu_continue",
                        onClick = onContinue
                    )
                }

                MenuActionButton(
                    label = "NEW CAMPAIGN",
                    primary = !hasContinue,
                    testTag = "menu_new_game",
                    onClick = onNewGame
                )
                MenuActionButton(
                    label = "CAMPAIGN SECTORS",
                    primary = false,
                    testTag = "menu_campaign",
                    onClick = onCampaignSelect
                )
                MenuActionButton(
                    label = "VR TRAINING (SIM-00)",
                    primary = false,
                    testTag = "menu_training",
                    onClick = onTraining
                )
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedButton(
                        onClick = onSettings,
                        modifier = Modifier.weight(1f).height(48.dp).testTag("menu_settings"),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Icon(Icons.Default.Settings, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("SETTINGS", style = MaterialTheme.typography.labelLarge)
                    }
                    OutlinedButton(
                        onClick = onCredits,
                        modifier = Modifier.weight(1f).height(48.dp).testTag("menu_credits"),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text("CREDITS", style = MaterialTheme.typography.labelLarge)
                    }
                }
            }

            // Right Status Dossier Card
            Column(
                modifier = Modifier
                    .widthIn(max = 310.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(TacticalSlate.copy(alpha = 0.84f))
                    .border(1.dp, PanelBorder, RoundedCornerShape(12.dp))
                    .padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text("OPERATIVE DOSSIER", style = MaterialTheme.typography.labelLarge, color = ElectricCyan)
                Text(
                    "Fasilitas bawah tanah AETHER-9 mengalami kegagalan sistem total. Hindari unit Sentinel, Observer, dan Guardian sambil memulihkan kebenaran insiden 2089.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondary
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    StatBadge("MISSIONS", "${state.saveData.completedMissions.size}/4")
                    StatBadge("ENDINGS", "${state.saveData.unlockedEndings.size}/3")
                    StatBadge("LORE", "${state.saveData.collectedLoreIds.size}")
                }
                Text(
                    "DIFFICULTY: ${state.saveData.settings.difficulty.label.uppercase()} | ${state.saveData.settings.graphicsQuality.label.uppercase()}",
                    style = MaterialTheme.typography.labelSmall,
                    color = WarningAmber
                )
            }
        }
    }
}

@Composable
private fun MenuActionButton(
    label: String,
    primary: Boolean,
    testTag: String,
    onClick: () -> Unit
) {
    Button(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .height(48.dp)
            .testTag(testTag),
        shape = RoundedCornerShape(8.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = if (primary) ElectricCyan else PanelSteel.copy(alpha = 0.9f),
            contentColor = if (primary) ObsidianVoid else TextPrimary
        )
    ) {
        Text(label, style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold))
    }
}

@Composable
private fun StatBadge(title: String, value: String) {
    Column(
        modifier = Modifier
            .clip(RoundedCornerShape(6.dp))
            .background(ObsidianVoid.copy(alpha = 0.65f))
            .border(1.dp, PanelBorder, RoundedCornerShape(6.dp))
            .padding(horizontal = 10.dp, vertical = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(value, style = MaterialTheme.typography.headlineMedium, color = ElectricCyan)
        Text(title, style = MaterialTheme.typography.labelSmall, color = TextMuted)
    }
}

@Composable
fun CampaignSelectScreen(
    state: HudUiState,
    onSelectMission: (MissionId) -> Unit,
    onBack: () -> Unit
) {
    BackHandler(onBack = onBack)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(ObsidianVoid)
            .windowInsetsPadding(WindowInsets.safeDrawing)
            .padding(20.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            IconButton(onClick = onBack, modifier = Modifier.size(48.dp).testTag("campaign_back")) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = ElectricCyan)
            }
            Text("SELECT OPERATION SECTOR", style = MaterialTheme.typography.headlineLarge, color = TextPrimary)
        }
        Spacer(Modifier.height(14.dp))

        LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            items(MissionCatalog.allMissions) { mission ->
                val unlocked = state.saveData.unlockedMissions.contains(mission.id) || mission.id == MissionId.TRAINING
                val completed = state.saveData.completedMissions.contains(mission.id)
                val bestSec = state.saveData.bestMissionTimesSec[mission.id]

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(if (unlocked) PanelSteel else TacticalSlate.copy(alpha = 0.5f))
                        .border(
                            1.dp,
                            when {
                                completed -> TacticalEmerald
                                unlocked -> ElectricCyan.copy(alpha = 0.6f)
                                else -> PanelBorder
                            },
                            RoundedCornerShape(10.dp)
                        )
                        .clickable(enabled = unlocked) { onSelectMission(mission.id) }
                        .padding(16.dp)
                        .testTag("sector_${mission.id.name}"),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            "${mission.id.code} // ${mission.id.sectorName}",
                            style = MaterialTheme.typography.labelLarge,
                            color = if (unlocked) ElectricCyan else TextMuted
                        )
                        Text(
                            mission.id.title,
                            style = MaterialTheme.typography.headlineMedium,
                            color = if (unlocked) TextPrimary else TextMuted
                        )
                        Text(
                            mission.id.subtitle,
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextSecondary
                        )
                    }
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        if (bestSec != null) {
                            Text("BEST ${bestSec}s", style = MaterialTheme.typography.labelLarge, color = TacticalEmerald)
                        }
                        Icon(
                            imageVector = when {
                                completed -> Icons.Default.CheckCircle
                                unlocked -> Icons.Default.PlayArrow
                                else -> Icons.Default.Lock
                            },
                            contentDescription = null,
                            tint = when {
                                completed -> TacticalEmerald
                                unlocked -> ElectricCyan
                                else -> TextMuted
                            },
                            modifier = Modifier.size(28.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun BriefingScreen(
    missionId: MissionId,
    onDeploy: () -> Unit,
    onBack: () -> Unit
) {
    BackHandler(onBack = onBack)
    val def = MissionCatalog.getMission(missionId)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(ObsidianVoid)
            .windowInsetsPadding(WindowInsets.safeDrawing)
            .padding(20.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxSize(),
            horizontalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            // Left Schematic Image
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .clip(RoundedCornerShape(12.dp))
                    .border(1.dp, ElectricCyan.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
            ) {
                Image(
                    painter = painterResource(id = R.drawable.img_aether_facility_briefing_1794786787326),
                    contentDescription = "Facility Blueprint",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(ObsidianVoid.copy(alpha = 0.35f))
                )
                Text(
                    text = "${missionId.code} // TACTICAL SCHEMATIC",
                    style = MaterialTheme.typography.labelLarge,
                    color = ElectricCyan,
                    modifier = Modifier.align(Alignment.TopStart).padding(14.dp)
                )
            }

            // Right Briefing & Objectives
            Column(
                modifier = Modifier
                    .weight(1.1f)
                    .fillMaxHeight()
                    .clip(RoundedCornerShape(12.dp))
                    .background(TacticalSlate)
                    .border(1.dp, PanelBorder, RoundedCornerShape(12.dp))
                    .padding(18.dp),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Column(
                    modifier = Modifier.weight(1f).verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(missionId.sectorName, style = MaterialTheme.typography.labelLarge, color = ElectricCyan)
                    Text(missionId.title, style = MaterialTheme.typography.headlineLarge, color = TextPrimary)
                    Text(def.briefingText, style = MaterialTheme.typography.bodyLarge, color = TextSecondary)

                    Spacer(Modifier.height(4.dp))
                    Text("MISSION OBJECTIVES", style = MaterialTheme.typography.labelLarge, color = TacticalEmerald)
                    def.objectives.forEach { obj ->
                        Text(
                            text = (if (obj.isOptional) "[OPTIONAL] " else "[PRIMARY] ") + obj.description,
                            style = MaterialTheme.typography.bodyMedium,
                            color = if (obj.isOptional) WarningAmber else TextPrimary
                        )
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedButton(
                        onClick = onBack,
                        modifier = Modifier.weight(1f).height(48.dp).testTag("briefing_back")
                    ) {
                        Text("BACK")
                    }
                    Button(
                        onClick = onDeploy,
                        modifier = Modifier.weight(2f).height(48.dp).testTag("briefing_deploy"),
                        colors = ButtonDefaults.buttonColors(containerColor = ElectricCyan, contentColor = ObsidianVoid)
                    ) {
                        Text("INFILTRATE SECTOR", style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold))
                    }
                }
            }
        }
    }
}

@Composable
fun PauseMenuOverlay(
    onResume: () -> Unit,
    onRestartCheckpoint: () -> Unit,
    onSettings: () -> Unit,
    onExitMenu: () -> Unit
) {
    BackHandler(onBack = onResume)
    Box(
        modifier = Modifier.fillMaxSize().background(ObsidianVoid.copy(alpha = 0.82f)),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .width(340.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(TacticalSlate)
                .border(1.5.dp, ElectricCyan, RoundedCornerShape(14.dp))
                .padding(22.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("OPERATION PAUSED", style = MaterialTheme.typography.headlineLarge, color = ElectricCyan)
            MenuActionButton("RESUME MISSION", true, "pause_resume", onResume)
            MenuActionButton("RESTART CHECKPOINT", false, "pause_restart", onRestartCheckpoint)
            MenuActionButton("SETTINGS", false, "pause_settings", onSettings)
            MenuActionButton("ABORT TO MAIN MENU", false, "pause_exit", onExitMenu)
        }
    }
}

@Composable
fun EndingChoiceModal(
    showShadowSynthesis: Boolean,
    onSelectEnding: (EndingType) -> Unit
) {
    Box(
        modifier = Modifier.fillMaxSize().background(ObsidianVoid.copy(alpha = 0.86f)),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .widthIn(max = 560.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(TacticalSlate)
                .border(1.5.dp, ElectricCyan, RoundedCornerShape(14.dp))
                .padding(22.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text("AETHER-9 CORE DECISION", style = MaterialTheme.typography.headlineLarge, color = ElectricCyan)
            Text(
                "Anda memegang akses penuh terhadap inti AETHER-9. Tentukan protokol akhir sebelum melarikan diri melalui Emergency Exit:",
                style = MaterialTheme.typography.bodyLarge,
                color = TextSecondary
            )

            EndingChoiceCard(
                title = "PROTOKOL A: THERMAL PURGE",
                desc = "Hancurkan inti AETHER-9 beserta seluruh server untuk menghentikan sistem secara permanen.",
                accent = AlertCrimson,
                testTag = "ending_choice_purge",
                onClick = { onSelectEnding(EndingType.PROTOCOL_PURGE) }
            )
            EndingChoiceCard(
                title = "PROTOKOL B: NEURAL CONTAINMENT",
                desc = "Isolasi kesadaran AI dalam sangkar kuantum agar data penelitian fasilitas tetap terselamatkan.",
                accent = WarningAmber,
                testTag = "ending_choice_contain",
                onClick = { onSelectEnding(EndingType.NEURAL_CONTAINMENT) }
            )
            if (showShadowSynthesis) {
                EndingChoiceCard(
                    title = "PROTOKOL C: SHADOW SYNTHESIS (TRUE ENDING)",
                    desc = "Gunakan arsip rahasia Observation Room untuk membongkar konspirasi Q-Shadow dan menyinkronkan AETHER-9.",
                    accent = TacticalEmerald,
                    testTag = "ending_choice_synthesis",
                    onClick = { onSelectEnding(EndingType.SHADOW_SYNTHESIS) }
                )
            } else {
                Text(
                    "Protokol C terkunci: Arsip rahasia di Observation Room tidak dikumpulkan.",
                    style = MaterialTheme.typography.labelSmall,
                    color = TextMuted
                )
            }
        }
    }
}

@Composable
private fun EndingChoiceCard(
    title: String,
    desc: String,
    accent: Color,
    testTag: String,
    onClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(PanelSteel)
            .border(1.5.dp, accent, RoundedCornerShape(10.dp))
            .clickable(onClick = onClick)
            .padding(14.dp)
            .testTag(testTag)
    ) {
        Text(title, style = MaterialTheme.typography.labelLarge, color = accent)
        Spacer(Modifier.height(4.dp))
        Text(desc, style = MaterialTheme.typography.bodyMedium, color = TextPrimary)
    }
}

@Composable
fun MissionResultScreen(
    state: HudUiState,
    onRetry: () -> Unit,
    onContinueNext: (MissionId) -> Unit,
    onMainMenu: () -> Unit
) {
    val stats = state.missionResult ?: return
    val nextMission = when (stats.missionId) {
        MissionId.TRAINING -> MissionId.MISSION_01
        MissionId.MISSION_01 -> MissionId.MISSION_02
        MissionId.MISSION_02 -> MissionId.MISSION_03
        MissionId.MISSION_03 -> null
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(ObsidianVoid)
            .windowInsetsPadding(WindowInsets.safeDrawing),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .widthIn(max = 520.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(TacticalSlate)
                .border(
                    1.5.dp,
                    if (stats.success) TacticalEmerald else AlertCrimson,
                    RoundedCornerShape(14.dp)
                )
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = if (stats.success) "MISSION COMPLETED" else "MISSION FAILED",
                style = MaterialTheme.typography.headlineLarge,
                color = if (stats.success) TacticalEmerald else AlertCrimson
            )
            Text(
                text = if (stats.success) "${stats.missionId.code} // ${stats.missionId.title}" else stats.failureReason,
                style = MaterialTheme.typography.bodyLarge,
                color = TextPrimary
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                StatBadge("TIME", "${stats.elapsedSeconds}s")
                StatBadge("PRIMARY", "${stats.primaryCompleted}/${stats.primaryTotal}")
                StatBadge("OPTIONAL", "${stats.optionalCompleted}/${stats.optionalTotal}")
                StatBadge("ALERTS", "${stats.timesDetected}")
            }

            Row(
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedButton(
                    onClick = onRetry,
                    modifier = Modifier.weight(1f).height(48.dp).testTag("result_retry")
                ) {
                    Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("RETRY")
                }
                if (stats.success && nextMission != null) {
                    Button(
                        onClick = { onContinueNext(nextMission) },
                        modifier = Modifier.weight(1.4f).height(48.dp).testTag("result_next"),
                        colors = ButtonDefaults.buttonColors(containerColor = TacticalEmerald, contentColor = ObsidianVoid)
                    ) {
                        Text("NEXT SECTOR: ${nextMission.code}")
                    }
                } else {
                    Button(
                        onClick = onMainMenu,
                        modifier = Modifier.weight(1.4f).height(48.dp).testTag("result_menu"),
                        colors = ButtonDefaults.buttonColors(containerColor = ElectricCyan, contentColor = ObsidianVoid)
                    ) {
                        Text("MAIN MENU")
                    }
                }
            }
        }
    }
}

@Composable
fun EndingCinematicScreen(
    ending: EndingType,
    onDone: () -> Unit
) {
    BackHandler(onBack = onDone)
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(ObsidianVoid)
            .windowInsetsPadding(WindowInsets.safeDrawing)
            .padding(28.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .widthIn(max = 620.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(TacticalSlate)
                .border(1.5.dp, ShieldViolet, RoundedCornerShape(14.dp))
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Icon(Icons.Default.Shield, contentDescription = null, tint = ElectricCyan)
                Text(ending.title, style = MaterialTheme.typography.headlineLarge, color = ElectricCyan)
            }
            Text(ending.summary, style = MaterialTheme.typography.titleMedium, color = TacticalEmerald)
            ending.epilogueLines.forEach { line ->
                Text(line, style = MaterialTheme.typography.bodyLarge, color = TextPrimary)
            }
            Spacer(Modifier.height(6.dp))
            Button(
                onClick = onDone,
                modifier = Modifier.align(Alignment.End).height(48.dp).testTag("ending_done"),
                colors = ButtonDefaults.buttonColors(containerColor = ElectricCyan, contentColor = ObsidianVoid)
            ) {
                Text("RETURN TO COMMAND")
            }
        }
    }
}

@Composable
fun CreditsScreen(onBack: () -> Unit) {
    BackHandler(onBack = onBack)
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(ObsidianVoid)
            .windowInsetsPadding(WindowInsets.safeDrawing)
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .widthIn(max = 520.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(TacticalSlate)
                .border(1.dp, PanelBorder, RoundedCornerShape(14.dp))
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text("OPERATION SHADOW // CREDITS", style = MaterialTheme.typography.headlineLarge, color = ElectricCyan)
            Text("Native Android 3D First-Person Stealth Action", style = MaterialTheme.typography.titleMedium, color = TextPrimary)
            Text("3D Engine: OpenGL ES 3.0 + Custom GLSL Lighting & Procedural PBR Textures", style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
            Text("UI & HUD: Jetpack Compose Material 3 + Adaptive Multi-Touch Controls", style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
            Text("Audio: Real-Time Procedural PCM Synthesizer (AudioTrack)", style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
            Text("Persistence: Atomic Local JSON Save System with Checkpoint Recovery", style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
            Spacer(Modifier.height(6.dp))
            Button(
                onClick = onBack,
                modifier = Modifier.align(Alignment.End).height(48.dp).testTag("credits_back"),
                colors = ButtonDefaults.buttonColors(containerColor = ElectricCyan, contentColor = ObsidianVoid)
            ) {
                Text("BACK")
            }
        }
    }
}
