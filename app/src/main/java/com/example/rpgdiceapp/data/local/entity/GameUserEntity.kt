package com.example.rpgdiceapp.data.local.entity

import androidx.room.Entity

@Entity(tableName = "game_users", primaryKeys = ["id", "gameId"])
data class GameUserEntity(
    val id: Int,
    val username: String,
    val name: String,
    val role: String,
    val avatarUrl: String,
    val gameId: Int
)
