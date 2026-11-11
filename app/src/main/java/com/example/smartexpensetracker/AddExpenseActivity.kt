package com.example.smartexpensetracker

import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.Spinner
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.smartexpensetracker.data.Expense
import com.example.smartexpensetracker.data.ExpenseDatabase
import com.example.smartexpensetracker.data.ExpenseRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class AddExpenseActivity : AppCompatActivity() {

    private lateinit var etExpenseTitle: EditText
    private lateinit var etExpenseAmount: EditText
    private lateinit var etExpenseNote: EditText
    private lateinit var spExpenseCategory: Spinner
    private lateinit var spPaymentMethod: Spinner
    private lateinit var btnSaveExpense: Button

    private lateinit var repository: ExpenseRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_add_expense)

        val expenseDao = ExpenseDatabase.getDatabase(this).expenseDao()
        repository = ExpenseRepository(expenseDao)

        etExpenseTitle = findViewById(R.id.etExpenseTitle)
        etExpenseAmount = findViewById(R.id.etExpenseAmount)
        etExpenseNote = findViewById(R.id.etExpenseNote)
        spExpenseCategory = findViewById(R.id.spExpenseCategory)
        spPaymentMethod = findViewById(R.id.spPaymentMethod)
        btnSaveExpense = findViewById(R.id.btnSaveExpense)

        // Receive scanned receipt details
        val scannedAmount = intent.getStringExtra("EXTRA_AMOUNT")
        val scannedText = intent.getStringExtra("EXTRA_RECEIPT_TEXT")

        if (!scannedAmount.isNullOrEmpty()) {
            etExpenseAmount.setText(scannedAmount)
        }

        if (!scannedText.isNullOrEmpty()) {
            etExpenseNote.setText(scannedText)
        }

        val categories = listOf(
            "Food", "Transport", "Shopping", "Bills",
            "Health", "Entertainment", "Education", "Other"
        )

        val paymentMethods = listOf(
            "Cash", "UPI", "Card", "Net Banking"
        )

        val categoryAdapter = ArrayAdapter(
            this,
            android.R.layout.simple_spinner_item,
            categories
        )

        categoryAdapter.setDropDownViewResource(
            android.R.layout.simple_spinner_dropdown_item
        )

        spExpenseCategory.adapter = categoryAdapter

        val paymentAdapter = ArrayAdapter(
            this,
            android.R.layout.simple_spinner_item,
            paymentMethods
        )

        paymentAdapter.setDropDownViewResource(
            android.R.layout.simple_spinner_dropdown_item
        )

        spPaymentMethod.adapter = paymentAdapter

        btnSaveExpense.setOnClickListener {
            saveExpense()
        }
    }

    private fun saveExpense() {

        val title = etExpenseTitle.text.toString().trim()
        val amountText = etExpenseAmount.text.toString().trim()
        val note = etExpenseNote.text.toString().trim()

        if (title.isEmpty()) {
            etExpenseTitle.error = "Please enter expense title"
            etExpenseTitle.requestFocus()
            return
        }

        val amount = amountText.toDoubleOrNull()

        if (amount == null || amount <= 0) {
            etExpenseAmount.error = "Enter a valid amount"
            etExpenseAmount.requestFocus()
            return
        }

        val category = spExpenseCategory.selectedItem.toString()
        val paymentMethod = spPaymentMethod.selectedItem.toString()

        val expense = Expense(
            title = title,
            amount = amount,
            category = category,
            date = System.currentTimeMillis(),
            note = note,
            paymentMethod = paymentMethod
        )

        lifecycleScope.launch {

            withContext(Dispatchers.IO) {
                repository.insertExpense(expense)
            }

            Toast.makeText(
                this@AddExpenseActivity,
                "Expense saved successfully!",
                Toast.LENGTH_SHORT
            ).show()

            finish()
        }
    }
}
