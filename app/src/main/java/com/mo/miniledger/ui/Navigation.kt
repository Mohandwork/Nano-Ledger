package com.mo.miniledger.ui

import kotlinx.serialization.Serializable

@Serializable
sealed interface NavRoute {
    @Serializable
    data object Dashboard : NavRoute
    
    @Serializable
    data object TransactionList : NavRoute
    
    @Serializable
    data object AddTransaction : NavRoute
    
    @Serializable
    data class ViewTransaction(val transactionId: Int) : NavRoute
}

