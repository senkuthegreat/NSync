package com.example.nsutconnect.ui.notices

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.nsutconnect.data.local.StudentRecordsProvider
import com.example.nsutconnect.data.model.NoticeItem
import com.example.nsutconnect.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NoticesScreen(
    onBack: () -> Unit,
    onNavigateToCalendar: (() -> Unit)? = null
) {
    val context = LocalContext.current
    val allNotices = remember { StudentRecordsProvider.getPersonalizedNotices() }
    var onlyPersonalized by remember { mutableStateOf(true) }
    var selectedCategory by remember { mutableStateOf<String?>(null) }

    val displayedNotices = remember(allNotices, onlyPersonalized, selectedCategory) {
        allNotices.filter { notice ->
            val matchPersonalized = !onlyPersonalized || notice.isRelevant
            val matchCategory = selectedCategory == null || notice.category.equals(selectedCategory, ignoreCase = true)
            matchPersonalized && matchCategory
        }
    }

    val categories = remember(allNotices) {
        allNotices.map { it.category }.distinct()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Notices & Circulars",
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
            // Anti-Spam Personalization Filter Header
            item {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(18.dp),
                    color = AppleCardSurface
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                Brush.verticalGradient(
                                    listOf(
                                        AppleAccent.copy(alpha = 0.12f),
                                        Color.Transparent
                                    )
                                )
                            )
                            .padding(16.dp)
                    ) {
                        Column {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.Security,
                                        contentDescription = null,
                                        tint = SafeGreen,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "Spam Shield Active",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = SafeGreen
                                    )
                                }

                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.clickable { onlyPersonalized = !onlyPersonalized }
                                ) {
                                    Text(
                                        text = if (onlyPersonalized) "Personalized Only" else "All Notices",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = Color.White
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Switch(
                                        checked = onlyPersonalized,
                                        onCheckedChange = { onlyPersonalized = it },
                                        colors = SwitchDefaults.colors(
                                            checkedThumbColor = Color.White,
                                            checkedTrackColor = AppleAccent,
                                            uncheckedTrackColor = AppleElevatedSurface
                                        ),
                                        modifier = Modifier.height(24.dp)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "Filtered strictly for B.Tech ECE/VLSI (Batch 2024-2028). Unrelated tenders, PhD defenses, and third-party notices are automatically hidden.",
                                fontSize = 11.sp,
                                color = TextSecondary,
                                lineHeight = 15.sp
                            )
                        }
                    }
                }
            }

            // Categories Filter Pills
            item {
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = PaddingValues(horizontal = 2.dp)
                ) {
                    item {
                        CategoryPill(
                            label = "All (${allNotices.size})",
                            isSelected = selectedCategory == null,
                            onClick = { selectedCategory = null }
                        )
                    }
                    items(categories) { cat ->
                        CategoryPill(
                            label = cat,
                            isSelected = selectedCategory == cat,
                            onClick = { selectedCategory = cat }
                        )
                    }
                }
            }

            // Notices List with AI Summaries
            items(displayedNotices, key = { it.id }) { notice ->
                NoticeCard(
                    notice = notice,
                    onOpenInCalendar = if (notice.id == "NOT-2026-655") onNavigateToCalendar else null,
                    onOpenNotice = {
                        try {
                            if (notice.id == "NOT-2026-655") {
                                com.example.nsutconnect.data.calendar.CloudCalendarRepository(context).openPdfInExternalViewer()
                            } else {
                                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(notice.downloadUrl))
                                context.startActivity(intent)
                            }
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                    }
                )
            }
        }
    }
}

@Composable
private fun CategoryPill(
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .bounceClick()
            .clickable { onClick() },
        shape = RoundedCornerShape(10.dp),
        color = if (isSelected) AppleAccent else AppleCardSurface
    ) {
        Text(
            text = label,
            fontSize = 12.sp,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
            color = if (isSelected) Color.White else TextSecondary,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp)
        )
    }
}

@Composable
private fun NoticeCard(
    notice: NoticeItem,
    onOpenInCalendar: (() -> Unit)? = null,
    onOpenNotice: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = AppleCardSurface
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Header Row: Category + Date
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = AppleElevatedSurface
                    ) {
                        Text(
                            text = notice.category,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = AppleAccent,
                            modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp)
                        )
                    }

                    if (notice.isUrgent) {
                        Spacer(modifier = Modifier.width(6.dp))
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = DangerRed.copy(alpha = 0.2f)
                        ) {
                            Text(
                                text = "IMPORTANT",
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                color = DangerRed,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                }

                Text(
                    text = notice.date,
                    fontSize = 11.sp,
                    color = TextSecondary
                )
            }

            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = notice.title,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )

            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Target: ${notice.targetDepartment}",
                fontSize = 11.sp,
                color = TextSecondary
            )

            Spacer(modifier = Modifier.height(12.dp))

            // AI Key Takeaway Banner
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = AppleAccent.copy(alpha = 0.12f),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.Top
                ) {
                    Icon(
                        imageVector = Icons.Default.AutoAwesome,
                        contentDescription = null,
                        tint = AppleAccent,
                        modifier = Modifier
                            .size(16.dp)
                            .padding(top = 1.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text(
                            text = "AI Key Takeaway",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = AppleAccent
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = notice.aiSummary,
                            fontSize = 12.sp,
                            color = Color.White,
                            lineHeight = 16.sp
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Actions
            if (onOpenInCalendar != null) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        modifier = Modifier
                            .bounceClick()
                            .clickable { onOpenInCalendar() },
                        shape = RoundedCornerShape(10.dp),
                        color = AppleAccent.copy(alpha = 0.2f),
                        border = androidx.compose.foundation.BorderStroke(1.dp, AppleAccent.copy(alpha = 0.4f))
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.CalendarMonth,
                                contentDescription = null,
                                tint = AppleAccent,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "View in Calendar",
                                fontSize = 11.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = AppleAccent
                            )
                        }
                    }

                    Row(
                        modifier = Modifier
                            .bounceClick()
                            .clickable { onOpenNotice() }
                            .padding(horizontal = 6.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Open Official PDF",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium,
                            color = TextSecondary
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.OpenInNew,
                            contentDescription = null,
                            tint = TextSecondary,
                            modifier = Modifier.size(12.dp)
                        )
                    }
                }
            } else {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onOpenNotice() },
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Official Circular",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = AppleAccent
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.OpenInNew,
                        contentDescription = null,
                        tint = AppleAccent,
                        modifier = Modifier.size(13.dp)
                    )
                }
            }
        }
    }
}
