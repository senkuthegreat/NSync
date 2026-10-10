package com.example.nsutconnect.data.model

data class NoticeItem(
    val id: String,
    val title: String,
    val date: String,
    val category: String,
    val targetDepartment: String,
    val aiSummary: String,
    val isRelevant: Boolean = true,
    val isUrgent: Boolean = false,
    val downloadUrl: String = ""
)
