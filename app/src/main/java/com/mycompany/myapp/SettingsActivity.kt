package com.md.hasenstro.app

import android.app.Activity
import android.app.AlertDialog
import android.content.Intent
import android.content.res.ColorStateList
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.graphics.drawable.GradientDrawable
import android.net.Uri
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import org.json.JSONArray
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.util.Locale

class SettingsActivity : Activity() {

    private val green = Color.rgb(0, 99, 65)
    private val dark = Color.rgb(24, 58, 41)
    private val bg = Color.rgb(244, 247, 242)
    private val settings by lazy {
        getSharedPreferences("app_settings", MODE_PRIVATE)
    }

    private var pendingBackup: String? = null
    private var pendingPdf: ByteArray? = null

    private data class Transaction(
        val key: String,
        val name: String,
        val amount: Double,
        val amountText: String,
        val date: String,
        val note: String,
        val received: Boolean
    )

    private data class CustomerLedger(
        val name: String,
        var entryTotal: Double = 0.0,
        var receivedTotal: Double = 0.0
    ) {
        val due: Double
            get() = entryTotal - receivedTotal
    }

    private fun label(text: String, size: Float = 16f): TextView =
        TextView(this).apply {
            this.text = text
            textSize = size
            setTextColor(dark)
            setPadding(4, 10, 4, 10)
        }

    private fun button(text: String, action: () -> Unit): Button =
        Button(this).apply {
            this.text = text
            isAllCaps = false
            setTextColor(Color.WHITE)
            backgroundTintList = ColorStateList.valueOf(green)
            setOnClickListener { action() }
        }

    private fun cardBackground(): GradientDrawable =
        GradientDrawable().apply {
            setColor(Color.WHITE)
            cornerRadius = 18f
            setStroke(1, Color.rgb(225, 232, 225))
        }

    private fun settingRow(
        icon: String,
        title: String,
        subtitle: String,
        action: () -> Unit
    ): View {
        val row = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(14, 14, 12, 14)
            background = cardBackground()
            elevation = 3f
            minimumHeight = 84
            isClickable = true
            isFocusable = true
            setOnClickListener { action() }
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                bottomMargin = 12
            }
        }

        val iconView = TextView(this).apply {
            text = icon
            textSize = 25f
            gravity = Gravity.CENTER
            background = GradientDrawable().apply {
                setColor(Color.rgb(231, 243, 234))
                cornerRadius = 18f
            }
            layoutParams = LinearLayout.LayoutParams(52, 52)
        }
        row.addView(iconView)

        val textColumn = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(12, 0, 6, 0)
            layoutParams = LinearLayout.LayoutParams(0, -2, 1f)
        }

        textColumn.addView(TextView(this).apply {
            text = title
            textSize = 16f
            setTextColor(dark)
            setTypeface(null, Typeface.BOLD)
            setPadding(0, 0, 0, 5)
        })

        textColumn.addView(TextView(this).apply {
            text = subtitle
            textSize = 12f
            setTextColor(Color.rgb(105, 115, 106))
        })

        row.addView(textColumn)

        row.addView(TextView(this).apply {
            text = "›"
            textSize = 28f
            gravity = Gravity.CENTER
            setTextColor(green)
            background = GradientDrawable().apply {
                setColor(Color.rgb(240, 247, 241))
                cornerRadius = 30f
            }
            layoutParams = LinearLayout.LayoutParams(34, 34)
        })

        return row
    }

    private fun sectionTitle(text: String): TextView =
        TextView(this).apply {
            this.text = text
            textSize = 14f
            setTextColor(green)
            setTypeface(null, Typeface.BOLD)
            setPadding(4, 18, 4, 10)
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        window.statusBarColor = green
        window.navigationBarColor = Color.WHITE
        @Suppress("DEPRECATION")
        run {
            window.decorView.systemUiVisibility =
                View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR
        }

        val content = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(18, 8, 18, 28)
            setBackgroundColor(bg)
        }

        content.addView(label("‹  সেটিং", 24f).apply {
            setTypeface(null, Typeface.BOLD)
            setOnClickListener { finish() }
        })

        content.addView(sectionTitle("প্রোফাইল"))
        content.addView(
            settingRow(
                "👤",
                "প্রোফাইল",
                "ব্যবসার নাম, মালিকের নাম ও ফোন নম্বর",
                ::editProfile
            )
        )

        content.addView(
            settingRow(
                "🔃",
                "ব্যাকআপ ডেটা",
                "ডেটা সংরক্ষণ বা আগের ডেটা ফিরিয়ে আনুন",
                ::showBackupOptions
            ).apply {
                setMargins(top = 10)
            }
        )

        content.addView(sectionTitle("অ্যাপ সম্পর্কে"))
        content.addView(
            settingRow(
                "↪️",
                "অ্যাপ সম্পর্কে",
                "অ্যাপের নাম ও ভার্সন",
                ::showAbout
            )
        )

        val scroll = ScrollView(this).apply {
            isFillViewport = true
            addView(content)
        }
        setContentView(scroll)

        val originalLeft = scroll.paddingLeft
        val originalTop = scroll.paddingTop
        val originalRight = scroll.paddingRight
        val originalBottom = scroll.paddingBottom

        scroll.setOnApplyWindowInsetsListener { view, insets ->
            @Suppress("DEPRECATION")
            val statusBarTop = insets.systemWindowInsetTop
            view.setPadding(
                originalLeft,
                originalTop + statusBarTop,
                originalRight,
                originalBottom
            )
            insets
        }
        scroll.requestApplyInsets()
    }

    private fun showBackupOptions() {
        AlertDialog.Builder(this)
            .setTitle("ব্যাকআপ ডেটা")
            .setItems(
                arrayOf("🔃 ব্যাকআপ তৈরি করুন", "↩️ ব্যাকআপ থেকে Restore করুন")
            ) { _, which ->
                if (which == 0) createBackup() else restoreBackup()
            }
            .setNegativeButton("বাতিল", null)
            .show()
    }

    private fun View.setMargins(top: Int = 0) {
        layoutParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        ).apply {
            topMargin = top
        }
    }

    private fun editProfile() {
        val form = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(24, 4, 24, 0)
        }

        fun field(hintText: String, key: String): EditText {
            val input = EditText(this).apply {
                hint = hintText
                setSingleLine(true)
                setText(settings.getString(key, ""))
            }
            form.addView(input)
            return input
        }

        val business = field("ব্যবসার নাম", "business_name")
        val owner = field("মালিকের নাম", "owner_name")
        val phone = field("ফোন নম্বর", "phone")

        AlertDialog.Builder(this)
            .setTitle("প্রোফাইল সম্পাদনা")
            .setView(form)
            .setNegativeButton("বাতিল", null)
            .setPositiveButton("সংরক্ষণ") { _, _ ->
                val saved = settings.edit()
                    .putString("business_name", business.text.toString().trim())
                    .putString("owner_name", owner.text.toString().trim())
                    .putString("phone", phone.text.toString().trim())
                    .commit()

                Toast.makeText(
                    this,
                    if (saved) "প্রোফাইল সংরক্ষিত হয়েছে"
                    else "সংরক্ষণ করা যায়নি",
                    Toast.LENGTH_SHORT
                ).show()
            }
            .show()
    }

    private fun showAbout() {
        val appName = try {
            val id = resources.getIdentifier("app_name", "string", packageName)
            if (id != 0) getString(id) else "Hasenstro"
        } catch (_: Exception) {
            "Hasenstro"
        }

        val version = try {
            @Suppress("DEPRECATION")
            packageManager.getPackageInfo(packageName, 0).versionName ?: "1.0"
        } catch (_: Exception) {
            "1.0"
        }

        AlertDialog.Builder(this)
            .setTitle("অ্যাপ সম্পর্কে")
            .setMessage(
                "অ্যাপ: $appName\n" +
                "ভার্সন: $version\n\n" +
                "কাস্টমার ও টাকা-পাওনার ব্যবস্থাপনা অ্যাপ"
            )
            .setPositiveButton("ঠিক আছে", null)
            .show()
    }

    private fun showPdfOptions() {
        AlertDialog.Builder(this)
            .setTitle("PDF রিপোর্ট")
            .setMessage(
                "ফোনে PDF হিসেবে সব গ্রাহকের হিসাব ও প্রতিটি লেনদেন সংরক্ষণ করুন।"
            )
            .setNegativeButton("বাতিল", null)
            .setPositiveButton("PDF তৈরি করুন") { _, _ ->
                createPdfBackup()
            }
            .show()
    }

    private fun readTransactions(): List<Transaction> {
        val prefs = getSharedPreferences("hasan_store_data", MODE_PRIVATE)

        return prefs.all.mapNotNull { (key, raw) ->
            if (!key.startsWith("entry_") &&
                !key.startsWith("receive_")
            ) return@mapNotNull null

            val value = raw as? String ?: return@mapNotNull null
            val parts = value.split("|", limit = 4)
            if (parts.size < 3) return@mapNotNull null

            val amountText = parts[1].trim()
            val amount = amountText.toDoubleOrNull()
                ?: return@mapNotNull null

            Transaction(
                key = key,
                name = parts[0].trim().ifEmpty { "নাম দেওয়া নেই" },
                amount = amount,
                amountText = amountText,
                date = parts[2].trim(),
                note = if (parts.size >= 4) parts[3].trim() else "",
                received = key.startsWith("receive_")
            )
        }.sortedBy {
            it.key.substringAfterLast("_").toLongOrNull() ?: 0L
        }
    }

    private fun createPdfBackup() {
        try {
            pendingPdf = generatePdf()
            val intent = Intent(Intent.ACTION_CREATE_DOCUMENT).apply {
                addCategory(Intent.CATEGORY_OPENABLE)
                type = "application/pdf"
                putExtra(Intent.EXTRA_TITLE, "hasenstro-report.pdf")
            }
            startActivityForResult(intent, 103)
        } catch (e: Exception) {
            pendingPdf = null
            Toast.makeText(
                this,
                "PDF তৈরি করা যায়নি: ${e.message}",
                Toast.LENGTH_LONG
            ).show()
        }
    }

    private fun generatePdf(): ByteArray {
        val pdf = PdfDocument()
        val pageWidth = 595
        val pageHeight = 842
        val margin = 34f
        val maxWidth = pageWidth - margin * 2

        val titlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(0, 99, 65)
            textSize = 18f
            typeface = Typeface.create("sans-serif", Typeface.BOLD)
        }
        val headingPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(24, 58, 41)
            textSize = 13f
            typeface = Typeface.create("sans-serif", Typeface.BOLD)
        }
        val bodyPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.BLACK
            textSize = 10.5f
            typeface = Typeface.create("sans-serif", Typeface.NORMAL)
        }
        val smallPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.DKGRAY
            textSize = 9f
            typeface = Typeface.create("sans-serif", Typeface.NORMAL)
        }

        var pageNumber = 0
        var page: PdfDocument.Page? = null
        var canvas: Canvas? = null
        var y = 0f

        fun newPage() {
            page?.let { pdf.finishPage(it) }
            pageNumber++
            page = pdf.startPage(
                PdfDocument.PageInfo.Builder(
                    pageWidth, pageHeight, pageNumber
                ).create()
            )
            canvas = page!!.canvas
            canvas!!.drawText("Hasenstro — হিসাব রিপোর্ট", margin, 32f, titlePaint)
            canvas!!.drawLine(margin, 42f, pageWidth - margin, 42f, headingPaint)
            y = 61f
        }

        fun line(text: String, paint: Paint = bodyPaint, gap: Float = 5f) {
            var remaining = text
            if (remaining.isEmpty()) {
                y += paint.textSize + gap
                return
            }

            while (remaining.isNotEmpty()) {
                if (y > pageHeight - 42f) newPage()

                val count = paint.breakText(
                    remaining, true, maxWidth, null
                ).coerceAtLeast(1)

                val piece = remaining.substring(0, count)
                canvas!!.drawText(piece, margin, y, paint)
                remaining = remaining.substring(count).trimStart()
                y += paint.textSize + gap
            }
        }

        try {
            newPage()

            val business = settings.getString("business_name", "").orEmpty()
            val owner = settings.getString("owner_name", "").orEmpty()
            val phone = settings.getString("phone", "").orEmpty()

            line("ব্যবসা: ${business.ifBlank { "নাম দেওয়া নেই" }}", headingPaint)
            if (owner.isNotBlank()) line("মালিক: $owner", bodyPaint)
            if (phone.isNotBlank()) line("ফোন: $phone", bodyPaint)

            val transactions = readTransactions()
            val customers = linkedMapOf<String, CustomerLedger>()

            for (tx in transactions) {
                val customerKey = tx.name.lowercase(Locale.ROOT)
                val ledger = customers.getOrPut(customerKey) {
                    CustomerLedger(tx.name)
                }
                if (tx.received) ledger.receivedTotal += tx.amount
                else ledger.entryTotal += tx.amount
            }

            val totalDue = customers.values.sumOf { it.due }
            val totalEntry = transactions.filter { !it.received }.sumOf { it.amount }
            val totalReceived = transactions.filter { it.received }.sumOf { it.amount }

            line("")
            line("সারসংক্ষেপ", headingPaint)
            line("মোট গ্রাহক: ${customers.size}")
            line("মোট বাকি/বিক্রি: ${money(totalEntry)}")
            line("মোট জমা: ${money(totalReceived)}")
            line("নিট বাকি: ${money(totalDue)}")
            line("")
            line("গ্রাহকভিত্তিক হিসাব", headingPaint)

            if (customers.isEmpty()) {
                line("এখনও কোনো লেনদেন পাওয়া যায়নি।")
            } else {
                for (customer in customers.values) {
                    line(customer.name, headingPaint)
                    line("বাকি/বিক্রি: ${money(customer.entryTotal)}")
                    line("জমা: ${money(customer.receivedTotal)}")
                    line("বর্তমান বাকি: ${money(customer.due)}")
                    line("")
                }
            }

            line("সম্পূর্ণ লেনদেনের তালিকা", headingPaint)

            if (transactions.isEmpty()) {
                line("কোনো লেনদেন নেই।")
            } else {
                for ((index, tx) in transactions.withIndex()) {
                    val type = if (tx.received) "জমা" else "বাকি/বিক্রি"
                    line("${index + 1}. ${tx.name} — $type")
                    line("টাকা: ${tx.amountText}    তারিখ: ${tx.date}")
                    if (tx.note.isNotBlank()) line("নোট: ${tx.note}")
                    line("")
                }
            }

            page?.let { pdf.finishPage(it) }
            page = null

            val output = ByteArrayOutputStream()
            pdf.writeTo(output)
            return output.toByteArray()
        } finally {
            page?.let {
                try {
                    pdf.finishPage(it)
                } catch (_: Exception) {
                }
            }
            pdf.close()
        }
    }

    private fun money(amount: Double): String =
        String.format(Locale.US, "%.2f", amount)

    private fun createBackup() {
        try {
            val root = JSONObject()
            val names = arrayOf("hasan_store_data", "app_settings")

            for (name in names) {
                val prefs = getSharedPreferences(name, MODE_PRIVATE)
                val values = JSONObject()

                for ((key, value) in prefs.all) {
                    val item = JSONObject()
                    when (value) {
                        is String -> {
                            item.put("type", "String")
                            item.put("value", value)
                        }
                        is Int -> {
                            item.put("type", "Int")
                            item.put("value", value)
                        }
                        is Long -> {
                            item.put("type", "Long")
                            item.put("value", value)
                        }
                        is Float -> {
                            item.put("type", "Float")
                            item.put("value", value.toDouble())
                        }
                        is Boolean -> {
                            item.put("type", "Boolean")
                            item.put("value", value)
                        }
                        is Set<*> -> {
                            item.put("type", "Set")
                            item.put("value", JSONArray(value.toList()))
                        }
                        else -> continue
                    }
                    values.put(key, item)
                }
                root.put(name, values)
            }

            pendingBackup = root.toString(2)

            val intent = Intent(Intent.ACTION_CREATE_DOCUMENT).apply {
                addCategory(Intent.CATEGORY_OPENABLE)
                type = "application/json"
                putExtra(Intent.EXTRA_TITLE, "hasenstro-backup.json")
            }
            startActivityForResult(intent, 101)
        } catch (e: Exception) {
            pendingBackup = null
            Toast.makeText(
                this,
                "ব্যাকআপ তৈরি করা যায়নি: ${e.message}",
                Toast.LENGTH_LONG
            ).show()
        }
    }

    private fun restoreBackup() {
        AlertDialog.Builder(this)
            .setTitle("ব্যাকআপ Restore")
            .setMessage(
                "Restore করলে বর্তমান হিসাব ও সেটিংস ব্যাকআপের ডেটা দিয়ে " +
                "প্রতিস্থাপিত হবে। আগে বর্তমান ডেটার JSON ব্যাকআপ নিন।"
            )
            .setNegativeButton("বাতিল", null)
            .setPositiveButton("ফাইল বেছে নিন") { _, _ ->
                val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
                    addCategory(Intent.CATEGORY_OPENABLE)
                    type = "application/json"
                }
                startActivityForResult(intent, 102)
            }
            .show()
    }

    private fun parseBackup(raw: String): Map<String, Map<String, Any>> {
        val root = JSONObject(raw)
        val names = listOf("hasan_store_data", "app_settings")

        if (names.any { !root.has(it) || root.isNull(it) }) {
            throw Exception("ব্যাকআপে প্রয়োজনীয় ডেটা নেই")
        }

        val result = linkedMapOf<String, Map<String, Any>>()

        for (name in names) {
            val values = root.getJSONObject(name)
            val restored = linkedMapOf<String, Any>()
            val keys = values.keys()

            while (keys.hasNext()) {
                val key = keys.next()
                val item = values.getJSONObject(key)
                val value = item.get("value")

                val parsed: Any = when (item.getString("type")) {
                    "String" -> value as? String
                        ?: throw Exception("ব্যাকআপে ভুল String ডেটা আছে")
                    "Int" -> (value as? Number)?.toInt()
                        ?: throw Exception("ব্যাকআপে ভুল Int ডেটা আছে")
                    "Long" -> (value as? Number)?.toLong()
                        ?: throw Exception("ব্যাকআপে ভুল Long ডেটা আছে")
                    "Float" -> (value as? Number)?.toFloat()
                        ?: throw Exception("ব্যাকআপে ভুল Float ডেটা আছে")
                    "Boolean" -> value as? Boolean
                        ?: throw Exception("ব্যাকআপে ভুল Boolean ডেটা আছে")
                    "Set" -> {
                        val array = value as? JSONArray
                            ?: throw Exception("ব্যাকআপে ভুল Set ডেটা আছে")
                        val set = mutableSetOf<String>()
                        for (i in 0 until array.length()) {
                            set.add(array.getString(i))
                        }
                        set
                    }
                    else -> throw Exception("অচেনা ডেটা টাইপ: ${item.optString("type")}")
                }

                restored[key] = parsed
            }
            result[name] = restored
        }

        return result
    }

    private fun applyBackup(data: Map<String, Map<String, Any>>) {
        for ((name, values) in data) {
            val editor = getSharedPreferences(name, MODE_PRIVATE).edit().clear()

            for ((key, value) in values) {
                when (value) {
                    is String -> editor.putString(key, value)
                    is Int -> editor.putInt(key, value)
                    is Long -> editor.putLong(key, value)
                    is Float -> editor.putFloat(key, value)
                    is Boolean -> editor.putBoolean(key, value)
                    is Set<*> -> {
                        @Suppress("UNCHECKED_CAST")
                        editor.putStringSet(key, value as Set<String>)
                    }
                }
            }

            if (!editor.commit()) {
                throw Exception("ডেটা সংরক্ষণ করা যায়নি: $name")
            }
        }
    }

    @Deprecated("Uses document picker result")
    override fun onActivityResult(
        requestCode: Int,
        resultCode: Int,
        data: Intent?
    ) {
        super.onActivityResult(requestCode, resultCode, data)

        if (resultCode != RESULT_OK) {
            if (requestCode == 101) pendingBackup = null
            if (requestCode == 103) pendingPdf = null
            return
        }

        val uri: Uri = data?.data ?: return

        try {
            when (requestCode) {
                101 -> {
                    val backup = pendingBackup
                        ?: throw Exception("ব্যাকআপ ডেটা পাওয়া যায়নি")

                    contentResolver.openOutputStream(uri)?.use {
                        it.write(backup.toByteArray(Charsets.UTF_8))
                    } ?: throw Exception("ফাইল লেখা যায়নি")

                    pendingBackup = null
                    Toast.makeText(
                        this,
                        "JSON ব্যাকআপ তৈরি হয়েছে",
                        Toast.LENGTH_LONG
                    ).show()
                }

                102 -> {
                    val raw = contentResolver.openInputStream(uri)?.bufferedReader()
                        ?.use { it.readText() }
                        ?: throw Exception("ফাইল পড়া যায়নি")

                    // Validate the entire backup before changing saved preferences.
                    val restored = parseBackup(raw)
                    applyBackup(restored)

                    Toast.makeText(
                        this,
                        "ব্যাকআপ Restore হয়েছে",
                        Toast.LENGTH_LONG
                    ).show()
                }

                103 -> {
                    val bytes = pendingPdf
                        ?: throw Exception("PDF ডেটা পাওয়া যায়নি")

                    contentResolver.openOutputStream(uri)?.use {
                        it.write(bytes)
                    } ?: throw Exception("PDF ফাইল লেখা যায়নি")

                    pendingPdf = null
                    Toast.makeText(
                        this,
                        "PDF রিপোর্ট সংরক্ষণ হয়েছে",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
        } catch (e: Exception) {
            pendingBackup = null
            pendingPdf = null
            Toast.makeText(
                this,
                "কাজটি করা যায়নি: ${e.message}",
                Toast.LENGTH_LONG
            ).show()
        }
    }
}
