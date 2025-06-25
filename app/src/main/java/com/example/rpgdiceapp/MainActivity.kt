package com.example.rpgdiceapp

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import com.example.rpgdiceapp.data.repository.UserRepository

class MainActivity : AppCompatActivity() {

    private lateinit var userRepository: UserRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        userRepository = UserRepository(applicationContext)

        val loginBtn = findViewById<Button>(R.id.loginButtonMain)
        val registerButton = findViewById<Button>(R.id.registerButtonMain)
        val diceButton: Button = findViewById(R.id.diceButton)
        val profileButton: Button = findViewById(R.id.profileButton)
        val logoutButton: Button =  findViewById<Button>(R.id.logoutButton)

        diceButton.setOnClickListener {
            val intent = Intent(this, DiceActivity::class.java)
            startActivity(intent)
        }


        if (userRepository.isUserLoggedIn()) {
            profileButton.visibility = View.VISIBLE
            logoutButton.visibility = View.VISIBLE
            loginBtn.visibility = View.GONE
            registerButton.visibility = View.GONE
        } else {
            profileButton.visibility = View.GONE
            logoutButton.visibility = View.GONE
            loginBtn.visibility = View.VISIBLE
            registerButton.visibility = View.VISIBLE
        }


        profileButton.setOnClickListener {
            val intent = Intent(this, ProfileActivity::class.java)
            startActivity(intent)
        }

        logoutButton.setOnClickListener {
            userRepository.logout()

            val intent = Intent(this, MainActivity::class.java)
            startActivity(intent)
            finish()
        }

        registerButton.setOnClickListener {
            val intent = Intent(this, RegisterWebViewActivity::class.java)
            startActivity(intent)
        }

        loginBtn.setOnClickListener {
            startActivity(Intent(this, LoginActivity::class.java))
        }
    }
}

