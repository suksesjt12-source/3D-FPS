package com.example.game.engine

import com.example.game.model.TileType
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

object Math3D {
    const val DEG_TO_RAD = (PI / 180.0).toFloat()
    const val RAD_TO_DEG = (180.0 / PI).toFloat()

    fun clamp(v: Float, minV: Float, maxV: Float): Float = when {
        v < minV -> minV
        v > maxV -> maxV
        else -> v
    }

    fun lerp(a: Float, b: Float, t: Float): Float = a + (b - a) * clamp(t, 0f, 1f)

    fun normalizeAngleDeg(deg: Float): Float {
        var a = deg % 360f
        if (a > 180f) a -= 360f
        if (a < -180f) a += 360f
        return a
    }

    fun lerpAngleDeg(current: Float, target: Float, t: Float): Float {
        val diff = normalizeAngleDeg(target - current)
        return normalizeAngleDeg(current + diff * clamp(t, 0f, 1f))
    }

    fun distance2D(x1: Float, z1: Float, x2: Float, z2: Float): Float {
        val dx = x2 - x1
        val dz = z2 - z1
        return sqrt(dx * dx + dz * dz)
    }

    fun forwardVectorFromYawDeg(yawDeg: Float): Pair<Float, Float> {
        val rad = yawDeg * DEG_TO_RAD
        // Yaw = 0 faces -Z, Yaw = +90 faces +X
        return sin(rad) to -cos(rad)
    }

    fun yawDegToTarget(fromX: Float, fromZ: Float, toX: Float, toZ: Float): Float {
        val dx = toX - fromX
        val dz = toZ - fromZ
        return atan2(dx, -dz) * RAD_TO_DEG
    }

    fun isTileSolid(tile: TileType): Boolean = when (tile) {
        TileType.WALL_STEEL,
        TileType.WALL_LAB_GLASS,
        TileType.WALL_SERVER,
        TileType.WALL_HAZARD,
        TileType.PILLAR,
        TileType.VOID -> true
        TileType.FLOOR,
        TileType.HAZARD_FLOOR -> false
    }

    /**
     * Checks line of sight between two 2D world points against solid walls and closed doors.
     */
    fun hasLineOfSight(
        x0: Float,
        z0: Float,
        x1: Float,
        z1: Float,
        grid: Array<Array<TileType>>,
        tileSize: Float,
        closedDoorTiles: Set<Pair<Int, Int>>
    ): Boolean {
        val dist = distance2D(x0, z0, x1, z1)
        if (dist < 0.1f) return true
        val steps = (dist / (tileSize * 0.18f)).toInt().coerceAtLeast(2)
        val height = grid.size
        val width = if (height > 0) grid[0].size else 0
        for (i in 1 until steps) {
            val t = i.toFloat() / steps.toFloat()
            val sx = x0 + (x1 - x0) * t
            val sz = z0 + (z1 - z0) * t
            val gx = (sx / tileSize).toInt()
            val gz = (sz / tileSize).toInt()
            if (gz !in 0 until height || gx !in 0 until width) return false
            val tile = grid[gz][gx]
            // Glass lab walls allow sight if WALL_LAB_GLASS, solid bulkheads block sight!
            if (isTileSolid(tile) && tile != TileType.WALL_LAB_GLASS) return false
            if (closedDoorTiles.contains(gx to gz)) return false
        }
        return true
    }

    /**
     * Resolves circle-vs-grid-tile sliding collision so player & robots slide smoothly along walls.
     */
    fun resolveCircleGridCollision(
        x: Float,
        z: Float,
        radius: Float,
        grid: Array<Array<TileType>>,
        tileSize: Float,
        closedDoorTiles: Set<Pair<Int, Int>>
    ): Pair<Float, Float> {
        var rx = x
        var rz = z
        val height = grid.size
        val width = if (height > 0) grid[0].size else 0

        val minGx = ((rx - radius) / tileSize).toInt() - 1
        val maxGx = ((rx + radius) / tileSize).toInt() + 1
        val minGz = ((rz - radius) / tileSize).toInt() - 1
        val maxGz = ((rz + radius) / tileSize).toInt() + 1

        for (gz in minGz..maxGz) {
            for (gx in minGx..maxGx) {
                val solid = if (gz !in 0 until height || gx !in 0 until width) {
                    true
                } else {
                    isTileSolid(grid[gz][gx]) || closedDoorTiles.contains(gx to gz)
                }
                if (!solid) continue

                val tileMinX = gx * tileSize
                val tileMaxX = (gx + 1) * tileSize
                val tileMinZ = gz * tileSize
                val tileMaxZ = (gz + 1) * tileSize

                val closestX = clamp(rx, tileMinX, tileMaxX)
                val closestZ = clamp(rz, tileMinZ, tileMaxZ)
                val dx = rx - closestX
                val dz = rz - closestZ
                val distSq = dx * dx + dz * dz
                if (distSq < radius * radius) {
                    val dist = sqrt(distSq)
                    if (dist > 0.0001f) {
                        val push = radius - dist
                        rx += (dx / dist) * push
                        rz += (dz / dist) * push
                    } else {
                        val centerX = (tileMinX + tileMaxX) * 0.5f
                        val centerZ = (tileMinZ + tileMaxZ) * 0.5f
                        val pdx = rx - centerX
                        val pdz = rz - centerZ
                        if (abs(pdx) > abs(pdz)) {
                            rx += if (pdx >= 0) radius else -radius
                        } else {
                            rz += if (pdz >= 0) radius else -radius
                        }
                    }
                }
            }
        }
        return rx to rz
    }
}
