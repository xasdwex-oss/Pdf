package com.pdfmangaeditor.models

import android.graphics.Color
import android.graphics.Path
import android.graphics.PointF
import java.util.UUID

data class ErasePath(
    val id: String = UUID.randomUUID().toString(),
    var points: MutableList<PointF>,
    var color: Int = Color.WHITE,
    var strokeWidth: Float = 30f
) {
    fun toPath(): Path {
        val path = Path()
        if (points.isEmpty()) return path
        path.moveTo(points[0].x, points[0].y)
        for (i in 1 until points.size) {
            path.lineTo(points[i].x, points[i].y)
        }
        return path
    }
}
