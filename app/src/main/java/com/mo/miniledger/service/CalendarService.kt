package com.mo.miniledger.service

import android.content.ContentUris
import android.content.Context
import android.provider.CalendarContract
import android.util.Log

data class CalendarEvent(
    val title: String,
    val location: String?,
    val description: String?
)

class CalendarService(private val context: Context) {

    fun getEventsForTimestamp(timestamp: Long): List<CalendarEvent> {
        val events = mutableListOf<CalendarEvent>()
        val projection = arrayOf(
            CalendarContract.Events.TITLE,
            CalendarContract.Events.EVENT_LOCATION,
            CalendarContract.Events.DESCRIPTION,
            CalendarContract.Events.DTSTART,
            CalendarContract.Events.DTEND
        )

        // Buffer of 1 day before and 4 months after for context (e.g., future hotel reservations)
        val startTime = timestamp - 86400000 // 1 day
        val endTime = timestamp + 10368000000 // 120 days (4 months)

        val selection = "(${CalendarContract.Events.DTSTART} <= $endTime) AND (${CalendarContract.Events.DTEND} >= $startTime)"

        try {
            val cursor = context.contentResolver.query(
                CalendarContract.Events.CONTENT_URI,
                projection,
                selection,
                null,
                "${CalendarContract.Events.DTSTART} ASC"
            )

            cursor?.use {
                val titleIndex = it.getColumnIndex(CalendarContract.Events.TITLE)
                val locationIndex = it.getColumnIndex(CalendarContract.Events.EVENT_LOCATION)
                val descIndex = it.getColumnIndex(CalendarContract.Events.DESCRIPTION)

                while (it.moveToNext()) {
                    events.add(
                        CalendarEvent(
                            title = it.getString(titleIndex) ?: "Untitled Event",
                            location = it.getString(locationIndex),
                            description = it.getString(descIndex)
                        )
                    )
                }
            }
        } catch (e: Exception) {
            Log.e("CalendarService", "Error querying calendar", e)
        }

        return events
    }
}
