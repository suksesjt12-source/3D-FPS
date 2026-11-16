package com.example

import androidx.test.core.app.ApplicationProvider
import com.example.game.model.Difficulty
import com.example.game.model.EndingType
import com.example.game.model.GameSettings
import com.example.game.model.GraphicsQuality
import com.example.game.model.MissionId
import com.example.game.model.SaveData
import com.example.game.save.SaveManager
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class SaveManagerTest {

    @Test
    fun saveAndLoadRoundTripPreservesProgressAndSettings() {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        val manager = SaveManager(context)

        val original = SaveData(
            currentMission = MissionId.MISSION_02,
            unlockedMissions = setOf(MissionId.TRAINING, MissionId.MISSION_01, MissionId.MISSION_02),
            completedMissions = setOf(MissionId.TRAINING, MissionId.MISSION_01),
            unlockedEndings = setOf(EndingType.NEURAL_CONTAINMENT),
            collectedLoreIds = setOf("lore_m1_lobby", "lore_m1_aris"),
            checkpointMission = MissionId.MISSION_02,
            checkpointX = 12.5f,
            checkpointZ = 18.2f,
            checkpointYaw = 90f,
            checkpointCompletedObjectives = setOf("m2_obj_cool"),
            bestMissionTimesSec = mapOf(MissionId.MISSION_01 to 142),
            settings = GameSettings(
                graphicsQuality = GraphicsQuality.HIGH,
                resolutionScale = 1.0f,
                lookSensitivity = 1.45f,
                invertY = true,
                difficulty = Difficulty.SHADOW
            )
        )

        assertTrue(manager.save(original))
        val loaded = manager.load()

        assertEquals(MissionId.MISSION_02, loaded.currentMission)
        assertEquals(original.unlockedMissions, loaded.unlockedMissions)
        assertEquals(original.completedMissions, loaded.completedMissions)
        assertEquals(original.unlockedEndings, loaded.unlockedEndings)
        assertEquals(original.collectedLoreIds, loaded.collectedLoreIds)
        assertEquals(12.5f, loaded.checkpointX, 0.01f)
        assertEquals(142, loaded.bestMissionTimesSec[MissionId.MISSION_01])
        assertEquals(GraphicsQuality.HIGH, loaded.settings.graphicsQuality)
        assertEquals(Difficulty.SHADOW, loaded.settings.difficulty)
        assertTrue(loaded.settings.invertY)
    }
}
