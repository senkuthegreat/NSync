package com.example.nsutconnect.ui.schedule

import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import com.example.nsutconnect.data.calendar.CalendarSyncResult
import com.example.nsutconnect.data.calendar.GoogleCalendarSyncManager
import com.example.nsutconnect.data.local.UserPreferences
import kotlinx.coroutines.launch
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.nsutconnect.R
import com.example.nsutconnect.data.model.ClassScheduleItem
import com.example.nsutconnect.data.model.ClassScheduleStatus
import com.example.nsutconnect.data.model.ClassType
import com.example.nsutconnect.data.model.ScheduleRow
import com.example.nsutconnect.data.model.WeeklyTimetable
import com.example.nsutconnect.theme.*
import com.example.nsutconnect.data.calendar.CloudCalendarRepository
import com.example.nsutconnect.data.model.AcademicEvent
import com.example.nsutconnect.data.model.CalendarCategory
import com.example.nsutconnect.ui.calendar.AcademicCalendarBottomSheetContent
import com.example.nsutconnect.ui.calendar.containsDate
import com.example.nsutconnect.ui.calendar.formatAcademicEventDates
import com.example.nsutconnect.ui.calendar.getCategoryTheme
import com.example.nsutconnect.ui.calendar.openAddToCalendarIntent
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Event
import androidx.compose.material.icons.filled.EventAvailable
import androidx.compose.material.icons.filled.Tune
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalTime
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScheduleScreen(
    timetable: WeeklyTimetable?,
    isLoading: Boolean,
    onRefresh: () -> Unit
) {
    val today = remember { LocalDate.now().dayOfWeek }
    val now = remember { LocalTime.now() }
    val actualTomorrow = remember(today) { today.plus(1) }
    val isWeekend = remember(today) { today == DayOfWeek.SATURDAY || today == DayOfWeek.SUNDAY }

    // Check if all classes for today are already over (only applicable on weekdays)
    val isTodayClassesOver = remember(timetable, today, now) {
        if (isWeekend) false
        else timetable?.isDayCompleted(today, now) ?: false
    }

    // Automatically default to next relevant school day
    val defaultDay = remember(timetable, today, now) {
        timetable?.getAutoDefaultDay(today, now) ?: if (isWeekend) DayOfWeek.MONDAY else today
    }

    var selectedDay by remember(defaultDay) {
        mutableStateOf(defaultDay)
    }

    // Selected class for bottom sheet slide-in pop-up
    var selectedClassForDetails by remember {
        mutableStateOf<ClassScheduleItem?>(null)
    }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    val days = listOf(
        DayOfWeek.MONDAY,
        DayOfWeek.TUESDAY,
        DayOfWeek.WEDNESDAY,
        DayOfWeek.THURSDAY,
        DayOfWeek.FRIDAY
    )

    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val prefs = remember { UserPreferences(context) }
    var hasCalendarPerm by remember {
        mutableStateOf(GoogleCalendarSyncManager.hasCalendarPermissions(context))
    }
    var isCalendarSyncing by remember { mutableStateOf(false) }

    val cloudRepo = remember { CloudCalendarRepository(context) }
    var academicEvents by remember { mutableStateOf<List<AcademicEvent>>(emptyList()) }
    var showAcademicCalendarSheet by remember { mutableStateOf(false) }

    // Load academic events asynchronously for the monthly calendar popup
    LaunchedEffect(Unit) {
        val cached = cloudRepo.fetchFromCloudDatabase()
        if (cached.isNotEmpty()) {
            academicEvents = cached
        }
    }

    val calendarPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val granted = permissions[Manifest.permission.WRITE_CALENDAR] == true &&
                      permissions[Manifest.permission.READ_CALENDAR] == true
        hasCalendarPerm = granted
        if (granted) {
            if (timetable != null) {
                coroutineScope.launch {
                    isCalendarSyncing = true
                    android.widget.Toast.makeText(
                        context,
                        "Syncing schedule to Google Calendar...",
                        android.widget.Toast.LENGTH_SHORT
                    ).show()
                    val result = GoogleCalendarSyncManager.syncTimetableToCalendar(context, timetable)
                    isCalendarSyncing = false
                    when (result) {
                        is CalendarSyncResult.Success -> {
                            prefs.setLastCalendarSync(System.currentTimeMillis(), result.eventCount)
                            android.widget.Toast.makeText(
                                context,
                                "Synced ${result.eventCount} classes to Google Calendar (${result.accountName})",
                                android.widget.Toast.LENGTH_LONG
                            ).show()
                        }
                        is CalendarSyncResult.Error -> {
                            android.widget.Toast.makeText(
                                context,
                                "Google Calendar sync failed: ${result.message}",
                                android.widget.Toast.LENGTH_LONG
                            ).show()
                        }
                        is CalendarSyncResult.PermissionRequired -> {
                            android.widget.Toast.makeText(
                                context,
                                "Calendar permission required",
                                android.widget.Toast.LENGTH_SHORT
                            ).show()
                        }
                    }
                }
            } else {
                android.widget.Toast.makeText(
                    context,
                    "Calendar connected! Pull down to refresh schedule first to sync classes.",
                    android.widget.Toast.LENGTH_LONG
                ).show()
            }
        } else {
            android.widget.Toast.makeText(
                context,
                "Calendar permission denied",
                android.widget.Toast.LENGTH_SHORT
            ).show()
        }
    }

    val onSyncGoogleCalendar = {
        if (!hasCalendarPerm) {
            calendarPermissionLauncher.launch(
                arrayOf(
                    Manifest.permission.READ_CALENDAR,
                    Manifest.permission.WRITE_CALENDAR
                )
            )
        } else if (timetable == null) {
            android.widget.Toast.makeText(
                context,
                "No timetable available. Pull down to refresh schedule first.",
                android.widget.Toast.LENGTH_SHORT
            ).show()
        } else if (!isCalendarSyncing) {
            coroutineScope.launch {
                isCalendarSyncing = true
                android.widget.Toast.makeText(
                    context,
                    "Syncing schedule to Google Calendar...",
                    android.widget.Toast.LENGTH_SHORT
                ).show()
                val result = GoogleCalendarSyncManager.syncTimetableToCalendar(context, timetable)
                isCalendarSyncing = false
                when (result) {
                    is CalendarSyncResult.Success -> {
                        prefs.setLastCalendarSync(System.currentTimeMillis(), result.eventCount)
                        android.widget.Toast.makeText(
                            context,
                            "Successfully synced ${result.eventCount} classes to Google Calendar (${result.accountName})",
                            android.widget.Toast.LENGTH_LONG
                        ).show()
                    }
                    is CalendarSyncResult.Error -> {
                        android.widget.Toast.makeText(
                            context,
                            "Failed to sync: ${result.message}",
                            android.widget.Toast.LENGTH_LONG
                        ).show()
                    }
                    is CalendarSyncResult.PermissionRequired -> {
                        calendarPermissionLauncher.launch(
                            arrayOf(
                                Manifest.permission.READ_CALENDAR,
                                Manifest.permission.WRITE_CALENDAR
                            )
                        )
                    }
                }
            }
        }
    }

    // Automatic silent sync whenever timetable is available and permissions granted
    LaunchedEffect(timetable, hasCalendarPerm) {
        if (hasCalendarPerm && timetable != null && prefs.isGoogleCalendarSyncEnabled()) {
            val lastSync = prefs.getLastCalendarSyncTimestamp()
            if (lastSync < timetable.lastSyncedTimestamp) {
                isCalendarSyncing = true
                val result = GoogleCalendarSyncManager.syncTimetableToCalendar(context, timetable)
                if (result is CalendarSyncResult.Success) {
                    prefs.setLastCalendarSync(System.currentTimeMillis(), result.eventCount)
                }
                isCalendarSyncing = false
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Schedule",
                        fontFamily = AppleSans,
                        fontWeight = FontWeight.Bold,
                        fontSize = 24.sp,
                        letterSpacing = (-0.5).sp,
                        color = AppleLabel,
                        maxLines = 1
                    )
                },
                actions = {
                    // Google Calendar Sync Button
                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = AppleElevatedSurface,
                        modifier = Modifier
                            .padding(end = 4.dp)
                            .bounceClick()
                            .clickable { onSyncGoogleCalendar() }
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                        ) {
                            if (isCalendarSyncing) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(14.dp),
                                    strokeWidth = 2.dp,
                                    color = AppleSystemBlue
                                )
                            } else {
                                Icon(
                                    painter = painterResource(id = R.drawable.ic_google_calendar),
                                    contentDescription = "Sync to Google Calendar",
                                    tint = Color.Unspecified,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(5.dp))
                            Text(
                                text = if (isCalendarSyncing) "Syncing..." else "Google Cal",
                                fontFamily = AppleSans,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 11.5.sp,
                                letterSpacing = (-0.2).sp,
                                color = AppleLabel
                            )
                        }
                    }

                    // Monthly Academic Calendar Icon Button (Slide-in bottom popup)
                    IconButton(
                        onClick = { showAcademicCalendarSheet = true },
                        modifier = Modifier.bounceClick()
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = AppleElevatedSurface,
                            modifier = Modifier.size(36.dp)
                        ) {
                            Box(
                                modifier = Modifier.fillMaxSize(),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.CalendarMonth,
                                    contentDescription = "Monthly Academic Calendar",
                                    tint = AppleAccent,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }
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
            Column(
                modifier = Modifier.fillMaxSize()
            ) {
                // Unified 5-day Apple Segmented Control (Mon – Fri)
                AppleWeekSegmentedControl(
                    days = days,
                    selectedDay = selectedDay,
                    today = today,
                    onDaySelected = { selectedDay = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 6.dp)
                )

                // Smart Banner: When classes are over on a weekday
                if (!isWeekend && isTodayClassesOver && timetable != null) {
                    val isFriday = today == DayOfWeek.FRIDAY
                    val targetDay = if (isFriday) DayOfWeek.MONDAY else actualTomorrow

                    if (selectedDay != today) {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = AppleElevatedSurface,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 14.dp, vertical = 4.dp)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 14.dp, vertical = 9.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = if (isFriday) "Classes finished for the week — Showing Monday" else "Today's classes ended — Showing tomorrow",
                                    fontFamily = AppleSans,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = AppleSecondaryLabel
                                )
                                Text(
                                    text = if (isFriday) "View Friday" else "View Today",
                                    fontFamily = AppleSans,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = AppleSystemBlue,
                                    modifier = Modifier.clickable { selectedDay = today }
                                )
                            }
                        }
                    } else {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = AppleElevatedSurface,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 14.dp, vertical = 4.dp)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 14.dp, vertical = 9.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = if (isFriday) "All of Friday's classes are completed" else "All of today's classes are completed",
                                    fontFamily = AppleSans,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = AppleSecondaryLabel
                                )
                                Text(
                                    text = if (isFriday) "Next Week (Mon)" else "Tomorrow",
                                    fontFamily = AppleSans,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = AppleSystemBlue,
                                    modifier = Modifier.clickable { selectedDay = targetDay }
                                )
                            }
                        }
                    }
                }

                if (timetable == null) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "No timetable available. Tap sync to load schedule.",
                            fontFamily = AppleSans,
                            color = AppleSecondaryLabel
                        )
                    }
                } else {
                    AnimatedContent(
                        targetState = selectedDay,
                        transitionSpec = {
                            val forward = targetState.value > initialState.value
                            appleSlideHorizontally(forward)
                        },
                        label = "day_schedule_slide"
                    ) { activeDay ->
                        val daySchedule = timetable.days.find { it.dayOfWeek == activeDay }
                        val classes = daySchedule?.classes?.sortedBy { it.startTime } ?: emptyList()
                        val scheduleRows = timetable.getScheduleRowsFor(activeDay)
                        val isCurrentDay = activeDay == today
                        val currentTime = LocalTime.now()

                        // Identify the immediate next upcoming class if viewing today
                        val nextUpcomingItem = if (isCurrentDay) {
                            classes.firstOrNull { it.isUpcomingAt(currentTime) }
                        } else null

                        LazyColumn(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(horizontal = 14.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            contentPadding = PaddingValues(top = 6.dp, bottom = 24.dp)
                        ) {
                            if (classes.isEmpty()) {
                                item {
                                    Surface(
                                        modifier = Modifier.fillMaxWidth(),
                                        shape = RoundedCornerShape(14.dp),
                                        color = AppleCardSurface
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(32.dp),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(
                                                text = "No lectures scheduled for this day",
                                                fontFamily = AppleSans,
                                                fontSize = 14.sp,
                                                color = AppleSecondaryLabel
                                            )
                                        }
                                    }
                                }
                            } else {
                                // Clean Section Header
                                item {
                                    val countStr = "${classes.size} class${if (classes.size > 1) "es" else ""}"
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = 2.dp, vertical = 4.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            Text(
                                                text = activeDay.getDisplayName(java.time.format.TextStyle.FULL, Locale.getDefault()).uppercase(),
                                                fontFamily = AppleSans,
                                                fontWeight = FontWeight.SemiBold,
                                                fontSize = 12.sp,
                                                letterSpacing = 0.5.sp,
                                                color = AppleSecondaryLabel
                                            )
                                            if (activeDay == today) {
                                                Surface(
                                                    shape = RoundedCornerShape(6.dp),
                                                    color = AppleSystemBlueBg
                                                ) {
                                                    Text(
                                                        text = "TODAY",
                                                        fontFamily = AppleSans,
                                                        fontWeight = FontWeight.Bold,
                                                        fontSize = 9.5.sp,
                                                        letterSpacing = 0.3.sp,
                                                        color = AppleSystemBlue,
                                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                                    )
                                                }
                                            } else if (activeDay == actualTomorrow) {
                                                Surface(
                                                    shape = RoundedCornerShape(6.dp),
                                                    color = AppleElevatedSurface
                                                ) {
                                                    Text(
                                                        text = "TOMORROW",
                                                        fontFamily = AppleSans,
                                                        fontWeight = FontWeight.SemiBold,
                                                        fontSize = 9.5.sp,
                                                        letterSpacing = 0.3.sp,
                                                        color = AppleTertiaryLabel,
                                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                                    )
                                                }
                                            }
                                        }
                                        Text(
                                            text = countStr,
                                            fontFamily = AppleSans,
                                            fontSize = 12.sp,
                                            color = AppleTertiaryLabel
                                        )
                                    }
                                }

                                // Class Cards and Break Strips
                                items(
                                    items = scheduleRows,
                                    key = { row ->
                                        when (row) {
                                            is ScheduleRow.ClassRow -> "class_${row.item.subjectCode}_${row.item.startTime}"
                                            is ScheduleRow.BreakRow -> "break_${row.startTime}_${row.endTime}"
                                        }
                                    }
                                ) { row ->
                                    Box(modifier = Modifier.animateItem()) {
                                        when (row) {
                                            is ScheduleRow.ClassRow -> {
                                                val item = row.item
                                                val status = if (isCurrentDay) {
                                                    item.getStatusAt(currentTime)
                                                } else {
                                                    ClassScheduleStatus.UPCOMING
                                                }
                                                val isNextUpcoming = isCurrentDay && (item == nextUpcomingItem)

                                                SimpleClassCard(
                                                    item = item,
                                                    status = status,
                                                    isNextUpcoming = isNextUpcoming,
                                                    isToday = isCurrentDay,
                                                    onClick = { selectedClassForDetails = item }
                                                )
                                            }
                                            is ScheduleRow.BreakRow -> {
                                                BreakStrip(
                                                    breakRow = row,
                                                    isToday = isCurrentDay,
                                                    currentTime = currentTime
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Slide-in Bottom Sheet Pop-Up for Full Class Details
    if (selectedClassForDetails != null) {
        val detailItem = selectedClassForDetails!!
        val isCurrentDay = selectedDay == today
        val status = if (isCurrentDay) {
            detailItem.getStatusAt(LocalTime.now())
        } else {
            ClassScheduleStatus.UPCOMING
        }

        ModalBottomSheet(
            onDismissRequest = { selectedClassForDetails = null },
            sheetState = sheetState,
            containerColor = AppleElevatedSurface,
            contentColor = AppleLabel,
            shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp),
            dragHandle = {
                BottomSheetDefaults.DragHandle(color = AppleTertiaryLabel)
            }
        ) {
            ClassDetailBottomSheet(
                item = detailItem,
                status = status,
                isToday = isCurrentDay,
                onDismiss = { selectedClassForDetails = null }
            )
        }
    }

    // Slide-in Bottom Sheet Pop-Up for Academic Calendar Direct Access
    if (showAcademicCalendarSheet) {
        val academicCalendarSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        ModalBottomSheet(
            onDismissRequest = { showAcademicCalendarSheet = false },
            sheetState = academicCalendarSheetState,
            containerColor = AppleElevatedSurface,
            contentColor = AppleLabel,
            shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
            dragHandle = {
                BottomSheetDefaults.DragHandle(color = AppleTertiaryLabel)
            }
        ) {
            AcademicCalendarBottomSheetContent(
                events = academicEvents,
                prefs = prefs,
                onDismiss = { showAcademicCalendarSheet = false },
                onOpenPdf = { cloudRepo.openPdfInExternalViewer() }
            )
        }
    }
}

/**
 * Unified Apple Segmented Control for 5 School Days (Mon – Fri).
 * Sleek, borderless container with perfectly proportioned day tabs.
 */
@Composable
fun AppleWeekSegmentedControl(
    days: List<DayOfWeek>,
    selectedDay: DayOfWeek,
    today: DayOfWeek,
    onDaySelected: (DayOfWeek) -> Unit,
    modifier: Modifier = Modifier
) {
    val selectedIndex = days.indexOf(selectedDay).coerceAtLeast(0)
    val animatedIndex by animateFloatAsState(
        targetValue = selectedIndex.toFloat(),
        animationSpec = spring(
            dampingRatio = 0.82f,
            stiffness = Spring.StiffnessMediumLow
        ),
        label = "week_pill_slider"
    )

    Surface(
        modifier = modifier.height(42.dp),
        shape = RoundedCornerShape(12.dp),
        color = AppleCardSurface
    ) {
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .padding(3.dp)
        ) {
            val totalWidth = maxWidth
            val count = days.size.coerceAtLeast(1)
            val tabWidth = totalWidth / count

            // Fluid Sliding Pill
            Box(
                modifier = Modifier
                    .offset(x = tabWidth * animatedIndex)
                    .width(tabWidth)
                    .fillMaxHeight()
                    .clip(RoundedCornerShape(9.dp))
                    .background(AppleSystemBlue)
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                days.forEachIndexed { index, day ->
                    val isSelected = index == selectedIndex
                    val isTodayDay = day == today
                    val dayName = when (day) {
                        DayOfWeek.MONDAY -> "Mon"
                        DayOfWeek.TUESDAY -> "Tue"
                        DayOfWeek.WEDNESDAY -> "Wed"
                        DayOfWeek.THURSDAY -> "Thu"
                        DayOfWeek.FRIDAY -> "Fri"
                        else -> day.name.take(3)
                    }

                    val tabTextColor by animateColorAsState(
                        targetValue = when {
                            isSelected -> Color.White
                            isTodayDay -> AppleSystemBlue
                            else -> AppleSecondaryLabel
                        },
                        animationSpec = tween(180),
                        label = "tab_text"
                    )

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(9.dp))
                            .bounceClick(scaleDown = 0.94f) { onDaySelected(day) }
                            .padding(vertical = 7.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = dayName,
                            fontFamily = AppleSans,
                            fontWeight = if (isSelected || isTodayDay) FontWeight.SemiBold else FontWeight.Medium,
                            fontSize = 13.5.sp,
                            letterSpacing = (-0.1).sp,
                            color = tabTextColor
                        )
                    }
                }
            }
        }
    }
}

/**
 * Break Strip separating consecutive classes:
 * Displays duration and time interval with zero dots and zero borders.
 * "Break (1 hr) — 12:00 PM – 1:00 PM"
 */
@Composable
fun BreakStrip(
    breakRow: ScheduleRow.BreakRow,
    isToday: Boolean,
    currentTime: LocalTime
) {
    val isOngoing = isToday && breakRow.isOngoingAt(currentTime)
    val isFinished = isToday && breakRow.isFinishedAt(currentTime)

    val alpha = if (isFinished) 0.5f else 1.0f
    val bgColor = if (isOngoing) AppleElevatedSurface else AppleCardSurface

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .alpha(alpha),
        shape = RoundedCornerShape(10.dp),
        color = bgColor
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 9.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "Break",
                    fontFamily = AppleSans,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 12.sp,
                    color = if (isOngoing) AppleSystemBlue else AppleSecondaryLabel
                )

                if (isOngoing) {
                    Surface(
                        shape = RoundedCornerShape(4.dp),
                        color = AppleSystemBlueBg
                    ) {
                        Text(
                            text = "NOW",
                            fontFamily = AppleSans,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = AppleSystemBlue,
                            modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp)
                        )
                    }
                }

                Text(
                    text = "(${breakRow.durationFormatted})",
                    fontFamily = AppleSans,
                    fontWeight = FontWeight.Normal,
                    fontSize = 11.sp,
                    color = AppleTertiaryLabel
                )
            }

            Text(
                text = breakRow.timeSlotDisplay,
                fontFamily = AppleSans,
                fontWeight = FontWeight.Normal,
                fontSize = 11.sp,
                color = AppleTertiaryLabel
            )
        }
    }
}

/**
 * Minimalist, borderless class card showing:
 * 1. Course Name
 * 2. Time
 * 3. Type
 * No dots, no borders.
 */
@Composable
fun SimpleClassCard(
    item: ClassScheduleItem,
    status: ClassScheduleStatus,
    isNextUpcoming: Boolean,
    isToday: Boolean,
    onClick: () -> Unit
) {
    val isOngoing = isToday && status == ClassScheduleStatus.ONGOING
    val isFinished = isToday && status == ClassScheduleStatus.FINISHED

    val typeColor = when (item.classType) {
        ClassType.LECTURE -> AppleSystemBlue
        ClassType.LAB -> AppleSystemGreen
        ClassType.TUTORIAL -> AppleSystemOrange
    }

    val typeBg = when (item.classType) {
        ClassType.LECTURE -> AppleSystemBlueBg
        ClassType.LAB -> AppleSystemGreenBg
        ClassType.TUTORIAL -> AppleSystemOrangeBg
    }

    val cardAlpha = if (isFinished) 0.5f else 1.0f

    // Borderless surfaces with subtle elevation for ongoing
    val cardBg = when {
        isOngoing -> AppleElevatedSurface
        isFinished -> AppleCardSurface.copy(alpha = 0.45f)
        else -> AppleCardSurface
    }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .alpha(cardAlpha)
            .clip(RoundedCornerShape(14.dp))
            .bounceClick(scaleDown = 0.96f) { onClick() },
        shape = RoundedCornerShape(14.dp),
        color = cardBg
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // Course name & Time
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = item.subjectName,
                    fontFamily = AppleSans,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 15.5.sp,
                    letterSpacing = (-0.2).sp,
                    color = if (isFinished) AppleSecondaryLabel else AppleLabel,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = "${item.startTimeFormatted} – ${item.endTimeFormatted}",
                    fontFamily = AppleSans,
                    fontWeight = if (isOngoing) FontWeight.SemiBold else FontWeight.Medium,
                    fontSize = 13.sp,
                    color = if (isOngoing) AppleSystemBlue else AppleSecondaryLabel
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            // State tag (NOW / NEXT / DONE) & Type badge (LECTURE, LAB, TUTORIAL)
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                if (isOngoing) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = AppleSystemGreen.copy(alpha = 0.18f)
                    ) {
                        Text(
                            text = "NOW",
                            fontFamily = AppleSans,
                            fontWeight = FontWeight.Bold,
                            fontSize = 9.5.sp,
                            color = AppleSystemGreen,
                            modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp)
                        )
                    }
                } else if (isNextUpcoming) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = AppleSystemBlueBg
                    ) {
                        Text(
                            text = "NEXT",
                            fontFamily = AppleSans,
                            fontWeight = FontWeight.Bold,
                            fontSize = 9.5.sp,
                            color = AppleSystemBlue,
                            modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp)
                        )
                    }
                } else if (isFinished) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = AppleBackground.copy(alpha = 0.6f)
                    ) {
                        Text(
                            text = "DONE",
                            fontFamily = AppleSans,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 9.sp,
                            color = AppleTertiaryLabel,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }

                // Type Badge (LECTURE / LAB / TUTORIAL)
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = typeBg
                ) {
                    Text(
                        text = item.classType.name,
                        fontFamily = AppleSans,
                        fontWeight = FontWeight.Bold,
                        fontSize = 10.sp,
                        color = typeColor,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.5.dp)
                    )
                }
            }
        }
    }
}

/**
 * Slide-in Bottom Sheet Modal displaying comprehensive class details:
 * Course Name, Subject Code, Time, Type, Room, Faculty, and Status.
 */
@Composable
fun ClassDetailBottomSheet(
    item: ClassScheduleItem,
    status: ClassScheduleStatus,
    isToday: Boolean,
    onDismiss: () -> Unit
) {
    val typeColor = when (item.classType) {
        ClassType.LECTURE -> AppleSystemBlue
        ClassType.LAB -> AppleSystemGreen
        ClassType.TUTORIAL -> AppleSystemOrange
    }

    val typeBg = when (item.classType) {
        ClassType.LECTURE -> AppleSystemBlueBg
        ClassType.LAB -> AppleSystemGreenBg
        ClassType.TUTORIAL -> AppleSystemOrangeBg
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp)
            .padding(bottom = 36.dp)
    ) {
        // Course Name
        Text(
            text = item.subjectName,
            fontFamily = AppleSans,
            fontWeight = FontWeight.Bold,
            fontSize = 22.sp,
            letterSpacing = (-0.5).sp,
            color = AppleLabel
        )

        Spacer(modifier = Modifier.height(10.dp))

        // Badges Row: Subject Code, Class Type, Status
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
                    text = item.subjectCode,
                    fontFamily = AppleSans,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 12.sp,
                    color = AppleSecondaryLabel,
                    modifier = Modifier.padding(horizontal = 9.dp, vertical = 4.dp)
                )
            }

            // Type Badge
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = typeBg
            ) {
                Text(
                    text = item.classType.name,
                    fontFamily = AppleSans,
                    fontWeight = FontWeight.Bold,
                    fontSize = 11.sp,
                    color = typeColor,
                    modifier = Modifier.padding(horizontal = 9.dp, vertical = 4.dp)
                )
            }

            // Status Badge
            if (isToday) {
                val statusText = when (status) {
                    ClassScheduleStatus.ONGOING -> "Happening Now"
                    ClassScheduleStatus.UPCOMING -> "Upcoming"
                    ClassScheduleStatus.FINISHED -> "Completed"
                }
                val statusColor = when (status) {
                    ClassScheduleStatus.ONGOING -> AppleSystemGreen
                    ClassScheduleStatus.UPCOMING -> AppleSystemBlue
                    ClassScheduleStatus.FINISHED -> AppleTertiaryLabel
                }
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = statusColor.copy(alpha = 0.15f)
                ) {
                    Text(
                        text = statusText,
                        fontFamily = AppleSans,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 11.sp,
                        color = statusColor,
                        modifier = Modifier.padding(horizontal = 9.dp, vertical = 4.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Details Container (borderless Apple card surface)
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = AppleCardSurface,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Time Slot
                DetailInfoRow(
                    iconRes = R.drawable.ic_huge_schedule,
                    iconTint = AppleSystemBlue,
                    label = "Time Slot",
                    value = "${item.startTimeFormatted} – ${item.endTimeFormatted}"
                )

                // Location / Room
                DetailInfoRow(
                    iconRes = R.drawable.ic_huge_location,
                    iconTint = AppleSystemOrange,
                    label = "Location",
                    value = item.roomNumber
                )

                // Faculty Instructor
                DetailInfoRow(
                    iconRes = R.drawable.ic_huge_user,
                    iconTint = AppleSystemGreen,
                    label = "Faculty Instructor",
                    value = item.facultyName
                )

                // Scheduled Day
                DetailInfoRow(
                    iconRes = R.drawable.ic_huge_schedule,
                    iconTint = AppleSecondaryLabel,
                    label = "Day",
                    value = item.dayOfWeek.getDisplayName(java.time.format.TextStyle.FULL, Locale.getDefault())
                )
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Done button
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
private fun DetailInfoRow(
    iconRes: Int,
    iconTint: Color,
    label: String,
    value: String
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Surface(
            shape = RoundedCornerShape(10.dp),
            color = iconTint.copy(alpha = 0.12f),
            modifier = Modifier.size(34.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    painter = painterResource(id = iconRes),
                    contentDescription = null,
                    tint = iconTint,
                    modifier = Modifier.size(17.dp)
                )
            }
        }

        Spacer(modifier = Modifier.width(14.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = label,
                fontFamily = AppleSans,
                fontSize = 11.5.sp,
                color = AppleSecondaryLabel
            )
            Spacer(modifier = Modifier.height(1.dp))
            Text(
                text = value,
                fontFamily = AppleSans,
                fontWeight = FontWeight.Medium,
                fontSize = 14.5.sp,
                color = AppleLabel
            )
        }
    }
}
