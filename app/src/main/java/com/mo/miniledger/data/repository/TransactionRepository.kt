package com.mo.miniledger.data.repository

import com.mo.miniledger.data.local.TransactionDao
import com.mo.miniledger.data.model.Transaction
import kotlinx.coroutines.flow.Flow

class TransactionRepository(private val transactionDao: TransactionDao) {

    fun getAllTransactions(): Flow<List<Transaction>> = transactionDao.getAllTransactions()

    fun getTransactionsInRange(startDate: Long, endDate: Long): Flow<List<Transaction>> =
        transactionDao.getTransactionsInRange(startDate, endDate)

    suspend fun upsertTransaction(transaction: Transaction) {
        transactionDao.upsertTransaction(transaction)
    }

    suspend fun deleteTransaction(transaction: Transaction) {
        transactionDao.deleteTransaction(transaction)
    }
}
