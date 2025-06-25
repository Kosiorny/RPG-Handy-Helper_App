package com.example.rpgdiceapp.data.local.entity

import androidx.room.Entity

@Entity(tableName = "game_schedules", primaryKeys = ["start", "gameId"])
data class GameScheduleEntity(
    val start: String,
    val end: String,
    val gameId: Int
)
