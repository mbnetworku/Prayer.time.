package com.prayertime.tv

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.ColorFilter
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.PixelFormat
import android.graphics.RadialGradient
import android.graphics.RectF
import android.graphics.Shader
import android.graphics.drawable.Drawable

/**
 * "Liquid glass" panel: a mostly clear body, a soft light sheen across the top,
 * and a bright rim that catches light at the top-left and bottom-right corners.
 */
class GlassDrawable(private val density: Float, private val radiusDp: Int, private val tint: Int? = null,
                    private val strong: Boolean = false,
                    private val dark: Boolean = false) : Drawable() {
    private val fill = Paint(Paint.ANTI_ALIAS_FLAG)
    private val sheen = Paint(Paint.ANTI_ALIAS_FLAG)
    private val glow = Paint(Paint.ANTI_ALIAS_FLAG)
    private val rim = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE }
    private val rect = RectF()
    private val path = Path()

    private fun white(a: Int) = Color.argb(a, 255, 255, 255)
    private fun tinted(a: Int) = tint?.let { Color.argb(a, Color.red(it), Color.green(it), Color.blue(it)) } ?: white(a / 4)

    override fun onBoundsChange(b: android.graphics.Rect) {
        super.onBoundsChange(b)
        val w = b.width().toFloat(); val h = b.height().toFloat()
        if (w <= 0 || h <= 0) return
        val r = (radiusDp * density).coerceAtMost(minOf(w, h) / 2)
        val sw = 1.4f * density
        rect.set(b.left + sw / 2, b.top + sw / 2, b.right - sw / 2, b.bottom - sw / 2)
        path.reset(); path.addRoundRect(rect, r, r, Path.Direction.CW)
        val x = b.left.toFloat(); val y = b.top.toFloat()
        // Body: very light, slightly brighter at the top.
        fill.shader = if (tint != null)
            LinearGradient(x, y, x, y + h, tinted(150), tinted(90), Shader.TileMode.CLAMP)
        else if (dark) LinearGradient(x, y, x, y + h, Color.argb(150, 8, 10, 16), Color.argb(115, 8, 10, 16), Shader.TileMode.CLAMP)
        else if (strong) LinearGradient(x, y, x, y + h, white(72), white(30), Shader.TileMode.CLAMP)
        else LinearGradient(x, y, x, y + h, white(30), white(8), Shader.TileMode.CLAMP)
        // Top sheen: soft reflection fading out by the middle.
        sheen.shader = LinearGradient(x, y, x, y + minOf(h * 0.55f, 160 * density),
            white(48), white(0), Shader.TileMode.CLAMP)
        // Soft inner glow from the top-left corner.
        glow.shader = RadialGradient(x + w * 0.15f, y, maxOf(w, h) * 0.6f, white(26), white(0), Shader.TileMode.CLAMP)
        // Rim: bright at top-left and bottom-right, faint in between.
        rim.strokeWidth = sw
        rim.shader = LinearGradient(x, y, x + w, y + h,
            intArrayOf(white(210), white(40), white(18), white(60), white(150)),
            floatArrayOf(0f, 0.3f, 0.55f, 0.8f, 1f), Shader.TileMode.CLAMP)
    }

    override fun draw(c: Canvas) {
        if (path.isEmpty) return
        c.drawPath(path, fill)
        c.save(); c.clipPath(path)
        c.drawPaint(glow)
        c.drawRect(rect, sheen)
        c.restore()
        c.drawPath(path, rim)
    }

    override fun setAlpha(alpha: Int) { fill.alpha = alpha; rim.alpha = alpha; invalidateSelf() }
    override fun setColorFilter(cf: ColorFilter?) { fill.colorFilter = cf; rim.colorFilter = cf; invalidateSelf() }
    @Deprecated("Deprecated in Java")
    override fun getOpacity() = PixelFormat.TRANSLUCENT
}
