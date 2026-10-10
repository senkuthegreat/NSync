package com.example.nsutconnect.ui.more

import android.widget.Toast
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.PinDrop
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

data class WidgetThemeOption(
    val id: String,
    val title: String,
    val description: String,
    val cardColor: Color,
    val borderColor: Color
)

data class AccentOption(
    val id: String,
    val name: String,
    val color: Color
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WidgetStudioScreen(
    prefs: UserPreferences,
    onBack: () -> Unit
) {
    val context = LocalContext.current

    var selectedTheme by remember { mutableStateOf(prefs.getWidgetTheme()) }
    var selectedAccent by remember { mutableStateOf(prefs.getWidgetAccent()) }
    var bunksEnabled by remember { mutableStateOf(prefs.isWidgetBunksEnabled()) }
    var bannerEnabled by remember { mutableStateOf(prefs.isWidgetBannerEnabled()) }
    var previewMode by remember { mutableIntStateOf(0) } // 0 = Attendance, 1 = Schedule

    val themeOptions = remember {
        listOf(
            WidgetThemeOption("oled", "OLED Pure", "Deep pitch black for AMOLED", Color(0xFF000000), Color(0xFF26262E)),
            WidgetThemeOption("dark", "Dark Slate", "Subtle charcoal finish", Color(0xFF121217), Color(0xFF2A2A38)),
            WidgetThemeOption("translucent", "Translucent", "Glassmorphic frosty blur", Color(0xD010111A), Color(0xFF36394F)),
            WidgetThemeOption("midnight", "Midnight", "Deep navy space vibe", Color(0xFF0A0D1A), Color(0xFF1F2A56))
        )
    }

    val accentOptions = remember {
        listOf(
            AccentOption("indigo", "Indigo", Color(0xFF6366F1)),
            AccentOption("cyan", "Neon Cyan", Color(0xFF06B6D4)),
            AccentOption("emerald", "Emerald", Color(0xFF10B981)),
            AccentOption("amber", "Amber", Color(0xFFF59E0B)),
            AccentOption("purple", "Cyber Purple", Color(0xFF8B5CF6))
        )
    }

    val activeAccentColor = remember(selectedAccent) {
        accentOptions.firstOrNull { it.id == selectedAccent }?.color ?: Color(0xFF6366F1)
    }

    val activeThemeOption = remember(selectedTheme) {
        themeOptions.firstOrNull { it.id == selectedTheme } ?: themeOptions[0]
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Widget Studio",
                        fontFamily = UnkemptFont,
                        fontWeight = FontWeight.Bold,
                        fontSize = 22.sp,
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
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = AppleBackground,
                    titleContentColor = AppleLabel
                )
            )
        },
        containerColor = AppleBackground
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            contentPadding = PaddingValues(top = 4.dp, bottom = 48.dp)
        ) {
            // Live Preview Header & Segmented Tab
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "LIVE WIDGET PREVIEW",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextSecondary,
                        letterSpacing = 1.sp
                    )

                    // Preview Toggle Tabs
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(AppleCardSurface)
                            .padding(2.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(10.dp))
                                .background(if (previewMode == 0) AppleElevatedSurface else Color.Transparent)
                                .clickable { previewMode = 0 }
                                .padding(horizontal = 10.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = "Attendance",
                                fontSize = 11.sp,
                                fontWeight = if (previewMode == 0) FontWeight.Bold else FontWeight.Normal,
                                color = if (previewMode == 0) Color.White else TextSecondary
                            )
                        }
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(10.dp))
                                .background(if (previewMode == 1) AppleElevatedSurface else Color.Transparent)
                                .clickable { previewMode = 1 }
                                .padding(horizontal = 10.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = "Schedule",
                                fontSize = 11.sp,
                                fontWeight = if (previewMode == 1) FontWeight.Bold else FontWeight.Normal,
                                color = if (previewMode == 1) Color.White else TextSecondary
                            )
                        }
                    }
                }
            }

            // Interactive Preview Card
            item {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, activeThemeOption.borderColor, RoundedCornerShape(20.dp)),
                    shape = RoundedCornerShape(20.dp),
                    color = activeThemeOption.cardColor
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp)
                    ) {
                        // Widget Card Header
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = if (previewMode == 0) "NSync Attendance" else "NSync Schedule",
                                    fontFamily = UnkemptFont,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 15.sp,
                                    color = Color.White
                                )
                                Text(
                                    text = if (previewMode == 0) "6 Courses · 11:20 AM" else "Today · Mon, Oct 12",
                                    fontFamily = AppleSans,
                                    fontSize = 10.sp,
                                    color = Color(0xFF8E8E93)
                                )
                            }

                            Row(verticalAlignment = Alignment.CenterVertically) {
                                if (previewMode == 1) {
                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = Color(0xFF2C2C2E),
                                        modifier = Modifier.padding(end = 8.dp)
                                    ) {
                                        Text(
                                            text = "LIVE",
                                            color = activeAccentColor,
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold,
                                            modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp)
                                        )
                                    }
                                }
                                Icon(
                                    painter = painterResource(id = R.drawable.ic_huge_refresh),
                                    contentDescription = null,
                                    tint = activeAccentColor,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        if (previewMode == 0) {
                            // Attendance preview items
                            AttendancePreviewRow(
                                code = "CSD301",
                                name = "Machine Learning",
                                stats = "32 / 38 classes",
                                pct = "84.2%",
                                safeBunk = "+4 bunks",
                                isSafe = true,
                                showBunks = bunksEnabled
                            )
                            HorizontalDivider(color = Color(0xFF2C2C2E), thickness = 0.5.dp, modifier = Modifier.padding(vertical = 6.dp))
                            AttendancePreviewRow(
                                code = "CSD302",
                                name = "Distributed Systems",
                                stats = "28 / 40 classes",
                                pct = "70.0%",
                                safeBunk = "Need 2",
                                isSafe = false,
                                showBunks = bunksEnabled
                            )
                        } else {
                            // Schedule preview items
                            if (bannerEnabled) {
                                Surface(
                                    shape = RoundedCornerShape(10.dp),
                                    color = Color(0xFF2C2C2E),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Column(modifier = Modifier.padding(8.dp)) {
                                        Text(
                                            text = "NOW IN PROGRESS (11:00 AM - 12:00 PM)",
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color(0xFF68D391)
                                        )
                                        Text(
                                            text = "Machine Learning · Block VI - 302",
                                            fontFamily = AppleSans,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 12.sp,
                                            color = Color.White
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.height(8.dp))
                            }
                            SchedulePreviewRow(time = "01:00 PM", subject = "Distributed Systems", room = "Room 204")
                            HorizontalDivider(color = Color(0xFF2C2C2E), thickness = 0.5.dp, modifier = Modifier.padding(vertical = 4.dp))
                            SchedulePreviewRow(time = "03:00 PM", subject = "Cloud Computing Lab", room = "Lab 4")
                        }
                    }
                }
            }

            // Theme Options Section
            item {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "BACKGROUND THEME",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextSecondary,
                    letterSpacing = 1.sp
                )
            }

            item {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    themeOptions.forEach { theme ->
                        val isSelected = selectedTheme == theme.id
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .bounceClick {
                                    selectedTheme = theme.id
                                    prefs.setWidgetTheme(theme.id)
                                },
                            shape = RoundedCornerShape(16.dp),
                            color = AppleCardSurface,
                            border = if (isSelected) androidx.compose.foundation.BorderStroke(1.5.dp, activeAccentColor) else null
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(14.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(32.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(theme.cardColor)
                                        .border(1.dp, theme.borderColor, RoundedCornerShape(8.dp))
                                )

                                Spacer(modifier = Modifier.width(12.dp))

                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = theme.title,
                                        fontFamily = AppleSans,
                                        fontWeight = FontWeight.SemiBold,
                                        fontSize = 14.sp,
                                        color = Color.White
                                    )
                                    Text(
                                        text = theme.description,
                                        fontFamily = AppleSans,
                                        fontSize = 11.sp,
                                        color = TextSecondary
                                    )
                                }

                                if (isSelected) {
                                    Icon(
                                        imageVector = Icons.Default.Check,
                                        contentDescription = null,
                                        tint = activeAccentColor,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Accent Color Section
            item {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "ACCENT COLOR",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextSecondary,
                    letterSpacing = 1.sp
                )
            }

            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(AppleCardSurface)
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    accentOptions.forEach { accent ->
                        val isSelected = selectedAccent == accent.id
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .clip(CircleShape)
                                .background(accent.color)
                                .border(
                                    width = if (isSelected) 3.dp else 0.dp,
                                    color = if (isSelected) Color.White else Color.Transparent,
                                    shape = CircleShape
                                )
                                .clickable {
                                    selectedAccent = accent.id
                                    prefs.setWidgetAccent(accent.id)
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            if (isSelected) {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }
                }
            }

            // Display Toggles Section
            item {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "DISPLAY PREFERENCES",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextSecondary,
                    letterSpacing = 1.sp
                )
            }

            item {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    color = AppleCardSurface
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Safe Bunks / Needed Deficit",
                                    fontFamily = AppleSans,
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 14.sp,
                                    color = Color.White
                                )
                                Text(
                                    text = "Show calculated safe bunks badge in attendance widget",
                                    fontFamily = AppleSans,
                                    fontSize = 11.sp,
                                    color = TextSecondary
                                )
                            }
                            Switch(
                                checked = bunksEnabled,
                                onCheckedChange = {
                                    bunksEnabled = it
                                    prefs.setWidgetBunksEnabled(it)
                                },
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = Color.White,
                                    checkedTrackColor = activeAccentColor,
                                    uncheckedThumbColor = Color.LightGray,
                                    uncheckedTrackColor = AppleElevatedSurface
                                )
                            )
                        }

                        HorizontalDivider(
                            color = Color(0xFF2C2C2E),
                            thickness = 0.5.dp,
                            modifier = Modifier.padding(vertical = 12.dp)
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Live Ongoing / Next Class Banner",
                                    fontFamily = AppleSans,
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 14.sp,
                                    color = Color.White
                                )
                                Text(
                                    text = "Show real-time class header in schedule widget",
                                    fontFamily = AppleSans,
                                    fontSize = 11.sp,
                                    color = TextSecondary
                                )
                            }
                            Switch(
                                checked = bannerEnabled,
                                onCheckedChange = {
                                    bannerEnabled = it
                                    prefs.setWidgetBannerEnabled(it)
                                },
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = Color.White,
                                    checkedTrackColor = activeAccentColor,
                                    uncheckedThumbColor = Color.LightGray,
                                    uncheckedTrackColor = AppleElevatedSurface
                                )
                            )
                        }
                    }
                }
            }

            // Instant Apply & Pin Buttons
            item {
                Spacer(modifier = Modifier.height(4.dp))
                Button(
                    onClick = {
                        NsutAttendanceWidgetReceiver.updateAll(context)
                        NsutScheduleWidgetReceiver.updateAll(context)
                        Toast.makeText(context, "✨ Home screen widgets updated!", Toast.LENGTH_SHORT).show()
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                        .bounceClick {
                            NsutAttendanceWidgetReceiver.updateAll(context)
                            NsutScheduleWidgetReceiver.updateAll(context)
                            Toast.makeText(context, "✨ Home screen widgets updated!", Toast.LENGTH_SHORT).show()
                        },
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = activeAccentColor,
                        contentColor = Color.White
                    )
                ) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Apply Changes to Home Screen",
                        fontFamily = AppleSans,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                }
            }

            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = {
                            val pinned = NsutAttendanceWidgetReceiver.requestPinWidget(context)
                            if (!pinned) {
                                Toast.makeText(context, "Long-press your home screen to add the widget", Toast.LENGTH_LONG).show()
                            }
                        },
                        modifier = Modifier
                            .weight(1f)
                            .height(46.dp),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = Color.White
                        )
                    ) {
                        Icon(
                            imageVector = Icons.Default.Widgets,
                            contentDescription = null,
                            tint = activeAccentColor,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "+ Attendance",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }

                    OutlinedButton(
                        onClick = {
                            val pinned = NsutScheduleWidgetReceiver.requestPinWidget(context)
                            if (!pinned) {
                                Toast.makeText(context, "Long-press your home screen to add the widget", Toast.LENGTH_LONG).show()
                            }
                        },
                        modifier = Modifier
                            .weight(1f)
                            .height(46.dp),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = Color.White
                        )
                    ) {
                        Icon(
                            imageVector = Icons.Default.Widgets,
                            contentDescription = null,
                            tint = activeAccentColor,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "+ Schedule",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun AttendancePreviewRow(
    code: String,
    name: String,
    stats: String,
    pct: String,
    safeBunk: String,
    isSafe: Boolean,
    showBunks: Boolean
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = name,
                fontFamily = AppleSans,
                fontWeight = FontWeight.Bold,
                fontSize = 12.sp,
                color = Color.White
            )
            Text(
                text = "$stats • $code",
                fontFamily = AppleSans,
                fontSize = 10.sp,
                color = Color(0xFF8F9CAE)
            )
        }

        if (showBunks) {
            Surface(
                shape = RoundedCornerShape(6.dp),
                color = Color(0xFF2C2C2E),
                modifier = Modifier.padding(end = 6.dp)
            ) {
                Text(
                    text = safeBunk,
                    color = if (isSafe) Color(0xFF30D158) else Color(0xFFFF9F0A),
                    fontSize = 9.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                )
            }
        }

        Surface(
            shape = RoundedCornerShape(6.dp),
            color = if (isSafe) Color(0x3330D158) else Color(0x33FF9F0A),
            modifier = Modifier.width(46.dp)
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.padding(vertical = 2.dp)
            ) {
                Text(
                    text = pct,
                    color = if (isSafe) Color(0xFF30D158) else Color(0xFFFF9F0A),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
private fun SchedulePreviewRow(
    time: String,
    subject: String,
    room: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 3.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Surface(
            shape = RoundedCornerShape(6.dp),
            color = Color(0xFF2C2C2E),
            modifier = Modifier.padding(end = 8.dp)
        ) {
            Text(
                text = time,
                color = Color(0xFF90CDF4),
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
            )
        }

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = subject,
                fontFamily = AppleSans,
                fontWeight = FontWeight.Bold,
                fontSize = 11.sp,
                color = Color.White
            )
            Text(
                text = room,
                fontFamily = AppleSans,
                fontSize = 9.sp,
                color = Color(0xFF8E8E93)
            )
        }
    }
}
