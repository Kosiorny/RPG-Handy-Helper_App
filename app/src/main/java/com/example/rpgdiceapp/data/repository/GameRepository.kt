package com.example.rpgdiceapp.data.repository

import android.content.Context
import com.example.rpgdiceapp.api.ApiService
import com.example.rpgdiceapp.data.local.AppDatabase
import com.example.rpgdiceapp.data.local.entity.GameEntity
import com.example.rpgdiceapp.data.local.entity.GameUserEntity
import com.example.rpgdiceapp.model.UserGameResponse

class GameRepository(context: Context) {

    private val apiService = ApiService(context)
    private val db = AppDatabase.getInstance(context)

    suspend fun syncUserGames(userId: Int) {
        val response: UserGameResponse = apiService.getUserGamesSuspend()

        response.userGames.forEach { game ->
            val gameEntity = GameEntity(
                game.id,
                game.name,
                game.description,
                game.systemName)
            db.gameDao().insertGames(listOf(gameEntity))

            db.gameDao().deleteUsersForGame(game.id)
            val users = game.users.map {
                GameUserEntity(
                    it.id,
                    it.username,
                    it.name,
                    it.role,
                    it.avatarUrl,
                    game.id)
            }
            db.gameDao().insertGameUsers(users)
        }
    }

    suspend fun clearGameData() {
        db.gameScheduleDao().clearAllSchedules()
        db.gameDao().clearAllGameUsers()
        db.gameDao().clearAllGames()
    }


}
