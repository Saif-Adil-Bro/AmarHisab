package com.example.ui

import androidx.compose.ui.graphics.Color
import java.util.*

fun getCategoryEmoji(category: String): String {
    return when (category.lowercase(Locale.ROOT).trim()) {
        "vegetables", "veggies", "সবজি" -> "🥬"
        "meat", "poultry", "fish", "মাংস", "মাছ" -> "🥩"
        "grocery", "groceries", "food", "বাজার", "grocery" -> "📦"
        "dairy", "eggs", "dairy/eggs", "দুধ", "ডিম" -> "🥛"
        "fruits", "fruit", "ফল" -> "🍎"
        "beverages", "drinks", "পানীয়" -> "🥤"
        "snacks", "নাস্তা" -> "🍿"
        else -> "🛒"
    }
}

fun getCategoryColor(category: String): Color {
    return when (category.lowercase(Locale.ROOT).trim()) {
        "vegetables", "veggies", "সবজি" -> Color(0xFF2E7D32) // Green
        "meat", "poultry", "fish", "মাংস", "মাছ" -> Color(0xFFC62828) // Red
        "grocery", "groceries", "food", "বাজার", "grocery" -> Color(0xFF1565C0) // Blue
        "dairy", "eggs", "dairy/eggs", "দুধ", "ডিম" -> Color(0xFFEF6C00) // Orange
        "fruits", "fruit", "ফল" -> Color(0xFFAD1457) // Pink
        "beverages", "drinks", "পানীয়" -> Color(0xFF00838F) // Teal
        "snacks", "নাস্তা" -> Color(0xFF6A1B9A) // Purple
        else -> Color(0xFF4E342E) // Brown
    }
}
