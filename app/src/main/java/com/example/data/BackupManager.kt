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
     * Serializes all tables (Profiles, Expenses, Shopping Items) to a single JSON string structure.
     */
    suspend fun getExportString(): String {
        val profiles = repository.getAllProfilesDirect()
        val expenses = repository.getAllExpensesDirect()
        val shoppingItems = repository.getAllShoppingItemsDirect()

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
            profilesArray.put(pObj)
        }
        root.put("profiles", profilesArray)

        // 2. Serialize Expenses Records
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

        // 3. Serialize Shopping Checklist Items
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
        root.put("shoppingListItems", itemsArray)

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

            // Parse profiles list
            val profiles = mutableListOf<ProfileEntity>()
            if (root.has("profiles")) {
                val array = root.getJSONArray("profiles")
                for (i in 0 until array.length()) {
                    val pObj = array.getJSONObject(i)
                    profiles.add(
                        ProfileEntity(
                            id = pObj.getLong("id"),
                            name = pObj.getString("name"),
                            colorHex = pObj.optString("colorHex", "#6750A4"),
                            isDefault = pObj.optBoolean("isDefault", false),
                            iconResId = pObj.optInt("iconResId", 0)
                        )
                    )
                }
            } else {
                return Result.failure(Exception("ব্যাকআপ ফাইলে কোনো প্রোফাইল নেই"))
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

            // Parse shopping checklist list
            val shoppingItems = mutableListOf<ShoppingListItemEntity>()
            if (root.has("shoppingListItems")) {
                val array = root.getJSONArray("shoppingListItems")
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

            // Overwrite database tables using repository controller
            repository.restoreDatabase(profiles, expenses, shoppingItems)
            Result.success(Unit)
        } catch (e: Exception) {
            e.printStackTrace()
            Result.failure(Exception("ডাটা ইম্পোর্ট ব্যর্থ হয়েছে: ${e.localizedMessage}"))
        }
    }
}
