package com.example.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface RecurringExpenseDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRecurringExpense(expense: RecurringExpenseEntity): Long

    @Update
    suspend fun updateRecurringExpense(expense: RecurringExpenseEntity)

    @Delete
    suspend fun deleteRecurringExpense(expense: RecurringExpenseEntity)

    @Query("SELECT * FROM recurring_expenses WHERE profileId = :profileId ORDER BY id DESC")
    fun getAllRecurringExpenses(profileId: Long): Flow<List<RecurringExpenseEntity>>

    @Query("SELECT * FROM recurring_expenses WHERE isActive = 1")
    suspend fun getActiveRecurringExpensesDirect(): List<RecurringExpenseEntity>

    @Query("SELECT * FROM recurring_expenses WHERE id = :id LIMIT 1")
    suspend fun getRecurringExpenseById(id: Long): RecurringExpenseEntity?

    @Query("SELECT * FROM recurring_expenses")
    suspend fun getAllRecurringExpensesDirect(): List<RecurringExpenseEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRecurringExpenses(expenses: List<RecurringExpenseEntity>)

    @Query("DELETE FROM recurring_expenses")
    suspend fun deleteAllRecurringExpenses()
}
