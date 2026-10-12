package com.example.nsutconnect.ui.more

import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Widgets
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.nsutconnect.R
import com.example.nsutconnect.data.local.UserPreferences
import com.example.nsutconnect.theme.*
import com.example.nsutconnect.widget.NsutAttendanceWidgetReceiver
import com.example.nsutconnect.widget.NsutScheduleWidgetReceiver
import com.example.nsutconnect.data.model.ClassType
import androidx.compose.ui.text.style.TextOverflow
import java.time.LocalDate
import java.time.LocalTime

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WidgetStudioScreen(
    prefs: UserPreferences,
    onBack: () -> Unit
) {
    val context = LocalContext.current

    var selectedTheme by remember { mutableStateOf(prefs.getWidgetTheme()) }
    var bunksEnabled by remember { mutableStateOf(prefs.isWidgetBunksEnabled()) }
    var bannerEnabled by remember { mutableStateOf(prefs.isWidgetBannerEnabled()) }
    var previewMode by remember { mutableIntStateOf(0) } // 0 = Attendance, 1 = Schedule

    val realAttendance = remember { prefs.getCachedAttendance() }
    val realTimetable = remember { prefs.getCachedTimetable() }

    val themeCardColor = when (selectedTheme) {
        "oled" -> Color(0xFF000000)
        "dark" -> Color(0xFF16161C)
        "translucent" -> Color(0xD012131F)
        "midnight" -> Color(0xFF0A0D1A)
        else -> Color(0xFF000000)
    }

    val themeBorderColor = when (selectedTheme) {
        "oled" -> Color(0xFF26262E)
        "dark" -> Color(0xFF2A2A38)
        "translucent" -> Color(0xFF36394F)
        "midnight" -> Color(0xFF1F2A56)
        else -> Color(0xFF26262E)
    }

    fun applyAndBroadcast() {
        NsutAttendanceWidgetReceiver.updateAll(context)
        NsutScheduleWidgetReceiver.updateAll(context)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Widget Studio",
                        fontFamily = AppleSans,
                        fontWeight = FontWeight.Bold,
                        fontSize = 20.sp,
                        color = AppleLabel
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = AppleLabel
                        )
                    }
                },
                actions = {
                    IconButton(
                        onClick = {
                            applyAndBroadcast()
                            Toast.makeText(context, "Widgets refreshed", Toast.LENGTH_SHORT).show()
                        }
                    ) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Refresh",
                            tint = AppleSystemBlue
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = AppleBackground,
                    titleContentColor = AppleLabel
                )
            )
        },
        containerColor = AppleBackground,
        bottomBar = {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = AppleBackground,
                tonalElevation = 8.dp
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = {
                            val pinned = if (previewMode == 0) {
                                NsutAttendanceWidgetReceiver.requestPinWidget(context)
                            } else {
                                NsutScheduleWidgetReceiver.requestPinWidget(context)
                            }
                            if (!pinned) {
                                Toast.makeText(
                                    context,
                                    "Long-press your home screen to place the widget",
                                    Toast.LENGTH_LONG
                                ).show()
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = AppleSystemBlue,
                            contentColor = Color.White
                        )
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (previewMode == 0) "Add Attendance Widget" else "Add Schedule Widget",
                            fontFamily = AppleSans,
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp
                        )
                    }
                }
            }
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            contentPadding = PaddingValues(top = 8.dp, bottom = 16.dp)
        ) {
            // Todoist-style Segmented Widget Switcher
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(AppleCardSurface)
                        .padding(3.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(10.dp))
                            .background(if (previewMode == 0) AppleElevatedSurface else Color.Transparent)
                            .clickable { previewMode = 0 }
                            .padding(vertical = 9.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Attendance",
                            fontFamily = AppleSans,
                            fontSize = 13.sp,
                            fontWeight = if (previewMode == 0) FontWeight.Bold else FontWeight.Medium,
                            color = if (previewMode == 0) Color.White else AppleSecondaryLabel
                        )
                    }

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(10.dp))
                            .background(if (previewMode == 1) AppleElevatedSurface else Color.Transparent)
                            .clickable { previewMode = 1 }
                            .padding(vertical = 9.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Schedule",
                            fontFamily = AppleSans,
                            fontSize = 13.sp,
                            fontWeight = if (previewMode == 1) FontWeight.Bold else FontWeight.Medium,
                            color = if (previewMode == 1) Color.White else AppleSecondaryLabel
                        )
                    }
                }
            }

            // Live Interactive Preview Card (with real user data)
            item {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    color = themeCardColor
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp)
                    ) {
                        // Header Row
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // NSync Brand + Tag
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = "N",
                                        fontFamily = UnkemptFont,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 17.sp,
                                        color = Color.White
                                    )
                                    Text(
                                        text = "Sync",
                                        fontFamily = UnkemptFont,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 17.sp,
                                        color = AppleSystemBlue
                                    )
                                }

                                Text(
                                    text = if (previewMode == 0) "Attendance" else "Schedule",
                                    fontFamily = AppleSans,
                                    fontWeight = FontWeight.Medium,
                                    fontSize = 15.sp,
                                    color = AppleSystemBlue,
                                    modifier = Modifier.padding(start = 6.dp)
                                )
                            }

                            // Live badge & Restyled Refresh Button (no bg when idle)
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                if (previewMode == 1) {
                                    val realTimetable = prefs.getCachedTimetable()
                                    val today = LocalDate.now().dayOfWeek
                                    val now = LocalTime.now()
                                    val isWeekend = today == java.time.DayOfWeek.SATURDAY || today == java.time.DayOfWeek.SUNDAY
                                    val isTodayCompleted = realTimetable?.isDayCompleted(today, now) ?: false
                                    val badgeText = when {
                                        isWeekend -> "MONDAY"
                                        isTodayCompleted -> {
                                            val tomorrow = today.plus(1)
                                            if (tomorrow == java.time.DayOfWeek.SATURDAY || tomorrow == java.time.DayOfWeek.SUNDAY) {
                                                "MONDAY"
                                            } else {
                                                "TOMORROW"
                                            }
                                        }
                                        else -> "TODAY"
                                    }

                                    Surface(
                                        shape = RoundedCornerShape(6.dp),
                                        color = Color(0xFF2C2C2E),
                                        modifier = Modifier.padding(end = 8.dp)
                                    ) {
                                        Text(
                                            text = badgeText,
                                            color = AppleSystemBlue,
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Bold,
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                        )
                                    }
                                }

                                Box(
                                    modifier = Modifier
                                        .size(28.dp)
                                        .clip(CircleShape)
                                        .clickable {
                                            applyAndBroadcast()
                                            Toast.makeText(context, "Widgets refreshed", Toast.LENGTH_SHORT).show()
                                        },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        painter = painterResource(id = R.drawable.ic_huge_refresh),
                                        contentDescription = "Refresh",
                                        tint = AppleSystemBlue,
                                        modifier = Modifier.size(15.dp)
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        if (previewMode == 0) {
                            // Real attendance subjects (excluding 0 total classes) or clean fallback
                            val subjectsToShow = realAttendance?.records
                                ?.filter { it.totalClasses > 0 }
                                ?.sortedBy { it.percentage } ?: listOf(
                                com.example.nsutconnect.data.model.AttendanceRecord(
                                    subjectCode = "VTECC305",
                                    subjectName = "Digital Circuits and Systems",
                                    attendedClasses = 24,
                                    totalClasses = 26
                                ),
                                com.example.nsutconnect.data.model.AttendanceRecord(
                                    subjectCode = "VTECC303",
                                    subjectName = "Probability Theory and Random Process",
                                    attendedClasses = 23,
                                    totalClasses = 29
                                ),
                                com.example.nsutconnect.data.model.AttendanceRecord(
                                    subjectCode = "VTECC302",
                                    subjectName = "Signals and Systems",
                                    attendedClasses = 16,
                                    totalClasses = 24
                                )
                            )

                            subjectsToShow.forEachIndexed { idx, sub ->
                                val pct = sub.percentage
                                val isSafe = pct >= 75.0
                                val bunkText = if (isSafe) {
                                    val safe = sub.calculateSafeBunks()
                                    if (safe > 0) "+$safe bunks" else "Safe"
                                } else {
                                    "Need ${sub.calculateClassesNeeded()}"
                                }

                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 5.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = sub.subjectName,
                                        fontFamily = AppleSans,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp,
                                        color = Color.White,
                                        maxLines = 1,
                                        modifier = Modifier.weight(1f)
                                    )

                                    if (bunksEnabled) {
                                        Text(
                                            text = bunkText,
                                            color = if (isSafe) Color(0xFF30D158) else if (pct >= 70.0) Color(0xFFFF9F0A) else Color(0xFFFF453A),
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            modifier = Modifier.padding(end = 8.dp)
                                        )
                                    }

                                    Surface(
                                        shape = RoundedCornerShape(6.dp),
                                        color = if (isSafe) Color(0xFF0D2818) else if (pct >= 70.0) Color(0xFF2C1E0A) else Color(0xFF2C0E0E),
                                        modifier = Modifier.width(48.dp)
                                    ) {
                                        Box(
                                            contentAlignment = Alignment.Center,
                                            modifier = Modifier.padding(vertical = 2.dp)
                                        ) {
                                            Text(
                                                text = String.format(java.util.Locale.getDefault(), "%.1f%%", pct),
                                                color = if (isSafe) Color(0xFF2ECC71) else if (pct >= 70.0) Color(0xFFFF9F0A) else Color(0xFFFF453A),
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                    }
                                }

                                if (idx < subjectsToShow.size - 1) {
                                    HorizontalDivider(
                                        color = Color(0xFF2C2C2E),
                                        thickness = 0.5.dp,
                                        modifier = Modifier.padding(vertical = 3.dp)
                                    )
                                }
                            }
                        } else {
                            // Dynamic Schedule Preview matching live widget
                            val realTimetable = prefs.getCachedTimetable()
                            val today = LocalDate.now().dayOfWeek
                            val now = LocalTime.now()
                            val isWeekend = today == java.time.DayOfWeek.SATURDAY || today == java.time.DayOfWeek.SUNDAY
                            val isTodayCompleted = realTimetable?.isDayCompleted(today, now) ?: false
                            val (activeDay, isToday) = when {
                                isWeekend -> Pair(java.time.DayOfWeek.MONDAY, false)
                                isTodayCompleted -> {
                                    val tomorrow = today.plus(1)
                                    if (tomorrow == java.time.DayOfWeek.SATURDAY || tomorrow == java.time.DayOfWeek.SUNDAY) {
                                        Pair(java.time.DayOfWeek.MONDAY, false)
                                    } else {
                                        Pair(tomorrow, false)
                                    }
                                }
                                else -> Pair(today, true)
                            }
                            val activeClasses = realTimetable?.getScheduleFor(activeDay) ?: emptyList()

                            val classesToShow = if (activeClasses.isNotEmpty()) {
                                activeClasses.sortedBy { it.startTime }
                            } else {
                                listOf(
                                    com.example.nsutconnect.data.model.ClassScheduleItem(
                                        id = "mock1",
                                        dayOfWeek = java.time.DayOfWeek.MONDAY,
                                        subjectCode = "VTECC305",
                                        subjectName = "Digital Circuits and Systems",
                                        roomNumber = "Hall 302",
                                        facultyName = "Dr. Anand Gupta",
                                        startTime = "10:00",
                                        endTime = "11:00",
                                        classType = ClassType.LECTURE
                                    ),
                                    com.example.nsutconnect.data.model.ClassScheduleItem(
                                        id = "mock2",
                                        dayOfWeek = java.time.DayOfWeek.MONDAY,
                                        subjectCode = "VTECC304",
                                        subjectName = "Microelectronics",
                                        roomNumber = "Room 8003",
                                        facultyName = "Prof. Raj Senani",
                                        startTime = "12:00",
                                        endTime = "13:00",
                                        classType = ClassType.LECTURE
                                    ),
                                    com.example.nsutconnect.data.model.ClassScheduleItem(
                                        id = "mock3",
                                        dayOfWeek = java.time.DayOfWeek.MONDAY,
                                        subjectCode = "VTECC302",
                                        subjectName = "Signals and Systems",
                                        roomNumber = "Block VI - 302",
                                        facultyName = "Dr. Dharmendra",
                                        startTime = "14:00",
                                        endTime = "16:00",
                                        classType = ClassType.TUTORIAL
                                    ),
                                    com.example.nsutconnect.data.model.ClassScheduleItem(
                                        id = "mock4",
                                        dayOfWeek = java.time.DayOfWeek.MONDAY,
                                        subjectCode = "VTECC303",
                                        subjectName = "Probability Theory and Random Process",
                                        roomNumber = "Room 8003",
                                        facultyName = "Prof. Amit Singhal",
                                        startTime = "16:00",
                                        endTime = "18:00",
                                        classType = ClassType.TUTORIAL
                                    )
                                )
                            }

                            if (classesToShow.isEmpty()) {
                                Text(
                                    text = if (!isToday) "No classes scheduled for tomorrow 🎉" else "No classes scheduled for today 🌴",
                                    color = Color(0xFF8E8E93),
                                    fontSize = 11.sp,
                                    fontFamily = AppleSans,
                                    modifier = Modifier.padding(vertical = 12.dp)
                                )
                            }

                            classesToShow.forEachIndexed { idx, item ->
                                val isOngoing = isToday && item.isOngoingAt(now)
                                val isFinished = isToday && item.isFinishedAt(now)

                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 5.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = item.subjectName,
                                            fontFamily = AppleSans,
                                            fontWeight = FontWeight.SemiBold,
                                            fontSize = 13.5.sp,
                                            color = if (isFinished) AppleSecondaryLabel else Color.White,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        val timeDisplay = "${item.startTimeFormatted} – ${item.endTimeFormatted}"
                                        Text(
                                            text = timeDisplay,
                                            fontFamily = AppleSans,
                                            fontSize = 11.5.sp,
                                            color = if (isOngoing) AppleSystemBlue else AppleSecondaryLabel,
                                            modifier = Modifier.padding(top = 2.dp)
                                        )
                                    }

                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        if (isOngoing) {
                                            Text(
                                                text = "now",
                                                fontFamily = AppleSans,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 11.sp,
                                                color = Color(0xFF22C55E)
                                            )
                                        } else if (isFinished) {
                                            Text(
                                                text = "done",
                                                fontFamily = AppleSans,
                                                fontWeight = FontWeight.Medium,
                                                fontSize = 11.sp,
                                                color = Color(0xFF71717A)
                                            )
                                        }

                                        val (typeBg, typeColor) = when (item.classType) {
                                            ClassType.LECTURE -> Pair(Color(0xFF1E203B), AppleSystemBlue)
                                            ClassType.LAB -> Pair(Color(0xFF0E2A18), Color(0xFF22C55E))
                                            ClassType.TUTORIAL -> Pair(Color(0xFF2B1D08), Color(0xFFF59E0B))
                                        }

                                        Surface(
                                            shape = RoundedCornerShape(7.dp),
                                            color = typeBg
                                        ) {
                                            Text(
                                                text = item.classType.shortName,
                                                fontFamily = AppleSans,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 9.5.sp,
                                                color = typeColor,
                                                modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp)
                                            )
                                        }
                                    }
                                }

                                if (idx < classesToShow.size - 1) {
                                    HorizontalDivider(
                                        color = Color(0xFF2C2C2E),
                                        thickness = 0.5.dp,
                                        modifier = Modifier.padding(vertical = 3.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Todoist-style Unified Configuration Card
            item {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    color = AppleCardSurface
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        // Dynamic Section Header per selected tab
                        Text(
                            text = if (previewMode == 0) "ATTENDANCE APPEARANCE" else "SCHEDULE APPEARANCE",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = AppleSecondaryLabel,
                            letterSpacing = 0.5.sp
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        // Theme Selection (Clean 3-way Segmented Control)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .background(AppleElevatedSurface)
                                .padding(2.dp)
                        ) {
                            listOf(
                                "oled" to "OLED Black",
                                "dark" to "Slate Dark",
                                "translucent" to "Glass"
                            ).forEach { (themeId, themeName) ->
                                val isSelected = selectedTheme == themeId
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(if (isSelected) AppleSystemBlue else Color.Transparent)
                                        .clickable {
                                            selectedTheme = themeId
                                            prefs.setWidgetTheme(themeId)
                                            applyAndBroadcast()
                                        }
                                        .padding(vertical = 8.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = themeName,
                                        fontFamily = AppleSans,
                                        fontSize = 12.sp,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                        color = if (isSelected) Color.White else AppleSecondaryLabel
                                    )
                                }
                            }
                        }

                        HorizontalDivider(
                            color = Color(0xFF2C2C2E),
                            thickness = 0.5.dp,
                            modifier = Modifier.padding(vertical = 14.dp)
                        )

                        // Separate toggles per widget mode
                        if (previewMode == 0) {
                            // Attendance-Only Toggle: Safe Bunk Counter
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "Safe Bunk Counter",
                                        fontFamily = AppleSans,
                                        fontWeight = FontWeight.SemiBold,
                                        fontSize = 14.sp,
                                        color = Color.White
                                    )
                                    Text(
                                        text = "Show calculated safe bunks / classes needed",
                                        fontFamily = AppleSans,
                                        fontSize = 11.sp,
                                        color = AppleSecondaryLabel
                                    )
                                }
                                Switch(
                                    checked = bunksEnabled,
                                    onCheckedChange = {
                                        bunksEnabled = it
                                        prefs.setWidgetBunksEnabled(it)
                                        applyAndBroadcast()
                                    },
                                    colors = SwitchDefaults.colors(
                                        checkedThumbColor = Color.White,
                                        checkedTrackColor = AppleSystemBlue,
                                        uncheckedThumbColor = Color.LightGray,
                                        uncheckedTrackColor = AppleElevatedSurface
                                    )
                                )
                            }
                        } else {
                            // Schedule-Only Toggle: Highlight Active Lecture
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "Highlight Active Lecture",
                                        fontFamily = AppleSans,
                                        fontWeight = FontWeight.SemiBold,
                                        fontSize = 14.sp,
                                        color = Color.White
                                    )
                                    Text(
                                        text = "Show NOW / NEXT live status tags on class cards",
                                        fontFamily = AppleSans,
                                        fontSize = 11.sp,
                                        color = AppleSecondaryLabel
                                    )
                                }
                                Switch(
                                    checked = bannerEnabled,
                                    onCheckedChange = {
                                        bannerEnabled = it
                                        prefs.setWidgetBannerEnabled(it)
                                        applyAndBroadcast()
                                    },
                                    colors = SwitchDefaults.colors(
                                        checkedThumbColor = Color.White,
                                        checkedTrackColor = AppleSystemBlue,
                                        uncheckedThumbColor = Color.LightGray,
                                        uncheckedTrackColor = AppleElevatedSurface
                                    )
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
