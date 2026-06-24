package com.mo.miniledger.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.mo.miniledger.data.model.Transaction
import com.mo.miniledger.ui.theme.ExpenseRed
import com.mo.miniledger.ui.theme.IncomeGreen
import com.mo.miniledger.ui.screens.formatCurrency
import java.util.*

import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.draw.clip
import com.mo.miniledger.ui.util.CategoryUI

@Composable
fun TransactionItem(
    transaction: Transaction,
    onClick: () -> Unit
) {
    val amount = transaction.getAmount()
    val isExpense = transaction.isExpense()
    val amountColor = if (isExpense) ExpenseRed else IncomeGreen
    
    // Heuristic to find merchant or category for title
    val category = transaction.data["Category"]
    val title = transaction.data["Merchant"] ?: category ?: "Unknown"
    val theme = CategoryUI.getTheme(category)
    
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        border = CardDefaults.outlinedCardBorder(),
        shape = MaterialTheme.shapes.medium
    ) {
        Row(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                Surface(
                    color = theme.color.copy(alpha = 0.15f),
                    shape = CircleShape,
                    modifier = Modifier.size(40.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = theme.icon,
                            contentDescription = null,
                            tint = theme.color,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.width(16.dp))
                Column {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = Date(transaction.timestamp).toString().substring(4, 10),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    if (transaction.data.isNotEmpty()) {
                        Text(
                            text = transaction.data.entries.filter { it.key != "Merchant" && it.key != "Amount" && it.key != "Category" }
                                .joinToString("; ") { "${it.key}: ${it.value}" },
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary,
                            maxLines = 1
                        )
                    }
                }
            }
            Text(
                text = (if (isExpense) "-" else "+") + formatCurrency(amount),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Black,
                color = amountColor
            )
        }
    }
}
