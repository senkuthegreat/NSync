package com.example.nsutconnect.data.model

enum class CalendarCategory(val displayName: String) {
    EXAM("Exams"),
    HOLIDAY("Holidays"),
    ACADEMIC("Academics"),
    REGISTRATION("Registration"),
    BREAK("Breaks")
}

data class AcademicEvent(
    val id: String,
    val title: String,
    val startDate: String,
    val endDate: String,
    val category: CalendarCategory,
    val description: String,
    val semester: String = "ODD 2026-27",
    val pageSource: Int = 1,
    val isCloudSynced: Boolean = true
)

data class CalendarScanState(
    val isScanning: Boolean = false,
    val isCloudSyncing: Boolean = false,
    val syncSource: String = "Cloud Database",
    val currentPage: Int = 0,
    val totalPages: Int = 3,
    val progressMessage: String = "",
    val eventsExtracted: Int = 0,
    val lastSyncedTime: String? = null,
    val error: String? = null
)
