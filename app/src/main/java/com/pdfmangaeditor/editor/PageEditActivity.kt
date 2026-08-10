package com.pdfmangaeditor.editor

import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.pdfmangaeditor.databinding.ActivityPageEditBinding
import com.pdfmangaeditor.models.TextElement

class PageEditActivity : AppCompatActivity() {

    companion object {
        const val EXTRA_PAGE_INDEX = "extra_page_index"
    }

    private lateinit var binding: ActivityPageEditBinding
    private val textEditor = TextEditor()
    private lateinit var undoRedo: UndoRedoManager<PageState>
    private var pageIndex = -1

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityPageEditBinding.inflate(layoutInflater)
        setContentView(binding.root)

        pageIndex = intent.getIntExtra(EXTRA_PAGE_INDEX, -1)
        val page = PageRepository.pages.getOrNull(pageIndex)
        if (page == null) {
            Toast.makeText(this, "خطأ: الصفحة غير موجودة", Toast.LENGTH_SHORT).show()
            finish(); return
        }

        undoRedo = UndoRedoManager(deepCopy = { state -> deepCopyState(state) })

        binding.pageEditView.page = page
        binding.pageEditView.onDragStart = { recordUndo(page) }

        setupToolSwitching(page)
        setupTextTool(page)
        setupEraseTool()
        setupShapeTool()
        setupUndoRedo(page)

        showToolBar(ToolMode.TEXT)
    }

    private fun deepCopyState(state: PageState): PageState = PageState(
        texts = state.texts.map { it.copy() },
        erasePaths = state.erasePaths.map { it.copy(points = it.points.map { p -> android.graphics.PointF(p.x, p.y) }.toMutableList()) },
        shapes = state.shapes.map { it.copy() }
    )

    private fun recordUndo(page: com.pdfmangaeditor.models.MangaPage) {
        undoRedo.recordState(PageStateUtil.capture(page))
        page.thumbnailDirty = true
        page.thumbnailDirty = true
    }

    private fun setupToolSwitching(page: com.pdfmangaeditor.models.MangaPage) {
        binding.btnToolText.setOnClickListener {
            binding.pageEditView.toolMode = ToolMode.TEXT
            showToolBar(ToolMode.TEXT)
        }
        binding.btnToolErase.setOnClickListener {
            binding.pageEditView.toolMode = ToolMode.ERASE
            showToolBar(ToolMode.ERASE)
        }
        binding.btnToolShape.setOnClickListener {
            binding.pageEditView.toolMode = ToolMode.SHAPE
            showToolBar(ToolMode.SHAPE)
        }
    }

    private fun showToolBar(mode: ToolMode) {
        binding.btnAddText.visibility = if (mode == ToolMode.TEXT) View.VISIBLE else View.GONE
        binding.btnMergeMode.visibility = if (mode == ToolMode.TEXT) View.VISIBLE else View.GONE
        binding.btnMergeNow.visibility = View.GONE
        binding.btnEraseSettings.visibility = if (mode == ToolMode.ERASE) View.VISIBLE else View.GONE
        binding.btnShapeSettings.visibility = if (mode == ToolMode.SHAPE) View.VISIBLE else View.GONE
    }

    private fun setupTextTool(page: com.pdfmangaeditor.models.MangaPage) {
        binding.btnAddText.setOnClickListener {
            TextStyleDialog.show(this, existing = null) { text, size, color, highlight, bold, italic, font ->
                if (text.isNotBlank()) {
                    recordUndo(page)
                    textEditor.addText(page.textElements, TextElement(
                        text = text, x = page.bitmap.width / 4f, y = page.bitmap.height / 4f,
                        width = page.bitmap.width / 2f, fontSize = size, color = color,
                        highlightColor = highlight, isBold = bold, isItalic = italic, fontFamily = font
                    ))
                    binding.pageEditView.invalidate()
                }
            }
        }

        binding.pageEditView.onTextTapped = { el ->
            TextStyleDialog.show(this, existing = el,
                onConfirm = { text, size, color, highlight, bold, italic, font ->
                    recordUndo(page)
                    textEditor.updateText(page.textElements, el.id) { t ->
                        t.text = text; t.fontSize = size; t.color = color
                        t.highlightColor = highlight; t.isBold = bold; t.isItalic = italic; t.fontFamily = font
                    }
                    binding.pageEditView.invalidate()
                },
                onDelete = {
                    recordUndo(page)
                    textEditor.deleteText(page.textElements, el.id)
                    binding.pageEditView.invalidate()
                }
            )
        }

        binding.btnMergeMode.setOnClickListener {
            binding.pageEditView.mergeMode = !binding.pageEditView.mergeMode
            binding.btnMergeMode.text = if (binding.pageEditView.mergeMode) "إلغاء الدمج" else "وضع الدمج"
            binding.btnMergeNow.visibility = if (binding.pageEditView.mergeMode) View.VISIBLE else View.GONE
            if (!binding.pageEditView.mergeMode) binding.pageEditView.clearSelection()
        }

        binding.pageEditView.onSelectionChanged = { selected -> binding.btnMergeNow.isEnabled = selected.size >= 2 }

        binding.btnMergeNow.setOnClickListener {
            val selected = binding.pageEditView.getSelectedIds()
            if (selected.size >= 2) {
                recordUndo(page)
                textEditor.mergeTexts(page.textElements, selected)
                binding.pageEditView.mergeMode = false
                binding.btnMergeMode.text = "وضع الدمج"
                binding.btnMergeNow.visibility = View.GONE
                binding.pageEditView.invalidate()
                Toast.makeText(this, "تم دمج النصوص", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun setupEraseTool() {
        binding.btnEraseSettings.setOnClickListener {
            EraseSettingsDialog.show(
                this,
                binding.pageEditView.eraseColor,
                binding.pageEditView.eraseWidth
            ) { color, width ->
                binding.pageEditView.eraseColor = color
                binding.pageEditView.eraseWidth = width
            }
        }
    }

    private fun setupShapeTool() {
        binding.btnShapeSettings.setOnClickListener {
            ShapeSettingsDialog.show(
                this,
                binding.pageEditView.shapeType,
                binding.pageEditView.shapeStrokeColor,
                binding.pageEditView.shapeStrokeWidth,
                binding.pageEditView.shapeFillColor
            ) { type, stroke, width, fill ->
                binding.pageEditView.shapeType = type
                binding.pageEditView.shapeStrokeColor = stroke
                binding.pageEditView.shapeStrokeWidth = width
                binding.pageEditView.shapeFillColor = fill
            }
        }
    }

    private fun setupUndoRedo(page: com.pdfmangaeditor.models.MangaPage) {
        binding.btnUndo.setOnClickListener {
            val restored = undoRedo.undo(PageStateUtil.capture(page))
            if (restored != null) {
                PageStateUtil.restore(page, restored)
                binding.pageEditView.page = page
            } else {
                Toast.makeText(this, "لا يوجد شي نتراجع عنه", Toast.LENGTH_SHORT).show()
            }
        }
        binding.btnRedo.setOnClickListener {
            val restored = undoRedo.redo(PageStateUtil.capture(page))
            if (restored != null) {
                PageStateUtil.restore(page, restored)
                binding.pageEditView.page = page
            } else {
                Toast.makeText(this, "لا يوجد شي نعيده", Toast.LENGTH_SHORT).show()
            }
        }
    }
}
