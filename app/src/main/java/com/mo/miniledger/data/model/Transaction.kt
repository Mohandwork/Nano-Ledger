package com.mo.miniledger.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "transactions")
data class Transaction(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val timestamp: Long,
    val data: Map<String, String> = emptyMap(),
    val flaggedItems: List<String> = emptyList()
) {
    /**
     * Tries to find the amount in the dynamic data.
     */
    fun getAmount(): Double {
        val amountKeys = listOf("Amount", "Total", "Total Amount", "Grand Total", "Price")
        return data.entries.find { entry ->
            amountKeys.any { it.equals(entry.key, ignoreCase = true) }
        }?.value?.toDoubleOrNull() ?: 0.0
    }

    /**
     * Tries to determine if the transaction is an expense.
     */
    fun isExpense(): Boolean {
        val typeValue = data.entries.find { it.key.equals("Type", ignoreCase = true) }?.value
        if (typeValue?.equals("Income", ignoreCase = true) == true) return false
        if (typeValue?.equals("Expense", ignoreCase = true) == true) return true
        
        // Default heuristics if not explicitly provided
        val category = data.entries.find { it.key.equals("Category", ignoreCase = true) }?.value
        val incomeCategories = listOf("Salary", "Investment", "Bonus", "Gift")
        if (incomeCategories.any { it.equals(category, ignoreCase = true) }) return false
        
        return true // Default to expense
    }
}
