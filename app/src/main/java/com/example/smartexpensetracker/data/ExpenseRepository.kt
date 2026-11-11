package com.example.smartexpensetracker.data

import kotlinx.coroutines.flow.Flow

class ExpenseRepository(
    private val expenseDao: ExpenseDao
) {

    // Get all expenses
    val allExpenses: Flow<List<Expense>> =
        expenseDao.getAllExpenses()

    // Get total expenses
    val totalExpenses: Flow<Double?> =
        expenseDao.getTotalExpenses()

    val categoryTotals: Flow<List<CategoryTotal>> =
        expenseDao.getCategoryTotals()

    // Add expense
    suspend fun insertExpense(expense: Expense) {
        expenseDao.insertExpense(expense)
    }

    // Update expense
    suspend fun updateExpense(expense: Expense) {
        expenseDao.updateExpense(expense)
    }

    // Delete expense
    suspend fun deleteExpense(expense: Expense) {
        expenseDao.deleteExpense(expense)
    }

    // Filter expenses by category
    fun getExpensesByCategory(category: String): Flow<List<Expense>> {
        return expenseDao.getExpensesByCategory(category)
    }
}
