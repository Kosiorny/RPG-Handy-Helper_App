package com.example.rpgdiceapp.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "users")
data class UserEntity(
    @PrimaryKey val id: Int,
    val username: String,
    val firstName: String,
    val surname: String,
    val email: String,
    val userPhotoPath: String,
    val passwordHash: String
)
