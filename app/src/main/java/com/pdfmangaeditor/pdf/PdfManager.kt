package com.pdfmangaeditor.pdf

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.graphics.pdf.PdfDocument
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import android.os.ParcelFileDescriptor
import com.pdfmangaeditor.models.MangaPage
import com.pdfmangaeditor.models.PageSource
import com.pdfmangaeditor.utils.PageRenderer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.IOException

class PdfManager(private val context: Context) {

    suspend fun loadPdfPages(uri: Uri): MutableList<MangaPage> = withContext(Dispatchers.IO) {
        val pfd: ParcelFileDescriptor = context.contentResolver.openFileDescriptor(uri, "r")
            ?: throw IOException("تعذر فتح الملف")

        val renderer = PdfRenderer(pfd)
        val pages = mutableListOf<MangaPage>()

        for (i in 0 until renderer.pageCount) {
            val page = renderer.openPage(i)
            val bitmap = Bitmap.createBitmap(page.width * 2, page.height * 2, Bitmap.Config.ARGB_8888)
            bitmap.eraseColor(Color.WHITE)
            page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
            pages.add(MangaPage(bitmap = bitmap, sourceType = PageSource.PDF_ORIGINAL))
            page.close()
        }

        renderer.close()
        pfd.close()
        pages
    }

    fun createBlankPage(width: Int = 1240, height: Int = 1754): MangaPage {
        val bmp = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        bmp.eraseColor(Color.WHITE)
        return MangaPage(bitmap = bmp, sourceType = PageSource.BLANK)
    }

    suspend fun loadImagePage(uri: Uri): MangaPage = withContext(Dispatchers.IO) {
        val input = context.contentResolver.openInputStream(uri)
        val bmp = BitmapFactory.decodeStream(input) ?: throw IOException("تعذر قراءة الصورة")
        input?.close()
        MangaPage(bitmap = bmp, sourceType = PageSource.IMPORTED_IMAGE)
    }

    suspend fun savePagesToPdf(pages: List<MangaPage>, outputUri: Uri) = withContext(Dispatchers.IO) {
        val document = PdfDocument()

        pages.forEachIndexed { index, mangaPage ->
            val flattenedBitmap = PageRenderer.flatten(mangaPage)
            val pageInfo = PdfDocument.PageInfo.Builder(flattenedBitmap.width, flattenedBitmap.height, index + 1).create()
            val page = document.startPage(pageInfo)
            page.canvas.drawBitmap(flattenedBitmap, 0f, 0f, null)
            document.finishPage(page)

            if (flattenedBitmap !== mangaPage.bitmap) flattenedBitmap.recycle()
        }

        context.contentResolver.openOutputStream(outputUri)?.use { out ->
            document.writeTo(out)
        } ?: throw IOException("تعذر الكتابة على الملف")

        document.close()
    }
}
