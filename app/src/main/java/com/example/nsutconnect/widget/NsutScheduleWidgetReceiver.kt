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

class NsutScheduleWidgetReceiver : AppWidgetProvider() {

    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        for (appWidgetId in appWidgetIds) {
            updateWidget(context, appWidgetManager, appWidgetId)
        }
        super.onUpdate(context, appWidgetManager, appWidgetIds)
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

            val today = LocalDate.now().dayOfWeek
            val now = LocalTime.now()

            if (timetable != null) {
                val isTodayCompleted = timetable.isDayCompleted(today, now)
                val targetDay = timetable.getAutoDefaultDay(today, now)
                val isShowingTomorrow = isTodayCompleted || (today != targetDay && today != java.time.DayOfWeek.SATURDAY && today != java.time.DayOfWeek.SUNDAY)
                val activeDay = if (isShowingTomorrow) targetDay else today
                val activeClasses = timetable.getScheduleFor(activeDay)

                val dayName = activeDay.name.lowercase().replaceFirstChar { it.uppercase() }
                val daySubtitle = if (isShowingTomorrow) {
                    "Tomorrow · $dayName"
                } else {
                    "$dayName · ${LocalDate.now().format(DateTimeFormatter.ofPattern("EEE, MMM d", Locale.getDefault()))}"
                }

                views.setTextViewText(R.id.tv_schedule_title, "NSUT Schedule")
                views.setTextViewText(R.id.tv_schedule_day_subtitle, daySubtitle)

                if (isShowingTomorrow) {
                    views.setTextViewText(R.id.tv_schedule_badge, "TOMORROW")
                    views.setInt(R.id.tv_schedule_badge, "setBackgroundResource", R.drawable.widget_pill_accent)
                } else {
                    val (currentOrNext, isOngoing) = timetable.getCurrentOrNextClass(today, now)
                    views.setTextViewText(R.id.tv_schedule_badge, if (isOngoing) "LIVE" else "TODAY")
                }

                if (activeClasses.isNotEmpty()) {
                    views.setViewVisibility(R.id.lv_schedule_classes, View.VISIBLE)
                    views.setViewVisibility(R.id.tv_schedule_empty, View.GONE)

                    if (!isShowingTomorrow) {
                        val (currentOrNext, isOngoing) = timetable.getCurrentOrNextClass(today, now)
                        if (currentOrNext != null) {
                            views.setViewVisibility(R.id.layout_current_class_banner, View.VISIBLE)
                            if (isOngoing) {
                                views.setTextViewText(R.id.tv_current_class_status, "NOW IN PROGRESS (${currentOrNext.timeSlotDisplay})")
                                views.setTextColor(R.id.tv_current_class_status, 0xFF68D391.toInt())
                            } else {
                                views.setTextViewText(R.id.tv_current_class_status, "NEXT UP AT ${currentOrNext.startTimeFormatted}")
                                views.setTextColor(R.id.tv_current_class_status, 0xFF6366F1.toInt())
                            }
                            views.setTextViewText(
                                R.id.tv_current_class_details,
                                "${currentOrNext.subjectName} · ${currentOrNext.roomNumber}"
                            )
                        } else {
                            views.setViewVisibility(R.id.layout_current_class_banner, View.GONE)
                        }
                    } else {
                        // Tomorrow mode
                        val firstClass = activeClasses.firstOrNull()
                        if (firstClass != null) {
                            views.setViewVisibility(R.id.layout_current_class_banner, View.VISIBLE)
                            views.setTextViewText(R.id.tv_current_class_status, "FIRST CLASS TOMORROW (${firstClass.startTimeFormatted})")
                            views.setTextColor(R.id.tv_current_class_status, 0xFF6366F1.toInt())
                            views.setTextViewText(
                                R.id.tv_current_class_details,
                                "${firstClass.subjectName} · ${firstClass.roomNumber}"
                            )
                        } else {
                            views.setViewVisibility(R.id.layout_current_class_banner, View.GONE)
                        }
                    }
                } else {
                    views.setViewVisibility(R.id.layout_current_class_banner, View.GONE)
                    views.setViewVisibility(R.id.lv_schedule_classes, View.GONE)
                    views.setViewVisibility(R.id.tv_schedule_empty, View.VISIBLE)
                    views.setTextViewText(R.id.tv_schedule_empty, "No classes scheduled for $dayName 🎉")
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
                appWidgetManager.requestPinAppWidget(myProvider, null, null)
            } else {
                false
            }
        }
    }
}
