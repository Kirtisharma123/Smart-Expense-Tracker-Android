package com.example.smartexpensetracker

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.smartexpensetracker.data.Expense
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class ExpenseAdapter : RecyclerView.Adapter<ExpenseAdapter.ExpenseViewHolder>() {

    private var expenseList = listOf<Expense>()

    class ExpenseViewHolder(itemView: View) :
        RecyclerView.ViewHolder(itemView) {

        val tvExpenseTitle: TextView =
            itemView.findViewById(R.id.tvExpenseTitle)

        val tvExpenseCategory: TextView =
            itemView.findViewById(R.id.tvExpenseCategory)

        val tvExpenseDate: TextView =
            itemView.findViewById(R.id.tvExpenseDate)

        val tvExpenseAmount: TextView =
            itemView.findViewById(R.id.tvExpenseAmount)
    }

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): ExpenseViewHolder {

        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_expense, parent, false)

        return ExpenseViewHolder(view)
    }

    override fun onBindViewHolder(
        holder: ExpenseViewHolder,
        position: Int
    ) {

        val expense = expenseList[position]

        holder.tvExpenseTitle.text = expense.title

        holder.tvExpenseCategory.text =
            "${expense.category} • ${expense.paymentMethod}"

        val dateFormat = SimpleDateFormat(
            "dd MMM yyyy",
            Locale.getDefault()
        )

        holder.tvExpenseDate.text =
            dateFormat.format(Date(expense.date))

        holder.tvExpenseAmount.text =
            "₹${String.format(Locale.getDefault(), "%.2f", expense.amount)}"
    }

    override fun getItemCount(): Int {
        return expenseList.size
    }

    fun updateExpenses(newExpenses: List<Expense>) {
        expenseList = newExpenses
        notifyDataSetChanged()
    }
}
