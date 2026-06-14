package com.example.data

import kotlinx.coroutines.flow.Flow

class ExpenseRepository(private val database: AppDatabase) {
    private val profileDao = database.profileDao()
    private val expenseDao = database.expenseDao()
    private val shoppingListDao = database.shoppingListDao()
    private val categoryDao = database.categoryDao()

    // Categories
    val allCategories: Flow<List<CategoryEntity>> = categoryDao.getAllCategories()

    suspend fun insertCategory(category: CategoryEntity): Long {
        return categoryDao.insertCategory(category)
    }

    suspend fun updateCategory(category: CategoryEntity) {
        categoryDao.updateCategory(category)
    }

    suspend fun deleteCategory(category: CategoryEntity) {
        categoryDao.deleteCategory(category)
    }

    suspend fun getCategoryByName(name: String): CategoryEntity? {
        return categoryDao.getCategoryByName(name)
    }

    suspend fun getAllCategoriesDirect(): List<CategoryEntity> = categoryDao.getAllCategoriesDirect()

    suspend fun insertCategories(categories: List<CategoryEntity>) {
        categoryDao.insertCategories(categories)
    }

    suspend fun deleteAllCategoriesAndRecreateDefaults() {
        categoryDao.deleteAllCategories()
        val defaults = listOf(
            CategoryEntity(name = "বাজার", iconEmoji = "🛒", colorHex = "#1565C0", isDefault = true),
            CategoryEntity(name = "শাকসবজি", iconEmoji = "🥬", colorHex = "#2E7D32", isDefault = true),
            CategoryEntity(name = "মাছ ও মাংস", iconEmoji = "🍗", colorHex = "#C62828", isDefault = true),
            CategoryEntity(name = "যাতায়াত", iconEmoji = "🚗", colorHex = "#ED6C02", isDefault = true),
            CategoryEntity(name = "বাসা ভাড়া", iconEmoji = "🏠", colorHex = "#4E342E", isDefault = true),
            CategoryEntity(name = "বিদ্যুৎ বিল", iconEmoji = "💡", colorHex = "#EF6C00", isDefault = true),
            CategoryEntity(name = "অন্যান্য", iconEmoji = "💰", colorHex = "#6A1B9A", isDefault = true)
        )
        categoryDao.insertCategories(defaults)
    }

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

    suspend fun getExpensesInDateRange(profileId: Long, startDate: Long, endDate: Long): List<ExpenseEntity> {
        return expenseDao.getExpensesInDateRange(profileId, startDate, endDate)
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

    // Recurring Expenses
    private val recurringExpenseDao = database.recurringExpenseDao()

    fun getAllRecurringExpenses(profileId: Long): Flow<List<RecurringExpenseEntity>> =
        recurringExpenseDao.getAllRecurringExpenses(profileId)

    suspend fun getActiveRecurringExpensesDirect(): List<RecurringExpenseEntity> =
        recurringExpenseDao.getActiveRecurringExpensesDirect()

    suspend fun getRecurringExpenseById(id: Long): RecurringExpenseEntity? =
        recurringExpenseDao.getRecurringExpenseById(id)

    suspend fun insertRecurringExpense(expense: RecurringExpenseEntity): Long =
        recurringExpenseDao.insertRecurringExpense(expense)

    suspend fun updateRecurringExpense(expense: RecurringExpenseEntity) {
        recurringExpenseDao.updateRecurringExpense(expense)
    }

    suspend fun deleteRecurringExpense(expense: RecurringExpenseEntity) {
        recurringExpenseDao.deleteRecurringExpense(expense)
    }

    // Backup & Restore Support
    suspend fun getAllProfilesDirect(): List<ProfileEntity> = profileDao.getAllProfilesDirect()
    suspend fun getAllExpensesDirect(): List<ExpenseEntity> = expenseDao.getAllExpensesDirect()
    suspend fun getAllShoppingItemsDirect(): List<ShoppingListItemEntity> = shoppingListDao.getAllShoppingItemsDirect()
    suspend fun getAllRecurringExpensesDirect(): List<RecurringExpenseEntity> = recurringExpenseDao.getAllRecurringExpensesDirect()

    suspend fun clearAllData() {
        shoppingListDao.deleteAllShoppingItems()
        expenseDao.deleteAllExpenses()
        recurringExpenseDao.deleteAllRecurringExpenses()
        categoryDao.deleteAllCategories()
        profileDao.deleteAllProfiles()
    }

    suspend fun restoreDatabase(
        profiles: List<ProfileEntity>,
        expenses: List<ExpenseEntity>,
        shoppingItems: List<ShoppingListItemEntity>,
        categories: List<CategoryEntity>,
        recurringExpenses: List<RecurringExpenseEntity>
    ) {
        // Delete dependent tables first, then profiles
        shoppingListDao.deleteAllShoppingItems()
        expenseDao.deleteAllExpenses()
        recurringExpenseDao.deleteAllRecurringExpenses()
        categoryDao.deleteAllCategories()
        profileDao.deleteAllProfiles()

        // Insert profiles first, then dependent records
        profileDao.insertProfiles(profiles)
        categoryDao.insertCategories(categories)
        profileDao.getAllProfilesDirect() // refresh/verify
        expenseDao.insertExpenses(expenses)
        shoppingListDao.insertShoppingItems(shoppingItems)
        recurringExpenseDao.insertRecurringExpenses(recurringExpenses)
    }
}
