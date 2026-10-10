package com.example.nsutconnect.data.model

import java.time.DayOfWeek
import java.time.LocalTime
import java.time.format.DateTimeFormatter

enum class ClassType {
    LECTURE,
    LAB,
    TUTORIAL
}

enum class ClassScheduleStatus {
    FINISHED,
    ONGOING,
    UPCOMING
}

data class ClassScheduleItem(
    val id: String,
    val dayOfWeek: DayOfWeek,
    val subjectCode: String,
    val subjectName: String,
    val roomNumber: String,
    val facultyName: String,
    val startTime: String, // format "09:00"
    val endTime: String,   // format "10:00"
    val classType: ClassType = ClassType.LECTURE
) {
    val room: String
        get() = roomNumber

    val startTimeFormatted: String
        get() = formatTo12Hour(startTime)

    val endTimeFormatted: String
        get() = formatTo12Hour(endTime)

    val timeSlotDisplay: String
        get() = "$startTimeFormatted – $endTimeFormatted"

    fun isOngoingAt(time: LocalTime): Boolean {
        return try {
            val start = LocalTime.parse(startTime, DateTimeFormatter.ofPattern("HH:mm"))
            val end = LocalTime.parse(endTime, DateTimeFormatter.ofPattern("HH:mm"))
            !time.isBefore(start) && time.isBefore(end)
        } catch (e: Exception) {
            false
        }
    }

    fun isUpcomingAt(time: LocalTime): Boolean {
        return try {
            val start = LocalTime.parse(startTime, DateTimeFormatter.ofPattern("HH:mm"))
            time.isBefore(start)
        } catch (e: Exception) {
            false
        }
    }

    fun isFinishedAt(time: LocalTime): Boolean {
        return try {
            val end = LocalTime.parse(endTime, DateTimeFormatter.ofPattern("HH:mm"))
            !time.isBefore(end) // currentTime >= end
        } catch (e: Exception) {
            false
        }
    }

    fun getStatusAt(time: LocalTime): ClassScheduleStatus {
        return when {
            isOngoingAt(time) -> ClassScheduleStatus.ONGOING
            isFinishedAt(time) -> ClassScheduleStatus.FINISHED
            else -> ClassScheduleStatus.UPCOMING
        }
    }

    companion object {
        fun formatTo12Hour(timeStr: String): String {
            return try {
                val time = LocalTime.parse(timeStr, DateTimeFormatter.ofPattern("HH:mm"))
                time.format(DateTimeFormatter.ofPattern("h:mm a", java.util.Locale.ENGLISH))
            } catch (e: Exception) {
                timeStr
            }
        }
    }
}

data class DaySchedule(
    val dayOfWeek: DayOfWeek,
    val classes: List<ClassScheduleItem>
)

data class WeeklyTimetable(
    val studentBranch: String,
    val section: String,
    val semester: String,
    val days: List<DaySchedule>,
    val lastSyncedTimestamp: Long = System.currentTimeMillis()
) {
    fun getScheduleFor(day: DayOfWeek): List<ClassScheduleItem> {
        return days.firstOrNull { it.dayOfWeek == day }?.classes ?: emptyList()
    }

    fun getCurrentOrNextClass(day: DayOfWeek, currentTime: LocalTime): Pair<ClassScheduleItem?, Boolean> {
        val todayClasses = getScheduleFor(day).sortedBy { it.startTime }
        val ongoing = todayClasses.firstOrNull { it.isOngoingAt(currentTime) }
        if (ongoing != null) {
            return Pair(ongoing, true) // true means currently active
        }
        val upcoming = todayClasses.firstOrNull { it.isUpcomingAt(currentTime) }
        return Pair(upcoming, false) // false means upcoming next
    }

    fun getScheduleRowsFor(day: DayOfWeek): List<ScheduleRow> {
        val classes = getScheduleFor(day).sortedBy { it.startTime }
        if (classes.isEmpty()) return emptyList()

        val rows = mutableListOf<ScheduleRow>()
        for (i in classes.indices) {
            val current = classes[i]
            rows.add(ScheduleRow.ClassRow(current))
            if (i < classes.size - 1) {
                val next = classes[i + 1]
                try {
                    val currentEnd = LocalTime.parse(current.endTime, DateTimeFormatter.ofPattern("HH:mm"))
                    val nextStart = LocalTime.parse(next.startTime, DateTimeFormatter.ofPattern("HH:mm"))
                    val gapMinutes = java.time.Duration.between(currentEnd, nextStart).toMinutes()
                    if (gapMinutes >= 15) {
                        rows.add(
                            ScheduleRow.BreakRow(
                                startTime = current.endTime,
                                endTime = next.startTime,
                                durationMinutes = gapMinutes
                            )
                        )
                    }
                } catch (_: Exception) {}
            }
        }
        return rows
    }

    fun isDayCompleted(day: DayOfWeek, currentTime: LocalTime): Boolean {
        if (day == DayOfWeek.SATURDAY || day == DayOfWeek.SUNDAY) return true
        val classes = getScheduleFor(day)
        if (classes.isEmpty()) return true
        return classes.all { it.isFinishedAt(currentTime) }
    }

    fun getAutoDefaultDay(today: DayOfWeek, currentTime: LocalTime): DayOfWeek {
        if (today == DayOfWeek.SATURDAY || today == DayOfWeek.SUNDAY) return DayOfWeek.MONDAY
        if (isDayCompleted(today, currentTime)) {
            val tomorrow = today.plus(1)
            if (tomorrow == DayOfWeek.SATURDAY || tomorrow == DayOfWeek.SUNDAY) return DayOfWeek.MONDAY
            val tomorrowClasses = getScheduleFor(tomorrow)
            if (tomorrowClasses.isEmpty()) {
                return DayOfWeek.MONDAY
            }
            return tomorrow
        }
        return today
    }
}

sealed class ScheduleRow {
    data class ClassRow(val item: ClassScheduleItem) : ScheduleRow()
    data class BreakRow(
        val startTime: String,
        val endTime: String,
        val durationMinutes: Long
    ) : ScheduleRow() {
        val startTimeFormatted: String
            get() = ClassScheduleItem.formatTo12Hour(startTime)

        val endTimeFormatted: String
            get() = ClassScheduleItem.formatTo12Hour(endTime)

        val timeSlotDisplay: String
            get() = "$startTimeFormatted – $endTimeFormatted"

        val durationFormatted: String
            get() {
                return if (durationMinutes >= 60) {
                    val hrs = durationMinutes / 60
                    val mins = durationMinutes % 60
                    if (mins == 0L) {
                        if (hrs == 1L) "1 hr" else "$hrs hrs"
                    } else {
                        "${hrs}h ${mins}m"
                    }
                } else {
                    "$durationMinutes min"
                }
            }

        fun isOngoingAt(time: LocalTime): Boolean {
            return try {
                val start = LocalTime.parse(startTime, DateTimeFormatter.ofPattern("HH:mm"))
                val end = LocalTime.parse(endTime, DateTimeFormatter.ofPattern("HH:mm"))
                !time.isBefore(start) && time.isBefore(end)
            } catch (e: Exception) {
                false
            }
        }

        fun isFinishedAt(time: LocalTime): Boolean {
            return try {
                val end = LocalTime.parse(endTime, DateTimeFormatter.ofPattern("HH:mm"))
                !time.isBefore(end)
            } catch (e: Exception) {
                false
            }
        }
    }
}

