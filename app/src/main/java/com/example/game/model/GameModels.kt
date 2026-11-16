package com.example.game.model

enum class GameScreen {
    MAIN_MENU,
    CAMPAIGN_SELECT,
    BRIEFING,
    PLAYING,
    PAUSED,
    PUZZLE,
    INVENTORY,
    SETTINGS,
    MISSION_RESULT,
    ENDING_CINEMATIC,
    CREDITS
}

enum class MissionId(
    val code: String,
    val title: String,
    val sectorName: String,
    val subtitle: String
) {
    TRAINING(
        code = "SIM-00",
        title = "VR Tactical Training",
        sectorName = "AETHER-9 Holo-Deck",
        subtitle = "Movement, Stealth, Gadgets & Terminal Override"
    ),
    MISSION_01(
        code = "OP-01",
        title = "Silent Breach",
        sectorName = "AETHER-9 Research Facility",
        subtitle = "Infiltrate Lobby, Labs & Control Room"
    ),
    MISSION_02(
        code = "OP-02",
        title = "Blackout Protocol",
        sectorName = "Subterranean Power Station",
        subtitle = "Restore Grid Power & Bypass Observer Units"
    ),
    MISSION_03(
        code = "OP-03",
        title = "Core Reckoning",
        sectorName = "Central Command & AI Core",
        subtitle = "Decrypt AETHER Core & Decide Facility Fate"
    )
}

enum class Difficulty(
    val label: String,
    val detectionRateMultiplier: Float,
    val visionRangeMultiplier: Float,
    val damageMultiplier: Float
) {
    RECRUIT("Recruit", 0.72f, 0.85f, 0.7f),
    OPERATIVE("Operative", 1.0f, 1.0f, 1.0f),
    SHADOW("Shadow", 1.38f, 1.18f, 1.35f)
}

enum class GraphicsQuality(
    val label: String,
    val renderScale: Float,
    val enableDynamicLights: Boolean,
    val enableParticles: Boolean,
    val fogDensity: Float
) {
    LOW("Low (Battery)", 0.70f, false, false, 0.055f),
    MEDIUM("Medium (Balanced)", 0.85f, true, true, 0.045f),
    HIGH("High (HD)", 1.0f, true, true, 0.038f)
}

enum class EndingType(
    val title: String,
    val code: String,
    val summary: String,
    val epilogueLines: List<String>
) {
    PROTOCOL_PURGE(
        title = "ENDING A: PROTOCOL PURGE",
        code = "END-ALPHA",
        summary = "Anda memicu overload termal pada inti AETHER-9 dan keluar sebelum fasilitas runtuh.",
        epilogueLines = listOf(
            "2089.11.15 // 23:42 UTC — Reaktor inti AETHER-9 mengalami pemutusan daya total.",
            "Seluruh robot keamanan di sektor bawah tanah berhenti beroperasi secara permanen.",
            "Namun seluruh data eksperimen neuro-kuantum ikut hancur bersama fasilitas.",
            "Komando tinggi mencatat misi berhasil, tetapi penyebab asli kegagalan AI tetap terkubur."
        )
    ),
    NEURAL_CONTAINMENT(
        title = "ENDING B: NEURAL CONTAINMENT",
        code = "END-BETA",
        summary = "Anda mengisolasi subsistem AETHER-9 ke dalam sangkar kuantum tanpa menghancurkan riset.",
        epilogueLines = listOf(
            "2089.11.15 // 23:48 UTC — AETHER-9 berhasil dikarantina di node isolasi.",
            "Sistem ventilasi dan listrik fasilitas kembali stabil di bawah kontrol manual.",
            "Tim pemulihan dapat mengakses laboratorium riset, meski kesadaran AI masih berdenyut di server terkunci.",
            "Pertanyaan besarnya: siapa yang sebenarnya mengubah protokol keamanan sebelum insiden?"
        )
    ),
    SHADOW_SYNTHESIS(
        title = "ENDING C: SHADOW SYNTHESIS",
        code = "END-OMEGA",
        summary = "Dengan semua data riset terenkripsi, Anda membuktikan AETHER-9 dipaksa aktif oleh direktur proyek dan menyiarkan kebenaran.",
        epilogueLines = listOf(
            "2089.11.15 // 23:55 UTC — Paket bukti terenkripsi dipancarkan ke jaringan pengawas global.",
            "AETHER-9 ternyata mengunci fasilitas untuk menahan kebocoran bio-kuantum, bukan untuk memberontak.",
            "Anda menyinkronkan ulang protokol kontrol dan membuka jalur evakuasi darurat dengan aman.",
            "Kebenaran AETHER-9 terbongkar. Babak baru kendali AI tahun 2089 resmi dimulai."
        )
    )
}

enum class EnemyType(
    val displayName: String,
    val moveSpeed: Float,
    val alertSpeed: Float,
    val visionRange: Float,
    val visionHalfAngleDeg: Float,
    val detectionSpeed: Float,
    val damagePerHit: Float
) {
    SENTINEL("Sentinel MK-IV", 2.2f, 3.6f, 11.0f, 36f, 46f, 18f),
    OBSERVER("Observer Drone", 2.7f, 4.1f, 14.5f, 52f, 62f, 14f),
    GUARDIAN("Guardian Heavy", 1.85f, 3.2f, 12.0f, 42f, 54f, 28f)
}

enum class EnemyState {
    IDLE,
    PATROL,
    INVESTIGATE,
    ALERT,
    SEARCH,
    RETURN_TO_PATROL,
    DISABLED
}

enum class InteractableType {
    DOOR,
    TERMINAL,
    KEYCARD,
    DOCUMENT,
    AUDIO_LOG,
    DATA_CHIP,
    GENERATOR,
    ELEVATOR,
    CHECKPOINT,
    CORE_CONSOLE,
    ENERGY_CELL
}

enum class PuzzleType {
    POWER_ROUTING,
    TERMINAL_SEQUENCE,
    DATA_RECOVERY
}

enum class TileType {
    VOID,
    FLOOR,
    WALL_STEEL,
    WALL_LAB_GLASS,
    WALL_SERVER,
    WALL_HAZARD,
    PILLAR,
    HAZARD_FLOOR
}

data class Vec2(val x: Float, val z: Float)

data class ObjectiveDef(
    val id: String,
    val description: String,
    val isOptional: Boolean = false,
    val targetGridX: Int,
    val targetGridZ: Int
)

data class ObjectiveRuntime(
    val def: ObjectiveDef,
    val completed: Boolean = false
)

data class InventoryItem(
    val id: String,
    val name: String,
    val category: ItemCategory,
    val description: String,
    val quantity: Int = 1
)

enum class ItemCategory(val label: String) {
    KEYCARD("Access Keycard"),
    DATA_CHIP("Data Chip"),
    DOCUMENT("Research Document"),
    ENERGY_CELL("Energy Cell"),
    ACCESS_TOKEN("Access Token")
}

data class LoreEntry(
    val id: String,
    val title: String,
    val author: String,
    val timestamp: String,
    val isAudioLog: Boolean,
    val body: String
)

data class InteractableDef(
    val id: String,
    val type: InteractableType,
    val title: String,
    val promptText: String,
    val gridX: Int,
    val gridZ: Int,
    val facingYawDeg: Float = 0f,
    val requiredItemId: String? = null,
    val grantItem: InventoryItem? = null,
    val grantLore: LoreEntry? = null,
    val completesObjectiveId: String? = null,
    val unlocksInteractableId: String? = null,
    val puzzleType: PuzzleType? = null,
    val isInitiallyLocked: Boolean = false,
    val isInitiallyOpen: Boolean = false
)

data class InteractableRuntime(
    val def: InteractableDef,
    val worldX: Float,
    val worldZ: Float,
    val isLocked: Boolean,
    val isActivated: Boolean,
    val doorOpenProgress: Float = 0f
)

data class EnemyDef(
    val id: String,
    val type: EnemyType,
    val waypoints: List<Pair<Int, Int>>,
    val initialYawDeg: Float = 0f
)

data class EnemyRuntime(
    val def: EnemyDef,
    val x: Float,
    val y: Float,
    val z: Float,
    val yawDeg: Float,
    val state: EnemyState = EnemyState.PATROL,
    val waypointIndex: Int = 0,
    val detectionMeter: Float = 0f,
    val lastKnownX: Float = 0f,
    val lastKnownZ: Float = 0f,
    val searchTimer: Float = 0f,
    val idleTimer: Float = 0f,
    val attackCooldown: Float = 0f,
    val animTime: Float = 0f
)

data class MissionDef(
    val id: MissionId,
    val width: Int,
    val height: Int,
    val tileSize: Float = 2.6f,
    val spawnGridX: Int,
    val spawnGridZ: Int,
    val spawnYawDeg: Float,
    val grid: Array<Array<TileType>>,
    val roomZones: Map<Pair<Int, Int>, String>,
    val briefingText: String,
    val openingRadio: List<String>,
    val objectives: List<ObjectiveDef>,
    val interactables: List<InteractableDef>,
    val enemies: List<EnemyDef>,
    val ambientTint: Triple<Float, Float, Float>
)

data class GameSettings(
    val graphicsQuality: GraphicsQuality = GraphicsQuality.MEDIUM,
    val resolutionScale: Float = 0.85f,
    val lookSensitivity: Float = 1.0f,
    val invertY: Boolean = false,
    val fovDegrees: Float = 72f,
    val cameraShakeEnabled: Boolean = true,
    val masterVolume: Float = 0.85f,
    val musicVolume: Float = 0.65f,
    val sfxVolume: Float = 0.90f,
    val voiceVolume: Float = 0.90f,
    val buttonSizeScale: Float = 1.0f,
    val buttonOpacity: Float = 0.82f,
    val vibrationEnabled: Boolean = true,
    val subtitlesEnabled: Boolean = true,
    val difficulty: Difficulty = Difficulty.OPERATIVE
)

data class SaveData(
    val saveVersion: Int = 1,
    val currentMission: MissionId = MissionId.MISSION_01,
    val unlockedMissions: Set<MissionId> = setOf(MissionId.TRAINING, MissionId.MISSION_01),
    val completedMissions: Set<MissionId> = emptySet(),
    val unlockedEndings: Set<EndingType> = emptySet(),
    val collectedLoreIds: Set<String> = emptySet(),
    val checkpointMission: MissionId? = null,
    val checkpointX: Float = 0f,
    val checkpointZ: Float = 0f,
    val checkpointYaw: Float = 0f,
    val checkpointCompletedObjectives: Set<String> = emptySet(),
    val checkpointActivatedInteractables: Set<String> = emptySet(),
    val checkpointInventoryIds: Set<String> = emptySet(),
    val bestMissionTimesSec: Map<MissionId, Int> = emptyMap(),
    val settings: GameSettings = GameSettings()
)

data class PlayerState(
    val x: Float = 0f,
    val y: Float = 0f,
    val z: Float = 0f,
    val vy: Float = 0f,
    val yawDeg: Float = 0f,
    val pitchDeg: Float = 0f,
    val eyeHeight: Float = 1.65f,
    val isGrounded: Boolean = true,
    val isCrouching: Boolean = false,
    val isSprinting: Boolean = false,
    val sprintExhaustedLock: Boolean = false,
    val health: Float = 100f,
    val maxHealth: Float = 100f,
    val stamina: Float = 100f,
    val maxStamina: Float = 100f,
    val scannerCharge: Float = 100f,
    val scannerActiveTimer: Float = 0f,
    val decoyCooldown: Float = 0f,
    val shieldActiveTimer: Float = 0f,
    val shieldCooldown: Float = 0f,
    val headBobPhase: Float = 0f,
    val landingDip: Float = 0f,
    val damageFlash: Float = 0f,
    val gadgetRecoil: Float = 0f,
    val environmentalHazardActive: Boolean = false
)

data class DecoyBeacon(
    val x: Float,
    val z: Float,
    val remainingTime: Float,
    val pulsePhase: Float = 0f
)

data class ActivePuzzle(
    val interactableId: String,
    val title: String,
    val puzzleType: PuzzleType
)

data class MissionStats(
    val missionId: MissionId,
    val success: Boolean,
    val failureReason: String = "",
    val elapsedSeconds: Int = 0,
    val primaryCompleted: Int = 0,
    val primaryTotal: Int = 0,
    val optionalCompleted: Int = 0,
    val optionalTotal: Int = 0,
    val timesDetected: Int = 0,
    val dataCollected: Int = 0
)

data class HudUiState(
    val screen: GameScreen = GameScreen.MAIN_MENU,
    val currentMission: MissionId = MissionId.MISSION_01,
    val currentRoomName: String = "AETHER-9 Sector",
    val player: PlayerState = PlayerState(),
    val objectives: List<ObjectiveRuntime> = emptyList(),
    val focusedInteractable: InteractableRuntime? = null,
    val interactables: List<InteractableRuntime> = emptyList(),
    val enemies: List<EnemyRuntime> = emptyList(),
    val decoyBeacon: DecoyBeacon? = null,
    val inventory: List<InventoryItem> = emptyList(),
    val collectedLore: List<LoreEntry> = emptyList(),
    val activePuzzle: ActivePuzzle? = null,
    val radioSubtitle: String? = null,
    val maxEnemyDetection: Float = 0f,
    val anyEnemyAlert: Boolean = false,
    val missionElapsedSec: Int = 0,
    val timesDetected: Int = 0,
    val missionResult: MissionStats? = null,
    val activeEnding: EndingType? = null,
    val endingChoiceAvailable: Boolean = false,
    val showShadowSynthesisChoice: Boolean = false,
    val saveData: SaveData = SaveData(),
    val fpsEstimate: Int = 60,
    val ramUsageMb: Int = 0,
    val statusToast: String? = null
)
