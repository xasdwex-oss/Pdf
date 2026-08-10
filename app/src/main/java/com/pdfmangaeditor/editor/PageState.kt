package com.pdfmangaeditor.editor

import android.graphics.PointF
import com.pdfmangaeditor.models.ErasePath
import com.pdfmangaeditor.models.MangaPage
import com.pdfmangaeditor.models.ShapeElement
import com.pdfmangaeditor.models.TextElement

data class PageState(
    val texts: List<TextElement>,
    val erasePaths: List<ErasePath>,
    val shapes: List<ShapeElement>
)

object PageStateUtil {

    fun capture(page: MangaPage): PageState = PageState(
        texts = page.textElements.map { it.copy() },
        erasePaths = page.erasePaths.map { it.copy(points = it.points.map { p -> PointF(p.x, p.y) }.toMutableList()) },
        shapes = page.shapes.map { it.copy() }
    )

    fun restore(page: MangaPage, state: PageState) {
        page.textElements = state.texts.map { it.copy() }.toMutableList()
        page.erasePaths = state.erasePaths.map { it.copy(points = it.points.map { p -> PointF(p.x, p.y) }.toMutableList()) }.toMutableList()
        page.shapes = state.shapes.map { it.copy() }.toMutableList()
    }
}
