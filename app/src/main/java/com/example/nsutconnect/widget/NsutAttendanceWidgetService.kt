package com.example.nsutconnect.widget

import android.content.Context
import android.content.Intent
import android.view.View
import android.widget.RemoteViews
import android.widget.RemoteViewsService
import com.example.nsutconnect.R
import com.example.nsutconnect.data.local.UserPreferences
import com.example.nsutconnect.data.model.AttendanceRecord
import java.util.Locale

class NsutAttendanceWidgetService : RemoteViewsService() {
    override fun onGetViewFactory(intent: Intent): RemoteViewsFactory {
        return AttendanceRemoteViewsFactory(applicationContext)
    }
}

class AttendanceRemoteViewsFactory(private val context: Context) : RemoteViewsService.RemoteViewsFactory {

    private var records: List<AttendanceRecord> = emptyList()

    override fun onCreate() {
        loadData()
    }

    override fun onDataSetChanged() {
        loadData()
    }

    private fun loadData() {
        val prefs = UserPreferences(context)
        val list = prefs.getCachedAttendance()?.records ?: emptyList()
        records = list.sortedWith(
            compareBy<AttendanceRecord> { record ->
                if (record.totalClasses == 0) 1 else 0
            }.thenBy { record ->
                record.percentage
            }
        )
    }

    override fun onDestroy() {
        records = emptyList()
    }

    override fun getCount(): Int = records.size

    override fun getViewAt(position: Int): RemoteViews {
        val views = RemoteViews(context.packageName, R.layout.widget_attendance_item)
        if (position >= records.size) return views

        val item = records[position]
        views.setTextViewText(R.id.tv_item_subject_name, item.subjectName)
        views.setTextViewText(
            R.id.tv_item_subject_counts,
            "${item.attendedClasses}/${item.totalClasses} classes • ${item.subjectCode}"
        )

        val pct = item.percentage
        val pctStr = String.format(Locale.getDefault(), "%.1f%%", pct)
        views.setTextViewText(R.id.tv_item_percentage_badge, pctStr)

        val prefs = UserPreferences(context)
        val showBunks = prefs.isWidgetBunksEnabled()

        if (!showBunks) {
            views.setViewVisibility(R.id.tv_item_bunk_chip, View.GONE)
        } else {
            views.setViewVisibility(R.id.tv_item_bunk_chip, View.VISIBLE)
            if (pct >= 75.0) {
                val safeBunks = item.calculateSafeBunks()
                views.setTextViewText(R.id.tv_item_bunk_chip, if (safeBunks > 0) "+$safeBunks bunk${if (safeBunks > 1) "s" else ""}" else "Safe")
                views.setTextColor(R.id.tv_item_bunk_chip, 0xFF30D158.toInt())
            } else {
                val needed = item.calculateClassesNeeded()
                views.setTextViewText(R.id.tv_item_bunk_chip, "Need $needed")
                views.setTextColor(R.id.tv_item_bunk_chip, if (pct >= 70.0) 0xFFFF9F0A.toInt() else 0xFFFF453A.toInt())
            }
        }

        if (pct >= 75.0) {
            views.setInt(R.id.tv_item_percentage_badge, "setBackgroundResource", R.drawable.widget_badge_safe)
            views.setTextColor(R.id.tv_item_percentage_badge, 0xFF30D158.toInt())
        } else if (pct >= 70.0) {
            views.setInt(R.id.tv_item_percentage_badge, "setBackgroundResource", R.drawable.widget_badge_warning)
            views.setTextColor(R.id.tv_item_percentage_badge, 0xFFFF9F0A.toInt())
        } else {
            views.setInt(R.id.tv_item_percentage_badge, "setBackgroundResource", R.drawable.widget_badge_danger)
            views.setTextColor(R.id.tv_item_percentage_badge, 0xFFFF453A.toInt())
        }

        val fillInIntent = Intent().apply {
            putExtra("destination_tab", "attendance")
        }
        views.setOnClickFillInIntent(R.id.widget_attendance_item_root, fillInIntent)

        return views
    }

    override fun getLoadingView(): RemoteViews? = null

    override fun getViewTypeCount(): Int = 1

    override fun getItemId(position: Int): Long = position.toLong()

    override fun hasStableIds(): Boolean = true
}
