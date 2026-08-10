package com.pdfmangaeditor.editor

import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.ItemTouchHelper
import com.pdfmangaeditor.databinding.ActivityEditorBinding
import com.pdfmangaeditor.pdf.PdfManager
import com.pdfmangaeditor.ui.EditorViewModel
import kotlinx.coroutines.launch

class EditorActivity : AppCompatActivity() {

    companion object {
        const val EXTRA_PDF_URI = "extra_pdf_uri"
    }

    private lateinit var binding: ActivityEditorBinding
    private val viewModel: EditorViewModel by viewModels()
    private lateinit var pdfManager: PdfManager
    private lateinit var adapter: PageAdapter
    private lateinit var itemTouchHelper: ItemTouchHelper

    private val pickImageLauncher = registerForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        uri?.let { loadImagePage(it) }
    }

    private val savePdfLauncher = registerForActivityResult(
        ActivityResultContracts.CreateDocument("application/pdf")
    ) { uri: Uri? ->
        uri?.let { savePdf(it) }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityEditorBinding.inflate(layoutInflater)
        setContentView(binding.root)

        pdfManager = PdfManager(applicationContext)

        setupRecyclerView()
        setupListeners()
        observeViewModel()

        val pdfUriString = intent.getStringExtra(EXTRA_PDF_URI)
        if (pdfUriString != null) {
            loadPdf(Uri.parse(pdfUriString))
        }
    }

    private fun setupRecyclerView() {
        adapter = PageAdapter(
            onDeleteClick = { position ->
                adapter.removeItem(position)
                viewModel.deletePage(position)
                renumberVisible()
            },
            onPageClick = { position ->
                Toast.makeText(this, "فتح الصفحة ${position + 1}", Toast.LENGTH_SHORT).show()
            },
            onStartDrag = { holder ->
                itemTouchHelper.startDrag(holder)
            }
        )

        binding.rvPages.layoutManager = GridLayoutManager(this, 3)
        binding.rvPages.adapter = adapter

        val callback = PageDragCallback(adapter) { from, to ->
            viewModel.movePage(from, to)
        }
        itemTouchHelper = ItemTouchHelper(callback)
        itemTouchHelper.attachToRecyclerView(binding.rvPages)
    }

    private fun setupListeners() {
        binding.btnAddBlank.setOnClickListener {
            val blank = pdfManager.createBlankPage()
            viewModel.addPage(blank)
        }

        binding.btnAddImage.setOnClickListener {
            pickImageLauncher.launch(arrayOf("image/*"))
        }

        binding.btnSave.setOnClickListener {
            savePdfLauncher.launch("منجا_معدلة.pdf")
        }
    }

    private fun observeViewModel() {
        viewModel.pages.observe(this) { pages ->
            adapter.submitList(pages)
        }
        viewModel.isLoading.observe(this) { loading ->
            binding.progressBar.visibility = if (loading) android.view.View.VISIBLE else android.view.View.GONE
        }
    }

    private fun loadPdf(uri: Uri) {
        viewModel.setLoading(true)
        lifecycleScope.launch {
            try {
                val pages = pdfManager.loadPdfPages(uri)
                viewModel.setPages(pages)
            } catch (e: Exception) {
                Toast.makeText(this@EditorActivity, "خطأ بفتح الملف: ${e.message}", Toast.LENGTH_LONG).show()
            } finally {
                viewModel.setLoading(false)
            }
        }
    }

    private fun loadImagePage(uri: Uri) {
        viewModel.setLoading(true)
        lifecycleScope.launch {
            try {
                val page = pdfManager.loadImagePage(uri)
                viewModel.addPage(page)
            } catch (e: Exception) {
                Toast.makeText(this@EditorActivity, "خطأ بإضافة الصورة: ${e.message}", Toast.LENGTH_LONG).show()
            } finally {
                viewModel.setLoading(false)
            }
        }
    }

    private fun savePdf(outputUri: Uri) {
        viewModel.setLoading(true)
        lifecycleScope.launch {
            try {
                pdfManager.savePagesToPdf(viewModel.currentPages(), outputUri)
                Toast.makeText(this@EditorActivity, "تم الحفظ بنجاح", Toast.LENGTH_SHORT).show()
            } catch (e: Exception) {
                Toast.makeText(this@EditorActivity, "خطأ بالحفظ: ${e.message}", Toast.LENGTH_LONG).show()
            } finally {
                viewModel.setLoading(false)
            }
        }
    }

    private fun renumberVisible() {
        binding.rvPages.adapter?.notifyDataSetChanged()
    }

    override fun onResume() {
        super.onResume()
        binding.rvPages.adapter?.notifyDataSetChanged()
    }

    override fun onResume() {
        super.onResume()
        binding.rvPages.adapter?.notifyDataSetChanged()
    }
}
