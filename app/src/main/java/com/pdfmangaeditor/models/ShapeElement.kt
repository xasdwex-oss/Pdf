package com.pdfmangaeditor.models

import android.graphics.Color
import java.util.UUID

enum class ShapeType { RECTANGLE, OVAL, LINE, ARROW }

data class ShapeElement(
    val id: String = UUID.randomUUID().toString(),
    var type: ShapeType,
    var startX: Float,
    var startY: Float,
    var endX: Float,
    var endY: Float,
    var strokeColor: Int = Color.BLACK,
    var strokeWidth: Float = 6f,
    var fillColor: Int? = null
)
