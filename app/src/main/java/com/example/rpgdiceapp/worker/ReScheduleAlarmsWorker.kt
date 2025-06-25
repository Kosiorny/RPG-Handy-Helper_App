package com.example.rpgdiceapp.worker

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.rpgdiceapp.data.local.AppDatabase
import com.example.rpgdiceapp.notifications.ReminderReceiver
import java.time.Instant

class ReScheduleAlarmsWorker(context: Context, workerParams: WorkerParameters) : CoroutineWorker(context, workerParams) {
    override suspend fun doWork(): Result {
        val db = AppDatabase.getInstance(applicationContext)
        val schedules = db.gameScheduleDao().getAllSchedules()
        val alarmManager = applicationContext.getSystemService(Context.ALARM_SERVICE) as AlarmManager

        schedules.forEach { schedule ->
            val startMillis = Instant.parse(schedule.start).toEpochMilli()

            val intent = Intent(applicationContext, ReminderReceiver::class.java).apply {
                putExtra("gameId", schedule.gameId)
            }
            val pendingIntent = PendingIntent.getBroadcast(
                applicationContext,
                schedule.gameId,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            alarmManager.setAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                startMillis,
                pendingIntent
            )
        }

        return Result.success()
    }
}
