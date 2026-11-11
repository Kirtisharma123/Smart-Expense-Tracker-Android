package com.example.smartexpensetracker

import android.content.Intent
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.graphics.drawable.ColorDrawable
import android.graphics.pdf.PdfDocument
import android.net.Uri
import android.os.Bundle
import android.util.TypedValue
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.result.contract.ActivityResultContracts.CreateDocument
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.smartexpensetracker.data.CategoryTotal
import com.example.smartexpensetracker.data.Expense
import com.example.smartexpensetracker.data.ExpenseDatabase
import com.example.smartexpensetracker.data.ExpenseRepository
import com.example.smartexpensetracker.data.ExcelExpenseExporter
import com.github.mikephil.charting.charts.BarChart
import com.github.mikephil.charting.charts.PieChart
import com.github.mikephil.charting.data.BarData
import com.github.mikephil.charting.data.BarDataSet
import com.github.mikephil.charting.data.BarEntry
import com.github.mikephil.charting.data.PieData
import com.github.mikephil.charting.data.PieDataSet
import com.github.mikephil.charting.data.PieEntry
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter
import com.github.mikephil.charting.utils.ColorTemplate
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MainActivity : AppCompatActivity() {

    private lateinit var repository: ExpenseRepository
    private lateinit var expenseAdapter: ExpenseAdapter
    private lateinit var receiptOcrHelper: ReceiptOcrHelper

    private lateinit var tvTotalExpenses: TextView
    private lateinit var tvEmptyExpenses: TextView
    private lateinit var rvExpenses: RecyclerView

    private lateinit var chartCategoryPie: PieChart
    private lateinit var chartCategoryBar: BarChart

    // Receipt Image Picker
    private val pickReceipt =
        registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
            if (uri != null) {
                receiptOcrHelper.scanReceipt(
                    imageUri = uri,
                    onSuccess = { extractedText ->
                        runOnUiThread {
                            val detectedAmount =
                                extractReceiptAmount(extractedText)

                            showReceiptResultDialog(
                                extractedText,
                                detectedAmount
                            )
                        }
                    },
                    onError = { exception ->
                        runOnUiThread {
                            Toast.makeText(
                                this,
                                "Scanning failed: ${exception.localizedMessage}",
                                Toast.LENGTH_LONG
                            ).show()
                        }
                    }
                )
            }
        }

    // PDF File Picker
    private val createPdfLauncher =
        registerForActivityResult(CreateDocument("application/pdf")) { uri ->
            if (uri != null) {
                lifecycleScope.launch {
                    try {
                        val expenses = repository.allExpenses.first()

                        if (expenses.isEmpty()) {
                            Toast.makeText(
                                this@MainActivity,
                                "No expenses available to export",
                                Toast.LENGTH_SHORT
                            ).show()
                        } else {
                            exportExpensesToPdf(uri, expenses)
                        }
                    } catch (exception: Exception) {
                        Toast.makeText(
                            this@MainActivity,
                            "Export failed: ${exception.localizedMessage}",
                            Toast.LENGTH_LONG
                        ).show()
                    }
                }
            }
        }

    // Excel File Export
    private val createExcelLauncher =
        registerForActivityResult(
            CreateDocument(
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
            )
        ) { uri ->

            if (uri != null) {

                lifecycleScope.launch {

                    try {

                        val expenses = repository.allExpenses.first()

                        if (expenses.isEmpty()) {

                            Toast.makeText(
                                this@MainActivity,
                                "No expenses available to export",
                                Toast.LENGTH_SHORT
                            ).show()

                        } else {

                            withContext(Dispatchers.IO) {

                                val outputStream =
                                    contentResolver.openOutputStream(uri)
                                        ?: throw Exception(
                                            "Unable to create Excel file."
                                        )

                                outputStream.use { output ->
                                    ExcelExpenseExporter.exportExpenses(
                                        expenses,
                                        output
                                    )
                                }
                            }

                            Toast.makeText(
                                this@MainActivity,
                                "Excel exported successfully!",
                                Toast.LENGTH_LONG
                            ).show()
                        }

                    } catch (exception: Exception) {

                        Toast.makeText(
                            this@MainActivity,
                            "Excel export failed: ${exception.localizedMessage ?: "Unknown error"}",
                            Toast.LENGTH_LONG
                        ).show()
                    }
                }
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        enableEdgeToEdge()
        setContentView(R.layout.activity_main)

        ViewCompat.setOnApplyWindowInsetsListener(
            findViewById(R.id.main)
        ) { view, insets ->

            val systemBars =
                insets.getInsets(WindowInsetsCompat.Type.systemBars())

            view.setPadding(
                systemBars.left,
                systemBars.top,
                systemBars.right,
                systemBars.bottom
            )

            insets
        }

        receiptOcrHelper = ReceiptOcrHelper(this)

        val btnAddExpense =
            findViewById<Button>(R.id.btnAddExpense)

        val btnScanReceipt =
            findViewById<Button>(R.id.btnScanReceipt)

        val btnExportPdf =
            findViewById<Button>(R.id.btnExportPdf)

        val btnExportExcel =
            findViewById<Button>(R.id.btnExportExcel)

        btnAddExpense.setOnClickListener {
            startActivity(
                Intent(this, AddExpenseActivity::class.java)
            )
        }

        btnScanReceipt.setOnClickListener {
            pickReceipt.launch("image/*")
        }

        btnExportPdf.setOnClickListener {
            createPdfLauncher.launch("SmartExpenseReport.pdf")
        }

        btnExportExcel.setOnClickListener {
            createExcelLauncher.launch("SmartExpenseReport.xlsx")
        }

        tvTotalExpenses = findViewById(R.id.tvTotalExpenses)
        tvEmptyExpenses = findViewById(R.id.tvEmptyExpenses)
        rvExpenses = findViewById(R.id.rvExpenses)

        chartCategoryPie = findViewById(R.id.chartCategoryPie)
        chartCategoryBar = findViewById(R.id.chartCategoryBar)

        expenseAdapter = ExpenseAdapter()
        rvExpenses.layoutManager = LinearLayoutManager(this)
        rvExpenses.adapter = expenseAdapter

        val expenseDao =
            ExpenseDatabase.getDatabase(this).expenseDao()

        repository = ExpenseRepository(expenseDao)

        setupCharts()
        observeExpenses()
        observeCategoryTotals()
    }

    // Premium Receipt Result Dialog
    private fun showReceiptResultDialog(
        extractedText: String,
        detectedAmount: String?
    ) {
        val navy = Color.rgb(23, 43, 77)
        val teal = Color.rgb(0, 150, 136)
        val lightBackground = Color.rgb(243, 247, 250)
        val darkText = Color.rgb(42, 54, 72)
        val secondaryText = Color.rgb(115, 128, 145)

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(22), dp(22), dp(22), dp(20))
            background = roundedBackground(Color.WHITE, 22)
        }

        val header = TextView(this).apply {
            text = "Receipt scanned successfully"
            setTextColor(navy)
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 20f)
            setTypeface(null, Typeface.BOLD)
        }

        root.addView(
            header,
            LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
        )

        val subtitle = TextView(this).apply {
            text = "Review the extracted details before continuing."
            setTextColor(secondaryText)
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 13f)
            setPadding(0, dp(7), 0, dp(18))
        }

        root.addView(subtitle)

        val amountCard = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setPadding(dp(16), dp(15), dp(16), dp(15))
            background = roundedBackground(
                Color.rgb(230, 247, 244),
                16
            )
        }

        val amountLabel = TextView(this).apply {
            text = "DETECTED TOTAL AMOUNT"
            setTextColor(secondaryText)
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 11f)
            setTypeface(null, Typeface.BOLD)
            gravity = Gravity.CENTER
        }

        val amountValue = TextView(this).apply {
            text = if (detectedAmount != null) {
                "₹$detectedAmount"
            } else {
                "Amount not detected"
            }
            setTextColor(teal)
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 25f)
            setTypeface(null, Typeface.BOLD)
            gravity = Gravity.CENTER
            setPadding(0, dp(7), 0, 0)
        }

        amountCard.addView(amountLabel)
        amountCard.addView(amountValue)

        root.addView(
            amountCard,
            LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply {
                bottomMargin = dp(18)
            }
        )

        val detailsTitle = TextView(this).apply {
            text = "EXTRACTED RECEIPT DETAILS"
            setTextColor(navy)
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 12f)
            setTypeface(null, Typeface.BOLD)
            setPadding(0, 0, 0, dp(9))
        }

        root.addView(detailsTitle)

        val receiptScroll = ScrollView(this).apply {
            isFillViewport = true
            background = roundedBackground(lightBackground, 12)
        }

        val receiptDetails = TextView(this).apply {
            text = if (extractedText.isNotBlank()) {
                extractedText
            } else {
                "No readable text found in this receipt."
            }
            setTextColor(darkText)
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 13f)
            setPadding(dp(13), dp(13), dp(13), dp(13))
            setLineSpacing(dp(3).toFloat(), 1f)
        }

        receiptScroll.addView(receiptDetails)

        root.addView(
            receiptScroll,
            LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                dp(155)
            ).apply {
                bottomMargin = dp(20)
            }
        )

        val buttonRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER
        }

        val cancelButton = Button(this).apply {
            text = "Cancel"
            isAllCaps = false
            textSize = 14f
            setTextColor(navy)
            background = roundedBackground(
                Color.rgb(235, 239, 245),
                12
            )
        }

        val continueButton = Button(this).apply {
            text = "Continue"
            isAllCaps = false
            textSize = 14f
            setTypeface(null, Typeface.BOLD)
            setTextColor(Color.WHITE)
            background = roundedBackground(teal, 12)
        }

        buttonRow.addView(
            cancelButton,
            LinearLayout.LayoutParams(0, dp(50), 1f).apply {
                marginEnd = dp(8)
            }
        )

        buttonRow.addView(
            continueButton,
            LinearLayout.LayoutParams(0, dp(50), 1f).apply {
                marginStart = dp(8)
            }
        )

        root.addView(buttonRow)

        val dialog = AlertDialog.Builder(this)
            .setView(root)
            .create()

        cancelButton.setOnClickListener {
            dialog.dismiss()
        }

        continueButton.setOnClickListener {
            val intent = Intent(
                this,
                AddExpenseActivity::class.java
            )

            intent.putExtra("EXTRA_AMOUNT", detectedAmount)
            intent.putExtra("EXTRA_RECEIPT_TEXT", extractedText)

            startActivity(intent)
            dialog.dismiss()
        }

        dialog.show()

        dialog.window?.apply {
            setBackgroundDrawable(
                ColorDrawable(Color.TRANSPARENT)
            )
            setLayout(
                (resources.displayMetrics.widthPixels * 0.92).toInt(),
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
        }
    }

    private fun dp(value: Int): Int {
        return (value * resources.displayMetrics.density).toInt()
    }

    private fun roundedBackground(
        color: Int,
        radius: Int
    ): GradientDrawable {
        return GradientDrawable().apply {
            setColor(color)
            cornerRadius = dp(radius).toFloat()
        }
    }

    // Chart Setup
    private fun setupCharts() {

        chartCategoryPie.apply {
            description.isEnabled = false
            isDrawHoleEnabled = true
            holeRadius = 45f
            setCenterText("Spending")
            setCenterTextSize(14f)
            legend.isEnabled = true
            setNoDataText("Add expenses to see your chart")
        }

        chartCategoryBar.apply {
            description.isEnabled = false
            axisRight.isEnabled = false
            legend.isEnabled = false
            xAxis.granularity = 1f
            xAxis.setDrawGridLines(false)
            axisLeft.axisMinimum = 0f
            setNoDataText("Add expenses to see your chart")
        }
    }

    // Observe Expenses
    private fun observeExpenses() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {

                combine(
                    repository.allExpenses,
                    repository.totalExpenses
                ) { expenses, total ->
                    Pair(expenses, total ?: 0.0)
                }.collect { (expenses, total) ->

                    expenseAdapter.updateExpenses(expenses)

                    tvTotalExpenses.text = String.format(
                        Locale.getDefault(),
                        "₹%.2f",
                        total
                    )

                    if (expenses.isEmpty()) {
                        tvEmptyExpenses.visibility = View.VISIBLE
                        rvExpenses.visibility = View.GONE
                    } else {
                        tvEmptyExpenses.visibility = View.GONE
                        rvExpenses.visibility = View.VISIBLE
                    }
                }
            }
        }
    }

    // Observe Category Totals
    private fun observeCategoryTotals() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                repository.categoryTotals.collect { categoryTotals ->
                    updateCharts(categoryTotals)
                }
            }
        }
    }

    // Update Pie and Bar Charts
    private fun updateCharts(categoryTotals: List<CategoryTotal>) {

        if (categoryTotals.isEmpty()) {
            chartCategoryPie.clear()
            chartCategoryPie.setNoDataText(
                "Add expenses to see your chart"
            )
            chartCategoryPie.invalidate()

            chartCategoryBar.clear()
            chartCategoryBar.setNoDataText(
                "Add expenses to see your chart"
            )
            chartCategoryBar.invalidate()

            return
        }

        val pieEntries = categoryTotals.map {
            PieEntry(it.total.toFloat(), it.category)
        }

        val pieDataSet = PieDataSet(pieEntries, "Categories")
        pieDataSet.colors = ColorTemplate.MATERIAL_COLORS.toList()
        pieDataSet.valueTextSize = 12f
        pieDataSet.valueTextColor = Color.BLACK

        chartCategoryPie.data = PieData(pieDataSet)
        chartCategoryPie.setCenterText("Spending")
        chartCategoryPie.invalidate()

        val barEntries = categoryTotals.mapIndexed { index, item ->
            BarEntry(index.toFloat(), item.total.toFloat())
        }

        val barDataSet = BarDataSet(barEntries, "Amount (₹)")
        barDataSet.colors = ColorTemplate.MATERIAL_COLORS.toList()
        barDataSet.valueTextSize = 11f
        barDataSet.valueTextColor = Color.BLACK

        val barData = BarData(barDataSet)
        barData.barWidth = 0.6f

        chartCategoryBar.data = barData

        chartCategoryBar.xAxis.valueFormatter =
            IndexAxisValueFormatter(
                categoryTotals.map { it.category }
            )

        chartCategoryBar.xAxis.labelRotationAngle = -35f
        chartCategoryBar.invalidate()
    }

    // Generate and Save PDF
    private suspend fun exportExpensesToPdf(
        uri: Uri,
        expenses: List<Expense>
    ) {
        try {
            withContext(Dispatchers.IO) {

                val pdfDocument = PdfDocument()

                try {
                    val pageWidth = 595
                    val pageHeight = 842

                    val paint = Paint().apply {
                        color = Color.BLACK
                        textSize = 12f
                        isAntiAlias = true
                    }

                    val titlePaint = Paint().apply {
                        color = Color.rgb(38, 58, 117)
                        textSize = 22f
                        isFakeBoldText = true
                        isAntiAlias = true
                    }

                    val total = expenses.sumOf { it.amount }

                    val dateFormat = SimpleDateFormat(
                        "dd MMM yyyy",
                        Locale.getDefault()
                    )

                    var pageNumber = 1

                    var page = pdfDocument.startPage(
                        PdfDocument.PageInfo.Builder(
                            pageWidth,
                            pageHeight,
                            pageNumber
                        ).create()
                    )

                    var canvas = page.canvas
                    var y = 55f

                    canvas.drawText(
                        "Smart Expense Tracker",
                        40f,
                        y,
                        titlePaint
                    )

                    y += 35f

                    canvas.drawText(
                        "Total Expenses: ₹${String.format(Locale.getDefault(), "%.2f", total)}",
                        40f,
                        y,
                        paint
                    )

                    y += 30f

                    canvas.drawText(
                        "Expense Details",
                        40f,
                        y,
                        titlePaint
                    )

                    y += 30f

                    for ((index, expense) in expenses.withIndex()) {

                        if (y > 770f) {
                            pdfDocument.finishPage(page)
                            pageNumber++

                            page = pdfDocument.startPage(
                                PdfDocument.PageInfo.Builder(
                                    pageWidth,
                                    pageHeight,
                                    pageNumber
                                ).create()
                            )

                            canvas = page.canvas
                            y = 55f

                            canvas.drawText(
                                "Smart Expense Tracker - Continued",
                                40f,
                                y,
                                titlePaint
                            )

                            y += 40f
                        }

                        canvas.drawText(
                            "${index + 1}. ${expense.title.take(55)}",
                            40f,
                            y,
                            paint
                        )

                        y += 20f

                        canvas.drawText(
                            "Amount: ₹${String.format(Locale.getDefault(), "%.2f", expense.amount)}",
                            55f,
                            y,
                            paint
                        )

                        y += 18f

                        canvas.drawText(
                            "Category: ${expense.category.take(55)}",
                            55f,
                            y,
                            paint
                        )

                        y += 18f

                        canvas.drawText(
                            "Date: ${dateFormat.format(Date(expense.date))}",
                            55f,
                            y,
                            paint
                        )

                        y += 18f

                        canvas.drawText(
                            "Payment: ${expense.paymentMethod.take(45)}",
                            55f,
                            y,
                            paint
                        )

                        y += 18f

                        if (expense.note.isNotBlank()) {
                            canvas.drawText(
                                "Note: ${expense.note.take(65)}",
                                55f,
                                y,
                                paint
                            )
                            y += 18f
                        }

                        y += 12f
                    }

                    pdfDocument.finishPage(page)

                    val outputStream =
                        contentResolver.openOutputStream(uri)
                            ?: throw Exception("Unable to open PDF file")

                    outputStream.use {
                        pdfDocument.writeTo(it)
                    }

                } finally {
                    pdfDocument.close()
                }
            }

            Toast.makeText(
                this,
                "PDF exported successfully!",
                Toast.LENGTH_LONG
            ).show()

        } catch (exception: Exception) {
            Toast.makeText(
                this,
                "PDF export failed: ${exception.localizedMessage}",
                Toast.LENGTH_LONG
            ).show()
        }
    }

    // Receipt Amount Detection
    private fun extractReceiptAmount(receiptText: String): String? {

        val lines = receiptText.lines()

        val amountPattern = Regex(
            """(?:₹|rs\.?|inr)?\s*([0-9][0-9,]*(?:\.\d{1,2})?)""",
            RegexOption.IGNORE_CASE
        )

        val priorityLabels = listOf(
            "grand total",
            "amount payable",
            "net payable",
            "total amount",
            "amount paid",
            "balance due",
            "total"
        )

        for (label in priorityLabels) {

            val matchingLines = lines.filter { line ->
                if (label == "total") {
                    Regex("""\btotal\b""", RegexOption.IGNORE_CASE)
                        .containsMatchIn(line) &&
                            !line.contains("subtotal", ignoreCase = true)
                } else {
                    line.contains(label, ignoreCase = true)
                }
            }

            for (line in matchingLines) {

                val matches = amountPattern.findAll(line).toList()

                val amountText = matches.lastOrNull()
                    ?.groupValues
                    ?.get(1)
                    ?.replace(",", "")

                val amount = amountText?.toDoubleOrNull()

                if (amount != null && amount > 0) {
                    return amountText
                }
            }
        }

        val standaloneAmounts = lines.mapNotNull { line ->

            val trimmedLine = line.trim()

            val match = Regex(
                """^[^\d]*([0-9][0-9,]*(?:\.\d{1,2})?)[^\d]*$"""
            ).find(trimmedLine)

            val amountText = match?.groupValues?.get(1)
                ?.replace(",", "")

            val amount = amountText?.toDoubleOrNull()

            if (amount != null && amount > 0) {
                amountText
            } else {
                null
            }
        }

        return standaloneAmounts.lastOrNull()
    }

    override fun onDestroy() {
        super.onDestroy()

        if (::receiptOcrHelper.isInitialized) {
            receiptOcrHelper.close()
        }
    }
}
