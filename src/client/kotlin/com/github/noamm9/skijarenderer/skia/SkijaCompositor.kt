package com.github.noamm9.skijarenderer.skia

import com.mojang.blaze3d.opengl.GlTexture
import dev.stray.Stray
import net.minecraft.client.Minecraft

/**
 * Composites the global Skija batch onto the Minecraft color texture.
 * <p>
 * Stray flushes this before {@code GuiRenderer.render} so panels sit under
 * vanilla text and items.
 */
object SkijaCompositor {
    private val glSurface = SkijaGlSurface()
    private var failed = false

    @JvmStatic
    fun composite() {
        if (failed) {
            if (Skija.hasBatch()) Skija.discard()
            return
        }
        if (!Skija.hasBatch()) return

        try {
            val minecraft = Minecraft.getInstance()
            val window = minecraft.window
            val mainRenderTarget = minecraft.mainRenderTarget

            val width = mainRenderTarget.width.takeIf { it > 0 } ?: run {
                Skija.discard()
                return
            }
            val height = mainRenderTarget.height.takeIf { it > 0 } ?: run {
                Skija.discard()
                return
            }
            val colorTexId = (mainRenderTarget.colorTexture as? GlTexture)?.glId() ?: run {
                Skija.discard()
                return
            }

            val rawWidth = width.toFloat()
            val rawHeight = height.toFloat()
            val guiWidth = window.guiScaledWidth.toFloat().coerceAtLeast(1f)
            val dpr = (rawWidth / guiWidth).takeIf { it.isFinite() && it > 0f } ?: 1f

            glSurface.render(width, height, rawWidth, rawHeight, dpr, colorTexId, clear = false) {
                Skija.flush()
            }
        } catch (t: Throwable) {
            failed = true
            Skija.discard()
            Stray.LOGGER.error("Skija GUI composite failed", t)
        }
    }

    @JvmStatic
    fun close() {
        Skija.discard()
        glSurface.close()
    }
}
