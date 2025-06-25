package com.example.rpgdiceapp.data.local.dao

import androidx.room.*
import com.example.rpgdiceapp.data.local.entity.GameEntity
import com.example.rpgdiceapp.data.local.entity.GameUserEntity

@Dao
interface GameDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertGames(games: List<GameEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertGameUsers(users: List<GameUserEntity>)

    @Query("DELETE FROM game_users WHERE gameId = :gameId")
    suspend fun deleteUsersForGame(gameId: Int)

    @Query("SELECT * FROM games WHERE id = :gameId LIMIT 1")
    suspend fun getGameById(gameId: Int): GameEntity?

    @Query("SELECT * FROM games")
    suspend fun getAllGames(): List<GameEntity>

    @Query("DELETE FROM games")
    suspend fun clearAllGames()

    @Query("DELETE FROM game_users")
    suspend fun clearAllGameUsers()

    @Query("SELECT * FROM game_users WHERE gameId = :gameId")
    suspend fun getUsersForGame(gameId: Int): List<GameUserEntity>


}
