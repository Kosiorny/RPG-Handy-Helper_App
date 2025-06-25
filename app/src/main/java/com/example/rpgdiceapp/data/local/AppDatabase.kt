package com.example.rpgdiceapp.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.rpgdiceapp.data.local.dao.UserDao
import com.example.rpgdiceapp.data.local.dao.GameDao
import com.example.rpgdiceapp.data.local.dao.GameScheduleDao
import com.example.rpgdiceapp.data.local.entity.UserEntity
import com.example.rpgdiceapp.data.local.entity.GameEntity
import com.example.rpgdiceapp.data.local.entity.GameUserEntity
import com.example.rpgdiceapp.data.local.entity.GameScheduleEntity

@Database(
    entities = [
        UserEntity::class,
        GameEntity::class,
        GameUserEntity::class,
        GameScheduleEntity::class
    ],
    version = 2
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun userDao(): UserDao
    abstract fun gameDao(): GameDao
    abstract fun gameScheduleDao(): GameScheduleDao

    companion object {
        @Volatile private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "rpg_db"
                )
                    .fallbackToDestructiveMigration() // 💥 resetuje bazę przy zmianie schematu
                    .build()
                    .also { INSTANCE = it }
            }
        }

    }
}
