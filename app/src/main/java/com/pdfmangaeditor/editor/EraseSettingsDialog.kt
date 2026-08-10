package com.pdfmangaeditor.editor

import android.app.AlertDialog
import android.content.Context
import android.graphics.Color
import android.widget.LinearLayout
import android.widget.SeekBar
import android.widget.TextView

object EraseSettingsDialog {

    private val palette = listOf(
        Color.WHITE, Color.BLACK, Color.RED, Color.BLUE, Color.YELLOW, Color.GREEN
    )

    fun show(context: Context, currentColor: Int, currentWidth: Float, onConfirm: (Int, Float) -> Unit) {
        var selectedColor = currentColor

        val root = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(48, 24, 48, 24)
        }

        val colorRow = LinearLayout(context).apply { orientation = LinearLayout.HORIZONTAL }
        palette.forEach { c ->
            val swatch = android.widget.View(context).apply {
                layoutParams = LinearLayout.LayoutParams(70, 70).apply { setMargins(8, 8, 8, 8) }
                setBackgroundColor(c)
                setOnClickListener { selectedColor = c }
            }
            colorRow.addView(swatch)
        }
        root.addView(TextView(context).apply { text = "لون الفرشاة (أبيض = مسح عادي):" })
        root.addView(colorRow)

        val sizeLabel = TextView(context).apply { text = "حجم الفرشاة: ${currentWidth.toInt()}" }
        root.addView(sizeLabel)

        val seek = SeekBar(context).apply {
            max = 150
            progress = currentWidth.toInt()
            setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
                override fun onProgressChanged(sb: SeekBar?, progress: Int, fromUser: Boolean) {
                    sizeLabel.text = "حجم الفرشاة: $progress"
                }
                override fun onStartTrackingTouch(sb: SeekBar?) {}
                override fun onStopTrackingTouch(sb: SeekBar?) {}
            })
        }
        root.addView(seek)

        AlertDialog.Builder(context)
            .setTitle("إعدادات المسح")
            .setView(root)
            .setPositiveButton("تطبيق") { _, _ ->
                onConfirm(selectedColor, seek.progress.toFloat().coerceAtLeast(5f))
            }
            .setNegativeButton("إلغاء", null)
            .show()
    }
}
