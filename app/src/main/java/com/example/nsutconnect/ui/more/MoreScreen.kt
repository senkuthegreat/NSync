package com.example.nsutconnect.ui.more

import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForwardIos
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
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
import com.example.nsutconnect.data.sync.SyncResult
import com.example.nsutconnect.theme.*
import com.example.nsutconnect.ui.calendar.AcademicCalendarScreen
import com.example.nsutconnect.ui.exam.ExamScreen
import com.example.nsutconnect.ui.result.ResultScreen
import kotlinx.coroutines.launch

enum class MoreSubDestination {
    HUB,
    RESULTS,
    CALENDAR,
    EXAMS,
    WIDGET_STUDIO
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MoreScreen(
    prefs: UserPreferences,
    syncState: SyncResult,
    onForceSync: () -> Unit = {},
    onLogout: (forgetCredentials: Boolean) -> Unit = {},
    onSubDestinationChanged: (Boolean) -> Unit = {}
) {
    var currentSubDestination by remember { mutableStateOf(MoreSubDestination.HUB) }

    LaunchedEffect(currentSubDestination) {
        onSubDestinationChanged(currentSubDestination != MoreSubDestination.HUB)
    }

    BackHandler(enabled = currentSubDestination != MoreSubDestination.HUB) {
        currentSubDestination = MoreSubDestination.HUB
    }

    AnimatedContent(
        targetState = currentSubDestination,
        transitionSpec = {
            applePushPopTransition(isPush = targetState != MoreSubDestination.HUB)
        },
        label = "more_sub_destination_anim"
    ) { destination ->
        when (destination) {
            MoreSubDestination.RESULTS -> {
                ResultScreen(onBack = { currentSubDestination = MoreSubDestination.HUB })
            }
            MoreSubDestination.CALENDAR -> {
                AcademicCalendarScreen(onBack = { currentSubDestination = MoreSubDestination.HUB })
            }
            MoreSubDestination.EXAMS -> {
                ExamScreen(onBack = { currentSubDestination = MoreSubDestination.HUB })
            }
            MoreSubDestination.WIDGET_STUDIO -> {
                WidgetStudioScreen(
                    prefs = prefs,
                    onBack = { currentSubDestination = MoreSubDestination.HUB }
                )
            }
            MoreSubDestination.HUB -> {
                MoreHubContent(
                    prefs = prefs,
                    syncState = syncState,
                    onNavigate = { currentSubDestination = it },
                    onLogout = onLogout
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MoreHubContent(
    prefs: UserPreferences,
    syncState: SyncResult,
    onNavigate: (MoreSubDestination) -> Unit,
    onLogout: (forgetCredentials: Boolean) -> Unit
) {
    var showLogoutDialog by remember { mutableStateOf(false) }
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    var isCheckingUpdates by remember { mutableStateOf(false) }
    var updateInfo by remember { mutableStateOf<com.example.nsutconnect.data.update.AppUpdateInfo?>(null) }
    val rollNumber = remember(syncState) { prefs.getRollNumber().ifBlank { "" } }
    val studentName = remember(syncState) { prefs.getStudentName().ifBlank { "Student" } }
    val branch = remember(syncState) { prefs.getBranch().ifBlank { "Engineering" } }

    val geometricStyles = remember {
        listOf("shapes", "squircles", "rings", "shape-grid", "waves", "patchwork", "slice", "triangles", "stripes", "weave", "stack")
    }
    val studentSeed = remember(rollNumber) { rollNumber.trim().ifBlank { "Student" } }
    val selectedStyle = remember(studentSeed) {
        val hash = kotlin.math.abs(studentSeed.hashCode())
        geometricStyles[hash % geometricStyles.size]
    }
    val avatarUrl = remember(studentSeed, selectedStyle) {
        "https://api.dicebear.com/10.x/$selectedStyle/png?seed=$studentSeed"
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "More",
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
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            contentPadding = PaddingValues(top = 4.dp, bottom = 40.dp)
        ) {
            // Profile Card with DiceBear cartoon PFP
            item {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    color = AppleCardSurface
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(56.dp)
                                .clip(CircleShape)
                                .background(AppleElevatedSurface),
                            contentAlignment = Alignment.Center
                        ) {
                            AsyncImage(
                                model = ImageRequest.Builder(LocalContext.current)
                                    .data(avatarUrl)
                                    .crossfade(true)
                                    .build(),
                                contentDescription = "Student Avatar",
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop
                            )
                        }

                        Spacer(modifier = Modifier.width(14.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = studentName,
                                fontFamily = AppleSans,
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp,
                                color = AppleLabel,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = rollNumber,
                                fontFamily = AppleSans,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 13.sp,
                                color = AppleAccent
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = branch,
                                fontFamily = AppleSans,
                                fontSize = 11.sp,
                                color = TextSecondary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }

            // Academic Hub Section Header
            item {
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "ACADEMIC & PERFORMANCE",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextSecondary,
                    letterSpacing = 1.sp
                )
            }

            // Feature 1: Check Results & CGPA
            item {
                HubFeatureCard(
                    title = "Results & CGPA",
                    icon = Icons.Default.Calculate,
                    onClick = { onNavigate(MoreSubDestination.RESULTS) }
                )
            }

            // Feature 2: Academic Calendar (Gemini AI Live Agent)
            item {
                HubFeatureCard(
                    title = "Academic Calendar",
                    icon = Icons.Default.CalendarMonth,
                    onClick = { onNavigate(MoreSubDestination.CALENDAR) }
                )
            }

            // Feature 3: Exam Datesheet & Seating Plan
            item {
                HubFeatureCard(
                    title = "Exam Datesheet & Seating Plan",
                    icon = Icons.Default.Badge,
                    onClick = { onNavigate(MoreSubDestination.EXAMS) }
                )
            }

            // Customization Section Header
            item {
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "HOME SCREEN & CUSTOMIZATION",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextSecondary,
                    letterSpacing = 1.sp
                )
            }

            // Feature 4: Widget Studio & Customization
            item {
                HubFeatureCard(
                    title = "Widget Studio",
                    icon = Icons.Default.Widgets,
                    onClick = { onNavigate(MoreSubDestination.WIDGET_STUDIO) }
                )
            }

            // System & Account Section
            item {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "SYSTEM & ACCOUNT",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextSecondary,
                    letterSpacing = 1.sp
                )
            }

            // Grouped Card: Check for Updates & Log Out
            item {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    color = AppleCardSurface
                ) {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        // Check for Updates Row
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .bounceClick {
                                    if (!isCheckingUpdates) {
                                        isCheckingUpdates = true
                                        coroutineScope.launch {
                                            val result = com.example.nsutconnect.data.update.AppUpdateManager.checkForUpdates(context)
                                            isCheckingUpdates = false
                                            if (result.hasUpdate) {
                                                updateInfo = result
                                            } else {
                                                Toast.makeText(
                                                    context,
                                                    "NSync is up to date (v${com.example.nsutconnect.BuildConfig.VERSION_NAME})",
                                                    Toast.LENGTH_SHORT
                                                ).show()
                                            }
                                        }
                                    }
                                }
                                .padding(horizontal = 16.dp, vertical = 14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(AppleSystemBlue.copy(alpha = 0.12f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.SystemUpdate,
                                    contentDescription = null,
                                    tint = AppleSystemBlue,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(14.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Check for Updates",
                                    fontFamily = AppleSans,
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 14.sp,
                                    color = AppleLabel
                                )
                                Text(
                                    text = if (isCheckingUpdates) "Checking for updates..." else "Version v${com.example.nsutconnect.BuildConfig.VERSION_NAME} • Up to date",
                                    fontFamily = AppleSans,
                                    fontSize = 11.sp,
                                    color = TextSecondary
                                )
                            }
                            if (isCheckingUpdates) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(16.dp),
                                    strokeWidth = 2.dp,
                                    color = AppleSystemBlue
                                )
                            } else {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.ArrowForwardIos,
                                    contentDescription = null,
                                    tint = TextSecondary.copy(alpha = 0.4f),
                                    modifier = Modifier.size(13.dp)
                                )
                            }
                        }

                        HorizontalDivider(
                            color = AppleBorder,
                            thickness = 0.5.dp,
                            modifier = Modifier.padding(start = 54.dp, end = 16.dp)
                        )

                        // Log Out Row
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .bounceClick { showLogoutDialog = true }
                                .padding(horizontal = 16.dp, vertical = 14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(AppleSystemRed.copy(alpha = 0.12f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    painter = painterResource(id = R.drawable.ic_huge_logout),
                                    contentDescription = null,
                                    tint = AppleSystemRed,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(14.dp))
                            Text(
                                text = "Log Out of NSUT IMS",
                                fontFamily = AppleSans,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 14.sp,
                                color = AppleSystemRed,
                                modifier = Modifier.weight(1f)
                            )
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowForwardIos,
                                contentDescription = null,
                                tint = AppleSystemRed.copy(alpha = 0.4f),
                                modifier = Modifier.size(13.dp)
                            )
                        }
                    }
                }
            }

            if (showLogoutDialog) {
                item {
                    AlertDialog(
                        onDismissRequest = { showLogoutDialog = false },
                        containerColor = AppleCardSurface,
                        shape = RoundedCornerShape(20.dp),
                        title = {
                            Text(
                                text = "Sign Out Options",
                                fontFamily = AppleSans,
                                fontWeight = FontWeight.Bold,
                                fontSize = 18.sp,
                                color = AppleLabel
                            )
                        },
                        text = {
                            Text(
                                text = "You can choose to keep your credentials remembered on this device so you only need to enter the security captcha next time, or wipe your account completely.",
                                fontFamily = AppleSans,
                                fontSize = 13.sp,
                                color = AppleSecondaryLabel,
                                lineHeight = 18.sp
                            )
                        },
                        confirmButton = {
                            Button(
                                onClick = {
                                    showLogoutDialog = false
                                    onLogout(false) // Keep credentials saved
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = AppleSystemBlue),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Text("Keep Saved Info", color = Color.White)
                            }
                        },
                        dismissButton = {
                            TextButton(
                                onClick = {
                                    showLogoutDialog = false
                                    onLogout(true) // Forget credentials
                                }
                            ) {
                                Text("Forget Account", color = AppleSystemRed)
                            }
                        }
                    )
                }
            }

            // Minimal Uncluttered Footer
            item {
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 12.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.ic_nsync_logo),
                        contentDescription = "NSync Logo",
                        modifier = Modifier
                            .size(16.dp)
                            .clip(RoundedCornerShape(4.dp))
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "NSync",
                        fontFamily = AppleSans,
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp,
                        color = AppleLabel
                    )
                    Text(
                        text = " • Crafted for NSUT",
                        fontFamily = AppleSans,
                        fontSize = 12.sp,
                        color = TextSecondary.copy(alpha = 0.6f)
                    )
                }
            }
        }

        updateInfo?.let { info ->
            com.example.nsutconnect.ui.update.UpdateDialog(
                updateInfo = info,
                onDismiss = { updateInfo = null }
            )
        }
    }
}

@Composable
private fun HubFeatureCard(
    title: String,
    icon: ImageVector,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .bounceClick { onClick() },
        shape = RoundedCornerShape(16.dp),
        color = AppleCardSurface
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 15.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = AppleElevatedSurface,
                modifier = Modifier.size(44.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = AppleAccent,
                        modifier = Modifier.size(22.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.width(16.dp))

            Text(
                text = title,
                fontFamily = AppleSans,
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color.White,
                modifier = Modifier.weight(1f)
            )

            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowForwardIos,
                contentDescription = null,
                tint = TextSecondary,
                modifier = Modifier.size(13.dp)
            )
        }
    }
}

