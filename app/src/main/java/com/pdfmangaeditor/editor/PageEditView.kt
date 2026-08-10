package com.pdfmangaeditor.editor

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.PointF
import android.graphics.RectF
import android.text.Layout
import android.text.StaticLayout
import android.text.TextPaint
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import com.pdfmangaeditor.models.ErasePath
import com.pdfmangaeditor.models.MangaPage
import com.pdfmangaeditor.models.ShapeElement
import com.pdfmangaeditor.models.ShapeType
import com.pdfmangaeditor.models.TextAlign
import com.pdfmangaeditor.models.TextElement
import kotlin.math.min

enum class ToolMode { TEXT, ERASE, SHAPE }

class PageEditView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : View(context, attrs) {

    var page: MangaPage? = null
        set(value) { field = value; invalidate() }

    var toolMode: ToolMode = ToolMode.TEXT
        set(value) { field = value; mergeMode = false; invalidate() }

    var mergeMode: Boolean = false
        set(value) { field = value; selectedIds.clear(); invalidate() }

    var eraseColor: Int = Color.WHITE
    var eraseWidth: Float = 30f

    var shapeType: ShapeType = ShapeType.RECTANGLE
    var shapeStrokeColor: Int = Color.BLACK
    var shapeStrokeWidth: Float = 6f
    var shapeFillColor: Int? = null

    var onTextTapped: ((TextElement) -> Unit)? = null
    var onSelectionChanged: ((List<String>) -> Unit)? = null
    var onDragStart: (() -> Unit)? = null

    private val selectedIds = mutableSetOf<String>()

    private var scaleFactor = 1f
    private var offsetX = 0f
    private var offsetY = 0f

    private var draggingElement: TextElement? = null
    private var lastTouchX = 0f
    private var lastTouchY = 0f
    private var isDragging = false
    private var dragStartRecorded = false

    private var currentErasePath: ErasePath? = null
    private var currentShape: ShapeElement? = null

    private val bgPaint = Paint().apply { color = Color.WHITE }
    private val selectionPaint = Paint().apply {
        style = Paint.Style.STROKE; strokeWidth = 3f; color = Color.parseColor("#2196F3")
    }
    private val checkPaint = Paint().apply { style = Paint.Style.FILL; color = Color.parseColor("#4CAF50") }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val p = page ?: return
        val bmp = p.bitmap

        scaleFactor = min(width.toFloat() / bmp.width, height.toFloat() / bmp.height)
        val drawW = bmp.width * scaleFactor
        val drawH = bmp.height * scaleFactor
        offsetX = (width - drawW) / 2f
        offsetY = (height - drawH) / 2f

        canvas.drawRect(offsetX, offsetY, offsetX + drawW, offsetY + drawH, bgPaint)
        canvas.save()
        canvas.translate(offsetX, offsetY)
        canvas.scale(scaleFactor, scaleFactor)
        canvas.drawBitmap(bmp, 0f, 0f, null)

        for (path in p.erasePaths) drawErasePath(canvas, path)
        currentErasePath?.let { drawErasePath(canvas, it) }

        for (shape in p.shapes) drawShape(canvas, shape)
        currentShape?.let { drawShape(canvas, it) }

        for (el in p.textElements) drawTextElement(canvas, el)

        canvas.restore()
    }

    private fun drawErasePath(canvas: Canvas, ep: ErasePath) {
        val paint = Paint().apply {
            isAntiAlias = true
            style = Paint.Style.STROKE
            strokeCap = Paint.Cap.ROUND
            strokeJoin = Paint.Join.ROUND
            color = ep.color
            strokeWidth = ep.strokeWidth
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
            style = Paint.Style.STROKE
            color = s.strokeColor
            strokeWidth = s.strokeWidth
            isAntiAlias = true
            strokeCap = Paint.Cap.ROUND
        }

        when (s.type) {
            ShapeType.RECTANGLE -> canvas.drawRect(left, top, right, bottom, strokePaint)
            ShapeType.OVAL -> canvas.drawOval(RectF(left, top, right, bottom), strokePaint)
            ShapeType.LINE -> canvas.drawLine(s.startX, s.startY, s.endX, s.endY, strokePaint)
            ShapeType.ARROW -> {
                canvas.drawLine(s.startX, s.startY, s.endX, s.endY, strokePaint)
                drawArrowHead(canvas, s.startX, s.startY, s.endX, s.endY, strokePaint)
            }
        }
    }

    private fun drawArrowHead(canvas: Canvas, sx: Float, sy: Float, ex: Float, ey: Float, paint: Paint) {
        val angle = Math.atan2((ey - sy).toDouble(), (ex - sx).toDouble())
        val arrowLen = 25f + paint.strokeWidth
        val arrowAngle = Math.toRadians(25.0)
        val x1 = ex - arrowLen * Math.cos(angle - arrowAngle)
        val y1 = ey - arrowLen * Math.sin(angle - arrowAngle)
        val x2 = ex - arrowLen * Math.cos(angle + arrowAngle)
        val y2 = ey - arrowLen * Math.sin(angle + arrowAngle)
        canvas.drawLine(ex, ey, x1.toFloat(), y1.toFloat(), paint)
        canvas.drawLine(ex, ey, x2.toFloat(), y2.toFloat(), paint)
    }

    private fun drawTextElement(canvas: Canvas, el: TextElement) {
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

        if (el.id in selectedIds) {
            canvas.drawRect(-4f, -4f, el.width + 4f, layout.height + 4f, selectionPaint)
            canvas.drawCircle(el.width - 10f, 10f, 10f, checkPaint)
        }
        canvas.restore()
    }

    private fun screenToPageX(sx: Float) = (sx - offsetX) / scaleFactor
    private fun screenToPageY(sy: Float) = (sy - offsetY) / scaleFactor

    private fun findElementAt(px: Float, py: Float): TextElement? {
        val p = page ?: return null
        for (el in p.textElements.asReversed()) {
            val approxHeight = el.fontSize * 1.3f * (el.text.count { it == '\n' } + 1)
            val rect = RectF(el.x, el.y, el.x + el.width, el.y + approxHeight)
            if (rect.contains(px, py)) return el
        }
        return null
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        val p = page ?: return false
        val px = screenToPageX(event.x)
        val py = screenToPageY(event.y)

        return when (toolMode) {
            ToolMode.TEXT -> handleTextTouch(event, px, py)
            ToolMode.ERASE -> handleEraseTouch(event, px, py, p)
            ToolMode.SHAPE -> handleShapeTouch(event, px, py, p)
        }
    }

    private fun handleTextTouch(event: MotionEvent, px: Float, py: Float): Boolean {
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                val target = findElementAt(px, py)
                if (target != null) {
                    if (mergeMode) {
                        if (target.id in selectedIds) selectedIds.remove(target.id) else selectedIds.add(target.id)
                        onSelectionChanged?.invoke(selectedIds.toList())
                        invalidate()
                    } else {
                        draggingElement = target
                        lastTouchX = px; lastTouchY = py
                        isDragging = false
                        dragStartRecorded = false
                    }
                    return true
                }
            }
            MotionEvent.ACTION_MOVE -> {
                draggingElement?.let { el ->
                    val dx = px - lastTouchX
                    val dy = py - lastTouchY
                    if (kotlin.math.abs(dx) > 1 || kotlin.math.abs(dy) > 1) {
                        if (!dragStartRecorded) {
                            onDragStart?.invoke()
                            dragStartRecorded = true
                        }
                        el.x += dx; el.y += dy
                        lastTouchX = px; lastTouchY = py
                        isDragging = true
                        invalidate()
                    }
                    return true
                }
            }
            MotionEvent.ACTION_UP -> {
                val el = draggingElement
                draggingElement = null
                if (el != null && !isDragging && !mergeMode) onTextTapped?.invoke(el)
                isDragging = false
                dragStartRecorded = false
            }
        }
        return super.onTouchEvent(event)
    }

    private fun handleEraseTouch(event: MotionEvent, px: Float, py: Float, p: MangaPage): Boolean {
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                onDragStart?.invoke()
                currentErasePath = ErasePath(
                    points = mutableListOf(PointF(px, py)),
                    color = eraseColor,
                    strokeWidth = eraseWidth
                )
                invalidate()
                return true
            }
            MotionEvent.ACTION_MOVE -> {
                currentErasePath?.points?.add(PointF(px, py))
                invalidate()
                return true
            }
            MotionEvent.ACTION_UP -> {
                currentErasePath?.let { p.erasePaths.add(it) }
                currentErasePath = null
                invalidate()
                return true
            }
        }
        return super.onTouchEvent(event)
    }

    private fun handleShapeTouch(event: MotionEvent, px: Float, py: Float, p: MangaPage): Boolean {
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                onDragStart?.invoke()
                currentShape = ShapeElement(
                    type = shapeType,
                    startX = px, startY = py, endX = px, endY = py,
                    strokeColor = shapeStrokeColor,
                    strokeWidth = shapeStrokeWidth,
                    fillColor = shapeFillColor
                )
                invalidate()
                return true
            }
            MotionEvent.ACTION_MOVE -> {
                currentShape?.let { it.endX = px; it.endY = py }
                invalidate()
                return true
            }
            MotionEvent.ACTION_UP -> {
                currentShape?.let { p.shapes.add(it) }
                currentShape = null
                invalidate()
                return true
            }
        }
        return super.onTouchEvent(event)
    }

    fun getSelectedIds(): List<String> = selectedIds.toList()

    fun clearSelection() {
        selectedIds.clear()
        onSelectionChanged?.invoke(emptyList())
        invalidate()
    }
}
