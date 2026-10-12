package com.example.nsutconnect.ui.attendance

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.nsutconnect.R
import com.example.nsutconnect.data.model.AttendanceRecord
import com.example.nsutconnect.data.model.AttendanceSummary
import com.example.nsutconnect.theme.*
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AttendanceScreen(
    attendance: AttendanceSummary?,
    isLoading: Boolean,
    onRefresh: () -> Unit
) {
    var selectedRecordForDetails by remember {
        mutableStateOf<AttendanceRecord?>(null)
    }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Attendance",
                        fontFamily = AppleSans,
                        fontWeight = FontWeight.Bold,
                        fontSize = 28.sp,
                        letterSpacing = (-0.6).sp,
                        color = AppleLabel
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = AppleBackground,
                    titleContentColor = AppleLabel
                )
            )
        },
        containerColor = AppleBackground
    ) { padding ->
        PullToRefreshBox(
            isRefreshing = isLoading,
            onRefresh = onRefresh,
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            if (attendance == null || attendance.records.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.padding(32.dp)
                    ) {
                        Text(
                            text = "No Attendance Records",
                            fontFamily = AppleSans,
                            fontWeight = FontWeight.Bold,
                            fontSize = 20.sp,
                            color = AppleLabel
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Pull down or tap below to sync attendance directly from NSUT IMS.",
                            fontFamily = AppleSans,
                            fontSize = 13.sp,
                            color = AppleSecondaryLabel,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                            lineHeight = 18.sp
                        )
                        Spacer(modifier = Modifier.height(18.dp))
                        Button(
                            onClick = onRefresh,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = AppleAccent,
                                contentColor = Color.White
                            ),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text(
                                text = "Sync from IMS Portal",
                                fontFamily = AppleSans,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 14.sp
                            )
                        }
                    }
                }
            } else {
                // Exclude subjects with 0 total classes, sort low attendance first
                val sortedRecords = remember(attendance.records) {
                    attendance.records
                        .filter { it.totalClasses > 0 }
                        .sortedBy { it.percentage }
                }

                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    contentPadding = PaddingValues(top = 2.dp, bottom = 24.dp)
                ) {
                    // Minimal borderless cards: Course Name, Need/Bunksafe, Percentage
                    items(sortedRecords, key = { it.subjectCode }) { record ->
                        Box(modifier = Modifier.animateItem()) {
                            SimpleAttendanceCard(
                                record = record,
                                onClick = { selectedRecordForDetails = record }
                            )
                        }
                    }
                }
            }
        }
    }

    // Slide-in Bottom Sheet Modal for Full Attendance Details
    if (selectedRecordForDetails != null) {
        val record = selectedRecordForDetails!!
        ModalBottomSheet(
            onDismissRequest = { selectedRecordForDetails = null },
            sheetState = sheetState,
            containerColor = AppleElevatedSurface,
            contentColor = AppleLabel,
            shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp),
            dragHandle = {
                Surface(
                    modifier = Modifier.padding(top = 10.dp, bottom = 12.dp),
                    color = AppleSecondaryLabel.copy(alpha = 0.35f),
                    shape = RoundedCornerShape(2.5.dp)
                ) {
                    Box(modifier = Modifier.size(width = 36.dp, height = 4.5.dp))
                }
            }
        ) {
            AttendanceDetailBottomSheet(
                record = record,
                onDismiss = { selectedRecordForDetails = null }
            )
        }
    }
}

/**
 * Minimalist, borderless attendance card showing strictly:
 * 1. Course Name
 * 2. Need / Bunk Safe
 * 3. Percentage
 * Zero dots, zero borders.
 */
@Composable
fun SimpleAttendanceCard(
    record: AttendanceRecord,
    onClick: () -> Unit
) {
    val hasClasses = record.totalClasses > 0

    val (statusColor, statusBg) = when {
        !hasClasses -> Pair(AppleSecondaryLabel, AppleElevatedSurface)
        record.percentage < 70.0 -> Pair(AppleSystemRed, AppleSystemRedBg)
        record.percentage < 75.0 -> Pair(AppleSystemOrange, AppleSystemOrangeBg)
        else -> Pair(AppleSystemGreen, AppleSystemGreenBg)
    }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .bounceClick(scaleDown = 0.96f) { onClick() },
        shape = RoundedCornerShape(14.dp),
        color = AppleCardSurface
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 15.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(end = 12.dp)
            ) {
                Text(
                    text = record.subjectName,
                    fontFamily = AppleSans,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 15.5.sp,
                    letterSpacing = (-0.2).sp,
                    color = AppleLabel
                )
                Spacer(modifier = Modifier.height(4.dp))
                if (hasClasses) {
                    if (record.percentage >= 75.0) {
                        val safeBunks = record.calculateSafeBunks()
                        val bunkText = if (safeBunks > 0) "+$safeBunks Bunks Safe" else "On Border (75%)"
                        Text(
                            text = bunkText,
                            fontFamily = AppleSans,
                            fontWeight = FontWeight.Medium,
                            fontSize = 12.5.sp,
                            color = AppleSystemGreen
                        )
                    } else {
                        val needed = record.calculateClassesNeeded()
                        Text(
                            text = "Need $needed Classes",
                            fontFamily = AppleSans,
                            fontWeight = FontWeight.Medium,
                            fontSize = 12.5.sp,
                            color = statusColor
                        )
                    }
                } else {
                    Text(
                        text = "No classes held yet",
                        fontFamily = AppleSans,
                        fontSize = 12.5.sp,
                        color = AppleTertiaryLabel
                    )
                }
            }

            Surface(
                shape = RoundedCornerShape(10.dp),
                color = statusBg
            ) {
                Text(
                    text = if (hasClasses) String.format(Locale.getDefault(), "%.1f%%", record.percentage) else "0 Held",
                    fontFamily = AppleSans,
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.5.sp,
                    color = statusColor,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                )
            }
        }
    }
}

/**
 * Slide-in Bottom Sheet Modal displaying comprehensive attendance details:
 * Course Name, Subject Code, Type, Attendance %, Attended/Total counts, Progress bar, Advice, Faculty,
 * and complete date-by-date attendance logs (like IMS NSIT app).
 */
@Composable
fun AttendanceDetailBottomSheet(
    record: AttendanceRecord,
    onDismiss: () -> Unit
) {
    val hasClasses = record.totalClasses > 0
    val (statusColor, statusBg) = when {
        !hasClasses -> Pair(AppleSecondaryLabel, AppleElevatedSurface)
        record.percentage < 70.0 -> Pair(AppleSystemRed, AppleSystemRedBg)
        record.percentage < 75.0 -> Pair(AppleSystemOrange, AppleSystemOrangeBg)
        else -> Pair(AppleSystemGreen, AppleSystemGreenBg)
    }

    val statusLabel = when {
        !hasClasses -> "No Classes Held"
        record.percentage < 70.0 -> "Critical"
        record.percentage < 75.0 -> "Low Attendance"
        else -> "Safe Attendance"
    }

    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(scrollState)
            .padding(horizontal = 20.dp)
            .padding(bottom = 36.dp)
    ) {
        // Course Name
        Text(
            text = record.subjectName,
            fontFamily = AppleSans,
            fontWeight = FontWeight.Bold,
            fontSize = 22.sp,
            letterSpacing = (-0.5).sp,
            color = AppleLabel
        )

        Spacer(modifier = Modifier.height(10.dp))

        // Badges Row: Subject Code, Type, Status
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Subject Code Badge
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = AppleCardSurface
            ) {
                Text(
                    text = record.subjectCode,
                    fontFamily = AppleSans,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 12.sp,
                    color = AppleSecondaryLabel,
                    modifier = Modifier.padding(horizontal = 9.dp, vertical = 4.dp)
                )
            }

            // Subject Type Badge
            if (record.subjectType.isNotBlank()) {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = AppleSystemBlueBg
                ) {
                    Text(
                        text = record.subjectType.uppercase(),
                        fontFamily = AppleSans,
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp,
                        color = AppleSystemBlue,
                        modifier = Modifier.padding(horizontal = 9.dp, vertical = 4.dp)
                    )
                }
            }

            // Status Badge
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = statusBg
            ) {
                Text(
                    text = statusLabel,
                    fontFamily = AppleSans,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 11.sp,
                    color = statusColor,
                    modifier = Modifier.padding(horizontal = 9.dp, vertical = 4.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(18.dp))

        // Attendance Percentage & Progress Card
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = AppleCardSurface,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (hasClasses) String.format(Locale.getDefault(), "%.1f%%", record.percentage) else "0.0%",
                        fontFamily = AppleSans,
                        fontWeight = FontWeight.Bold,
                        fontSize = 32.sp,
                        letterSpacing = (-0.5).sp,
                        color = statusColor
                    )

                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            text = if (hasClasses) "${record.attendedClasses}/${record.totalClasses} Attended" else "0 Classes",
                            fontFamily = AppleSans,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 14.sp,
                            color = AppleLabel
                        )
                        if (hasClasses) {
                            Text(
                                text = "${record.absentClasses} Missed",
                                fontFamily = AppleSans,
                                fontSize = 12.sp,
                                color = AppleSecondaryLabel
                            )
                        }
                    }
                }

                if (hasClasses) {
                    Spacer(modifier = Modifier.height(12.dp))
                    val targetProg = (record.percentage / 100.0).toFloat().coerceIn(0f, 1f)
                    var startAnim by remember { mutableStateOf(false) }
                    LaunchedEffect(Unit) { startAnim = true }
                    val animatedProg by animateFloatAsState(
                        targetValue = if (startAnim) targetProg else 0f,
                        animationSpec = spring(
                            dampingRatio = 0.82f,
                            stiffness = Spring.StiffnessLow
                        ),
                        label = "animated_attendance_progress"
                    )
                    LinearProgressIndicator(
                        progress = { animatedProg },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(6.dp)
                            .clip(RoundedCornerShape(3.dp)),
                        color = statusColor,
                        trackColor = AppleElevatedSurface
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Bunk / Attendance Advice & Faculty Details Container
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = AppleCardSurface,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Advice Row
                if (hasClasses) {
                    if (record.percentage >= 75.0) {
                        val safeBunks = record.calculateSafeBunks()
                        AttendanceInfoRow(
                            iconRes = R.drawable.ic_huge_shield,
                            iconTint = AppleSystemGreen,
                            label = "Bunk Advice",
                            value = if (safeBunks > 0) "+$safeBunks Bunks Safe" else "On 75% Threshold",
                            subtitle = if (safeBunks > 0) "You can miss $safeBunks classes and stay above 75%" else "Attend upcoming classes to stay safe"
                        )
                    } else {
                        val needed = record.calculateClassesNeeded()
                        AttendanceInfoRow(
                            iconRes = R.drawable.ic_huge_attendance,
                            iconTint = statusColor,
                            label = "Attendance Goal",
                            value = "Need $needed Classes",
                            subtitle = "Attend $needed consecutive classes to reach 75%"
                        )
                    }
                } else {
                    AttendanceInfoRow(
                        iconRes = R.drawable.ic_huge_attendance,
                        iconTint = AppleSecondaryLabel,
                        label = "Status",
                        value = "No classes held yet",
                        subtitle = "Tracking begins once classes are conducted"
                    )
                }

                // Faculty Instructor
                if (record.facultyName.isNotBlank()) {
                    AttendanceInfoRow(
                        iconRes = R.drawable.ic_huge_user,
                        iconTint = AppleSystemBlue,
                        label = "Faculty Instructor",
                        value = record.facultyName
                    )
                }
            }
        }

        // Date-wise Attendance Logs (Like IMS NSIT App)
        if (record.history.isNotEmpty()) {
            Spacer(modifier = Modifier.height(18.dp))

            var filterOption by remember { mutableStateOf("All") }
            val filteredHistory = remember(filterOption, record.history) {
                when (filterOption) {
                    "Present" -> record.history.filter { it.isPresent == true }
                    "Absent" -> record.history.filter { it.isPresent == false }
                    else -> record.history
                }
            }

            // Section Header & Filter Tabs
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Dates Marked (${record.history.size})",
                    fontFamily = AppleSans,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 14.sp,
                    letterSpacing = (-0.2).sp,
                    color = AppleLabel
                )

                // Segmented Filter Tabs: All, Present, Absent
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = AppleCardSurface
                ) {
                    Row(modifier = Modifier.padding(2.dp)) {
                        listOf("All", "Present", "Absent").forEach { tab ->
                            val isSelected = filterOption == tab
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(if (isSelected) AppleElevatedSurface else Color.Transparent)
                                    .clickable { filterOption = tab }
                                    .padding(horizontal = 9.dp, vertical = 4.dp)
                            ) {
                                Text(
                                    text = tab,
                                    fontFamily = AppleSans,
                                    fontSize = 11.sp,
                                    fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                                    color = if (isSelected) AppleLabel else AppleSecondaryLabel
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // History List Container
            if (filteredHistory.isEmpty()) {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    color = AppleCardSurface
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "No records for this filter",
                            fontFamily = AppleSans,
                            fontSize = 13.sp,
                            color = AppleSecondaryLabel
                        )
                    }
                }
            } else {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    color = AppleCardSurface
                ) {
                    Column(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
                    ) {
                        filteredHistory.forEachIndexed { index, entry ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 10.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = entry.displayDate,
                                    fontFamily = AppleSans,
                                    fontWeight = FontWeight.Medium,
                                    fontSize = 13.5.sp,
                                    color = AppleLabel
                                )

                                val (badgeBg, badgeColor) = when (entry.isPresent) {
                                    true -> Pair(AppleSystemGreenBg, AppleSystemGreen)
                                    false -> Pair(AppleSystemRedBg, AppleSystemRed)
                                    null -> Pair(AppleElevatedSurface, AppleSecondaryLabel)
                                }

                                Surface(
                                    shape = RoundedCornerShape(6.dp),
                                    color = badgeBg
                                ) {
                                    Text(
                                        text = entry.statusText,
                                        fontFamily = AppleSans,
                                        fontWeight = FontWeight.SemiBold,
                                        fontSize = 11.sp,
                                        color = badgeColor,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                    )
                                }
                            }

                            if (index < filteredHistory.size - 1) {
                                HorizontalDivider(
                                    color = AppleElevatedSurface.copy(alpha = 0.5f),
                                    thickness = 0.5.dp
                                )
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Close Button
        Button(
            onClick = onDismiss,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(14.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = AppleCardSurface,
                contentColor = AppleLabel
            )
        ) {
            Text(
                text = "Close",
                fontFamily = AppleSans,
                fontWeight = FontWeight.SemiBold,
                fontSize = 15.sp
            )
        }
    }
}

@Composable
private fun AttendanceInfoRow(
    iconRes: Int,
    iconTint: Color,
    label: String,
    value: String,
    subtitle: String? = null
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Surface(
            shape = RoundedCornerShape(10.dp),
            color = iconTint.copy(alpha = 0.12f),
            modifier = Modifier.size(36.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    painter = painterResource(id = iconRes),
                    contentDescription = null,
                    tint = iconTint,
                    modifier = Modifier.size(18.dp)
                )
            }
        }

        Spacer(modifier = Modifier.width(14.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = label,
                fontFamily = AppleSans,
                fontSize = 11.5.sp,
                fontWeight = FontWeight.Normal,
                color = AppleSecondaryLabel
            )
            Spacer(modifier = Modifier.height(1.dp))
            Text(
                text = value,
                fontFamily = AppleSans,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                color = AppleLabel
            )
            if (subtitle != null) {
                Spacer(modifier = Modifier.height(1.dp))
                Text(
                    text = subtitle,
                    fontFamily = AppleSans,
                    fontSize = 11.sp,
                    color = AppleTertiaryLabel
                )
            }
        }
    }
}
