package com.example.nsutconnect.ui.exam

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.nsutconnect.data.calendar.GoogleCalendarSyncManager
import com.example.nsutconnect.data.local.AdmitCardManager
import com.example.nsutconnect.data.local.StudentRecordsProvider
import com.example.nsutconnect.data.local.UserPreferences
import com.example.nsutconnect.data.model.ExamDatesheetItem
import com.example.nsutconnect.data.model.SeatingPlan
import com.example.nsutconnect.theme.*
import kotlinx.coroutines.launch

enum class ExamTab {
    DATESHEET,
    SEATING_PLAN
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExamScreen(
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val midSemDatesheet = remember { StudentRecordsProvider.getMidSemDatesheet(context) }
    val endSemDatesheet = remember { StudentRecordsProvider.getEndSemDatesheet(context) }

    // Selected cycle state: defaults to Mid-Sem (official datesheet)
    var selectedCycle by remember { mutableStateOf("MID_SEM") }

    var selectedExamItem by remember { mutableStateOf<ExamDatesheetItem?>(null) }
    var showAdmitCardSheet by remember { mutableStateOf(false) }

    val currentDatesheet = remember(selectedCycle, midSemDatesheet, endSemDatesheet) {
        if (selectedCycle == "MID_SEM") midSemDatesheet else endSemDatesheet
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Datesheet",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                },
                navigationIcon = {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier.bounceClick()
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = Color.White
                        )
                    }
                },
                actions = {
                    IconButton(
                        onClick = { StudentRecordsProvider.openOfficialDatesheetPdf(context) },
                        modifier = Modifier.bounceClick()
                    ) {
                        Icon(
                            imageVector = Icons.Default.PictureAsPdf,
                            contentDescription = "Official Datesheet PDF",
                            tint = AppleAccent
                        )
                    }
                    IconButton(
                        onClick = { showAdmitCardSheet = true },
                        modifier = Modifier.bounceClick()
                    ) {
                        Icon(
                            imageVector = Icons.Default.Badge,
                            contentDescription = "Official Admit Card",
                            tint = AppleAccent
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = AppleBackground
                )
            )
        },
        containerColor = AppleBackground
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Exam Cycle Switcher (Mid-Sem vs End-Sem) with sliding pill
            item {
                BoxWithConstraints(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(42.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(AppleCardSurface)
                        .padding(4.dp)
                ) {
                    val totalWidth = maxWidth
                    val tabWidth = totalWidth / 2
                    val animatedIndex by animateFloatAsState(
                        targetValue = if (selectedCycle == "MID_SEM") 0f else 1f,
                        animationSpec = spring(
                            dampingRatio = 0.82f,
                            stiffness = Spring.StiffnessMediumLow
                        ),
                        label = "exam_cycle_slider"
                    )

                    // Sliding Pill
                    Box(
                        modifier = Modifier
                            .offset(x = tabWidth * animatedIndex)
                            .width(tabWidth)
                            .fillMaxHeight()
                            .clip(RoundedCornerShape(9.dp))
                            .background(AppleElevatedSurface)
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        val midTextColor by animateColorAsState(
                            targetValue = if (selectedCycle == "MID_SEM") Color.White else TextSecondary,
                            animationSpec = tween(180),
                            label = "mid_text_color"
                        )
                        val endTextColor by animateColorAsState(
                            targetValue = if (selectedCycle == "END_SEM") Color.White else TextSecondary,
                            animationSpec = tween(180),
                            label = "end_text_color"
                        )

                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(9.dp))
                                .bounceClick(scaleDown = 0.94f) { selectedCycle = "MID_SEM" }
                                .padding(vertical = 8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "Mid-Semester",
                                fontSize = 13.sp,
                                fontWeight = if (selectedCycle == "MID_SEM") FontWeight.Bold else FontWeight.Medium,
                                color = midTextColor
                            )
                        }

                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(9.dp))
                                .bounceClick(scaleDown = 0.94f) { selectedCycle = "END_SEM" }
                                .padding(vertical = 8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "End-Semester",
                                fontSize = 13.sp,
                                fontWeight = if (selectedCycle == "END_SEM") FontWeight.Bold else FontWeight.Medium,
                                color = endTextColor
                            )
                        }
                    }
                }
            }

            if (currentDatesheet.isEmpty()) {
                item {
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        color = AppleCardSurface
                    ) {
                        Column(
                            modifier = Modifier.padding(22.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = WarningOrange.copy(alpha = 0.15f),
                                modifier = Modifier.size(52.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.Default.EventBusy,
                                        contentDescription = null,
                                        tint = WarningOrange,
                                        modifier = Modifier.size(28.dp)
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(14.dp))
                            Text(
                                text = "End-Sem Datesheet Not Released Yet",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                textAlign = TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "The NSUT Examination Branch has not released the course-wise timetable for End-Semester examinations yet. Subject dates, shifts, and timings will appear here automatically once published on the IMS portal.",
                                fontSize = 12.5.sp,
                                color = TextSecondary,
                                textAlign = TextAlign.Center,
                                lineHeight = 18.sp
                            )

                            Spacer(modifier = Modifier.height(18.dp))

                            // Official Academic Calendar Window Reference
                            Surface(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp),
                                color = AppleElevatedSurface
                            ) {
                                Column(modifier = Modifier.padding(14.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = "ACADEMIC CALENDAR WINDOW",
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = TextSecondary,
                                            letterSpacing = 0.5.sp
                                        )
                                        Surface(
                                            shape = RoundedCornerShape(4.dp),
                                            color = AppleAccent.copy(alpha = 0.15f)
                                        ) {
                                            Text(
                                                text = "Cir. No. 655",
                                                fontSize = 9.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = AppleAccent,
                                                modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                                            )
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(8.dp))

                                    Text(
                                        text = "23 Nov 2026 – 09 Dec 2026",
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White
                                    )
                                    Spacer(modifier = Modifier.height(3.dp))
                                    Text(
                                        text = "Scheduled period for End Semester Theory Examinations (ESE)",
                                        fontSize = 11.sp,
                                        color = TextSecondary
                                    )
                                }
                            }
                        }
                    }
                }
            } else {
                items(currentDatesheet, key = { "${it.examType}_${it.courseCode}" }) { item ->
                    Box(modifier = Modifier.animateItem()) {
                        ExamDatesheetCard(
                            item = item,
                            onClick = { selectedExamItem = item }
                        )
                    }
                }
            }
        }

        // Slide-in bottom sheet for complete exam allotment and details
        selectedExamItem?.let { item ->
            ExamDetailBottomSheet(
                item = item,
                onDismiss = { selectedExamItem = null }
            )
        }

        if (showAdmitCardSheet) {
            AdmitCardBottomSheet(
                onDismiss = { showAdmitCardSheet = false }
            )
        }
    }
}

@Composable
private fun ExamDatesheetCard(
    item: ExamDatesheetItem,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .bounceClick()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        color = AppleCardSurface
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp)
        ) {
            // Course Name (prominent, bold) + Short Room Tag (compact)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = item.courseName,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    modifier = Modifier.weight(1f),
                    maxLines = 2
                )

                Spacer(modifier = Modifier.width(8.dp))

                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = if (item.isRoomReleased) AppleElevatedSurface else Color.White.copy(alpha = 0.06f)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = if (item.isRoomReleased) Icons.Default.MeetingRoom else Icons.Default.Schedule,
                            contentDescription = null,
                            tint = if (item.isRoomReleased) AppleAccent else TextSecondary,
                            modifier = Modifier.size(12.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = item.shortRoom,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = if (item.isRoomReleased) Color.White else TextSecondary
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Date and Time (cleanly aligned)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.CalendarToday,
                        contentDescription = null,
                        tint = AppleAccent,
                        modifier = Modifier.size(13.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "${item.day}, ${item.date}",
                        fontSize = 12.5.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color.White.copy(alpha = 0.9f)
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Schedule,
                        contentDescription = null,
                        tint = TextSecondary,
                        modifier = Modifier.size(13.dp)
                    )
                    Spacer(modifier = Modifier.width(5.dp))
                    Text(
                        text = item.timeSlot,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Normal,
                        color = TextSecondary
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ExamDetailBottomSheet(
    item: ExamDatesheetItem,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = AppleElevatedSurface,
        dragHandle = { BottomSheetDefaults.DragHandle() }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 28.dp)
                .navigationBarsPadding()
        ) {
            // Badges row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = AppleAccent.copy(alpha = 0.15f)
                ) {
                    Text(
                        text = item.courseCode,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = AppleAccent,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                    )
                }

                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = Color.White.copy(alpha = 0.08f)
                ) {
                    Text(
                        text = if (item.examType == "MID_SEM") "MID-SEMESTER" else "END-SEMESTER",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextSecondary,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = item.courseName,
                fontSize = 19.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                lineHeight = 24.sp
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = "${item.credits} Credits · Theory Examination",
                fontSize = 12.sp,
                color = TextSecondary
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Section 1: Schedule & Timing
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                color = AppleCardSurface
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text(
                        text = "SCHEDULE & TIMING",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextSecondary,
                        letterSpacing = 0.5.sp
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    DetailItemRow(
                        icon = Icons.Default.CalendarToday,
                        label = "Date & Day",
                        value = "${item.day}, ${item.date}"
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    DetailItemRow(
                        icon = Icons.Default.Schedule,
                        label = "Time Slot",
                        value = item.timeSlot
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    DetailItemRow(
                        icon = Icons.Default.AccessTime,
                        label = "Reporting Time",
                        value = item.reportingTime
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    DetailItemRow(
                        icon = Icons.Default.WbTwilight,
                        label = "Shift",
                        value = item.shift
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Section 2: Allotment & Seating
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                color = AppleCardSurface
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "ALLOTMENT & SEATING",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextSecondary,
                            letterSpacing = 0.5.sp
                        )
                        if (!item.isRoomReleased) {
                            Surface(
                                shape = RoundedCornerShape(4.dp),
                                color = WarningOrange.copy(alpha = 0.15f)
                            ) {
                                Text(
                                    text = "1 DAY PRIOR",
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = WarningOrange,
                                    modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    DetailItemRow(
                        icon = Icons.Default.MeetingRoom,
                        label = "Room Number",
                        value = if (item.isRoomReleased) item.shortRoom else "Releases 1 day prior to exam",
                        valueColor = if (item.isRoomReleased) AppleAccent else WarningOrange
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    DetailItemRow(
                        icon = Icons.Default.Apartment,
                        label = "Examination Hall",
                        value = if (item.isRoomReleased) item.examBlock else "Main Campus (Room TBA)"
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    DetailItemRow(
                        icon = Icons.Default.AirlineSeatReclineNormal,
                        label = "Desk Allotment",
                        value = if (item.isRoomReleased) item.deskNumber else "Released with seating plan",
                        valueColor = if (item.isRoomReleased) SafeGreen else TextSecondary
                    )

                    if (!item.isRoomReleased) {
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = "ℹ Room numbers and desk seating are officially notified by the NSUT Examination Division 1 day before each examination date.",
                            fontSize = 11.sp,
                            color = TextSecondary,
                            lineHeight = 15.sp
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Section 3: Important Rules
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                color = AppleCardSurface
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text(
                        text = "EXAMINATION GUIDELINES",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextSecondary,
                        letterSpacing = 0.5.sp
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = "• Physical Admit Card & NSUT Student ID Card are mandatory.\n" +
                               "• Mobile phones, smartwatches, and study notes strictly prohibited.\n" +
                               "• Entry closes 30 minutes after commencement of examination.",
                        fontSize = 11.5.sp,
                        color = TextSecondary,
                        lineHeight = 16.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Add Exam to Calendar Action Button
            Button(
                onClick = {
                    try {
                        val intent = Intent(Intent.ACTION_INSERT).apply {
                            data = android.provider.CalendarContract.Events.CONTENT_URI
                            putExtra(android.provider.CalendarContract.Events.TITLE, "${item.courseName} (${item.courseCode}) Exam")
                            putExtra(android.provider.CalendarContract.Events.EVENT_LOCATION, "${item.shortRoom}, ${item.examBlock}")
                            putExtra(android.provider.CalendarContract.Events.DESCRIPTION, "Desk: ${item.deskNumber}\nShift: ${item.shift}\nTiming: ${item.timeSlot}")
                        }
                        context.startActivity(intent)
                    } catch (e: Exception) {
                        Toast.makeText(context, "Could not open calendar app", Toast.LENGTH_SHORT).show()
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .bounceClick(),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = AppleAccent)
            ) {
                Icon(
                    imageVector = Icons.Default.CalendarToday,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Add Exam to Calendar",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
private fun DetailItemRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    value: String,
    valueColor: Color = Color.White
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = TextSecondary,
                modifier = Modifier.size(13.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = label,
                fontSize = 12.sp,
                color = TextSecondary
            )
        }

        Text(
            text = value,
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
            color = valueColor
        )
    }
}

@Composable
private fun SeatingPlanCard(seating: SeatingPlan) {
    val context = LocalContext.current

    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        // Hero Boarding-Pass style Card
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(18.dp),
            color = AppleCardSurface
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "EXAMINATION SEATING PASS",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextSecondary,
                        letterSpacing = 1.sp
                    )
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = SafeGreen.copy(alpha = 0.18f)
                    ) {
                        Text(
                            text = "SHIFT 1 • VERIFIED",
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            color = SafeGreen,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = seating.examination,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )

                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1.1f)) {
                        Text(text = "EXAM HALL / ROOM", fontSize = 10.sp, color = TextSecondary)
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = seating.roomNumber,
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Black,
                            color = AppleAccent
                        )
                        Text(text = seating.examHall, fontSize = 11.sp, color = Color.White, fontWeight = FontWeight.Medium)
                        Text(text = seating.floor, fontSize = 10.sp, color = TextSecondary)
                    }

                    Column(
                        modifier = Modifier.weight(0.9f),
                        horizontalAlignment = Alignment.End
                    ) {
                        Text(text = "ASSIGNED SEAT", fontSize = 10.sp, color = TextSecondary)
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = seating.seatNumber,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Black,
                            color = SafeGreen
                        )
                        Text(text = "${seating.row} • Col ${seating.columnNumber}", fontSize = 12.sp, color = Color.White)
                        Text(text = "Report: ${seating.reportingTime}", fontSize = 10.sp, color = WarningOrange)
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))
                HorizontalDivider(color = Color.White.copy(alpha = 0.08f))
                Spacer(modifier = Modifier.height(12.dp))

                // Student Identification Details
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "CANDIDATE: ${seating.studentName}",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Text(
                            text = "Roll: ${seating.rollNumber}",
                            fontSize = 10.sp,
                            color = AppleAccent
                        )
                    }

                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = AppleElevatedSurface
                    ) {
                        Text(
                            text = "Cap: ${seating.totalCapacity} Seats",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Medium,
                            color = TextSecondary,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                        )
                    }
                }
            }
        }

        // Visual Classroom Layout Matrix
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(18.dp),
            color = AppleCardSurface
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "${seating.roomNumber} Seating Layout",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Text(
                            text = "${seating.rowCount} Rows × ${seating.columnCount} Columns • ${seating.totalCapacity} Total Desks",
                            fontSize = 11.sp,
                            color = TextSecondary
                        )
                    }

                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = AppleElevatedSurface
                    ) {
                        Text(
                            text = "Page 23 IMS",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = AppleAccent,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(AppleElevatedSurface)
                        .padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    // Blackboard Podium Header
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(6.dp))
                            .background(Color.White.copy(alpha = 0.05f))
                            .padding(vertical = 5.dp),
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = "— INSTRUCTOR PODIUM & BLACKBOARD (FRONT) —",
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextSecondary,
                            letterSpacing = 0.5.sp
                        )
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    // Column headers
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Spacer(modifier = Modifier.width(28.dp)) // Row label spacer
                        (1..seating.columnCount).forEach { colIdx ->
                            Text(
                                text = "Col $colIdx",
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (colIdx == seating.columnNumber) AppleAccent else TextSecondary,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }

                    // Render 8 Rows
                    seating.deskMatrix.forEachIndexed { rowIndex, rowRolls ->
                        val currentRowNum = rowIndex + 1
                        val isUserRow = currentRowNum == seating.rowNumber

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Row identifier
                            Text(
                                text = "R$currentRowNum",
                                fontSize = 10.sp,
                                fontWeight = if (isUserRow) FontWeight.Bold else FontWeight.Normal,
                                color = if (isUserRow) AppleAccent else TextSecondary,
                                modifier = Modifier.width(28.dp)
                            )

                            // 4 Desks in this row
                            rowRolls.forEachIndexed { colIndex, roll ->
                                val currentColNum = colIndex + 1
                                val isUserSeat = roll == seating.rollNumber || (isUserRow && currentColNum == seating.columnNumber)

                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(34.dp)
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(
                                            if (isUserSeat) AppleAccent
                                            else Color.White.copy(alpha = 0.06f)
                                        )
                                        .then(
                                            if (isUserSeat) Modifier.border(1.5.dp, Color.White, RoundedCornerShape(6.dp))
                                            else Modifier
                                        )
                                        .clickable {
                                            Toast.makeText(
                                                context,
                                                if (isUserSeat) "Your Assigned Seat: Desk 16 (Row 8, Col 2)" else "Seat R${currentRowNum}C${currentColNum}: $roll",
                                                Toast.LENGTH_SHORT
                                            ).show()
                                        },
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (isUserSeat) {
                                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                            Text(
                                                text = "★ YOU",
                                                fontSize = 9.sp,
                                                fontWeight = FontWeight.Black,
                                                color = Color.White
                                            )
                                            Text(
                                                text = "Desk 16",
                                                fontSize = 7.5.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = Color.White.copy(alpha = 0.9f)
                                            )
                                        }
                                    } else {
                                        Text(
                                            text = roll.takeLast(4),
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Medium,
                                            color = TextSecondary
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Legend
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(12.dp)
                                .clip(RoundedCornerShape(3.dp))
                                .background(AppleAccent)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(text = "Your Desk 16 (Row 8, Col 2)", fontSize = 10.sp, color = Color.White)
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(12.dp)
                                .clip(RoundedCornerShape(3.dp))
                                .background(Color.White.copy(alpha = 0.12f))
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(text = "Other Candidates (ECE VLSI)", fontSize = 10.sp, color = TextSecondary)
                    }
                }
            }
        }

        // Alternate Day Exam Room Card
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            color = AppleCardSurface
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.SwapHoriz,
                            contentDescription = null,
                            tint = AppleAccent,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Alternate Day Allotment",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }

                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = AppleElevatedSurface
                    ) {
                        Text(
                            text = "Page 21 IMS",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Medium,
                            color = TextSecondary,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = seating.alternateDayRoom,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = SafeGreen
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Allotted for FCFO0301 (Community Internship Evaluation) • Assigned: Desk 3 (Shift 1)",
                    fontSize = 11.sp,
                    color = TextSecondary,
                    lineHeight = 15.sp
                )
            }
        }

        // Official IMS Notice & PDF Download Button
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            color = AppleCardSurface
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Verified,
                        contentDescription = null,
                        tint = SafeGreen,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "IMS Verified Circular Reference",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }

                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = seating.officialNoticeRef,
                    fontSize = 12.sp,
                    color = TextSecondary
                )

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedButton(
                    onClick = {
                        try {
                            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(seating.pdfDownloadUrl))
                            context.startActivity(intent)
                        } catch (e: Exception) {
                            Toast.makeText(context, "Opening seating circular in browser...", Toast.LENGTH_SHORT).show()
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .bounceClick(),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = Color.White
                    )
                ) {
                    Icon(
                        imageVector = Icons.Default.OpenInNew,
                        contentDescription = null,
                        tint = AppleAccent,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Open Official IMS Seating Notice (PDF)",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color.White
                    )
                }
            }
        }

        // Exam Guidelines
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            color = AppleCardSurface
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = null,
                        tint = AppleAccent,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Hall Rules & Guidelines",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
                Spacer(modifier = Modifier.height(10.dp))
                seating.guidelines.forEach { rule ->
                    Row(
                        modifier = Modifier.padding(vertical = 4.dp),
                        verticalAlignment = Alignment.Top
                    ) {
                        Text(text = "• ", fontSize = 12.sp, color = AppleAccent)
                        Text(
                            text = rule,
                            fontSize = 12.sp,
                            color = TextSecondary,
                            lineHeight = 16.sp
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdmitCardBottomSheet(
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    var isDownloading by remember { mutableStateOf(false) }
    val prefs = remember { UserPreferences(context) }
    val candidateName = remember { prefs.getStudentName().ifBlank { "STUDENT" } }
    val candidateRoll = remember { prefs.getRollNumber().ifBlank { "STUDENT" } }
    val candidateBranch = remember { prefs.getBranch().ifBlank { "B.Tech" } }
    val isRohit = candidateRoll.equals("2024UCO1500", ignoreCase = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = AppleElevatedSurface,
        dragHandle = { BottomSheetDefaults.DragHandle() }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 10.dp)
                .navigationBarsPadding(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Official NSUT Admit Card",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
            Text(
                text = "MID SEM EXAMINATION - SEP 2026",
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                color = AppleAccent
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Hall Ticket Card
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                color = AppleCardSurface
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(text = "CANDIDATE", fontSize = 10.sp, color = TextSecondary)
                            Text(text = candidateName, fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color.White)
                            Text(text = "Roll No: $candidateRoll", fontSize = 11.sp, color = AppleAccent)
                        }
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = AppleElevatedSurface
                        ) {
                            Icon(
                                imageVector = Icons.Default.QrCode2,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier
                                    .padding(8.dp)
                                    .size(36.dp)
                                    .bounceClick()
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))
                    Text(text = "Program: $candidateBranch", fontSize = 11.sp, color = TextSecondary)
                    Text(text = if (isRohit) "Center: APJ Abdul Kalam Block (Room 113/6)" else "Center: Main Campus Examination Block", fontSize = 11.sp, color = TextSecondary)

                    Spacer(modifier = Modifier.height(12.dp))
                    HorizontalDivider(color = Color.White.copy(alpha = 0.1f))
                    Spacer(modifier = Modifier.height(10.dp))

                    Text(
                        text = "Registered Papers Verified (Mid Sem Sep 2026)",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = SafeGreen
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            Button(
                onClick = {
                    if (!isDownloading) {
                        isDownloading = true
                        coroutineScope.launch {
                            val info = com.example.nsutconnect.data.local.AdmitCardInfo(
                                rollNumber = candidateRoll,
                                candidateName = candidateName,
                                branch = candidateBranch
                            )
                            AdmitCardManager.downloadAdmitCardPdf(context, info)
                            isDownloading = false
                        }
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .bounceClick(),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = AppleAccent
                ),
                enabled = !isDownloading
            ) {
                Icon(
                    imageVector = Icons.Default.Download,
                    contentDescription = null,
                    tint = Color.White
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = if (isDownloading) "Saving Official PDF..." else "Save Admit Card PDF to Device",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }

            Spacer(modifier = Modifier.height(10.dp))
        }
    }
}
