package com.example.data

import android.content.Context
import android.net.Uri
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * BackupManager manual back-up helper.
 * Serializes and deserializes application database tables into WCAG-compliant JSON format.
 */
class BackupManager(private val repository: ExpenseRepository) {

    /**
     * Serializes all tables (Profiles, Categories, Budgets, Recurring Expenses, Expenses, Shopping Items)
     * to a single JSON string structure.
     */
    suspend fun getExportString(): String {
        val profiles = repository.getAllProfilesDirect()
        val expenses = repository.getAllExpensesDirect()
        val shoppingItems = repository.getAllShoppingItemsDirect()
        val categories = repository.getAllCategoriesDirect()
        val recurringExpenses = repository.getAllRecurringExpensesDirect()
        val debts = repository.getAllDebtsDirect()

        val root = JSONObject()
        root.put("version", 1)
        root.put("exportDate", SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.US).format(Date()))

        // 1. Serialize User Profiles
        val profilesArray = JSONArray()
        for (profile in profiles) {
            val pObj = JSONObject()
            pObj.put("id", profile.id)
            pObj.put("name", profile.name)
            pObj.put("colorHex", profile.colorHex)
            pObj.put("isDefault", profile.isDefault)
            pObj.put("iconResId", profile.iconResId)
            pObj.put("monthlyBudget", profile.monthlyBudget)
            profilesArray.put(pObj)
        }
        root.put("profiles", profilesArray)

        // 2. Serialize Categories
        val categoriesArray = JSONArray()
        for (category in categories) {
            val cObj = JSONObject()
            cObj.put("id", category.id)
            cObj.put("name", category.name)
            cObj.put("iconEmoji", category.iconEmoji)
            cObj.put("colorHex", category.colorHex)
            cObj.put("isDefault", category.isDefault)
            categoriesArray.put(cObj)
        }
        root.put("categories", categoriesArray)

        // 3. Serialize Budgets (from Profiles)
        val budgetsArray = JSONArray()
        for (profile in profiles) {
            val bObj = JSONObject()
            bObj.put("profileId", profile.id)
            bObj.put("amount", profile.monthlyBudget)
            budgetsArray.put(bObj)
        }
        root.put("budgets", budgetsArray)

        // 4. Serialize Recurring Expenses
        val recurringArray = JSONArray()
        for (re in recurringExpenses) {
            val rObj = JSONObject()
            rObj.put("id", re.id)
            rObj.put("profileId", re.profileId)
            rObj.put("itemName", re.itemName)
            rObj.put("amount", re.amount)
            rObj.put("category", re.category)
            rObj.put("frequency", re.frequency)
            rObj.put("nextDueDate", re.nextDueDate)
            rObj.put("isActive", re.isActive)
            recurringArray.put(rObj)
        }
        root.put("recurring_expenses", recurringArray)

        // 5. Serialize Expenses Records
        val expensesArray = JSONArray()
        for (expense in expenses) {
            val eObj = JSONObject()
            eObj.put("id", expense.id)
            eObj.put("profileId", expense.profileId)
            eObj.put("itemName", expense.itemName)
            eObj.put("price", expense.price)
            eObj.put("currency", expense.currency)
            eObj.put("category", expense.category)
            eObj.put("date", expense.date)
            expensesArray.put(eObj)
        }
        root.put("expenses", expensesArray)

        // 6. Serialize Shopping Checklist Items
        val itemsArray = JSONArray()
        for (item in shoppingItems) {
            val iObj = JSONObject()
            iObj.put("id", item.id)
            iObj.put("profileId", item.profileId)
            iObj.put("itemName", item.itemName)
            iObj.put("quantity", item.quantity)
            iObj.put("unit", item.unit)
            iObj.put("estimatedPrice", item.estimatedPrice)
            iObj.put("isPurchased", item.isPurchased)
            iObj.put("dateAdded", item.dateAdded)
            itemsArray.put(iObj)
        }
        root.put("shopping_list_items", itemsArray)

        // 7. Serialize Debts
        val debtsArray = JSONArray()
        for (debt in debts) {
            val dObj = JSONObject()
            dObj.put("id", debt.id)
            dObj.put("profileId", debt.profileId)
            dObj.put("personName", debt.personName)
            dObj.put("amount", debt.amount)
            dObj.put("paidAmount", debt.paidAmount)
            dObj.put("type", debt.type)
            dObj.put("date", debt.date)
            if (debt.dueDate != null) {
                dObj.put("dueDate", debt.dueDate)
            } else {
                dObj.put("dueDate", JSONObject.NULL)
            }
            if (debt.notes != null) {
                dObj.put("notes", debt.notes)
            } else {
                dObj.put("notes", JSONObject.NULL)
            }
            dObj.put("status", debt.status)
            if (debt.contactNumber != null) {
                dObj.put("contactNumber", debt.contactNumber)
            } else {
                dObj.put("contactNumber", JSONObject.NULL)
            }
            debtsArray.put(dObj)
        }
        root.put("debts", debtsArray)

        return root.toString(4)
    }

    /**
     * Writes the backup JSON string structure directly to the SAF URI.
     */
    suspend fun exportDataToUri(context: Context, uri: Uri): Boolean {
        return try {
            val jsonString = getExportString()
            context.contentResolver.openOutputStream(uri)?.use { outputStream ->
                OutputStreamWriter(outputStream).use { writer ->
                    writer.write(jsonString)
                }
            }
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    /**
     * Reads, validates, and imports the JSON data from the SAF URI back into the database.
     */
    suspend fun importDataFromUri(context: Context, uri: Uri): Result<Unit> {
        return try {
            val jsonBuilder = StringBuilder()
            context.contentResolver.openInputStream(uri)?.use { inputStream ->
                BufferedReader(InputStreamReader(inputStream)).use { reader ->
                    var line: String?
                    while (reader.readLine().also { line = it } != null) {
                        jsonBuilder.append(line)
                    }
                }
            }

            val jsonContent = jsonBuilder.toString()
            if (jsonContent.trim().isEmpty()) {
                return Result.failure(Exception("ফাইলটি খালি"))
            }

            val root = JSONObject(jsonContent)
            if (!root.has("version")) {
                return Result.failure(Exception("অবৈধ ব্যাকআপ ফাইল ফরম্যাট"))
            }

            val version = root.getInt("version")
            if (version > 1) {
                return Result.failure(Exception("অসমর্থিত ব্যাকআপ সংস্করণ: $version"))
            }

            // Parse budgets first to map them
            val budgetMap = mutableMapOf<Long, Double>()
            if (root.has("budgets")) {
                val array = root.getJSONArray("budgets")
                for (i in 0 until array.length()) {
                    val bObj = array.getJSONObject(i)
                    val pId = bObj.getLong("profileId")
                    val amt = bObj.getDouble("amount")
                    budgetMap[pId] = amt
                }
            }

            // Parse profiles list
            val profiles = mutableListOf<ProfileEntity>()
            if (root.has("profiles")) {
                val array = root.getJSONArray("profiles")
                for (i in 0 until array.length()) {
                    val pObj = array.getJSONObject(i)
                    val pId = pObj.getLong("id")
                    profiles.add(
                        ProfileEntity(
                            id = pId,
                            name = pObj.getString("name"),
                            colorHex = pObj.optString("colorHex", "#6750A4"),
                            isDefault = pObj.optBoolean("isDefault", false),
                            iconResId = pObj.optInt("iconResId", 0),
                            monthlyBudget = budgetMap[pId] ?: pObj.optDouble("monthlyBudget", 0.0)
                        )
                    )
                }
            } else {
                return Result.failure(Exception("ব্যাকআপ ফাইলে কোনো প্রোফাইল নেই"))
            }

            // Parse categories list
            val categories = mutableListOf<CategoryEntity>()
            if (root.has("categories")) {
                val array = root.getJSONArray("categories")
                for (i in 0 until array.length()) {
                    val cObj = array.getJSONObject(i)
                    categories.add(
                        CategoryEntity(
                            id = cObj.getLong("id"),
                            name = cObj.getString("name"),
                            iconEmoji = cObj.getString("iconEmoji"),
                            colorHex = cObj.getString("colorHex"),
                            isDefault = cObj.optBoolean("isDefault", false)
                        )
                    )
                }
            }

            // Parse recurring expenses list
            val recurringExpenses = mutableListOf<RecurringExpenseEntity>()
            val recKey = if (root.has("recurring_expenses")) "recurring_expenses" else "recurringExpenses"
            if (root.has(recKey)) {
                val array = root.getJSONArray(recKey)
                for (i in 0 until array.length()) {
                    val rObj = array.getJSONObject(i)
                    recurringExpenses.add(
                        RecurringExpenseEntity(
                            id = rObj.getLong("id"),
                            profileId = rObj.getLong("profileId"),
                            itemName = rObj.getString("itemName"),
                            amount = rObj.getDouble("amount"),
                            category = rObj.optString("category", "Grocery"),
                            frequency = rObj.getString("frequency"),
                            nextDueDate = rObj.getLong("nextDueDate"),
                            isActive = rObj.optBoolean("isActive", true)
                        )
                    )
                }
            }

            // Parse expenses list
            val expenses = mutableListOf<ExpenseEntity>()
            if (root.has("expenses")) {
                val array = root.getJSONArray("expenses")
                for (i in 0 until array.length()) {
                    val eObj = array.getJSONObject(i)
                    expenses.add(
                        ExpenseEntity(
                            id = eObj.getLong("id"),
                            profileId = eObj.getLong("profileId"),
                            itemName = eObj.getString("itemName"),
                            price = eObj.getDouble("price"),
                            currency = eObj.optString("currency", "৳"),
                            category = eObj.optString("category", "Grocery"),
                            date = eObj.optLong("date", System.currentTimeMillis())
                        )
                    )
                }
            }

            // Parse shopping checklist list (checking both "shopping_list_items" and legacy "shoppingListItems")
            val shoppingItems = mutableListOf<ShoppingListItemEntity>()
            val shopKey = if (root.has("shopping_list_items")) "shopping_list_items" else "shoppingListItems"
            if (root.has(shopKey)) {
                val array = root.getJSONArray(shopKey)
                for (i in 0 until array.length()) {
                    val iObj = array.getJSONObject(i)
                    shoppingItems.add(
                        ShoppingListItemEntity(
                            id = iObj.getLong("id"),
                            profileId = iObj.getLong("profileId"),
                            itemName = iObj.getString("itemName"),
                            quantity = iObj.optDouble("quantity", 1.0),
                            unit = iObj.optString("unit", "kg"),
                            estimatedPrice = iObj.optDouble("estimatedPrice", 0.0),
                            isPurchased = iObj.optBoolean("isPurchased", false),
                            dateAdded = iObj.optLong("dateAdded", System.currentTimeMillis())
                        )
                    )
                }
            }

            // Parse debts list
            val debts = mutableListOf<Debt>()
            if (root.has("debts")) {
                val array = root.getJSONArray("debts")
                for (i in 0 until array.length()) {
                    val dObj = array.getJSONObject(i)
                    val dueDateValue = if (dObj.isNull("dueDate")) null else dObj.getLong("dueDate")
                    val notesValue = if (dObj.isNull("notes")) null else dObj.getString("notes")
                    val contactValue = if (dObj.isNull("contactNumber")) null else dObj.getString("contactNumber")
                    debts.add(
                        Debt(
                            id = dObj.getLong("id"),
                            profileId = dObj.getLong("profileId"),
                            personName = dObj.getString("personName"),
                            amount = dObj.getDouble("amount"),
                            paidAmount = dObj.optDouble("paidAmount", 0.0),
                            type = dObj.getString("type"),
                            date = dObj.optLong("date", System.currentTimeMillis()),
                            dueDate = dueDateValue,
                            notes = notesValue,
                            status = dObj.optString("status", "unpaid"),
                            contactNumber = contactValue
                        )
                    )
                }
            }

            // Overwrite database tables using repository controller
            repository.restoreDatabase(profiles, expenses, shoppingItems, categories, recurringExpenses, debts)
            Result.success(Unit)
        } catch (e: Exception) {
            e.printStackTrace()
            Result.failure(Exception("ডাটা ইম্পোর্ট ব্যর্থ হয়েছে: ${e.localizedMessage}"))
        }
    }
}
