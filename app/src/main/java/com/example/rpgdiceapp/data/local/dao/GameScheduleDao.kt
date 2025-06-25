package com.example.rpgdiceapp.data.local.dao

import androidx.room.*
import com.example.rpgdiceapp.data.local.entity.GameScheduleEntity

@Dao
interface GameScheduleDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSchedules(schedules: List<GameScheduleEntity>)

    @Query("DELETE FROM game_schedules WHERE gameId = :gameId")
    suspend fun deleteSchedulesForGame(gameId: Int)

    @Query("SELECT * FROM game_schedules WHERE gameId = :gameId")
    suspend fun getSchedulesForGame(gameId: Int): List<GameScheduleEntity>

    @Query("SELECT * FROM game_schedules")
    suspend fun getAllSchedules(): List<GameScheduleEntity>

    @Query("DELETE FROM game_schedules")
    suspend fun clearAllSchedules()


}
