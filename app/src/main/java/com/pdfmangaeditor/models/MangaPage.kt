package com.pdfmangaeditor.models

import android.graphics.Bitmap
import java.util.UUID

enum class PageSource { PDF_ORIGINAL, BLANK, IMPORTED_IMAGE }

data class MangaPage(
    val id: String = UUID.randomUUID().toString(),
    var bitmap: Bitmap,
    var rotation: Int = 0,
    var sourceType: PageSource = PageSource.PDF_ORIGINAL,
    var textElements: MutableList<TextElement> = mutableListOf(),
    var erasePaths: MutableList<ErasePath> = mutableListOf(),
    var shapes: MutableList<ShapeElement> = mutableListOf(),

    @Transient var cachedThumbnail: Bitmap? = null,
    @Transient var thumbnailDirty: Boolean = true
)
