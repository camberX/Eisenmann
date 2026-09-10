@file:Suppress("unused")

package com.github.noamm9.skijarenderer.skia

import org.jetbrains.skija.*
import org.jetbrains.skija.svg.SVGDOM
import org.jetbrains.skija.svg.SVGLengthContext
import org.joml.Matrix3x2fc
import java.awt.Color
import kotlin.math.max
import kotlin.math.round

object Skija {
    private val imageCache = HashMap<SkijaImage, CachedImage>()
    private val typefaces = HashMap<SkijaFont, Typeface>()
    private val fonts = HashMap<FontKey, Font>()
    private var canvas: Canvas? = null
    private var frameSaveCount = 0
    private var globalAlpha = 1f
    private val batch = ArrayList<() -> Unit>()
    private var consumed = false

    val defaultFont = SkijaFont("assets/skijarenderer/inter.ttf")

    fun beginFrame(canvas: Canvas, width: Float, height: Float, dpr: Float) {
        this.canvas = canvas
        this.globalAlpha = 1f
        frameSaveCount = canvas.save()
        canvas.clipRect(Rect.makeXYWH(0f, 0f, width, height))
        canvas.scale(dpr, dpr)
    }

    fun endFrame() {
        currentCanvas().restoreToCount(frameSaveCount)
        canvas = null
        frameSaveCount = 0
        globalAlpha = 1f
    }

    fun push() = dispatch { currentCanvas().save() }

    fun pop() = dispatch { currentCanvas().restore() }

    fun scale(x: Number, y: Number) = dispatch { currentCanvas().scale(x.toFloat(), y.toFloat()) }

    fun scale(n: Number) = scale(n, n)

    fun translate(x: Number, y: Number) = dispatch { currentCanvas().translate(x.toFloat(), y.toFloat()) }

    fun rotate(radians: Number) = dispatch {
        currentCanvas().rotate(Math.toDegrees(radians.toDouble()).toFloat())
    }

    fun transform(matrix: Matrix3x2fc) = dispatch { currentCanvas().concat(matrix.toSkijaMatrix()) }

    fun globalAlpha(amount: Number) = dispatch {
        globalAlpha = amount.toFloat().coerceIn(0f, 1f)
    }

    fun pushScissor(x: Number, y: Number, w: Number, h: Number) = dispatch {
        currentCanvas().save()
        currentCanvas().clipRect(Rect.makeXYWH(x.toFloat(), y.toFloat(), w.toFloat(), h.toFloat()))
    }

    fun popScissor() = dispatch { currentCanvas().restore() }

    fun line(x1: Number, y1: Number, x2: Number, y2: Number, thickness: Number, color: Color) = dispatch {
        paint(color, PaintMode.STROKE).use {
            it.strokeWidth = thickness.toFloat()
            currentCanvas().drawLine(x1.toFloat(), y1.toFloat(), x2.toFloat(), y2.toFloat(), it)
        }
    }

    fun drawHalfRoundedRect(x: Number, y: Number, w: Number, h: Number, color: Color, radius: Number, roundTop: Boolean) = dispatch {
        val fx = x.toFloat()
        val fy = y.toFloat()
        val fw = w.toFloat()
        val fh = h.toFloat()
        val fr = radius.toFloat().coerceAtLeast(0f)
        val radii = if (roundTop) {
            floatArrayOf(fr, fr, fr, fr, 0f, 0f, 0f, 0f)
        }
        else {
            floatArrayOf(0f, 0f, 0f, 0f, fr, fr, fr, fr)
        }

        paint(color).use {
            currentCanvas().drawRRect(RRect.makeComplexXYWH(fx, fy, fw, fh, radii), it)
        }
    }

    fun rect(x: Number, y: Number, w: Number, h: Number, color: Color, radius: Number) = dispatch {
        paint(color).use {
            currentCanvas().drawRRect(RRect.makeXYWH(x.toFloat(), y.toFloat(), w.toFloat(), h.toFloat(), radius.toFloat()), it)
        }
    }

    fun rect(x: Number, y: Number, w: Number, h: Number, color: Color) = dispatch {
        paint(color).use {
            currentCanvas().drawRect(Rect.makeXYWH(x.toFloat(), y.toFloat(), w.toFloat(), h.toFloat()), it)
        }
    }

    fun rect(x: Number, y: Number, w: Number, h: Number, argb: Int) = dispatch {
        paint(argb).use {
            currentCanvas().drawRect(Rect.makeXYWH(x.toFloat(), y.toFloat(), w.toFloat(), h.toFloat()), it)
        }
    }

    fun rect(x: Number, y: Number, w: Number, h: Number, argb: Int, radius: Number) = dispatch {
        paint(argb).use {
            currentCanvas().drawRRect(RRect.makeXYWH(x.toFloat(), y.toFloat(), w.toFloat(), h.toFloat(), radius.toFloat()), it)
        }
    }

    fun rrect(
        x: Number,
        y: Number,
        w: Number,
        h: Number,
        argb: Int,
        tl: Number,
        tr: Number,
        br: Number,
        bl: Number
    ) = dispatch {
        val radii = floatArrayOf(
            tl.toFloat(), tl.toFloat(),
            tr.toFloat(), tr.toFloat(),
            br.toFloat(), br.toFloat(),
            bl.toFloat(), bl.toFloat()
        )
        paint(argb).use {
            currentCanvas().drawRRect(RRect.makeComplexXYWH(x.toFloat(), y.toFloat(), w.toFloat(), h.toFloat(), radii), it)
        }
    }

    fun hollowRect(x: Number, y: Number, w: Number, h: Number, thickness: Number, color: Color, radius: Number) = dispatch {
        paint(color, PaintMode.STROKE).use {
            it.strokeWidth = thickness.toFloat()
            currentCanvas().drawRRect(RRect.makeXYWH(x.toFloat(), y.toFloat(), w.toFloat(), h.toFloat(), radius.toFloat()), it)
        }
    }

    fun hollowRect(x: Number, y: Number, w: Number, h: Number, thickness: Number, argb: Int, radius: Number) = dispatch {
        paint(argb, PaintMode.STROKE).use {
            it.strokeWidth = thickness.toFloat()
            it.strokeJoin = PaintStrokeJoin.ROUND
            currentCanvas().drawRRect(RRect.makeXYWH(x.toFloat(), y.toFloat(), w.toFloat(), h.toFloat(), radius.toFloat()), it)
        }
    }

    fun gradientRect(
        x: Number,
        y: Number,
        w: Number,
        h: Number,
        color1: Color,
        color2: Color,
        gradient: SkijaGradient,
        radius: Float
    ) = dispatch {
        paint(Color.WHITE).use { fill ->
            linearGradient(x.toFloat(), y.toFloat(), w.toFloat(), h.toFloat(), color1, color2, gradient).use { shader ->
                fill.shader = shader
                currentCanvas().drawRRect(RRect.makeXYWH(x.toFloat(), y.toFloat(), w.toFloat(), h.toFloat(), radius), fill)
            }
        }
    }

    fun gradientRect(
        x: Number,
        y: Number,
        w: Number,
        h: Number,
        argb1: Int,
        argb2: Int,
        gradient: SkijaGradient,
        radius: Number
    ) = dispatch {
        paint(0xFFFFFFFF.toInt()).use { fill ->
            linearGradient(x.toFloat(), y.toFloat(), w.toFloat(), h.toFloat(), argb1, argb2, gradient).use { shader ->
                fill.shader = shader
                currentCanvas().drawRRect(RRect.makeXYWH(x.toFloat(), y.toFloat(), w.toFloat(), h.toFloat(), radius.toFloat()), fill)
            }
        }
    }

    fun dropShadow(x: Number, y: Number, width: Number, height: Number, blur: Number, spread: Number, radius: Number) = dispatch {
        val rect = Rect.makeXYWH(
            x.toFloat() - spread.toFloat(),
            y.toFloat() - spread.toFloat(),
            width.toFloat() + spread.toFloat() * 2f,
            height.toFloat() + spread.toFloat() * 2f
        )
        currentCanvas().drawRectShadow(rect, 0f, 0f, blur.toFloat(), radius.toFloat(), Color(0, 0, 0, 125).toSkijaColor())
    }

    fun circle(x: Number, y: Number, radius: Number, color: Color) = dispatch {
        paint(color).use {
            currentCanvas().drawCircle(x.toFloat(), y.toFloat(), radius.toFloat(), it)
        }
    }

    fun circle(x: Number, y: Number, radius: Number, argb: Int) = dispatch {
        paint(argb).use {
            currentCanvas().drawCircle(x.toFloat(), y.toFloat(), radius.toFloat(), it)
        }
    }

    fun line(x1: Number, y1: Number, x2: Number, y2: Number, thickness: Number, argb: Int) = dispatch {
        paint(argb, PaintMode.STROKE).use {
            it.strokeWidth = thickness.toFloat()
            it.strokeCap = PaintStrokeCap.ROUND
            currentCanvas().drawLine(x1.toFloat(), y1.toFloat(), x2.toFloat(), y2.toFloat(), it)
        }
    }

    fun text(text: String, x: Number, y: Number, size: Number, color: Color, font: SkijaFont = defaultFont) = dispatch {
        paint(color).use { fill ->
            val skijaFont = skijaFont(font, size.toFloat())
            currentCanvas().drawString(text, x.toFloat(), y.toFloat() - skijaFont.metrics.ascent, skijaFont, fill)
        }
    }

    fun textGradient(
        text: String,
        x: Number,
        y: Number,
        size: Number,
        width: Number,
        color1: Color,
        color2: Color,
        font: SkijaFont = defaultFont,
        direction: SkijaGradient = SkijaGradient.LEFT_RIGHT
    ) {
        if (text.isEmpty()) return

        dispatch {
            paint(Color.WHITE).use { fill ->
                linearGradient(x.toFloat(), y.toFloat(), width.toFloat(), size.toFloat(), color1, color2, direction).use { shader ->
                    fill.shader = shader
                    val skijaFont = skijaFont(font, size.toFloat())
                    currentCanvas().drawString(text, x.toFloat(), y.toFloat() - skijaFont.metrics.ascent, skijaFont, fill)
                }
            }
        }
    }

    fun textShadow(text: String, x: Number, y: Number, size: Number, color: Color, font: SkijaFont = defaultFont) {
        text(text, round(x.toFloat() + 2f), round(y.toFloat() + 2f), size, Color.BLACK, font)
        text(text, round(x.toFloat()), round(y.toFloat()), size, color, font)
    }

    fun textWidth(text: String, size: Number, font: SkijaFont = defaultFont): Float {
        return skijaFont(font, size.toFloat()).measureTextWidth(text)
    }

    fun drawWrappedString(text: String, x: Number, y: Number, w: Number, size: Number, color: Color, font: SkijaFont = defaultFont, lineHeight: Number = 1f) {
        var cursorY = y.toFloat()
        val spacing = size.toFloat() * lineHeight.toFloat()
        wrap(text, w.toFloat(), size.toFloat(), font).forEach { line ->
            text(line, x, cursorY, size, color, font)
            cursorY += spacing
        }
    }

    fun createImage(resourcePath: String): SkijaImage {
        val image = imageCache.keys.find { it.location == resourcePath } ?: SkijaImage(resourcePath)
        val cached = imageCache.getOrPut(image) { CachedImage(0, loadImage(image)) }
        cached.count ++
        return image
    }

    fun deleteImage(image: SkijaImage) {
        val cached = imageCache[image] ?: return
        cached.count --
        if (cached.count > 0) return

        cached.image.close()
        imageCache.remove(image)
    }

    fun image(image: SkijaImage, x: Number, y: Number, w: Number, h: Number, radius: Number) = dispatch {
        val rect = Rect.makeXYWH(x.toFloat(), y.toFloat(), w.toFloat(), h.toFloat())
        currentCanvas().save()
        currentCanvas().clipRRect(RRect.makeXYWH(x.toFloat(), y.toFloat(), w.toFloat(), h.toFloat(), radius.toFloat()))
        currentCanvas().drawImageRect(getImage(image), rect)
        currentCanvas().restore()
    }

    fun image(path: String, x: Number, y: Number, w: Number, h: Number, radius: Number) {
        val existing = imageCache.keys.find { it.location == path }
        image(existing ?: createImage(path), x, y, w, h, radius)
    }

    fun image(image: SkijaImage, x: Number, y: Number, w: Number, h: Number) = dispatch {
        currentCanvas().drawImageRect(getImage(image), Rect.makeXYWH(x.toFloat(), y.toFloat(), w.toFloat(), h.toFloat()))
    }

    fun wrap(text: String, maxWidth: Float, size: Float, font: SkijaFont = defaultFont): List<String> {
        val words = text.split(' ')
        val lines = mutableListOf<String>()
        var line = StringBuilder()

        fun flush() {
            if (line.isNotEmpty()) {
                lines.add(line.toString())
                line = StringBuilder()
            }
        }

        for (word in words) {
            val candidate = if (line.isEmpty()) word else "$line $word"
            if (textWidth(candidate, size, font) <= maxWidth || line.isEmpty()) {
                if (line.isNotEmpty()) line.append(' ')
                line.append(word)
            }
            else {
                flush()
                line.append(word)
            }
        }

        flush()
        return lines
    }

    internal fun hasBatch(): Boolean = batch.isNotEmpty()
    internal fun flush() {
        for (op in batch) op()
        batch.clear()
    }

    internal fun discard() {
        batch.clear()
    }

    private fun dispatch(op: () -> Unit) {
        batch.add(op)
    }

    private fun currentCanvas(): Canvas {
        return canvas ?: throw IllegalStateException("Skija frame has not started")
    }

    private fun skijaFont(font: SkijaFont, size: Float): Font {
        return fonts.getOrPut(FontKey(font, size)) {
            Font(typefaces.getOrPut(font) {
                Typeface.makeFromData(Data.makeFromBytes(font.bytes))
            }, size).setEdging(FontEdging.SUBPIXEL_ANTI_ALIAS)
        }
    }

    private fun getImage(image: SkijaImage): Image {
        return imageCache[image]?.image ?: throw IllegalStateException("Image (${image.location}) doesn't exist")
    }

    private fun loadImage(image: SkijaImage): Image {
        if (! image.isSvg) return Image.makeFromEncoded(image.bytes)

        val data = Data.makeFromBytes(image.bytes)
        val dom = SVGDOM(data)
        val root = dom.root ?: throw IllegalStateException("Failed to read SVG root: ${image.location}")
        val intrinsic = root.getIntrinsicSize(SVGLengthContext(256f, 256f, 96f))
        val width = max(1, intrinsic.x.toInt())
        val height = max(1, intrinsic.y.toInt())
        val surface = Surface.makeRaster(ImageInfo(width, height, ColorType.N32, ColorAlphaType.PREMUL, ColorSpace.getSRGB()))

        surface.canvas.clear(0)
        dom.setContainerSize(width.toFloat(), height.toFloat())
        dom.render(surface.canvas)
        val snapshot = surface.makeImageSnapshot()
        surface.close()
        dom.close()
        data.close()
        return snapshot
    }

    private fun paint(color: Color, mode: PaintMode = PaintMode.FILL): Paint {
        return Paint().setAntiAlias(true).setMode(mode).setColor(color.toSkijaColor()).setAlphaf((color.alpha / 255f) * globalAlpha)
    }

    private fun paint(argb: Int, mode: PaintMode = PaintMode.FILL): Paint {
        val alpha = ((argb ushr 24) and 0xff) / 255f
        return Paint().setAntiAlias(true).setMode(mode).setColor(argb).setAlphaf(alpha * globalAlpha)
    }

    private fun linearGradient(x: Float, y: Float, w: Float, h: Float, color1: Color, color2: Color, direction: SkijaGradient): Shader {
        val colors = intArrayOf(color1.toSkijaColor(), color2.toSkijaColor())
        return when (direction) {
            SkijaGradient.LEFT_RIGHT -> Shader.makeLinearGradient(x, y, x + w, y, colors)
            SkijaGradient.TOP_BOTTOM -> Shader.makeLinearGradient(x, y, x, y + h, colors)
        }
    }

    private fun linearGradient(x: Float, y: Float, w: Float, h: Float, argb1: Int, argb2: Int, direction: SkijaGradient): Shader {
        val colors = intArrayOf(argb1, argb2)
        return when (direction) {
            SkijaGradient.LEFT_RIGHT -> Shader.makeLinearGradient(x, y, x + w, y, colors)
            SkijaGradient.TOP_BOTTOM -> Shader.makeLinearGradient(x, y, x, y + h, colors)
        }
    }

    private fun Color.toSkijaColor(): Int {
        return ((alpha and 0xff) shl 24) or
            ((red and 0xff) shl 16) or
            ((green and 0xff) shl 8) or
            (blue and 0xff)
    }

    private fun Matrix3x2fc.toSkijaMatrix(): Matrix33 {
        return Matrix33(
            m00(), m10(), m20(),
            m01(), m11(), m21(),
            0f, 0f, 1f
        )
    }

    private data class CachedImage(var count: Int, val image: Image)
    private data class FontKey(val font: SkijaFont, val size: Float)
}