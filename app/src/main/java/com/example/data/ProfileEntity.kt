package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Entity representing a user profile (e.g., Personal, Family, Office).
 * Allows switching between distinct grocery/expense tracking contexts.
 */
@Entity(tableName = "profiles")
data class ProfileEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val name: String,
    val colorHex: String = "#6750A4",
    val isDefault: Boolean = false,
    val iconResId: Int = 0 // Used to track avatar color or numerical icon themes
)
