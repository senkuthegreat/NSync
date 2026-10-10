package com.example.nsutconnect.data.sync

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.example.nsutconnect.MainActivity
import com.example.nsutconnect.R
import com.example.nsutconnect.data.local.UserPreferences
import java.util.concurrent.TimeUnit

class NsutSyncWorker(
    private val context: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(context, workerParams) {

    override suspend fun doWork(): Result {
        val prefs = UserPreferences(context)
        if (!prefs.isAutoLoginEnabled() || prefs.getRollNumber().isBlank()) {
            return Result.success()
        }

        val repository = NsutSyncRepository(context)
        val result = repository.syncData(isBackground = true)

        if (result is SyncResult.Success) {
            val attendance = repository.attendance.value
            if (attendance != null && prefs.isNotificationEnabled()) {
                val threshold = prefs.getAttendanceThreshold().toDouble()
                val lowSubjects = attendance.records.filter { it.percentage < threshold }
                if (lowSubjects.isNotEmpty()) {
                    showLowAttendanceNotification(lowSubjects.map { "${it.subjectName} (${String.format("%.1f", it.percentage)}%)" })
                }
            }
        }

        return Result.success()
    }

    private fun showLowAttendanceNotification(lowSubjects: List<String>) {
        val channelId = "nsut_attendance_alerts"
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "Attendance Warnings",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Alerts when attendance falls below threshold"
            }
            notificationManager.createNotificationChannel(channel)
        }

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(android.R.drawable.ic_dialog_alert)
            .setContentTitle("NSUT Attendance Alert!")
            .setContentText("${lowSubjects.size} subject(s) below 75% attendance: ${lowSubjects.first()}")
            .setStyle(
                NotificationCompat.BigTextStyle()
                    .bigText("The following subjects are below your target threshold:\n" + lowSubjects.joinToString("\n• ", prefix = "• "))
            )
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        notificationManager.notify(1001, notification)
    }

    companion object {
        private const val WORK_NAME = "nsut_periodic_sync_work"

        fun schedulePeriodicSync(context: Context, intervalHours: Long = 4) {
            val workRequest = PeriodicWorkRequestBuilder<NsutSyncWorker>(
                intervalHours, TimeUnit.HOURS
            ).build()

            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                WORK_NAME,
                ExistingPeriodicWorkPolicy.UPDATE,
                workRequest
            )
        }
    }
}
