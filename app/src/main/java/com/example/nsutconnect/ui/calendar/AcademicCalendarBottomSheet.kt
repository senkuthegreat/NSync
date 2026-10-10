package com.example.nsutconnect.ui.calendar

import android.content.Context
import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.nsutconnect.data.local.UserPreferences
import com.example.nsutconnect.data.model.AcademicEvent
import com.example.nsutconnect.data.model.CalendarCategory
import com.example.nsutconnect.theme.*
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.util.Locale

/**
 * Slide-in Bottom Sheet content for Academic Calendar direct access from Schedule Screen.
 * Provides interactive month calendar, quick tag filtering, upcoming events list,
 * direct add-to-calendar reminder support, and settings for schedule integration.
 */
@Composable
fun AcademicCalendarBottomSheetContent(
    events: List<AcademicEvent>,
    prefs: UserPreferences,
    onDismiss: () -> Unit,
    onOpenPdf: () -> Unit,
    onPreferencesChanged: () -> Unit = {}
) {
    val context = LocalContext.current
    val today = remember { LocalDate.now() }
    var currentMonth by remember { mutableStateOf(YearMonth.from(today)) }
    var selectedDate by remember { mutableStateOf<LocalDate?>(null) }
    var selectedCategoryTag by remember { mutableStateOf<CalendarCategory?>(null) }

    // Filter events by selected category tag if any
    val filteredEvents = remember(events, selectedCategoryTag) {
        if (selectedCategoryTag == null) events
        else events.filter { it.category == selectedCategoryTag }
    }

    val selectedDateEvents = remember(filteredEvents, selectedDate) {
        if (selectedDate == null) emptyList()
        else filteredEvents.filter { it.containsDate(selectedDate!!) }
    }

    val upcomingEvents = remember(filteredEvents, today) {
        filteredEvents.filter { event ->
            val end = runCatching { LocalDate.parse(event.endDate) }.getOrNull()
                ?: runCatching { LocalDate.parse(event.startDate) }.getOrNull()
                ?: return@filter true
            !end.isBefore(today)
        }.sortedWith(compareBy({ it.startDate }, { it.endDate }))
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .fillMaxHeight(0.92f)
            .padding(horizontal = 16.dp)
            .padding(bottom = 20.dp)
    ) {
        // 1. Header with title & actions
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.CalendarMonth,
                        contentDescription = null,
                        tint = AppleAccent,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Academic Calendar",
                        fontFamily = AppleSans,
                        fontWeight = FontWeight.Bold,
                        fontSize = 20.sp,
                        letterSpacing = (-0.4).sp,
                        color = AppleLabel
                    )
                }
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "Office of Dean Academics",
                    fontFamily = AppleSans,
                    fontSize = 11.5.sp,
                    color = AppleSecondaryLabel
                )
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                // Official PDF circular viewer
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .bounceClick { onOpenPdf() },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.PictureAsPdf,
                        contentDescription = "Official PDF Circular",
                        tint = AppleAccent,
                        modifier = Modifier.size(20.dp)
                    )
                }

                // Close Button
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .bounceClick { onDismiss() },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Close",
                        tint = AppleSecondaryLabel,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }

        // 2. Quick Tag Filter Chips Row for Calendar View
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(bottom = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // "All" filter chip
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = if (selectedCategoryTag == null) AppleAccent else AppleElevatedSurface,
                border = BorderStroke(1.dp, if (selectedCategoryTag == null) AppleAccent else AppleBorder),
                modifier = Modifier.bounceClick { selectedCategoryTag = null }
            ) {
                Text(
                    text = "All (${events.size})",
                    fontFamily = AppleSans,
                    fontSize = 11.5.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = if (selectedCategoryTag == null) Color.White else AppleSecondaryLabel,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                )
            }

            CalendarCategory.values().forEach { cat ->
                val isSelected = selectedCategoryTag == cat
                val (catBg, catText, _) = getCategoryTheme(cat)
                val count = events.count { it.category == cat }

                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = if (isSelected) catText else AppleElevatedSurface,
                    border = BorderStroke(
                        1.dp,
                        if (isSelected) catText else AppleBorder
                    ),
                    modifier = Modifier.bounceClick {
                        selectedCategoryTag = if (isSelected) null else cat
                    }
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(6.dp)
                                .clip(CircleShape)
                                .background(if (isSelected) Color.White else catText)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "${cat.displayName} ($count)",
                            fontFamily = AppleSans,
                            fontSize = 11.5.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                            color = if (isSelected) Color.White else AppleSecondaryLabel
                        )
                    }
                }
            }
        }

        // 4. Scrollable Content: Monthly Calendar + Event Cards
        LazyColumn(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            contentPadding = PaddingValues(bottom = 16.dp)
        ) {
            // Monthly Calendar View
            item {
                MonthlyCalendarView(
                    currentMonth = currentMonth,
                    selectedDate = selectedDate,
                    today = today,
                    events = filteredEvents,
                    onMonthChange = { newMonth -> currentMonth = newMonth },
                    onDateSelect = { date ->
                        selectedDate = if (selectedDate == date) null else date
                    }
                )
            }

            // Events List Section Header
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (selectedDate != null) {
                        Column {
                            Text(
                                text = "Events on ${selectedDate!!.format(DateTimeFormatter.ofPattern("EEE, dd MMM yyyy", Locale.ENGLISH))}",
                                fontFamily = AppleSans,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                color = AppleLabel
                            )
                            Text(
                                text = if (selectedDateEvents.isEmpty()) "No scheduled milestone"
                                else "${selectedDateEvents.size} milestone${if (selectedDateEvents.size > 1) "s" else ""}",
                                fontFamily = AppleSans,
                                fontSize = 11.5.sp,
                                color = AppleSecondaryLabel
                            )
                        }

                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = AppleElevatedSurface,
                            modifier = Modifier.bounceClick { selectedDate = null }
                        ) {
                            Text(
                                text = "Show All",
                                fontFamily = AppleSans,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = AppleAccent,
                                modifier = Modifier.padding(horizontal = 9.dp, vertical = 5.dp)
                            )
                        }
                    } else {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = if (selectedCategoryTag != null) "${selectedCategoryTag!!.displayName} (${upcomingEvents.size})"
                                else "Upcoming Milestones (${upcomingEvents.size})",
                                fontFamily = AppleSans,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                color = AppleLabel
                            )
                        }
                    }
                }
            }

            // Events display
            if (selectedDate != null) {
                if (selectedDateEvents.isEmpty()) {
                    item {
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(14.dp),
                            color = AppleCardSurface
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(24.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "No academic milestones scheduled for this date",
                                    fontFamily = AppleSans,
                                    fontSize = 13.sp,
                                    color = AppleSecondaryLabel
                                )
                            }
                        }
                    }
                } else {
                    items(selectedDateEvents, key = { "sheet_sel_${it.id}" }) { event ->
                        Box(modifier = Modifier.animateItem()) {
                            AcademicEventCard(
                                event = event,
                                today = today,
                                onAddToCalendar = { openAddToCalendarIntent(context, event) }
                            )
                        }
                    }
                }

                // If date was picked, also show Other Upcoming Milestones below
                if (upcomingEvents.isNotEmpty()) {
                    item {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Other Upcoming Milestones",
                            fontFamily = AppleSans,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = AppleSecondaryLabel
                        )
                    }

                    items(upcomingEvents, key = { "sheet_other_${it.id}" }) { event ->
                        Box(modifier = Modifier.animateItem()) {
                            AcademicEventCard(
                                event = event,
                                today = today,
                                onAddToCalendar = { openAddToCalendarIntent(context, event) }
                            )
                        }
                    }
                }
            } else {
                if (upcomingEvents.isEmpty()) {
                    item {
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(14.dp),
                            color = AppleCardSurface
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(24.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "No upcoming events matching this filter",
                                    fontFamily = AppleSans,
                                    fontSize = 13.sp,
                                    color = AppleSecondaryLabel
                                )
                            }
                        }
                    }
                } else {
                    items(upcomingEvents, key = { "sheet_upcoming_${it.id}" }) { event ->
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
