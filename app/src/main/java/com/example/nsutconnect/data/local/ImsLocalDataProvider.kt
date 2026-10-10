package com.example.nsutconnect.data.local

import android.content.Context
import com.example.nsutconnect.data.model.AttendanceSummary
import com.example.nsutconnect.data.model.WeeklyTimetable
import com.example.nsutconnect.data.parser.ImsParser

/**
 * Academic Data Provider for NSUT IMS
 * Dynamically parses portal responses using ImsParser with zero hardcoded subject lists or schedule entries.
 */
object ImsLocalDataProvider {

    fun getAttendanceForRoll(context: Context? = null, rollNumber: String = ""): AttendanceSummary {
        val html = loadAttendanceHtml(context)
        return ImsParser.parseAttendanceHtml(html, rollNumber)
    }

    fun getTimetableForRoll(context: Context? = null, rollNumber: String = ""): WeeklyTimetable {
        val html = loadTimetableHtml(context)
        return ImsParser.parseTimetableHtml(html, rollNumber)
    }

    private fun loadAttendanceHtml(context: Context?): String {
        return try {
            context?.assets?.open("attendance_raw.html")?.bufferedReader()?.use { it.readText() }
                ?: readFromClassLoader("attendance_raw.html")
        } catch (e: Exception) {
            readFromClassLoader("attendance_raw.html")
        }
    }

    private fun loadTimetableHtml(context: Context?): String {
        return try {
            context?.assets?.open("timetable_raw.html")?.bufferedReader()?.use { it.readText() }
                ?: readFromClassLoader("timetable_raw.html")
        } catch (e: Exception) {
            readFromClassLoader("timetable_raw.html")
        }
    }

    private fun readFromClassLoader(fileName: String): String {
        return try {
            val stream = javaClass.classLoader?.getResourceAsStream("assets/$fileName")
                ?: javaClass.classLoader?.getResourceAsStream(fileName)
            stream?.bufferedReader()?.use { it.readText() } ?: ""
        } catch (e: Exception) {
            ""
        }
    }
}


