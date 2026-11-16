package com.example.game.render

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Shader
import android.opengl.GLES30
import android.opengl.GLUtils

object TextureAtlasBuilder {

    enum class MaterialTex {
        WALL_STEEL,
        WALL_LAB_GLASS,
        WALL_SERVER,
        WALL_HAZARD,
        FLOOR_METAL,
        FLOOR_HAZARD,
        CEILING_TECH,
        DOOR_BLAST,
        PROP_CONSOLE,
        ROBOT_METAL
    }

    fun buildAllTextures(): Map<MaterialTex, Int> {
        val out = mutableMapOf<MaterialTex, Int>()
        MaterialTex.entries.forEach { mat ->
            val bmp = createTextureBitmap(mat)
            val texIds = IntArray(1)
            GLES30.glGenTextures(1, texIds, 0)
            val id = texIds[0]
            GLES30.glBindTexture(GLES30.GL_TEXTURE_2D, id)
            GLES30.glTexParameteri(GLES30.GL_TEXTURE_2D, GLES30.GL_TEXTURE_MIN_FILTER, GLES30.GL_LINEAR_MIPMAP_LINEAR)
            GLES30.glTexParameteri(GLES30.GL_TEXTURE_2D, GLES30.GL_TEXTURE_MAG_FILTER, GLES30.GL_LINEAR)
            GLES30.glTexParameteri(GLES30.GL_TEXTURE_2D, GLES30.GL_TEXTURE_WRAP_S, GLES30.GL_REPEAT)
            GLES30.glTexParameteri(GLES30.GL_TEXTURE_2D, GLES30.GL_TEXTURE_WRAP_T, GLES30.GL_REPEAT)
            GLUtils.texImage2D(GLES30.GL_TEXTURE_2D, 0, bmp, 0)
            GLES30.glGenerateMipmap(GLES30.GL_TEXTURE_2D)
            bmp.recycle()
            out[mat] = id
        }
        return out
    }

    private fun createTextureBitmap(mat: MaterialTex): Bitmap {
        val size = 256
        val bmp = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val c = Canvas(bmp)
        val p = Paint(Paint.ANTI_ALIAS_FLAG)

        when (mat) {
            MaterialTex.WALL_STEEL -> {
                p.shader = LinearGradient(0f, 0f, 0f, size.toFloat(), Color.rgb(34, 46, 62), Color.rgb(20, 28, 40), Shader.TileMode.CLAMP)
                c.drawRect(0f, 0f, size.toFloat(), size.toFloat(), p)
                p.shader = null
                // Bulkhead ribs & seams
                p.color = Color.rgb(14, 20, 29)
                p.strokeWidth = 4f
                c.drawLine(0f, 128f, 256f, 128f, p)
                c.drawLine(128f, 0f, 128f, 256f, p)
                p.color = Color.rgb(58, 76, 98)
                p.style = Paint.Style.STROKE
                p.strokeWidth = 3f
                c.drawRect(14f, 14f, 114f, 114f, p)
                c.drawRect(142f, 14f, 242f, 114f, p)
                c.drawRect(14f, 142f, 114f, 242f, p)
                c.drawRect(142f, 142f, 242f, 242f, p)
                // Cyan LED accent strip
                p.style = Paint.Style.FILL
                p.color = Color.rgb(0, 215, 245)
                c.drawRect(0f, 198f, 256f, 205f, p)
            }

            MaterialTex.WALL_LAB_GLASS -> {
                c.drawColor(Color.rgb(14, 34, 48))
                p.color = Color.rgb(38, 54, 72)
                c.drawRect(0f, 0f, 256f, 44f, p)
                c.drawRect(0f, 210f, 256f, 256f, p)
                c.drawRect(0f, 0f, 24f, 256f, p)
                c.drawRect(232f, 0f, 256f, 256f, p)
                // Holographic grid lines on lab glass
                p.color = Color.argb(120, 0, 229, 255)
                p.strokeWidth = 2f
                for (i in 48..208 step 32) {
                    c.drawLine(24f, i.toFloat(), 232f, i.toFloat(), p)
                }
                for (i in 56..200 step 48) {
                    c.drawLine(i.toFloat(), 44f, i.toFloat(), 210f, p)
                }
            }

            MaterialTex.WALL_SERVER -> {
                c.drawColor(Color.rgb(12, 18, 28))
                p.color = Color.rgb(24, 32, 46)
                c.drawRect(16f, 12f, 118f, 244f, p)
                c.drawRect(138f, 12f, 240f, 244f, p)
                for (y in 26..226 step 22) {
                    p.color = Color.rgb(8, 12, 20)
                    c.drawRect(24f, y.toFloat(), 110f, y + 12f, p)
                    c.drawRect(146f, y.toFloat(), 232f, y + 12f, p)
                    p.color = if ((y / 22) % 3 == 0) Color.rgb(0, 230, 118) else Color.rgb(0, 229, 255)
                    c.drawRect(30f, y + 4f, 42f, y + 8f, p)
                    c.drawRect(152f, y + 4f, 166f, y + 8f, p)
                    p.color = Color.rgb(255, 179, 0)
                    c.drawRect(94f, y + 4f, 102f, y + 8f, p)
                }
            }

            MaterialTex.WALL_HAZARD -> {
                c.drawColor(Color.rgb(32, 36, 42))
                p.color = Color.rgb(245, 175, 0)
                p.strokeWidth = 24f
                for (x in -256..512 step 56) {
                    c.drawLine(x.toFloat(), 180f, x + 76f, 256f, p)
                }
                p.color = Color.rgb(20, 25, 33)
                c.drawRect(0f, 0f, 256f, 180f, p)
                p.color = Color.rgb(255, 160, 0)
                c.drawRect(0f, 174f, 256f, 182f, p)
            }

            MaterialTex.FLOOR_METAL -> {
                c.drawColor(Color.rgb(24, 32, 43))
                p.color = Color.rgb(34, 46, 61)
                p.strokeWidth = 3f
                p.style = Paint.Style.STROKE
                c.drawRect(4f, 4f, 252f, 252f, p)
                c.drawLine(128f, 0f, 128f, 256f, p)
                c.drawLine(0f, 128f, 256f, 128f, p)
                p.style = Paint.Style.FILL
                p.color = Color.rgb(16, 22, 30)
                for (gx in 22..230 step 32) {
                    for (gy in 22..230 step 32) {
                        c.drawRect(gx.toFloat(), gy.toFloat(), gx + 14f, gy + 8f, p)
                    }
                }
            }

            MaterialTex.FLOOR_HAZARD -> {
                c.drawColor(Color.rgb(48, 22, 12))
                p.color = Color.rgb(255, 145, 0)
                p.strokeWidth = 14f
                for (x in -256..512 step 48) {
                    c.drawLine(x.toFloat(), 0f, x + 256f, 256f, p)
                }
                p.color = Color.argb(110, 255, 60, 20)
                c.drawRect(0f, 0f, 256f, 256f, p)
            }

            MaterialTex.CEILING_TECH -> {
                c.drawColor(Color.rgb(14, 19, 27))
                p.color = Color.rgb(24, 33, 46)
                c.drawRect(12f, 12f, 244f, 244f, p)
                p.color = Color.rgb(185, 240, 255)
                c.drawRect(88f, 36f, 168f, 52f, p)
                c.drawRect(88f, 204f, 168f, 220f, p)
            }

            MaterialTex.DOOR_BLAST -> {
                c.drawColor(Color.rgb(46, 58, 74))
                p.color = Color.rgb(26, 35, 48)
                c.drawRect(16f, 16f, 240f, 240f, p)
                p.color = Color.rgb(0, 229, 255)
                c.drawRect(118f, 24f, 138f, 232f, p)
                p.color = Color.rgb(255, 179, 0)
                c.drawRect(24f, 214f, 232f, 232f, p)
            }

            MaterialTex.PROP_CONSOLE -> {
                c.drawColor(Color.rgb(18, 28, 42))
                p.color = Color.rgb(0, 195, 235)
                c.drawRect(24f, 36f, 232f, 148f, p)
                p.color = Color.rgb(8, 20, 32)
                c.drawRect(32f, 44f, 224f, 140f, p)
                p.color = Color.rgb(0, 230, 118)
                for (y in 56..126 step 16) {
                    c.drawRect(44f, y.toFloat(), (120 + (y % 70)).toFloat(), y + 7f, p)
                }
            }

            MaterialTex.ROBOT_METAL -> {
                p.shader = LinearGradient(0f, 0f, 256f, 256f, Color.rgb(72, 84, 98), Color.rgb(34, 42, 54), Shader.TileMode.CLAMP)
                c.drawRect(0f, 0f, 256f, 256f, p)
                p.shader = null
                p.color = Color.rgb(18, 24, 32)
                p.strokeWidth = 5f
                c.drawLine(0f, 64f, 256f, 64f, p)
                c.drawLine(0f, 192f, 256f, 192f, p)
            }
        }
        return bmp
    }
}
