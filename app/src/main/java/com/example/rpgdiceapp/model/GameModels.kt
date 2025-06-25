package com.example.rpgdiceapp.model

data class UserGameResponse(
    val userGames: List<UserGame>,
    val message: String,
    val error: Int,
    val timestamp: String
)

data class UserGame(
    val id: Int,
    val name: String,
    val description: String,
    val systemName: String,
    val users: List<GameUser>
)

data class GameUser(
    val id: Int,
    val username: String,
    val name: String,
    val role: String,
    val avatarUrl: String
)

data class FutureGamesResponse(
    val futureGames: List<FutureGame>,
    val message: String,
    val error: Int,
    val timestamp: String
)

data class FutureGame(
    val start: String,
    val end: String
)
