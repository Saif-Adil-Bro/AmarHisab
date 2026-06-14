package com.example.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

/**
 * Data Access Object for Expenses table.
 * Supports insert, update, remove operations as well as time-filtered aggregated queries.
 */
@Dao
interface ExpenseDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertExpense(expense: ExpenseEntity): Long

    @Update
    suspend fun updateExpense(expense: ExpenseEntity)

    @Delete
    suspend fun deleteExpense(expense: ExpenseEntity)

    /**
     * Emits expenses belonging to a specific profile, sorted from newest to oldest.
     */
    @Query("SELECT * FROM expenses WHERE profileId = :profileId ORDER BY date DESC, id DESC")
    fun getExpensesByProfile(profileId: Long): Flow<List<ExpenseEntity>>

    /**
     * Alias method pointing to chronological profile expenses.
     */
    @Query("SELECT * FROM expenses WHERE profileId = :profileId ORDER BY date DESC, id DESC")
    fun getAllExpenses(profileId: Long): Flow<List<ExpenseEntity>>

    /**
     * Returns a distinct list of purchased item names for auto-complete suggestions.
     */
    @Query("SELECT DISTINCT itemName FROM expenses WHERE profileId = :profileId ORDER BY itemName ASC")
    fun getDistinctItemNames(profileId: Long): Flow<List<String>>

    /**
     * Calculates Today's aggregated price sum asynchronously via Flow.
     */
    @Query("SELECT SUM(price) FROM expenses WHERE profileId = :profileId AND date >= :startOfDay AND date <= :endOfDay")
    fun getDailyTotal(profileId: Long, startOfDay: Long, endOfDay: Long): Flow<Double?>

    /**
     * Calculates Today's aggregated price sum synchronously/suspend.
     */
    @Query("SELECT SUM(price) FROM expenses WHERE profileId = :profileId AND date >= :startOfDay AND date <= :endOfDay")
    suspend fun getTodayTotal(profileId: Long, startOfDay: Long, endOfDay: Long): Double?

    /**
     * Calculates Weekly aggregated price sum asynchronously via Flow.
     */
    @Query("SELECT SUM(price) FROM expenses WHERE profileId = :profileId AND date >= :startOfWeek AND date <= :endOfWeek")
    fun getWeeklyTotal(profileId: Long, startOfWeek: Long, endOfWeek: Long): Flow<Double?>

    /**
     * Calculates Weekly aggregated price sum synchronously/suspend.
     */
    @Query("SELECT SUM(price) FROM expenses WHERE profileId = :profileId AND date >= :startOfWeek AND date <= :endOfWeek")
    suspend fun getWeekTotal(profileId: Long, startOfWeek: Long, endOfWeek: Long): Double?

    /**
     * Calculates Monthly aggregated price sum asynchronously via Flow.
     */
    @Query("SELECT SUM(price) FROM expenses WHERE profileId = :profileId AND date >= :startOfMonth AND date <= :endOfMonth")
    fun getMonthlyTotal(profileId: Long, startOfMonth: Long, endOfMonth: Long): Flow<Double?>

    /**
     * Calculates Monthly aggregated price sum synchronously/suspend.
     */
    @Query("SELECT SUM(price) FROM expenses WHERE profileId = :profileId AND date >= :startOfMonth AND date <= :endOfMonth")
    suspend fun getMonthTotal(profileId: Long, startOfMonth: Long, endOfMonth: Long): Double?

    @Query("SELECT * FROM expenses WHERE id = :id")
    suspend fun getExpenseById(id: Long): ExpenseEntity?

    /**
     * Retrieves all expenses directly.
     */
    @Query("SELECT * FROM expenses")
    suspend fun getAllExpensesDirect(): List<ExpenseEntity>

    /**
     * Deletes all expenses.
     */
    @Query("DELETE FROM expenses")
    suspend fun deleteAllExpenses()

    /**
     * Bulk inserts expenses.
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertExpenses(expenses: List<ExpenseEntity>)
}
