package com.pdfmangaeditor

import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.pdfmangaeditor.databinding.ActivityMainBinding
import com.pdfmangaeditor.editor.EditorActivity

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    private val pickPdfLauncher = registerForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri != null) {
            handleSelectedPdf(uri)
        } else {
            Toast.makeText(this, "لم يتم اختيار أي ملف", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.btnSelectPdf.setOnClickListener {
            pickPdfLauncher.launch(arrayOf("application/pdf"))
        }
    }

    private fun handleSelectedPdf(uri: Uri) {
        try {
            contentResolver.takePersistableUriPermission(
                uri,
                android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION
            )
        } catch (e: SecurityException) {
            // تجنيب الأخطاء في حال عدم دعم المزود للصلاحية الدائمة
        }

        binding.tvStatus.text = "تم اختيار: ${uri.lastPathSegment}"

        val intent = android.content.Intent(this, EditorActivity::class.java).apply {
            putExtra(EditorActivity.EXTRA_PDF_URI, uri.toString())
        }
        startActivity(intent)
    }
}
