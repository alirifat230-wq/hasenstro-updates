package com.md.hasenstro.app

import android.app.DatePickerDialog
import android.content.Intent
import android.content.res.ColorStateList
import android.graphics.Color
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class CustomerDetailActivity : AppCompatActivity() {
    private var receiveMode = false

    @Suppress("DEPRECATION")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.statusBarColor = Color.rgb(0, 99, 65)
        window.navigationBarColor = Color.WHITE
        window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR
        setContentView(R.layout.activity_customer_detail)

        val name = intent.getStringExtra("customer_name") ?: ""
        val due = intent.getDoubleExtra("customer_due", 0.0)

        val nameView = findViewById<TextView>(R.id.detailCustomerName)
        val dueView = findViewById<TextView>(R.id.detailCustomerDue)
        val amount = findViewById<EditText>(R.id.detailAmount)
        val date = findViewById<EditText>(R.id.detailDate)
        val note = findViewById<EditText>(R.id.detailNote)
        val addButton = findViewById<Button>(R.id.detailAddButton)
        val receiveButton = findViewById<Button>(R.id.detailReceiveButton)
        val confirmButton = findViewById<Button>(R.id.detailConfirmButton)

        nameView.text = name
        dueView.text = "৳ " + String.format(Locale.US, "%,.2f", due)
        date.setText(SimpleDateFormat("dd MMMM yyyy", Locale("bn", "BD")).format(Date()))

        findViewById<TextView>(R.id.detailBackButton).setOnClickListener {
            finish()
        }

        findViewById<TextView>(R.id.detailMenuButton).setOnClickListener {
            Toast.makeText(this, "মেনু", Toast.LENGTH_SHORT).show()
        }

        findViewById<Button>(R.id.detailReportButton).setOnClickListener {
            startActivity(
                Intent(this, ReportActivity::class.java)
                    .putExtra("customer_name", name)
            )
        }

        fun updateMode(receive: Boolean) {
            receiveMode = receive
            addButton.backgroundTintList = ColorStateList.valueOf(
                if (!receive) Color.rgb(0, 99, 65) else Color.rgb(225, 244, 231)
            )
            addButton.setTextColor(
                if (!receive) Color.WHITE else Color.rgb(0, 99, 65)
            )
            receiveButton.backgroundTintList = ColorStateList.valueOf(
                if (receive) Color.rgb(0, 99, 65) else Color.rgb(225, 244, 231)
            )
            receiveButton.setTextColor(
                if (receive) Color.WHITE else Color.rgb(0, 99, 65)
            )
            confirmButton.text = if (receive) "জমা নিশ্চিত করুন" else "নিশ্চিত করুন"
            amount.hint = if (receive) "কত টাকা জমা দিলেন?" else "কত টাকা বাকি দিলেন?"
        }

        addButton.setOnClickListener { updateMode(false) }
        receiveButton.setOnClickListener { updateMode(true) }
        updateMode(false)

        date.setOnClickListener {
            val calendar = Calendar.getInstance()
            DatePickerDialog(
                this,
                { _, year, month, day ->
                    calendar.set(year, month, day)
                    date.setText(
                        SimpleDateFormat("dd MMMM yyyy", Locale("bn", "BD"))
                            .format(calendar.time)
                    )
                },
                calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH),
                calendar.get(Calendar.DAY_OF_MONTH)
            ).show()
        }

        confirmButton.setOnClickListener {
            val amountText = amount.text.toString().trim()
            val value = amountText.toDoubleOrNull()

            if (value == null || !value.isFinite() || value <= 0.0) {
                amount.error = "সঠিক টাকার পরিমাণ লিখুন"
                amount.requestFocus()
                return@setOnClickListener
            }

            val prefs = getSharedPreferences("hasan_store_data", MODE_PRIVATE)
            val prefix = if (receiveMode) "receive_" else "entry_"
            val key = prefix + System.currentTimeMillis()
            val record = "$name|$amountText|${date.text}|${note.text.toString().trim()}"

            if (prefs.edit().putString(key, record).commit()) {
                Toast.makeText(
                    this,
                    if (receiveMode) "জমার হিসাব সংরক্ষণ হয়েছে"
                    else "বাকির হিসাব সংরক্ষণ হয়েছে",
                    Toast.LENGTH_LONG
                ).show()
                finish()
            } else {
                Toast.makeText(this, "সংরক্ষণ করা যায়নি", Toast.LENGTH_LONG).show()
            }
        }
    }
}
