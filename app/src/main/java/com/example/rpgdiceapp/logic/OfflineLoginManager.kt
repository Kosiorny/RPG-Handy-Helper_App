package com.example.rpgdiceapp.logic

import android.content.Context
import android.util.Log
import android.widget.Toast
import at.favre.lib.crypto.bcrypt.BCrypt
import com.example.rpgdiceapp.data.local.AppDatabase
import com.example.rpgdiceapp.data.local.entity.UserEntity
import com.example.rpgdiceapp.model.UserData
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

object OfflineLoginManager {




    fun hashPassword(password: String): String {
        return BCrypt.withDefaults().hashToString(12, password.toCharArray())
    }

    fun verifyPassword(input: String, storedHash: String): Boolean {
        return BCrypt.verifyer().verify(input.toCharArray(), storedHash).verified
    }

    suspend fun saveUserLocally(context: Context, userData: UserData, plainPassword: String) {
        val db = AppDatabase.getInstance(context)
        val dao = db.userDao()
        val hash = hashPassword(plainPassword)

        val entity = UserEntity(
            id = userData.id,
            username = userData.username,
            firstName = userData.firstName,
            surname = userData.surname,
            email = userData.email,
            userPhotoPath = userData.userPhotoPath,
            passwordHash = hash
        )

        dao.insertUser(entity)
        Log.d("OFFLINE_SAVE", "Zapisano użytkownika: ${userData.username}")
    }



    suspend fun loginOffline(context: Context, username: String, password: String): UserEntity? {
        val db = AppDatabase.getInstance(context)
        val user = db.userDao().getUserByUsername(username)
        return if (user != null && verifyPassword(password, user.passwordHash)) {
            withContext(Dispatchers.Main) {
                Toast.makeText(context, "Zalogowano offline", Toast.LENGTH_SHORT).show()
            }
            user
        } else {
            null
        }
    }
}