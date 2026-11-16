package com.example.game.engine

import com.example.game.audio.SoundEffect
import com.example.game.data.MissionCatalog
import com.example.game.model.ActivePuzzle
import com.example.game.model.DecoyBeacon
import com.example.game.model.EnemyRuntime
import com.example.game.model.EnemyState
import com.example.game.model.GameSettings
import com.example.game.model.InteractableRuntime
import com.example.game.model.InteractableType
import com.example.game.model.InventoryItem
import com.example.game.model.LoreEntry
import com.example.game.model.MissionDef
import com.example.game.model.MissionId
import com.example.game.model.MissionStats
import com.example.game.model.ObjectiveRuntime
import com.example.game.model.PlayerState
import com.example.game.model.SaveData
import com.example.game.model.TileType
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin

class SimulationEngine {

    var mission: MissionDef = MissionCatalog.getMission(MissionId.MISSION_01)
        private set

    var player = PlayerState()
        private set

    var objectives = listOf<ObjectiveRuntime>()
        private set

    var interactables = listOf<InteractableRuntime>()
        private set

    var enemies = listOf<EnemyRuntime>()
        private set

    var decoyBeacon: DecoyBeacon? = null
        private set

    val inventory = mutableListOf<InventoryItem>()
    val collectedLore = mutableListOf<LoreEntry>()

    var elapsedTimeSec = 0f
        private set

    var timesDetected = 0
        private set

    private var footstepTimer = 0f
    private var checkpointX = 0f
    private var checkpointZ = 0f
    private var checkpointYaw = 0f
    private var lastAlertSet = mutableSetOf<String>()

    // Callbacks for audio/UI events
    var onPlaySound: ((SoundEffect, Float) -> Unit)? = null
    var onRadioMessage: ((String) -> Unit)? = null
    var onTriggerPuzzle: ((ActivePuzzle) -> Unit)? = null
    var onCheckpointSaved: ((Float, Float, Float, Set<String>, Set<String>, Set<String>) -> Unit)? = null
    var onMissionFinished: ((MissionStats) -> Unit)? = null
    var onOpenEndingChoice: (() -> Unit)? = null
    var onHapticPulse: (() -> Unit)? = null

    fun loadMission(missionId: MissionId, saveData: SaveData, fromCheckpoint: Boolean = false) {
        mission = MissionCatalog.getMission(missionId)
        val ts = mission.tileSize

        val spawnX = if (fromCheckpoint && saveData.checkpointMission == missionId) {
            saveData.checkpointX
        } else {
            (mission.spawnGridX + 0.5f) * ts
        }
        val spawnZ = if (fromCheckpoint && saveData.checkpointMission == missionId) {
            saveData.checkpointZ
        } else {
            (mission.spawnGridZ + 0.5f) * ts
        }
        val spawnYaw = if (fromCheckpoint && saveData.checkpointMission == missionId) {
            saveData.checkpointYaw
        } else {
            mission.spawnYawDeg
        }

        checkpointX = spawnX
        checkpointZ = spawnZ
        checkpointYaw = spawnYaw

        player = PlayerState(
            x = spawnX,
            y = 0f,
            z = spawnZ,
            yawDeg = spawnYaw,
            pitchDeg = 0f
        )

        val completedObjIds = if (fromCheckpoint && saveData.checkpointMission == missionId) {
            saveData.checkpointCompletedObjectives
        } else emptySet()

        val activatedInterIds = if (fromCheckpoint && saveData.checkpointMission == missionId) {
            saveData.checkpointActivatedInteractables
        } else emptySet()

        val savedInvIds = if (fromCheckpoint && saveData.checkpointMission == missionId) {
            saveData.checkpointInventoryIds
        } else emptySet()

        objectives = mission.objectives.map { def ->
            ObjectiveRuntime(def = def, completed = completedObjIds.contains(def.id))
        }

        inventory.clear()
        collectedLore.clear()

        interactables = mission.interactables.map { def ->
            val wasActivated = activatedInterIds.contains(def.id)
            if (wasActivated || (def.grantItem != null && savedInvIds.contains(def.grantItem.id))) {
                def.grantItem?.let { item ->
                    if (inventory.none { it.id == item.id }) inventory.add(item)
                }
                def.grantLore?.let { lore ->
                    if (collectedLore.none { it.id == lore.id }) collectedLore.add(lore)
                }
            }
            val unlockedByDependency = mission.interactables.any { parent ->
                parent.unlocksInteractableId == def.id && activatedInterIds.contains(parent.id)
            }
            val isLocked = def.isInitiallyLocked && !unlockedByDependency && !wasActivated
            InteractableRuntime(
                def = def,
                worldX = (def.gridX + 0.5f) * ts,
                worldZ = (def.gridZ + 0.5f) * ts,
                isLocked = isLocked,
                isActivated = wasActivated,
                doorOpenProgress = if (def.type == InteractableType.DOOR && (def.isInitiallyOpen || wasActivated)) 1f else 0f
            )
        }

        enemies = mission.enemies.map { def ->
            val firstWp = def.waypoints.firstOrNull() ?: (mission.spawnGridX to mission.spawnGridZ)
            EnemyRuntime(
                def = def,
                x = (firstWp.first + 0.5f) * ts,
                y = 0f,
                z = (firstWp.second + 0.5f) * ts,
                yawDeg = def.initialYawDeg,
                state = EnemyState.PATROL,
                waypointIndex = 0
            )
        }

        decoyBeacon = null
        elapsedTimeSec = 0f
        timesDetected = 0
        lastAlertSet.clear()
    }

    fun respawnAtCheckpoint() {
        player = PlayerState(
            x = checkpointX,
            y = 0f,
            z = checkpointZ,
            yawDeg = checkpointYaw,
            pitchDeg = 0f,
            health = 100f,
            stamina = 100f,
            scannerCharge = 100f
        )
        enemies = enemies.map { e ->
            val firstWp = e.def.waypoints.firstOrNull()
            val wx = if (firstWp != null) (firstWp.first + 0.5f) * mission.tileSize else e.x
            val wz = if (firstWp != null) (firstWp.second + 0.5f) * mission.tileSize else e.z
            e.copy(
                x = wx,
                z = wz,
                state = EnemyState.PATROL,
                detectionMeter = 0f,
                searchTimer = 0f,
                waypointIndex = 0
            )
        }
        decoyBeacon = null
    }

    fun applyLookDelta(dxDeg: Float, dyDeg: Float, settings: GameSettings) {
        val sens = 0.24f * settings.lookSensitivity
        val ySign = if (settings.invertY) 1f else -1f
        val nextYaw = Math3D.normalizeAngleDeg(player.yawDeg + dxDeg * sens)
        val nextPitch = Math3D.clamp(player.pitchDeg + dyDeg * sens * ySign, -68f, 68f)
        player = player.copy(yawDeg = nextYaw, pitchDeg = nextPitch)
    }

    fun toggleCrouch() {
        player = player.copy(isCrouching = !player.isCrouching)
        onPlaySound?.invoke(SoundEffect.UI_CLICK, 0.9f)
    }

    fun jump() {
        if (player.isGrounded && player.stamina >= 12f) {
            player = player.copy(
                vy = 5.1f,
                isGrounded = false,
                isCrouching = false,
                stamina = (player.stamina - 12f).coerceAtLeast(0f)
            )
            onPlaySound?.invoke(SoundEffect.FOOTSTEP, 0.8f)
        }
    }

    fun activateScanner() {
        if (player.scannerCharge < 25f || player.scannerActiveTimer > 0.2f) return
        player = player.copy(
            scannerCharge = (player.scannerCharge - 30f).coerceAtLeast(0f),
            scannerActiveTimer = 5.5f,
            gadgetRecoil = 1f
        )
        onPlaySound?.invoke(SoundEffect.SCANNER_PULSE, 1f)
        onHapticPulse?.invoke()
    }

    fun deployDecoy() {
        if (player.decoyCooldown > 0f) return
        val (fx, fz) = Math3D.forwardVectorFromYawDeg(player.yawDeg)
        val rawX = player.x + fx * 2.2f
        val rawZ = player.z + fz * 2.2f
        val closedDoors = closedDoorTileSet()
        val (safeX, safeZ) = Math3D.resolveCircleGridCollision(
            rawX,
            rawZ,
            0.3f,
            mission.grid,
            mission.tileSize,
            closedDoors
        )
        decoyBeacon = DecoyBeacon(x = safeX, z = safeZ, remainingTime = 6.5f)
        player = player.copy(decoyCooldown = 10.0f, gadgetRecoil = 1f)
        onPlaySound?.invoke(SoundEffect.DECOY_PING, 1f)
        onHapticPulse?.invoke()

        // Lure nearby enemies within 15m into INVESTIGATE
        enemies = enemies.map { e ->
            if (e.state != EnemyState.DISABLED && e.state != EnemyState.ALERT) {
                val dist = Math3D.distance2D(e.x, e.z, safeX, safeZ)
                if (dist <= 15.5f) {
                    e.copy(
                        state = EnemyState.INVESTIGATE,
                        lastKnownX = safeX,
                        lastKnownZ = safeZ,
                        detectionMeter = maxOf(e.detectionMeter, 35f)
                    )
                } else e
            } else e
        }
    }

    fun activateEnergyShield() {
        if (player.shieldCooldown > 0f || player.shieldActiveTimer > 0f) return
        player = player.copy(
            shieldActiveTimer = 5.0f,
            shieldCooldown = 13.0f,
            gadgetRecoil = 0.8f
        )
        onPlaySound?.invoke(SoundEffect.SHIELD_ACTIVATE, 1f)
        onHapticPulse?.invoke()
    }

    fun step(
        dtRaw: Float,
        moveX: Float,
        moveY: Float,
        sprintHeld: Boolean,
        settings: GameSettings
    ) {
        val dt = dtRaw.coerceIn(0.001f, 0.05f)
        elapsedTimeSec += dt

        val closedDoors = closedDoorTileSet()
        updatePlayerPhysics(dt, moveX, moveY, sprintHeld, closedDoors)
        updateGadgets(dt)
        updateDoors(dt)
        updateEnemies(dt, moveX, moveY, sprintHeld, settings, closedDoors)
        checkFailureState()
    }

    private fun updatePlayerPhysics(
        dt: Float,
        moveX: Float,
        moveY: Float,
        sprintHeld: Boolean,
        closedDoors: Set<Pair<Int, Int>>
    ) {
        val inputMag = kotlin.math.sqrt(moveX * moveX + moveY * moveY).coerceAtMost(1f)
        val isMoving = inputMag > 0.08f
        var sprintLock = player.sprintExhaustedLock
        if (sprintLock && player.stamina >= 28f) {
            sprintLock = false
        }

        val wantsSprint = sprintHeld && isMoving && !player.isCrouching && !sprintLock && player.stamina > 2f
        val baseSpeed = when {
            player.isCrouching -> 2.15f
            wantsSprint -> 5.65f
            else -> 3.55f
        }

        val (fx, fz) = Math3D.forwardVectorFromYawDeg(player.yawDeg)
        val rx = -fz
        val rz = fx

        // moveY positive = forward on virtual joystick
        val worldDx = (fx * moveY + rx * moveX) * baseSpeed * dt
        val worldDz = (fz * moveY + rz * moveX) * baseSpeed * dt

        val (nx, nz) = Math3D.resolveCircleGridCollision(
            player.x + worldDx,
            player.z + worldDz,
            0.42f,
            mission.grid,
            mission.tileSize,
            closedDoors
        )

        var ny = player.y
        var nvy = player.vy
        var grounded = player.isGrounded
        var landDip = Math3D.lerp(player.landingDip, 0f, dt * 10f)

        if (!grounded) {
            nvy -= 14.5f * dt
            ny += nvy * dt
            if (ny <= 0f) {
                ny = 0f
                nvy = 0f
                grounded = true
                landDip = 0.14f
                onPlaySound?.invoke(SoundEffect.FOOTSTEP, 0.75f)
            }
        }

        val targetEye = if (player.isCrouching) 1.05f else 1.65f
        val smoothEye = Math3D.lerp(player.eyeHeight, targetEye, dt * 10f)

        var stamina = player.stamina
        if (wantsSprint) {
            stamina = (stamina - 22f * dt).coerceAtLeast(0f)
            if (stamina <= 0.5f) sprintLock = true
        } else {
            val regenRate = if (isMoving) 13f else 22f
            stamina = (stamina + regenRate * dt).coerceAtMost(player.maxStamina)
        }

        val headBob = if (isMoving && grounded) {
            player.headBobPhase + dt * (if (wantsSprint) 13.5f else if (player.isCrouching) 6.5f else 9.5f)
        } else {
            player.headBobPhase
        }

        if (isMoving && grounded) {
            val stepInterval = if (wantsSprint) 0.30f else if (player.isCrouching) 0.56f else 0.44f
            footstepTimer += dt
            if (footstepTimer >= stepInterval) {
                footstepTimer = 0f
                val pitch = if (player.isCrouching) 0.72f else if (wantsSprint) 1.15f else 0.96f
                onPlaySound?.invoke(SoundEffect.FOOTSTEP, pitch)
            }
        } else {
            footstepTimer = 0.25f
        }

        // Environmental hazard floor check
        val gx = (nx / mission.tileSize).toInt().coerceIn(0, mission.width - 1)
        val gz = (nz / mission.tileSize).toInt().coerceIn(0, mission.height - 1)
        val onHazard = mission.grid[gz][gx] == TileType.HAZARD_FLOOR && grounded
        var health = player.health
        var dmgFlash = (player.damageFlash - dt * 2.5f).coerceAtLeast(0f)

        if (onHazard && player.shieldActiveTimer <= 0f) {
            health = (health - 18f * dt).coerceAtLeast(0f)
            dmgFlash = 0.65f
        }

        player = player.copy(
            x = nx,
            y = ny,
            z = nz,
            vy = nvy,
            eyeHeight = smoothEye,
            isGrounded = grounded,
            isSprinting = wantsSprint,
            sprintExhaustedLock = sprintLock,
            stamina = stamina,
            headBobPhase = headBob,
            landingDip = landDip,
            health = health,
            damageFlash = dmgFlash,
            gadgetRecoil = (player.gadgetRecoil - dt * 3.5f).coerceAtLeast(0f),
            environmentalHazardActive = onHazard
        )
    }

    private fun updateGadgets(dt: Float) {
        val hasAuxCell = inventory.any { it.id == "item_m2_cell" }
        val rechargeMult = if (hasAuxCell) 1.45f else 1.0f

        val nextScanTimer = (player.scannerActiveTimer - dt).coerceAtLeast(0f)
        val nextScanCharge = if (nextScanTimer <= 0f) {
            (player.scannerCharge + 9.5f * rechargeMult * dt).coerceAtMost(100f)
        } else player.scannerCharge

        val nextDecoyCd = (player.decoyCooldown - dt * rechargeMult).coerceAtLeast(0f)
        val nextShieldActive = (player.shieldActiveTimer - dt).coerceAtLeast(0f)
        val nextShieldCd = (player.shieldCooldown - dt * rechargeMult).coerceAtLeast(0f)

        player = player.copy(
            scannerActiveTimer = nextScanTimer,
            scannerCharge = nextScanCharge,
            decoyCooldown = nextDecoyCd,
            shieldActiveTimer = nextShieldActive,
            shieldCooldown = nextShieldCd
        )

        decoyBeacon = decoyBeacon?.let { beacon ->
            val rem = beacon.remainingTime - dt
            if (rem <= 0f) null
            else beacon.copy(remainingTime = rem, pulsePhase = beacon.pulsePhase + dt * 4f)
        }
    }

    private fun updateDoors(dt: Float) {
        interactables = interactables.map { inter ->
            if (inter.def.type == InteractableType.DOOR) {
                val target = if (inter.isActivated) 1f else 0f
                val prog = Math3D.lerp(inter.doorOpenProgress, target, dt * 6f)
                inter.copy(doorOpenProgress = prog)
            } else inter
        }
    }

    private fun updateEnemies(
        dt: Float,
        moveX: Float,
        moveY: Float,
        sprintHeld: Boolean,
        settings: GameSettings,
        closedDoors: Set<Pair<Int, Int>>
    ) {
        val diff = settings.difficulty
        val playerMoving = abs(moveX) > 0.1f || abs(moveY) > 0.1f
        val playerNoiseRadius = when {
            player.isSprinting && playerMoving -> 8.5f
            playerMoving && !player.isCrouching -> 3.0f
            else -> 0.4f
        }

        var health = player.health
        var dmgFlash = player.damageFlash

        enemies = enemies.map { e ->
            if (e.state == EnemyState.DISABLED) {
                return@map e.copy(animTime = e.animTime + dt)
            }

            val distToPlayer = Math3D.distance2D(e.x, e.z, player.x, player.z)
            val visionRange = e.def.type.visionRange * diff.visionRangeMultiplier *
                (if (player.isCrouching) 0.72f else 1.0f)

            var canSeePlayer = false
            if (distToPlayer <= visionRange) {
                val angleToPlayer = Math3D.yawDegToTarget(e.x, e.z, player.x, player.z)
                val angleDiff = abs(Math3D.normalizeAngleDeg(angleToPlayer - e.yawDeg))
                if (angleDiff <= e.def.type.visionHalfAngleDeg) {
                    canSeePlayer = Math3D.hasLineOfSight(
                        e.x, e.z, player.x, player.z,
                        mission.grid, mission.tileSize, closedDoors
                    )
                }
            }

            val hearsPlayer = distToPlayer <= playerNoiseRadius && Math3D.hasLineOfSight(
                e.x, e.z, player.x, player.z,
                mission.grid, mission.tileSize, closedDoors
            )

            var meter = e.detectionMeter
            var state = e.state
            var lkx = e.lastKnownX
            var lkz = e.lastKnownZ
            var searchTimer = e.searchTimer
            var idleTimer = e.idleTimer
            var wpIdx = e.waypointIndex
            var atkCd = (e.attackCooldown - dt).coerceAtLeast(0f)

            if (canSeePlayer) {
                val distFactor = (1.35f - (distToPlayer / visionRange)).coerceIn(0.45f, 1.4f)
                meter = (meter + e.def.type.detectionSpeed * diff.detectionRateMultiplier * distFactor * dt)
                    .coerceAtMost(100f)
                lkx = player.x
                lkz = player.z
                if (meter >= 99f) {
                    state = EnemyState.ALERT
                    if (!lastAlertSet.contains(e.def.id)) {
                        lastAlertSet.add(e.def.id)
                        timesDetected++
                        onPlaySound?.invoke(SoundEffect.ALERT_SIREN, 1f)
                        onHapticPulse?.invoke()
                    }
                } else if (meter >= 32f && state != EnemyState.ALERT) {
                    state = EnemyState.INVESTIGATE
                }
            } else {
                lastAlertSet.remove(e.def.id)
                val decayRate = if (state == EnemyState.ALERT) 14f else 22f
                meter = (meter - decayRate * dt).coerceAtLeast(0f)

                if (hearsPlayer && state != EnemyState.ALERT) {
                    lkx = player.x
                    lkz = player.z
                    meter = maxOf(meter, 40f)
                    state = EnemyState.INVESTIGATE
                }
            }

            var nx = e.x
            var nz = e.z
            var nyaw = e.yawDeg

            when (state) {
                EnemyState.IDLE -> {
                    idleTimer -= dt
                    if (idleTimer <= 0f) state = EnemyState.PATROL
                }

                EnemyState.PATROL, EnemyState.RETURN_TO_PATROL -> {
                    val wps = e.def.waypoints
                    if (wps.isNotEmpty()) {
                        val targetWp = wps[wpIdx % wps.size]
                        val tx = (targetWp.first + 0.5f) * mission.tileSize
                        val tz = (targetWp.second + 0.5f) * mission.tileSize
                        val d = Math3D.distance2D(nx, nz, tx, tz)
                        if (d < 0.35f) {
                            wpIdx = (wpIdx + 1) % wps.size
                            state = EnemyState.IDLE
                            idleTimer = 0.85f
                        } else {
                            val tYaw = Math3D.yawDegToTarget(nx, nz, tx, tz)
                            nyaw = Math3D.lerpAngleDeg(nyaw, tYaw, dt * 6f)
                            val (fx, fz) = Math3D.forwardVectorFromYawDeg(nyaw)
                            val stepDist = e.def.type.moveSpeed * dt
                            val (rx, rz) = Math3D.resolveCircleGridCollision(
                                nx + fx * stepDist, nz + fz * stepDist, 0.38f,
                                mission.grid, mission.tileSize, closedDoors
                            )
                            nx = rx
                            nz = rz
                            if (state == EnemyState.RETURN_TO_PATROL && d < 0.8f) {
                                state = EnemyState.PATROL
                            }
                        }
                    }
                }

                EnemyState.INVESTIGATE -> {
                    val beacon = decoyBeacon
                    val targetX = if (beacon != null && Math3D.distance2D(nx, nz, beacon.x, beacon.z) < 15.5f) {
                        beacon.x
                    } else lkx
                    val targetZ = if (beacon != null && Math3D.distance2D(nx, nz, beacon.x, beacon.z) < 15.5f) {
                        beacon.z
                    } else lkz

                    val d = Math3D.distance2D(nx, nz, targetX, targetZ)
                    val tYaw = Math3D.yawDegToTarget(nx, nz, targetX, targetZ)
                    nyaw = Math3D.lerpAngleDeg(nyaw, tYaw, dt * 7f)
                    if (d > 0.7f) {
                        val (fx, fz) = Math3D.forwardVectorFromYawDeg(nyaw)
                        val stepDist = e.def.type.moveSpeed * 1.15f * dt
                        val (rx, rz) = Math3D.resolveCircleGridCollision(
                            nx + fx * stepDist, nz + fz * stepDist, 0.38f,
                            mission.grid, mission.tileSize, closedDoors
                        )
                        nx = rx
                        nz = rz
                    } else {
                        state = EnemyState.SEARCH
                        searchTimer = 4.0f
                    }
                }

                EnemyState.ALERT -> {
                    if (!canSeePlayer && meter <= 20f) {
                        state = EnemyState.SEARCH
                        searchTimer = 5.5f
                    } else {
                        val tYaw = Math3D.yawDegToTarget(nx, nz, lkx, lkz)
                        nyaw = Math3D.lerpAngleDeg(nyaw, tYaw, dt * 10f)
                        val d = Math3D.distance2D(nx, nz, lkx, lkz)
                        if (d > 1.8f) {
                            val (fx, fz) = Math3D.forwardVectorFromYawDeg(nyaw)
                            val stepDist = e.def.type.alertSpeed * dt
                            val (rx, rz) = Math3D.resolveCircleGridCollision(
                                nx + fx * stepDist, nz + fz * stepDist, 0.38f,
                                mission.grid, mission.tileSize, closedDoors
                            )
                            nx = rx
                            nz = rz
                        }
                        if (canSeePlayer && distToPlayer <= 6.0f && atkCd <= 0f) {
                            atkCd = 1.25f
                            if (player.shieldActiveTimer <= 0f) {
                                health = (health - e.def.type.damagePerHit * diff.damageMultiplier)
                                    .coerceAtLeast(0f)
                                dmgFlash = 1.0f
                                onPlaySound?.invoke(SoundEffect.DAMAGE_HIT, 1f)
                                onHapticPulse?.invoke()
                            } else {
                                onPlaySound?.invoke(SoundEffect.SHIELD_ACTIVATE, 1.3f)
                            }
                        }
                    }
                }

                EnemyState.SEARCH -> {
                    searchTimer -= dt
                    nyaw = Math3D.normalizeAngleDeg(nyaw + 68f * dt)
                    if (searchTimer <= 0f) {
                        state = EnemyState.RETURN_TO_PATROL
                    }
                }

                EnemyState.DISABLED -> Unit
            }

            e.copy(
                x = nx,
                z = nz,
                yawDeg = nyaw,
                state = state,
                waypointIndex = wpIdx,
                detectionMeter = meter,
                lastKnownX = lkx,
                lastKnownZ = lkz,
                searchTimer = searchTimer,
                idleTimer = idleTimer,
                attackCooldown = atkCd,
                animTime = e.animTime + dt
            )
        }

        player = player.copy(health = health, damageFlash = dmgFlash)
    }

    fun getFocusedInteractable(): InteractableRuntime? {
        val (fx, fz) = Math3D.forwardVectorFromYawDeg(player.yawDeg)
        val closedDoors = closedDoorTileSet()
        var best: InteractableRuntime? = null
        var bestScore = Float.MAX_VALUE

        for (inter in interactables) {
            if (inter.isActivated && inter.def.type != InteractableType.DOOR &&
                inter.def.type != InteractableType.AUDIO_LOG &&
                inter.def.type != InteractableType.DOCUMENT &&
                inter.def.type != InteractableType.CORE_CONSOLE
            ) continue

            val dx = inter.worldX - player.x
            val dz = inter.worldZ - player.z
            val dist = kotlin.math.sqrt(dx * dx + dz * dz)
            if (dist > 3.15f) continue
            val dot = if (dist > 0.01f) (dx * fx + dz * fz) / dist else 1f
            if (dot < 0.52f) continue

            if (!Math3D.hasLineOfSight(
                    player.x, player.z, inter.worldX, inter.worldZ,
                    mission.grid, mission.tileSize, closedDoors - (inter.def.gridX to inter.def.gridZ)
                )
            ) continue

            val score = dist - dot * 0.8f
            if (score < bestScore) {
                bestScore = score
                best = inter
            }
        }
        return best
    }

    fun interactWithFocused(): String? {
        val target = getFocusedInteractable() ?: return null

        if (target.isLocked) {
            onPlaySound?.invoke(SoundEffect.PUZZLE_FAIL, 1f)
            return "Terkunci: Retas terminal atau pulihkan daya terlebih dahulu."
        }

        val reqId = target.def.requiredItemId
        if (reqId != null && inventory.none { it.id == reqId }) {
            onPlaySound?.invoke(SoundEffect.PUZZLE_FAIL, 1f)
            return "Membutuhkan item akses khusus sebelum mengoperasikan ${target.def.title}."
        }

        // If interactable requires a puzzle and is not yet activated, launch puzzle
        val puzzle = target.def.puzzleType
        if (puzzle != null && !target.isActivated) {
            onPlaySound?.invoke(SoundEffect.TERMINAL_BEEP, 1f)
            onTriggerPuzzle?.invoke(
                ActivePuzzle(
                    interactableId = target.def.id,
                    title = target.def.title,
                    puzzleType = puzzle
                )
            )
            return null
        }

        return completeInteractableActivation(target.def.id)
    }

    fun completeInteractableActivation(interactableId: String): String {
        val target = interactables.firstOrNull { it.def.id == interactableId } ?: return ""

        if (target.def.type == InteractableType.CORE_CONSOLE) {
            completeObjective(target.def.completesObjectiveId)
            target.def.unlocksInteractableId?.let { unlockId ->
                interactables = interactables.map {
                    if (it.def.id == unlockId) it.copy(isLocked = false) else it
                }
            }
            interactables = interactables.map {
                if (it.def.id == interactableId) it.copy(isActivated = true) else it
            }
            onPlaySound?.invoke(SoundEffect.TERMINAL_BEEP, 1.2f)
            onOpenEndingChoice?.invoke()
            return "AETHER-9 Core siap menerima perintah protokol akhir."
        }

        if (target.def.type == InteractableType.DOOR) {
            val nextOpen = !target.isActivated
            interactables = interactables.map {
                if (it.def.id == interactableId) it.copy(isActivated = nextOpen) else it
            }
            onPlaySound?.invoke(SoundEffect.DOOR_SLIDE, 1f)
            return if (nextOpen) "${target.def.title} dibuka." else "${target.def.title} ditutup."
        }

        if (target.def.type == InteractableType.ELEVATOR) {
            val primaryPending = objectives.any { !it.def.isOptional && !it.completed && it.def.id != target.def.completesObjectiveId }
            if (primaryPending) {
                onPlaySound?.invoke(SoundEffect.PUZZLE_FAIL, 1f)
                return "Selesaikan seluruh objektif utama sebelum evakuasi sektor."
            }
            completeObjective(target.def.completesObjectiveId)
            finishMission(success = true)
            return "Memulai ekstraksi sektor..."
        }

        interactables = interactables.map {
            if (it.def.id == interactableId) it.copy(isActivated = true) else it
        }

        target.def.grantItem?.let { item ->
            if (inventory.none { it.id == item.id }) {
                inventory.add(item)
            }
        }
        target.def.grantLore?.let { lore ->
            if (collectedLore.none { it.id == lore.id }) {
                collectedLore.add(lore)
                onRadioMessage?.invoke("${lore.title}: ${lore.body}")
            }
        }
        target.def.unlocksInteractableId?.let { unlockId ->
            interactables = interactables.map {
                if (it.def.id == unlockId) it.copy(isLocked = false) else it
            }
        }
        completeObjective(target.def.completesObjectiveId)

        if (target.def.type == InteractableType.CHECKPOINT) {
            checkpointX = player.x
            checkpointZ = player.z
            checkpointYaw = player.yawDeg
            saveCurrentCheckpoint()
            onPlaySound?.invoke(SoundEffect.PUZZLE_SUCCESS, 1f)
            return "Checkpoint disinkronkan & progres tersimpan."
        }

        if (target.def.type == InteractableType.GENERATOR) {
            // Also disable closest patrolling guardian/sentinel on power reroute feedback
            onPlaySound?.invoke(SoundEffect.PUZZLE_SUCCESS, 1f)
            return "${target.def.title} berhasil dipulihkan!"
        }

        onPlaySound?.invoke(SoundEffect.ITEM_PICKUP, 1f)
        return "${target.def.title} selesai diproses."
    }

    fun disableNearestEnemyFromCore() {
        enemies = enemies.map { it.copy(state = EnemyState.DISABLED, detectionMeter = 0f) }
    }

    private fun completeObjective(objId: String?) {
        if (objId == null) return
        objectives = objectives.map {
            if (it.def.id == objId) it.copy(completed = true) else it
        }
    }

    private fun saveCurrentCheckpoint() {
        onCheckpointSaved?.invoke(
            checkpointX,
            checkpointZ,
            checkpointYaw,
            objectives.filter { it.completed }.map { it.def.id }.toSet(),
            interactables.filter { it.isActivated }.map { it.def.id }.toSet(),
            inventory.map { it.id }.toSet()
        )
    }

    private fun checkFailureState() {
        if (player.health <= 0f) {
            finishMission(
                success = false,
                failureReason = "Kegagalan Kritis: Operatif dilumpuhkan oleh sistem pertahanan AETHER-9."
            )
        }
    }

    private fun finishMission(success: Boolean, failureReason: String = "") {
        val primary = objectives.filter { !it.def.isOptional }
        val optional = objectives.filter { it.def.isOptional }
        if (success) {
            onPlaySound?.invoke(SoundEffect.MISSION_COMPLETE, 1f)
        } else {
            onPlaySound?.invoke(SoundEffect.PUZZLE_FAIL, 0.8f)
        }
        onMissionFinished?.invoke(
            MissionStats(
                missionId = mission.id,
                success = success,
                failureReason = failureReason,
                elapsedSeconds = elapsedTimeSec.toInt(),
                primaryCompleted = primary.count { it.completed },
                primaryTotal = primary.size,
                optionalCompleted = optional.count { it.completed },
                optionalTotal = optional.size,
                timesDetected = timesDetected,
                dataCollected = collectedLore.size + inventory.size
            )
        )
    }

    fun currentRoomName(): String {
        val gx = (player.x / mission.tileSize).toInt().coerceIn(0, mission.width - 1)
        val gz = (player.z / mission.tileSize).toInt().coerceIn(0, mission.height - 1)
        return mission.roomZones[gx to gz] ?: mission.id.sectorName
    }

    private fun closedDoorTileSet(): Set<Pair<Int, Int>> {
        return interactables
            .filter { it.def.type == InteractableType.DOOR && it.doorOpenProgress < 0.7f }
            .map { it.def.gridX to it.def.gridZ }
            .toSet()
    }
}
