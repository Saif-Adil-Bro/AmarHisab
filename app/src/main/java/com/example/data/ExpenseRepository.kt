package com.example.data

import kotlinx.coroutines.flow.Flow

class ExpenseRepository(private val database: AppDatabase) {
    private val profileDao = database.profileDao()
    private val expenseDao = database.expenseDao()
    private val shoppingListDao = database.shoppingListDao()

    // Profiles
    val allProfiles: Flow<List<ProfileEntity>> = profileDao.getAllProfiles()

    suspend fun insertProfile(profile: ProfileEntity): Long {
        return profileDao.insertProfile(profile)
    }

    suspend fun updateProfile(profile: ProfileEntity) {
        profileDao.updateProfile(profile)
    }

    suspend fun deleteProfile(profile: ProfileEntity) {
        profileDao.deleteProfile(profile)
    }

    suspend fun getDefaultProfile(): ProfileEntity? {
        return profileDao.getDefaultProfile()
    }

    suspend fun clearDefaultProfile() {
        profileDao.clearDefaultProfile()
    }

    // Expenses
    fun getAllExpenses(profileId: Long): Flow<List<ExpenseEntity>> =
        expenseDao.getAllExpenses(profileId)

    fun getExpensesByProfile(profileId: Long): Flow<List<ExpenseEntity>> =
        expenseDao.getExpensesByProfile(profileId)

    fun getDailyTotal(profileId: Long, startOfDay: Long, endOfDay: Long): Flow<Double?> =
        expenseDao.getDailyTotal(profileId, startOfDay, endOfDay)

    suspend fun getTodayTotal(profileId: Long, startOfDay: Long, endOfDay: Long): Double? {
        return expenseDao.getTodayTotal(profileId, startOfDay, endOfDay)
    }

    fun getWeeklyTotal(profileId: Long, startOfWeek: Long, endOfWeek: Long): Flow<Double?> =
        expenseDao.getWeeklyTotal(profileId, startOfWeek, endOfWeek)

    suspend fun getWeekTotal(profileId: Long, startOfWeek: Long, endOfWeek: Long): Double? {
        return expenseDao.getWeekTotal(profileId, startOfWeek, endOfWeek)
    }

    fun getMonthlyTotal(profileId: Long, startOfMonth: Long, endOfMonth: Long): Flow<Double?> =
        expenseDao.getMonthlyTotal(profileId, startOfMonth, endOfMonth)

    suspend fun getMonthTotal(profileId: Long, startOfMonth: Long, endOfMonth: Long): Double? {
        return expenseDao.getMonthTotal(profileId, startOfMonth, endOfMonth)
    }

    fun getDistinctItemNames(profileId: Long): Flow<List<String>> =
        expenseDao.getDistinctItemNames(profileId)

    suspend fun insertExpense(expense: ExpenseEntity): Long {
        return expenseDao.insertExpense(expense)
    }

    suspend fun updateExpense(expense: ExpenseEntity) {
        expenseDao.updateExpense(expense)
    }

    suspend fun deleteExpense(expense: ExpenseEntity) {
        expenseDao.deleteExpense(expense)
    }

    suspend fun getExpenseById(id: Long): ExpenseEntity? {
        return expenseDao.getExpenseById(id)
    }

    // Shopping List
    fun getAllShoppingItems(profileId: Long): Flow<List<ShoppingListItemEntity>> =
        shoppingListDao.getAllItems(profileId)

    fun getItemsByProfile(profileId: Long): Flow<List<ShoppingListItemEntity>> =
        shoppingListDao.getItemsByProfile(profileId)

    suspend fun updatePurchaseStatus(itemId: Long, isPurchased: Boolean) {
        shoppingListDao.updatePurchaseStatus(itemId, isPurchased)
    }

    suspend fun insertShoppingItem(item: ShoppingListItemEntity): Long {
        return shoppingListDao.insertItem(item)
    }

    suspend fun updateShoppingItem(item: ShoppingListItemEntity) {
        shoppingListDao.updateItem(item)
    }

    suspend fun deleteShoppingItem(item: ShoppingListItemEntity) {
        shoppingListDao.deleteItem(item)
    }

    suspend fun getShoppingItemById(id: Long): ShoppingListItemEntity? {
        return shoppingListDao.getItemById(id)
    }

    // Backup & Restore Support
    suspend fun getAllProfilesDirect(): List<ProfileEntity> = profileDao.getAllProfilesDirect()
    suspend fun getAllExpensesDirect(): List<ExpenseEntity> = expenseDao.getAllExpensesDirect()
    suspend fun getAllShoppingItemsDirect(): List<ShoppingListItemEntity> = shoppingListDao.getAllShoppingItemsDirect()

    suspend fun clearAllData() {
        shoppingListDao.deleteAllShoppingItems()
        expenseDao.deleteAllExpenses()
        profileDao.deleteAllProfiles()
    }

    suspend fun restoreDatabase(
        profiles: List<ProfileEntity>,
        expenses: List<ExpenseEntity>,
        shoppingItems: List<ShoppingListItemEntity>
    ) {
        // Delete dependent tables first, then profiles
        shoppingListDao.deleteAllShoppingItems()
        expenseDao.deleteAllExpenses()
        profileDao.deleteAllProfiles()

        // Insert profiles first, then dependent records
        profileDao.insertProfiles(profiles)
        expenseDao.insertExpenses(expenses)
        shoppingListDao.insertShoppingItems(shoppingItems)
    }
}
