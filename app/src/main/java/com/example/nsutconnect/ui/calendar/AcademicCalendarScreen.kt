package com.example.nsutconnect.ui.calendar

import android.content.Context
import android.content.Intent
import android.provider.CalendarContract
import android.widget.Toast
import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.EventAvailable
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Today
import com.example.nsutconnect.data.calendar.CloudCalendarRepository
import androidx.compose.material3.*
import androidx.compose.runtime.*
import kotlinx.coroutines.launch
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.nsutconnect.data.calendar.GeminiAcademicCalendarAgent
import com.example.nsutconnect.data.model.AcademicEvent
import com.example.nsutconnect.data.model.CalendarCategory
import com.example.nsutconnect.theme.*
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AcademicCalendarScreen(
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val agent = remember { GeminiAcademicCalendarAgent(context) }
    val cloudRepo = agent.cloudRepo

    var events by remember { mutableStateOf<List<AcademicEvent>>(emptyList()) }
    val today = remember { LocalDate.now() }
    var currentMonth by remember { mutableStateOf(YearMonth.from(today)) }
    var selectedDate by remember { mutableStateOf<LocalDate?>(null) }
    var showAllEvents by remember { mutableStateOf(false) }
    var isCheckingUpdates by remember { mutableStateOf(false) }

    // Silently fetch and sync official events, and auto-check for updates from NSUT portal
    LaunchedEffect(Unit) {
        val cached = cloudRepo.fetchFromCloudDatabase()
        if (cached.isNotEmpty()) {
            events = cached
        }
        isCheckingUpdates = true
        agent.syncAcademicCalendarFlow(forceScanNewDoc = false).collect { scanState ->
            if (!scanState.isScanning && !scanState.isCloudSyncing) {
                isCheckingUpdates = false
                val fresh = cloudRepo.fetchFromCloudDatabase()
                if (fresh.isNotEmpty()) {
                    events = fresh
                }
            }
        }
        isCheckingUpdates = false
    }

    val upcomingEvents = remember(events, today) {
        events.filter { event ->
            val end = runCatching { LocalDate.parse(event.endDate) }.getOrNull()
                ?: runCatching { LocalDate.parse(event.startDate) }.getOrNull()
                ?: return@filter true
            !end.isBefore(today)
        }.sortedWith(compareBy({ it.startDate }, { it.endDate }))
    }

    val selectedDateEvents = remember(events, selectedDate) {
        if (selectedDate == null) emptyList()
        else events.filter { it.containsDate(selectedDate!!) }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "Academic Calendar",
                            fontSize = 19.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Text(
                            text = if (isCheckingUpdates) "Checking official portal for updates..." else "Session 2026-27 • Official Schedule",
                            fontSize = 11.sp,
                            color = if (isCheckingUpdates) AppleAccent else TextSecondary
                        )
                    }
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
                        onClick = { cloudRepo.openPdfInExternalViewer() },
                        modifier = Modifier.bounceClick()
                    ) {
                        Icon(
                            imageVector = Icons.Default.PictureAsPdf,
                            contentDescription = "Official PDF Circular",
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
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // 1. Monthly Calendar on Top
            item {
                MonthlyCalendarView(
                    currentMonth = currentMonth,
                    selectedDate = selectedDate,
                    today = today,
                    events = events,
                    onMonthChange = { newMonth ->
                        currentMonth = newMonth
                    },
                    onDateSelect = { date ->
                        selectedDate = if (selectedDate == date) null else date
                    }
                )
            }

            // 2. Upcoming Events List Below
            if (selectedDate != null) {
                // When a specific date is clicked on the monthly calendar
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 2.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "Events on ${selectedDate!!.format(DateTimeFormatter.ofPattern("EEE, dd MMM yyyy", Locale.ENGLISH))}",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                            Text(
                                text = if (selectedDateEvents.isEmpty()) "No scheduled milestone"
                                else "${selectedDateEvents.size} milestone${if (selectedDateEvents.size > 1) "s" else ""}",
                                fontSize = 12.sp,
                                color = TextSecondary
                            )
                        }

                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = AppleCardSurface,
                            border = BorderStroke(1.dp, AppleBorder),
                            modifier = Modifier.bounceClick { selectedDate = null }
                        ) {
                            Text(
                                text = "Show All Upcoming",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = AppleAccent,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                            )
                        }
                    }
                }

                if (selectedDateEvents.isEmpty()) {
                    item {
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(14.dp),
                            color = AppleCardSurface,
                            border = BorderStroke(1.dp, AppleBorder)
                        ) {
                            Column(
                                modifier = Modifier.padding(20.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Icon(
                                    imageVector = Icons.Default.CalendarMonth,
                                    contentDescription = null,
                                    tint = TextSecondary,
                                    modifier = Modifier.size(28.dp)
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = "No academic events on this date",
                                    fontSize = 13.sp,
                                    color = TextSecondary
                                )
                            }
                        }
                    }
                } else {
                    items(selectedDateEvents, key = { "sel_${it.id}" }) { event ->
                        AcademicEventCard(
                            event = event,
                            today = today,
                            onAddToCalendar = { openAddToCalendarIntent(context, event) }
                        )
                    }
                }

                // Followed by Other Upcoming Milestones
                item {
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Other Upcoming Milestones",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextSecondary
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = AppleElevatedSurface
                        ) {
                            Text(
                                text = "${upcomingEvents.size}",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = AppleAccent,
                                modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp)
                            )
                        }
                    }
                }

                items(upcomingEvents, key = { "other_${it.id}" }) { event ->
                    Box(modifier = Modifier.animateItem()) {
                        AcademicEventCard(
                            event = event,
                            today = today,
                            onAddToCalendar = { openAddToCalendarIntent(context, event) }
                        )
                    }
                }

            } else {
                // Default: Upcoming Events
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 2.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = if (showAllEvents) "All Academic Milestones" else "Upcoming Events",
                                fontSize = 17.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = AppleElevatedSurface
                            ) {
                                Text(
                                    text = if (showAllEvents) "${events.size}" else "${upcomingEvents.size}",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = AppleAccent,
                                    modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp)
                                )
                            }
                        }

                        // Toggle between Upcoming and All
                        if (events.isNotEmpty() && events.size > upcomingEvents.size) {
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = AppleCardSurface,
                                border = BorderStroke(1.dp, AppleBorder),
                                modifier = Modifier.bounceClick { showAllEvents = !showAllEvents }
                            ) {
                                Text(
                                    text = if (showAllEvents) "Show Upcoming (${upcomingEvents.size})" else "Show All (${events.size})",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = AppleAccent,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                                )
                            }
                        }
                    }
                }

                val listToDisplay = if (showAllEvents) {
                    events.sortedWith(compareBy({ it.startDate }, { it.endDate }))
                } else {
                    upcomingEvents
                }

                if (listToDisplay.isEmpty()) {
                    item {
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(16.dp),
                            color = AppleCardSurface,
                            border = BorderStroke(1.dp, AppleBorder)
                        ) {
                            Column(
                                modifier = Modifier.padding(28.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Icon(
                                    imageVector = Icons.Default.CalendarMonth,
                                    contentDescription = null,
                                    tint = TextSecondary,
                                    modifier = Modifier.size(36.dp)
                                )
                                Spacer(modifier = Modifier.height(10.dp))
                                Text(
                                    text = "No upcoming events found for this session",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = TextSecondary
                                )
                            }
                        }
                    }
                } else {
                    items(listToDisplay, key = { it.id }) { event ->
                        Box(modifier = Modifier.animateItem()) {
                            AcademicEventCard(
                                event = event,
                                today = today,
                                onAddToCalendar = { openAddToCalendarIntent(context, event) }
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * Monthly Calendar View
 * Renders month/year header with navigation, days of the week, and an interactive grid
 * with event category dot indicators and today/selected highlights.
 */
@Composable
fun MonthlyCalendarView(
    currentMonth: YearMonth,
    selectedDate: LocalDate?,
    today: LocalDate,
    events: List<AcademicEvent>,
    onMonthChange: (YearMonth) -> Unit,
    onDateSelect: (LocalDate) -> Unit
) {
    val monthTitle = remember(currentMonth) {
        currentMonth.format(DateTimeFormatter.ofPattern("MMMM yyyy", Locale.ENGLISH))
    }
    val isViewingCurrentMonth = remember(currentMonth, today) {
        currentMonth == YearMonth.from(today)
    }

    val daysOfWeek = remember {
        listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun")
    }

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        color = AppleCardSurface,
        border = BorderStroke(1.dp, AppleBorder)
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 14.dp)
        ) {
            // Month Header & Navigation
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = monthTitle,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    if (!isViewingCurrentMonth) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = AppleElevatedSurface,
                            modifier = Modifier
                                .bounceClick { onMonthChange(YearMonth.from(today)) }
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Today,
                                    contentDescription = "Today",
                                    tint = AppleAccent,
                                    modifier = Modifier.size(13.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "Today",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = AppleAccent
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(4.dp))
                    }

                    IconButton(
                        onClick = { onMonthChange(currentMonth.minusMonths(1)) },
                        modifier = Modifier
                            .size(32.dp)
                            .bounceClick()
                    ) {
                        Icon(
                            imageVector = Icons.Default.ChevronLeft,
                            contentDescription = "Previous Month",
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    IconButton(
                        onClick = { onMonthChange(currentMonth.plusMonths(1)) },
                        modifier = Modifier
                            .size(32.dp)
                            .bounceClick()
                    ) {
                        Icon(
                            imageVector = Icons.Default.ChevronRight,
                            contentDescription = "Next Month",
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Weekday Headers
            Row(
                modifier = Modifier.fillMaxWidth()
            ) {
                for (dayName in daysOfWeek) {
                    Text(
                        text = dayName,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = TextSecondary,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Days Grid with fluid directional slide
            AnimatedContent(
                targetState = currentMonth,
                transitionSpec = {
                    val forward = targetState.isAfter(initialState)
                    appleSlideHorizontally(forward)
                },
                label = "calendar_month_slide"
            ) { activeMonth ->
                val firstDayOfMonth = remember(activeMonth) { activeMonth.atDay(1) }
                val startDayOffset = remember(firstDayOfMonth) {
                    (firstDayOfMonth.dayOfWeek.value - 1) % 7
                }
                val daysInMonth = remember(activeMonth) { activeMonth.lengthOfMonth() }
                val totalSlots = startDayOffset + daysInMonth
                val totalRows = (totalSlots + 6) / 7

                Column(
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    for (rowIndex in 0 until totalRows) {
                        Row(
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            for (colIndex in 0 until 7) {
                                val slotIndex = rowIndex * 7 + colIndex
                                if (slotIndex in startDayOffset until totalSlots) {
                                    val dayNumber = slotIndex - startDayOffset + 1
                                    val date = activeMonth.atDay(dayNumber)
                                    val isToday = date == today
                                    val isSelected = date == selectedDate
                                    val dayEvents = events.filter { it.containsDate(date) }

                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .height(44.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Surface(
                                            modifier = Modifier
                                                .size(36.dp)
                                                .bounceClick { onDateSelect(date) },
                                            shape = CircleShape,
                                            color = when {
                                                isSelected -> AppleAccent
                                                isToday -> AppleElevatedSurface
                                                else -> Color.Transparent
                                            },
                                            border = if (isToday && !isSelected) {
                                                BorderStroke(1.5.dp, AppleAccent)
                                            } else null
                                        ) {
                                            Column(
                                                modifier = Modifier.fillMaxSize(),
                                                horizontalAlignment = Alignment.CenterHorizontally,
                                                verticalArrangement = Arrangement.Center
                                            ) {
                                                Text(
                                                    text = dayNumber.toString(),
                                                    fontSize = 13.sp,
                                                    fontWeight = if (isSelected || isToday) FontWeight.Bold else FontWeight.Medium,
                                                    color = when {
                                                        isSelected -> Color.White
                                                        isToday -> AppleAccent
                                                        else -> Color.White
                                                    }
                                                )

                                                // Event indicator dots
                                                if (dayEvents.isNotEmpty()) {
                                                    Spacer(modifier = Modifier.height(2.dp))
                                                    Row(
                                                        horizontalArrangement = Arrangement.spacedBy(2.dp),
                                                        verticalAlignment = Alignment.CenterVertically
                                                    ) {
                                                        dayEvents.take(3).forEach { evt ->
                                                            val dotColor = if (isSelected) Color.White else getCategoryTheme(evt.category).third
                                                            Box(
                                                                modifier = Modifier
                                                                    .size(3.5.dp)
                                                                    .background(dotColor, CircleShape)
                                                            )
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    }
                                } else {
                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .height(44.dp)
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

/**
 * Modern Card for an Academic Event / Milestone
 */
@Composable
fun AcademicEventCard(
    event: AcademicEvent,
    today: LocalDate,
    onAddToCalendar: (() -> Unit)? = null
) {
    val (catBg, catText, _) = getCategoryTheme(event.category)
    val (relativeText, relativeColor) = getEventRelativeStatus(event.startDate, event.endDate, today)

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = AppleCardSurface,
        border = BorderStroke(1.dp, AppleBorder)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Category & Relative Status row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = catBg
                ) {
                    Text(
                        text = event.category.displayName.uppercase(),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = catText,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                    )
                }

                if (relativeText.isNotBlank()) {
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = relativeColor.copy(alpha = 0.14f)
                    ) {
                        Text(
                            text = relativeText,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = relativeColor,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = event.title,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )

            Spacer(modifier = Modifier.height(10.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.CalendarMonth,
                    contentDescription = null,
                    tint = AppleAccent,
                    modifier = Modifier.size(15.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = formatAcademicEventDates(event.startDate, event.endDate),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color.White
                )
            }
        }
    }
}

/**
 * Checks if a specific date falls within the start and end dates of this event.
 */
fun AcademicEvent.containsDate(date: LocalDate): Boolean {
    val start = runCatching { LocalDate.parse(startDate) }.getOrNull() ?: return false
    val end = runCatching { LocalDate.parse(endDate) }.getOrNull() ?: start
    return !date.isBefore(start) && !date.isAfter(end)
}

/**
 * Formats start and end dates into clean human-readable string.
 */
fun formatAcademicEventDates(startDateStr: String, endDateStr: String): String {
    val start = runCatching { LocalDate.parse(startDateStr) }.getOrNull()
    val end = runCatching { LocalDate.parse(endDateStr) }.getOrNull()
    if (start == null) return startDateStr
    val displayDateFormatter = DateTimeFormatter.ofPattern("EEE, dd MMM yyyy", Locale.ENGLISH)
    if (end == null || start == end) {
        return start.format(displayDateFormatter)
    }
    return if (start.month == end.month && start.year == end.year) {
        val monthYear = start.format(DateTimeFormatter.ofPattern("MMM yyyy", Locale.ENGLISH))
        "${start.dayOfMonth} – ${end.dayOfMonth} $monthYear"
    } else {
        val startFmt = start.format(DateTimeFormatter.ofPattern("dd MMM", Locale.ENGLISH))
        val endFmt = end.format(DateTimeFormatter.ofPattern("dd MMM yyyy", Locale.ENGLISH))
        "$startFmt – $endFmt"
    }
}

/**
 * Calculates relative time badge (Ongoing, Tomorrow, In X days, Next month, etc.)
 */
fun getEventRelativeStatus(startDateStr: String, endDateStr: String, today: LocalDate): Pair<String, Color> {
    val start = runCatching { LocalDate.parse(startDateStr) }.getOrNull() ?: return "" to TextSecondary
    val end = runCatching { LocalDate.parse(endDateStr) }.getOrNull() ?: start

    return when {
        !today.isBefore(start) && !today.isAfter(end) -> {
            if (start == end) "Today" to SafeGreen
            else "Ongoing" to SafeGreen
        }
        start == today.plusDays(1) -> "Tomorrow" to WarningOrange
        today.isBefore(start) -> {
            val days = ChronoUnit.DAYS.between(today, start)
            when {
                days < 7 -> "In $days days" to AppleAccent
                days < 14 -> "Next week" to AppleAccent
                days < 30 -> "In ${days / 7} weeks" to AppleAccent
                else -> {
                    val months = (days / 30).coerceAtLeast(1)
                    "In $months month${if (months > 1) "s" else ""}" to TextSecondary
                }
            }
        }
        else -> "Past" to TextSecondary.copy(alpha = 0.5f)
    }
}

/**
 * Returns color styling for category badge.
 */
fun getCategoryTheme(category: CalendarCategory): Triple<Color, Color, Color> {
    return when (category) {
        CalendarCategory.EXAM -> Triple(
            DangerRed.copy(alpha = 0.16f),
            DangerRed,
            DangerRed
        )
        CalendarCategory.HOLIDAY -> Triple(
            SafeGreen.copy(alpha = 0.16f),
            SafeGreen,
            SafeGreen
        )
        CalendarCategory.ACADEMIC -> Triple(
            AppleAccent.copy(alpha = 0.16f),
            AppleAccent,
            AppleAccent
        )
        CalendarCategory.REGISTRATION -> Triple(
            WarningOrange.copy(alpha = 0.16f),
            WarningOrange,
            WarningOrange
        )
        CalendarCategory.BREAK -> Triple(
            Color(0xFF06B6D4).copy(alpha = 0.16f),
            Color(0xFF06B6D4),
            Color(0xFF06B6D4)
        )
    }
}

/**
 * Launches Android System Calendar Intent so users can save/remind themselves of milestones.
 */
fun openAddToCalendarIntent(context: Context, event: AcademicEvent) {
    try {
        val start = runCatching { LocalDate.parse(event.startDate) }.getOrNull() ?: return
        val end = runCatching { LocalDate.parse(event.endDate) }.getOrNull() ?: start
        val startMillis = start.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
        val endMillis = end.plusDays(1).atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()

        val intent = Intent(Intent.ACTION_INSERT).apply {
            data = CalendarContract.Events.CONTENT_URI
            putExtra(CalendarContract.Events.TITLE, event.title)
            putExtra(CalendarContract.Events.DESCRIPTION, "${event.description}\n[NSUT Academic Calendar - ${event.semester}]")
            putExtra(CalendarContract.Events.EVENT_LOCATION, "Netaji Subhas University of Technology (NSUT)")
            putExtra(CalendarContract.EXTRA_EVENT_ALL_DAY, true)
            putExtra(CalendarContract.EXTRA_EVENT_BEGIN_TIME, startMillis)
            putExtra(CalendarContract.EXTRA_EVENT_END_TIME, endMillis)
        }
        context.startActivity(intent)
    } catch (e: Exception) {
        Toast.makeText(context, "Could not open calendar app", Toast.LENGTH_SHORT).show()
    }
}

