package com.example.nsutconnect.widget

import android.content.Context
import android.content.Intent
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
        val isTodayCompleted = timetable.isDayCompleted(today, now)
        val targetDay = timetable.getAutoDefaultDay(today, now)
        val isShowingTomorrow = isTodayCompleted || (today != targetDay && today != java.time.DayOfWeek.SATURDAY && today != java.time.DayOfWeek.SUNDAY)
        val activeDay = if (isShowingTomorrow) targetDay else today

        val dayClasses = timetable.getScheduleFor(activeDay)
        classes = if (isShowingTomorrow) {
            dayClasses
        } else {
            // Focus on current ongoing and upcoming classes of today
            val currentAndUpcoming = dayClasses.filter { it.isOngoingAt(now) || it.isUpcomingAt(now) }
            if (currentAndUpcoming.isNotEmpty()) currentAndUpcoming else dayClasses
        }
    }

    override fun onDestroy() {
        classes = emptyList()
    }

    override fun getCount(): Int = classes.size

    override fun getViewAt(position: Int): RemoteViews {
        val views = RemoteViews(context.packageName, R.layout.widget_schedule_item)
        if (position >= classes.size) return views

        val item = classes[position]
        views.setTextViewText(R.id.tv_schedule_item_time, "${item.startTime} - ${item.endTime}")
        views.setTextViewText(R.id.tv_schedule_item_name, item.subjectName)
        views.setTextViewText(
            R.id.tv_schedule_item_location,
            "${item.roomNumber}${if (item.facultyName.isNotBlank()) " • " + item.facultyName else ""}"
        )

        when (item.classType) {
            ClassType.LECTURE -> {
                views.setTextViewText(R.id.tv_schedule_item_type, "LEC")
                views.setInt(R.id.tv_schedule_item_type, "setBackgroundResource", R.drawable.widget_pill_accent)
                views.setTextColor(R.id.tv_schedule_item_type, 0xFF6366F1.toInt())
            }
            ClassType.LAB -> {
                views.setTextViewText(R.id.tv_schedule_item_type, "LAB")
                views.setInt(R.id.tv_schedule_item_type, "setBackgroundResource", R.drawable.widget_badge_safe)
                views.setTextColor(R.id.tv_schedule_item_type, 0xFF2ECC71.toInt())
            }
            ClassType.TUTORIAL -> {
                views.setTextViewText(R.id.tv_schedule_item_type, "TUT")
                views.setInt(R.id.tv_schedule_item_type, "setBackgroundResource", R.drawable.widget_badge_warning)
                views.setTextColor(R.id.tv_schedule_item_type, 0xFFF39C12.toInt())
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
