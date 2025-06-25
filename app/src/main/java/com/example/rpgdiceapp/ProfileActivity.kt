package com.example.rpgdiceapp

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.bumptech.glide.Glide
import com.example.rpgdiceapp.api.ApiService
import com.example.rpgdiceapp.data.local.AppDatabase
import com.example.rpgdiceapp.data.local.entity.GameScheduleEntity
import com.example.rpgdiceapp.data.local.entity.UserEntity
import com.example.rpgdiceapp.data.repository.UserRepository
import kotlinx.coroutines.*
import java.time.Instant

class ProfileActivity : AppCompatActivity() {

    private lateinit var profileImageView: ImageView
    private lateinit var usernameTextView: TextView
    private lateinit var fullNameTextView: TextView
    private lateinit var emailTextView: TextView
    private lateinit var gamesButton: Button
    private lateinit var testButton: Button
    private lateinit var syncButton: ImageButton


    private lateinit var userRepository: UserRepository

    @SuppressLint("SetTextI18n")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_profile)

        profileImageView = findViewById(R.id.profileImageView)
        usernameTextView = findViewById(R.id.usernameTextView)
        fullNameTextView = findViewById(R.id.fullNameTextView)
        emailTextView = findViewById(R.id.emailTextView)
        gamesButton = findViewById(R.id.GamesListButton)
        syncButton = findViewById<ImageButton>(R.id.SyncButton)
        testButton = findViewById(R.id.TestButton)

        userRepository = UserRepository(applicationContext)


        CoroutineScope(Dispatchers.IO).launch {
            val userEntity = getUserFromLocalDatabase()

            withContext(Dispatchers.Main) {
                if (userEntity != null) {

                    setProfileData(userEntity.username, userEntity.firstName, userEntity.surname, userEntity.email, userEntity.userPhotoPath)
                } else {

                    val intent = intent
                    val username = intent.getStringExtra("username")
                    val firstName = intent.getStringExtra("firstName")
                    val surname = intent.getStringExtra("surname")
                    val email = intent.getStringExtra("email")
                    val userPhotoPath = intent.getStringExtra("userPhotoPath")

                    if (username != null && firstName != null && surname != null && email != null && userPhotoPath != null) {
                        setProfileData(username, firstName, surname, email, userPhotoPath)
                    } else {

                        Log.w("PROFILE", "Brak danych w Intent – wykonuję GET /api/v1/authorized/user")

                        val apiService = ApiService(applicationContext)

                        apiService.getAuthorizedUser(
                            onSuccess = { user ->
                                runOnUiThread {
                                    setProfileData(
                                        user.username,
                                        user.firstName,
                                        user.surname,
                                        user.email,
                                        user.userPhotoPath
                                    )
                                }
                            },
                            onError = { err ->
                                Log.e("PROFILE", "Błąd pobierania danych użytkownika: $err")
                                runOnUiThread {
                                    emailTextView.text = "Błąd ładowania danych: $err"
                                }
                            }
                        )
                    }
                }
            }
        }

        gamesButton.setOnClickListener {
            val intent = Intent(this, UserGamesActivity::class.java)
            startActivity(intent)
        }

        testButton.setOnClickListener {
            insertTestSchedule(this)
        }

        syncButton.setOnClickListener {
            CoroutineScope(Dispatchers.IO).launch {
                val userId = userRepository.getLoggedInUserId()
                if (userId != null) {
                    try {
                        userRepository.syncAllUserData(userId.toInt())
                        runOnUiThread {
                            val intent = Intent(this@ProfileActivity, ProfileActivity::class.java)
                            finish() // zamknij bieżącą
                            startActivity(intent) // uruchom ponownie

                            Toast.makeText(this@ProfileActivity, "Dane zsynchronizowane", Toast.LENGTH_SHORT).show()
                        }
                    } catch (e: Exception) {
                        runOnUiThread {
                            Toast.makeText(this@ProfileActivity, "Błąd synchronizacji: ${e.message}", Toast.LENGTH_LONG).show()
                        }
                    }
                }
            }
        }

    }

    fun insertTestSchedule(context: Context, gameId: Int = 999) {
        val db = AppDatabase.getInstance(context)
        val dao = db.gameScheduleDao()

        val now = Instant.now()
        val start = now.plusSeconds(30).toString() // za 30 sekund
        val end = now.plusSeconds(3600).toString() // +1 godzina

        val testSchedule = GameScheduleEntity(
            start = start,
            end = end,
            gameId = gameId
        )

        CoroutineScope(Dispatchers.IO).launch {
            dao.insertSchedules(listOf(testSchedule))
            UserRepository(context).setReminderAlarms(context, listOf(testSchedule))
            Log.d("TEST", "Wstawiono testowy termin: $start")
        }
    }



    private suspend fun getUserFromLocalDatabase(): UserEntity? {
        val prefs = getSharedPreferences("auth", Context.MODE_PRIVATE)
        val userId = prefs.getLong("userId", -1)

        if (userId == -1L) {
            return null
        }

        return userRepository.getUserFromDatabase(userId)
    }

    fun onLogoClick(view: View) {
        val intent = Intent(this, MainActivity::class.java)
        startActivity(intent)
    }



    @SuppressLint("SetTextI18n")
    private fun setProfileData(
        username: String,
        firstName: String,
        surname: String,
        email: String,
        userPhotoPath: String?
    ) {
        usernameTextView.text = username
        fullNameTextView.text = "$firstName $surname"
        emailTextView.text = email

        val fallbackPath = "/img/profilePics/defaultProfilePic.png"
        val photoUrl = "http://192.168.0.157:8888${userPhotoPath ?: ""}"

        Glide.with(this)
            .load(photoUrl)
            .circleCrop()
            .placeholder(R.drawable.ic_default_profile)
            .error(
                Glide.with(this)
                    .load(R.drawable.ic_default_profile)
                    .circleCrop()
            )
            .into(profileImageView)

    }
}
