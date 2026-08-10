package com.pdfmangaeditor.editor

import android.app.AlertDialog
import android.content.Context
import android.graphics.Color
import android.widget.LinearLayout
import android.widget.RadioButton
import android.widget.RadioGroup
import android.widget.SeekBar
import android.widget.TextView
import com.pdfmangaeditor.models.ShapeType

object ShapeSettingsDialog {

    private val palette = listOf(
        Color.BLACK, Color.WHITE, Color.RED, Color.BLUE, Color.YELLOW, Color.GREEN
    )

    fun show(
        context: Context,
        currentType: ShapeType,
        currentStroke: Int,
        currentWidth: Float,
        currentFill: Int?,
        onConfirm: (ShapeType, Int, Float, Int?) -> Unit
    ) {
        var selectedStroke = currentStroke
        var selectedFill = currentFill

        val root = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(48, 24, 48, 24)
        }

        root.addView(TextView(context).apply { text = "نوع الشكل:" })
        val radioGroup = RadioGroup(context).apply { orientation = RadioGroup.HORIZONTAL }
        val types = listOf(
            ShapeType.RECTANGLE to "مستطيل",
            ShapeType.OVAL to "دائرة/بيضاوي",
            ShapeType.LINE to "خط",
            ShapeType.ARROW to "سهم"
        )
        var selectedType = currentType
        types.forEach { (type, label) ->
            val rb = RadioButton(context).apply {
                text = label
                isChecked = type == currentType
                setOnClickListener { selectedType = type }
            }
            radioGroup.addView(rb)
        }
        root.addView(radioGroup)

        root.addView(TextView(context).apply { text = "لون الإطار:" })
        val strokeRow = LinearLayout(context).apply { orientation = LinearLayout.HORIZONTAL }
        palette.forEach { c ->
            val swatch = android.widget.View(context).apply {
                layoutParams = LinearLayout.LayoutParams(70, 70).apply { setMargins(8, 8, 8, 8) }
                setBackgroundColor(c)
                setOnClickListener { selectedStroke = c }
            }
            strokeRow.addView(swatch)
        }
        root.addView(strokeRow)

        val widthLabel = TextView(context).apply { text = "سمك الإطار: ${currentWidth.toInt()}" }
        root.addView(widthLabel)
        val widthSeek = SeekBar(context).apply {
            max = 40
            progress = currentWidth.toInt()
            setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
                override fun onProgressChanged(sb: SeekBar?, progress: Int, fromUser: Boolean) {
                    widthLabel.text = "سمك الإطار: $progress"
                }
                override fun onStartTrackingTouch(sb: SeekBar?) {}
                override fun onStopTrackingTouch(sb: SeekBar?) {}
            })
        }
        root.addView(widthSeek)

        root.addView(TextView(context).apply { text = "لون التعبئة (اختياري):" })
        val fillRow = LinearLayout(context).apply { orientation = LinearLayout.HORIZONTAL }
        fillRow.addView(android.widget.Button(context).apply {
            text = "بدون"
            setOnClickListener { selectedFill = null }
        })
        palette.forEach { c ->
            val swatch = android.widget.View(context).apply {
                layoutParams = LinearLayout.LayoutParams(70, 70).apply { setMargins(8, 8, 8, 8) }
                setBackgroundColor(c)
                setOnClickListener { selectedFill = c }
            }
            fillRow.addView(swatch)
        }
        root.addView(fillRow)

        AlertDialog.Builder(context)
            .setTitle("إعدادات الشكل")
            .setView(root)
            .setPositiveButton("تطبيق") { _, _ ->
                onConfirm(selectedType, selectedStroke, widthSeek.progress.toFloat().coerceAtLeast(2f), selectedFill)
            }
            .setNegativeButton("إلغاء", null)
            .show()
    }
}
