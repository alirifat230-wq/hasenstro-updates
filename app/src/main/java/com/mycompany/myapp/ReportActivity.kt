package com.md.hasenstro.app

import android.content.ContentValues
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.view.Gravity
import android.view.View
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class ReportActivity : AppCompatActivity() {

    private data class Record(
        val date: String,
        val note: String,
        val amount: Double,
        val received: Boolean,
        val timestamp: Long
    )

    private var customerName = ""
    private var records = listOf<Record>()
    private var due = 0.0

    private fun money(value: Double): String =
        "৳ " + String.format(Locale.US, "%,.2f", value)

    private fun textView(
        value: String,
        size: Float,
        color: Int,
        bold: Boolean = false
    ): TextView {
        return TextView(this).apply {
            text = value
            textSize = size
            setTextColor(color)
            if (bold) setTypeface(typeface, Typeface.BOLD)
            setPadding(12, 10, 12, 10)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        customerName = intent.getStringExtra("customer_name") ?: ""
        loadRecords()

        window.statusBarColor = android.graphics.Color.rgb(0, 99, 65)
        window.navigationBarColor = android.graphics.Color.WHITE
        @Suppress("DEPRECATION")
        run {
            window.decorView.systemUiVisibility =
                View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR
        }

        val green = android.graphics.Color.rgb(0, 99, 65)
        val dark = android.graphics.Color.rgb(24, 58, 41)
        val bg = android.graphics.Color.rgb(244, 247, 242)

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(bg)
            setPadding(16, 12, 16, 16)
        }

        val header = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }

        val back = textView("←", 30f, green, true).apply {
            gravity = Gravity.CENTER
            setOnClickListener { finish() }
        }
        header.addView(back, LinearLayout.LayoutParams(48, 56))
        header.addView(
            textView("রিপোর্ট", 21f, dark, true),
            LinearLayout.LayoutParams(0, -2, 1f)
        )
        // রিপোর্ট পেজে মেনু রাখা হয়নি।
        root.addView(header)

        root.addView(
            textView(customerName, 22f, dark, true)
        )
        root.addView(textView("টাকা পাবো", 15f, dark))
        root.addView(
            textView(money(due), 30f, green, true)
        )

        val download = Button(this).apply {
            text = "PDF ডাউনলোড"
            setTextColor(android.graphics.Color.WHITE)
            backgroundTintList =
                android.content.res.ColorStateList.valueOf(green)
            setOnClickListener { downloadPdf() }
        }
        root.addView(
            download,
            LinearLayout.LayoutParams(-1, 52).apply {
                topMargin = 8
                bottomMargin = 16
            }
        )

        val today = SimpleDateFormat("dd MMMM, yyyy", Locale("bn", "BD"))
            .format(Date())
        root.addView(textView(today, 17f, dark, true))

        val headings = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            setBackgroundColor(android.graphics.Color.rgb(225, 244, 231))
        }
        headings.addView(textView("বিবরণ", 14f, dark, true),
            LinearLayout.LayoutParams(0, -2, 1.5f))
        headings.addView(textView("দিলাম", 14f, green, true),
            LinearLayout.LayoutParams(0, -2, 1f))
        headings.addView(textView("পেলাম", 14f, green, true),
            LinearLayout.LayoutParams(0, -2, 1f))
        root.addView(headings)

        val list = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
        }

        if (records.isEmpty()) {
            list.addView(textView("এখনো কোনো লেনদেন নেই", 15f, dark))
        } else {
            records.forEach { record ->
                val row = LinearLayout(this).apply {
                    orientation = LinearLayout.HORIZONTAL
                    gravity = Gravity.CENTER_VERTICAL
                }

                val details = buildString {
                    append(record.date)
                    if (record.note.isNotBlank()) {
                        append("\n")
                        append(record.note)
                    }
                }

                row.addView(
                    textView(details, 13f, dark),
                    LinearLayout.LayoutParams(0, -2, 1.5f)
                )
                row.addView(
                    textView(
                        if (!record.received) money(record.amount) else "—",
                        13f, dark
                    ),
                    LinearLayout.LayoutParams(0, -2, 1f)
                )
                row.addView(
                    textView(
                        if (record.received) money(record.amount) else "—",
                        13f, green
                    ),
                    LinearLayout.LayoutParams(0, -2, 1f)
                )
                list.addView(row)
                val divider = View(this).apply {
                    setBackgroundColor(android.graphics.Color.LTGRAY)
                }
                list.addView(divider, LinearLayout.LayoutParams(-1, 1))
            }
        }

        val scroll = ScrollView(this)
        scroll.addView(list)
        root.addView(
            scroll,
            LinearLayout.LayoutParams(-1, 0, 1f)
        )
        setContentView(root)
    }

    private fun loadRecords() {
        val prefs = getSharedPreferences("hasan_store_data", MODE_PRIVATE)
        val all = mutableListOf<Record>()
        var totalGiven = 0.0
        var totalReceived = 0.0

        prefs.all.forEach { (key, raw) ->
            if (raw !is String) return@forEach
            if (!key.startsWith("entry_") && !key.startsWith("receive_")) {
                return@forEach
            }

            val parts = raw.split("|", limit = 4)
            if (parts.size < 3 || parts[0] != customerName) return@forEach

            val value = parts[1].toDoubleOrNull() ?: return@forEach
            val received = key.startsWith("receive_")
            if (received) totalReceived += value else totalGiven += value

            all.add(
                Record(
                    date = parts[2],
                    note = parts.getOrElse(3) { "" },
                    amount = value,
                    received = received,
                    timestamp = key.substringAfter('_').toLongOrNull() ?: 0L
                )
            )
        }

        records = all.sortedByDescending { it.timestamp }
        due = totalGiven - totalReceived
    }

    private fun downloadPdf() {
        val pdf = PdfDocument()
        val pageWidth = 595
        val pageHeight = 842
        val margin = 38f
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            textSize = 12f
            color = android.graphics.Color.BLACK
            typeface = Typeface.create("sans-serif", Typeface.NORMAL)
        }
        val bold = Paint(paint).apply {
            typeface = Typeface.create("sans-serif", Typeface.BOLD)
            textSize = 17f
        }

        val lines = mutableListOf<String>()
        lines.add("Hasan Store - Customer Report")
        lines.add("Customer: $customerName")
        lines.add("Total due: ${money(due)}")
        lines.add("Date | Details | Gave | Received")
        lines.add("------------------------------------------")
        records.forEach {
            lines.add("${it.date} | ${it.note} | " +
                "${if (!it.received) money(it.amount) else "-"} | " +
                "${if (it.received) money(it.amount) else "-"}")
        }

        try {
            var pageNumber = 1
            var page = pdf.startPage(
                PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNumber).create()
            )
            var canvas: Canvas = page.canvas
            var y = 55f

            lines.forEachIndexed { index, line ->
                if (y > pageHeight - 45f) {
                    pdf.finishPage(page)
                    pageNumber++
                    page = pdf.startPage(
                        PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNumber).create()
                    )
                    canvas = page.canvas
                    y = 55f
                }

                val p = if (index == 0) bold else paint
                val maxWidth = pageWidth - margin * 2
                var remaining = line
                while (remaining.isNotEmpty()) {
                    var end = remaining.length
                    while (end > 1 && p.measureText(remaining.substring(0, end)) > maxWidth) {
                        end--
                    }
                    canvas.drawText(remaining.substring(0, end), margin, y, p)
                    y += if (index == 0) 28f else 21f
                    remaining = remaining.substring(end)
                    if (y > pageHeight - 45f && remaining.isNotEmpty()) {
                        pdf.finishPage(page)
                        pageNumber++
                        page = pdf.startPage(
                            PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNumber).create()
                        )
                        canvas = page.canvas
                        y = 55f
                    }
                }
            }
            pdf.finishPage(page)

            val fileName = "HasanStore_Report_${System.currentTimeMillis()}.pdf"
            val values = ContentValues().apply {
                put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
                put(MediaStore.MediaColumns.MIME_TYPE, "application/pdf")
                put(
                    MediaStore.MediaColumns.RELATIVE_PATH,
                    Environment.DIRECTORY_DOWNLOADS
                )
            }
            val uri = contentResolver.insert(
                MediaStore.Downloads.EXTERNAL_CONTENT_URI, values
            ) ?: throw IllegalStateException("ফাইল তৈরি করা যায়নি")

            contentResolver.openOutputStream(uri)?.use { output ->
                pdf.writeTo(output)
            } ?: throw IllegalStateException("ফাইলে লেখা যায়নি")

            Toast.makeText(
                this,
                "PDF Downloads ফোল্ডারে সংরক্ষণ হয়েছে",
                Toast.LENGTH_LONG
            ).show()
        } catch (e: Exception) {
            Toast.makeText(
                this,
                "PDF তৈরি করা যায়নি: ${e.message}",
                Toast.LENGTH_LONG
            ).show()
        } finally {
            pdf.close()
        }
    }
}
