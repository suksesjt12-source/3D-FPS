package com.example.game.render

import com.example.game.model.MissionDef
import com.example.game.model.TileType
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

class MeshBuffer(vertices: FloatArray) {
    val vertexCount: Int = vertices.size / 8
    val buffer: FloatBuffer = ByteBuffer
        .allocateDirect(vertices.size * 4)
        .order(ByteOrder.nativeOrder())
        .asFloatBuffer()
        .apply {
            put(vertices)
            position(0)
        }
}

object MeshBuilder {

    /**
     * Builds batched static level meshes grouped by TextureAtlasBuilder.MaterialTex.
     */
    fun buildLevelMeshes(mission: MissionDef): Map<TextureAtlasBuilder.MaterialTex, MeshBuffer> {
        val buckets = mutableMapOf<TextureAtlasBuilder.MaterialTex, MutableList<Float>>()
        fun bucket(mat: TextureAtlasBuilder.MaterialTex) = buckets.getOrPut(mat) { mutableListOf() }

        val ts = mission.tileSize
        val wallH = 3.4f

        for (z in 0 until mission.height) {
            for (x in 0 until mission.width) {
                val tile = mission.grid[z][x]
                val wx = x * ts
                val wz = z * ts

                when (tile) {
                    TileType.FLOOR, TileType.HAZARD_FLOOR -> {
                        val floorMat = if (tile == TileType.HAZARD_FLOOR) {
                            TextureAtlasBuilder.MaterialTex.FLOOR_HAZARD
                        } else {
                            TextureAtlasBuilder.MaterialTex.FLOOR_METAL
                        }
                        addQuad(
                            bucket(floorMat),
                            wx, 0f, wz + ts,
                            wx + ts, 0f, wz + ts,
                            wx + ts, 0f, wz,
                            wx, 0f, wz,
                            0f, 1f, 0f,
                            1f, 1f
                        )
                        // Ceiling
                        addQuad(
                            bucket(TextureAtlasBuilder.MaterialTex.CEILING_TECH),
                            wx, wallH, wz,
                            wx + ts, wallH, wz,
                            wx + ts, wallH, wz + ts,
                            wx, wallH, wz + ts,
                            0f, -1f, 0f,
                            1f, 1f
                        )
                    }

                    TileType.PILLAR -> {
                        // Floor & Ceiling around pillar
                        addQuad(
                            bucket(TextureAtlasBuilder.MaterialTex.FLOOR_METAL),
                            wx, 0f, wz + ts,
                            wx + ts, 0f, wz + ts,
                            wx + ts, 0f, wz,
                            wx, 0f, wz,
                            0f, 1f, 0f,
                            1f, 1f
                        )
                        addQuad(
                            bucket(TextureAtlasBuilder.MaterialTex.CEILING_TECH),
                            wx, wallH, wz,
                            wx + ts, wallH, wz,
                            wx + ts, wallH, wz + ts,
                            wx, wallH, wz + ts,
                            0f, -1f, 0f,
                            1f, 1f
                        )
                        // Center architectural column + base plinth
                        val margin = ts * 0.2f
                        addBox(
                            bucket(TextureAtlasBuilder.MaterialTex.WALL_HAZARD),
                            wx + margin, 0f, wz + margin,
                            wx + ts - margin, 0.35f, wz + ts - margin
                        )
                        addBox(
                            bucket(TextureAtlasBuilder.MaterialTex.WALL_STEEL),
                            wx + margin * 1.25f, 0.35f, wz + margin * 1.25f,
                            wx + ts - margin * 1.25f, wallH, wz + ts - margin * 1.25f
                        )
                    }

                    TileType.WALL_STEEL,
                    TileType.WALL_LAB_GLASS,
                    TileType.WALL_SERVER,
                    TileType.WALL_HAZARD -> {
                        val mat = when (tile) {
                            TileType.WALL_LAB_GLASS -> TextureAtlasBuilder.MaterialTex.WALL_LAB_GLASS
                            TileType.WALL_SERVER -> TextureAtlasBuilder.MaterialTex.WALL_SERVER
                            TileType.WALL_HAZARD -> TextureAtlasBuilder.MaterialTex.WALL_HAZARD
                            else -> TextureAtlasBuilder.MaterialTex.WALL_STEEL
                        }
                        addBox(
                            bucket(mat),
                            wx, 0f, wz,
                            wx + ts, wallH, wz + ts
                        )
                    }

                    TileType.VOID -> Unit
                }

                // Overhead sci-fi conduits on every 3rd corridor tile
                if ((tile == TileType.FLOOR) && ((x + z) % 4 == 0)) {
                    addBox(
                        bucket(TextureAtlasBuilder.MaterialTex.ROBOT_METAL),
                        wx, wallH - 0.24f, wz + ts * 0.44f,
                        wx + ts, wallH - 0.08f, wz + ts * 0.56f
                    )
                }
            }
        }

        return buckets.mapValues { MeshBuffer(it.value.toFloatArray()) }
    }

    fun buildUnitCube(): MeshBuffer {
        val list = mutableListOf<Float>()
        addBox(list, -0.5f, -0.5f, -0.5f, 0.5f, 0.5f, 0.5f)
        return MeshBuffer(list.toFloatArray())
    }

    fun buildCylinder(segments: Int = 16): MeshBuffer {
        val out = mutableListOf<Float>()
        val step = (2.0 * PI / segments).toFloat()
        for (i in 0 until segments) {
            val a0 = i * step
            val a1 = (i + 1) * step
            val x0 = cos(a0) * 0.5f
            val z0 = sin(a0) * 0.5f
            val x1 = cos(a1) * 0.5f
            val z1 = sin(a1) * 0.5f

            val nx0 = cos(a0)
            val nz0 = sin(a0)
            val nx1 = cos(a1)
            val nz1 = sin(a1)

            // Side quad
            pushVertex(out, x0, -0.5f, z0, nx0, 0f, nz0, i.toFloat() / segments, 1f)
            pushVertex(out, x1, -0.5f, z1, nx1, 0f, nz1, (i + 1).toFloat() / segments, 1f)
            pushVertex(out, x1, 0.5f, z1, nx1, 0f, nz1, (i + 1).toFloat() / segments, 0f)

            pushVertex(out, x0, -0.5f, z0, nx0, 0f, nz0, i.toFloat() / segments, 1f)
            pushVertex(out, x1, 0.5f, z1, nx1, 0f, nz1, (i + 1).toFloat() / segments, 0f)
            pushVertex(out, x0, 0.5f, z0, nx0, 0f, nz0, i.toFloat() / segments, 0f)

            // Top cap
            pushVertex(out, 0f, 0.5f, 0f, 0f, 1f, 0f, 0.5f, 0.5f)
            pushVertex(out, x0, 0.5f, z0, 0f, 1f, 0f, 0.5f + x0, 0.5f + z0)
            pushVertex(out, x1, 0.5f, z1, 0f, 1f, 0f, 0.5f + x1, 0.5f + z1)
        }
        return MeshBuffer(out.toFloatArray())
    }

    fun buildVisionConeWedge(halfAngleDeg: Float = 38f, segments: Int = 12): MeshBuffer {
        val out = mutableListOf<Float>()
        val startRad = (-halfAngleDeg * PI / 180.0).toFloat()
        val endRad = (halfAngleDeg * PI / 180.0).toFloat()
        val step = (endRad - startRad) / segments

        for (i in 0 until segments) {
            val a0 = startRad + i * step
            val a1 = startRad + (i + 1) * step
            val x0 = sin(a0)
            val z0 = -cos(a0)
            val x1 = sin(a1)
            val z1 = -cos(a1)

            pushVertex(out, 0f, 0.04f, 0f, 0f, 1f, 0f, 0.5f, 0f)
            pushVertex(out, x0, 0.04f, z0, 0f, 1f, 0f, 0f, 1f)
            pushVertex(out, x1, 0.04f, z1, 0f, 1f, 0f, 1f, 1f)
        }
        return MeshBuffer(out.toFloatArray())
    }

    fun buildRing(segments: Int = 32): MeshBuffer {
        val out = mutableListOf<Float>()
        val step = (2.0 * PI / segments).toFloat()
        for (i in 0 until segments) {
            val a0 = i * step
            val a1 = (i + 1) * step
            val ix0 = cos(a0) * 0.88f
            val iz0 = sin(a0) * 0.88f
            val ox0 = cos(a0) * 1.0f
            val oz0 = sin(a0) * 1.0f
            val ix1 = cos(a1) * 0.88f
            val iz1 = sin(a1) * 0.88f
            val ox1 = cos(a1) * 1.0f
            val oz1 = sin(a1) * 1.0f

            pushVertex(out, ix0, 0.05f, iz0, 0f, 1f, 0f, 0f, 0f)
            pushVertex(out, ox0, 0.05f, oz0, 0f, 1f, 0f, 1f, 0f)
            pushVertex(out, ox1, 0.05f, oz1, 0f, 1f, 0f, 1f, 1f)

            pushVertex(out, ix0, 0.05f, iz0, 0f, 1f, 0f, 0f, 0f)
            pushVertex(out, ox1, 0.05f, oz1, 0f, 1f, 0f, 1f, 1f)
            pushVertex(out, ix1, 0.05f, iz1, 0f, 1f, 0f, 0f, 1f)
        }
        return MeshBuffer(out.toFloatArray())
    }

    private fun addBox(
        out: MutableList<Float>,
        x0: Float, y0: Float, z0: Float,
        x1: Float, y1: Float, z1: Float
    ) {
        // Front (+Z)
        addQuad(out, x0, y0, z1, x1, y0, z1, x1, y1, z1, x0, y1, z1, 0f, 0f, 1f, 1f, 1f)
        // Back (-Z)
        addQuad(out, x1, y0, z0, x0, y0, z0, x0, y1, z0, x1, y1, z0, 0f, 0f, -1f, 1f, 1f)
        // Left (-X)
        addQuad(out, x0, y0, z0, x0, y0, z1, x0, y1, z1, x0, y1, z0, -1f, 0f, 0f, 1f, 1f)
        // Right (+X)
        addQuad(out, x1, y0, z1, x1, y0, z0, x1, y1, z0, x1, y1, z1, 1f, 0f, 0f, 1f, 1f)
        // Top (+Y)
        addQuad(out, x0, y1, z1, x1, y1, z1, x1, y1, z0, x0, y1, z0, 0f, 1f, 0f, 1f, 1f)
        // Bottom (-Y)
        addQuad(out, x0, y0, z0, x1, y0, z0, x1, y0, z1, x0, y0, z1, 0f, -1f, 0f, 1f, 1f)
    }

    private fun addQuad(
        out: MutableList<Float>,
        x0: Float, y0: Float, z0: Float,
        x1: Float, y1: Float, z1: Float,
        x2: Float, y2: Float, z2: Float,
        x3: Float, y3: Float, z3: Float,
        nx: Float, ny: Float, nz: Float,
        uMax: Float, vMax: Float
    ) {
        pushVertex(out, x0, y0, z0, nx, ny, nz, 0f, vMax)
        pushVertex(out, x1, y1, z1, nx, ny, nz, uMax, vMax)
        pushVertex(out, x2, y2, z2, nx, ny, nz, uMax, 0f)

        pushVertex(out, x0, y0, z0, nx, ny, nz, 0f, vMax)
        pushVertex(out, x2, y2, z2, nx, ny, nz, uMax, 0f)
        pushVertex(out, x3, y3, z3, nx, ny, nz, 0f, 0f)
    }

    private fun pushVertex(
        out: MutableList<Float>,
        x: Float, y: Float, z: Float,
        nx: Float, ny: Float, nz: Float,
        u: Float, v: Float
    ) {
        out.add(x)
        out.add(y)
        out.add(z)
        out.add(nx)
        out.add(ny)
        out.add(nz)
        out.add(u)
        out.add(v)
    }
}
