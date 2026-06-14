package com.example.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

/**
 * Data Access Object for Shopping List Items.
 * Manages checklists, updates purchase status, and queries by active profile contexts.
 */
@Dao
interface ShoppingListDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertItem(item: ShoppingListItemEntity): Long

    @Update
    suspend fun updateItem(item: ShoppingListItemEntity)

    @Delete
    suspend fun deleteItem(item: ShoppingListItemEntity)

    /**
     * Emits shopping list items, displaying unpurchased ones at the top and ordered by addition date.
     */
    @Query("SELECT * FROM shopping_list_items WHERE profileId = :profileId ORDER BY isPurchased ASC, dateAdded DESC")
    fun getItemsByProfile(profileId: Long): Flow<List<ShoppingListItemEntity>>

    /**
     * Alias method pointing to custom ordered profile shopping items.
     */
    @Query("SELECT * FROM shopping_list_items WHERE profileId = :profileId ORDER BY isPurchased ASC, dateAdded DESC")
    fun getAllItems(profileId: Long): Flow<List<ShoppingListItemEntity>>

    /**
     * Directly updates purchase status of an item.
     */
    @Query("UPDATE shopping_list_items SET isPurchased = :isPurchased WHERE id = :itemId")
    suspend fun updatePurchaseStatus(itemId: Long, isPurchased: Boolean)

    @Query("SELECT * FROM shopping_list_items WHERE id = :id")
    suspend fun getItemById(id: Long): ShoppingListItemEntity?

    /**
     * Retrieves all shopping list items directly.
     */
    @Query("SELECT * FROM shopping_list_items")
    suspend fun getAllShoppingItemsDirect(): List<ShoppingListItemEntity>

    /**
     * Deletes all shopping list items.
     */
    @Query("DELETE FROM shopping_list_items")
    suspend fun deleteAllShoppingItems()

    /**
     * Bulk inserts shopping list items.
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertShoppingItems(items: List<ShoppingListItemEntity>)
}
