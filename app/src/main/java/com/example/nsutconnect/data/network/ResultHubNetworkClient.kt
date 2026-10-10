package com.example.nsutconnect.data.network

import com.example.nsutconnect.data.model.CourseGrade
import com.example.nsutconnect.data.model.SemesterResult
import com.example.nsutconnect.data.model.StudentAcademicRecord
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL

class ResultHubNetworkClient {

    companion object {
        private const val BASE_URL = "https://api.resulthubnsut.com/api/nsut/"
        private const val USER_AGENT = "NSUTConnect/1.0 (Linux; Android)"

        private val COURSE_TITLES = mapOf(
            "FCMT0101" to "Mathematics - I",
            "FCCH0103" to "Environmental Science and Green Chemistry",
            "FCEE0106" to "Fundamentals of Electrical Engineering",
            "FCHS0105" to "English",
            "FCME0116" to "Engineering Graphics & CAD",
            "VAPD0115" to "Yoga",
            "FCMT0201" to "Mathematics - II",
            "FCPH0124" to "Oscillations, Waves and Optics",
            "FCCS0102" to "Computer Programming",
            "VTECC201" to "Network Analysis and Synthesis",
            "VTECC202" to "Electronic Devices and Circuits",
            "VTITC203" to "Data Structures and Algorithms",
            "VTECC302" to "Signals & Systems",
            "VTECC303" to "Probability Theory & Stochastic Processes",
            "VTECC304" to "Microelectronics: Devices to Circuits",
            "VTECC305" to "Digital Circuits & Systems",
            "VTPHC301" to "Solid State Physics",
            "VANM0303" to "Innovation and Entrepreneurship",
            "FCFO0301" to "Community Internship"
        )

        private val COURSE_CREDITS = mapOf(
            "FCMT0101" to 4,
            "FCCH0103" to 4,
            "FCEE0106" to 4,
            "FCHS0105" to 4,
            "FCME0116" to 4,
            "VAPD0115" to 0,
            "FCMT0201" to 4,
            "FCPH0124" to 4,
            "FCCS0102" to 4,
            "VTECC201" to 4,
            "VTECC202" to 4,
            "VTITC203" to 4,
            "VTECC302" to 4,
            "VTECC303" to 4,
            "VTECC304" to 4,
            "VTECC305" to 4,
            "VTPHC301" to 4,
            "VANM0303" to 2,
            "FCFO0301" to 2
        )
    }

    suspend fun fetchStudentAcademicRecord(rollNumber: String): StudentAcademicRecord? = withContext(Dispatchers.IO) {
        var connection: HttpURLConnection? = null
        try {
            val url = URL("${BASE_URL}students/$rollNumber")
            connection = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                connectTimeout = 8000
                readTimeout = 8000
                setRequestProperty("User-Agent", USER_AGENT)
                setRequestProperty("Accept", "application/json")
            }

            val responseCode = connection.responseCode
            if (responseCode != HttpURLConnection.HTTP_OK) {
                return@withContext null
            }

            val reader = BufferedReader(InputStreamReader(connection.inputStream))
            val responseText = reader.use { it.readText() }
            val root = JSONObject(responseText)

            if (!root.optBoolean("success", false)) {
                return@withContext null
            }

            val data = root.getJSONObject("data")
            val roll = data.optString("rollNo", rollNumber)
            val name = data.optString("name", "Student")
            val branchCode = data.optString("branch_code", "UEV")
            val yearOfStudy = data.optString("year_of_study", "2025")
            val cgpa = data.optDouble("cgpa", 0.0).toFloat()
            val collegeRank = if (data.has("rank")) data.optInt("rank") else null
            val branchRank = if (data.has("branch_rank")) data.optInt("branch_rank") else null
            val percentile = if (data.has("percentile")) data.optDouble("percentile").toFloat() else null
            val creditsCompleted = data.optInt("credits_completed", 0)

            // Parse Grade Distribution
            val gradeDist = mutableMapOf<String, Int>()
            if (data.has("stats")) {
                val stats = data.getJSONObject("stats")
                if (stats.has("grade_distribution")) {
                    val gdObj = stats.getJSONObject("grade_distribution")
                    val keys = gdObj.keys()
                    while (keys.hasNext()) {
                        val k = keys.next()
                        gradeDist[k] = gdObj.getInt(k)
                    }
                }
            }

            // Parse Semesters
            val semArray = data.optJSONArray("semesters")
            val semesterList = mutableListOf<SemesterResult>()
            var totalCoursesCount = 0

            if (semArray != null) {
                for (i in 0 until semArray.length()) {
                    val semObj = semArray.getJSONObject(i)
                    val semNumber = semObj.optInt("semester", i + 1)
                    val sgpa = semObj.optDouble("sgpa", 0.0).toFloat()
                    val creditsReg = semObj.optInt("credits_registered", 20)
                    val creditsSec = semObj.optInt("credits_secured", creditsReg)

                    val coursesList = mutableListOf<CourseGrade>()
                    val subjArray = semObj.optJSONArray("subjects")
                    if (subjArray != null) {
                        for (j in 0 until subjArray.length()) {
                            val subjObj = subjArray.getJSONObject(j)
                            val code = subjObj.optString("subject_code", "")
                            val grade = subjObj.optString("grade", "")
                            val marks = subjObj.optInt("marks", gradeToPoints(grade))
                            val title = COURSE_TITLES[code] ?: code
                            val credits = COURSE_CREDITS[code] ?: 4

                            coursesList.add(
                                CourseGrade(
                                    code = code,
                                    title = title,
                                    credits = credits,
                                    grade = grade,
                                    gradePoints = marks,
                                    status = if (grade == "F") "FAIL" else "PASS"
                                )
                            )
                        }
                    }

                    totalCoursesCount += coursesList.size

                    semesterList.add(
                        SemesterResult(
                            semesterNumber = semNumber,
                            session = if (semNumber % 2 != 0) "Odd Sem (2025-26)" else "Even Sem (2025-26)",
                            sgpa = sgpa,
                            creditsEarned = creditsSec,
                            totalCredits = creditsReg,
                            courses = coursesList
                        )
                    )
                }
            }

            // Sort semesters latest first
            semesterList.sortByDescending { it.semesterNumber }
            val sortedSemesters = semesterList

            val rankPercentileText = when {
                branchRank != null && collegeRank != null -> "#$branchRank in $branchCode · #$collegeRank College"
                collegeRank != null -> "Rank #$collegeRank in Batch"
                percentile != null -> "Top ${String.format("%.1f", percentile)}% Percentile"
                else -> "Active Student"
            }

            StudentAcademicRecord(
                rollNumber = roll,
                name = name,
                program = "B.Tech. - ELECTRONICS ENGINEERING (VLSI)",
                cumulativeGpa = cgpa,
                totalCreditsCompleted = if (creditsCompleted > 0) creditsCompleted else sortedSemesters.sumOf { it.creditsEarned },
                totalCoursesCompleted = totalCoursesCount,
                rankPercentile = rankPercentileText,
                semesters = sortedSemesters,
                collegeRank = collegeRank,
                branchRank = branchRank,
                percentile = percentile,
                branchCode = branchCode,
                yearOfStudy = yearOfStudy,
                gradeDistribution = gradeDist,
                isLiveFromResultHub = true
            )
        } catch (e: Exception) {
            e.printStackTrace()
            null
        } finally {
            connection?.disconnect()
        }
    }

    private fun gradeToPoints(grade: String): Int {
        return when (grade.uppercase()) {
            "O" -> 10
            "A+" -> 9
            "A" -> 8
            "B+" -> 7
            "B" -> 6
            "C" -> 5
            "D" -> 4
            else -> 0
        }
    }
}
