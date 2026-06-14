package com.example.data

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Entity representing an item in the shopping checklist.
 * Linked to a specific Profile via a foreign key constraint with cascade delete.
 */
@Entity(
    tableName = "shopping_list_items",
    foreignKeys = [
        ForeignKey(
            entity = ProfileEntity::class,
            parentColumns = ["id"],
            childColumns = ["profileId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["profileId"])]
)
data class ShoppingListItemEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val profileId: Long,
    val itemName: String,
    val quantity: Double = 1.0, // Default quantity
    val unit: String = "kg", // Default unit. Supports: kg, gm, pcs, liter
    val estimatedPrice: Double = 0.0, // Default estimated price
    val isPurchased: Boolean = false, // Default purchase state
    val dateAdded: Long = System.currentTimeMillis() // Default registration timestamp
)
