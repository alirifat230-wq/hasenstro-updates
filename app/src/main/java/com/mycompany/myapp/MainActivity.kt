package com.md.hasenstro.app

import android.app.AlertDialog
import android.graphics.Color
import android.graphics.Typeface
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.RadioButton
import android.widget.RadioGroup
import android.widget.TextView
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MainActivity : AppCompatActivity() {

    private lateinit var totalDueView: TextView
    private lateinit var searchCustomer: EditText
    private lateinit var emptyCustomer: LinearLayout

    private var selectedSort = 2

    private data class Customer(
        val name: String,
        var due: Double = 0.0,
        var lastDate: Long = 0L
    )

    @Suppress("DEPRECATION")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        window.statusBarColor = Color.rgb(0, 99, 65)
        window.navigationBarColor = Color.WHITE
        window.decorView.systemUiVisibility =
            View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR

        setContentView(R.layout.activity_main)

        totalDueView = findViewById(R.id.totalDue)
        searchCustomer = findViewById(R.id.searchCustomer)
        emptyCustomer = findViewById(R.id.emptyCustomer)

        findViewById<View>(R.id.addMoneyButton).setOnClickListener {
            startActivity(Intent(this, AddMoneyActivity::class.java))
        }


        findViewById<View>(R.id.navSettings).setOnClickListener {
            startActivity(Intent(this, SettingsActivity::class.java))
        }

        findViewById<View>(R.id.filterButton).setOnClickListener {
            showFilterDialog()
        }

        searchCustomer.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(
                s: CharSequence?, start: Int, count: Int, after: Int
            ) = Unit

            override fun onTextChanged(
                s: CharSequence?, start: Int, before: Int, count: Int
            ) {
                refreshHome()
            }

            override fun afterTextChanged(s: Editable?) = Unit
        })

        if (android.os.Build.VERSION.SDK_INT >= 35) {
            val content = findViewById<ViewGroup>(android.R.id.content)
            val root = content.getChildAt(0)

            root?.setOnApplyWindowInsetsListener { view, insets ->
                val top = insets.getInsets(
                    android.view.WindowInsets.Type.statusBars()
                ).top
                val bottom = insets.getInsets(
                    android.view.WindowInsets.Type.navigationBars()
                ).bottom
                view.setPadding(0, top, 0, bottom)
                insets
            }
            root?.requestApplyInsets()
        }
    }

    override fun onResume() {
        super.onResume()
        if (::totalDueView.isInitialized) refreshHome()

        val prefs = getSharedPreferences("hasenstro_update_check", MODE_PRIVATE)
        val now = System.currentTimeMillis()
        val lastCheck = prefs.getLong("last_check", 0L)
        if (now - lastCheck >= 6 * 60 * 60 * 1000L) {
            prefs.edit().putLong("last_check", now).apply()
            AppUpdater.check(this)
        }
    }

    private fun parseDate(date: String): Long {
        if (date.isBlank()) return 0L
        return try {
            SimpleDateFormat("dd-MM-yyyy", Locale.getDefault())
                .apply { isLenient = false }
                .parse(date)?.time ?: 0L
        } catch (_: Exception) {
            0L
        }
    }

    private fun readCustomers(): List<Customer> {
        val prefs = getSharedPreferences("hasan_store_data", MODE_PRIVATE)
        val customers = linkedMapOf<String, Customer>()

        for ((key, value) in prefs.all) {
            if (value !is String) continue
            if (!key.startsWith("entry_") && !key.startsWith("receive_")) {
                continue
            }

            val parts = value.split("|", limit = 4)
            if (parts.size < 2) continue

            val name = parts[0].trim()
            val amount = parts[1].trim().toDoubleOrNull() ?: continue

            if (name.isEmpty() || !amount.isFinite() || amount <= 0.0) {
                continue
            }

            val normalizedName = name.lowercase(Locale.ROOT)
            val customer = customers.getOrPut(normalizedName) {
                Customer(name)
            }

            val transactionDate = if (parts.size >= 3) {
                parseDate(parts[2].trim())
            } else {
                0L
            }

            if (transactionDate > customer.lastDate) {
                customer.lastDate = transactionDate
            }

            if (key.startsWith("entry_")) {
                customer.due += amount
            } else {
                customer.due -= amount
            }
        }

        val result = customers.values.filter { it.due > 0.000001 }

        return when (selectedSort) {
            0 -> result.sortedBy { it.name.lowercase(Locale.ROOT) }
            1 -> result.sortedBy { it.due }
            2 -> result.sortedByDescending { it.due }
            3 -> result.sortedByDescending { it.lastDate }
            4 -> result.sortedBy { it.lastDate }
            else -> result.sortedByDescending { it.due }
        }
    }

    private fun showFilterDialog() {
        var temporarySort = selectedSort

        val green = Color.rgb(25, 110, 72)
        val dark = Color.rgb(38, 56, 45)
        val muted = Color.rgb(115, 130, 120)
        val pale = Color.rgb(239, 248, 242)
        val border = Color.rgb(220, 230, 222)
        val red = Color.rgb(211, 47, 47)

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(20), dp(18), dp(20), dp(20))
            setBackgroundColor(Color.WHITE)
        }

        val header = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = android.view.Gravity.CENTER_VERTICAL
        }

        val heading = TextView(this).apply {
            text = "ফিল্টার"
            textSize = 22f
            setTypeface(null, android.graphics.Typeface.BOLD)
            setTextColor(dark)
        }

        val close = TextView(this).apply {
            text = "×"
            textSize = 30f
            gravity = android.view.Gravity.CENTER
            setTextColor(muted)
            background = android.graphics.drawable.GradientDrawable().apply {
                shape = android.graphics.drawable.GradientDrawable.OVAL
                setColor(Color.rgb(245, 247, 245))
            }
        }

        header.addView(heading, LinearLayout.LayoutParams(0, dp(44), 1f))
        header.addView(close, LinearLayout.LayoutParams(dp(42), dp(42)))
        root.addView(header)

        val optionViews = mutableListOf<Pair<Int, TextView>>()

        fun refreshOptions() {
            optionViews.forEach { (index, view) ->
                val selected = temporarySort == index
                view.background =
                    android.graphics.drawable.GradientDrawable().apply {
                        cornerRadius = dp(12).toFloat()
                        setColor(if (selected) pale else Color.WHITE)
                        setStroke(dp(1), if (selected) green else border)
                    }
                view.setTextColor(if (selected) green else dark)
                view.setTypeface(
                    null,
                    if (selected) android.graphics.Typeface.BOLD
                    else android.graphics.Typeface.NORMAL
                )
                view.text = when (index) {
                    0 -> if (selected) "✓  কাস্টমার: নাম অনুযায়ী A → Z"
                         else "○  কাস্টমার: নাম অনুযায়ী A → Z"
                    1 -> if (selected) "✓  কম → বেশি" else "কম → বেশি"
                    2 -> if (selected) "✓  বেশি → কম" else "বেশি → কম"
                    3 -> if (selected) "✓  নতুন → পুরাতন" else "নতুন → পুরাতন"
                    else -> if (selected) "✓  পুরাতন → নতুন" else "পুরাতন → নতুন"
                }
            }
        }

        fun addSection(title: String) {
            val label = TextView(this).apply {
                text = title
                textSize = 14f
                setTypeface(null, android.graphics.Typeface.BOLD)
                setTextColor(muted)
                setPadding(0, dp(16), 0, dp(10))
            }
            root.addView(label)
        }

        fun addOption(index: Int, parent: LinearLayout) {
            val option = TextView(this).apply {
                textSize = 14f
                gravity = android.view.Gravity.CENTER
                setPadding(dp(8), dp(14), dp(8), dp(14))
                minHeight = dp(54)
                setOnClickListener {
                    temporarySort = index
                    refreshOptions()
                }
            }
            optionViews.add(index to option)
            parent.addView(
                option,
                LinearLayout.LayoutParams(
                    0,
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    1f
                ).apply {
                    marginStart = dp(5)
                    marginEnd = dp(5)
                }
            )
        }

        fun addDashedDivider() {
            val divider = View(this).apply {
                background = android.graphics.drawable.GradientDrawable().apply {
                    shape = android.graphics.drawable.GradientDrawable.LINE
                    setStroke(dp(1), border, dp(5).toFloat(), dp(4).toFloat())
                }
            }
            root.addView(
                divider,
                LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    dp(2)
                ).apply {
                    topMargin = dp(10)
                    bottomMargin = dp(2)
                }
            )
        }

        addSection("কাস্টমার")
        val customerRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
        }
        addOption(0, customerRow)
        root.addView(customerRow)

        addDashedDivider()
        addSection("পাওনা পরিমাণ")
        val dueRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
        }
        addOption(1, dueRow)
        addOption(2, dueRow)
        root.addView(dueRow)

        addDashedDivider()
        addSection("সর্বশেষ লেনদেন")
        val dateRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
        }
        addOption(3, dateRow)
        addOption(4, dateRow)
        root.addView(dateRow)

        refreshOptions()

        val confirm = TextView(this).apply {
            text = "নিশ্চিত"
            textSize = 16f
            gravity = android.view.Gravity.CENTER
            setTextColor(Color.WHITE)
            setTypeface(null, android.graphics.Typeface.BOLD)
            background = android.graphics.drawable.GradientDrawable().apply {
                cornerRadius = dp(12).toFloat()
                setColor(red)
            }
            setPadding(dp(12), dp(15), dp(12), dp(15))
        }

        root.addView(
            confirm,
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                topMargin = dp(22)
            }
        )

        val dialog = AlertDialog.Builder(this)
            .setView(root)
            .create()

        close.setOnClickListener { dialog.dismiss() }
        confirm.setOnClickListener {
            selectedSort = temporarySort
            refreshHome()
            dialog.dismiss()
        }

        dialog.show()
        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)
        dialog.window?.setLayout(
            (resources.displayMetrics.widthPixels * 0.94f).toInt(),
            ViewGroup.LayoutParams.WRAP_CONTENT
        )
    }

    private fun refreshHome() {
        val customers = readCustomers()
        val total = customers.sumOf { it.due }

        totalDueView.text = "৳ " + String.format(
            Locale.US, "%,.2f", total
        )

        val query = searchCustomer.text.toString().trim()
        val filtered = customers.filter {
            it.name.contains(query, ignoreCase = true)
        }

        val parent = emptyCustomer.parent as? ViewGroup ?: return

        for (i in parent.childCount - 1 downTo 0) {
            if (parent.getChildAt(i).tag == "customer_row") {
                parent.removeViewAt(i)
            }
        }

        val emptyIndex = parent.indexOfChild(emptyCustomer)

        if (filtered.isEmpty()) {
            emptyCustomer.visibility = View.VISIBLE
            val message = emptyCustomer.getChildAt(1) as? TextView
            message?.text = if (query.isEmpty()) {
                "এখনো কোনো কাস্টমার নেই"
            } else {
                "কোনো কাস্টমার পাওয়া যায়নি"
            }
            return
        }

        emptyCustomer.visibility = View.GONE

        filtered.forEachIndexed { index, customer ->
            val row = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
                setPadding(dp(14), dp(15), dp(14), dp(15))
                setBackgroundColor(Color.WHITE)
                tag = "customer_row"
            }

            val nameView = TextView(this).apply {
                text = customer.name
                textSize = 15f
                setTextColor(Color.rgb(24, 58, 41))
                typeface = Typeface.DEFAULT_BOLD
                layoutParams = LinearLayout.LayoutParams(
                    0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f
                )
            }

            val amountView = TextView(this).apply {
                text = "৳ " + String.format(
                    Locale.US, "%,.2f", customer.due
                )
                textSize = 15f
                setTextColor(Color.rgb(0, 99, 65))
                typeface = Typeface.DEFAULT_BOLD
                gravity = Gravity.END
            }

            row.addView(nameView)
            row.addView(amountView)

            val openCustomerDetail = View.OnClickListener {
                val intent = Intent(this@MainActivity, CustomerDetailActivity::class.java)
                intent.putExtra("customer_name", customer.name)
                intent.putExtra("customer_due", customer.due)
                startActivity(intent)
            }
            row.setOnClickListener(openCustomerDetail)
            nameView.setOnClickListener(openCustomerDetail)
            amountView.setOnClickListener(openCustomerDetail)

            val params = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
            params.topMargin = dp(1)

            parent.addView(row, emptyIndex + index, params)
        }
    }

    private fun dp(value: Int): Int {
        return (value * resources.displayMetrics.density + 0.5f).toInt()
    }
}
