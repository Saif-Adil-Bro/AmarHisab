package com.example.data

import kotlinx.coroutines.flow.Flow

class DebtRepository(private val database: AppDatabase) {
    private val debtDao = database.debtDao()

    fun getAllDebtsByProfile(profileId: Long): Flow<List<Debt>> {
        return debtDao.getAllDebtsByProfile(profileId)
    }

    fun getDebtsByType(profileId: Long, type: String): Flow<List<Debt>> {
        return debtDao.getDebtsByType(profileId, type)
    }

    fun getTotalBorrowed(profileId: Long): Flow<Double?> {
        return debtDao.getTotalBorrowed(profileId)
    }

    fun getTotalLent(profileId: Long): Flow<Double?> {
        return debtDao.getTotalLent(profileId)
    }

    fun getUnpaidDebts(profileId: Long): Flow<List<Debt>> {
        return debtDao.getUnpaidDebts(profileId)
    }

    fun getDistinctDebtPersonNames(profileId: Long): Flow<List<String>> {
        return debtDao.getDistinctDebtPersonNames(profileId)
    }

    suspend fun getDebtById(id: Long): Debt? {
        return debtDao.getDebtById(id)
    }

    suspend fun insertDebt(debt: Debt): Long {
        return debtDao.insertDebt(debt)
    }

    suspend fun updateDebt(debt: Debt) {
        debtDao.updateDebt(debt)
    }

    suspend fun deleteDebt(debt: Debt) {
        debtDao.deleteDebt(debt)
    }
}
