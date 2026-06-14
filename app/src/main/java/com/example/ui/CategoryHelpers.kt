package com.example.ui

import androidx.compose.ui.graphics.Color
import com.example.data.CategoryEntity
import java.util.*

fun getCategoryEmoji(category: String, customCategories: List<CategoryEntity> = emptyList()): String {
    // 1. Search in custom categories
    val match = customCategories.find { it.name.trim().lowercase(Locale.ROOT) == category.trim().lowercase(Locale.ROOT) }
    if (match != null) return match.iconEmoji
    
    // 2. Fallback to hardcoded defaults
    return when (category.lowercase(Locale.ROOT).trim()) {
        "vegetables", "veggies", "সবজি", "শাকসবজি" -> "🥬"
        "meat", "poultry", "fish", "মাংস", "মাছ", "মাছ ও মাংস" -> "🍖"
        "grocery", "groceries", "food", "বাজার", "grocery" -> "📦"
        "dairy", "eggs", "dairy/eggs", "দুধ", "ডিম" -> "🥛"
        "fruits", "fruit", "ফল" -> "🍎"
        "beverages", "drinks", "পানীয়" -> "🥤"
        "snacks", "নাস্তা" -> "🍿"
        "transport", "travel", "যাতায়াত" -> "🚗"
        "rent", "বাসা ভাড়া" -> "🏠"
        "bills", "electricity", "বিদ্যুৎ বিল" -> "💡"
        else -> "🛒"
    }
}

fun getCategoryColor(category: String, customCategories: List<CategoryEntity> = emptyList()): Color {
    val match = customCategories.find { it.name.trim().lowercase(Locale.ROOT) == category.trim().lowercase(Locale.ROOT) }
    if (match != null) {
        return try {
            Color(android.graphics.Color.parseColor(match.colorHex))
        } catch (e: Exception) {
            Color(0xFF6750A4)
        }
    }
    
    return when (category.lowercase(Locale.ROOT).trim()) {
        "vegetables", "veggies", "সবজি", "শাকসবজি" -> Color(0xFF2E7D32) // Green
        "meat", "poultry", "fish", "মাংস", "মাছ", "মাছ ও মাংস" -> Color(0xFFC62828) // Red
        "grocery", "groceries", "food", "বাজার", "grocery" -> Color(0xFF1565C0) // Blue
        "dairy", "eggs", "dairy/eggs", "দুধ", "ডিম" -> Color(0xFFEF6C00) // Orange
        "fruits", "fruit", "ফল" -> Color(0xFFAD1457) // Pink
        "beverages", "drinks", "পানীয়" -> Color(0xFF00838F) // Teal
        "snacks", "নাস্তা" -> Color(0xFF6A1B9A) // Purple
        "transport", "travel", "যাতায়াত" -> Color(0xFFED6C02) // Orange/Amber
        "rent", "বাসা ভাড়া" -> Color(0xFF4E342E) // Brown
        "bills", "electricity", "বিদ্যুৎ বিল" -> Color(0xFFEF6C00) // Orangeish
        else -> Color(0xFF5D5E63) // Grey
    }
}
