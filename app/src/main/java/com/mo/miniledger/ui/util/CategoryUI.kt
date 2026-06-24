package com.mo.miniledger.ui.util

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ReceiptLong
import androidx.compose.material.icons.automirrored.rounded.TrendingUp
import androidx.compose.material.icons.rounded.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector

data class CategoryTheme(
    val icon: ImageVector,
    val color: Color
)

object CategoryUI {
    private val defaultTheme = CategoryTheme(Icons.AutoMirrored.Rounded.ReceiptLong, Color(0xFF9E9E9E))

    private val categoryMap = mapOf(
        "Food" to CategoryTheme(Icons.Rounded.Restaurant, Color(0xFFFF9800)),
        "Dining" to CategoryTheme(Icons.Rounded.Restaurant, Color(0xFFFF9800)),
        "Groceries" to CategoryTheme(Icons.Rounded.LocalGroceryStore, Color(0xFF4CAF50)),
        "Travel" to CategoryTheme(Icons.Rounded.Flight, Color(0xFF2196F3)),
        "Transportation" to CategoryTheme(Icons.Rounded.DirectionsCar, Color(0xFF00BCD4)),
        "Shopping" to CategoryTheme(Icons.Rounded.ShoppingBag, Color(0xFFE91E63)),
        "Entertainment" to CategoryTheme(Icons.Rounded.TheaterComedy, Color(0xFF9C27B0)),
        "Health" to CategoryTheme(Icons.Rounded.MedicalServices, Color(0xFFF44336)),
        "Utilities" to CategoryTheme(Icons.Rounded.Power, Color(0xFF795548)),
        "Salary" to CategoryTheme(Icons.Rounded.Payments, Color(0xFF4CAF50)),
        "Investment" to CategoryTheme(Icons.AutoMirrored.Rounded.TrendingUp, Color(0xFF009688)),
        "Education" to CategoryTheme(Icons.Rounded.School, Color(0xFF3F51B5)),
        "Housing" to CategoryTheme(Icons.Rounded.Home, Color(0xFF607D8B))
    )

    fun getTheme(category: String?): CategoryTheme {
        if (category == null) return defaultTheme
        return categoryMap.entries.find { it.key.equals(category, ignoreCase = true) }?.value ?: defaultTheme
    }
}
