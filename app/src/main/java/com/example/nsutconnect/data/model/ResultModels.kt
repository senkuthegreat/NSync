package com.example.nsutconnect.data.model

data class CourseGrade(
    val code: String,
    val title: String,
    val credits: Int,
    val grade: String,
    val gradePoints: Int,
    val status: String = if (grade == "F") "FAIL" else "PASS"
)

data class SemesterResult(
    val semesterNumber: Int,
    val session: String,
    val sgpa: Float,
    val creditsEarned: Int,
    val totalCredits: Int,
    val courses: List<CourseGrade>
)

data class StudentAcademicRecord(
    val rollNumber: String,
    val name: String,
    val program: String,
    val cumulativeGpa: Float,
    val totalCreditsCompleted: Int,
    val totalCoursesCompleted: Int,
    val rankPercentile: String,
    val semesters: List<SemesterResult>,
    val collegeRank: Int? = null,
    val branchRank: Int? = null,
    val percentile: Float? = null,
    val branchCode: String = "UEV",
    val yearOfStudy: String = "2025",
    val gradeDistribution: Map<String, Int> = emptyMap(),
    val isLiveFromResultHub: Boolean = false
)
