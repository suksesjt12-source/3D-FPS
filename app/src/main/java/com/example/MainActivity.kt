package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.game.model.EndingType
import com.example.game.model.GameScreen
import com.example.game.model.MissionId
import com.example.game.render.GameGLSurfaceView
import com.example.game.ui.BriefingScreen
import com.example.game.ui.CampaignSelectScreen
import com.example.game.ui.CreditsScreen
import com.example.game.ui.EndingChoiceModal
import com.example.game.ui.EndingCinematicScreen
import com.example.game.ui.GameViewModel
import com.example.game.ui.GameplayHud
import com.example.game.ui.InventoryModalScreen
import com.example.game.ui.MainMenuScreen
import com.example.game.ui.MissionResultScreen
import com.example.game.ui.PauseMenuOverlay
import com.example.game.ui.PuzzleModalScreen
import com.example.game.ui.SettingsScreen
import com.example.game.ui.TouchControlsOverlay
import com.example.ui.theme.OperationShadowTheme

class MainActivity : ComponentActivity() {

    private val viewModel: GameViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        hideSystemBars()

        setContent {
            OperationShadowTheme {
                val state by viewModel.uiState.collectAsStateWithLifecycle()
                val inMissionViewport = state.screen == GameScreen.PLAYING ||
                    state.screen == GameScreen.PAUSED ||
                    state.screen == GameScreen.PUZZLE ||
                    state.screen == GameScreen.INVENTORY

                Box(modifier = Modifier.fillMaxSize()) {
                    if (inMissionViewport) {
                        BackHandler(enabled = state.screen == GameScreen.PLAYING) {
                            viewModel.pauseGame()
                        }

                        GameGLSurfaceView(
                            simulation = viewModel.simulation,
                            settings = state.saveData.settings,
                            isPlaying = state.screen == GameScreen.PLAYING,
                            onFpsMeasured = viewModel::updateFps,
                            modifier = Modifier.fillMaxSize()
                        )

                        if (state.screen == GameScreen.PLAYING && !state.endingChoiceAvailable) {
                            TouchControlsOverlay(
                                player = state.player,
                                focusedInteractable = state.focusedInteractable,
                                settings = state.saveData.settings,
                                onMoveInput = viewModel::setMoveInput,
                                onSprintHeld = viewModel::setSprintHeld,
                                onLookDelta = viewModel::onLookDelta,
                                onToggleCrouch = viewModel::onToggleCrouch,
                                onJump = viewModel::onJump,
                                onInteract = viewModel::onInteract,
                                onScanner = viewModel::onActivateScanner,
                                onDecoy = viewModel::onDeployDecoy,
                                onShield = viewModel::onActivateShield
                            )
                        }

                        GameplayHud(
                            state = state,
                            onPause = viewModel::pauseGame,
                            onOpenInventory = viewModel::openInventory
                        )

                        if (state.endingChoiceAvailable) {
                            EndingChoiceModal(
                                showShadowSynthesis = state.showShadowSynthesisChoice,
                                onSelectEnding = viewModel::selectEndingDecision
                            )
                        }
                    }

                    when (state.screen) {
                        GameScreen.MAIN_MENU -> MainMenuScreen(
                            state = state,
                            onNewGame = viewModel::startNewCampaign,
                            onContinue = viewModel::continueCampaign,
                            onCampaignSelect = viewModel::openCampaignSelect,
                            onTraining = { viewModel.openMissionBriefing(MissionId.TRAINING) },
                            onSettings = { viewModel.openSettings(GameScreen.MAIN_MENU) },
                            onCredits = viewModel::openCredits
                        )

                        GameScreen.CAMPAIGN_SELECT -> CampaignSelectScreen(
                            state = state,
                            onSelectMission = viewModel::openMissionBriefing,
                            onBack = viewModel::returnToMainMenu
                        )

                        GameScreen.BRIEFING -> BriefingScreen(
                            missionId = state.currentMission,
                            onDeploy = { viewModel.launchMission(state.currentMission) },
                            onBack = viewModel::returnToMainMenu
                        )

                        GameScreen.PAUSED -> PauseMenuOverlay(
                            onResume = viewModel::resumeGame,
                            onRestartCheckpoint = viewModel::restartCheckpoint,
                            onSettings = { viewModel.openSettings(GameScreen.PLAYING) },
                            onExitMenu = viewModel::returnToMainMenu
                        )

                        GameScreen.PUZZLE -> state.activePuzzle?.let { puzzle ->
                            PuzzleModalScreen(
                                puzzle = puzzle,
                                onSolved = viewModel::onPuzzleSolved,
                                onCancel = viewModel::onPuzzleCancelled
                            )
                        }

                        GameScreen.INVENTORY -> InventoryModalScreen(
                            state = state,
                            onPlayAudioLog = viewModel::playLoreAudio,
                            onClose = viewModel::resumeGame
                        )

                        GameScreen.SETTINGS -> SettingsScreen(
                            settings = state.saveData.settings,
                            onUpdateSettings = viewModel::updateSettings,
                            onBack = viewModel::closeSettings
                        )

                        GameScreen.MISSION_RESULT -> MissionResultScreen(
                            state = state,
                            onRetry = { viewModel.launchMission(state.currentMission) },
                            onContinueNext = { next -> viewModel.openMissionBriefing(next) },
                            onMainMenu = viewModel::returnToMainMenu
                        )

                        GameScreen.ENDING_CINEMATIC -> EndingCinematicScreen(
                            ending = state.activeEnding ?: EndingType.NEURAL_CONTAINMENT,
                            onDone = viewModel::returnToMainMenu
                        )

                        GameScreen.CREDITS -> CreditsScreen(
                            onBack = viewModel::returnToMainMenu
                        )

                        GameScreen.PLAYING -> Unit
                    }
                }
            }
        }
    }

    override fun onPause() {
        super.onPause()
        viewModel.onAppPaused()
    }

    override fun onResume() {
        super.onResume()
        hideSystemBars()
        viewModel.onAppResumed()
    }

    private fun hideSystemBars() {
        val controller = WindowCompat.getInsetsController(window, window.decorView)
        controller.systemBarsBehavior =
            WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        controller.hide(WindowInsetsCompat.Type.systemBars())
    }
}
