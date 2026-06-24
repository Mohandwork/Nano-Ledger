package com.mo.miniledger

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.material3.adaptive.ExperimentalMaterial3AdaptiveApi
import androidx.compose.material3.adaptive.layout.AnimatedPane
import androidx.compose.material3.adaptive.layout.ListDetailPaneScaffold
import androidx.compose.material3.adaptive.layout.ListDetailPaneScaffoldRole
import androidx.compose.material3.adaptive.navigation.rememberListDetailPaneScaffoldNavigator
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import com.mo.miniledger.ui.NavRoute
import com.mo.miniledger.ui.TransactionViewModel
import com.mo.miniledger.ui.screens.AddTransactionScreen
import com.mo.miniledger.ui.screens.DashboardScreen
import com.mo.miniledger.ui.screens.TransactionListScreen
import com.mo.miniledger.ui.screens.TransactionDetailScreen
import com.mo.miniledger.ui.theme.MiniLedgerTheme
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    @OptIn(ExperimentalMaterial3AdaptiveApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val app = application as MiniLedgerApplication
        val viewModel = TransactionViewModel(
            app.repository,
            app.calendarService,
            app.locationService
        )

        setContent {
            MiniLedgerTheme {
                val scaffoldNavigator = rememberListDetailPaneScaffoldNavigator<NavRoute>()
                val scope = rememberCoroutineScope()
                
                val balance by viewModel.balance.collectAsState()
                val income by viewModel.totalIncome.collectAsState()
                val expense by viewModel.totalExpense.collectAsState()
                val transactions by viewModel.allTransactions.collectAsState()

                ListDetailPaneScaffold(
                    directive = scaffoldNavigator.scaffoldDirective,
                    value = scaffoldNavigator.scaffoldValue,
                    listPane = {
                        AnimatedPane {
                            DashboardScreen(
                                balance = balance,
                                income = income,
                                expense = expense,
                                recentTransactions = transactions,
                                onAddTransactionClick = {
                                    scope.launch {
                                        scaffoldNavigator.navigateTo(ListDetailPaneScaffoldRole.Detail, NavRoute.AddTransaction)
                                    }
                                },
                                onSeeAllClick = {
                                    scope.launch {
                                        scaffoldNavigator.navigateTo(ListDetailPaneScaffoldRole.Detail, NavRoute.TransactionList)
                                    }
                                },
                                onTransactionClick = { transaction ->
                                    scope.launch {
                                        scaffoldNavigator.navigateTo(ListDetailPaneScaffoldRole.Detail, NavRoute.ViewTransaction(transaction.id))
                                    }
                                }
                            )
                        }
                    },
                    detailPane = {
                        AnimatedPane {
                            val currentDetail = scaffoldNavigator.currentDestination?.contentKey
                            when (currentDetail) {
                                is NavRoute.AddTransaction -> {
                                    val isProcessing by viewModel.isProcessing.collectAsState()
                                    val extracted by viewModel.extractedTransaction.collectAsState()
                                    val receiptBitmap by viewModel.receiptBitmap.collectAsState()

                                    AddTransactionScreen(
                                        onBackClick = {
                                            viewModel.clearExtractedTransaction()
                                            scope.launch {
                                                scaffoldNavigator.navigateBack()
                                            }
                                        },
                                        onSaveClick = { timestamp, data, flaggedItems ->
                                            viewModel.addTransaction(timestamp, data, flaggedItems)
                                            viewModel.clearExtractedTransaction()
                                            scope.launch {
                                                scaffoldNavigator.navigateBack()
                                            }
                                        },
                                        isProcessing = isProcessing,
                                        extractedTransaction = extracted,
                                        receiptBitmap = receiptBitmap,
                                        onReceiptPicked = { bitmap ->
                                            viewModel.processReceipt(bitmap)
                                        },
                                        onClearExtracted = {
                                            viewModel.clearExtractedTransaction()
                                        }
                                    )
                                }
                                is NavRoute.TransactionList -> {
                                    TransactionListScreen(
                                        transactions = transactions,
                                        onBackClick = {
                                            scope.launch {
                                                scaffoldNavigator.navigateBack()
                                            }
                                        },
                                        onTransactionClick = { transaction ->
                                            scope.launch {
                                                scaffoldNavigator.navigateTo(ListDetailPaneScaffoldRole.Detail, NavRoute.ViewTransaction(transaction.id))
                                            }
                                        }
                                    )
                                }
                                is NavRoute.ViewTransaction -> {
                                    val transaction = transactions.find { it.id == currentDetail.transactionId }
                                    TransactionDetailScreen(
                                        transaction = transaction,
                                        onBackClick = {
                                            scope.launch {
                                                scaffoldNavigator.navigateBack()
                                            }
                                        },
                                        onDeleteClick = {
                                            transaction?.let { viewModel.deleteTransaction(it) }
                                            scope.launch {
                                                scaffoldNavigator.navigateBack()
                                            }
                                        }
                                    )
                                }
                                else -> {
                                    // Default detail or placeholder
                                }
                            }
                        }
                    }
                )
            }
        }
    }
}
