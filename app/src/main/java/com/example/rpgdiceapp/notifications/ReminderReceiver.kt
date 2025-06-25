package com.example.rpgdiceapp.notifications

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import com.example.rpgdiceapp.R
import kotlin.random.Random

class ReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        val channel = NotificationChannel("game_reminders", "Game Reminders", NotificationManager.IMPORTANCE_HIGH)
        notificationManager.createNotificationChannel(channel)

        val message = intent.getStringExtra("notificationMessage") ?: "Zbliża się zaplanowana sesja RPG!"

        val notification = NotificationCompat.Builder(context, "game_reminders")
            .setContentTitle("Przypomnienie o grze")
            .setContentText(message)
            .setSmallIcon(R.drawable.logo_dark)
            .setAutoCancel(true)
            .build()

        notificationManager.notify(Random.nextInt(), notification)
    }

}
