package com.mo.miniledger

import android.app.Application
import com.mo.miniledger.data.local.AppDatabase
import com.mo.miniledger.data.repository.TransactionRepository
import com.mo.miniledger.service.CalendarService
import com.mo.miniledger.service.LocationService

class MiniLedgerApplication : Application() {
    val database by lazy { AppDatabase.getDatabase(this) }
    val repository by lazy { TransactionRepository(database.transactionDao) }
    val calendarService by lazy { CalendarService(this) }
    val locationService by lazy { LocationService(this) }
}
