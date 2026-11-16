package com.example

import com.example.game.data.MissionCatalog
import com.example.game.engine.Math3D
import com.example.game.engine.SimulationEngine
import com.example.game.model.Difficulty
import com.example.game.model.EnemyState
import com.example.game.model.GameSettings
import com.example.game.model.GraphicsQuality
import com.example.game.model.MissionId
import com.example.game.model.SaveData
import com.example.game.model.TileType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class OperationShadowEngineTest {

    @Test
    fun allMissionsHaveValidSpawnsAndWaypoints() {
        for (mission in MissionCatalog.allMissions) {
            val spawnTile = mission.grid[mission.spawnGridZ][mission.spawnGridX]
            assertFalse(
                "Mission ${mission.id} spawn should be walkable",
                Math3D.isTileSolid(spawnTile)
            )
            assertEquals(mission.height, mission.grid.size)
            assertEquals(mission.width, mission.grid[0].size)

            for (enemy in mission.enemies) {
                assertTrue("Enemy ${enemy.id} must have waypoints", enemy.waypoints.isNotEmpty())
                for ((wx, wz) in enemy.waypoints) {
                    val wpTile = mission.grid[wz][wx]
                    assertFalse(
                        "Enemy ${enemy.id} waypoint ($wx,$wz) in ${mission.id} must be walkable",
                        Math3D.isTileSolid(wpTile)
                    )
                }
            }

            for (inter in mission.interactables) {
                val tile = mission.grid[inter.gridZ][inter.gridX]
                assertFalse(
                    "Interactable ${inter.id} at (${inter.gridX},${inter.gridZ}) in ${mission.id} must be walkable",
                    Math3D.isTileSolid(tile)
                )
            }
        }
    }

    @Test
    fun playerMovementStaminaAndCollisionWork() {
        val sim = SimulationEngine()
        val settings = GameSettings(difficulty = Difficulty.OPERATIVE, graphicsQuality = GraphicsQuality.MEDIUM)
        sim.loadMission(MissionId.TRAINING, SaveData(), fromCheckpoint = false)

        val startX = sim.player.x
        val startStamina = sim.player.stamina

        // Sprint forward for 0.8 seconds
        repeat(40) {
            sim.step(0.02f, 0f, 1f, sprintHeld = true, settings = settings)
        }

        assertTrue("Player should advance in world", sim.player.x > startX + 0.5f)
        assertTrue("Sprinting should consume stamina", sim.player.stamina < startStamina)
        assertTrue("Player should stay inside facility bounds", sim.player.x > 0f && sim.player.z > 0f)
    }

    @Test
    fun decoyLuresNearbyEnemyToInvestigate() {
        val sim = SimulationEngine()
        val settings = GameSettings()
        sim.loadMission(MissionId.TRAINING, SaveData(), fromCheckpoint = false)

        // Position player near the patrol hall so decoy is within 15m of Sentinel
        sim.deployDecoy()
        assertNotNull("Decoy beacon should exist after deployment", sim.decoyBeacon)
        assertTrue("Decoy cooldown should activate", sim.player.decoyCooldown > 0f)

        val sentinel = sim.enemies.first()
        val dist = Math3D.distance2D(sentinel.x, sentinel.z, sim.decoyBeacon!!.x, sim.decoyBeacon!!.z)
        if (dist <= 15.5f) {
            assertEquals(EnemyState.INVESTIGATE, sentinel.state)
        }
    }

    @Test
    fun lineOfSightBlockedBySteelWalls() {
        val mission = MissionCatalog.getMission(MissionId.MISSION_01)
        val ts = mission.tileSize
        // (2,2) is Main Lobby, (9,2) is across steel wall at col 7
        val visible = Math3D.hasLineOfSight(
            2.5f * ts, 1.5f * ts,
            9.5f * ts, 1.5f * ts,
            mission.grid, ts, emptySet()
        )
        assertFalse("Steel wall between Lobby and Security should block LoS", visible)

        // (2,2) to (5,2) within Lobby should have clear LoS
        val lobbyVisible = Math3D.hasLineOfSight(
            2.5f * ts, 1.5f * ts,
            5.5f * ts, 1.5f * ts,
            mission.grid, ts, emptySet()
        )
        assertTrue("Same room LoS should be clear", lobbyVisible)
    }

    @Test
    fun energyShieldBlocksHazardDamage() {
        val sim = SimulationEngine()
        val settings = GameSettings()
        sim.loadMission(MissionId.MISSION_02, SaveData(), fromCheckpoint = false)
        sim.activateEnergyShield()
        assertTrue(sim.player.shieldActiveTimer > 0f)
        // Step simulation and verify health remains full
        repeat(10) {
            sim.step(0.02f, 0f, 0f, false, settings)
        }
        assertEquals(100f, sim.player.health, 0.01f)
    }
}
