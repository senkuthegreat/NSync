package com.example.nsutconnect.data.parser

import com.example.nsutconnect.data.model.ClassType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import java.time.DayOfWeek

class ImsParserTest {

    @Test
    fun testParseAttendanceHtml() {
        val file = File("src/main/assets/attendance_raw.html")
        assertTrue("attendance_raw.html must exist", file.exists())
        val html = file.readText()

        val summary = ImsParser.parseAttendanceHtml(html, "2024UCO1500")
        assertEquals("STUDENT", summary.studentName)
        assertEquals("2024UCO1500", summary.rollNumber)
        assertEquals(7, summary.records.size)

        // Verify sorted: lowest percentage first, 0-class held at bottom
        val r0 = summary.records[0]
        assertEquals("VTPHC301", r0.subjectCode)
        assertEquals(10, r0.attendedClasses)
        assertEquals(22, r0.totalClasses)
        assertEquals(45.45, r0.percentage, 0.5)

        val r1 = summary.records[1]
        assertEquals("VTECC302", r1.subjectCode)
        assertEquals(14, r1.attendedClasses)
        assertEquals(24, r1.totalClasses)

        val r4 = summary.records[4]
        assertEquals("VTECC305", r4.subjectCode)
        assertEquals(24, r4.attendedClasses)
        assertEquals(26, r4.totalClasses)

        // 0-classes held at bottom
        val r5 = summary.records[5]
        assertEquals(0, r5.totalClasses)
        val r6 = summary.records[6]
        assertEquals(0, r6.totalClasses)

        // Verify date-by-date history entries
        assertTrue("VTPHC301 history should not be empty", r0.history.isNotEmpty())
        assertEquals(32, r0.history.size)
        assertEquals(10, r0.history.count { it.isPresent == true })
        assertEquals(12, r0.history.count { it.isPresent == false })
        assertEquals(10, r0.history.count { it.isPresent == null })
        val latest = r0.history.first()
        assertEquals("Oct-06", latest.date)
        assertEquals("Present", latest.statusText)
        assertEquals(true, latest.isPresent)
    }

    @Test
    fun testParseTimetableHtml() {
        val file = File("src/main/assets/timetable_raw.html")
        assertTrue("timetable_raw.html must exist", file.exists())
        val html = file.readText()

        val timetable = ImsParser.parseTimetableHtml(html, "2024UCO1500")
        assertNotNull(timetable)

        // Monday check
        val mondayClasses = timetable.getScheduleFor(DayOfWeek.MONDAY)
        assertEquals(4, mondayClasses.size)

        assertEquals("10:00", mondayClasses[0].startTime)
        assertEquals("11:00", mondayClasses[0].endTime)
        assertEquals("VTECC305", mondayClasses[0].subjectCode)
        assertEquals("Dr. Neeraj Goel", mondayClasses[0].facultyName)

        assertEquals("12:00", mondayClasses[1].startTime)
        assertEquals("13:00", mondayClasses[1].endTime)
        assertEquals("VTECC304", mondayClasses[1].subjectCode)
        assertEquals("Prof. Raj Senani", mondayClasses[1].facultyName)

        // Merged tutorials check
        assertEquals("14:00", mondayClasses[2].startTime)
        assertEquals("16:00", mondayClasses[2].endTime)
        assertEquals("VTECC302", mondayClasses[2].subjectCode)
        assertEquals(ClassType.TUTORIAL, mondayClasses[2].classType)

        assertEquals("16:00", mondayClasses[3].startTime)
        assertEquals("18:00", mondayClasses[3].endTime)
        assertEquals("VTECC303", mondayClasses[3].subjectCode)
        assertEquals(ClassType.TUTORIAL, mondayClasses[3].classType)

        // Thursday check
        val thursdayClasses = timetable.getScheduleFor(DayOfWeek.THURSDAY)
        assertEquals(2, thursdayClasses.size)
        // Tutorial merged 14:00-16:00
        assertEquals("14:00", thursdayClasses[0].startTime)
        assertEquals("16:00", thursdayClasses[0].endTime)
        assertEquals("VTPHC301", thursdayClasses[0].subjectCode)
        assertEquals(ClassType.TUTORIAL, thursdayClasses[0].classType)

        // Lab Grp-2 merged 16:00-18:00
        assertEquals("16:00", thursdayClasses[1].startTime)
        assertEquals("18:00", thursdayClasses[1].endTime)
        assertEquals("VTECC304", thursdayClasses[1].subjectCode)
        assertEquals(ClassType.LAB, thursdayClasses[1].classType)

        // Status verification
        val at12pm = java.time.LocalTime.of(12, 0)
        val at3pm = java.time.LocalTime.of(15, 0)
        val at7pm = java.time.LocalTime.of(19, 0)

        assertEquals(com.example.nsutconnect.data.model.ClassScheduleStatus.UPCOMING, thursdayClasses[0].getStatusAt(at12pm))
        assertEquals(com.example.nsutconnect.data.model.ClassScheduleStatus.ONGOING, thursdayClasses[0].getStatusAt(at3pm))
        assertEquals(com.example.nsutconnect.data.model.ClassScheduleStatus.FINISHED, thursdayClasses[0].getStatusAt(at7pm))

        // When Thursday is completed at 7:00 PM, auto-default to Friday!
        assertTrue(timetable.isDayCompleted(DayOfWeek.THURSDAY, at7pm))
        assertEquals(DayOfWeek.FRIDAY, timetable.getAutoDefaultDay(DayOfWeek.THURSDAY, at7pm))

        // Thursday schedule rows: 2 classes back-to-back, no breaks
        val thursdayRows = timetable.getScheduleRowsFor(DayOfWeek.THURSDAY)
        assertEquals(2, thursdayRows.size)
        assertTrue(thursdayRows.all { it is com.example.nsutconnect.data.model.ScheduleRow.ClassRow })

        // Friday schedule rows: 6 classes + 2 breaks (12-1 PM and 4-5 PM) = 8 rows
        val fridayRows = timetable.getScheduleRowsFor(DayOfWeek.FRIDAY)
        assertEquals(8, fridayRows.size)
        val fridayBreaks = fridayRows.filterIsInstance<com.example.nsutconnect.data.model.ScheduleRow.BreakRow>()
        assertEquals(2, fridayBreaks.size)

        assertEquals("12:00", fridayBreaks[0].startTime)
        assertEquals("13:00", fridayBreaks[0].endTime)
        assertEquals("1 hr", fridayBreaks[0].durationFormatted)
        assertEquals("12:00 PM – 1:00 PM", fridayBreaks[0].timeSlotDisplay)

        assertEquals("16:00", fridayBreaks[1].startTime)
        assertEquals("17:00", fridayBreaks[1].endTime)
        assertEquals("1 hr", fridayBreaks[1].durationFormatted)
        assertEquals("4:00 PM – 5:00 PM", fridayBreaks[1].timeSlotDisplay)

        // Weekend & Friday evening checks:
        assertTrue(timetable.isDayCompleted(DayOfWeek.FRIDAY, at7pm))
        assertEquals(DayOfWeek.MONDAY, timetable.getAutoDefaultDay(DayOfWeek.FRIDAY, at7pm))
        assertEquals(DayOfWeek.MONDAY, timetable.getAutoDefaultDay(DayOfWeek.SATURDAY, at12pm))
        assertEquals(DayOfWeek.MONDAY, timetable.getAutoDefaultDay(DayOfWeek.SUNDAY, at12pm))
    }

    @Test
    fun testParseAttendanceWithTwoLetterSubjectCodes() {
        val sampleHtml = """
            <html>
            <body>
            <div id="myreport">
            <table class="plum_fieldbig">
                <tr class="plum_head">
                    <td>Days</td><td>MA101</td><td>CS101</td><td>EC201</td>
                </tr>
                <tr>
                    <td>Aug-01</td><td>Present</td><td>Absent</td><td>Present</td>
                </tr>
                <tr>
                    <td>Aug-02</td><td>Present</td><td>Present</td><td>--</td>
                </tr>
                <tr>
                    <td>Overall</td><td>2/2</td><td>1/2</td><td>1/1</td>
                </tr>
            </table>
            </div>
            </body>
            </html>
        """.trimIndent()

        val summary = ImsParser.parseAttendanceHtml(sampleHtml, "2024UCO1001")
        assertNotNull(summary)
        assertEquals(3, summary.records.size)

        val ma = summary.records.firstOrNull { it.subjectCode == "MA101" }
        assertNotNull(ma)
        assertEquals(2, ma!!.attendedClasses)
        assertEquals(2, ma.totalClasses)
        assertEquals(100.0, ma.percentage, 0.01)

        val cs = summary.records.firstOrNull { it.subjectCode == "CS101" }
        assertNotNull(cs)
        assertEquals(1, cs!!.attendedClasses)
        assertEquals(2, cs.totalClasses)
        assertEquals(50.0, cs.percentage, 0.01)
    }

    @Test
    fun testParseSemesterAndEnrolledCourses() {
        val sampleCoursesHtml = """
            <html>
            <body>
                <div id="div2" class="plum_head">Semester 5 Registered Courses</div>
                <table class="plum_fieldbig">
                    <tr><th>Course Code</th><th>Course Title</th></tr>
                    <tr><td>MA101</td><td>Mathematics I</td></tr>
                    <tr><td>CS101</td><td>Programming Fundamentals</td></tr>
                </table>
            </body>
            </html>
        """.trimIndent()

        val sem = ImsParser.parseSemesterFromHtml(sampleCoursesHtml)
        assertEquals("5", sem)

        val courses = ImsParser.parseEnrolledCoursesHtml(sampleCoursesHtml)
        assertEquals(2, courses.size)
        assertEquals("Mathematics I", courses["MA101"])
        assertEquals("Programming Fundamentals", courses["CS101"])
    }
}
