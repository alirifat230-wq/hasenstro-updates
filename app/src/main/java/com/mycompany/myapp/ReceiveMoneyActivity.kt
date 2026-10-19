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

class ReceiveMoneyActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        window.statusBarColor = Color.rgb(0, 99, 65)
        window.navigationBarColor = Color.WHITE
        @Suppress("DEPRECATION")
        run {
            window.decorView.systemUiVisibility =
                View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR
        }

        setContentView(R.layout.activity_receive_money)

        val nameInput = findViewById<EditText>(R.id.receiveCustomerName)
        val amountInput = findViewById<EditText>(R.id.receiveAmount)
        val dateInput = findViewById<EditText>(R.id.receiveDate)
        val noteInput = findViewById<EditText>(R.id.receiveNote)

        dateInput.setText(
            SimpleDateFormat("dd-MM-yyyy", Locale.getDefault()).format(Date())
        )

        dateInput.setOnClickListener {
            val calendar = Calendar.getInstance()
            DatePickerDialog(
                this,
                { _, year, month, day ->
                    dateInput.setText(
                        String.format(
                            Locale.getDefault(),
                            "%02d-%02d-%04d",
                            day, month + 1, year
                        )
                    )
                },
                calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH),
                calendar.get(Calendar.DAY_OF_MONTH)
            ).show()
        }

        findViewById<TextView>(R.id.receiveBackButton).setOnClickListener {
            finish()
        }

        findViewById<Button>(R.id.saveReceiveButton).setOnClickListener {
            val name = nameInput.text.toString().trim()
            val amountText = amountInput.text.toString().trim()
            val amount = amountText.toDoubleOrNull()
            val date = dateInput.text.toString().trim()
            val note = noteInput.text.toString().trim()

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
            val key = "receive_" + System.currentTimeMillis()
            val record = "$name|$amountText|$date|$note"

            if (prefs.edit().putString(key, record).commit()) {
                Toast.makeText(
                    this,
                    "জমার হিসাব সংরক্ষণ হয়েছে",
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
