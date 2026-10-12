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
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.util.Locale

import android.os.Bundle

class NsutScheduleWidgetReceiver : AppWidgetProvider() {

    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        for (appWidgetId in appWidgetIds) {
            updateWidget(context, appWidgetManager, appWidgetId)
        }
        super.onUpdate(context, appWidgetManager, appWidgetIds)
    }

    override fun onAppWidgetOptionsChanged(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetId: Int,
        newOptions: Bundle
    ) {
        super.onAppWidgetOptionsChanged(context, appWidgetManager, appWidgetId, newOptions)
        updateWidget(context, appWidgetManager, appWidgetId)
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        if (intent.action == ACTION_REFRESH_SCHEDULE) {
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
        const val ACTION_REFRESH_SCHEDULE = "com.example.nsutconnect.ACTION_REFRESH_SCHEDULE"

        fun updateWidget(context: Context, appWidgetManager: AppWidgetManager, appWidgetId: Int) {
            val views = RemoteViews(context.packageName, R.layout.widget_schedule)
            val prefs = UserPreferences(context)
            val timetable = prefs.getCachedTimetable()

            // Dynamic Widget Theme Background
            val bgRes = when (prefs.getWidgetTheme()) {
                "oled" -> R.drawable.widget_card_bg_oled
                "dark" -> R.drawable.widget_card_bg_dark
                "translucent" -> R.drawable.widget_card_bg_translucent
                "midnight" -> R.drawable.widget_card_bg_midnight
                else -> R.drawable.widget_card_bg_oled
            }
            views.setInt(R.id.widget_schedule_root, "setBackgroundResource", bgRes)

            // Accent color for refresh button & badge
            val accentColor = when (prefs.getWidgetAccent()) {
                "cyan" -> 0xFF06B6D4.toInt()
                "emerald" -> 0xFF10B981.toInt()
                "amber" -> 0xFFF59E0B.toInt()
                "purple" -> 0xFF8B5CF6.toInt()
                else -> 0xFF6366F1.toInt()
            }
            views.setInt(R.id.btn_refresh_schedule, "setColorFilter", accentColor)

            // Render Unkempt bold cursive + indigo title header
            val headerBitmap = WidgetHeaderHelper.createHeaderBitmap(context, "Schedule")
            views.setImageViewBitmap(R.id.iv_widget_header_title, headerBitmap)

            val today = LocalDate.now().dayOfWeek
            val now = LocalTime.now()

            if (timetable != null) {
                val isWeekend = today == java.time.DayOfWeek.SATURDAY || today == java.time.DayOfWeek.SUNDAY
                val isTodayCompleted = timetable.isDayCompleted(today, now)

                val (activeDay, badgeText) = when {
                    isWeekend -> Pair(java.time.DayOfWeek.MONDAY, "MONDAY")
                    isTodayCompleted -> {
                        val tomorrow = today.plus(1)
                        if (tomorrow == java.time.DayOfWeek.SATURDAY || tomorrow == java.time.DayOfWeek.SUNDAY) {
                            Pair(java.time.DayOfWeek.MONDAY, "MONDAY")
                        } else {
                            Pair(tomorrow, "TOMORROW")
                        }
                    }
                    else -> Pair(today, "TODAY")
                }
                val activeClasses = timetable.getScheduleFor(activeDay)

                val dayName = activeDay.name.lowercase().replaceFirstChar { it.uppercase() }
                views.setTextViewText(R.id.tv_schedule_title, "NSync Schedule")
                views.setTextViewText(R.id.tv_schedule_day_subtitle, "$dayName Schedule")

                // Badge is always clean accent pill with TODAY / TOMORROW / MONDAY (no LIVE tag)
                views.setTextViewText(R.id.tv_schedule_badge, badgeText)
                views.setInt(R.id.tv_schedule_badge, "setBackgroundResource", R.drawable.widget_pill_accent)
                views.setTextColor(R.id.tv_schedule_badge, accentColor)

                views.setViewVisibility(R.id.layout_current_class_banner, View.GONE)
                if (activeClasses.isNotEmpty()) {
                    views.setViewVisibility(R.id.lv_schedule_classes, View.VISIBLE)
                    views.setViewVisibility(R.id.tv_schedule_empty, View.GONE)
                } else {
                    views.setViewVisibility(R.id.lv_schedule_classes, View.GONE)
                    views.setViewVisibility(R.id.tv_schedule_empty, View.VISIBLE)
                    val emptyMsg = when (badgeText) {
                        "TOMORROW" -> "No classes scheduled for tomorrow 🎉"
                        "MONDAY" -> "No classes scheduled for Monday 🎉"
                        else -> "No classes scheduled for today 🌴"
                    }
                    views.setTextViewText(R.id.tv_schedule_empty, emptyMsg)
                }
            } else {
                views.setViewVisibility(R.id.layout_current_class_banner, View.GONE)
                views.setViewVisibility(R.id.lv_schedule_classes, View.GONE)
                views.setViewVisibility(R.id.tv_schedule_empty, View.VISIBLE)
                views.setTextViewText(R.id.tv_schedule_empty, "Log in to sync timetable")
            }

            // Remote adapter for classes list
            val serviceIntent = Intent(context, NsutScheduleWidgetService::class.java).apply {
                putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
                data = Uri.parse(toUri(Intent.URI_INTENT_SCHEME))
            }
            views.setRemoteAdapter(R.id.lv_schedule_classes, serviceIntent)
            views.setEmptyView(R.id.lv_schedule_classes, R.id.tv_schedule_empty)

            // Intent to launch MainActivity to schedule tab
            val clickIntent = Intent(context, MainActivity::class.java).apply {
                putExtra("destination_tab", "schedule")
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            }
            val pendingClickIntent = PendingIntent.getActivity(
                context,
                201,
                clickIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            views.setOnClickPendingIntent(R.id.widget_schedule_root, pendingClickIntent)
            views.setPendingIntentTemplate(R.id.lv_schedule_classes, pendingClickIntent)

            // Intent for Refresh button
            val refreshIntent = Intent(context, NsutScheduleWidgetReceiver::class.java).apply {
                action = ACTION_REFRESH_SCHEDULE
            }
            val pendingRefreshIntent = PendingIntent.getBroadcast(
                context,
                202,
                refreshIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            views.setOnClickPendingIntent(R.id.btn_refresh_schedule, pendingRefreshIntent)

            appWidgetManager.updateAppWidget(appWidgetId, views)
            appWidgetManager.notifyAppWidgetViewDataChanged(appWidgetId, R.id.lv_schedule_classes)
        }

        fun updateAll(context: Context) {
            val appWidgetManager = AppWidgetManager.getInstance(context)
            val thisWidget = ComponentName(context, NsutScheduleWidgetReceiver::class.java)
            val allWidgetIds = appWidgetManager.getAppWidgetIds(thisWidget)
            for (id in allWidgetIds) {
                updateWidget(context, appWidgetManager, id)
            }
            appWidgetManager.notifyAppWidgetViewDataChanged(allWidgetIds, R.id.lv_schedule_classes)
        }

        fun requestPinWidget(context: Context): Boolean {
            val appWidgetManager = AppWidgetManager.getInstance(context)
            val myProvider = ComponentName(context, NsutScheduleWidgetReceiver::class.java)
            return if (appWidgetManager.isRequestPinAppWidgetSupported) {
                val previewViews = RemoteViews(context.packageName, R.layout.widget_schedule_preview)
                val extras = android.os.Bundle().apply {
                    putParcelable(AppWidgetManager.EXTRA_APPWIDGET_PREVIEW, previewViews)
                }
                appWidgetManager.requestPinAppWidget(myProvider, extras, null)
            } else {
                false
            }
        }
    }
}
