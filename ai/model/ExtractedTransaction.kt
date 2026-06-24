package com.mo.miniledger.ai.model

import kotlinx.serialization.Serializable

@Serializable
data class ExtractedTransaction(
    val amount: Double,
    val date: Long? = null,
    val category: String,
    val isExpense: Boolean = true,
    val merchantName: String? = null,
    val dynamicFields: Map<String, String> = emptyMap(),
    val flaggedItems: List<String> = emptyList()
)
