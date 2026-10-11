package com.example.nsutconnect.data.local

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.core.content.FileProvider
import com.example.nsutconnect.data.model.*
import com.example.nsutconnect.data.network.ResultHubNetworkClient
import java.io.File
import java.io.FileOutputStream
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale

object StudentRecordsProvider {

    private val resultHubClient = ResultHubNetworkClient()
    private var cachedAcademicRecord: StudentAcademicRecord? = null

    suspend fun getOrFetchAcademicRecord(
        rollNumber: String = "",
        forceRefresh: Boolean = false
    ): StudentAcademicRecord {
        if (!forceRefresh && cachedAcademicRecord != null && cachedAcademicRecord?.rollNumber == rollNumber) {
            return cachedAcademicRecord!!
        }

        val remoteRecord = if (rollNumber.isNotBlank()) resultHubClient.fetchStudentAcademicRecord(rollNumber) else null
        if (remoteRecord != null) {
            cachedAcademicRecord = remoteRecord
            return remoteRecord
        }

        return getAcademicRecord(rollNumber)
    }

    fun getAcademicRecord(rollNumber: String = ""): StudentAcademicRecord {
        cachedAcademicRecord?.let {
            if (it.rollNumber == rollNumber) return it
        }

        if (rollNumber.isNotBlank() && !rollNumber.equals("2024UCO1500", ignoreCase = true)) {
            return StudentAcademicRecord(
                rollNumber = rollNumber,
                name = "Student",
                program = "B.Tech.",
                cumulativeGpa = 0.0f,
                totalCreditsCompleted = 0,
                totalCoursesCompleted = 0,
                rankPercentile = "",
                semesters = emptyList(),
                collegeRank = null,
                branchRank = null,
                percentile = null,
                branchCode = "",
                yearOfStudy = "",
                gradeDistribution = emptyMap(),
                isLiveFromResultHub = false
            )
        }

        val sem1 = SemesterResult(
            semesterNumber = 1,
            session = "Odd Sem (2025-26)",
            sgpa = 3.80f,
            creditsEarned = 20,
            totalCredits = 20,
            courses = listOf(
                CourseGrade("FCMT0101", "Mathematics - I", 4, "B+", 7),
                CourseGrade("FCCH0103", "Environmental Science and Green Chemistry", 4, "D", 4),
                CourseGrade("FCEE0106", "Fundamentals of Electrical Engineering", 4, "D", 4),
                CourseGrade("FCHS0105", "English", 4, "D", 4),
                CourseGrade("FCME0116", "Engineering Graphics & CAD", 4, "F", 0, "FAIL"),
                CourseGrade("VAPD0115", "Yoga", 0, "F", 0, "FAIL")
            )
        )

        val sem2 = SemesterResult(
            semesterNumber = 2,
            session = "Even Sem (2025-26)",
            sgpa = 6.83f,
            creditsEarned = 24,
            totalCredits = 24,
            courses = listOf(
                CourseGrade("FCMT0201", "Mathematics - II", 4, "A", 8),
                CourseGrade("FCPH0124", "Oscillations, Waves and Optics", 4, "A", 8),
                CourseGrade("VTITC203", "Data Structures and Algorithms", 4, "A", 8),
                CourseGrade("FCCS0102", "Computer Programming", 4, "B", 6),
                CourseGrade("VTECC202", "Electronic Devices and Circuits", 4, "B", 6),
                CourseGrade("VTECC201", "Network Analysis and Synthesis", 4, "C", 5)
            )
        )

        val totalCredits = 44
        val allCourses = sem1.courses.size + sem2.courses.size
        val cgpa = 5.45f

        return StudentAcademicRecord(
            rollNumber = rollNumber,
            name = "STUDENT",
            program = "B.Tech.",
            cumulativeGpa = cgpa,
            totalCreditsCompleted = totalCredits,
            totalCoursesCompleted = allCourses,
            rankPercentile = "#59 in UEV · #1691 College",
            semesters = listOf(sem2, sem1), // latest first
            collegeRank = 1691,
            branchRank = 59,
            percentile = 19.94f,
            branchCode = "UEV",
            yearOfStudy = "2025",
            gradeDistribution = mapOf(
                "A" to 3,
                "B+" to 1,
                "B" to 2,
                "C" to 1,
                "D" to 3,
                "F" to 2
            ),
            isLiveFromResultHub = false
        )
    }

    private data class RegisteredCourse(
        val code: String,
        val name: String,
        val credits: Int = 4
    )

    private val DEFAULT_COURSES = listOf(
        RegisteredCourse("VTECC302", "Signals and Systems", 4),
        RegisteredCourse("VTECC303", "Probability Theory and Random Process", 4),
        RegisteredCourse("VTECC304", "Microelectronics", 4),
        RegisteredCourse("VTECC305", "Digital Circuits and Systems", 4),
        RegisteredCourse("VTPHC301", "Solid State Physics", 4),
        RegisteredCourse("VANM0303", "Innovation, Business Models and Entrepreneurship", 2)
    )

    private fun getRegisteredCourses(context: Context?): List<RegisteredCourse> {
        if (context == null) return DEFAULT_COURSES
        try {
            val userPrefs = UserPreferences(context)
            val cachedTimetable = userPrefs.getCachedTimetable()
            if (cachedTimetable != null) {
                val courses = cachedTimetable.days
                    .flatMap { it.classes }
                    .filter { it.subjectCode.isNotBlank() && !it.subjectCode.equals("LUNCH", ignoreCase = true) }
                    .distinctBy { it.subjectCode }
                    .map { RegisteredCourse(it.subjectCode, it.subjectName, 4) }
                if (courses.isNotEmpty()) return courses
            }

            val attendance = userPrefs.getCachedAttendance()
            if (attendance != null) {
                val courses = attendance.records
                    .filter { it.subjectCode.isNotBlank() }
                    .distinctBy { it.subjectCode }
                    .map { RegisteredCourse(it.subjectCode, it.subjectName, 4) }
                if (courses.isNotEmpty()) return courses
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return DEFAULT_COURSES
    }

    fun getExamDatesheet(context: Context? = null): List<ExamDatesheetItem> {
        val endSem = getEndSemDatesheet(context)
        return if (endSem.isNotEmpty()) endSem else getMidSemDatesheet(context)
    }

    fun getMidSemDatesheet(context: Context? = null): List<ExamDatesheetItem> {
        if (context != null) {
            val cached = UserPreferences(context).getCachedDatesheet("MID_SEM")
            if (!cached.isNullOrEmpty()) {
                return cached
            }
        }

        val today = LocalDate.now()
        val year = if (today.year >= 2026) today.year else 2026
        val dateFormatter = DateTimeFormatter.ofPattern("dd MMM yyyy", Locale.ENGLISH)

        // Authentic Official Mid-Sem Theory Examinations for 3rd Sem ECE (VLSI Design and Technology):
        // Sourced directly from "Revised Final Mid Sem Date Sheet ODD Sem 2026-27" (NSUT Examination Division)
        // Shift 1: 10:00 AM - 11:30 AM
        val schedule = listOf(
            ExamScheduleEntry(
                code = "VTPHC301",
                name = "Solid State Physics",
                date = LocalDate.of(year, 9, 21),
                room = "Released 1 day prior",
                hall = "Main Campus",
                shift = "Shift 1 (Morning)"
            ),
            ExamScheduleEntry(
                code = "VTECC302",
                name = "Signals and Systems",
                date = LocalDate.of(year, 9, 22),
                room = "Released 1 day prior",
                hall = "Main Campus",
                shift = "Shift 1 (Morning)"
            ),
            ExamScheduleEntry(
                code = "VTECC303",
                name = "Probability Theory and Random Process",
                date = LocalDate.of(year, 9, 24),
                room = "Released 1 day prior",
                hall = "Main Campus",
                shift = "Shift 1 (Morning)"
            ),
            ExamScheduleEntry(
                code = "VTECC304",
                name = "Microelectronics",
                date = LocalDate.of(year, 9, 28),
                room = "Desk allotment on IMS portal",
                hall = "Main Campus",
                shift = "Shift 1 (Morning)"
            ),
            ExamScheduleEntry(
                code = "VTECC305",
                name = "Digital Circuits and Systems",
                date = LocalDate.of(year, 9, 29),
                room = "Desk allotment on IMS portal",
                hall = "Main Campus",
                shift = "Shift 1 (Morning)"
            )
        )

        return schedule.map { entry ->
            val isUpcoming = !entry.date.isBefore(today)
            ExamDatesheetItem(
                courseCode = entry.code,
                courseName = entry.name,
                date = entry.date.format(dateFormatter),
                day = entry.date.dayOfWeek.getDisplayName(TextStyle.FULL, Locale.ENGLISH),
                timeSlot = "10:00 AM - 11:30 AM",
                shift = entry.shift,
                isUpcoming = isUpcoming,
                credits = 4,
                examType = "MID_SEM",
                examDateRaw = entry.date,
                roomAllotment = entry.room,
                reportingTime = "09:30 AM (30 min prior)",
                examBlock = entry.hall
            )
        }
    }

    fun openOfficialDatesheetPdf(context: Context) {
        try {
            val cacheFile = File(context.cacheDir, "nsut_datesheet_midsem_odd_2026.pdf")
            if (!cacheFile.exists() || cacheFile.length() == 0L) {
                context.assets.open("datesheet_midsem_odd_2026.pdf").use { input ->
                    FileOutputStream(cacheFile).use { output ->
                        input.copyTo(output)
                    }
                }
            }
            val uri: Uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                cacheFile
            )
            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(uri, "application/pdf")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(Intent.createChooser(intent, "Open Official NSUT Datesheet PDF"))
        } catch (e: Exception) {
            Toast.makeText(context, "Could not open datesheet PDF: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    fun getEndSemDatesheet(context: Context? = null): List<ExamDatesheetItem> {
        if (context != null) {
            val cached = UserPreferences(context).getCachedDatesheet("END_SEM")
            if (!cached.isNullOrEmpty()) {
                return cached
            }
        }
        // End-Semester datesheet has not been officially released by NSUT Examination Division yet.
        // It will be dynamically populated once notified on the IMS portal.
        return emptyList()
    }

    fun getSeatingPlan(rollNumber: String = "STUDENT"): SeatingPlan {
        val isRohit = rollNumber.equals("2024UCO1500", ignoreCase = true)
        val displayRoom = if (isRohit) "Room 113/6 (28 Sep) & Room 305/5 (29 Sep)" else "Desk allotment notified 1 day prior"
        val displaySeat = if (isRohit) "Desk 16 (Row 8, Col 2 on 28 Sep) / Desk 3 on 29 Sep" else "Assigned by Examination Division"
        val displayRow = if (isRohit) "Row 8 (28 Sep) / Row 1 (29 Sep)" else "As per Notice"
        val displayFloor = if (isRohit) "Block 6 (1st Fl.) / Block 5 (3rd Fl.)" else "Main Campus Examination Block"
        val displayHall = if (isRohit) "APJ Abdul Kalam Block (Block 6) & Block 5" else "Main Campus Examination Hall"
        val displayDivision = if (isRohit) "Division of Electronics & Communication Engineering (ECE)" else "NSUT Examination Division"

        return SeatingPlan(
            rollNumber = rollNumber,
            studentName = if (rollNumber != "STUDENT") rollNumber else "STUDENT",
            examination = "Mid-Semester Theory Examination (Sep 2026)",
            examHall = displayHall,
            roomNumber = displayRoom,
            floor = displayFloor,
            row = displayRow,
            seatNumber = displaySeat,
            reportingTime = "09:30 AM (30 min prior)",
            shift = "Shift 1 (10:00 AM - 11:30 AM)",
            invigilatorDivision = displayDivision,
            guidelines = listOf(
                "Physical Hall Ticket / Admit Card and NSUT Student ID Card are mandatory.",
                "Students must check their pockets and belongings to ensure no written notes, mobile phones, or smartwatches are brought into the Examination Hall.",
                "Verify that nothing is written on desk, geometry box, cardboard, or calculator.",
                "Seating plans and room allocations are released 1 day prior to each examination by the Examination Section."
            ),
            columnNumber = if (isRohit) 2 else 0,
            rowNumber = if (isRohit) 8 else 0,
            columnCount = 4,
            rowCount = 8,
            totalCapacity = 32,
            deskMatrix = if (isRohit) {
                listOf(
                    listOf("2025UEV2858", "2025UEV2869", "2025UEV4575", "2025UEV4648"),
                    listOf("2025UEV2859", "2025UEV2870", "2025UEV4590", "2025UEV4654"),
                    listOf("2025UEV2860", "2025UEV2872", "2025UEV4592", "2025UEV4659"),
                    listOf("2025UEV2861", "2025UEV4505", "2025UEV4601", "2025UEV4674"),
                    listOf("2025UEV2863", "2025UEV4519", "2025UEV4605", "2025UEV6005"),
                    listOf("2025UEV2864", "2025UEV4521", "2025UEV4611", "2025UEV6521"),
                    listOf("2025UEV2865", "2025UEV4544", "2025UEV4621", "2025UEV6554"),
                    listOf("2025UEV2866", rollNumber.ifBlank { "YOUR SEAT" }, "2025UEV4644", "2024UEV2828")
                )
            } else emptyList(),
            officialNoticeRef = if (isRohit) "Main Campus Theory Exam 28-09-2026 Shift 1 (Page 23) & 29-09-2026 Shift 1 (Page 21)" else "Official Exam Notice",
            pdfDownloadUrl = "https://www.imsnsit.org/imsnsit/student_login.php",
            alternateDayRoom = if (isRohit) "Room 305/5 (Block 5, 3rd Floor - Shift 1 on 29 Sep)" else ""
        )
    }

private data class ExamScheduleEntry(
    val code: String,
    val name: String,
    val date: LocalDate,
    val room: String,
    val hall: String,
    val shift: String
)

    fun getPersonalizedNotices(): List<NoticeItem> {
        return listOf(
            NoticeItem(
                id = "NOT-2026-655",
                title = "ACADEMIC/ ACTIVITY CALENDAR FOR ALL PROGRAMS DURING JULY 2026 TO DECEMBER 2026 (ODD SEMESTER)",
                date = "20-07-2026",
                category = "Academics",
                targetDepartment = "All B.Tech Programs (Odd Semester 2026-27)",
                aiSummary = "Official University Circular F.No. 220(57)/Acad./NSUT/655 approved by Dean Academics. End-Sem exams begin 23 Nov 2026. View authentic PDF in app.",
                isRelevant = true,
                isUrgent = true,
                downloadUrl = "https://www.imsnsit.org/imsnsit/notifications.php"
            ),
            NoticeItem(
                id = "NOT-2026-092",
                title = "Main Campus | Seating Plan for (Mid Sem.) Theory Examination",
                date = "28-09-2026",
                category = "Examinations",
                targetDepartment = "All UG Students (B.Tech)",
                aiSummary = "Official seating allotment from IMS. Room and desk allocations are posted by the Examination Section 1 day prior to each examination.",
                isRelevant = true,
                isUrgent = true,
                downloadUrl = "https://www.imsnsit.org/imsnsit/notifications.php"
            ),
            NoticeItem(
                id = "NOT-2026-088",
                title = "Participation in India Mobile Congress (IMC) 2026 at Yashobhoomi, Dwarka from 7-10 October",
                date = "01-10-2026",
                category = "Departmental",
                targetDepartment = "Electronics and Communication Engineering",
                aiSummary = "ECE students selected to participate in Asia's largest telecom and frontier tech expo. Attendance waiver granted.",
                isRelevant = true,
                isUrgent = false,
                downloadUrl = "https://www.imsnsit.org/imsnsit/notifications.php"
            ),
            NoticeItem(
                id = "NOT-2026-085",
                title = "One Week Online Workshop 'THE PHOTONICS SPECTRUM 2026' from 01 Oct - 07 Oct, 2026",
                date = "29-09-2026",
                category = "Workshops",
                targetDepartment = "ECE & VLSI Department",
                aiSummary = "Hands-on photonic circuits, semiconductor waveguides, and optoelectronic communication modeling.",
                isRelevant = true,
                isUrgent = false,
                downloadUrl = "https://www.imsnsit.org/imsnsit/notifications.php"
            ),
            NoticeItem(
                id = "NOT-2026-081",
                title = "E-DISTRICT SCHOLARSHIP & SIEMENS SCHOLARSHIP FOR THE YEAR 2026-27",
                date = "01-10-2026",
                category = "Scholarships",
                targetDepartment = "All Academic Sections UG",
                aiSummary = "Online application portals open for post-matric state scholarship and Siemens engineering merit grants.",
                isRelevant = true,
                isUrgent = false,
                downloadUrl = "https://www.imsnsit.org/imsnsit/notifications.php"
            ),
            NoticeItem(
                id = "NOT-2026-077",
                title = "Registration schedule for 3rd, 5th and 7th semester ODD 2026",
                date = "21-07-2026",
                category = "Academics",
                targetDepartment = "B.Tech UG Continuing Batches",
                aiSummary = "Course elective preference registration and fee clearance verification on IMS portal.",
                isRelevant = true,
                isUrgent = false,
                downloadUrl = "https://www.imsnsit.org/imsnsit/notifications.php"
            ),
            NoticeItem(
                id = "NOT-2026-072",
                title = "Annual Cultural Festival 'Moksha' Dates & Student Committee Nominations",
                date = "25-09-2026",
                category = "Campus Life",
                targetDepartment = "All Students",
                aiSummary = "Dean Student Welfare Office releases official council committee selections for Moksha 2027.",
                isRelevant = false,
                isUrgent = false,
                downloadUrl = "https://www.imsnsit.org/imsnsit/notifications.php"
            ),
            NoticeItem(
                id = "NOT-2026-068",
                title = "COURSE EXIT FORM - FILLING for 3rd semester students SUBJECT-DATA STRUCTURE",
                date = "06-10-2026",
                category = "Academics",
                targetDepartment = "3rd Semester Students",
                aiSummary = "Mandatory course feedback survey before 09 October 2026 on IMS student portal.",
                isRelevant = true,
                isUrgent = false,
                downloadUrl = "https://www.imsnsit.org/imsnsit/notifications.php"
            )
        )
    }
}
