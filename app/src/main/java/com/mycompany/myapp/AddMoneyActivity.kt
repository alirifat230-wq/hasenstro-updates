package com.md.hasenstro.app

import android.app.DatePickerDialog
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

class AddMoneyActivity : AppCompatActivity() {

    @Suppress("DEPRECATION")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        window.statusBarColor = Color.rgb(0, 99, 65)
        window.navigationBarColor = Color.WHITE
        window.decorView.systemUiVisibility =
            View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR

        setContentView(R.layout.activity_add_money)

    val root = findViewById<android.widget.ScrollView>(R.id.addMoneyRoot)
    val originalLeft = root.paddingLeft
    val originalTop = root.paddingTop
    val originalRight = root.paddingRight
    val originalBottom = root.paddingBottom

    root.setOnApplyWindowInsetsListener { view, insets ->
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
    root.requestApplyInsets()

        val nameInput = findViewById<EditText>(R.id.customerName)
        val amountInput = findViewById<EditText>(R.id.moneyAmount)
        val dateInput = findViewById<EditText>(R.id.moneyDate)
        val noteInput = findViewById<EditText>(R.id.moneyNote)
        val saveButton = findViewById<Button>(R.id.saveMoneyButton)

        dateInput.setText(
            SimpleDateFormat("dd-MM-yyyy", Locale.getDefault()).format(Date())
        )

        findViewById<TextView>(R.id.backButton).setOnClickListener {
            finish()
        }

        dateInput.setOnClickListener {
            val calendar = Calendar.getInstance()

            DatePickerDialog(
                this,
                { _, year, month, day ->
                    dateInput.setText(
                        String.format(
                            Locale.getDefault(),
                            "%02d-%02d-%04d",
                            day,
                            month + 1,
                            year
                        )
                    )
                },
                calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH),
                calendar.get(Calendar.DAY_OF_MONTH)
            ).show()
        }

        saveButton.setOnClickListener {
            val name = nameInput.text.toString().trim()
            val amountText = amountInput.text.toString().trim()
            val date = dateInput.text.toString().trim()
            val note = noteInput.text.toString().trim()
            val amount = amountText.toDoubleOrNull()

            if (name.isEmpty()) {
                nameInput.error = "কাস্টমারের নাম লিখুন"
                nameInput.requestFocus()
                return@setOnClickListener
            }

            if (amount == null || !amount.isFinite() || amount <= 0.0) {
                amountInput.error = "সঠিক টাকার পরিমাণ লিখুন"
                amountInput.requestFocus()
                return@setOnClickListener
            }

            val prefs = getSharedPreferences("hasan_store_data", MODE_PRIVATE)
            val key = "entry_" + System.currentTimeMillis()

            val saved = prefs.edit()
                .putString(key, "$name|$amountText|$date|$note")
                .commit()

            if (saved) {
                Toast.makeText(
                    this,
                    "টাকার হিসাব সংরক্ষণ হয়েছে",
                    Toast.LENGTH_LONG
                ).show()
                finish()
            } else {
                Toast.makeText(
                    this,
                    "সংরক্ষণ করা যায়নি। আবার চেষ্টা করুন।",
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }
}
