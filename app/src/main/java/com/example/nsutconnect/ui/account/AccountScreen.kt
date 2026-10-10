package com.example.nsutconnect.ui.account

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CalendarMonth
import com.example.nsutconnect.data.model.CalendarCategory
import com.example.nsutconnect.ui.calendar.getCategoryTheme
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.nsutconnect.R
import com.example.nsutconnect.data.local.UserPreferences
import com.example.nsutconnect.data.model.AttendanceSummary
import com.example.nsutconnect.data.model.WeeklyTimetable
import com.example.nsutconnect.data.sync.SyncResult
import com.example.nsutconnect.theme.*
import com.example.nsutconnect.widget.NsutAttendanceWidgetReceiver
import com.example.nsutconnect.widget.NsutScheduleWidgetReceiver
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AccountScreen(
    prefs: UserPreferences,
    syncState: SyncResult,
    onForceSync: () -> Unit,
    onLogout: () -> Unit,
    onBack: (() -> Unit)? = null
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    val rollNumber = remember(syncState) { prefs.getRollNumber().ifBlank { "" } }
    val studentName = remember(syncState) { prefs.getStudentName().ifBlank { "Student" } }
    val branch = remember(syncState) { prefs.getBranch().ifBlank { "Engineering" } }

    // Deterministic abstract/geometric avatar calculated by roll number / user id
    val geometricStyles = remember {
        listOf(
            "shapes",
            "squircles",
            "rings",
            "shape-grid",
            "waves",
            "patchwork",
            "slice",
            "triangles",
            "stripes",
            "weave",
            "stack"
        )
    }

    val studentSeed = remember(rollNumber) { rollNumber.trim().ifBlank { "Student" } }
    val selectedStyle = remember(studentSeed) {
        val hash = kotlin.math.abs(studentSeed.hashCode())
        geometricStyles[hash % geometricStyles.size]
    }

    val avatarUrl = remember(studentSeed, selectedStyle) {
        "https://api.dicebear.com/10.x/$selectedStyle/png?seed=$studentSeed"
    }

    val attendance = remember(syncState) { prefs.getCachedAttendance() }
    val timetable = remember(syncState) { prefs.getCachedTimetable() }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Account",
                        fontFamily = AppleSans,
                        fontWeight = FontWeight.Bold,
                        fontSize = 28.sp,
                        letterSpacing = (-0.6).sp,
                        color = AppleLabel
                    )
                },
                navigationIcon = {
                    if (onBack != null) {
                        IconButton(
                            onClick = onBack,
                            modifier = Modifier.bounceClick()
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Back",
                                tint = AppleLabel
                            )
                        }
                    }
                },
                actions = {
                    // Clean, minimal header — reload icon removed per user design request
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = AppleBackground,
                    titleContentColor = AppleLabel
                )
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = AppleBackground
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            contentPadding = PaddingValues(top = 4.dp, bottom = 32.dp)
        ) {
            // Minimalist Student Profile Card (No badges, no tags, no keystore badge)
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = AppleCardSurface)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp)
                    ) {
                        // Geometric DiceBear Avatar (Calculated deterministically by Roll Number)
                        Box(
                            modifier = Modifier
                                .size(72.dp)
                                .clip(RoundedCornerShape(20.dp))
                                .background(AppleElevatedSurface),
                            contentAlignment = Alignment.Center
                        ) {
                            AsyncImage(
                                model = ImageRequest.Builder(context)
                                    .data(avatarUrl)
                                    .crossfade(true)
                                    .build(),
                                contentDescription = "Student Avatar",
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize(),
                                error = painterResource(id = R.drawable.ic_huge_user),
                                placeholder = painterResource(id = R.drawable.ic_huge_user)
                            )
                        }

                        Spacer(modifier = Modifier.width(16.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = studentName,
                                fontFamily = AppleSans,
                                fontWeight = FontWeight.Bold,
                                fontSize = 20.sp,
                                letterSpacing = (-0.4).sp,
                                color = AppleLabel
                            )

                            Spacer(modifier = Modifier.height(3.dp))

                            Text(
                                text = "$rollNumber · Sem 3",
                                fontFamily = AppleSans,
                                fontWeight = FontWeight.Medium,
                                fontSize = 13.5.sp,
                                color = AppleSecondaryLabel
                            )

                            Spacer(modifier = Modifier.height(2.dp))

                            Text(
                                text = branch,
                                fontFamily = AppleSans,
                                fontSize = 12.sp,
                                color = AppleSecondaryLabel.copy(alpha = 0.75f),
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis,
                                lineHeight = 16.sp
                            )
                        }
                    }
                }
            }

            // Section Header: Home Screen Widgets
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 4.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = "HOME SCREEN WIDGETS",
                        fontFamily = AppleSans,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 12.sp,
                        letterSpacing = 0.6.sp,
                        color = AppleSecondaryLabel
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "Tap a widget preview to add directly to your home screen launcher.",
                        fontFamily = AppleSans,
                        fontSize = 12.sp,
                        color = AppleTertiaryLabel
                    )
                }
            }

            // Interactive Preview: Attendance Widget
            item {
                AttendanceWidgetInteractivePreview(
                    attendance = attendance,
                    onPin = {
                        val pinned = NsutAttendanceWidgetReceiver.requestPinWidget(context)
                        coroutineScope.launch {
                            snackbarHostState.showSnackbar(
                                if (pinned) "Attendance widget pinned to home screen"
                                else "Long-press home screen to place the Attendance widget"
                            )
                        }
                    }
                )
            }

            // Interactive Preview: Schedule Widget
            item {
                ScheduleWidgetInteractivePreview(
                    timetable = timetable,
                    onPin = {
                        val pinned = NsutScheduleWidgetReceiver.requestPinWidget(context)
                        coroutineScope.launch {
                            snackbarHostState.showSnackbar(
                                if (pinned) "Schedule widget pinned to home screen"
                                else "Long-press home screen to place the Schedule widget"
                            )
                        }
                    }
                )
            }

            // Schedule & Academic Calendar Integration Settings
            item {
                ScheduleCalendarAccountSettingsCard(prefs = prefs)
            }

            // Log Out Button (Tactile Apple Red Card with spring bounce)
            item {
                Spacer(modifier = Modifier.height(6.dp))
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .bounceClick { onLogout() },
                    shape = RoundedCornerShape(16.dp),
                    color = AppleSystemRedBg
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 16.dp),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_huge_logout),
                            contentDescription = null,
                            tint = AppleSystemRed,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Log Out of NSUT IMS",
                            fontFamily = AppleSans,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 15.sp,
                            color = AppleSystemRed
                        )
                    }
                }
            }
        }
    }
}

/**
 * Authentic launcher preview card for the Attendance Widget.
 * Shows strictly course-by-course attendance status — NO overall attendance.
 * Tapping triggers physical bounce and home screen widget pin.
 */
@Composable
private fun AttendanceWidgetInteractivePreview(
    attendance: AttendanceSummary?,
    onPin: () -> Unit
) {
    val sampleRecords = remember(attendance) {
        if (attendance != null && attendance.records.isNotEmpty()) {
            attendance.records.sortedWith(
                compareBy<com.example.nsutconnect.data.model.AttendanceRecord> { if (it.totalClasses == 0) 1 else 0 }
                    .thenBy { it.percentage }
            ).take(2)
        } else {
            emptyList()
        }
    }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .bounceClick { onPin() },
        shape = RoundedCornerShape(18.dp),
        color = AppleCardSurface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // Widget Header Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(24.dp)
                            .clip(RoundedCornerShape(6.dp))
                            .background(AppleElevatedSurface),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_huge_attendance),
                            contentDescription = null,
                            tint = AppleSystemBlue,
                            modifier = Modifier.size(14.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "NSUT Attendance",
                        fontFamily = AppleSans,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 13.sp,
                        color = AppleLabel
                    )
                }

                // Launcher Widget Tag
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = AppleElevatedSurface
                ) {
                    Text(
                        text = "4 × 2 WIDGET",
                        fontFamily = AppleSans,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.4.sp,
                        color = AppleTertiaryLabel,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Widget Inner Canvas: Course-wise Attendance (NO overall attendance)
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                color = AppleElevatedSurface
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    if (sampleRecords.isNotEmpty()) {
                        sampleRecords.forEach { record ->
                            val pct = record.percentage
                            val (badgeColor, badgeBg) = when {
                                pct >= 75.0 -> Pair(AppleSystemGreen, AppleSystemGreenBg)
                                pct >= 70.0 -> Pair(AppleSystemOrange, AppleSystemOrangeBg)
                                else -> Pair(AppleSystemRed, AppleSystemRedBg)
                            }

                            Column(modifier = Modifier.fillMaxWidth()) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
                                        Text(
                                            text = record.subjectName,
                                            fontFamily = AppleSans,
                                            fontWeight = FontWeight.SemiBold,
                                            fontSize = 13.sp,
                                            color = AppleLabel,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Spacer(modifier = Modifier.height(1.dp))
                                        Text(
                                            text = "${record.attendedClasses}/${record.totalClasses} classes · ${record.subjectCode}",
                                            fontFamily = AppleSans,
                                            fontSize = 11.sp,
                                            color = AppleSecondaryLabel
                                        )
                                    }

                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        val chipText = if (pct >= 75.0) {
                                            val safe = record.calculateSafeBunks()
                                            if (safe > 0) "+$safe Safe" else "75%"
                                        } else {
                                            "Need ${record.calculateClassesNeeded()}"
                                        }
                                        Text(
                                            text = chipText,
                                            fontFamily = AppleSans,
                                            fontWeight = FontWeight.Medium,
                                            fontSize = 11.sp,
                                            color = badgeColor,
                                            modifier = Modifier.padding(end = 6.dp)
                                        )

                                        Surface(
                                            shape = RoundedCornerShape(7.dp),
                                            color = badgeBg
                                        ) {
                                            Text(
                                                text = String.format(Locale.ENGLISH, "%.1f%%", pct),
                                                fontFamily = AppleSans,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 12.sp,
                                                color = badgeColor,
                                                modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp)
                                            )
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.height(4.dp))
                                // Progress bar for this course
                                val progressFloat = (pct / 100.0).toFloat().coerceIn(0f, 1f)
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(3.5.dp)
                                        .clip(RoundedCornerShape(2.dp))
                                        .background(AppleCardSurface)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth(progressFloat)
                                            .fillMaxHeight()
                                            .background(badgeColor)
                                    )
                                }
                            }
                        }
                    } else {
                        // Fallback sample course rows
                        Text(
                            text = "Course attendance updates live from IMS",
                            fontFamily = AppleSans,
                            fontSize = 12.sp,
                            color = AppleSecondaryLabel
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Action prompt pill
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Tap preview to pin to launcher",
                    fontFamily = AppleSans,
                    fontSize = 11.5.sp,
                    color = AppleSecondaryLabel
                )

                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = AppleElevatedSurface
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                    ) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_huge_widgets),
                            contentDescription = null,
                            tint = AppleSystemBlue,
                            modifier = Modifier.size(13.dp)
                        )
                        Spacer(modifier = Modifier.width(5.dp))
                        Text(
                            text = "Pin Widget",
                            fontFamily = AppleSans,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 11.5.sp,
                            color = AppleSystemBlue
                        )
                    }
                }
            }
        }
    }
}

/**
 * Authentic launcher preview card for the Schedule Widget.
 * Focuses on current ongoing and upcoming lectures of today;
 * automatically shifts to tomorrow's schedule once today's classes finish or on weekends.
 * Tapping triggers physical bounce and home screen widget pin.
 */
@Composable
private fun ScheduleWidgetInteractivePreview(
    timetable: WeeklyTimetable?,
    onPin: () -> Unit
) {
    val today = remember { LocalDate.now().dayOfWeek }
    val now = remember { LocalTime.now() }
    val isTodayCompleted = remember(timetable) {
        timetable?.isDayCompleted(today, now) ?: false
    }
    val targetDay = remember(timetable) {
        timetable?.getAutoDefaultDay(today, now) ?: today
    }
    val isShowingTomorrow = isTodayCompleted || (today != targetDay && today != java.time.DayOfWeek.SATURDAY && today != java.time.DayOfWeek.SUNDAY)

    val activeDay = if (isShowingTomorrow) targetDay else today
    val classesForActiveDay = remember(timetable, activeDay) {
        timetable?.getScheduleFor(activeDay) ?: emptyList()
    }

    val ongoingClass = remember(classesForActiveDay, isShowingTomorrow, now) {
        if (!isShowingTomorrow) classesForActiveDay.firstOrNull { it.isOngoingAt(now) } else null
    }

    val upcomingClasses = remember(classesForActiveDay, isShowingTomorrow, now) {
        if (!isShowingTomorrow) {
            classesForActiveDay.filter { it.isUpcomingAt(now) }
        } else {
            classesForActiveDay
        }
    }

    val daySubtitle = if (isShowingTomorrow) {
        "Tomorrow · ${targetDay.name.lowercase().replaceFirstChar { it.uppercase() }}"
    } else {
        "Today · ${LocalDate.now().format(DateTimeFormatter.ofPattern("EEE, MMM d", Locale.getDefault()))}"
    }

    val badgeText = when {
        isShowingTomorrow -> "TOMORROW"
        ongoingClass != null -> "LIVE"
        else -> "TODAY"
    }
    val badgeColor = when {
        isShowingTomorrow -> AppleTertiaryLabel
        ongoingClass != null -> AppleSystemGreen
        else -> AppleSystemBlue
    }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .bounceClick { onPin() },
        shape = RoundedCornerShape(18.dp),
        color = AppleCardSurface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // Widget Header Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(24.dp)
                            .clip(RoundedCornerShape(6.dp))
                            .background(AppleElevatedSurface),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_huge_schedule),
                            contentDescription = null,
                            tint = AppleSystemBlue,
                            modifier = Modifier.size(14.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text(
                            text = "NSUT Schedule",
                            fontFamily = AppleSans,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 13.sp,
                            color = AppleLabel
                        )
                        Text(
                            text = daySubtitle,
                            fontFamily = AppleSans,
                            fontSize = 11.sp,
                            color = AppleSecondaryLabel
                        )
                    }
                }

                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = AppleElevatedSurface
                ) {
                    Text(
                        text = badgeText,
                        fontFamily = AppleSans,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.4.sp,
                        color = badgeColor,
                        modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Widget Inner Canvas: Current ongoing + Upcoming classes
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                color = AppleElevatedSurface
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // 1. Ongoing class if happening right now
                    if (ongoingClass != null) {
                        val roomDisplay = if (ongoingClass.roomNumber.startsWith("Room", ignoreCase = true)) ongoingClass.roomNumber else "Room ${ongoingClass.roomNumber}"
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = AppleSystemGreenBg,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(10.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "NOW IN PROGRESS",
                                        fontFamily = AppleSans,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 10.sp,
                                        letterSpacing = 0.4.sp,
                                        color = AppleSystemGreen
                                    )
                                    Text(
                                        text = ongoingClass.timeSlotDisplay,
                                        fontFamily = AppleSans,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = AppleSystemGreen
                                    )
                                }
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = ongoingClass.subjectName,
                                    fontFamily = AppleSans,
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 13.5.sp,
                                    color = AppleLabel,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Spacer(modifier = Modifier.height(1.dp))
                                Text(
                                    text = roomDisplay,
                                    fontFamily = AppleSans,
                                    fontSize = 11.sp,
                                    color = AppleSecondaryLabel
                                )
                            }
                        }
                    }

                    // 2. Upcoming classes of today or tomorrow
                    val displayUpcoming = upcomingClasses.take(if (ongoingClass != null) 2 else 3)
                    if (displayUpcoming.isNotEmpty()) {
                        displayUpcoming.forEachIndexed { index, item ->
                            val roomDisplay = if (item.roomNumber.startsWith("Room", ignoreCase = true)) item.roomNumber else "Room ${item.roomNumber}"
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(AppleCardSurface)
                                    .padding(horizontal = 10.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(
                                            text = item.timeSlotDisplay,
                                            fontFamily = AppleSans,
                                            fontWeight = FontWeight.SemiBold,
                                            fontSize = 11.sp,
                                            color = AppleSystemBlue
                                        )
                                        if (index == 0 && ongoingClass == null && !isShowingTomorrow) {
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Surface(
                                                shape = RoundedCornerShape(4.dp),
                                                color = AppleSystemBlueBg
                                            ) {
                                                Text(
                                                    text = "UP NEXT",
                                                    fontFamily = AppleSans,
                                                    fontSize = 8.5.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = AppleSystemBlue,
                                                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                                )
                                            }
                                        }
                                    }
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = item.subjectName,
                                        fontFamily = AppleSans,
                                        fontWeight = FontWeight.Medium,
                                        fontSize = 12.5.sp,
                                        color = AppleLabel,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }

                                Text(
                                    text = roomDisplay,
                                    fontFamily = AppleSans,
                                    fontSize = 11.sp,
                                    color = AppleSecondaryLabel
                                )
                            }
                        }
                    } else if (ongoingClass == null) {
                        Text(
                            text = "No classes remaining for today 🎉",
                            fontFamily = AppleSans,
                            fontSize = 12.sp,
                            color = AppleSecondaryLabel,
                            modifier = Modifier.padding(vertical = 4.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Action prompt pill
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Tap preview to pin to launcher",
                    fontFamily = AppleSans,
                    fontSize = 11.5.sp,
                    color = AppleSecondaryLabel
                )

                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = AppleElevatedSurface
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                    ) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_huge_widgets),
                            contentDescription = null,
                            tint = AppleSystemBlue,
                            modifier = Modifier.size(13.dp)
                        )
                        Spacer(modifier = Modifier.width(5.dp))
                        Text(
                            text = "Pin Widget",
                            fontFamily = AppleSans,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 11.5.sp,
                            color = AppleSystemBlue
                        )
                    }
                }
            }
        }
    }
}

/**
 * Schedule & Academic Calendar settings card with toggle and category tag filters.
 */
@Composable
private fun ScheduleCalendarAccountSettingsCard(
    prefs: UserPreferences
) {
    var isEnabled by remember { mutableStateOf(prefs.isAcademicCalendarInScheduleEnabled()) }
    var enabledCategories by remember { mutableStateOf(prefs.getCalendarCategoryFilters()) }

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        color = AppleCardSurface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // Header Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(26.dp)
                            .clip(RoundedCornerShape(7.dp))
                            .background(AppleElevatedSurface),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.CalendarMonth,
                            contentDescription = null,
                            tint = AppleAccent,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(9.dp))
                    Text(
                        text = "Schedule & Academic Calendar",
                        fontFamily = AppleSans,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 13.5.sp,
                        color = AppleLabel
                    )
                }

                Switch(
                    checked = isEnabled,
                    onCheckedChange = { checked ->
                        isEnabled = checked
                        prefs.setAcademicCalendarInScheduleEnabled(checked)
                    },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = Color.White,
                        checkedTrackColor = AppleSystemGreen,
                        uncheckedThumbColor = AppleTertiaryLabel,
                        uncheckedTrackColor = AppleElevatedSurface
                    )
                )
            }

            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "Show university milestones, holidays, and fests inside weekly schedule timetable.",
                fontFamily = AppleSans,
                fontSize = 11.5.sp,
                color = AppleSecondaryLabel
            )

            if (isEnabled) {
                Spacer(modifier = Modifier.height(12.dp))
                HorizontalDivider(color = AppleBorder, thickness = 0.5.dp)
                Spacer(modifier = Modifier.height(10.dp))

                Text(
                    text = "TAG FILTRATION",
                    fontFamily = AppleSans,
                    fontWeight = FontWeight.Bold,
                    fontSize = 10.sp,
                    color = AppleTertiaryLabel,
                    letterSpacing = 0.5.sp
                )

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    CalendarCategory.values().forEach { cat ->
                        val isCatActive = enabledCategories.contains(cat)
                        val (catBg, catText, _) = getCategoryTheme(cat)

                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = if (isCatActive) catBg else AppleElevatedSurface,
                            border = BorderStroke(
                                1.dp,
                                if (isCatActive) catText.copy(alpha = 0.5f) else AppleBorder
                            ),
                            modifier = Modifier.bounceClick {
                                val updated = prefs.toggleCalendarCategoryFilter(cat)
                                enabledCategories = updated
                            }
                        ) {
                            Text(
                                text = cat.displayName,
                                fontFamily = AppleSans,
                                fontSize = 11.sp,
                                fontWeight = if (isCatActive) FontWeight.Bold else FontWeight.Medium,
                                color = if (isCatActive) catText else AppleSecondaryLabel,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}
