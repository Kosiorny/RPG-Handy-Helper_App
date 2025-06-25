package com.example.rpgdiceapp.data.repository

import android.content.Context
import com.example.rpgdiceapp.api.ApiService
import com.example.rpgdiceapp.data.local.AppDatabase
import com.example.rpgdiceapp.data.local.entity.UserEntity
import com.example.rpgdiceapp.logic.OfflineLoginManager
import com.example.rpgdiceapp.model.UserData
import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Intent
import android.util.Log
import com.example.rpgdiceapp.data.local.entity.GameScheduleEntity
import com.example.rpgdiceapp.notifications.ReminderReceiver
import java.time.Instant
import androidx.core.content.edit
import retrofit2.HttpException
import java.io.IOException
import java.time.LocalDateTime
import java.time.ZoneId


class UserRepository(private val context: Context) {

    private val apiService = ApiService(context)
    private val db = AppDatabase.getInstance(context)
    private val prefs = context.getSharedPreferences("auth", Context.MODE_PRIVATE)


    suspend fun login(username: String, password: String): Result<UserEntity> {
        return try {

            apiService.loginSuspend(username, password)
            val userData = apiService.getAuthorizedUserSuspend()

            val lastUsername = prefs.getString("username", null)
            if (lastUsername != null && lastUsername != userData.username) {
                // Użytkownik się zmienił → wyczyść bazę
                db.userDao().clearAll()
                db.gameDao().clearAllGames()
                db.gameDao().clearAllGameUsers()
                db.gameScheduleDao().clearAllSchedules()
                Log.d("USER_REPO", "Zmieniono użytkownika – baza została wyczyszczona.")
            }

            // Zapisz nowego użytkownika
            prefs.edit { putString("username", userData.username) }



            val gameRepository = GameRepository(context)
            val scheduleRepository = GameScheduleRepository(context)

            // 🔁 Czyszczenie danych z poprzedniego stanu
            gameRepository.clearGameData()

            // 📥 Synchronizacja z serwera
            gameRepository.syncUserGames(userData.id)

            val gamesResponse = apiService.getUserGamesSuspend()
            gamesResponse.userGames.forEach { game ->
                scheduleRepository.syncSchedulesForGame(game.id)
            }

            val allSchedules = gamesResponse.userGames.flatMap { game ->
                db.gameScheduleDao().getSchedulesForGame(game.id)
            }
            setReminderAlarms(context, allSchedules)

            saveUserLoggedInStatus(userData.id)

            Result.success(userData.toEntityWithHashedPassword(password)).also {
                OfflineLoginManager.saveUserLocally(context, userData, password)
            }

        } catch (e: HttpException) {
            val code = e.code()
            val errorBody = e.response()?.errorBody()?.string()
            val message = Regex("\"message\"\\s*:\\s*\"([^\"]+)\"")
                .find(errorBody.orEmpty())
                ?.groupValues?.get(1)
                ?: when (code) {
                    401, 403, 404 -> "Nieprawidłowy login lub hasło"
                    else -> "Błąd serwera ($code)"
                }

            Log.e("LOGIN_EXCEPTION_MESSAGE", "Zwracany komunikat: $message")

            return Result.failure(Exception(message))

        } catch (e: IOException) {
            // Brak internetu
            val offlineUser = OfflineLoginManager.loginOffline(context, username, password)
            if (offlineUser != null) {
                saveUserLoggedInStatus(offlineUser.id)
                Result.success(offlineUser)
            } else {
                Result.failure(Exception("Brak połączenia z Internetem i brak danych offline"))
            }
        } catch (e: Exception) {
            Log.e("LOGIN_EXCEPTION", "Nieznany wyjątek", e)
            return Result.failure(e)
        }
    }

    fun clearAllReminderAlarms(context: Context) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        for (id in 0..9999) {
            val intent = Intent(context, ReminderReceiver::class.java)
            val pendingIntent = PendingIntent.getBroadcast(
                context,
                id,
                intent,
                PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
            )
            if (pendingIntent != null) {
                alarmManager.cancel(pendingIntent)
            }
        }
    }

    suspend fun setReminderAlarms(context: Context, schedules: List<GameScheduleEntity>) {
        clearAllReminderAlarms(context)

        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val db = AppDatabase.getInstance(context)
        val gameDao = db.gameDao()

        schedules.forEach { schedule ->
            val game = gameDao.getGameById(schedule.gameId)
            val gameTitle = game?.name ?: "Twoja gra"
            val instant = try {
                Instant.parse(schedule.start)
            } catch (e: Exception) {
                LocalDateTime.parse(schedule.start)
                    .atZone(ZoneId.systemDefault())
                    .toInstant()
            }

            val zonedDateTime = instant.atZone(ZoneId.systemDefault())
            val hourText = zonedDateTime.toLocalTime().toString().substring(0, 5)

            val oneHourBefore = instant.minusSeconds(3600).toEpochMilli()
            val oneDayBefore = instant.minusSeconds(86400).toEpochMilli()


            fun setAlarm(timeMillis: Long, requestCodeOffset: Int, message: String) {
                val intent = Intent(context, ReminderReceiver::class.java).apply {
                    putExtra("notificationMessage", message)
                }
                val pendingIntent = PendingIntent.getBroadcast(
                    context,
                    schedule.gameId + requestCodeOffset,
                    intent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )
                alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, timeMillis, pendingIntent)
            }

            setAlarm(
                oneDayBefore,
                1000,
                "Pamiętaj, jutro gra '$gameTitle' już o $hourText."
            )

            setAlarm(
                oneHourBefore,
                2000,
                "Gra '$gameTitle' zaczyna się już o $hourText, nie zapomnij."
            )
        }
    }



    suspend fun getUserFromDatabase(userId: Long): UserEntity? {
        return db.userDao().getUserById(userId)
    }

    private fun saveUserLoggedInStatus(userId: Int) {
        prefs.edit {
            putLong("userId", userId.toLong())
        }
    }

    fun isUserLoggedIn(): Boolean {
        val userId = prefs.getLong("userId", -1)
        return userId != -1L
    }


    fun logout() {
        prefs.edit {
            remove("userId")
        }
    }

    fun getLoggedInUserId(): Int? {
        val id = prefs.getLong("userId", -1L)
        return if (id != -1L) id.toInt() else null
    }

    suspend fun syncAllUserData(userId: Int) {
        val gameRepository = GameRepository(context)
        val scheduleRepository = GameScheduleRepository(context)

        // 🧑‍💻 Pobierz aktualne dane użytkownika z serwera
        val userData = apiService.getAuthorizedUserSuspend()

        // 🔐 Pobierz hasło z bazy, jeśli istnieje
        val localUser = db.userDao().getUserById(userData.id.toLong())
        val existingPasswordHash = localUser?.passwordHash ?: OfflineLoginManager.hashPassword("")

        // 💾 Zaktualizuj dane użytkownika lokalnie
        OfflineLoginManager.saveUserLocally(context, userData, existingPasswordHash)

        // 🧹 Wyczyść dane gier
        gameRepository.clearGameData()

        // ⬇️ Pobierz dane gier i zapisz lokalnie
        gameRepository.syncUserGames(userId)

        // ⬇️ Pobierz i zaktualizuj harmonogramy dla każdej gry
        val gamesResponse = apiService.getUserGamesSuspend()
        gamesResponse.userGames.forEach { game ->
            scheduleRepository.syncSchedulesForGame(game.id)
        }

        val scheduleDao = AppDatabase.getInstance(context).gameScheduleDao()
        val allSchedules = scheduleDao.getAllSchedules()
        val userRepo = UserRepository(context)
        userRepo.setReminderAlarms(context, allSchedules)

    }


}



fun UserData.toEntityWithHashedPassword(password: String): UserEntity {
    return UserEntity(
        id = this.id,
        username = this.username,
        firstName = this.firstName,
        surname = this.surname,
        email = this.email,
        userPhotoPath = this.userPhotoPath,
        passwordHash = OfflineLoginManager.hashPassword(password)
    )
}
