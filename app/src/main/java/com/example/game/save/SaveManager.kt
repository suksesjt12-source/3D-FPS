package com.example.game.save

import android.content.Context
import com.example.game.model.Difficulty
import com.example.game.model.EndingType
import com.example.game.model.GameSettings
import com.example.game.model.GraphicsQuality
import com.example.game.model.MissionId
import com.example.game.model.SaveData
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

class SaveManager(private val context: Context) {

    private val saveFile: File
        get() = File(context.filesDir, "operation_shadow_save.json")

    private val backupFile: File
        get() = File(context.filesDir, "operation_shadow_save.bak")

    fun load(): SaveData {
        val primary = tryReadFile(saveFile)
        if (primary != null) return primary

        val backup = tryReadFile(backupFile)
        if (backup != null) return backup

        return SaveData()
    }

    fun save(data: SaveData): Boolean {
        return try {
            val json = serialize(data).toString(2)
            if (saveFile.exists()) {
                saveFile.copyTo(backupFile, overwrite = true)
            }
            val tmp = File(context.filesDir, "operation_shadow_save.tmp")
            tmp.writeText(json)
            if (!tmp.renameTo(saveFile)) {
                saveFile.writeText(json)
                tmp.delete()
            }
            true
        } catch (_: Exception) {
            false
        }
    }

    fun resetCampaignKeepingSettings(current: SaveData): SaveData {
        val fresh = SaveData(settings = current.settings)
        save(fresh)
        return fresh
    }

    private fun tryReadFile(file: File): SaveData? {
        if (!file.exists()) return null
        return try {
            val text = file.readText()
            if (text.isBlank()) return null
            deserialize(JSONObject(text))
        } catch (_: Exception) {
            null
        }
    }

    fun serialize(data: SaveData): JSONObject {
        val root = JSONObject()
        root.put("saveVersion", data.saveVersion)
        root.put("currentMission", data.currentMission.name)
        root.put("unlockedMissions", JSONArray(data.unlockedMissions.map { it.name }))
        root.put("completedMissions", JSONArray(data.completedMissions.map { it.name }))
        root.put("unlockedEndings", JSONArray(data.unlockedEndings.map { it.name }))
        root.put("collectedLoreIds", JSONArray(data.collectedLoreIds.toList()))
        if (data.checkpointMission != null) {
            root.put("checkpointMission", data.checkpointMission.name)
            root.put("checkpointX", data.checkpointX.toDouble())
            root.put("checkpointZ", data.checkpointZ.toDouble())
            root.put("checkpointYaw", data.checkpointYaw.toDouble())
            root.put(
                "checkpointCompletedObjectives",
                JSONArray(data.checkpointCompletedObjectives.toList())
            )
            root.put(
                "checkpointActivatedInteractables",
                JSONArray(data.checkpointActivatedInteractables.toList())
            )
            root.put("checkpointInventoryIds", JSONArray(data.checkpointInventoryIds.toList()))
        }
        val timesObj = JSONObject()
        data.bestMissionTimesSec.forEach { (k, v) -> timesObj.put(k.name, v) }
        root.put("bestMissionTimesSec", timesObj)

        val s = data.settings
        val settingsObj = JSONObject().apply {
            put("graphicsQuality", s.graphicsQuality.name)
            put("resolutionScale", s.resolutionScale.toDouble())
            put("lookSensitivity", s.lookSensitivity.toDouble())
            put("invertY", s.invertY)
            put("fovDegrees", s.fovDegrees.toDouble())
            put("cameraShakeEnabled", s.cameraShakeEnabled)
            put("masterVolume", s.masterVolume.toDouble())
            put("musicVolume", s.musicVolume.toDouble())
            put("sfxVolume", s.sfxVolume.toDouble())
            put("voiceVolume", s.voiceVolume.toDouble())
            put("buttonSizeScale", s.buttonSizeScale.toDouble())
            put("buttonOpacity", s.buttonOpacity.toDouble())
            put("vibrationEnabled", s.vibrationEnabled)
            put("subtitlesEnabled", s.subtitlesEnabled)
            put("difficulty", s.difficulty.name)
        }
        root.put("settings", settingsObj)
        return root
    }

    fun deserialize(root: JSONObject): SaveData {
        val currentMission = parseEnum(root.optString("currentMission"), MissionId.MISSION_01)
        val unlockedMissions = parseEnumSet<MissionId>(root.optJSONArray("unlockedMissions"))
            .ifEmpty { setOf(MissionId.TRAINING, MissionId.MISSION_01) }
        val completedMissions = parseEnumSet<MissionId>(root.optJSONArray("completedMissions"))
        val unlockedEndings = parseEnumSet<EndingType>(root.optJSONArray("unlockedEndings"))
        val collectedLoreIds = parseStringSet(root.optJSONArray("collectedLoreIds"))

        val cpMissionStr = root.optString("checkpointMission", "")
        val checkpointMission = if (cpMissionStr.isNotBlank()) {
            runCatching { MissionId.valueOf(cpMissionStr) }.getOrNull()
        } else null

        val timesObj = root.optJSONObject("bestMissionTimesSec")
        val timesMap = mutableMapOf<MissionId, Int>()
        if (timesObj != null) {
            timesObj.keys().forEach { key ->
                runCatching { MissionId.valueOf(key) }.getOrNull()?.let { m ->
                    timesMap[m] = timesObj.optInt(key, 0)
                }
            }
        }

        val sObj = root.optJSONObject("settings")
        val settings = if (sObj != null) {
            GameSettings(
                graphicsQuality = parseEnum(
                    sObj.optString("graphicsQuality"),
                    GraphicsQuality.MEDIUM
                ),
                resolutionScale = sObj.optDouble("resolutionScale", 0.85).toFloat()
                    .coerceIn(0.6f, 1.0f),
                lookSensitivity = sObj.optDouble("lookSensitivity", 1.0).toFloat()
                    .coerceIn(0.4f, 2.5f),
                invertY = sObj.optBoolean("invertY", false),
                fovDegrees = sObj.optDouble("fovDegrees", 72.0).toFloat().coerceIn(58f, 90f),
                cameraShakeEnabled = sObj.optBoolean("cameraShakeEnabled", true),
                masterVolume = sObj.optDouble("masterVolume", 0.85).toFloat().coerceIn(0f, 1f),
                musicVolume = sObj.optDouble("musicVolume", 0.65).toFloat().coerceIn(0f, 1f),
                sfxVolume = sObj.optDouble("sfxVolume", 0.90).toFloat().coerceIn(0f, 1f),
                voiceVolume = sObj.optDouble("voiceVolume", 0.90).toFloat().coerceIn(0f, 1f),
                buttonSizeScale = sObj.optDouble("buttonSizeScale", 1.0).toFloat()
                    .coerceIn(0.75f, 1.35f),
                buttonOpacity = sObj.optDouble("buttonOpacity", 0.82).toFloat()
                    .coerceIn(0.35f, 1.0f),
                vibrationEnabled = sObj.optBoolean("vibrationEnabled", true),
                subtitlesEnabled = sObj.optBoolean("subtitlesEnabled", true),
                difficulty = parseEnum(sObj.optString("difficulty"), Difficulty.OPERATIVE)
            )
        } else {
            GameSettings()
        }

        return SaveData(
            saveVersion = root.optInt("saveVersion", 1),
            currentMission = currentMission,
            unlockedMissions = unlockedMissions,
            completedMissions = completedMissions,
            unlockedEndings = unlockedEndings,
            collectedLoreIds = collectedLoreIds,
            checkpointMission = checkpointMission,
            checkpointX = root.optDouble("checkpointX", 0.0).toFloat(),
            checkpointZ = root.optDouble("checkpointZ", 0.0).toFloat(),
            checkpointYaw = root.optDouble("checkpointYaw", 0.0).toFloat(),
            checkpointCompletedObjectives = parseStringSet(
                root.optJSONArray("checkpointCompletedObjectives")
            ),
            checkpointActivatedInteractables = parseStringSet(
                root.optJSONArray("checkpointActivatedInteractables")
            ),
            checkpointInventoryIds = parseStringSet(root.optJSONArray("checkpointInventoryIds")),
            bestMissionTimesSec = timesMap,
            settings = settings
        )
    }

    private inline fun <reified T : Enum<T>> parseEnum(raw: String, fallback: T): T {
        return runCatching { enumValueOf<T>(raw) }.getOrDefault(fallback)
    }

    private inline fun <reified T : Enum<T>> parseEnumSet(arr: JSONArray?): Set<T> {
        if (arr == null) return emptySet()
        val out = mutableSetOf<T>()
        for (i in 0 until arr.length()) {
            runCatching { enumValueOf<T>(arr.getString(i)) }.getOrNull()?.let { out.add(it) }
        }
        return out
    }

    private fun parseStringSet(arr: JSONArray?): Set<String> {
        if (arr == null) return emptySet()
        val out = mutableSetOf<String>()
        for (i in 0 until arr.length()) {
            out.add(arr.getString(i))
        }
        return out
    }
}
