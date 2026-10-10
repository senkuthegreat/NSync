package com.example.nsutconnect.data.calendar

import android.Manifest
import android.content.ContentValues
import android.content.Context
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.CalendarContract
import androidx.core.content.ContextCompat
import com.example.nsutconnect.data.model.ClassScheduleItem
import com.example.nsutconnect.data.model.WeeklyTimetable
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.time.temporal.TemporalAdjusters
import java.util.TimeZone

sealed class CalendarSyncResult {
    data class Success(val eventCount: Int, val accountName: String) : CalendarSyncResult()
    data class PermissionRequired(val message: String = "Calendar permissions required") : CalendarSyncResult()
    data class Error(val message: String) : CalendarSyncResult()
}

object GoogleCalendarSyncManager {

    private const val SYNC_TAG = "[NSUT_SCHEDULE_SYNC]"

    fun hasCalendarPermissions(context: Context): Boolean {
        val writePerm = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.WRITE_CALENDAR
        ) == PackageManager.PERMISSION_GRANTED
        val readPerm = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.READ_CALENDAR
        ) == PackageManager.PERMISSION_GRANTED
        return writePerm && readPerm
    }

    /**
     * Finds the primary Google Calendar account or the first writable calendar.
     */
    fun getTargetCalendar(context: Context): Pair<Long, String>? {
        if (!hasCalendarPermissions(context)) return null

        val projection = arrayOf(
            CalendarContract.Calendars._ID,
            CalendarContract.Calendars.ACCOUNT_NAME,
            CalendarContract.Calendars.ACCOUNT_TYPE,
            CalendarContract.Calendars.CALENDAR_DISPLAY_NAME,
            CalendarContract.Calendars.CALENDAR_ACCESS_LEVEL,
            CalendarContract.Calendars.IS_PRIMARY
        )

        return try {
            val cursor = context.contentResolver.query(
                CalendarContract.Calendars.CONTENT_URI,
                projection,
                null,
                null,
                null
            ) ?: return null

            var primaryGoogleCal: Pair<Long, String>? = null
            var emailGoogleCal: Pair<Long, String>? = null
            var fallbackGoogleCal: Pair<Long, String>? = null
            var firstWritableCal: Pair<Long, String>? = null

            cursor.use { c ->
                val idIdx = c.getColumnIndex(CalendarContract.Calendars._ID)
                val accNameIdx = c.getColumnIndex(CalendarContract.Calendars.ACCOUNT_NAME)
                val accTypeIdx = c.getColumnIndex(CalendarContract.Calendars.ACCOUNT_TYPE)
                val accessIdx = c.getColumnIndex(CalendarContract.Calendars.CALENDAR_ACCESS_LEVEL)
                val primaryIdx = c.getColumnIndex(CalendarContract.Calendars.IS_PRIMARY)

                while (c.moveToNext()) {
                    val id = c.getLong(idIdx)
                    val accName = c.getString(accNameIdx) ?: "Default"
                    val accType = c.getString(accTypeIdx) ?: ""
                    val isPrimary = if (primaryIdx >= 0) c.getInt(primaryIdx) == 1 else false
                    val accessLevel = if (accessIdx >= 0) c.getInt(accessIdx) else CalendarContract.Calendars.CAL_ACCESS_OWNER

                    // Ensure calendar is writable (at least CAL_ACCESS_CONTRIBUTOR = 500)
                    if (accessLevel < CalendarContract.Calendars.CAL_ACCESS_CONTRIBUTOR) {
                        continue
                    }

                    if (firstWritableCal == null) {
                        firstWritableCal = Pair(id, accName)
                    }

                    if (accType.equals("com.google", ignoreCase = true)) {
                        if (isPrimary && primaryGoogleCal == null) {
                            primaryGoogleCal = Pair(id, accName)
                        } else if (accName.contains("@") && emailGoogleCal == null) {
                            emailGoogleCal = Pair(id, accName)
                        } else if (fallbackGoogleCal == null) {
                            fallbackGoogleCal = Pair(id, accName)
                        }
                    }
                }
            }

            primaryGoogleCal ?: emailGoogleCal ?: fallbackGoogleCal ?: firstWritableCal
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    /**
     * Directly and automatically syncs the weekly timetable into the user's Google Calendar.
     * Replaces previous NSUT classes to prevent duplicates.
     */
    suspend fun syncTimetableToCalendar(
        context: Context,
        timetable: WeeklyTimetable
    ): CalendarSyncResult = withContext(Dispatchers.IO) {
        if (!hasCalendarPermissions(context)) {
            return@withContext CalendarSyncResult.PermissionRequired()
        }

        val targetCal = getTargetCalendar(context)
            ?: return@withContext CalendarSyncResult.Error("No writable Google Calendar found on device.")

        val calendarId = targetCal.first
        val accountName = targetCal.second
        val contentResolver = context.contentResolver

        try {
            // 1. Delete previously synced NSUT classes to prevent duplicates
            try {
                val deleteSelection = "${CalendarContract.Events.CALENDAR_ID} = ? AND ${CalendarContract.Events.DESCRIPTION} LIKE ?"
                val deleteArgs = arrayOf(calendarId.toString(), "%$SYNC_TAG%")
                contentResolver.delete(CalendarContract.Events.CONTENT_URI, deleteSelection, deleteArgs)
            } catch (e: Exception) {
                e.printStackTrace()
            }

            // 2. Determine semester end date for recurrence (e.g., Dec 31 for odd sem, June 30 for even sem)
            val now = LocalDate.now()
            val semEndDate = if (now.monthValue in 1..6) {
                LocalDate.of(now.year, 6, 30)
            } else {
                LocalDate.of(now.year, 12, 31)
            }
            val safeSemEndDate = if (semEndDate.isBefore(now)) now.plusMonths(5) else semEndDate
            val untilDateStr = String.format(
                java.util.Locale.US,
                "%04d%02d%02dT235959Z",
                safeSemEndDate.year,
                safeSemEndDate.monthValue,
                safeSemEndDate.dayOfMonth
            )

            val zoneId = ZoneId.systemDefault()
            val timeZoneId = TimeZone.getDefault().id.ifBlank { "Asia/Kolkata" }
            var syncedCount = 0

            // 3. Insert each class as a weekly recurring event
            for (daySchedule in timetable.days) {
                val dayOfWeek = daySchedule.dayOfWeek
                val byDayStr = toRruleDay(dayOfWeek)

                // First occurrence of this day of week starting from today
                val firstOccurrenceDate = now.with(TemporalAdjusters.nextOrSame(dayOfWeek))

                for (item in daySchedule.classes) {
                    val startTime = try {
                        LocalTime.parse(item.startTime, DateTimeFormatter.ofPattern("HH:mm"))
                    } catch (e: Exception) {
                        LocalTime.of(9, 0)
                    }

                    val endTime = try {
                        LocalTime.parse(item.endTime, DateTimeFormatter.ofPattern("HH:mm"))
                    } catch (e: Exception) {
                        startTime.plusHours(1)
                    }

                    val startZdt = ZonedDateTime.of(firstOccurrenceDate, startTime, zoneId)
                    val startMillis = startZdt.toInstant().toEpochMilli()

                    val durationSeconds = java.time.Duration.between(startTime, endTime).seconds.coerceAtLeast(60L)
                    val durationStr = "P${durationSeconds}S"

                    val typeLabel = item.classType.name.lowercase().replaceFirstChar { it.uppercase() }
                    val title = "${item.subjectName} ($typeLabel)"

                    val description = buildString {
                        appendLine("Subject Code: ${item.subjectCode}")
                        if (item.facultyName.isNotBlank()) appendLine("Instructor: ${item.facultyName}")
                        appendLine("Timing: ${item.timeSlotDisplay}")
                        if (item.room.isNotBlank()) appendLine("Venue: ${item.room}")
                        appendLine()
                        appendLine("Automatically synced by NSync")
                        append(SYNC_TAG)
                    }

                    val eventValues = ContentValues().apply {
                        put(CalendarContract.Events.CALENDAR_ID, calendarId)
                        put(CalendarContract.Events.TITLE, title)
                        put(CalendarContract.Events.DESCRIPTION, description)
                        if (item.room.isNotBlank()) {
                            put(CalendarContract.Events.EVENT_LOCATION, item.room)
                        }
                        put(CalendarContract.Events.DTSTART, startMillis)
                        // IMPORTANT: In Android CalendarProvider, recurring events must use DURATION instead of DTEND
                        put(CalendarContract.Events.DURATION, durationStr)
                        // IMPORTANT: RRULE must NOT have "RRULE:" prefix
                        put(CalendarContract.Events.RRULE, "FREQ=WEEKLY;BYDAY=$byDayStr;UNTIL=$untilDateStr")
                        put(CalendarContract.Events.EVENT_TIMEZONE, timeZoneId)
                        put(CalendarContract.Events.HAS_ALARM, 1)
                    }

                    val uri: Uri? = contentResolver.insert(CalendarContract.Events.CONTENT_URI, eventValues)
                    val eventId = uri?.lastPathSegment?.toLongOrNull()

                    if (uri != null) {
                        syncedCount++
                    }

                    // Add 10-minute advance notification reminder
                    if (eventId != null) {
                        try {
                            val reminderValues = ContentValues().apply {
                                put(CalendarContract.Reminders.EVENT_ID, eventId)
                                put(CalendarContract.Reminders.MINUTES, 10)
                                put(CalendarContract.Reminders.METHOD, CalendarContract.Reminders.METHOD_ALERT)
                            }
                            contentResolver.insert(CalendarContract.Reminders.CONTENT_URI, reminderValues)
                        } catch (_: Exception) {}
                    }
                }
            }

            CalendarSyncResult.Success(syncedCount, accountName)
        } catch (e: Exception) {
            e.printStackTrace()
            CalendarSyncResult.Error(e.localizedMessage ?: "Unknown calendar error")
        }
    }

    private fun toRruleDay(day: DayOfWeek): String = when (day) {
        DayOfWeek.MONDAY -> "MO"
        DayOfWeek.TUESDAY -> "TU"
        DayOfWeek.WEDNESDAY -> "WE"
        DayOfWeek.THURSDAY -> "TH"
        DayOfWeek.FRIDAY -> "FR"
        DayOfWeek.SATURDAY -> "SA"
        DayOfWeek.SUNDAY -> "SU"
    }
}
