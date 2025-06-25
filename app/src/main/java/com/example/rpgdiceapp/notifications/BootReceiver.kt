package com.example.rpgdiceapp.notifications

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.example.rpgdiceapp.worker.ReScheduleAlarmsWorker

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            val work = OneTimeWorkRequestBuilder<ReScheduleAlarmsWorker>().build()
            WorkManager.getInstance(context).enqueue(work)
        }
    }
}
