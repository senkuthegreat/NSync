package com.example.nsutconnect.widget

import android.content.Context
import android.content.Intent
import android.view.View
import android.widget.RemoteViews
import android.widget.RemoteViewsService
import com.example.nsutconnect.R
import com.example.nsutconnect.data.local.UserPreferences
import com.example.nsutconnect.data.model.ClassScheduleItem
import com.example.nsutconnect.data.model.ClassType
import java.time.LocalDate

class NsutScheduleWidgetService : RemoteViewsService() {
    override fun onGetViewFactory(intent: Intent): RemoteViewsFactory {
        return ScheduleRemoteViewsFactory(applicationContext)
    }
}

class ScheduleRemoteViewsFactory(private val context: Context) : RemoteViewsService.RemoteViewsFactory {

    private var classes: List<ClassScheduleItem> = emptyList()

    private var isDisplayingToday: Boolean = true

    override fun onCreate() {
        loadData()
    }

    override fun onDataSetChanged() {
        loadData()
    }

    private fun loadData() {
        val prefs = UserPreferences(context)
        val timetable = prefs.getCachedTimetable() ?: return
        val today = java.time.LocalDate.now().dayOfWeek
        val now = java.time.LocalTime.now()

        val isWeekend = today == java.time.DayOfWeek.SATURDAY || today == java.time.DayOfWeek.SUNDAY
        val isTodayCompleted = timetable.isDayCompleted(today, now)

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

        isDisplayingToday = isToday
        val dayClasses = timetable.getScheduleFor(activeDay).sortedBy { it.startTime }
        classes = dayClasses
    }

    override fun onDestroy() {
        classes = emptyList()
    }

    override fun getCount(): Int = classes.size

    override fun getViewAt(position: Int): RemoteViews {
        val views = RemoteViews(context.packageName, R.layout.widget_schedule_item)
        if (position >= classes.size) return views

        val item = classes[position]
        val now = java.time.LocalTime.now()

        val isOngoing = isDisplayingToday && item.isOngoingAt(now)
        val isFinished = isDisplayingToday && item.isFinishedAt(now)

        // No background block (borderless rows with dividers like attendance)
        views.setInt(R.id.widget_schedule_item_root, "setBackgroundResource", 0)

        // Title & Time (12-hour formatted: 2:00 PM – 4:00 PM, without room number)
        views.setTextViewText(R.id.tv_schedule_item_name, item.subjectName)
        views.setTextColor(R.id.tv_schedule_item_name, if (isFinished) 0xFF8E8E93.toInt() else 0xFFFFFFFF.toInt())

        val timeDisplay = "${item.startTimeFormatted} – ${item.endTimeFormatted}"
        views.setTextViewText(R.id.tv_schedule_item_time, timeDisplay)
        views.setTextColor(R.id.tv_schedule_item_time, if (isOngoing) 0xFF6366F1.toInt() else 0xFF8E8E93.toInt())

        // Status tag (now / done without bg)
        when {
            isOngoing -> {
                views.setViewVisibility(R.id.tv_schedule_item_status, View.VISIBLE)
                views.setTextViewText(R.id.tv_schedule_item_status, "now")
                views.setInt(R.id.tv_schedule_item_status, "setBackgroundResource", 0)
                views.setTextColor(R.id.tv_schedule_item_status, 0xFF22C55E.toInt())
            }
            isFinished -> {
                views.setViewVisibility(R.id.tv_schedule_item_status, View.VISIBLE)
                views.setTextViewText(R.id.tv_schedule_item_status, "done")
                views.setInt(R.id.tv_schedule_item_status, "setBackgroundResource", 0)
                views.setTextColor(R.id.tv_schedule_item_status, 0xFF71717A.toInt())
            }
            else -> {
                views.setViewVisibility(R.id.tv_schedule_item_status, View.GONE)
            }
        }

        // Type tag (LEC / LAB / TUT)
        views.setTextViewText(R.id.tv_schedule_item_type, item.classType.shortName)
        when (item.classType) {
            ClassType.LECTURE -> {
                views.setInt(R.id.tv_schedule_item_type, "setBackgroundResource", R.drawable.widget_badge_lecture_bg)
                views.setTextColor(R.id.tv_schedule_item_type, 0xFF6366F1.toInt())
            }
            ClassType.LAB -> {
                views.setInt(R.id.tv_schedule_item_type, "setBackgroundResource", R.drawable.widget_badge_lab_bg)
                views.setTextColor(R.id.tv_schedule_item_type, 0xFF22C55E.toInt())
            }
            ClassType.TUTORIAL -> {
                views.setInt(R.id.tv_schedule_item_type, "setBackgroundResource", R.drawable.widget_badge_tutorial_bg)
                views.setTextColor(R.id.tv_schedule_item_type, 0xFFF59E0B.toInt())
            }
        }

        val fillInIntent = Intent().apply {
            putExtra("destination_tab", "schedule")
        }
        views.setOnClickFillInIntent(R.id.widget_schedule_item_root, fillInIntent)

        return views
    }

    override fun getLoadingView(): RemoteViews? = null

    override fun getViewTypeCount(): Int = 1

    override fun getItemId(position: Int): Long = position.toLong()

    override fun hasStableIds(): Boolean = true
}
