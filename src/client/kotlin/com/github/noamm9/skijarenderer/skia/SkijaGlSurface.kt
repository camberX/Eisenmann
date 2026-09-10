package com.github.noamm9.skijarenderer.skia

import com.mojang.blaze3d.opengl.GlStateManager
import org.jetbrains.skija.BackendRenderTarget
import org.jetbrains.skija.Canvas
import org.jetbrains.skija.ColorSpace
import org.jetbrains.skija.DirectContext
import org.jetbrains.skija.Surface
import org.jetbrains.skija.SurfaceColorFormat
import org.jetbrains.skija.SurfaceOrigin
import org.lwjgl.opengl.GL11C
import org.lwjgl.opengl.GL30C
import org.lwjgl.opengl.GL33C

/**
 * Owns the GL framebuffer / Skija surface interop for drawing onto a Minecraft output
 * color texture. Binds the texture as an FBO, opens a Skija frame, runs the supplied
 * draw block, flushes, and restores prior GL state. Used by [SkijaCompositor].
 */
internal class SkijaGlSurface {
    private var context: DirectContext? = null
    private var renderTarget: BackendRenderTarget? = null
    private var surface: Surface? = null

    private var fbo = 0
    private var depthStencil = 0
    private var attachedWidth = 0
    private var attachedHeight = 0
    private var lastTextureId = 0

    fun render(
        width: Int, height: Int, rawWidth: Float, rawHeight: Float,
        dpr: Float, colorTexId: Int, clear: Boolean,
        draw: (Canvas) -> Unit
    ) {
        val previousFbo = GL11C.glGetInteger(GL30C.GL_FRAMEBUFFER_BINDING)
        val previousViewport = IntArray(4)
        GL11C.glGetIntegerv(GL11C.GL_VIEWPORT, previousViewport)

        bindTarget(colorTexId, width, height)

        GlStateManager._viewport(0, 0, width, height)
        GL33C.glBindSampler(0, 0)

        val directContext = context ?: DirectContext.makeGL().also { context = it }
        directContext.resetGLAll()

        val skijaSurface = surfaceFor(width, height, colorTexId)
        if (clear) skijaSurface.canvas.clear(0)

        Skija.beginFrame(skijaSurface.canvas, rawWidth, rawHeight, dpr)
        try {
            draw(skijaSurface.canvas)
        }
        finally {
            Skija.endFrame()
        }

        skijaSurface.flushAndSubmit()
        directContext.flush().submit(true)

        GL30C.glBindVertexArray(0)
        GL30C.glUseProgram(0)

        GlStateManager._disableDepthTest()
        GlStateManager._disableCull()
        GlStateManager._enableBlend()
        GlStateManager._blendFuncSeparate(770, 771, 1, 0)

        GlStateManager._glBindFramebuffer(GL30C.GL_FRAMEBUFFER, previousFbo)
        GlStateManager._viewport(previousViewport[0], previousViewport[1], previousViewport[2], previousViewport[3])
    }

    private fun bindTarget(colorTexId: Int, width: Int, height: Int) {
        if (fbo == 0) fbo = GlStateManager.glGenFramebuffers()
        GlStateManager._glBindFramebuffer(GL30C.GL_FRAMEBUFFER, fbo)
        GlStateManager._glFramebufferTexture2D(GL30C.GL_FRAMEBUFFER, GL30C.GL_COLOR_ATTACHMENT0, GL11C.GL_TEXTURE_2D, colorTexId, 0)

        if (depthStencil == 0 || attachedWidth != width || attachedHeight != height) {
            if (depthStencil != 0) GL30C.glDeleteRenderbuffers(depthStencil)
            depthStencil = GL30C.glGenRenderbuffers()
            GL30C.glBindRenderbuffer(GL30C.GL_RENDERBUFFER, depthStencil)
            GL30C.glRenderbufferStorage(GL30C.GL_RENDERBUFFER, GL30C.GL_DEPTH24_STENCIL8, width, height)
            GL30C.glBindRenderbuffer(GL30C.GL_RENDERBUFFER, 0)
            GL30C.glFramebufferRenderbuffer(GL30C.GL_FRAMEBUFFER, GL30C.GL_DEPTH_STENCIL_ATTACHMENT, GL30C.GL_RENDERBUFFER, depthStencil)
            attachedWidth = width
            attachedHeight = height
        }
    }

    private fun surfaceFor(width: Int, height: Int, textureId: Int): Surface {
        val existing = surface
        if (existing != null && existing.width == width && existing.height == height && lastTextureId == textureId) {
            return existing
        }

        surface?.close()
        renderTarget?.close()

        val directContext = context ?: DirectContext.makeGL().also { context = it }
        val target = BackendRenderTarget.makeGL(width, height, 0, 8, fbo, GL30C.GL_RGBA8)
        val created = Surface.makeFromBackendRenderTarget(
            directContext,
            target,
            SurfaceOrigin.BOTTOM_LEFT,
            SurfaceColorFormat.RGBA_8888,
            ColorSpace.getSRGB()
        )

        renderTarget = target
        surface = created
        lastTextureId = textureId
        return created
    }

    fun close() {
        surface?.close()
        surface = null
        renderTarget?.close()
        renderTarget = null
        if (depthStencil != 0) {
            GL30C.glDeleteRenderbuffers(depthStencil)
            depthStencil = 0
        }
        if (fbo != 0) {
            GlStateManager._glDeleteFramebuffers(fbo)
            fbo = 0
        }
        context?.close()
        context = null
    }
}
