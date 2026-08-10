package com.pdfmangaeditor.utils

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.text.Layout
import android.text.StaticLayout
import android.text.TextPaint
import com.pdfmangaeditor.models.ErasePath
import com.pdfmangaeditor.models.MangaPage
import com.pdfmangaeditor.models.ShapeElement
import com.pdfmangaeditor.models.ShapeType
import com.pdfmangaeditor.models.TextAlign
import kotlin.math.min

object PageRenderer {

    fun flatten(page: MangaPage): Bitmap {
        val result = page.bitmap.copy(Bitmap.Config.ARGB_8888, true)
        val canvas = Canvas(result)

        for (ep in page.erasePaths) drawErasePath(canvas, ep)
        for (s in page.shapes) drawShape(canvas, s)

        for (el in page.textElements) {
            canvas.save()
            canvas.translate(el.x, el.y)
            canvas.rotate(el.rotation, el.width / 2, el.fontSize)

            val textPaint = TextPaint().apply {
                isAntiAlias = true; color = el.color; textSize = el.fontSize; typeface = el.getTypeface()
            }
            val alignment = when (el.align) {
                TextAlign.RIGHT -> Layout.Alignment.ALIGN_OPPOSITE
                TextAlign.CENTER -> Layout.Alignment.ALIGN_CENTER
                TextAlign.LEFT -> Layout.Alignment.ALIGN_NORMAL
            }
            val layout = StaticLayout.Builder
                .obtain(el.text, 0, el.text.length, textPaint, el.width.toInt().coerceAtLeast(50))
                .setAlignment(alignment).setLineSpacing(0f, 1.1f).build()

            el.highlightColor?.let { hColor ->
                canvas.drawRect(-4f, -4f, el.width + 4f, layout.height + 4f, Paint().apply { color = hColor })
            }
            layout.draw(canvas)
            canvas.restore()
        }
        return result
    }

    private fun drawErasePath(canvas: Canvas, ep: ErasePath) {
        val paint = Paint().apply {
            isAntiAlias = true; style = Paint.Style.STROKE
            strokeCap = Paint.Cap.ROUND; strokeJoin = Paint.Join.ROUND
            color = ep.color; strokeWidth = ep.strokeWidth
        }
        canvas.drawPath(ep.toPath(), paint)
    }

    private fun drawShape(canvas: Canvas, s: ShapeElement) {
        val left = min(s.startX, s.endX)
        val top = min(s.startY, s.endY)
        val right = maxOf(s.startX, s.endX)
        val bottom = maxOf(s.startY, s.endY)

        s.fillColor?.let { fc ->
            val fillPaint = Paint().apply { style = Paint.Style.FILL; color = fc; isAntiAlias = true }
            when (s.type) {
                ShapeType.RECTANGLE -> canvas.drawRect(left, top, right, bottom, fillPaint)
                ShapeType.OVAL -> canvas.drawOval(RectF(left, top, right, bottom), fillPaint)
                else -> {}
            }
        }
        val strokePaint = Paint().apply {
            style = Paint.Style.STROKE; color = s.strokeColor; strokeWidth = s.strokeWidth
            isAntiAlias = true; strokeCap = Paint.Cap.ROUND
        }
        when (s.type) {
            ShapeType.RECTANGLE -> canvas.drawRect(left, top, right, bottom, strokePaint)
            ShapeType.OVAL -> canvas.drawOval(RectF(left, top, right, bottom), strokePaint)
            ShapeType.LINE -> canvas.drawLine(s.startX, s.startY, s.endX, s.endY, strokePaint)
            ShapeType.ARROW -> canvas.drawLine(s.startX, s.startY, s.endX, s.endY, strokePaint)
        }
    }

    fun flattenThumbnail(page: MangaPage, maxWidth: Int = 400): Bitmap {
        val full = flatten(page)
        if (full.width <= maxWidth) return full

        val scale = maxWidth.toFloat() / full.width
        val targetH = (full.height * scale).toInt().coerceAtLeast(1)
        val scaled = Bitmap.createScaledBitmap(full, maxWidth, targetH, true)

        if (scaled !== full) full.recycle()
        return scaled
    }
}
