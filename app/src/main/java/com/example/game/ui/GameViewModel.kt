package com.example.game.ui

import android.app.Application
import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.game.audio.ProceduralAudioEngine
import com.example.game.audio.SoundEffect
import com.example.game.data.MissionCatalog
import com.example.game.engine.SimulationEngine
import com.example.game.model.EndingType
import com.example.game.model.EnemyState
import com.example.game.model.GameScreen
import com.example.game.model.GameSettings
import com.example.game.model.HudUiState
import com.example.game.model.LoreEntry
import com.example.game.model.MissionId
import com.example.game.save.SaveManager
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class GameViewModel(application: Application) : AndroidViewModel(application) {

    private val saveManager = SaveManager(application)
    val audioEngine = ProceduralAudioEngine()
    val simulation = SimulationEngine()

    private val _uiState = MutableStateFlow(HudUiState())
    val uiState: StateFlow<HudUiState> = _uiState.asStateFlow()

    @Volatile
    private var joyX = 0f
    @Volatile
    private var joyY = 0f
    @Volatile
    private var sprintHeld = false

    private var simLoopJob: Job? = null
    private var radioJob: Job? = null
    private var toastJob: Job? = null
    private var currentFps = 60

    init {
        val loaded = saveManager.load()
        audioEngine.updateSettings(loaded.settings)
        audioEngine.setAmbienceState(ProceduralAudioEngine.AmbienceMode.MENU)
        audioEngine.startAmbience()

        _uiState.update { it.copy(saveData = loaded, currentMission = loaded.currentMission) }
        wireSimulationCallbacks()
    }

    private fun wireSimulationCallbacks() {
        simulation.onPlaySound = { effect, pitch ->
            audioEngine.playSfx(effect, pitch)
        }
        simulation.onRadioMessage = { message ->
            showRadioSubtitle(message)
        }
        simulation.onTriggerPuzzle = { puzzle ->
            _uiState.update {
                it.copy(
                    screen = GameScreen.PUZZLE,
                    activePuzzle = puzzle
                )
            }
            audioEngine.setAmbienceState(ProceduralAudioEngine.AmbienceMode.PAUSED)
        }
        simulation.onCheckpointSaved = { x, z, yaw, objs, inters, inv ->
            val currentSave = _uiState.value.saveData
            val updated = currentSave.copy(
                currentMission = simulation.mission.id,
                checkpointMission = simulation.mission.id,
                checkpointX = x,
                checkpointZ = z,
                checkpointYaw = yaw,
                checkpointCompletedObjectives = objs,
                checkpointActivatedInteractables = inters,
                checkpointInventoryIds = inv,
                collectedLoreIds = currentSave.collectedLoreIds + simulation.collectedLore.map { it.id }
            )
            saveManager.save(updated)
            _uiState.update { it.copy(saveData = updated) }
            showStatusToast("CHECKPOINT DISIMPAN")
        }
        simulation.onOpenEndingChoice = {
            val hasSecretArchive = simulation.inventory.any { it.id == "item_m3_truth" }
            _uiState.update {
                it.copy(
                    endingChoiceAvailable = true,
                    showShadowSynthesisChoice = hasSecretArchive
                )
            }
            audioEngine.setAmbienceState(ProceduralAudioEngine.AmbienceMode.PAUSED)
            showRadioSubtitle("AETHER-9 CORE: Protokol akhir siap. Pilih nasib fasilitas AETHER-9.")
        }
        simulation.onMissionFinished = { stats ->
            stopSimulationLoop()
            val prevSave = _uiState.value.saveData
            val nextSave = if (stats.success) {
                val unlocked = prevSave.unlockedMissions.toMutableSet()
                when (stats.missionId) {
                    MissionId.TRAINING -> unlocked.add(MissionId.MISSION_01)
                    MissionId.MISSION_01 -> unlocked.add(MissionId.MISSION_02)
                    MissionId.MISSION_02 -> unlocked.add(MissionId.MISSION_03)
                    MissionId.MISSION_03 -> Unit
                }
                val bestTimes = prevSave.bestMissionTimesSec.toMutableMap()
                val existingBest = bestTimes[stats.missionId]
                if (existingBest == null || stats.elapsedSeconds < existingBest) {
                    bestTimes[stats.missionId] = stats.elapsedSeconds
                }
                prevSave.copy(
                    unlockedMissions = unlocked,
                    completedMissions = prevSave.completedMissions + stats.missionId,
                    collectedLoreIds = prevSave.collectedLoreIds + simulation.collectedLore.map { it.id },
                    bestMissionTimesSec = bestTimes,
                    checkpointMission = null
                )
            } else {
                prevSave
            }
            saveManager.save(nextSave)
            _uiState.update {
                it.copy(
                    screen = if (stats.success && stats.missionId == MissionId.MISSION_03 && it.activeEnding != null) {
                        GameScreen.ENDING_CINEMATIC
                    } else {
                        GameScreen.MISSION_RESULT
                    },
                    missionResult = stats,
                    saveData = nextSave
                )
            }
            audioEngine.setAmbienceState(ProceduralAudioEngine.AmbienceMode.MENU)
        }
        simulation.onHapticPulse = {
            triggerHaptic(35L)
        }
    }

    fun startNewCampaign() {
        audioEngine.playSfx(SoundEffect.UI_CLICK, 1f)
        val fresh = saveManager.resetCampaignKeepingSettings(_uiState.value.saveData)
        _uiState.update { it.copy(saveData = fresh) }
        openMissionBriefing(MissionId.MISSION_01)
    }

    fun continueCampaign() {
        audioEngine.playSfx(SoundEffect.UI_CLICK, 1f)
        val save = _uiState.value.saveData
        if (save.checkpointMission != null) {
            launchMission(save.checkpointMission, fromCheckpoint = true)
        } else {
            openMissionBriefing(save.currentMission)
        }
    }

    fun openCampaignSelect() {
        audioEngine.playSfx(SoundEffect.UI_CLICK, 1f)
        _uiState.update { it.copy(screen = GameScreen.CAMPAIGN_SELECT) }
    }

    fun openMissionBriefing(missionId: MissionId) {
        audioEngine.playSfx(SoundEffect.UI_CLICK, 1f)
        _uiState.update {
            it.copy(
                currentMission = missionId,
                screen = GameScreen.BRIEFING
            )
        }
    }

    fun launchMission(missionId: MissionId, fromCheckpoint: Boolean = false) {
        audioEngine.playSfx(SoundEffect.TERMINAL_BEEP, 1f)
        val save = _uiState.value.saveData
        simulation.loadMission(missionId, save, fromCheckpoint)
        val updatedSave = save.copy(currentMission = missionId)
        saveManager.save(updatedSave)

        _uiState.update {
            it.copy(
                screen = GameScreen.PLAYING,
                currentMission = missionId,
                activeEnding = null,
                endingChoiceAvailable = false,
                missionResult = null,
                activePuzzle = null,
                saveData = updatedSave
            )
        }
        syncUiFromSimulation()
        startSimulationLoop()
        audioEngine.setAmbienceState(ProceduralAudioEngine.AmbienceMode.EXPLORATION)

        val def = MissionCatalog.getMission(missionId)
        def.openingRadio.firstOrNull()?.let { firstLine ->
            showRadioSubtitle(firstLine)
        }
    }

    fun pauseGame() {
        if (_uiState.value.screen != GameScreen.PLAYING) return
        audioEngine.playSfx(SoundEffect.UI_CLICK, 0.9f)
        stopSimulationLoop()
        audioEngine.setAmbienceState(ProceduralAudioEngine.AmbienceMode.PAUSED)
        _uiState.update { it.copy(screen = GameScreen.PAUSED) }
    }

    fun resumeGame() {
        audioEngine.playSfx(SoundEffect.UI_CLICK, 1.1f)
        _uiState.update {
            it.copy(
                screen = GameScreen.PLAYING,
                activePuzzle = null,
                endingChoiceAvailable = false
            )
        }
        audioEngine.setAmbienceState(ProceduralAudioEngine.AmbienceMode.EXPLORATION)
        startSimulationLoop()
    }

    fun restartCheckpoint() {
        audioEngine.playSfx(SoundEffect.TERMINAL_BEEP, 1f)
        simulation.respawnAtCheckpoint()
        resumeGame()
        showStatusToast("DIMUAT DARI CHECKPOINT")
    }

    fun openInventory() {
        audioEngine.playSfx(SoundEffect.UI_CLICK, 1f)
        stopSimulationLoop()
        audioEngine.setAmbienceState(ProceduralAudioEngine.AmbienceMode.PAUSED)
        _uiState.update { it.copy(screen = GameScreen.INVENTORY) }
    }

    fun openSettings(fromScreen: GameScreen) {
        audioEngine.playSfx(SoundEffect.UI_CLICK, 1f)
        if (fromScreen == GameScreen.PLAYING) {
            stopSimulationLoop()
        }
        returnFromSettingsScreen = fromScreen
        _uiState.update { it.copy(screen = GameScreen.SETTINGS) }
    }

    private var returnFromSettingsScreen = GameScreen.MAIN_MENU

    fun closeSettings() {
        audioEngine.playSfx(SoundEffect.UI_CLICK, 0.9f)
        _uiState.update { it.copy(screen = returnFromSettingsScreen) }
        if (returnFromSettingsScreen == GameScreen.PLAYING) {
            audioEngine.setAmbienceState(ProceduralAudioEngine.AmbienceMode.EXPLORATION)
            startSimulationLoop()
        }
    }

    fun openCredits() {
        audioEngine.playSfx(SoundEffect.UI_CLICK, 1f)
        _uiState.update { it.copy(screen = GameScreen.CREDITS) }
    }

    fun returnToMainMenu() {
        audioEngine.playSfx(SoundEffect.UI_CLICK, 0.85f)
        stopSimulationLoop()
        audioEngine.setAmbienceState(ProceduralAudioEngine.AmbienceMode.MENU)
        _uiState.update {
            it.copy(
                screen = GameScreen.MAIN_MENU,
                activePuzzle = null,
                endingChoiceAvailable = false,
                missionResult = null
            )
        }
    }

    fun updateSettings(transform: (GameSettings) -> GameSettings) {
        val current = _uiState.value.saveData
        val updatedSettings = transform(current.settings)
        val updatedSave = current.copy(settings = updatedSettings)
        audioEngine.updateSettings(updatedSettings)
        saveManager.save(updatedSave)
        _uiState.update { it.copy(saveData = updatedSave) }
    }

    fun setMoveInput(x: Float, y: Float) {
        joyX = x
        joyY = y
    }

    fun setSprintHeld(held: Boolean) {
        sprintHeld = held
    }

    fun onLookDelta(dx: Float, dy: Float) {
        if (_uiState.value.screen != GameScreen.PLAYING) return
        simulation.applyLookDelta(dx, dy, _uiState.value.saveData.settings)
    }

    fun onToggleCrouch() {
        if (_uiState.value.screen != GameScreen.PLAYING) return
        simulation.toggleCrouch()
        syncUiFromSimulation()
    }

    fun onJump() {
        if (_uiState.value.screen != GameScreen.PLAYING) return
        simulation.jump()
        syncUiFromSimulation()
    }

    fun onActivateScanner() {
        if (_uiState.value.screen != GameScreen.PLAYING) return
        simulation.activateScanner()
        syncUiFromSimulation()
    }

    fun onDeployDecoy() {
        if (_uiState.value.screen != GameScreen.PLAYING) return
        simulation.deployDecoy()
        syncUiFromSimulation()
    }

    fun onActivateShield() {
        if (_uiState.value.screen != GameScreen.PLAYING) return
        simulation.activateEnergyShield()
        syncUiFromSimulation()
    }

    fun onInteract() {
        if (_uiState.value.screen != GameScreen.PLAYING) return
        val msg = simulation.interactWithFocused()
        if (!msg.isNullOrBlank()) {
            showStatusToast(msg)
        }
        syncUiFromSimulation()
    }

    fun onPuzzleSolved() {
        val puzzle = _uiState.value.activePuzzle ?: return
        audioEngine.playSfx(SoundEffect.PUZZLE_SUCCESS, 1f)
        val msg = simulation.completeInteractableActivation(puzzle.interactableId)
        _uiState.update {
            it.copy(
                screen = GameScreen.PLAYING,
                activePuzzle = null
            )
        }
        if (msg.isNotBlank()) showStatusToast(msg)
        audioEngine.setAmbienceState(ProceduralAudioEngine.AmbienceMode.EXPLORATION)
        syncUiFromSimulation()
        startSimulationLoop()
    }

    fun onPuzzleCancelled() {
        audioEngine.playSfx(SoundEffect.UI_CLICK, 0.85f)
        _uiState.update {
            it.copy(
                screen = GameScreen.PLAYING,
                activePuzzle = null
            )
        }
        audioEngine.setAmbienceState(ProceduralAudioEngine.AmbienceMode.EXPLORATION)
        startSimulationLoop()
    }

    fun selectEndingDecision(ending: EndingType) {
        audioEngine.playSfx(SoundEffect.MISSION_COMPLETE, 1f)
        simulation.disableNearestEnemyFromCore()
        val prevSave = _uiState.value.saveData
        val updatedSave = prevSave.copy(unlockedEndings = prevSave.unlockedEndings + ending)
        saveManager.save(updatedSave)

        _uiState.update {
            it.copy(
                endingChoiceAvailable = false,
                activeEnding = ending,
                saveData = updatedSave
            )
        }
        audioEngine.setAmbienceState(ProceduralAudioEngine.AmbienceMode.EXPLORATION)
        showRadioSubtitle("PROTOKOL ${ending.code} AKTIF: Segera menuju Emergency Exit di sektor barat!")
        showStatusToast("Pintu Evakuasi Terbuka!")
        syncUiFromSimulation()
    }

    fun playLoreAudio(lore: LoreEntry) {
        audioEngine.playRadioCue()
        showRadioSubtitle("${lore.title}: ${lore.body}")
    }

    fun updateFps(fps: Int) {
        currentFps = fps
    }

    fun onAppPaused() {
        if (_uiState.value.screen == GameScreen.PLAYING) {
            pauseGame()
        }
        audioEngine.setAmbienceState(ProceduralAudioEngine.AmbienceMode.PAUSED)
    }

    fun onAppResumed() {
        if (_uiState.value.screen == GameScreen.MAIN_MENU) {
            audioEngine.setAmbienceState(ProceduralAudioEngine.AmbienceMode.MENU)
        }
    }

    private fun startSimulationLoop() {
        simLoopJob?.cancel()
        simLoopJob = viewModelScope.launch {
            var lastNs = System.nanoTime()
            var uiCounter = 0
            while (true) {
                val now = System.nanoTime()
                val dt = ((now - lastNs) / 1_000_000_000f).coerceIn(0.004f, 0.05f)
                lastNs = now

                val state = _uiState.value
                if (state.screen == GameScreen.PLAYING && !state.endingChoiceAvailable) {
                    simulation.step(dt, joyX, joyY, sprintHeld, state.saveData.settings)
                    uiCounter++
                    if (uiCounter >= 2) {
                        uiCounter = 0
                        syncUiFromSimulation()
                    }
                }
                delay(16L)
            }
        }
    }

    private fun stopSimulationLoop() {
        simLoopJob?.cancel()
        simLoopJob = null
        joyX = 0f
        joyY = 0f
        sprintHeld = false
    }

    private fun syncUiFromSimulation() {
        val maxDet = simulation.enemies.maxOfOrNull { it.detectionMeter } ?: 0f
        val anyAlert = simulation.enemies.any { it.state == EnemyState.ALERT }

        audioEngine.setAmbienceState(
            mode = if (anyAlert || maxDet > 35f) {
                ProceduralAudioEngine.AmbienceMode.SUSPENSE
            } else {
                ProceduralAudioEngine.AmbienceMode.EXPLORATION
            },
            suspense = (maxDet / 100f).coerceIn(0f, 1f)
        )

        val rt = Runtime.getRuntime()
        val usedMb = ((rt.totalMemory() - rt.freeMemory()) / (1024 * 1024)).toInt()

        _uiState.update {
            it.copy(
                currentRoomName = simulation.currentRoomName(),
                player = simulation.player,
                objectives = simulation.objectives,
                focusedInteractable = simulation.getFocusedInteractable(),
                interactables = simulation.interactables,
                enemies = simulation.enemies,
                decoyBeacon = simulation.decoyBeacon,
                inventory = simulation.inventory.toList(),
                collectedLore = simulation.collectedLore.toList(),
                maxEnemyDetection = maxDet,
                anyEnemyAlert = anyAlert,
                missionElapsedSec = simulation.elapsedTimeSec.toInt(),
                timesDetected = simulation.timesDetected,
                fpsEstimate = currentFps,
                ramUsageMb = usedMb
            )
        }
    }

    private fun showRadioSubtitle(msg: String) {
        if (!_uiState.value.saveData.settings.subtitlesEnabled) return
        audioEngine.playRadioCue()
        radioJob?.cancel()
        radioJob = viewModelScope.launch {
            _uiState.update { it.copy(radioSubtitle = msg) }
            delay(6500L)
            _uiState.update { it.copy(radioSubtitle = null) }
        }
    }

    private fun showStatusToast(msg: String) {
        toastJob?.cancel()
        toastJob = viewModelScope.launch {
            _uiState.update { it.copy(statusToast = msg) }
            delay(2800L)
            _uiState.update { it.copy(statusToast = null) }
        }
    }

    private fun triggerHaptic(durationMs: Long) {
        if (!_uiState.value.saveData.settings.vibrationEnabled) return
        val app = getApplication<Application>()
        runCatching {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val vm = app.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
                vm.defaultVibrator.vibrate(
                    VibrationEffect.createOneShot(durationMs, VibrationEffect.DEFAULT_AMPLITUDE)
                )
            } else {
                @Suppress("DEPRECATION")
                val v = app.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    v.vibrate(VibrationEffect.createOneShot(durationMs, VibrationEffect.DEFAULT_AMPLITUDE))
                } else {
                    @Suppress("DEPRECATION")
                    v.vibrate(durationMs)
                }
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        stopSimulationLoop()
        audioEngine.stopAmbience()
    }
}
