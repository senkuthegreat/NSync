package com.example.nsutconnect.data.model

data class AttendanceRecord(
    val subjectCode: String,
    val subjectName: String,
    val attendedClasses: Int,
    val totalClasses: Int,
    val facultyName: String = "",
    val subjectType: String = "Theory",
    val history: List<AttendanceDateEntry> = emptyList()
) {
    val percentage: Double
        get() = if (totalClasses > 0) (attendedClasses.toDouble() / totalClasses) * 100.0 else 0.0

    val absentClasses: Int
        get() = (totalClasses - attendedClasses).coerceAtLeast(0)

    /**
     * Calculates how many classes can be safely bunked while maintaining >= threshold% (default 75%)
     * Formula: floor( (attended / 0.75) - total )
     */
    fun calculateSafeBunks(threshold: Double = 75.0): Int {
        if (percentage < threshold || totalClasses == 0) return 0
        val targetRatio = threshold / 100.0
        val maxTotalPossible = (attendedClasses / targetRatio).toInt()
        return (maxTotalPossible - totalClasses).coerceAtLeast(0)
    }

    /**
     * Calculates how many consecutive classes must be attended to reach threshold% (default 75%)
     * Formula: ceil( (targetRatio * total - attended) / (1 - targetRatio) )
     */
    fun calculateClassesNeeded(threshold: Double = 75.0): Int {
        if (percentage >= threshold || totalClasses == 0) return 0
        val targetRatio = threshold / 100.0
        val numerator = (targetRatio * totalClasses) - attendedClasses
        val denominator = 1.0 - targetRatio
        return kotlin.math.ceil(numerator / denominator).toInt().coerceAtLeast(1)
    }

    val status: AttendanceStatus
        get() = when {
            percentage >= 75.0 -> AttendanceStatus.SAFE
            percentage >= 70.0 -> AttendanceStatus.WARNING
            else -> AttendanceStatus.CRITICAL
        }
}

enum class AttendanceStatus {
    SAFE,       // >= 75%
    WARNING,    // 70% - 74.9%
    CRITICAL    // < 70%
}

data class AttendanceSummary(
    val studentName: String,
    val rollNumber: String,
    val branch: String,
    val semester: String,
    val records: List<AttendanceRecord>,
    val lastSyncedTimestamp: Long = System.currentTimeMillis()
) {
    val totalAttended: Int
        get() = records.sumOf { it.attendedClasses }

    val totalConducted: Int
        get() = records.sumOf { it.totalClasses }

    val overallPercentage: Double
        get() = if (totalConducted > 0) (totalAttended.toDouble() / totalConducted) * 100.0 else 100.0

    val safeBunksOverall: Int
        get() {
            if (overallPercentage < 75.0 || totalConducted == 0) return 0
            val maxAllowedTotal = (totalAttended / 0.75).toInt()
            return (maxAllowedTotal - totalConducted).coerceAtLeast(0)
        }

    val classesNeededOverall: Int
        get() {
            if (overallPercentage >= 75.0 || totalConducted == 0) return 0
            val numerator = (0.75 * totalConducted) - totalAttended
            return kotlin.math.ceil(numerator / 0.25).toInt().coerceAtLeast(1)
        }
}

data class AttendanceDateEntry(
    val date: String,       // e.g. "Aug-12" or "Oct-06"
    val statusText: String, // "Present", "Absent", "Holiday", "Suspended", etc.
    val statusCode: String, // "1", "0", "1+1", "0+1", "GH", "CS", "CR", etc.
    val isPresent: Boolean? = null // true = Present, false = Absent, null = Neutral
) {
    val displayDate: String
        get() = date.replace("-", " ") // e.g. "Aug 12"
}

