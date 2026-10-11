package com.example.nsutconnect.ui.main

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.Crossfade
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.nsutconnect.R
import com.example.nsutconnect.data.network.CaptchaSession
import com.example.nsutconnect.data.local.UserPreferences
import com.example.nsutconnect.data.sync.NsutSyncRepository
import com.example.nsutconnect.data.sync.NsutSyncWorker
import com.example.nsutconnect.data.sync.SyncResult
import com.example.nsutconnect.theme.*
import com.example.nsutconnect.ui.account.AccountScreen
import com.example.nsutconnect.ui.attendance.AttendanceScreen
import com.example.nsutconnect.ui.more.MoreScreen
import com.example.nsutconnect.ui.schedule.ScheduleScreen
import kotlinx.coroutines.launch

enum class MainTab {
    ATTENDANCE,
    SCHEDULE,
    MORE
}

@Composable
fun MainScreen(
    initialTab: String = "attendance",
    onLogout: () -> Unit = {}
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val repository = remember { NsutSyncRepository(context) }
    val prefs = remember { UserPreferences(context) }

    val startTab = when (initialTab) {
        "schedule" -> MainTab.SCHEDULE
        "more", "account" -> MainTab.MORE
        else -> MainTab.ATTENDANCE
    }
    var currentTab by remember { mutableStateOf(startTab) }

    val attendance by repository.attendance.collectAsStateWithLifecycle()
    val timetable by repository.timetable.collectAsStateWithLifecycle()
    val syncState by repository.syncState.collectAsStateWithLifecycle()

    val isLoading = syncState is SyncResult.Loading

    // Automatically sync on open and schedule periodic background worker
    LaunchedEffect(Unit) {
        NsutSyncWorker.schedulePeriodicSync(context, 4)
        repository.syncData(isBackground = true)
    }

    Scaffold(
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        bottomBar = {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = AppleCardSurface,
                tonalElevation = 0.dp
            ) {
                BoxWithConstraints(
                    modifier = Modifier
                        .fillMaxWidth()
                        .windowInsetsPadding(WindowInsets.navigationBars)
                        .height(64.dp)
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    val totalWidth = maxWidth
                    val tabWidth = totalWidth / 3
                    val animatedIndex by animateFloatAsState(
                        targetValue = currentTab.ordinal.toFloat(),
                        animationSpec = spring(
                            dampingRatio = 0.82f,
                            stiffness = Spring.StiffnessMediumLow
                        ),
                        label = "tab_pill_slider"
                    )

                    // Apple Signature Sliding Pill Indicator
                    Box(
                        modifier = Modifier
                            .offset(x = tabWidth * animatedIndex)
                            .width(tabWidth)
                            .fillMaxHeight()
                            .clip(RoundedCornerShape(12.dp))
                            .background(AppleElevatedSurface)
                    )

                    Row(
                        modifier = Modifier.fillMaxSize(),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        MainTabItem(
                            selected = currentTab == MainTab.ATTENDANCE,
                            iconRes = R.drawable.ic_huge_attendance,
                            label = "Attendance",
                            onClick = { currentTab = MainTab.ATTENDANCE }
                        )
                        MainTabItem(
                            selected = currentTab == MainTab.SCHEDULE,
                            iconRes = R.drawable.ic_huge_schedule,
                            label = "Schedule",
                            onClick = { currentTab = MainTab.SCHEDULE }
                        )
                        MainTabItem(
                            selected = currentTab == MainTab.MORE,
                            iconRes = R.drawable.ic_huge_more,
                            label = "More",
                            onClick = { currentTab = MainTab.MORE }
                        )
                    }
                }
            }
        },
        containerColor = AppleBackground
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(bottom = padding.calculateBottomPadding())
        ) {
            AnimatedContent(
                targetState = currentTab,
                transitionSpec = {
                    appleSlideHorizontally(targetState.ordinal > initialState.ordinal)
                },
                label = "main_screen_tab_slide"
            ) { tab ->
                when (tab) {
                    MainTab.ATTENDANCE -> {
                        AttendanceScreen(
                            attendance = attendance,
                            isLoading = isLoading,
                            onRefresh = {
                                coroutineScope.launch { repository.syncData() }
                            }
                        )
                    }
                    MainTab.SCHEDULE -> {
                        ScheduleScreen(
                            timetable = timetable,
                            isLoading = isLoading,
                            onRefresh = {
                                coroutineScope.launch { repository.syncData() }
                            }
                        )
                    }
                    MainTab.MORE -> {
                        MoreScreen(
                            prefs = prefs,
                            syncState = syncState,
                            onForceSync = {
                                coroutineScope.launch {
                                    repository.syncData()
                                    repository.updateWidgets()
                                }
                            },
                            onLogout = { forget ->
                                repository.logout(forgetCredentials = forget)
                                onLogout()
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun RowScope.MainTabItem(
    selected: Boolean,
    iconRes: Int,
    label: String,
    onClick: () -> Unit
) {
    val contentColor by animateColorAsState(
        targetValue = if (selected) AppleSystemBlue else AppleSecondaryLabel,
        animationSpec = tween(durationMillis = 180),
        label = "tab_content_color"
    )

    Box(
        modifier = Modifier
            .weight(1f)
            .fillMaxHeight()
            .bounceClick(scaleDown = 0.90f) { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                painter = painterResource(id = iconRes),
                contentDescription = label,
                tint = contentColor,
                modifier = Modifier
                    .size(20.dp)
                    .appleTabPop(selected = selected)
            )
            Spacer(modifier = Modifier.height(3.dp))
            Text(
                text = label,
                fontFamily = AppleSans,
                fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
                fontSize = 11.sp,
                color = contentColor
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SessionExpiredDialog(
    rollNumber: String,
    captchaSession: CaptchaSession?,
    isCaptchaLoading: Boolean,
    isSubmitting: Boolean,
    errorMessage: String?,
    onRefreshCaptcha: () -> Unit,
    onSubmit: (String) -> Unit,
    onDismiss: () -> Unit,
    onLogout: () -> Unit
) {
    var captchaInput by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = AppleCardSurface,
        shape = RoundedCornerShape(24.dp),
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Surface(
                    shape = CircleShape,
                    color = AppleSystemBlue.copy(alpha = 0.15f),
                    modifier = Modifier.size(36.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_huge_lock),
                            contentDescription = null,
                            tint = AppleSystemBlue,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
                Text(
                    text = "Resume IMS Session",
                    fontFamily = AppleSans,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    color = AppleLabel
                )
            }
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "Your IMS session timed out. Enter the security code below to resume live sync as $rollNumber without retyping your password.",
                    fontFamily = AppleSans,
                    fontSize = 13.sp,
                    color = AppleSecondaryLabel,
                    lineHeight = 18.sp
                )

                if (errorMessage != null) {
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = AppleSystemRedBg,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = errorMessage,
                            fontFamily = AppleSans,
                            fontSize = 12.sp,
                            color = AppleSystemRed,
                            modifier = Modifier.padding(10.dp)
                        )
                    }
                }

                // Captcha Image Preview
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Surface(
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp),
                        shape = RoundedCornerShape(10.dp),
                        color = Color(0xFFF1F3F9)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            if (isCaptchaLoading) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(20.dp),
                                    strokeWidth = 2.dp,
                                    color = AppleSystemBlue
                                )
                            } else if (captchaSession?.captchaBitmap != null) {
                                Image(
                                    bitmap = captchaSession.captchaBitmap.asImageBitmap(),
                                    contentDescription = "Captcha Code",
                                    modifier = Modifier.fillMaxSize(),
                                    contentScale = ContentScale.Fit
                                )
                            } else {
                                Text(
                                    text = "Tap refresh",
                                    fontFamily = AppleSans,
                                    fontSize = 11.sp,
                                    color = Color.DarkGray
                                )
                            }
                        }
                    }

                    IconButton(
                        onClick = onRefreshCaptcha,
                        modifier = Modifier
                            .size(48.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(AppleElevatedSurface)
                    ) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_huge_refresh),
                            contentDescription = "Refresh Captcha",
                            tint = AppleSystemBlue,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }

                // Captcha Input Field
                TextField(
                    value = captchaInput,
                    onValueChange = {
                        captchaInput = it.filter { char -> char.isLetterOrDigit() }.take(6).uppercase()
                    },
                    placeholder = {
                        Text(
                            text = "Enter code shown above",
                            fontFamily = AppleSans,
                            fontSize = 13.sp,
                            color = AppleSecondaryLabel.copy(alpha = 0.6f)
                        )
                    },
                    singleLine = true,
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = AppleElevatedSurface,
                        unfocusedContainerColor = AppleElevatedSurface,
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent,
                        focusedTextColor = AppleLabel,
                        unfocusedTextColor = AppleLabel
                    ),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onSubmit(captchaInput) },
                colors = ButtonDefaults.buttonColors(containerColor = AppleSystemBlue),
                shape = RoundedCornerShape(10.dp),
                enabled = !isSubmitting && captchaInput.isNotBlank()
            ) {
                if (isSubmitting) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        strokeWidth = 2.dp,
                        color = Color.White
                    )
                } else {
                    Text(
                        text = "Resume",
                        fontFamily = AppleSans,
                        fontWeight = FontWeight.SemiBold,
                        color = Color.White
                    )
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(
                    text = "Offline Mode",
                    fontFamily = AppleSans,
                    color = AppleSecondaryLabel
                )
            }
        }
    )
}
