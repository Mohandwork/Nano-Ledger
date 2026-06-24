package com.mo.miniledger.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mo.miniledger.ai.ReceiptProcessor
import com.mo.miniledger.ai.model.ExtractedTransaction
import com.mo.miniledger.data.model.Transaction
import com.mo.miniledger.data.repository.TransactionRepository
import com.mo.miniledger.service.CalendarService
import com.mo.miniledger.service.LocationService
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import android.graphics.Bitmap

class TransactionViewModel(
    private val repository: TransactionRepository,
    private val calendarService: CalendarService,
    private val locationService: LocationService
) : ViewModel() {

    private val receiptProcessor = ReceiptProcessor()

    private val _extractedTransaction = MutableStateFlow<ExtractedTransaction?>(null)
    val extractedTransaction: StateFlow<ExtractedTransaction?> = _extractedTransaction.asStateFlow()

    private val _isProcessing = MutableStateFlow(false)
    val isProcessing: StateFlow<Boolean> = _isProcessing.asStateFlow()

    private val _receiptBitmap = MutableStateFlow<Bitmap?>(null)
    val receiptBitmap: StateFlow<Bitmap?> = _receiptBitmap.asStateFlow()

    val allTransactions: StateFlow<List<Transaction>> = repository.getAllTransactions()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val totalIncome: StateFlow<Double> = allTransactions
        .map { list -> list.filter { !it.isExpense() }.sumOf { it.getAmount() } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    val totalExpense: StateFlow<Double> = allTransactions
        .map { list -> list.filter { it.isExpense() }.sumOf { it.getAmount() } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    val balance: StateFlow<Double> = allTransactions
        .map { list -> 
            val income = list.filter { !it.isExpense() }.sumOf { it.getAmount() }
            val expense = list.filter { it.isExpense() }.sumOf { it.getAmount() }
            income - expense
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    fun addTransaction(
        timestamp: Long,
        data: Map<String, String>,
        flaggedItems: List<String>
    ) {
        viewModelScope.launch {
            repository.upsertTransaction(
                Transaction(
                    timestamp = timestamp,
                    data = data,
                    flaggedItems = flaggedItems
                )
            )
        }
    }

    fun deleteTransaction(transaction: Transaction) {
        viewModelScope.launch {
            repository.deleteTransaction(transaction)
        }
    }

    fun processReceipt(bitmap: Bitmap) {
        viewModelScope.launch {
            _receiptBitmap.value = bitmap
            _isProcessing.value = true

            // Get initial context (Calendar events for NOW and upcoming 4 months)
            val currentEvents = calendarService.getEventsForTimestamp(System.currentTimeMillis())
            val contextString = if (currentEvents.isNotEmpty()) {
                currentEvents.joinToString("; ") { event ->
                    "${event.title} at ${event.location ?: "Unknown Location"}${if (event.description != null) " (Details: ${event.description})" else ""}"
                }
            } else null

            val extracted = receiptProcessor.processReceipt(bitmap, contextString)
            
            if (extracted != null) {
                val location = locationService.getCurrentLocation()
                val timestamp = extracted.suggestedTimestamp ?: System.currentTimeMillis()
                
                val enrichedFields = extracted.fields.toMutableMap()
                location?.let {
                    enrichedFields["Verified Location"] = "${it.latitude}, ${it.longitude}"
                }
                
                // If timestamp changed significantly, re-check events? (Optional, staying simple for now)
                
                _extractedTransaction.value = extracted.copy(fields = enrichedFields, suggestedTimestamp = timestamp)
            } else {
                _extractedTransaction.value = null
            }

            _isProcessing.value = false
        }
    }

    fun clearExtractedTransaction() {
        _extractedTransaction.value = null
        _receiptBitmap.value = null
    }
}
