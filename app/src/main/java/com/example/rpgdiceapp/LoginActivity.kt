package com.example.rpgdiceapp

import android.annotation.SuppressLint
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.text.InputType
import android.util.Log
import android.view.MotionEvent
import android.view.View
import android.widget.EditText
import android.widget.ProgressBar
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.example.rpgdiceapp.data.repository.UserRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import androidx.core.content.edit

class LoginActivity : AppCompatActivity() {
    private var passwordVisible = false
    private lateinit var userRepository: UserRepository

    @SuppressLint("ClickableViewAccessibility")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        userRepository = UserRepository(applicationContext)

        val progressBar = findViewById<ProgressBar>(R.id.progressBar)
        val loginField = findViewById<EditText>(R.id.loginEditText)
        val passwordField = findViewById<EditText>(R.id.passwordEditText)
        val loginButton = findViewById<com.google.android.material.button.MaterialButton>(R.id.loginButton)

        passwordField.setOnTouchListener { v, event ->
            if (event.action == MotionEvent.ACTION_UP) {
                val drawableEnd = 2
                if (event.rawX >= (passwordField.right - passwordField.compoundDrawables[drawableEnd].bounds.width())) {
                    passwordVisible = !passwordVisible
                    passwordField.inputType = if (passwordVisible)
                        InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD
                    else
                        InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
                    passwordField.setSelection(passwordField.text.length)
                    v.performClick()
                    return@setOnTouchListener true
                }
            }
            false
        }

        loginButton.setOnClickListener {
            val login = loginField.text.toString().trim()
            val password = passwordField.text.toString().trim()

            if (login.isBlank() || password.isBlank()) {
                Toast.makeText(this, "Uzupełnij login i hasło", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            Log.d("LOGIN_DEBUG", "login: '$login', password: '$password'")

            progressBar.visibility = View.VISIBLE

            CoroutineScope(Dispatchers.IO).launch {
                val result = userRepository.login(login, password)
                runOnUiThread {

                    progressBar.visibility = View.GONE

                    result
                        .onSuccess { user ->
                            Toast.makeText(this@LoginActivity, "Zalogowano", Toast.LENGTH_SHORT).show()

                            val prefs = getSharedPreferences("settings", MODE_PRIVATE)
                            val alreadyAsked = prefs.getBoolean("askedForNotifications", false)

                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && !alreadyAsked) {
                                if (ContextCompat.checkSelfPermission(
                                        this@LoginActivity,
                                        android.Manifest.permission.POST_NOTIFICATIONS
                                    ) != PackageManager.PERMISSION_GRANTED
                                ) {
                                    ActivityCompat.requestPermissions(
                                        this@LoginActivity,
                                        arrayOf(android.Manifest.permission.POST_NOTIFICATIONS),
                                        1234
                                    )
                                }

                                prefs.edit { putBoolean("askedForNotifications", true) }
                            }

//                            // ✳️ Uruchomienie WorkManager po zalogowaniu
//                            val workRequest = PeriodicWorkRequestBuilder<com.example.rpgdiceapp.worker.SyncWorker>(
//                                3, TimeUnit.HOURS // lub inny interwał: np. 2, 6, 12
//                            ).setConstraints(
//                                Constraints.Builder()
//                                    .setRequiredNetworkType(NetworkType.CONNECTED)
//                                    .build()
//                            ).build()
//
//                            WorkManager.getInstance(this@LoginActivity).enqueueUniquePeriodicWork(
//                                "periodic_sync",
//                                ExistingPeriodicWorkPolicy.UPDATE,
//                                workRequest
//                            )


                            val intent = Intent(this@LoginActivity, ProfileActivity::class.java).apply {
                                putExtra("username", user.username)
                                putExtra("firstName", user.firstName)
                                putExtra("surname", user.surname)
                                putExtra("email", user.email)
                                putExtra("userPhotoPath", user.userPhotoPath)
                            }
                            startActivity(intent)
                            finish()
                        }
                        .onFailure { e ->
                            Log.e("LOGIN_ERROR", "Błąd: ${e.message}")

                            val message = e.message?.lowercase().orEmpty()

                            val msg = when {
                                "login" in message && "hasło" in message ->
                                    "Błędny login lub hasło"
                                "brak połączenia" in message ->
                                    "Brak połączenia z serwerem i brak danych offline"
                                "błąd serwera" in message ->
                                    "Problem po stronie serwera – spróbuj ponownie"
                                else ->
                                    "Błąd logowania: ${e.message}"
                            }

                            Toast.makeText(this@LoginActivity, msg, Toast.LENGTH_LONG).show()
                        }






                }
            }
        }
    }

    fun onLogoClick(view: View) {
        startActivity(Intent(this, MainActivity::class.java))
    }

    fun onRegisterClick(view: View) {
        startActivity(Intent(this, RegisterWebViewActivity::class.java))
    }
}

