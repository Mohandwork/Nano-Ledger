package com.mo.miniledger.ai.model

import kotlinx.serialization.Serializable

@Serializable
data class ExtractedTransaction(
    val fields: Map<String, String> = emptyMap(),
    val flaggedItems: List<String> = emptyList(),
    val items: List<LineItem> = emptyList(),
    val suggestedTimestamp: Long? = null
)

@Serializable
data class LineItem(
    val itemId: String,
    val description: String,
    val totalPrice: Double,
    val category: String, // "FOOD" | "TRANSPORT" | "LODGING" | "OTHER"
    val policyStatus: String, // "APPROVED" | "VIOLATION_RESTRICTED" | "VIOLATION_LIMIT_EXCEEDED"
    val violationReason: String? = null,
    val inlineActions: List<LineItemAction> = emptyList()
)

@Serializable
data class LineItemAction(
    val actionId: String,
    val buttonText: String,
    val uiStyleHint: String, // "PRIMARY" | "SECONDARY" | "OUTLINED"
    val resultingValueModifier: Double
)
