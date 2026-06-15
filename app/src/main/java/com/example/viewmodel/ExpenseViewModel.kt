package com.example.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.*
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.Calendar

class ExpenseViewModel(application: Application) : AndroidViewModel(application) {
    private val repository: ExpenseRepository
    private val userPrefs: UserPreferencesRepository

    init {
        val database = AppDatabase.getDatabase(application)
        repository = ExpenseRepository(database)
        userPrefs = UserPreferencesRepository(application)
        processRecurringExpensesSync()
    }

    private val _activeProfileId = MutableStateFlow<Long?>(null)
    val activeProfileId: StateFlow<Long?> = _activeProfileId.asStateFlow()

    // Settings DataStore preferences
    val defaultProfileId: StateFlow<Long?> = userPrefs.defaultProfileIdFlow
        .stateIn(viewModelScope, SharingStarted.Eagerly, null)

    val defaultCurrency: StateFlow<String> = userPrefs.defaultCurrencyFlow
        .stateIn(viewModelScope, SharingStarted.Eagerly, "৳")

    val themePreference: StateFlow<String> = userPrefs.themePreferenceFlow
        .stateIn(viewModelScope, SharingStarted.Eagerly, "System")

    val appLanguage: StateFlow<String> = userPrefs.appLanguageFlow
        .stateIn(viewModelScope, SharingStarted.Eagerly, "Bangla")

    // Expose all profiles. Auto-populate if empty.
    val allProfiles: StateFlow<List<ProfileEntity>> = repository.allProfiles
        .onEach { list ->
            if (list.isEmpty()) {
                insertInitialProfiles()
            } else if (_activeProfileId.value == null) {
                val savedId = defaultProfileId.value
                val profileToSelect = if (savedId != null) {
                    list.find { it.id == savedId }
                } else null
                val finalSelection = profileToSelect ?: list.find { it.isDefault } ?: list.first()
                _activeProfileId.value = finalSelection.id
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Expose all categories. Seeding defaults if empty.
    val allCategories: StateFlow<List<CategoryEntity>> = repository.allCategories
        .onEach { list ->
            if (list.isEmpty()) {
                repository.deleteAllCategoriesAndRecreateDefaults()
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun insertCategory(name: String, iconEmoji: String, colorHex: String) {
        viewModelScope.launch {
            repository.insertCategory(
                CategoryEntity(
                    name = name.trim(),
                    iconEmoji = iconEmoji,
                    colorHex = colorHex,
                    isDefault = false
                )
            )
        }
    }

    fun updateCategory(category: CategoryEntity) {
        viewModelScope.launch {
            repository.updateCategory(category)
        }
    }

    fun deleteCategory(category: CategoryEntity) {
        viewModelScope.launch {
            repository.deleteCategory(category)
        }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    val activeProfile: StateFlow<ProfileEntity?> = _activeProfileId
        .flatMapLatest { id ->
            if (id == null) {
                flowOf(null)
            } else {
                repository.allProfiles.map { list -> list.find { it.id == id } }
            }
        }
        .distinctUntilChanged()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    @OptIn(ExperimentalCoroutinesApi::class)
    val allExpenses: StateFlow<List<ExpenseEntity>> = _activeProfileId
        .flatMapLatest { id ->
            if (id == null) flowOf(emptyList())
            else repository.getAllExpenses(id)
        }
        .distinctUntilChanged()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    @OptIn(ExperimentalCoroutinesApi::class)
    val distinctItemNames: StateFlow<List<String>> = _activeProfileId
        .flatMapLatest { id ->
            if (id == null) flowOf(emptyList())
            else repository.getDistinctItemNames(id)
        }
        .distinctUntilChanged()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    @OptIn(ExperimentalCoroutinesApi::class)
    val shoppingList: StateFlow<List<ShoppingListItemEntity>> = _activeProfileId
        .flatMapLatest { id ->
            if (id == null) flowOf(emptyList())
            else repository.getAllShoppingItems(id)
        }
        .distinctUntilChanged()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private fun convertToDefaultCurrency(price: Double, currency: String): Double {
        return when (currency) {
            "৳" -> price
            "$" -> price * 120.0
            "£" -> price * 150.0
            "₹" -> price * 1.4
            else -> price
        }
    }

    // Dynamic aggregated totals based on active profile expenses
    val dailyTotal: StateFlow<Double> = allExpenses
        .map { list ->
            val (start, end) = getStartAndEndOfToday()
            list.filter { it.date in start..end }
                .sumOf { convertToDefaultCurrency(it.price, it.currency) }
        }
        .flowOn(Dispatchers.Default)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    val weeklyTotal: StateFlow<Double> = allExpenses
        .map { list ->
            val (start, end) = getStartAndEndOfWeek()
            list.filter { it.date in start..end }
                .sumOf { convertToDefaultCurrency(it.price, it.currency) }
        }
        .flowOn(Dispatchers.Default)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    val monthlyTotal: StateFlow<Double> = allExpenses
        .map { list ->
            val (start, end) = getStartAndEndOfMonth()
            list.filter { it.date in start..end }
                .sumOf { convertToDefaultCurrency(it.price, it.currency) }
        }
        .flowOn(Dispatchers.Default)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    val budgetUsagePercentage: StateFlow<Double> = combine(monthlyTotal, activeProfile) { total, profile ->
        val budget = profile?.monthlyBudget ?: 0.0
        calculateBudgetUsagePercentage(total, budget)
    }
        .flowOn(Dispatchers.Default)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    fun calculateBudgetUsagePercentage(spent: Double, budget: Double): Double {
        return if (budget > 0.0) (spent / budget) * 100.0 else 0.0
    }

    fun updateMonthlyBudget(budget: Double) {
        val currentProfile = activeProfile.value ?: return
        viewModelScope.launch {
            repository.updateProfile(currentProfile.copy(monthlyBudget = budget))
        }
    }

    // Profiles API
    fun selectProfile(profileId: Long) {
        _activeProfileId.value = profileId
    }

    // Save preferences
    fun saveDefaultProfileId(profileId: Long?) {
        viewModelScope.launch {
            userPrefs.saveDefaultProfileId(profileId)
        }
    }

    fun saveDefaultCurrency(currency: String) {
        viewModelScope.launch {
            userPrefs.saveDefaultCurrency(currency)
        }
    }

    fun saveThemePreference(theme: String) {
        viewModelScope.launch {
            userPrefs.saveThemePreference(theme)
        }
    }

    fun saveAppLanguage(language: String) {
        viewModelScope.launch {
            userPrefs.saveAppLanguage(language)
        }
    }

    fun insertProfile(name: String, iconResId: Int, colorHex: String) {
        viewModelScope.launch {
            repository.insertProfile(
                ProfileEntity(
                    name = name,
                    iconResId = iconResId,
                    colorHex = colorHex,
                    isDefault = false
                )
            )
        }
    }

    private fun insertInitialProfiles() {
        viewModelScope.launch {
            val personalId = repository.insertProfile(
                ProfileEntity(
                    name = "আমার পকেট",
                    iconResId = 0,
                    colorHex = "#6750A4",
                    isDefault = true
                )
            )
            repository.insertProfile(
                ProfileEntity(
                    name = "সংসার বাজার",
                    iconResId = 1,
                    colorHex = "#3F51B5",
                    isDefault = false
                )
            )
            repository.insertProfile(
                ProfileEntity(
                    name = "অফিস প্যান্ট্রি",
                    iconResId = 2,
                    colorHex = "#E91E63",
                    isDefault = false
                )
            )
            _activeProfileId.value = personalId
        }
    }

    // Expense items mutation API
    fun insertExpense(itemName: String, price: Double, currency: String, category: String, date: Long) {
        val pid = _activeProfileId.value ?: return
        viewModelScope.launch {
            repository.insertExpense(
                ExpenseEntity(
                    profileId = pid,
                    itemName = itemName.trim(),
                    price = price,
                    currency = currency,
                    category = category,
                    date = date
                )
            )
        }
    }

    fun updateExpense(id: Long, itemName: String, price: Double, currency: String, category: String, date: Long) {
        val pid = _activeProfileId.value ?: return
        viewModelScope.launch {
            repository.updateExpense(
                ExpenseEntity(
                    id = id,
                    profileId = pid,
                    itemName = itemName.trim(),
                    price = price,
                    currency = currency,
                    category = category,
                    date = date
                )
            )
        }
    }

    fun deleteProfile(profile: ProfileEntity) {
        viewModelScope.launch {
            // If the deleted profile is the active profile, switch active profile to another one
            if (_activeProfileId.value == profile.id) {
                val otherProfile = allProfiles.value.firstOrNull { it.id != profile.id }
                if (otherProfile != null) {
                    _activeProfileId.value = otherProfile.id
                }
            }
            // If it is the default launch profile, clear it
            if (defaultProfileId.value == profile.id) {
                userPrefs.saveDefaultProfileId(null)
            }
            repository.deleteProfile(profile)
        }
    }

    fun deleteExpense(expense: ExpenseEntity) {
        viewModelScope.launch {
            repository.deleteExpense(expense)
        }
    }

    // Shopping List items mutation API
    fun insertShoppingItem(itemName: String, quantity: Double, unit: String, estimatedPrice: Double) {
        val pid = _activeProfileId.value ?: return
        viewModelScope.launch {
            repository.insertShoppingItem(
                ShoppingListItemEntity(
                    profileId = pid,
                    itemName = itemName.trim(),
                    quantity = quantity,
                    unit = unit,
                    estimatedPrice = estimatedPrice,
                    isPurchased = false
                )
            )
        }
    }

    fun toggleShoppingItemPurchased(item: ShoppingListItemEntity, actualPrice: Double?) {
        viewModelScope.launch {
            val updated = item.copy(isPurchased = !item.isPurchased)
            repository.updateShoppingItem(updated)

            if (updated.isPurchased && actualPrice != null) {
                // Instantly convert to an active profile expense
                repository.insertExpense(
                    ExpenseEntity(
                        profileId = item.profileId,
                        itemName = item.itemName,
                        price = actualPrice,
                        currency = "৳",
                        category = "Grocery",
                        date = System.currentTimeMillis()
                    )
                )
            }
        }
    }

    fun deleteShoppingItem(item: ShoppingListItemEntity) {
        viewModelScope.launch {
            repository.deleteShoppingItem(item)
        }
    }

    fun clearAllData() {
        viewModelScope.launch {
            repository.clearAllData()
            _activeProfileId.value = null
        }
    }

    // Time calculations helper
    private fun getStartAndEndOfToday(): Pair<Long, Long> {
        val calendar = Calendar.getInstance()
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        val start = calendar.timeInMillis

        calendar.set(Calendar.HOUR_OF_DAY, 23)
        calendar.set(Calendar.MINUTE, 59)
        calendar.set(Calendar.SECOND, 59)
        calendar.set(Calendar.MILLISECOND, 999)
        val end = calendar.timeInMillis

        return Pair(start, end)
    }

    private fun getStartAndEndOfWeek(): Pair<Long, Long> {
        val calendar = Calendar.getInstance()
        calendar.set(Calendar.DAY_OF_WEEK, calendar.firstDayOfWeek)
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        val start = calendar.timeInMillis

        calendar.add(Calendar.DAY_OF_WEEK, 6)
        calendar.set(Calendar.HOUR_OF_DAY, 23)
        calendar.set(Calendar.MINUTE, 59)
        calendar.set(Calendar.SECOND, 59)
        calendar.set(Calendar.MILLISECOND, 999)
        val end = calendar.timeInMillis

        return Pair(start, end)
    }

    private fun getStartAndEndOfMonth(): Pair<Long, Long> {
        val calendar = Calendar.getInstance()
        calendar.set(Calendar.DAY_OF_MONTH, 1)
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        val start = calendar.timeInMillis

        calendar.set(Calendar.DAY_OF_MONTH, calendar.getActualMaximum(Calendar.DAY_OF_MONTH))
        calendar.set(Calendar.HOUR_OF_DAY, 23)
        calendar.set(Calendar.MINUTE, 59)
        calendar.set(Calendar.SECOND, 59)
        calendar.set(Calendar.MILLISECOND, 999)
        val end = calendar.timeInMillis

        return Pair(start, end)
    }

    // Recurring Expenses
    @OptIn(ExperimentalCoroutinesApi::class)
    val allRecurringExpenses: StateFlow<List<RecurringExpenseEntity>> = _activeProfileId
        .flatMapLatest { id ->
            if (id == null) flowOf(emptyList())
            else repository.getAllRecurringExpenses(id)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun insertRecurringExpense(itemName: String, amount: Double, category: String, frequency: String, nextDueDate: Long) {
        val pid = _activeProfileId.value ?: return
        viewModelScope.launch {
            repository.insertRecurringExpense(
                RecurringExpenseEntity(
                    profileId = pid,
                    itemName = itemName.trim(),
                    amount = amount,
                    category = category,
                    frequency = frequency,
                    nextDueDate = nextDueDate,
                    isActive = true
                )
            )
        }
    }

    fun updateRecurringExpense(expense: RecurringExpenseEntity) {
        viewModelScope.launch {
            repository.updateRecurringExpense(expense)
        }
    }

    fun deleteRecurringExpense(expense: RecurringExpenseEntity) {
        viewModelScope.launch {
            repository.deleteRecurringExpense(expense)
        }
    }

    fun toggleRecurringExpenseActive(expense: RecurringExpenseEntity) {
        viewModelScope.launch {
            repository.updateRecurringExpense(expense.copy(isActive = !expense.isActive))
        }
    }

    private fun processRecurringExpensesSync() {
        viewModelScope.launch {
            try {
                val activeSec = repository.getActiveRecurringExpensesDirect()
                val now = System.currentTimeMillis()
                for (item in activeSec) {
                    if (item.nextDueDate <= now) {
                        var updatedDueDate = item.nextDueDate
                        while (updatedDueDate <= now) {
                            repository.insertExpense(
                                ExpenseEntity(
                                    profileId = item.profileId,
                                    itemName = item.itemName,
                                    price = item.amount,
                                    currency = "৳",
                                    category = item.category,
                                    date = updatedDueDate
                                )
                            )
                            val calendar = Calendar.getInstance().apply {
                                timeInMillis = updatedDueDate
                            }
                            when (item.frequency) {
                                "DAILY" -> calendar.add(Calendar.DAY_OF_YEAR, 1)
                                "WEEKLY" -> calendar.add(Calendar.WEEK_OF_YEAR, 1)
                                "MONTHLY" -> calendar.add(Calendar.MONTH, 1)
                                "YEARLY" -> calendar.add(Calendar.YEAR, 1)
                                else -> calendar.add(Calendar.DAY_OF_YEAR, 1)
                            }
                            updatedDueDate = calendar.timeInMillis
                        }
                        repository.updateRecurringExpense(item.copy(nextDueDate = updatedDueDate))
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    class Factory(private val application: Application) : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(ExpenseViewModel::class.java)) {
                @Suppress("UNCHECKED_CAST")
                return ExpenseViewModel(application) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class")
        }
    }
}
