package com.example.rpgdiceapp.data.repository

import android.content.Context
import com.example.rpgdiceapp.api.ApiService
import com.example.rpgdiceapp.data.local.AppDatabase
import com.example.rpgdiceapp.data.local.entity.GameScheduleEntity

class GameScheduleRepository(context: Context) {

    private val apiService = ApiService(context)
    private val db = AppDatabase.getInstance(context)

    suspend fun syncSchedulesForGame(gameId: Int) {
        val response = apiService.getFutureGamesSuspend(gameId)

        db.gameScheduleDao().deleteSchedulesForGame(gameId)

        val schedules = response.futureGames?.map {
            GameScheduleEntity(it.start, it.end, gameId)
        } ?: emptyList()

        db.gameScheduleDao().insertSchedules(schedules)
    }



}
