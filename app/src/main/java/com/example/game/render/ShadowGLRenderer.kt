package com.example.game.render

import android.opengl.GLES30
import android.opengl.GLSurfaceView
import android.opengl.Matrix
import com.example.game.engine.Math3D
import com.example.game.engine.SimulationEngine
import com.example.game.model.EnemyRuntime
import com.example.game.model.EnemyState
import com.example.game.model.EnemyType
import com.example.game.model.GameSettings
import com.example.game.model.InteractableRuntime
import com.example.game.model.InteractableType
import com.example.game.model.MissionId
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.opengles.GL10
import kotlin.math.cos
import kotlin.math.sin

class ShadowGLRenderer(
    private val simulation: SimulationEngine,
    private val settingsProvider: () -> GameSettings,
    private val onFpsMeasured: (Int) -> Unit
) : GLSurfaceView.Renderer {

    private var program = 0
    private var uMvpLoc = 0
    private var uModelLoc = 0
    private var uTexLoc = 0
    private var uCamPosLoc = 0
    private var uCamDirLoc = 0
    private var uFogColorLoc = 0
    private var uFogDensityLoc = 0
    private var uEmissiveLoc = 0
    private var uAlphaLoc = 0
    private var uDynamicLightLoc = 0
    private var uPointLightPosLoc = 0
    private var uPointLightColorLoc = 0

    private var textures = emptyMap<TextureAtlasBuilder.MaterialTex, Int>()
    private var levelMeshes = emptyMap<TextureAtlasBuilder.MaterialTex, MeshBuffer>()
    private var loadedMissionId: MissionId? = null

    private lateinit var cubeMesh: MeshBuffer
    private lateinit var cylinderMesh: MeshBuffer
    private lateinit var wedgeSentinelMesh: MeshBuffer
    private lateinit var wedgeObserverMesh: MeshBuffer
    private lateinit var ringMesh: MeshBuffer

    private val projMatrix = FloatArray(16)
    private val viewMatrix = FloatArray(16)
    private val modelMatrix = FloatArray(16)
    private val vpMatrix = FloatArray(16)
    private val mvpMatrix = FloatArray(16)

    private var surfaceWidth = 1280
    private var surfaceHeight = 720
    private var frameCounter = 0
    private var lastFpsTimestampNs = System.nanoTime()
    private var animClock = 0f

    override fun onSurfaceCreated(gl: GL10?, config: EGLConfig?) {
        GLES30.glClearColor(0.02f, 0.05f, 0.09f, 1f)
        GLES30.glEnable(GLES30.GL_DEPTH_TEST)
        GLES30.glEnable(GLES30.GL_CULL_FACE)
        GLES30.glEnable(GLES30.GL_BLEND)
        GLES30.glBlendFunc(GLES30.GL_SRC_ALPHA, GLES30.GL_ONE_MINUS_SRC_ALPHA)

        program = buildShaderProgram(VERTEX_SHADER, FRAGMENT_SHADER)
        uMvpLoc = GLES30.glGetUniformLocation(program, "uMVP")
        uModelLoc = GLES30.glGetUniformLocation(program, "uModel")
        uTexLoc = GLES30.glGetUniformLocation(program, "uTex")
        uCamPosLoc = GLES30.glGetUniformLocation(program, "uCamPos")
        uCamDirLoc = GLES30.glGetUniformLocation(program, "uCamDir")
        uFogColorLoc = GLES30.glGetUniformLocation(program, "uFogColor")
        uFogDensityLoc = GLES30.glGetUniformLocation(program, "uFogDensity")
        uEmissiveLoc = GLES30.glGetUniformLocation(program, "uEmissive")
        uAlphaLoc = GLES30.glGetUniformLocation(program, "uAlpha")
        uDynamicLightLoc = GLES30.glGetUniformLocation(program, "uEnableDynamicLight")
        uPointLightPosLoc = GLES30.glGetUniformLocation(program, "uPointLightPos")
        uPointLightColorLoc = GLES30.glGetUniformLocation(program, "uPointLightColor")

        textures = TextureAtlasBuilder.buildAllTextures()
        cubeMesh = MeshBuilder.buildUnitCube()
        cylinderMesh = MeshBuilder.buildCylinder(16)
        wedgeSentinelMesh = MeshBuilder.buildVisionConeWedge(36f, 12)
        wedgeObserverMesh = MeshBuilder.buildVisionConeWedge(52f, 14)
        ringMesh = MeshBuilder.buildRing(32)

        rebuildLevelIfNeeded()
    }

    override fun onSurfaceChanged(gl: GL10?, width: Int, height: Int) {
        surfaceWidth = width.coerceAtLeast(1)
        surfaceHeight = height.coerceAtLeast(1)
        GLES30.glViewport(0, 0, surfaceWidth, surfaceHeight)
    }

    override fun onDrawFrame(gl: GL10?) {
        rebuildLevelIfNeeded()
        animClock += 0.016f
        trackFps()

        val settings = settingsProvider()
        val quality = settings.graphicsQuality
        val mission = simulation.mission
        val player = simulation.player

        val (tintR, tintG, tintB) = mission.ambientTint
        GLES30.glClearColor(tintR * 0.4f, tintG * 0.4f, tintB * 0.4f, 1f)
        GLES30.glClear(GLES30.GL_COLOR_BUFFER_BIT or GLES30.GL_DEPTH_BUFFER_BIT)

        val aspect = surfaceWidth.toFloat() / surfaceHeight.toFloat()
        Matrix.perspectiveM(projMatrix, 0, settings.fovDegrees, aspect, 0.1f, 75f)

        val bobY = sin(player.headBobPhase) * 0.042f - player.landingDip
        val shakeOffset = if (settings.cameraShakeEnabled && player.damageFlash > 0.05f) {
            sin(animClock * 48f) * 0.05f * player.damageFlash
        } else 0f

        val camX = player.x + shakeOffset
        val camY = player.y + player.eyeHeight + bobY
        val camZ = player.z

        val yawRad = player.yawDeg * Math3D.DEG_TO_RAD
        val pitchRad = player.pitchDeg * Math3D.DEG_TO_RAD
        val dirX = sin(yawRad) * cos(pitchRad)
        val dirY = sin(pitchRad)
        val dirZ = -cos(yawRad) * cos(pitchRad)

        Matrix.setLookAtM(
            viewMatrix, 0,
            camX, camY, camZ,
            camX + dirX, camY + dirY, camZ + dirZ,
            0f, 1f, 0f
        )
        Matrix.multiplyMM(vpMatrix, 0, projMatrix, 0, viewMatrix, 0)

        GLES30.glUseProgram(program)
        GLES30.glUniform3f(uCamPosLoc, camX, camY, camZ)
        GLES30.glUniform3f(uCamDirLoc, dirX, dirY, dirZ)
        GLES30.glUniform3f(uFogColorLoc, tintR, tintG, tintB)
        GLES30.glUniform1f(uFogDensityLoc, quality.fogDensity)
        GLES30.glUniform1i(uDynamicLightLoc, if (quality.enableDynamicLights) 1 else 0)

        // Point light from nearest alert enemy or scanner pulse
        val alertEnemy = simulation.enemies.firstOrNull { it.state == EnemyState.ALERT }
        if (alertEnemy != null) {
            val pulse = 0.7f + 0.3f * sin(animClock * 10f)
            GLES30.glUniform3f(uPointLightPosLoc, alertEnemy.x, 1.8f, alertEnemy.z)
            GLES30.glUniform3f(uPointLightColorLoc, 1.4f * pulse, 0.15f, 0.25f)
        } else if (player.scannerActiveTimer > 0f) {
            GLES30.glUniform3f(uPointLightPosLoc, camX, camY, camZ)
            GLES30.glUniform3f(uPointLightColorLoc, 0.0f, 1.15f, 1.35f)
        } else {
            GLES30.glUniform3f(uPointLightPosLoc, camX, camY + 0.5f, camZ)
            GLES30.glUniform3f(uPointLightColorLoc, 0.05f, 0.22f, 0.32f)
        }

        // 1. Static Level Architecture
        Matrix.setIdentityM(modelMatrix, 0)
        GLES30.glUniform3f(uEmissiveLoc, 0f, 0f, 0f)
        GLES30.glUniform1f(uAlphaLoc, 1f)
        for ((mat, mesh) in levelMeshes) {
            val texId = textures[mat] ?: continue
            drawMesh(mesh, texId, modelMatrix)
        }

        // 2. Interactables (Doors, Terminals, Keycards, Generators, Checkpoints, Elevators)
        val scannerActive = player.scannerActiveTimer > 0f
        for (inter in simulation.interactables) {
            drawInteractable(inter, scannerActive)
        }

        // 3. Decoy Beacon
        simulation.decoyBeacon?.let { beacon ->
            val texId = textures[TextureAtlasBuilder.MaterialTex.PROP_CONSOLE] ?: 0
            Matrix.setIdentityM(modelMatrix, 0)
            Matrix.translateM(modelMatrix, 0, beacon.x, 0.24f, beacon.z)
            Matrix.rotateM(modelMatrix, 0, animClock * 120f, 0f, 1f, 0f)
            Matrix.scaleM(modelMatrix, 0, 0.32f, 0.32f, 0.32f)
            GLES30.glUniform3f(uEmissiveLoc, 0f, 0.9f, 1f)
            GLES30.glUniform1f(uAlphaLoc, 1f)
            drawMesh(cylinderMesh, texId, modelMatrix)

            val ringScale = 0.6f + (animClock * 2.2f % 2.4f)
            Matrix.setIdentityM(modelMatrix, 0)
            Matrix.translateM(modelMatrix, 0, beacon.x, 0.04f, beacon.z)
            Matrix.scaleM(modelMatrix, 0, ringScale, 1f, ringScale)
            GLES30.glUniform3f(uEmissiveLoc, 0f, 0.85f, 1f)
            GLES30.glUniform1f(uAlphaLoc, 0.45f)
            drawMesh(ringMesh, texId, modelMatrix)
        }

        // 4. Articulated 3D Security Robots & Floor Vision Cones
        for (enemy in simulation.enemies) {
            drawEnemy(enemy, scannerActive)
        }

        // 5. Scanner 3D Pulse Ring
        if (scannerActive) {
            val wave = ((5.5f - player.scannerActiveTimer) * 4.5f) % 14f
            val texId = textures[TextureAtlasBuilder.MaterialTex.PROP_CONSOLE] ?: 0
            Matrix.setIdentityM(modelMatrix, 0)
            Matrix.translateM(modelMatrix, 0, player.x, 0.06f, player.z)
            Matrix.scaleM(modelMatrix, 0, wave.coerceAtLeast(0.5f), 1f, wave.coerceAtLeast(0.5f))
            GLES30.glUniform3f(uEmissiveLoc, 0f, 0.95f, 1f)
            GLES30.glUniform1f(uAlphaLoc, (1f - wave / 14f).coerceIn(0.1f, 0.7f))
            drawMesh(ringMesh, texId, modelMatrix)
        }

        // 6. Floating Ambient Dust/Sparks (Medium & High Quality)
        if (quality.enableParticles) {
            drawAmbientParticles(player.x, player.z)
        }

        // 7. First-Person 3D Cyber-Arm & Gadget Viewmodel
        drawFirstPersonViewmodel(bobY, player.isCrouching)
    }

    private fun drawInteractable(inter: InteractableRuntime, scannerHighlight: Boolean) {
        val doorTex = textures[TextureAtlasBuilder.MaterialTex.DOOR_BLAST] ?: 0
        val consoleTex = textures[TextureAtlasBuilder.MaterialTex.PROP_CONSOLE] ?: 0
        val steelTex = textures[TextureAtlasBuilder.MaterialTex.WALL_STEEL] ?: 0
        val ts = simulation.mission.tileSize

        when (inter.def.type) {
            InteractableType.DOOR -> {
                val lift = inter.doorOpenProgress * 2.95f
                Matrix.setIdentityM(modelMatrix, 0)
                Matrix.translateM(modelMatrix, 0, inter.worldX, 1.6f + lift, inter.worldZ)
                Matrix.scaleM(modelMatrix, 0, ts, 3.2f, ts * 0.25f)
                if (inter.isLocked) {
                    GLES30.glUniform3f(uEmissiveLoc, 0.65f, 0.08f, 0.15f)
                } else {
                    GLES30.glUniform3f(uEmissiveLoc, 0f, 0.35f, 0.45f)
                }
                GLES30.glUniform1f(uAlphaLoc, 1f)
                drawMesh(cubeMesh, doorTex, modelMatrix)
            }

            InteractableType.TERMINAL, InteractableType.CORE_CONSOLE -> {
                val glow = if (inter.isActivated) Triple(0f, 0.9f, 0.45f) else Triple(0f, 0.8f, 1f)
                // Base pedestal
                Matrix.setIdentityM(modelMatrix, 0)
                Matrix.translateM(modelMatrix, 0, inter.worldX, 0.55f, inter.worldZ)
                Matrix.scaleM(modelMatrix, 0, 0.65f, 1.1f, 0.5f)
                GLES30.glUniform3f(uEmissiveLoc, if (scannerHighlight) 0.1f else 0f, if (scannerHighlight) 0.3f else 0f, 0f)
                GLES30.glUniform1f(uAlphaLoc, 1f)
                drawMesh(cubeMesh, steelTex, modelMatrix)

                // Glowing screen
                Matrix.setIdentityM(modelMatrix, 0)
                Matrix.translateM(modelMatrix, 0, inter.worldX, 1.25f, inter.worldZ)
                Matrix.rotateM(modelMatrix, 0, -24f, 1f, 0f, 0f)
                Matrix.scaleM(modelMatrix, 0, 0.72f, 0.48f, 0.08f)
                GLES30.glUniform3f(uEmissiveLoc, glow.first, glow.second, glow.third)
                drawMesh(cubeMesh, consoleTex, modelMatrix)
            }

            InteractableType.GENERATOR -> {
                val gColor = if (inter.isActivated) Triple(0f, 0.85f, 0.55f) else Triple(1f, 0.55f, 0f)
                Matrix.setIdentityM(modelMatrix, 0)
                Matrix.translateM(modelMatrix, 0, inter.worldX, 0.9f, inter.worldZ)
                if (inter.isActivated) Matrix.rotateM(modelMatrix, 0, animClock * 160f, 0f, 1f, 0f)
                Matrix.scaleM(modelMatrix, 0, 1.0f, 1.8f, 1.0f)
                GLES30.glUniform3f(uEmissiveLoc, gColor.first * 0.5f, gColor.second * 0.5f, gColor.third * 0.5f)
                GLES30.glUniform1f(uAlphaLoc, 1f)
                drawMesh(cylinderMesh, steelTex, modelMatrix)
            }

            InteractableType.KEYCARD,
            InteractableType.DATA_CHIP,
            InteractableType.DOCUMENT,
            InteractableType.AUDIO_LOG,
            InteractableType.ENERGY_CELL -> {
                if (inter.isActivated) return
                val bob = sin(animClock * 3.5f) * 0.08f
                Matrix.setIdentityM(modelMatrix, 0)
                Matrix.translateM(modelMatrix, 0, inter.worldX, 0.45f, inter.worldZ)
                Matrix.scaleM(modelMatrix, 0, 0.55f, 0.9f, 0.55f)
                GLES30.glUniform3f(uEmissiveLoc, 0f, 0f, 0f)
                GLES30.glUniform1f(uAlphaLoc, 1f)
                drawMesh(cubeMesh, steelTex, modelMatrix)

                Matrix.setIdentityM(modelMatrix, 0)
                Matrix.translateM(modelMatrix, 0, inter.worldX, 1.12f + bob, inter.worldZ)
                Matrix.rotateM(modelMatrix, 0, animClock * 85f, 0f, 1f, 0f)
                Matrix.scaleM(modelMatrix, 0, 0.34f, 0.22f, 0.08f)
                GLES30.glUniform3f(uEmissiveLoc, 0f, 0.95f, 1f)
                drawMesh(cubeMesh, consoleTex, modelMatrix)
            }

            InteractableType.CHECKPOINT, InteractableType.ELEVATOR -> {
                val color = if (inter.def.type == InteractableType.ELEVATOR) {
                    Triple(0.1f, 1f, 0.55f)
                } else {
                    Triple(0f, 0.78f, 1f)
                }
                Matrix.setIdentityM(modelMatrix, 0)
                Matrix.translateM(modelMatrix, 0, inter.worldX, 0.05f, inter.worldZ)
                Matrix.scaleM(modelMatrix, 0, 1.0f, 1f, 1.0f)
                GLES30.glUniform3f(uEmissiveLoc, color.first, color.second, color.third)
                GLES30.glUniform1f(uAlphaLoc, 0.85f)
                drawMesh(ringMesh, consoleTex, modelMatrix)

                Matrix.setIdentityM(modelMatrix, 0)
                Matrix.translateM(modelMatrix, 0, inter.worldX, 1.3f, inter.worldZ)
                Matrix.scaleM(modelMatrix, 0, 0.16f, 2.5f, 0.16f)
                GLES30.glUniform1f(uAlphaLoc, 0.28f)
                drawMesh(cylinderMesh, consoleTex, modelMatrix)
            }
        }
    }

    private fun drawEnemy(enemy: EnemyRuntime, scannerHighlight: Boolean) {
        val bodyTex = textures[TextureAtlasBuilder.MaterialTex.ROBOT_METAL] ?: 0
        val visorTex = textures[TextureAtlasBuilder.MaterialTex.PROP_CONSOLE] ?: 0

        val visorColor = when (enemy.state) {
            EnemyState.ALERT -> Triple(1.0f, 0.12f, 0.25f)
            EnemyState.INVESTIGATE, EnemyState.SEARCH -> Triple(1.0f, 0.68f, 0.0f)
            EnemyState.DISABLED -> Triple(0.15f, 0.15f, 0.20f)
            else -> if (scannerHighlight) Triple(0f, 1f, 0.95f) else Triple(0f, 0.85f, 1.0f)
        }

        val stride = sin(enemy.animTime * 6.5f) * 0.08f
        val hoverBob = if (enemy.def.type == EnemyType.OBSERVER) sin(enemy.animTime * 4.5f) * 0.14f else 0f
        val baseY = if (enemy.def.type == EnemyType.OBSERVER) 1.55f + hoverBob else 0f

        when (enemy.def.type) {
            EnemyType.SENTINEL -> {
                // Legs
                for (side in listOf(-0.2f, 0.2f)) {
                    val legOffset = if (side < 0) stride else -stride
                    Matrix.setIdentityM(modelMatrix, 0)
                    Matrix.translateM(modelMatrix, 0, enemy.x, 0.45f, enemy.z)
                    Matrix.rotateM(modelMatrix, 0, -enemy.yawDeg, 0f, 1f, 0f)
                    Matrix.translateM(modelMatrix, 0, side, 0f, legOffset)
                    Matrix.scaleM(modelMatrix, 0, 0.18f, 0.9f, 0.22f)
                    GLES30.glUniform3f(uEmissiveLoc, 0f, 0f, 0f)
                    GLES30.glUniform1f(uAlphaLoc, 1f)
                    drawMesh(cubeMesh, bodyTex, modelMatrix)
                }
                // Torso
                Matrix.setIdentityM(modelMatrix, 0)
                Matrix.translateM(modelMatrix, 0, enemy.x, 1.2f, enemy.z)
                Matrix.rotateM(modelMatrix, 0, -enemy.yawDeg, 0f, 1f, 0f)
                Matrix.scaleM(modelMatrix, 0, 0.65f, 0.75f, 0.45f)
                drawMesh(cubeMesh, bodyTex, modelMatrix)

                // Head + Visor
                Matrix.setIdentityM(modelMatrix, 0)
                Matrix.translateM(modelMatrix, 0, enemy.x, 1.75f, enemy.z)
                Matrix.rotateM(modelMatrix, 0, -enemy.yawDeg, 0f, 1f, 0f)
                Matrix.scaleM(modelMatrix, 0, 0.36f, 0.28f, 0.38f)
                GLES30.glUniform3f(uEmissiveLoc, visorColor.first * 0.7f, visorColor.second * 0.7f, visorColor.third * 0.7f)
                drawMesh(cubeMesh, visorTex, modelMatrix)
            }

            EnemyType.OBSERVER -> {
                // Hovering Tri-Wing Core
                Matrix.setIdentityM(modelMatrix, 0)
                Matrix.translateM(modelMatrix, 0, enemy.x, baseY, enemy.z)
                Matrix.rotateM(modelMatrix, 0, -enemy.yawDeg, 0f, 1f, 0f)
                Matrix.scaleM(modelMatrix, 0, 0.62f, 0.38f, 0.62f)
                GLES30.glUniform3f(uEmissiveLoc, 0f, 0f, 0f)
                GLES30.glUniform1f(uAlphaLoc, 1f)
                drawMesh(cylinderMesh, bodyTex, modelMatrix)

                // Rotating wing blades
                Matrix.setIdentityM(modelMatrix, 0)
                Matrix.translateM(modelMatrix, 0, enemy.x, baseY + 0.1f, enemy.z)
                Matrix.rotateM(modelMatrix, 0, enemy.animTime * 220f, 0f, 1f, 0f)
                Matrix.scaleM(modelMatrix, 0, 1.25f, 0.06f, 0.18f)
                GLES30.glUniform3f(uEmissiveLoc, visorColor.first, visorColor.second, visorColor.third)
                drawMesh(cubeMesh, visorTex, modelMatrix)
            }

            EnemyType.GUARDIAN -> {
                // Heavy armored treads + dual chassis
                Matrix.setIdentityM(modelMatrix, 0)
                Matrix.translateM(modelMatrix, 0, enemy.x, 0.35f, enemy.z)
                Matrix.rotateM(modelMatrix, 0, -enemy.yawDeg, 0f, 1f, 0f)
                Matrix.scaleM(modelMatrix, 0, 1.05f, 0.7f, 1.05f)
                GLES30.glUniform3f(uEmissiveLoc, 0f, 0f, 0f)
                GLES30.glUniform1f(uAlphaLoc, 1f)
                drawMesh(cubeMesh, bodyTex, modelMatrix)

                Matrix.setIdentityM(modelMatrix, 0)
                Matrix.translateM(modelMatrix, 0, enemy.x, 1.25f, enemy.z)
                Matrix.rotateM(modelMatrix, 0, -enemy.yawDeg, 0f, 1f, 0f)
                Matrix.scaleM(modelMatrix, 0, 0.92f, 1.1f, 0.72f)
                drawMesh(cubeMesh, bodyTex, modelMatrix)

                // Dual Heavy Optical Visor
                Matrix.setIdentityM(modelMatrix, 0)
                Matrix.translateM(modelMatrix, 0, enemy.x, 1.65f, enemy.z)
                Matrix.rotateM(modelMatrix, 0, -enemy.yawDeg, 0f, 1f, 0f)
                Matrix.translateM(modelMatrix, 0, 0f, 0f, -0.35f)
                Matrix.scaleM(modelMatrix, 0, 0.68f, 0.22f, 0.15f)
                GLES30.glUniform3f(uEmissiveLoc, visorColor.first, visorColor.second, visorColor.third)
                drawMesh(cubeMesh, visorTex, modelMatrix)
            }
        }

        // Vision Cone Floor Projection
        if (enemy.state != EnemyState.DISABLED) {
            val range = enemy.def.type.visionRange * settingsProvider().difficulty.visionRangeMultiplier
            val coneMesh = if (enemy.def.type == EnemyType.OBSERVER) wedgeObserverMesh else wedgeSentinelMesh
            Matrix.setIdentityM(modelMatrix, 0)
            Matrix.translateM(modelMatrix, 0, enemy.x, 0.02f, enemy.z)
            Matrix.rotateM(modelMatrix, 0, -enemy.yawDeg, 0f, 1f, 0f)
            Matrix.scaleM(modelMatrix, 0, range, 1f, range)
            GLES30.glUniform3f(uEmissiveLoc, visorColor.first, visorColor.second, visorColor.third)
            GLES30.glUniform1f(uAlphaLoc, if (scannerHighlight) 0.34f else 0.18f)
            GLES30.glDisable(GLES30.GL_CULL_FACE)
            drawMesh(coneMesh, visorTex, modelMatrix)
            GLES30.glEnable(GLES30.GL_CULL_FACE)
        }
    }

    private fun drawAmbientParticles(px: Float, pz: Float) {
        val texId = textures[TextureAtlasBuilder.MaterialTex.PROP_CONSOLE] ?: 0
        GLES30.glUniform3f(uEmissiveLoc, 0.1f, 0.75f, 0.95f)
        GLES30.glUniform1f(uAlphaLoc, 0.55f)
        for (i in 0 until 14) {
            val angle = i * 0.45f + animClock * 0.25f
            val dist = 2.5f + (i % 5) * 1.8f
            val x = px + cos(angle) * dist
            val z = pz + sin(angle) * dist
            val y = 0.4f + ((animClock * 0.45f + i * 0.37f) % 2.4f)
            Matrix.setIdentityM(modelMatrix, 0)
            Matrix.translateM(modelMatrix, 0, x, y, z)
            Matrix.scaleM(modelMatrix, 0, 0.035f, 0.035f, 0.035f)
            drawMesh(cubeMesh, texId, modelMatrix)
        }
    }

    private fun drawFirstPersonViewmodel(bobY: Float, isCrouching: Boolean) {
        val armTex = textures[TextureAtlasBuilder.MaterialTex.ROBOT_METAL] ?: 0
        val gadgetTex = textures[TextureAtlasBuilder.MaterialTex.PROP_CONSOLE] ?: 0
        val player = simulation.player

        // Draw viewmodel in camera-local space by resetting viewMatrix to identity
        Matrix.setIdentityM(vpMatrix, 0)
        Matrix.multiplyMM(vpMatrix, 0, projMatrix, 0, vpMatrix, 0)

        GLES30.glClear(GLES30.GL_DEPTH_BUFFER_BIT)
        val swayX = sin(player.headBobPhase * 0.5f) * 0.015f
        val crouchOff = if (isCrouching) -0.03f else 0f
        val recoilZ = player.gadgetRecoil * 0.08f

        // Forearm
        Matrix.setIdentityM(modelMatrix, 0)
        Matrix.translateM(modelMatrix, 0, 0.30f + swayX, -0.32f + bobY * 0.4f + crouchOff, -0.58f + recoilZ)
        Matrix.rotateM(modelMatrix, 0, -14f, 0f, 1f, 0f)
        Matrix.rotateM(modelMatrix, 0, 12f, 1f, 0f, 0f)
        Matrix.scaleM(modelMatrix, 0, 0.12f, 0.11f, 0.46f)
        GLES30.glUniform3f(uEmissiveLoc, 0f, 0f, 0f)
        GLES30.glUniform1f(uAlphaLoc, 1f)
        drawMesh(cubeMesh, armTex, modelMatrix)

        // Multi-Tool / Scanner Emitter Core on Wrist
        val glow = when {
            player.shieldActiveTimer > 0f -> Triple(0.45f, 0.35f, 1.0f)
            player.scannerActiveTimer > 0f -> Triple(0f, 1.0f, 0.95f)
            else -> Triple(0f, 0.55f, 0.78f)
        }
        Matrix.setIdentityM(modelMatrix, 0)
        Matrix.translateM(modelMatrix, 0, 0.25f + swayX, -0.23f + bobY * 0.4f + crouchOff, -0.68f + recoilZ)
        Matrix.rotateM(modelMatrix, 0, -14f, 0f, 1f, 0f)
        Matrix.scaleM(modelMatrix, 0, 0.09f, 0.07f, 0.20f)
        GLES30.glUniform3f(uEmissiveLoc, glow.first, glow.second, glow.third)
        drawMesh(cubeMesh, gadgetTex, modelMatrix)
    }

    private fun drawMesh(mesh: MeshBuffer, texId: Int, model: FloatArray) {
        if (mesh.vertexCount == 0) return
        Matrix.multiplyMM(mvpMatrix, 0, vpMatrix, 0, model, 0)
        GLES30.glUniformMatrix4fv(uMvpLoc, 1, false, mvpMatrix, 0)
        GLES30.glUniformMatrix4fv(uModelLoc, 1, false, model, 0)

        GLES30.glActiveTexture(GLES30.GL_TEXTURE0)
        GLES30.glBindTexture(GLES30.GL_TEXTURE_2D, texId)
        GLES30.glUniform1i(uTexLoc, 0)

        val stride = 8 * 4
        mesh.buffer.position(0)
        GLES30.glEnableVertexAttribArray(0)
        GLES30.glVertexAttribPointer(0, 3, GLES30.GL_FLOAT, false, stride, mesh.buffer)

        mesh.buffer.position(3)
        GLES30.glEnableVertexAttribArray(1)
        GLES30.glVertexAttribPointer(1, 3, GLES30.GL_FLOAT, false, stride, mesh.buffer)

        mesh.buffer.position(6)
        GLES30.glEnableVertexAttribArray(2)
        GLES30.glVertexAttribPointer(2, 2, GLES30.GL_FLOAT, false, stride, mesh.buffer)

        GLES30.glDrawArrays(GLES30.GL_TRIANGLES, 0, mesh.vertexCount)
    }

    private fun rebuildLevelIfNeeded() {
        val currentId = simulation.mission.id
        if (loadedMissionId != currentId) {
            loadedMissionId = currentId
            levelMeshes = MeshBuilder.buildLevelMeshes(simulation.mission)
        }
    }

    private fun trackFps() {
        frameCounter++
        val now = System.nanoTime()
        val elapsedNs = now - lastFpsTimestampNs
        if (elapsedNs >= 1_000_000_000L) {
            val fps = ((frameCounter * 1_000_000_000L) / elapsedNs).toInt().coerceIn(1, 120)
            onFpsMeasured(fps)
            frameCounter = 0
            lastFpsTimestampNs = now
        }
    }

    private fun buildShaderProgram(vsCode: String, fsCode: String): Int {
        val vs = compileShader(GLES30.GL_VERTEX_SHADER, vsCode)
        val fs = compileShader(GLES30.GL_FRAGMENT_SHADER, fsCode)
        val prog = GLES30.glCreateProgram()
        GLES30.glAttachShader(prog, vs)
        GLES30.glAttachShader(prog, fs)
        GLES30.glLinkProgram(prog)
        GLES30.glDeleteShader(vs)
        GLES30.glDeleteShader(fs)
        return prog
    }

    private fun compileShader(type: Int, src: String): Int {
        val shader = GLES30.glCreateShader(type)
        GLES30.glShaderSource(shader, src)
        GLES30.glCompileShader(shader)
        return shader
    }

    companion object {
        private const val VERTEX_SHADER = """#version 300 es
            layout(location = 0) in vec3 aPos;
            layout(location = 1) in vec3 aNormal;
            layout(location = 2) in vec2 aUV;

            uniform mat4 uMVP;
            uniform mat4 uModel;

            out vec3 vWorldPos;
            out vec3 vNormal;
            out vec2 vUV;

            void main() {
                vec4 worldPos = uModel * vec4(aPos, 1.0);
                vWorldPos = worldPos.xyz;
                vNormal = mat3(uModel) * aNormal;
                vUV = aUV;
                gl_Position = uMVP * vec4(aPos, 1.0);
            }
        """

        private const val FRAGMENT_SHADER = """#version 300 es
            precision mediump float;

            in vec3 vWorldPos;
            in vec3 vNormal;
            in vec2 vUV;

            uniform sampler2D uTex;
            uniform vec3 uCamPos;
            uniform vec3 uCamDir;
            uniform vec3 uFogColor;
            uniform float uFogDensity;
            uniform vec3 uEmissive;
            uniform float uAlpha;
            uniform int uEnableDynamicLight;
            uniform vec3 uPointLightPos;
            uniform vec3 uPointLightColor;

            out vec4 fragColor;

            void main() {
                vec4 albedo = texture(uTex, vUV);
                vec3 N = normalize(vNormal);
                vec3 V = normalize(uCamPos - vWorldPos);

                // Overhead sci-fi directional ambient light
                vec3 keyDir = normalize(vec3(0.35, 1.0, 0.25));
                float diff = max(dot(N, keyDir), 0.0) * 0.55 + 0.34;

                // Tactical player headlamp spotlight
                vec3 toFrag = vWorldPos - uCamPos;
                float dist = length(toFrag);
                vec3 L = normalize(-toFrag);
                float spotCos = dot(normalize(toFrag), normalize(uCamDir));
                float spot = smoothstep(0.72, 0.96, spotCos) * clamp(1.0 - dist / 18.0, 0.0, 1.0);
                float spotDiff = max(dot(N, L), 0.0) * spot * 0.92;

                // Specular metallic sheen
                vec3 H = normalize(L + V);
                float spec = pow(max(dot(N, H), 0.0), 24.0) * spot * 0.38;

                // Dynamic alert/scanner point light
                vec3 dynLight = vec3(0.0);
                if (uEnableDynamicLight == 1) {
                    vec3 pToF = uPointLightPos - vWorldPos;
                    float pDist = length(pToF);
                    float atten = 1.0 / (1.0 + 0.14 * pDist * pDist);
                    float pDiff = max(dot(N, normalize(pToF)), 0.0);
                    dynLight = uPointLightColor * pDiff * atten;
                }

                vec3 lit = albedo.rgb * (diff + spotDiff + dynLight) + vec3(spec) + uEmissive;

                // Exponential atmospheric depth fog
                float fogFactor = exp(-pow(dist * uFogDensity, 2.0));
                vec3 finalRgb = mix(uFogColor, lit, clamp(fogFactor, 0.0, 1.0));

                fragColor = vec4(finalRgb, albedo.a * uAlpha);
            }
        """
    }
}
