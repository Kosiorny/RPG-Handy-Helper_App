package com.example.rpgdiceapp.api

import android.content.Context
import com.example.rpgdiceapp.model.*
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.*
import retrofit2.converter.gson.GsonConverterFactory
import androidx.core.content.edit

class ApiService(context: Context) {

    private val prefs = context.getSharedPreferences("auth", Context.MODE_PRIVATE)

    fun saveToken(token: String) {
        prefs.edit { putString("jwt", token) }
    }

    fun getToken(): String? = prefs.getString("jwt", null)

    private val client = OkHttpClient.Builder()
        .addInterceptor(AuthInterceptor { getToken() })
        .addInterceptor(HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        })
        .build()
    private val retrofit = Retrofit.Builder()
        .baseUrl("http://10.0.2.2:8888/api/mobile/v1/")
        .client(client)
        .addConverterFactory(GsonConverterFactory.create())
        .build()
    private val api = retrofit.create(ApiInterface::class.java)

    fun getAuthorizedUser(onSuccess: (UserData) -> Unit, onError: (String) -> Unit) {
        api.getAuthorizedUser().enqueue(object : Callback<UserData> {
            override fun onResponse(call: Call<UserData>, response: Response<UserData>) {
                if (response.isSuccessful && response.body() != null) {
                    onSuccess(response.body()!!)
                } else {
                    onError("Błąd pobierania użytkownika: ${response.code()}")
                }
            }

            override fun onFailure(call: Call<UserData>, t: Throwable) {
                onError("Błąd sieci: ${t.message}")
            }
        })
    }

    fun loginSuspend(username: String, password: String) {
        val credentials = mapOf("username" to username, "password" to password)
        val response = api.login(credentials).execute()

        if (response.isSuccessful && response.body() != null) {
            val token = response.body()!!.authResponse.accessToken
            saveToken(token)
        } else {
            val errorJson = response.errorBody()?.string()
            val msg = Regex("\"message\"\\s*:\\s*\"([^\"]+)\"")
                .find(errorJson.orEmpty())
                ?.groupValues?.get(1)
                ?: "Błąd logowania: ${response.code()}"

            throw Exception(msg)
        }
    }


    fun getAuthorizedUserSuspend(): UserData {
        val response = api.getAuthorizedUser().execute()
        if (response.isSuccessful && response.body() != null) {
            return response.body()!!
        } else {
            throw Exception("Błąd pobierania użytkownika: ${response.code()}")
        }
    }

    fun getUserGamesSuspend(): UserGameResponse {
        val response = api.getUserGames().execute()
        if (response.isSuccessful && response.body() != null) {
            return response.body()!!
        } else {
            throw Exception("Błąd pobierania gier użytkownika: ${response.code()}")
        }
    }

    fun getFutureGamesSuspend(gameId: Int): FutureGamesResponse {
        val response = api.getFutureGames(gameId).execute()
        if (response.isSuccessful && response.body() != null) {
            return response.body()!!
        } else {
            throw Exception("Błąd pobierania terminów gry $gameId: ${response.code()}")
        }
    }

}
