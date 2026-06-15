package com.example.data

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Entity representing a debt or borrowing entry, linked to an active profile.
 */
@Entity(
    tableName = "debts",
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
data class Debt(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val profileId: Long,
    val personName: String,
    val amount: Double,
    val paidAmount: Double = 0.0,
    val type: String, // "borrowed" or "lent"
    val date: Long = System.currentTimeMillis(),
    val dueDate: Long? = null,
    val notes: String? = null,
    val status: String = "unpaid", // "unpaid", "partial", "paid"
    val contactNumber: String? = null
)
