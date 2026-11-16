package com.example.game.ui

import androidx.activity.compose.BackHandler
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
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.game.model.ActivePuzzle
import com.example.game.model.Difficulty
import com.example.game.model.GameSettings
import com.example.game.model.GraphicsQuality
import com.example.game.model.HudUiState
import com.example.game.model.LoreEntry
import com.example.game.model.PuzzleType
import com.example.ui.theme.AlertCrimson
import com.example.ui.theme.ElectricCyan
import com.example.ui.theme.ObsidianVoid
import com.example.ui.theme.PanelBorder
import com.example.ui.theme.PanelSteel
import com.example.ui.theme.TacticalEmerald
import com.example.ui.theme.TacticalSlate
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.WarningAmber
import kotlin.math.abs

@Composable
fun PuzzleModalScreen(
    puzzle: ActivePuzzle,
    onSolved: () -> Unit,
    onCancel: () -> Unit
) {
    BackHandler(onBack = onCancel)
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(ObsidianVoid.copy(alpha = 0.88f))
            .windowInsetsPadding(WindowInsets.safeDrawing),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .widthIn(max = 560.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(TacticalSlate)
                .border(1.5.dp, ElectricCyan, RoundedCornerShape(14.dp))
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(puzzle.puzzleType.name.replace("_", " "), style = MaterialTheme.typography.labelLarge, color = ElectricCyan)
                    Text(puzzle.title, style = MaterialTheme.typography.headlineMedium, color = TextPrimary)
                }
                OutlinedButton(onClick = onCancel, modifier = Modifier.height(48.dp).testTag("puzzle_cancel")) {
                    Text("ABORT")
                }
            }

            when (puzzle.puzzleType) {
                PuzzleType.POWER_ROUTING -> PowerRoutingPuzzleContent(onSolved = onSolved)
                PuzzleType.TERMINAL_SEQUENCE -> TerminalSequencePuzzleContent(onSolved = onSolved)
                PuzzleType.DATA_RECOVERY -> DataRecoveryPuzzleContent(onSolved = onSolved)
            }
        }
    }
}

@Composable
private fun PowerRoutingPuzzleContent(onSolved: () -> Unit) {
    // 4x4 grid of rotatable conduit tiles:
    // Bitmask: 1=N, 2=E, 4=S, 8=W
    // Types: STRAIGHT (5 -> 10), CORNER (3 -> 6 -> 12 -> 9), TEE (7 -> 14 -> 13 -> 11)
    val initialMasks = remember {
        listOf(
            6, 10, 12, 6,
            5, 3, 5, 9,
            3, 10, 14, 5,
            10, 12, 3, 9
        )
    }
    val tiles = remember { mutableStateListOf<Int>().apply { addAll(initialMasks) } }

    fun rotateMask(m: Int): Int {
        var r = 0
        if (m and 1 != 0) r = r or 2
        if (m and 2 != 0) r = r or 4
        if (m and 4 != 0) r = r or 8
        if (m and 8 != 0) r = r or 1
        return r
    }

    fun poweredSet(): Set<Int> {
        val visited = mutableSetOf<Int>()
        if (tiles[0] and 8 == 0 && tiles[0] and 1 == 0 && tiles[0] and 2 == 0 && tiles[0] and 4 == 0) return emptySet()
        // Source feeds into top-left cell (index 0) from West (8)
        if (tiles[0] and 8 == 0 && tiles[0] and 1 == 0) return emptySet()
        val queue = ArrayDeque<Int>()
        queue.add(0)
        visited.add(0)
        while (queue.isNotEmpty()) {
            val idx = queue.removeFirst()
            val r = idx / 4
            val c = idx % 4
            val m = tiles[idx]
            // North
            if ((m and 1 != 0) && r > 0) {
                val n = (r - 1) * 4 + c
                if ((tiles[n] and 4 != 0) && visited.add(n)) queue.add(n)
            }
            // East
            if ((m and 2 != 0) && c < 3) {
                val n = r * 4 + (c + 1)
                if ((tiles[n] and 8 != 0) && visited.add(n)) queue.add(n)
            }
            // South
            if ((m and 4 != 0) && r < 3) {
                val n = (r + 1) * 4 + c
                if ((tiles[n] and 1 != 0) && visited.add(n)) queue.add(n)
            }
            // West
            if ((m and 8 != 0) && c > 0) {
                val n = r * 4 + (c - 1)
                if ((tiles[n] and 2 != 0) && visited.add(n)) queue.add(n)
            }
        }
        return visited
    }

    val powered = poweredSet()
    val solved = powered.contains(15) && ((tiles[15] and 2 != 0) || (tiles[15] and 4 != 0))

    Text(
        "Ketuk modul kabel untuk memutar jalur arus dari IN (kiri atas) menuju OUT (kanan bawah).",
        style = MaterialTheme.typography.bodyMedium,
        color = TextSecondary
    )

    Column(verticalArrangement = Arrangement.spacedBy(6.dp), horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
        for (r in 0 until 4) {
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                for (c in 0 until 4) {
                    val idx = r * 4 + c
                    val mask = tiles[idx]
                    val isPowered = powered.contains(idx)
                    Box(
                        modifier = Modifier
                            .size(56.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (isPowered) ElectricCyan.copy(alpha = 0.22f) else PanelSteel)
                            .border(
                                1.5.dp,
                                when {
                                    idx == 0 -> TacticalEmerald
                                    idx == 15 && solved -> TacticalEmerald
                                    idx == 15 -> WarningAmber
                                    isPowered -> ElectricCyan
                                    else -> PanelBorder
                                },
                                RoundedCornerShape(8.dp)
                            )
                            .clickable { tiles[idx] = rotateMask(mask) }
                            .testTag("power_tile_$idx")
                    ) {
                        Canvas(modifier = Modifier.fillMaxSize()) {
                            val mid = Offset(size.width / 2f, size.height / 2f)
                            val lineColor = if (isPowered) ElectricCyan else TextMuted
                            val stroke = 9f
                            if (mask and 1 != 0) drawLine(lineColor, mid, Offset(mid.x, 0f), stroke, StrokeCap.Round)
                            if (mask and 2 != 0) drawLine(lineColor, mid, Offset(size.width, mid.y), stroke, StrokeCap.Round)
                            if (mask and 4 != 0) drawLine(lineColor, mid, Offset(mid.x, size.height), stroke, StrokeCap.Round)
                            if (mask and 8 != 0) drawLine(lineColor, mid, Offset(0f, mid.y), stroke, StrokeCap.Round)
                            drawCircle(lineColor, radius = 6f, center = mid)
                        }
                    }
                }
            }
        }
    }

    Button(
        onClick = onSolved,
        enabled = solved,
        modifier = Modifier.fillMaxWidth().height(48.dp).testTag("puzzle_confirm"),
        colors = ButtonDefaults.buttonColors(containerColor = TacticalEmerald, contentColor = ObsidianVoid)
    ) {
        Text(if (solved) "CIRCUIT COMPLETE — ENGAGE POWER" else "ROUTE POWER TO BOTTOM-RIGHT NODE")
    }
}

@Composable
private fun TerminalSequencePuzzleContent(onSolved: () -> Unit) {
    val targetCode = remember { listOf("A9", "X4", "Q7", "K2") }
    val choices = remember { listOf("A9", "B3", "X4", "Q7", "M1", "K2") }
    val entered = remember { mutableStateListOf<String>() }
    var errorMsg by remember { mutableStateOf<String?>(null) }

    Text(
        "Masukkan urutan cipher keamanan 2089 sesuai protokol dekripsi: ${targetCode.joinToString(" - ")}",
        style = MaterialTheme.typography.bodyMedium,
        color = TextSecondary
    )

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        for (i in 0 until 4) {
            val valStr = entered.getOrNull(i) ?: "--"
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(48.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(ObsidianVoid)
                    .border(1.5.dp, if (valStr == targetCode[i]) TacticalEmerald else ElectricCyan, RoundedCornerShape(8.dp)),
                contentAlignment = Alignment.Center
            ) {
                Text(valStr, style = MaterialTheme.typography.headlineMedium, color = TextPrimary)
            }
        }
    }

    errorMsg?.let {
        Text(it, style = MaterialTheme.typography.labelSmall, color = AlertCrimson)
    }

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        choices.forEach { code ->
            Button(
                onClick = {
                    errorMsg = null
                    val nextIdx = entered.size
                    if (nextIdx < 4) {
                        if (targetCode[nextIdx] == code) {
                            entered.add(code)
                            if (entered.size == 4) {
                                onSolved()
                            }
                        } else {
                            entered.clear()
                            errorMsg = "SEQUENCE MISMATCH — BUFFER RESET"
                        }
                    }
                },
                modifier = Modifier.weight(1f).height(48.dp).testTag("cipher_$code"),
                colors = ButtonDefaults.buttonColors(containerColor = PanelSteel, contentColor = ElectricCyan)
            ) {
                Text(code, style = MaterialTheme.typography.labelLarge)
            }
        }
    }
}

@Composable
private fun DataRecoveryPuzzleContent(onSolved: () -> Unit) {
    val targetA = 72
    val targetB = 45
    val targetC = 85

    var bandA by remember { mutableIntStateOf(30) }
    var bandB by remember { mutableIntStateOf(70) }
    var bandC by remember { mutableIntStateOf(40) }

    val aligned = abs(bandA - targetA) <= 6 && abs(bandB - targetB) <= 6 && abs(bandC - targetC) <= 6

    Text(
        "Selaraskan 3 pita frekuensi kuantum agar cocok dengan sinyal target ($targetA / $targetB / $targetC ±6 Hz).",
        style = MaterialTheme.typography.bodyMedium,
        color = TextSecondary
    )

    BandSlider("BAND ALPHA ($bandA / $targetA Hz)", bandA, abs(bandA - targetA) <= 6) { bandA = it }
    BandSlider("BAND BETA ($bandB / $targetB Hz)", bandB, abs(bandB - targetB) <= 6) { bandB = it }
    BandSlider("BAND GAMMA ($bandC / $targetC Hz)", bandC, abs(bandC - targetC) <= 6) { bandC = it }

    Button(
        onClick = onSolved,
        enabled = aligned,
        modifier = Modifier.fillMaxWidth().height(48.dp).testTag("recovery_confirm"),
        colors = ButtonDefaults.buttonColors(containerColor = TacticalEmerald, contentColor = ObsidianVoid)
    ) {
        Text(if (aligned) "SIGNAL LOCKED — EXTRACT DATA" else "ALIGN ALL THREE FREQUENCY BANDS")
    }
}

@Composable
private fun BandSlider(label: String, value: Int, locked: Boolean, onChange: (Int) -> Unit) {
    Column {
        Text(label, style = MaterialTheme.typography.labelSmall, color = if (locked) TacticalEmerald else WarningAmber)
        Slider(
            value = value.toFloat(),
            onValueChange = { onChange(it.toInt()) },
            valueRange = 0f..100f
        )
    }
}

@Composable
fun InventoryModalScreen(
    state: HudUiState,
    onPlayAudioLog: (LoreEntry) -> Unit,
    onClose: () -> Unit
) {
    BackHandler(onBack = onClose)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(ObsidianVoid.copy(alpha = 0.94f))
            .windowInsetsPadding(WindowInsets.safeDrawing)
            .padding(20.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            IconButton(onClick = onClose, modifier = Modifier.size(48.dp).testTag("inventory_close")) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = ElectricCyan)
            }
            Text("INVENTORY & RESEARCH ARCHIVES", style = MaterialTheme.typography.headlineLarge, color = TextPrimary)
        }
        Spacer(Modifier.height(12.dp))

        Row(modifier = Modifier.fillMaxSize(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            // Left: Hardware & Access Items
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .clip(RoundedCornerShape(12.dp))
                    .background(TacticalSlate)
                    .border(1.dp, PanelBorder, RoundedCornerShape(12.dp))
                    .padding(14.dp)
            ) {
                Text("KEYCARDS & DEVICES (${state.inventory.size})", style = MaterialTheme.typography.labelLarge, color = ElectricCyan)
                Spacer(Modifier.height(8.dp))
                if (state.inventory.isEmpty()) {
                    Text("Belum ada item akses yang dikumpulkan di sektor ini.", style = MaterialTheme.typography.bodyMedium, color = TextMuted)
                } else {
                    LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(state.inventory) { item ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(PanelSteel)
                                    .padding(10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Icon(Icons.Default.Key, contentDescription = null, tint = TacticalEmerald)
                                Column {
                                    Text(item.name, style = MaterialTheme.typography.titleMedium, color = TextPrimary)
                                    Text("${item.category.label} — ${item.description}", style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
                                }
                            }
                        }
                    }
                }
            }

            // Right: Collected Lore & Audio Logs
            Column(
                modifier = Modifier
                    .weight(1.2f)
                    .fillMaxHeight()
                    .clip(RoundedCornerShape(12.dp))
                    .background(TacticalSlate)
                    .border(1.dp, PanelBorder, RoundedCornerShape(12.dp))
                    .padding(14.dp)
            ) {
                Text("DOCUMENTS & AUDIO LOGS (${state.collectedLore.size})", style = MaterialTheme.typography.labelLarge, color = WarningAmber)
                Spacer(Modifier.height(8.dp))
                if (state.collectedLore.isEmpty()) {
                    Text("Pindai ruangan dengan Scanner untuk menemukan dokumen & rekaman audio.", style = MaterialTheme.typography.bodyMedium, color = TextMuted)
                } else {
                    LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(state.collectedLore) { lore ->
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(PanelSteel)
                                    .padding(12.dp),
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(lore.title, style = MaterialTheme.typography.titleMedium, color = ElectricCyan)
                                        Text("${lore.author} // ${lore.timestamp}", style = MaterialTheme.typography.labelSmall, color = TextMuted)
                                    }
                                    if (lore.isAudioLog) {
                                        IconButton(onClick = { onPlayAudioLog(lore) }, modifier = Modifier.size(48.dp)) {
                                            Icon(Icons.Default.PlayArrow, contentDescription = "Play Audio Log", tint = TacticalEmerald)
                                        }
                                    } else {
                                        Icon(Icons.Default.GraphicEq, contentDescription = null, tint = TextSecondary)
                                    }
                                }
                                Text(lore.body, style = MaterialTheme.typography.bodyMedium, color = TextPrimary)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun SettingsScreen(
    settings: GameSettings,
    onUpdateSettings: ((GameSettings) -> GameSettings) -> Unit,
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
            IconButton(onClick = onBack, modifier = Modifier.size(48.dp).testTag("settings_back")) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = ElectricCyan)
            }
            Text("SYSTEM & CONTROL SETTINGS", style = MaterialTheme.typography.headlineLarge, color = TextPrimary)
        }
        Spacer(Modifier.height(10.dp))

        Row(
            modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            // Column 1: Graphics & Difficulty
            Column(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(12.dp))
                    .background(TacticalSlate)
                    .border(1.dp, PanelBorder, RoundedCornerShape(12.dp))
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text("GRAPHICS & DIFFICULTY", style = MaterialTheme.typography.labelLarge, color = ElectricCyan)

                Text("Quality Profile", style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    GraphicsQuality.entries.forEach { q ->
                        FilterChip(
                            selected = settings.graphicsQuality == q,
                            onClick = {
                                onUpdateSettings {
                                    it.copy(graphicsQuality = q, resolutionScale = q.renderScale)
                                }
                            },
                            label = { Text(q.name, style = MaterialTheme.typography.labelSmall) }
                        )
                    }
                }

                Text("Difficulty", style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Difficulty.entries.forEach { d ->
                        FilterChip(
                            selected = settings.difficulty == d,
                            onClick = { onUpdateSettings { it.copy(difficulty = d) } },
                            label = { Text(d.label, style = MaterialTheme.typography.labelSmall) }
                        )
                    }
                }

                SettingSlider(
                    label = "Resolution Scale: ${(settings.resolutionScale * 100).toInt()}%",
                    value = settings.resolutionScale,
                    range = 0.6f..1.0f
                ) { v -> onUpdateSettings { it.copy(resolutionScale = v) } }

                SettingSlider(
                    label = "Camera FOV: ${settings.fovDegrees.toInt()}°",
                    value = settings.fovDegrees,
                    range = 60f..88f
                ) { v -> onUpdateSettings { it.copy(fovDegrees = v) } }

                SettingToggle("Camera Shake", settings.cameraShakeEnabled) { c ->
                    onUpdateSettings { it.copy(cameraShakeEnabled = c) }
                }
                SettingToggle("Radio Subtitles", settings.subtitlesEnabled) { c ->
                    onUpdateSettings { it.copy(subtitlesEnabled = c) }
                }
            }

            // Column 2: Controls & Audio Mixer
            Column(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(12.dp))
                    .background(TacticalSlate)
                    .border(1.dp, PanelBorder, RoundedCornerShape(12.dp))
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text("CONTROLS & AUDIO MIXER", style = MaterialTheme.typography.labelLarge, color = ElectricCyan)

                SettingSlider(
                    label = "Look Sensitivity: ${"%.2f".format(settings.lookSensitivity)}x",
                    value = settings.lookSensitivity,
                    range = 0.4f..2.2f
                ) { v -> onUpdateSettings { it.copy(lookSensitivity = v) } }

                SettingSlider(
                    label = "HUD Button Size: ${(settings.buttonSizeScale * 100).toInt()}%",
                    value = settings.buttonSizeScale,
                    range = 0.8f..1.3f
                ) { v -> onUpdateSettings { it.copy(buttonSizeScale = v) } }

                SettingSlider(
                    label = "HUD Opacity: ${(settings.buttonOpacity * 100).toInt()}%",
                    value = settings.buttonOpacity,
                    range = 0.4f..1.0f
                ) { v -> onUpdateSettings { it.copy(buttonOpacity = v) } }

                SettingToggle("Invert Y-Axis", settings.invertY) { c ->
                    onUpdateSettings { it.copy(invertY = c) }
                }
                SettingToggle("Haptic Vibration", settings.vibrationEnabled) { c ->
                    onUpdateSettings { it.copy(vibrationEnabled = c) }
                }

                SettingSlider(
                    label = "Master Volume: ${(settings.masterVolume * 100).toInt()}%",
                    value = settings.masterVolume,
                    range = 0f..1f
                ) { v -> onUpdateSettings { it.copy(masterVolume = v) } }

                SettingSlider(
                    label = "Music / Ambience: ${(settings.musicVolume * 100).toInt()}%",
                    value = settings.musicVolume,
                    range = 0f..1f
                ) { v -> onUpdateSettings { it.copy(musicVolume = v) } }

                SettingSlider(
                    label = "SFX Volume: ${(settings.sfxVolume * 100).toInt()}%",
                    value = settings.sfxVolume,
                    range = 0f..1f
                ) { v -> onUpdateSettings { it.copy(sfxVolume = v) } }
            }
        }
    }
}

@Composable
private fun SettingSlider(
    label: String,
    value: Float,
    range: ClosedFloatingPointRange<Float>,
    onChange: (Float) -> Unit
) {
    Column {
        Text(label, style = MaterialTheme.typography.labelSmall, color = TextPrimary)
        Slider(value = value, onValueChange = onChange, valueRange = range)
    }
}

@Composable
private fun SettingToggle(
    label: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, style = MaterialTheme.typography.bodyMedium, color = TextPrimary)
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}
