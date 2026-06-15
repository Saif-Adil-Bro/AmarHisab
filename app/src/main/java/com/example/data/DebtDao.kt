package com.example.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface DebtDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDebt(debt: Debt): Long

    @Update
    suspend fun updateDebt(debt: Debt)

    @Delete
    suspend fun deleteDebt(debt: Debt)

    @Query("SELECT * FROM debts WHERE profileId = :profileId ORDER BY date DESC")
    fun getAllDebtsByProfile(profileId: Long): Flow<List<Debt>>

    @Query("SELECT * FROM debts WHERE profileId = :profileId AND type = :type ORDER BY date DESC")
    fun getDebtsByType(profileId: Long, type: String): Flow<List<Debt>>

    @Query("SELECT SUM(amount) FROM debts WHERE profileId = :profileId AND type = 'borrowed'")
    fun getTotalBorrowed(profileId: Long): Flow<Double?>

    @Query("SELECT SUM(amount) FROM debts WHERE profileId = :profileId AND type = 'lent'")
    fun getTotalLent(profileId: Long): Flow<Double?>

    @Query("SELECT * FROM debts WHERE profileId = :profileId AND status != 'paid' ORDER BY date DESC")
    fun getUnpaidDebts(profileId: Long): Flow<List<Debt>>

    @Query("SELECT * FROM debts WHERE id = :id")
    suspend fun getDebtById(id: Long): Debt?

    @Query("SELECT DISTINCT personName FROM debts WHERE profileId = :profileId")
    fun getDistinctDebtPersonNames(profileId: Long): Flow<List<String>>
}
