package com.example.nsutconnect.data.model

import java.time.LocalDate

data class ExamDatesheetItem(
    val courseCode: String,
    val courseName: String,
    val date: String,
    val day: String,
    val timeSlot: String,
    val shift: String,
    val isUpcoming: Boolean = false,
    val credits: Int = 4,
    val examType: String = "MID_SEM", // "MID_SEM" or "END_SEM"
    val examDateRaw: LocalDate? = null,
    val roomAllotment: String = "Room 113/6 (Desk 16)",
    val reportingTime: String = "09:30 AM (30 min prior)",
    val examBlock: String = "APJ Abdul Kalam Block (Block 6)"
) {
    /**
     * At NSUT, seating plans and room allocations are officially published by the
     * Examination Division 1 day before the exam. For any future exams (> 1 day away)
     * or exams where seating was published per-day on notice board, room numbers
     * indicate either the verified room or release 1 day prior.
     */
    val isRoomReleased: Boolean
        get() {
            if (roomAllotment.contains("1 day prior", ignoreCase = true)) return false
            val d = examDateRaw ?: return true
            val today = LocalDate.now()
            return !d.isAfter(today.plusDays(1))
        }

    val shortRoom: String
        get() = if (isRoomReleased) {
            roomAllotment.substringBefore("(").trim()
        } else {
            "1 day prior"
        }

    val roomDisplay: String
        get() = if (isRoomReleased) {
            shortRoom
        } else {
            "Releases 1 day prior"
        }

    val deskNumber: String
        get() {
            if (!isRoomReleased) return "Released with seating plan"
            val start = roomAllotment.indexOf("(")
            val end = roomAllotment.indexOf(")")
            return if (start != -1 && end != -1 && end > start) {
                roomAllotment.substring(start + 1, end).trim()
            } else {
                "Assigned in room"
            }
        }
}

data class SeatingPlan(
    val rollNumber: String,
    val studentName: String,
    val examination: String,
    val examHall: String,
    val roomNumber: String,
    val floor: String,
    val row: String,
    val seatNumber: String,
    val reportingTime: String,
    val shift: String,
    val invigilatorDivision: String,
    val guidelines: List<String>,
    val columnNumber: Int = 2,
    val rowNumber: Int = 8,
    val columnCount: Int = 4,
    val rowCount: Int = 8,
    val totalCapacity: Int = 32,
    val deskMatrix: List<List<String>> = emptyList(),
    val officialNoticeRef: String = "Main Campus Theory Exam 28-09-2026 Shift 1 (Page 23)",
    val pdfDownloadUrl: String = "https://www.imsnsit.org/imsnsit/plum_url.php?Yk8XilDXI97wwdgTG2RE35iOgpFxLwm4iEDTm+z17FUDsiikgei0RHNOh+y2gAfT+W3kH9BVzykcvRkVh/+vStK7qk//+0veoS0HJ5VdJnJ5fPFP2d1v9XvSDbRzK9PAsb66As8dJxSDZRMxSyDL1B8CLM8JSrWfh+C6RIf0nIgeeip3p0EkmIsKm8JRQgHiIZr0QO0+DelJcebQk2DAIQ==",
    val alternateDayRoom: String = "Room 305/5 (Block 5, 3rd Floor - Shift 1)"
)
