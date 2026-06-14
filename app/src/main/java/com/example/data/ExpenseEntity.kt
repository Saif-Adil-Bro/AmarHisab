package com.example.data

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Entity representing an individual purchase/expense.
 * Linked to a specific Profile via a foreign key constraint with cascade delete.
 */
@Entity(
    tableName = "expenses",
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
data class ExpenseEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val profileId: Long,
    val itemName: String,
    val price: Double,
    val currency: String = "৳", // Default currency. Supports: ৳, $, £, ₹
    val category: String = "Grocery", // Default category
    val date: Long = System.currentTimeMillis() // Default current timestamp
)
