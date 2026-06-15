package com.example.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

@OptIn(ExperimentalCoroutinesApi::class)
class DebtViewModel(application: Application) : AndroidViewModel(application) {

    private val debtRepository: DebtRepository
    private val repository: ExpenseRepository
    private val userPrefs: UserPreferencesRepository

    init {
        val database = AppDatabase.getDatabase(application)
        debtRepository = DebtRepository(database)
        repository = ExpenseRepository(database)
        userPrefs = UserPreferencesRepository(application)
    }

    // List of profiles to fetch fallback selected context
    val allProfiles: Flow<List<ProfileEntity>> = repository.allProfiles

    // Selected language: we default to "Bangla"
    val appLanguage: StateFlow<String> = userPrefs.appLanguageFlow
        .stateIn(viewModelScope, SharingStarted.Eagerly, "Bangla")

    // Synchronize selected profile with central expense app preference
    val activeProfileId: StateFlow<Long?> = combine(
        userPrefs.defaultProfileIdFlow,
        repository.allProfiles
    ) { defaultId, profiles ->
        defaultId ?: profiles.firstOrNull()?.id
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val defaultCurrency: StateFlow<String> = userPrefs.defaultCurrencyFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "৳")

    // Reactive list of all debts matching active profile
    val allDebts: StateFlow<List<Debt>> = activeProfileId
        .flatMapLatest { profileId ->
            if (profileId != null) {
                debtRepository.getAllDebtsByProfile(profileId)
            } else {
                flowOf(emptyList())
            }
        }
        .flowOn(Dispatchers.IO)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Sum of borrowed money
    val totalBorrowed: StateFlow<Double> = activeProfileId
        .flatMapLatest { profileId ->
            if (profileId != null) {
                debtRepository.getTotalBorrowed(profileId)
            } else {
                flowOf(0.0)
            }
        }
        .map { it ?: 0.0 }
        .flowOn(Dispatchers.IO)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    // Sum of lent money
    val totalLent: StateFlow<Double> = activeProfileId
        .flatMapLatest { profileId ->
            if (profileId != null) {
                debtRepository.getTotalLent(profileId)
            } else {
                flowOf(0.0)
            }
        }
        .map { it ?: 0.0 }
        .flowOn(Dispatchers.IO)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    // Lent - Borrowed
    val netBalance: StateFlow<Double> = combine(totalLent, totalBorrowed) { lent, borrowed ->
        lent - borrowed
    }
    .flowOn(Dispatchers.IO)
    .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    // Suggestive history names
    val distinctPersonNames: StateFlow<List<String>> = activeProfileId
        .flatMapLatest { profileId ->
            if (profileId != null) {
                debtRepository.getDistinctDebtPersonNames(profileId)
            } else {
                flowOf(emptyList())
            }
        }
        .flowOn(Dispatchers.IO)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Insert new debt item asynchronously in background thread
    fun insertDebt(
        personName: String,
        amount: Double,
        type: String, // "borrowed" or "lent"
        dueDate: Long? = null,
        notes: String? = null,
        contactNumber: String? = null
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            val pId = activeProfileId.value ?: return@launch
            val debt = Debt(
                profileId = pId,
                personName = personName,
                amount = amount,
                paidAmount = 0.0,
                type = type,
                dueDate = dueDate,
                notes = notes,
                status = "unpaid",
                contactNumber = contactNumber
            )
            debtRepository.insertDebt(debt)
        }
    }

    // Save/update existing debt asynchronously
    fun updateDebt(debt: Debt) {
        viewModelScope.launch(Dispatchers.IO) {
            debtRepository.updateDebt(debt)
        }
    }

    // Delete debt asynchronously
    fun deleteDebt(debt: Debt) {
        viewModelScope.launch(Dispatchers.IO) {
            debtRepository.deleteDebt(debt)
        }
    }

    // Record payments asynchronously and recalculate status automatically
    fun recordPayment(debt: Debt, paymentAmount: Double) {
        viewModelScope.launch(Dispatchers.IO) {
            val totalPaidSoFar = debt.paidAmount + paymentAmount
            val finalPaidAmount = if (totalPaidSoFar > debt.amount) debt.amount else totalPaidSoFar
            val status = when {
                finalPaidAmount >= debt.amount -> "paid"
                finalPaidAmount > 0.0 -> "partial"
                else -> "unpaid"
            }
            val updatedDebt = debt.copy(
                paidAmount = finalPaidAmount,
                status = status
            )
            debtRepository.updateDebt(updatedDebt)
        }
    }

    suspend fun getDebtById(id: Long): Debt? {
        return debtRepository.getDebtById(id)
    }

    class Factory(private val application: Application) : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(DebtViewModel::class.java)) {
                @Suppress("UNCHECKED_CAST")
                return DebtViewModel(application) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class")
        }
    }
}
