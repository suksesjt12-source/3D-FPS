# Operation Shadow — 3D First-Person Action Stealth (Android)

## Overview
**Operation Shadow** is a single-player, offline 3D first-person sci-fi action stealth game set in the subterranean **AETHER-9** research complex in 2089.

> **Environment Note:** This project is implemented natively for Android in **Kotlin + OpenGL ES 3.0 + Jetpack Compose** so that it compiles and runs directly on Android ARM64/x86_64 devices and emulators without requiring the Unity 6.3 LTS Editor binary.

## Implemented Architecture (`app/src/main/java/com/example/`)
- `game/model/GameModels.kt`: Mission, Objective, AI State, Interactable, Puzzle, Inventory, Lore, Ending, Settings, and Save schemas.
- `game/data/MissionCatalog.kt`: 4 complete playable 3D levels (`SIM-00 VR Training`, `OP-01 Research Facility`, `OP-02 Power Station`, `OP-03 Central Command`), room zones, primary/optional objectives, keycards, documents, audio logs, and robot patrol routes.
- `game/engine/Math3D.kt` & `SimulationEngine.kt`: 60Hz first-person physics (walk, sprint, crouch, jump, gravity, smooth wall collision, stamina/exhaustion, health, electrical floor hazards), universal raycast interaction, 3 gadgets (`Environmental Scanner`, `Decoy Device`, `Energy Shield`), and full Enemy AI state machine (`IDLE`, `PATROL`, `INVESTIGATE`, `ALERT`, `SEARCH`, `RETURN_TO_PATROL`, `DISABLED`) with vision cones, line-of-sight wall occlusion, sound investigation, and difficulty scaling.
- `game/render/`: OpenGL ES 3.0 renderer (`ShadowGLRenderer.kt`), custom GLSL ES 3.00 shaders (headlamp spotlight, dynamic alert/scanner lights, exponential depth fog, metallic specular sheen), procedural PBR-inspired textures (`TextureAtlasBuilder.kt`), batched 3D world geometry, articulated 3D Sentinel/Observer/Guardian robots, floor vision wedges, and 1st-person cyber-forearm viewmodel (`MeshBuilder.kt`).
- `game/audio/ProceduralAudioEngine.kt`: Real-time `AudioTrack` PCM synthesizer for footsteps, hydraulic doors, scanner sweeps, decoy sonar, shield activation, alert sirens, puzzle feedback, and dynamic exploration/suspense ambience.
- `game/save/SaveManager.kt`: Atomic local JSON save/backup system with schema versioning, checkpoint recovery, mission unlocks, endings, lore, and control/graphics/audio settings.
- `game/ui/`: Multi-touch landscape controls (`TouchControlsOverlay.kt`), tactical mini-radar & HUD (`GameplayHud.kt`), Campaign/Briefing/Pause/Result/3-Ending screens (`MenuScreens.kt`), and 3 interactive sci-fi puzzles + Inventory/Audio Logs + Settings (`OverlayScreens.kt`).

## Android Build Instructions
```bash
gradle assembleDebug
gradle :app:testDebugUnitTest
```
Release APK / AAB exports require configuring release keystore environment variables (`KEYSTORE_PATH`, `STORE_PASSWORD`, `KEY_PASSWORD`) in your CI or build environment without committing private keystores to version control.
