package com.example.smartexpensetracker.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface ExpenseDao {

    // Add a new expense
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertExpense(expense: Expense)

    // Import multiple expenses from Excel
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertExpenses(expenses: List<Expense>)

    // Update an existing expense
    @Update
    suspend fun updateExpense(expense: Expense)

    // Delete an expense
    @Delete
    suspend fun deleteExpense(expense: Expense)

    // Get all expenses
    @Query("SELECT * FROM expenses ORDER BY date DESC")
    fun getAllExpenses(): Flow<List<Expense>>

    // Get total expense amount
    @Query("SELECT SUM(amount) FROM expenses")
    fun getTotalExpenses(): Flow<Double?>

    // Get expenses by category
    @Query("SELECT * FROM expenses WHERE category = :category ORDER BY date DESC")
    fun getExpensesByCategory(category: String): Flow<List<Expense>>

    // Get expenses by category totals
    @Query("SELECT category, SUM(amount) AS total FROM expenses GROUP BY category")
    fun getCategoryTotals(): Flow<List<CategoryTotal>>
}
