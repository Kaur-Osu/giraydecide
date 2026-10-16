package com.giraydecide.wear

import android.content.Context
import android.animation.ValueAnimator
import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.graphics.*
import android.text.StaticLayout
import android.text.Layout
import android.view.View
import kotlin.math.tan
import kotlin.math.sqrt

class WheelView(context: Context, private val labels: List<String>) : View(context) {
    var angle = 0f
        set(value) { field = value; invalidate() }
    private val preferences = AppPreferences(context)
    private var celebrationAnimator: ValueAnimator? = null
    private var celebrationProgress = -1f
    fun celebrate() {
        if (!preferences.celebrationEnabled || !isAttachedToWindow) return
        celebrationAnimator?.cancel()
        celebrationAnimator = ValueAnimator.ofFloat(0f, 1f).apply {
            duration = 1100
            addUpdateListener { celebrationProgress = it.animatedValue as Float; invalidate() }
            addListener(object : AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: Animator) { celebrationProgress = -1f; invalidate() }
            })
            start()
        }
    }
    override fun onDetachedFromWindow() {
        celebrationAnimator?.cancel(); celebrationAnimator = null
        super.onDetachedFromWindow()
    }
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val textPaint = android.text.TextPaint(Paint.ANTI_ALIAS_FLAG)
    private val colors = intArrayOf(Color.rgb(184,243,107), Color.rgb(184,161,255), Color.rgb(105,209,245), Color.rgb(255,188,114), Color.rgb(251,145,177), Color.rgb(90,219,185))
    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val density = resources.displayMetrics.density
        val cx = width / 2f
        val cy = height / 2f + 3f
        val radius = minOf(width, height) / 2f - 5f * density
        val rect = RectF(cx - radius, cy - radius, cx + radius, cy + radius)
        val sweep = 360f / labels.size.coerceAtLeast(1)
        canvas.save()
        canvas.rotate(angle, cx, cy)
        labels.forEachIndexed { i, label ->
            val start = -90f + i * sweep
            paint.color = if (preferences.multicolor) colors[i % colors.size] else preferences.singleColor
            paint.style = Paint.Style.FILL
            canvas.drawArc(rect, start, sweep - 1.5f, true, paint)
            val sector = Path().apply { moveTo(cx, cy); arcTo(rect, start, sweep - 1.5f); close() }
            canvas.save()
            canvas.clipPath(sector)
            val middle = start + sweep / 2f
            canvas.rotate(middle, cx, cy)
            canvas.translate(cx + radius * .66f, cy)
            // Keep words upright on either side of the wheel.
            val orientation = ((middle + angle) % 360f + 360f) % 360f
            if (orientation > 90f && orientation < 270f) canvas.rotate(180f)
            textPaint.color = Color.rgb(9,13,23)
            textPaint.textAlign = Paint.Align.LEFT
            textPaint.typeface = Typeface.create("sans-serif", Typeface.BOLD)
            // Inscribe a rectangle wholly inside this wedge and the outer circle.
            val inner = radius * .37f
            val outer = radius * .95f
            val halfAngle = Math.toRadians(((sweep - 1.5f) / 2f).coerceAtMost(89f).toDouble())
            val halfHeight = minOf((inner * tan(halfAngle)).toFloat(), sqrt(radius * radius - outer * outer)) * .9f
            val availableWidth = ((outer - inner) * .96f).toInt().coerceAtLeast(1)
            val availableHeight = (halfHeight * 2f).coerceAtLeast(1f)
            fun layoutAt(size: Float): StaticLayout {
                textPaint.textSize = size
                return StaticLayout.Builder.obtain(label, 0, label.length, textPaint, availableWidth)
                    .setAlignment(Layout.Alignment.ALIGN_CENTER).setIncludePad(false)
                    .setBreakStrategy(Layout.BREAK_STRATEGY_SIMPLE)
                    .setHyphenationFrequency(Layout.HYPHENATION_FREQUENCY_NONE).build()
            }
            // Wrap and shrink the full label; never discard characters or add ellipses.
            var size = 12f * density
            var layout = layoutAt(size)
            while (layout.height > availableHeight && size > .5f) {
                size *= .9f
                layout = layoutAt(size)
            }
            canvas.translate(-availableWidth / 2f, -layout.height / 2f)
            layout.draw(canvas)
            canvas.restore()
        }
        canvas.restore()
        paint.color = Color.rgb(9,13,23)
        canvas.drawCircle(cx, cy, radius * .32f, paint)
        paint.color = Color.WHITE
        paint.textSize = radius * .18f
        paint.textAlign = Paint.Align.CENTER
        paint.typeface = Typeface.create("sans-serif", Typeface.BOLD)
        canvas.drawText("GIRA", cx, cy + paint.textSize / 3, paint)
        val pointer = Path().apply { moveTo(cx - 9, cy - radius - 7); lineTo(cx + 9, cy - radius - 7); lineTo(cx, cy - radius + 12); close() }
        canvas.drawPath(pointer, paint)
        val progress = celebrationProgress
        if (progress >= 0f) {
            paint.style = Paint.Style.FILL
            paint.alpha = ((1f - progress) * 255).toInt().coerceIn(0, 255)
            for (i in 0 until 30) {
                val direction = Math.toRadians(i * 137.5)
                val travel = radius * progress * (.7f + (i % 5) * .15f)
                val x = cx + kotlin.math.cos(direction).toFloat() * travel
                val y = cy + kotlin.math.sin(direction).toFloat() * travel + progress * progress * radius * .35f
                paint.color = colors[i % colors.size]
                paint.alpha = ((1f - progress) * 255).toInt().coerceIn(0, 255)
                canvas.save()
                canvas.translate(x, y); canvas.rotate(i * 23f + progress * 220f)
                canvas.drawRect(-2f * density, -1f * density, 2f * density, 1f * density, paint)
                canvas.restore()
            }
            paint.alpha = 255
        }
    }
}