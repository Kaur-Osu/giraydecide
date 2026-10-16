package com.giraydecide.wear

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.view.View
import android.view.accessibility.AccessibilityNodeInfo

/** Compact visible circle inside a 48 dp touch target. Icons do not depend on a font. */
class RoundIconButton(context: Context, private val icon: String, private val green: Boolean = false) : View(context) {
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    init { isClickable = true; isFocusable = true }
    override fun onInitializeAccessibilityNodeInfo(info: AccessibilityNodeInfo) {
        super.onInitializeAccessibilityNodeInfo(info)
        info.className = "android.widget.Button"
    }
    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val density = resources.displayMetrics.density
        val size = if (green) 40f else 32f
        val cx = width / 2f; val cy = height / 2f
        paint.style = Paint.Style.FILL
        paint.color = if (green) Color.rgb(184,243,107) else Color.rgb(38,47,65)
        canvas.drawCircle(cx, cy, size * density / 2f, paint)
        canvas.save()
        canvas.translate(cx - 12f * density, cy - 12f * density)
        canvas.scale(density, density)
        paint.color = if (green) Color.rgb(9,13,23) else Color.WHITE
        paint.style = Paint.Style.STROKE; paint.strokeWidth = 1.8f
        paint.strokeCap = Paint.Cap.ROUND; paint.strokeJoin = Paint.Join.ROUND
        when (icon) {
            "add" -> { canvas.drawLine(6f,12f,18f,12f,paint); canvas.drawLine(12f,6f,12f,18f,paint) }
            "save" -> {
                canvas.drawRoundRect(RectF(4f,4f,20f,20f),2f,2f,paint)
                canvas.drawRect(8f,4f,16f,10f,paint)
                canvas.drawRect(8f,14f,16f,20f,paint)
            }
            "trash" -> {
                canvas.drawLine(5f,7f,19f,7f,paint)
                canvas.drawLine(9f,4f,15f,4f,paint)
                canvas.drawRoundRect(RectF(7f,7f,17f,20f),1f,1f,paint)
                canvas.drawLine(10f,10f,10f,17f,paint); canvas.drawLine(14f,10f,14f,17f,paint)
            }
            "edit" -> {
                val path = Path().apply { moveTo(5f,19f); lineTo(6f,14f); lineTo(16f,4f); lineTo(20f,8f); lineTo(10f,18f); close() }
                canvas.drawPath(path,paint); canvas.drawLine(14f,6f,18f,10f,paint)
            }
            "home" -> {
                val roof = Path().apply { moveTo(3f,11f); lineTo(12f,4f); lineTo(21f,11f) }
                canvas.drawPath(roof,paint)
                val house = Path().apply { moveTo(6f,9f); lineTo(6f,20f); lineTo(10f,20f); lineTo(10f,14f); lineTo(14f,14f); lineTo(14f,20f); lineTo(18f,20f); lineTo(18f,9f) }
                canvas.drawPath(house,paint)
            }
            "editDocument" -> {
                val sheet = Path().apply { moveTo(13f,3f); lineTo(4f,3f); lineTo(4f,21f); lineTo(17f,21f); lineTo(17f,17f) }
                canvas.drawPath(sheet,paint)
                canvas.drawLine(7f,7f,11f,7f,paint); canvas.drawLine(7f,11f,9f,11f,paint)
                val pencil = Path().apply { moveTo(10f,17f); lineTo(11f,13f); lineTo(19f,5f); lineTo(22f,8f); lineTo(14f,16f); close() }
                canvas.drawPath(pencil,paint)
            }
            "back" -> {
                canvas.drawLine(18f,12f,6f,12f,paint)
                canvas.drawLine(6f,12f,11f,7f,paint); canvas.drawLine(6f,12f,11f,17f,paint)
            }
            "delete" -> {
                paint.color = Color.rgb(255,154,165)
                canvas.drawLine(7f,7f,17f,17f,paint); canvas.drawLine(17f,7f,7f,17f,paint)
            }
        }
        canvas.restore()
    }
}