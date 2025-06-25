package com.example.rpgdiceapp.api

import com.example.rpgdiceapp.model.*
import retrofit2.Call
import retrofit2.http.*

interface ApiInterface {


    @POST("auth/login")
    fun login(@Body credentials: Map<String, String>): Call<LoginResponse>

    @GET("authorized/user")
    fun getAuthorizedUser(): Call<UserData>

    @GET("authorized/game/userGames")
    fun getUserGames(): Call<UserGameResponse>

    @GET("authorized/schedulers/futureGames/{gameId}")
    fun getFutureGames(@Path("gameId") gameId: Int): Call<FutureGamesResponse>


}