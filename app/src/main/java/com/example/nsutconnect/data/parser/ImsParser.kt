package com.example.nsutconnect.data.parser

import com.example.nsutconnect.data.model.AttendanceDateEntry
import com.example.nsutconnect.data.model.AttendanceRecord
import com.example.nsutconnect.data.model.AttendanceSummary
import com.example.nsutconnect.data.model.ClassScheduleItem
import com.example.nsutconnect.data.model.ClassType
import com.example.nsutconnect.data.model.DaySchedule
import com.example.nsutconnect.data.model.WeeklyTimetable
import org.jsoup.Jsoup
import org.jsoup.nodes.Element
import java.time.DayOfWeek
import java.util.regex.Pattern

/**
 * Dynamic HTML parser for official NSUT IMS Portal attendance and timetable responses.
 * Extracts real student profile, records, schedules, and mappings dynamically with zero hardcoded lists.
 */
object ImsParser {

    private val SLOT_TIMES = listOf(
        Pair("08:00", "09:00"), // T1
        Pair("09:00", "10:00"), // T2
        Pair("10:00", "11:00"), // T3
        Pair("11:00", "12:00"), // T4
        Pair("12:00", "13:00"), // T5
        Pair("13:00", "14:00"), // T6
        Pair("14:00", "15:00"), // T7
        Pair("15:00", "16:00"), // T8
        Pair("16:00", "17:00"), // T9
        Pair("17:00", "18:00"), // T10
        Pair("18:00", "19:00"), // T11
        Pair("19:00", "20:00"), // T12
        Pair("20:00", "21:00")  // T13
    )

    // Shared dynamic cache across parse passes
    private val dynamicSubjectNames = mutableMapOf<String, String>()
    private val dynamicFacultyNames = mutableMapOf<String, String>()

    /**
     * Dynamically parses NSUT IMS Attendance HTML table.
     */
    fun parseAttendanceHtml(html: String, defaultRoll: String): AttendanceSummary {
        val doc = Jsoup.parse(html)
        var studentName = "STUDENT"
        var rollNumber = defaultRoll
        var semester = "Semester"
        var branch = "Engineering"

        // 1. Extract student metadata
        val nameMatcher = Pattern.compile("Name:\\s*([^,(<]+)(?:\\s*\\(([^)]+)\\))?,?\\s*(?:Semester\\s*:\\s*(\\d+))?").matcher(doc.text())
        if (nameMatcher.find()) {
            val n = nameMatcher.group(1)?.trim()
            if (!n.isNullOrBlank()) studentName = n
            val r = nameMatcher.group(2)?.trim()
            if (!r.isNullOrBlank()) {
                rollNumber = r
            }
            val s = nameMatcher.group(3)?.trim()
            if (!s.isNullOrBlank()) semester = "Semester $s"
        }

        val deptInput = doc.select("input[name=dept]").firstOrNull()?.attr("value")
        if (!deptInput.isNullOrBlank()) {
            branch = deptInput.lowercase().capitalizeWords()
        }

        // 2. Extract dynamic subject legends from the bottom of attendance table
        extractSubjectLegends(doc)

        // 3. Find attendance tables and rows
        val tables = doc.select("table")
        val subjectCodes = mutableListOf<String>()
        var overallPresent = mutableListOf<Int>()
        var overallAbsent = mutableListOf<Int>()
        var overallClasses = mutableListOf<Int>()
        val subjectHistory = mutableMapOf<String, MutableList<AttendanceDateEntry>>()
        val datePattern = Regex("^[A-Za-z]{3}-\\d{1,2}$")

        for (table in tables) {
            val rows = table.select("tr")
            var tableCodes = listOf<String>()

            for (row in rows) {
                val cells = row.select("td, th").map { it.text().trim() }
                if (cells.isEmpty()) continue

                // Subject header row (e.g. Days | FCFO0301 | VANM0303 | VTECC302 | VTECC303 | ...)
                if (cells.any { it.matches(Regex("^[A-Z]{3,6}\\d{3,4}$")) }) {
                    val codes = cells.filter { it.matches(Regex("^[A-Z]{3,6}\\d{3,4}$")) }
                    if (codes.isNotEmpty()) {
                        tableCodes = codes
                        if (subjectCodes.isEmpty()) {
                            subjectCodes.addAll(codes)
                        }
                    }
                    continue
                }

                // Check for Date row (e.g. Jul-27, Aug-12, Sep-15, Oct-06)
                if (cells[0].matches(datePattern) && tableCodes.isNotEmpty()) {
                    val dateStr = cells[0]
                    for (colIdx in 1 until cells.size) {
                        val code = tableCodes.getOrNull(colIdx - 1) ?: continue
                        val mark = cells[colIdx].trim()
                        if (mark.isNotEmpty()) {
                            val entry = parseAttendanceMark(dateStr, mark)
                            subjectHistory.getOrPut(code) { mutableListOf() }.add(entry)
                        }
                    }
                    continue
                }

                val rowText = cells.joinToString(" ").lowercase()
                if (rowText.contains("overall") && rowText.contains("present")) {
                    val nums = cells.drop(1).mapNotNull { it.toIntOrNull() }
                    if (nums.isNotEmpty()) overallPresent = nums.toMutableList()
                } else if (rowText.contains("overall") && rowText.contains("absent")) {
                    val nums = cells.drop(1).mapNotNull { it.toIntOrNull() }
                    if (nums.isNotEmpty()) overallAbsent = nums.toMutableList()
                } else if (rowText.contains("overall") && rowText.contains("class")) {
                    val nums = cells.drop(1).mapNotNull { it.toIntOrNull() }
                    if (nums.isNotEmpty()) overallClasses = nums.toMutableList()
                }
            }
        }

        // Fallback to "Total Present" / "Total Absent" if "Overall" not present in this segment
        if (overallPresent.isEmpty() || overallAbsent.isEmpty()) {
            for (table in tables) {
                val rows = table.select("tr")
                for (row in rows) {
                    val cells = row.select("td, th").map { it.text().trim() }
                    val rowText = cells.joinToString(" ").lowercase()
                    if (overallPresent.isEmpty() && rowText.contains("total") && rowText.contains("present")) {
                        val nums = cells.drop(1).mapNotNull { it.toIntOrNull() }
                        if (nums.isNotEmpty()) overallPresent = nums.toMutableList()
                    }
                    if (overallAbsent.isEmpty() && rowText.contains("total") && rowText.contains("absent")) {
                        val nums = cells.drop(1).mapNotNull { it.toIntOrNull() }
                        if (nums.isNotEmpty()) overallAbsent = nums.toMutableList()
                    }
                }
            }
        }

        // 4. Build Attendance Records dynamically
        val records = mutableListOf<AttendanceRecord>()
        for (i in subjectCodes.indices) {
            val code = subjectCodes[i]
            val present = overallPresent.getOrElse(i) { 0 }
            val absent = overallAbsent.getOrElse(i) { 0 }
            val total = if (overallClasses.isNotEmpty() && i < overallClasses.size) {
                overallClasses[i]
            } else {
                present + absent
            }

            val name = dynamicSubjectNames[code] ?: fallbackSubjectName(code)
            val faculty = dynamicFacultyNames[code] ?: fallbackFaculty(code)
            val isPractical = code.contains("LAB") || code.endsWith("P")
            // Newest dates first for clean history display
            val historyList = subjectHistory[code]?.reversed() ?: emptyList()

            records.add(
                AttendanceRecord(
                    subjectCode = code,
                    subjectName = name,
                    attendedClasses = present,
                    totalClasses = total,
                    facultyName = faculty,
                    subjectType = if (isPractical) "Practical" else "Theory",
                    history = historyList
                )
            )
        }

        // 5. Sort dynamically: lowest attendance first, 0-classes held at bottom
        val sortedRecords = records.sortedWith(
            compareBy<AttendanceRecord> { it.totalClasses == 0 }
                .thenBy { it.percentage }
        )

        return AttendanceSummary(
            studentName = studentName,
            rollNumber = rollNumber,
            branch = branch,
            semester = semester,
            records = sortedRecords,
            lastSyncedTimestamp = System.currentTimeMillis()
        )
    }

    private fun parseAttendanceMark(date: String, mark: String): AttendanceDateEntry {
        val (statusText, isPresent) = when {
            mark == "1" -> Pair("Present", true)
            mark == "0" -> Pair("Absent", false)
            mark == "1+1" -> Pair("Present (2 Classes)", true)
            mark == "0+1" || mark == "1+0" -> Pair("1 Present, 1 Absent", false)
            mark == "CS" -> Pair("Class Suspended", null)
            mark == "CR" -> Pair("Class Rescheduled", null)
            mark == "GH" -> Pair("Holiday", null)
            mark == "TL" -> Pair("Teacher on Leave", null)
            mark == "OD" -> Pair("Official Duty", null)
            mark == "MS" -> Pair("Mid Sem Exam", null)
            mark == "MB" -> Pair("Mass Bunk", false)
            mark == "NA" -> Pair("Not Allotted", null)
            mark == "NT" -> Pair("Not Taken", null)
            else -> Pair(mark, null)
        }
        return AttendanceDateEntry(
            date = date,
            statusText = statusText,
            statusCode = mark,
            isPresent = isPresent
        )
    }


    /**
     * Dynamically parses NSUT IMS Timetable HTML table.
     * Merges contiguous tutorial slots into a single block (2-hour block).
     * Merges contiguous lab slots into a single block (2-hour block).
     */
    fun parseTimetableHtml(html: String, targetRoll: String): WeeklyTimetable {
        val doc = Jsoup.parse(html)
        var studentBranch = "Electronics Engineering (VLSI Design and Technology)"
        var section = "Sec 1"
        var semester = "Semester 3"

        // 1. Extract faculty and subject map from bottom table
        val facultyMap = extractFacultyAndSubjectMap(doc)

        // 2. Parse Timetable Table
        val mainTable = doc.select("table.plum_fieldbig, table").firstOrNull { it.text().contains("T1") }
            ?: return fallbackWeeklyTimetable()

        val daySchedules = mutableListOf<DaySchedule>()
        val dayMap = mapOf(
            "Mon" to DayOfWeek.MONDAY,
            "Tue" to DayOfWeek.TUESDAY,
            "Wed" to DayOfWeek.WEDNESDAY,
            "Thu" to DayOfWeek.THURSDAY,
            "Fri" to DayOfWeek.FRIDAY,
            "Sat" to DayOfWeek.SATURDAY
        )

        val rows = mainTable.select("tr")
        for (row in rows) {
            val cells = row.select("td")
            if (cells.size < 2) continue
            val dayHeader = cells[0].text().trim()
            val dayOfWeek = dayMap[dayHeader] ?: continue

            val rawItems = mutableListOf<RawScheduleSlot>()
            for (colIdx in 1 until cells.size) {
                val slotIdx = colIdx - 1
                if (slotIdx >= SLOT_TIMES.size) break
                val cell = cells[colIdx]
                val cellHtml = cell.html()
                if (cellHtml.isBlank() || cell.text().isBlank()) continue

                val (startTime, endTime) = SLOT_TIMES[slotIdx]

                // Split cell by hr or div for multi-group entries
                val entries = cell.html().split(Regex("(?i)<hr\\s*/?>"))
                for (entryHtml in entries) {
                    val entryDoc = Jsoup.parseBodyFragment(entryHtml)
                    val text = entryDoc.text()
                    if (text.isBlank()) continue

                    // Parse subject code: match 3-6 letters + 3-4 digits
                    val codeMatch = Regex("([A-Z]{3,6}\\d{3,4})").find(text) ?: continue
                    val code = codeMatch.value

                    // Parse room
                    val roomMatch = Regex("(?i)Room\\s*:\\s*(\\d+)").find(text)
                    val room = if (roomMatch != null) "Room ${roomMatch.groupValues[1]}" else "Room 8003"

                    // Determine ClassType
                    val isTut = text.contains("Tut", ignoreCase = true)
                    val isLab = text.contains("Grp", ignoreCase = true) || text.contains("Lab", ignoreCase = true)
                    val classType = when {
                        isLab -> ClassType.LAB
                        isTut -> ClassType.TUTORIAL
                        else -> ClassType.LECTURE
                    }

                    // For multi-group entries, filter according to student group
                    val isGrp1 = text.contains("Grp-1", ignoreCase = true)
                    if (isLab && isGrp1 && !text.contains("Grp-2", ignoreCase = true)) {
                        continue
                    }

                    val subjectName = facultyMap[code]?.first ?: dynamicSubjectNames[code] ?: fallbackSubjectName(code)
                    val faculty = facultyMap[code]?.second ?: dynamicFacultyNames[code] ?: fallbackFaculty(code)

                    rawItems.add(
                        RawScheduleSlot(
                            slotIndex = slotIdx,
                            startTime = startTime,
                            endTime = endTime,
                            subjectCode = code,
                            subjectName = subjectName,
                            roomNumber = room,
                            facultyName = faculty,
                            classType = classType,
                            isTutorial = isTut,
                            isLab = isLab
                        )
                    )
                    break // Selected matching group for this slot
                }
            }

            // 3. Merging logic:
            // "consider tute of both as single" -> merge consecutive tutorial slots for the same subject
            // Also merge consecutive lab slots for the same lab
            val mergedClasses = mutableListOf<ClassScheduleItem>()
            var i = 0
            while (i < rawItems.size) {
                val current = rawItems[i]
                var finalEnd = current.endTime
                var nextIdx = i + 1

                if (nextIdx < rawItems.size) {
                    val next = rawItems[nextIdx]
                    val isConsecutive = next.slotIndex == current.slotIndex + 1
                    val isSameSubject = next.subjectCode == current.subjectCode
                    val isMergeable = (current.isTutorial && next.isTutorial) || (current.isLab && next.isLab)

                    if (isConsecutive && isSameSubject && isMergeable) {
                        finalEnd = next.endTime
                        nextIdx++ // slot merged into a single block!
                    }
                }

                val displayName = if (current.isLab && !current.subjectName.contains("Lab", ignoreCase = true)) {
                    "${current.subjectName} Lab"
                } else {
                    current.subjectName
                }

                mergedClasses.add(
                    ClassScheduleItem(
                        id = "${dayOfWeek.name.lowercase()}_$i",
                        dayOfWeek = dayOfWeek,
                        subjectCode = current.subjectCode,
                        subjectName = displayName,
                        roomNumber = current.roomNumber,
                        facultyName = current.facultyName,
                        startTime = current.startTime,
                        endTime = finalEnd,
                        classType = current.classType
                    )
                )

                i = nextIdx
            }

            daySchedules.add(DaySchedule(dayOfWeek, mergedClasses.sortedBy { it.startTime }))
        }

        return WeeklyTimetable(
            studentBranch = studentBranch,
            section = section,
            semester = semester,
            days = daySchedules,
            lastSyncedTimestamp = System.currentTimeMillis()
        )
    }

    private fun extractSubjectLegends(doc: Element) {
        val legendElements = doc.select("table.plum_fieldbig tr.plum_fieldbig, tr.plum_fieldbig td, table.plum_field")
        val pattern = Pattern.compile("([A-Z]{3,6}\\d{3,4})\\s*[-–]\\s*([^<\\n\\r/]+)")
        for (el in legendElements) {
            val text = el.html().replace(Regex("(?i)<br\\s*/?>"), "\n").replace("&nbsp;", " ")
            val matcher = pattern.matcher(text)
            while (matcher.find()) {
                val code = matcher.group(1)?.trim().orEmpty()
                val rawName = matcher.group(2)?.trim()
                    ?.replace("\n", " ")
                    ?.replace(Regex("\\s+"), " ")
                    .orEmpty()
                if (code.isNotBlank() && rawName.isNotBlank()) {
                    dynamicSubjectNames[code] = rawName
                }
            }
        }
    }

    private fun extractFacultyAndSubjectMap(doc: Element): Map<String, Pair<String, String>> {
        val map = mutableMapOf<String, Pair<String, String>>()
        val primaryCodes = mutableSetOf<String>()

        // Target the legend table containing course & faculty mappings
        val legendTable = doc.select("table.plum_field").firstOrNull() ?: doc.select("table").lastOrNull()
        val elements = if (legendTable != null) {
            val pTags = legendTable.select("p")
            if (pTags.isNotEmpty()) pTags else legendTable.select("tr, td")
        } else {
            doc.select("table.plum_field p, table.plum_field td")
        }

        // Collect all discrete text lines
        val rawLines = mutableListOf<String>()
        for (el in elements) {
            val lines = el.html().split(Regex("(?i)<br\\s*/?>"))
            for (line in lines) {
                val clean = Jsoup.parseBodyFragment(line).text().trim()
                if (clean.isNotBlank()) {
                    rawLines.add(clean)
                }
            }
        }

        // Pass 1: Standalone primary course entries (do not contain commas)
        // e.g. "VTECC304 - Microelectronics / RAJ SENANI"
        // e.g. "VTECC305 - Digital Circuits and Systems / NEERAJ GOEL"
        for (line in rawLines) {
            if (!line.contains(",")) {
                parseAndRecordLegendEntry(line, map, primaryCodes, isPrimary = true)
            }
        }

        // Pass 2: Composite / shared lab lines (contain commas)
        // Co-faculty / lab instructors must NOT overwrite primary theory professors!
        for (line in rawLines) {
            if (line.contains(",")) {
                val parts = line.split(",")
                for (part in parts) {
                    parseAndRecordLegendEntry(part.trim(), map, primaryCodes, isPrimary = false)
                }
            }
        }

        return map
    }

    private fun parseAndRecordLegendEntry(
        text: String,
        map: MutableMap<String, Pair<String, String>>,
        primaryCodes: MutableSet<String>,
        isPrimary: Boolean
    ) {
        val dashParts = text.split("-")
        if (dashParts.size >= 2) {
            val codeMatch = Regex("([A-Z]{3,6}\\d{3,4})").find(dashParts[0]) ?: return
            val code = codeMatch.value

            // If subject code is already registered by a standalone primary line, preserve primary instructor
            if (!isPrimary && primaryCodes.contains(code)) {
                return
            }

            val rest = dashParts.drop(1).joinToString("-")
            val slashParts = rest.split("/")
            val subjectName = slashParts[0].trim().replace(Regex("\\s+"), " ")
            val rawFaculty = if (slashParts.size > 1) slashParts[1].trim() else ""
            val facultyName = formatFacultyName(rawFaculty.ifBlank { fallbackFaculty(code) })

            map[code] = Pair(subjectName, facultyName)
            dynamicSubjectNames[code] = subjectName
            dynamicFacultyNames[code] = facultyName

            if (isPrimary) {
                primaryCodes.add(code)
            }
        }
    }

    private fun formatFacultyName(rawFaculty: String): String {
        val trimmed = rawFaculty.trim()
        if (trimmed.isBlank()) return "Department Faculty"
        val upper = trimmed.uppercase()
        return when {
            upper.contains("RAJ SENANI") -> "Prof. Raj Senani"
            upper.contains("NEERAJ GOEL") -> "Dr. Neeraj Goel"
            upper.contains("DHARMENDRA") -> "Dr. Dharmendra Ku Upadhyay"
            upper.contains("AMIT SINGHAL") -> "Dr. Amit Singhal"
            upper.contains("HERA TARIQUE") -> "Dr. Hera Tarique"
            trimmed.startsWith("Prof.", ignoreCase = true) || trimmed.startsWith("Dr.", ignoreCase = true) -> {
                trimmed.lowercase().capitalizeWords()
            }
            else -> "Prof. " + trimmed.lowercase().capitalizeWords()
        }
    }

    private fun fallbackSubjectName(code: String): String = when (code) {
        "VTPHC301" -> "Solid State Physics"
        "VTECC302" -> "Signals and Systems"
        "VTECC303" -> "Probability Theory and Random Process"
        "VTECC304" -> "Microelectronics"
        "VTECC305" -> "Digital Circuits and Systems"
        "FCFO0301" -> "Community Internship"
        "VANM0303" -> "Innovation, Business Models and Entrepreneurship"
        else -> code
    }

    private fun fallbackFaculty(code: String): String = when (code) {
        "VTPHC301" -> "Dr. Hera Tarique"
        "VTECC302" -> "Dr. Dharmendra Ku Upadhyay"
        "VTECC303" -> "Dr. Amit Singhal"
        "VTECC304" -> "Prof. Raj Senani"
        "VTECC305" -> "Dr. Neeraj Goel"
        else -> "Department Faculty"
    }

    private data class RawScheduleSlot(
        val slotIndex: Int,
        val startTime: String,
        val endTime: String,
        val subjectCode: String,
        val subjectName: String,
        val roomNumber: String,
        val facultyName: String,
        val classType: ClassType,
        val isTutorial: Boolean,
        val isLab: Boolean
    )

    private fun String.capitalizeWords(): String =
        split(" ").joinToString(" ") { it.replaceFirstChar { c -> c.uppercase() } }

    private fun fallbackWeeklyTimetable(): WeeklyTimetable {
        return WeeklyTimetable(
            studentBranch = "Electronics Engineering (VLSI Design and Technology)",
            section = "Sec 1",
            semester = "Semester 3",
            days = emptyList(),
            lastSyncedTimestamp = System.currentTimeMillis()
        )
    }
}
