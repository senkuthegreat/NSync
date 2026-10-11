package com.example.nsutconnect.ui.result

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.Calculate
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.nsutconnect.data.local.StudentRecordsProvider
import com.example.nsutconnect.data.model.CourseGrade
import com.example.nsutconnect.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ResultScreen(
    onBack: () -> Unit
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val prefs = remember { com.example.nsutconnect.data.local.UserPreferences(context) }
    val rollNumber = remember { prefs.getRollNumber() }

    var record by remember { mutableStateOf(StudentRecordsProvider.getAcademicRecord(rollNumber)) }
    var selectedSemIndex by remember { mutableIntStateOf(0) }
    var showTargetPlanner by remember { mutableStateOf(false) }
    var targetCgpaInput by remember { mutableFloatStateOf(7.0f) }

    // Silent background sync
    LaunchedEffect(rollNumber) {
        if (rollNumber.isNotBlank()) {
            try {
                val liveRecord = StudentRecordsProvider.getOrFetchAcademicRecord(rollNumber = rollNumber, forceRefresh = false)
                record = liveRecord
            } catch (_: Exception) {
            }
        }
    }

    val activeSemester = record.semesters.getOrNull(selectedSemIndex) ?: record.semesters.firstOrNull()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Results & CGPA",
                        fontSize = 19.sp,
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
                        onClick = { showTargetPlanner = !showTargetPlanner },
                        modifier = Modifier.bounceClick()
                    ) {
                        Icon(
                            imageVector = Icons.Default.Calculate,
                            contentDescription = "CGPA Target Planner",
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
            if (record.semesters.isNotEmpty()) {
                // Hero CGPA Metric Card
                item {
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(20.dp),
                        color = AppleCardSurface
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(
                                    Brush.verticalGradient(
                                        colors = listOf(
                                            AppleAccent.copy(alpha = 0.12f),
                                            Color.Transparent
                                        )
                                    )
                                )
                                .padding(20.dp)
                        ) {
                            Column {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column {
                                        Text(
                                            text = "CUMULATIVE GPA",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = TextSecondary,
                                            letterSpacing = 1.sp
                                        )
                                        Spacer(modifier = Modifier.height(2.dp))
                                        Row(verticalAlignment = Alignment.Bottom) {
                                            Text(
                                                text = String.format("%.2f", record.cumulativeGpa),
                                                fontSize = 36.sp,
                                                fontWeight = FontWeight.Black,
                                                color = Color.White
                                            )
                                            Text(
                                                text = " / 10",
                                                fontSize = 15.sp,
                                                fontWeight = FontWeight.Medium,
                                                color = TextSecondary,
                                                modifier = Modifier.padding(bottom = 5.dp, start = 4.dp)
                                            )
                                        }
                                    }

                                    if (record.rankPercentile.isNotBlank()) {
                                        Surface(
                                            color = AppleAccent.copy(alpha = 0.2f),
                                            shape = RoundedCornerShape(12.dp)
                                        ) {
                                            Row(
                                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Icon(
                                                    imageVector = Icons.AutoMirrored.Filled.TrendingUp,
                                                    contentDescription = null,
                                                    tint = AppleAccent,
                                                    modifier = Modifier.size(13.dp)
                                                )
                                                Spacer(modifier = Modifier.width(4.dp))
                                                Text(
                                                    text = record.rankPercentile,
                                                    fontSize = 11.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = AppleAccent
                                                )
                                            }
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.height(18.dp))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "${record.totalCreditsCompleted} Credits Secured",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = TextSecondary
                                    )
                                    if (record.percentile != null) {
                                        Text(
                                            text = "${String.format("%.1f", record.percentile)}% Percentile",
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = AppleAccent
                                        )
                                    } else if (record.isLiveFromResultHub) {
                                        Text(
                                            text = "ResultHub NSUT",
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = SafeGreen
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // Target CGPA Planner Collapsible
                item {
                    AnimatedVisibility(
                        visible = showTargetPlanner,
                        enter = appleAccordionEnter(),
                        exit = appleAccordionExit()
                    ) {
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(18.dp),
                            color = AppleElevatedSurface
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text(
                                        text = "Target CGPA Planner",
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White
                                    )
                                    Text(
                                        text = String.format("Target: %.2f", targetCgpaInput),
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = AppleAccent
                                    )
                                }
                                Spacer(modifier = Modifier.height(6.dp))

                                Slider(
                                    value = targetCgpaInput,
                                    onValueChange = { targetCgpaInput = it },
                                    valueRange = 6.0f..9.5f,
                                    steps = 35,
                                    colors = SliderDefaults.colors(
                                        thumbColor = AppleAccent,
                                        activeTrackColor = AppleAccent,
                                        inactiveTrackColor = Color.White.copy(alpha = 0.15f)
                                    )
                                )

                                val totalDegreeCredits = 160
                                val remainingCredits = totalDegreeCredits - record.totalCreditsCompleted
                                val neededAvgSgpa = if (remainingCredits > 0) {
                                    ((targetCgpaInput * totalDegreeCredits) - (record.cumulativeGpa * record.totalCreditsCompleted)) / remainingCredits
                                } else 0f

                                Surface(
                                    shape = RoundedCornerShape(10.dp),
                                    color = if (neededAvgSgpa in 0.0f..10.0f) AppleAccent.copy(alpha = 0.15f) else DangerRed.copy(alpha = 0.15f),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Column(modifier = Modifier.padding(12.dp)) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(
                                                imageVector = Icons.Default.CheckCircle,
                                                contentDescription = null,
                                                tint = if (neededAvgSgpa in 0.0f..10.0f) AppleAccent else DangerRed,
                                                modifier = Modifier.size(16.dp)
                                            )
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text(
                                                text = "Required avg SGPA / sem: ${String.format("%.2f", neededAvgSgpa)}",
                                                fontSize = 13.sp,
                                                color = Color.White,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(
                                            text = "Across remaining ${remainingCredits / 20} semesters (${remainingCredits} credits)",
                                            fontSize = 11.sp,
                                            color = TextSecondary
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // Semester Selector Tabs with fluid sliding pill
                item {
                    BoxWithConstraints(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(58.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .background(AppleCardSurface)
                            .padding(3.dp)
                    ) {
                        val totalWidth = maxWidth
                        val count = record.semesters.size.coerceAtLeast(1)
                        val tabWidth = totalWidth / count
                        val animatedIndex by animateFloatAsState(
                            targetValue = selectedSemIndex.toFloat(),
                            animationSpec = spring(
                                dampingRatio = 0.82f,
                                stiffness = Spring.StiffnessMediumLow
                            ),
                            label = "sem_pill_slider"
                        )

                        // Sliding Pill
                        Box(
                            modifier = Modifier
                                .offset(x = tabWidth * animatedIndex)
                                .width(tabWidth)
                                .fillMaxHeight()
                                .clip(RoundedCornerShape(11.dp))
                                .background(AppleAccent)
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            record.semesters.forEachIndexed { index, sem ->
                                val isSelected = index == selectedSemIndex
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape(11.dp))
                                        .bounceClick(scaleDown = 0.94f) { selectedSemIndex = index }
                                        .padding(vertical = 10.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Text(
                                            text = "Sem ${sem.semesterNumber}",
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = if (isSelected) Color.White else TextSecondary
                                        )
                                        Text(
                                            text = "SGPA ${String.format("%.2f", sem.sgpa)}",
                                            fontSize = 11.sp,
                                            color = if (isSelected) Color.White.copy(alpha = 0.9f) else TextSecondary
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // Course Grades List
                if (activeSemester != null) {
                    items(activeSemester.courses, key = { it.code }) { course ->
                        Box(modifier = Modifier.animateItem()) {
                            CourseGradeCard(course = course)
                        }
                    }
                }
            } else {
                item {
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(20.dp),
                        color = AppleCardSurface
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(28.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                imageVector = Icons.Default.Info,
                                contentDescription = null,
                                tint = AppleAccent,
                                modifier = Modifier.size(48.dp)
                            )
                            Spacer(modifier = Modifier.height(14.dp))
                            Text(
                                text = "University Results",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = if (rollNumber.isNotBlank())
                                    "No published results found for $rollNumber on ResultHub yet.\nResults appear automatically once official university gazettes are released."
                                else
                                    "Please sign in with your NSUT roll number to view semester results.",
                                fontSize = 13.sp,
                                color = TextSecondary,
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                                lineHeight = 18.sp
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CourseGradeCard(course: CourseGrade) {
    val color = gradeColor(course.grade)

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        color = AppleCardSurface
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = AppleElevatedSurface
                    ) {
                        Text(
                            text = course.code,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = AppleAccent,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (course.credits == 0) "Audit (0 Credits)" else "${course.credits} Credits",
                        fontSize = 11.sp,
                        color = TextSecondary
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = course.title,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color.White
                )
            }

            Column(horizontalAlignment = Alignment.End) {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = color.copy(alpha = 0.18f)
                ) {
                    Text(
                        text = course.grade,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Black,
                        color = color,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                    )
                }
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = if (course.credits == 0) "Audit" else "${course.gradePoints} Pts",
                    fontSize = 10.sp,
                    color = TextSecondary
                )
            }
        }
    }
}

private fun gradeColor(grade: String): Color {
    return when (grade) {
        "O" -> Color(0xFFA855F7)
        "A+" -> AppleAccent
        "A" -> Color(0xFF06B6D4)
        "B+" -> Color(0xFF10B981)
        "B" -> Color(0xFF3B82F6)
        "C" -> Color(0xFFF59E0B)
        "D" -> Color(0xFFF97316)
        "F" -> DangerRed
        else -> TextSecondary
    }
}
