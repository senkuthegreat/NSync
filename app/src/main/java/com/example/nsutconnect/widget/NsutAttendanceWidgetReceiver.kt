package com.example.nsutconnect.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.view.View
import android.widget.RemoteViews
import com.example.nsutconnect.MainActivity
import com.example.nsutconnect.R
import com.example.nsutconnect.data.local.UserPreferences
import com.example.nsutconnect.data.sync.NsutSyncRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class NsutAttendanceWidgetReceiver : AppWidgetProvider() {

    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        for (appWidgetId in appWidgetIds) {
            updateWidget(context, appWidgetManager, appWidgetId)
        }
        super.onUpdate(context, appWidgetManager, appWidgetIds)
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        if (intent.action == ACTION_REFRESH_ATTENDANCE) {
            val pendingResult = goAsync()
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    val repo = NsutSyncRepository(context)
                    repo.syncData(isBackground = true)
                    updateAll(context)
                } finally {
                    pendingResult.finish()
                }
            }
        }
    }

    companion object {
        const val ACTION_REFRESH_ATTENDANCE = "com.example.nsutconnect.ACTION_REFRESH_ATTENDANCE"

        fun updateWidget(context: Context, appWidgetManager: AppWidgetManager, appWidgetId: Int) {
            val views = RemoteViews(context.packageName, R.layout.widget_attendance)
            val prefs = UserPreferences(context)
            val attendance = prefs.getCachedAttendance()

            // Dynamic Widget Theme Background
            val bgRes = when (prefs.getWidgetTheme()) {
                "oled" -> R.drawable.widget_card_bg_oled
                "dark" -> R.drawable.widget_card_bg_dark
                "translucent" -> R.drawable.widget_card_bg_translucent
                "midnight" -> R.drawable.widget_card_bg_midnight
                else -> R.drawable.widget_card_bg_oled
            }
            views.setInt(R.id.widget_attendance_root, "setBackgroundResource", bgRes)

            // Accent color for refresh button
            val accentColor = when (prefs.getWidgetAccent()) {
                "cyan" -> 0xFF06B6D4.toInt()
                "emerald" -> 0xFF10B981.toInt()
                "amber" -> 0xFFF59E0B.toInt()
                "purple" -> 0xFF8B5CF6.toInt()
                else -> 0xFF6366F1.toInt()
            }
            views.setInt(R.id.btn_refresh_attendance, "setColorFilter", accentColor)

            if (attendance != null && attendance.records.isNotEmpty()) {
                val timeStr = SimpleDateFormat("h:mm a", Locale.getDefault()).format(Date(attendance.lastSyncedTimestamp))
                views.setTextViewText(R.id.tv_attendance_subtitle, "${attendance.records.size} Courses · $timeStr")
                views.setViewVisibility(R.id.tv_overall_percentage, View.GONE)
                views.setViewVisibility(R.id.layout_stats_bar, View.GONE)
                views.setViewVisibility(R.id.lv_attendance_subjects, View.VISIBLE)
                views.setViewVisibility(R.id.tv_attendance_empty, View.GONE)
            } else {
                views.setTextViewText(R.id.tv_attendance_subtitle, "Tap to log in and sync")
                views.setViewVisibility(R.id.tv_overall_percentage, View.GONE)
                views.setViewVisibility(R.id.layout_stats_bar, View.GONE)
                views.setViewVisibility(R.id.lv_attendance_subjects, View.GONE)
                views.setViewVisibility(R.id.tv_attendance_empty, View.VISIBLE)
            }

            // Set up RemoteViewsService for ListView
            val serviceIntent = Intent(context, NsutAttendanceWidgetService::class.java).apply {
                putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
                data = Uri.parse(toUri(Intent.URI_INTENT_SCHEME))
            }
            views.setRemoteAdapter(R.id.lv_attendance_subjects, serviceIntent)
            views.setEmptyView(R.id.lv_attendance_subjects, R.id.tv_attendance_empty)

            // Intent to launch MainActivity when clicking the widget body
            val clickIntent = Intent(context, MainActivity::class.java).apply {
                putExtra("destination_tab", "attendance")
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            }
            val pendingClickIntent = PendingIntent.getActivity(
                context,
                101,
                clickIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            views.setOnClickPendingIntent(R.id.widget_attendance_root, pendingClickIntent)
            views.setPendingIntentTemplate(R.id.lv_attendance_subjects, pendingClickIntent)

            // Intent for Refresh button
            val refreshIntent = Intent(context, NsutAttendanceWidgetReceiver::class.java).apply {
                action = ACTION_REFRESH_ATTENDANCE
            }
            val pendingRefreshIntent = PendingIntent.getBroadcast(
                context,
                102,
                refreshIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            views.setOnClickPendingIntent(R.id.btn_refresh_attendance, pendingRefreshIntent)

            appWidgetManager.updateAppWidget(appWidgetId, views)
            appWidgetManager.notifyAppWidgetViewDataChanged(appWidgetId, R.id.lv_attendance_subjects)
        }

        fun updateAll(context: Context) {
            val appWidgetManager = AppWidgetManager.getInstance(context)
            val thisWidget = ComponentName(context, NsutAttendanceWidgetReceiver::class.java)
            val allWidgetIds = appWidgetManager.getAppWidgetIds(thisWidget)
            for (id in allWidgetIds) {
                updateWidget(context, appWidgetManager, id)
            }
            appWidgetManager.notifyAppWidgetViewDataChanged(allWidgetIds, R.id.lv_attendance_subjects)
        }

        fun requestPinWidget(context: Context): Boolean {
            val appWidgetManager = AppWidgetManager.getInstance(context)
            val myProvider = ComponentName(context, NsutAttendanceWidgetReceiver::class.java)
            return if (appWidgetManager.isRequestPinAppWidgetSupported) {
                appWidgetManager.requestPinAppWidget(myProvider, null, null)
            } else {
                false
            }
        }
    }
}
