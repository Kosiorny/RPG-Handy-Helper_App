package com.example.rpgdiceapp

import android.annotation.SuppressLint
import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
import android.os.Bundle
import android.util.Log
import android.view.Gravity
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.content.res.ResourcesCompat
import androidx.core.view.setPadding
import androidx.lifecycle.lifecycleScope
import com.example.rpgdiceapp.data.local.AppDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import androidx.core.view.isVisible
import androidx.core.graphics.toColorInt

class UserGamesActivity : AppCompatActivity() {

    private fun formatDateTime(iso: String): String {
        return try {
            val parsed = LocalDateTime.parse(iso)
            val formatter = DateTimeFormatter.ofPattern("dd.MM.yyyy, HH:mm")
            parsed.format(formatter)
        } catch (e: Exception) {
            Log.w("FormatDateTime", "Błąd parsowania daty: ${e.message}")
            iso
        }
    }

    fun onLogoClick(view: View) {
        val intent = Intent(this, MainActivity::class.java)
        startActivity(intent)
    }

    private fun getCurrentUserId(): Int {
        val prefs = getSharedPreferences("auth", MODE_PRIVATE)
        return prefs.getLong("userId", -1L).toInt()
    }

    private fun toggleVisibility(view: View) {
        view.visibility = if (view.isVisible) View.GONE else View.VISIBLE
    }

    @SuppressLint("SetTextI18n")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_user_games)

        val accentColor = getColor(R.color.accent)
        val backgroundColor = getColor(R.color.background)
        val textPrimary = getColor(R.color.textPrimary)
        val textSecondary = getColor(R.color.textSecondary)
        val success = getColor(R.color.success)

        val fontSpartan = ResourcesCompat.getFont(this, R.font.league_spartan_bold)
        val fontMono = ResourcesCompat.getFont(this, R.font.jetbrains_mono)

        val container = findViewById<LinearLayout>(R.id.gamesListView)
        container.setBackgroundColor(backgroundColor)
        window.decorView.setBackgroundColor(backgroundColor)

        val db = AppDatabase.getInstance(this)
        val gameDao = db.gameDao()
        val scheduleDao = db.gameScheduleDao()

        lifecycleScope.launch {
            val games = withContext(Dispatchers.IO) { gameDao.getAllGames() }

            games.forEach { game ->
                val gameContainer = LinearLayout(this@UserGamesActivity).apply {
                    orientation = LinearLayout.VERTICAL
                    setPadding(32)
                    setBackgroundResource(R.drawable.card_background)
                    val params = LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                    )
                    params.setMargins(0, 0, 0, 32)
                    layoutParams = params
                    elevation = 8f
                }

                val detailsContainer = LinearLayout(this@UserGamesActivity).apply {
                    orientation = LinearLayout.VERTICAL
                    visibility = View.GONE
                    setPadding(0, 16, 0, 0)
                }

                gameContainer.setOnClickListener {
                    toggleVisibility(detailsContainer)
                }


                val headerLayout = LinearLayout(this@UserGamesActivity).apply {
                    orientation = LinearLayout.VERTICAL
                }

                // Nagłówek systemu
                val systemText = TextView(this@UserGamesActivity).apply {
                    text = game.systemName
                    textSize = 14f
                    setTextColor(accentColor)
                    setTypeface(fontSpartan, Typeface.BOLD)

                }
                headerLayout.addView(systemText)

                // Tytuł kampanii
                val titleText = TextView(this@UserGamesActivity).apply {
                    text = game.name
                    textSize = 18f
                    setTextColor(textPrimary)
                    setTypeface(fontSpartan, Typeface.BOLD)
                }
                headerLayout.addView(titleText)

                // Opis gry
                val descText = TextView(this@UserGamesActivity).apply {
                    text = game.description
                    textSize = 14f
                    setTextColor(textSecondary)
                    typeface = fontMono
                }
                headerLayout.addView(descText)

                gameContainer.addView(headerLayout)

                val schedules = withContext(Dispatchers.IO) {
                    scheduleDao.getSchedulesForGame(game.id)
                }

                val users = withContext(Dispatchers.IO) {
                    gameDao.getUsersForGame(game.id)
                }

                val playerCount = users.size
                val currentUser = users.find { it.id == getCurrentUserId() }

                val countText = TextView(this@UserGamesActivity).apply {
                    text = "Liczba graczy: $playerCount"
                    textSize = 14f
                    setTextColor(textPrimary)
                    typeface = fontMono
                }
                detailsContainer.addView(countText)

                val roleLabel = TextView(this@UserGamesActivity).apply {
                    text = "Twoja rola:"
                    textSize = 14f
                    setTextColor(textPrimary)
                    typeface = fontMono
                }
                detailsContainer.addView(roleLabel)

                val roleBadge = TextView(this@UserGamesActivity).apply {
                    text = currentUser?.role ?: "BRAK"
                    textSize = 14f
                    setTextColor(Color.WHITE)
                    typeface = fontSpartan
                    setPadding(24, 8, 24, 8)
                    gravity = Gravity.CENTER
                    background = ContextCompat.getDrawable(
                        this@UserGamesActivity,
                        when (currentUser?.role) {
                            "GAMEMASTER" -> R.drawable.badge_background_gamemaster
                            "PLAYER" -> R.drawable.badge_background_player
                            "SPECTATOR" -> R.drawable.badge_background_spectator
                            else -> R.drawable.badge_background_spectator // fallback
                        }
                    )
                    layoutParams = LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.WRAP_CONTENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                    ).apply {
                        topMargin = 8
                    }
                }
                detailsContainer.addView(roleBadge)


                if (schedules.isNotEmpty()) {
                    val schedulesLabel = TextView(this@UserGamesActivity).apply {
                        text = "Terminy spotkań:"
                        textSize = 14f
                        setTextColor(success)
                        setTypeface(fontSpartan, Typeface.BOLD)
                        setPadding(0, 12, 0, 4)
                    }
                    detailsContainer.addView(schedulesLabel)

                    schedules.forEach { schedule ->
                        val dateText = TextView(this@UserGamesActivity).apply {
                            // załóżmy, że formatDateTime zwraca coś np. "2025-06-22 14:00"
                            val startStr = formatDateTime(schedule.start)
                            val endStr = formatDateTime(schedule.end)

                            // "%-10s %s" = pierwszy ciąg ma 10 znaków, drugi po spacji
                            text = String.format(
                                "%-15s %s\n%-15s %s",
                                "Początek:", startStr,
                                "Koniec:", endStr
                            )

                            textSize = 14f
                            typeface = Typeface.MONOSPACE // monospace dla wyrównania
                            setTextColor(textSecondary)
                        }
                        detailsContainer.addView(dateText)
                    }

                }

                if (users.isNotEmpty()) {
                    val usersLabel = TextView(this@UserGamesActivity).apply {
                        text = "Gracze:"
                        textSize = 14f
                        setTextColor(textPrimary)
                        setTypeface(fontSpartan, Typeface.BOLD)
                        setPadding(0, 12, 0, 4)
                    }
                    detailsContainer.addView(usersLabel)

                    users.forEach { user ->
                        val userText = TextView(this@UserGamesActivity).apply {
                            text = "${user.name} (${user.username}) — ${user.role}"
                            textSize = 14f
                            typeface = fontMono
                            setTextColor("#CCCCCC".toColorInt())
                        }
                        detailsContainer.addView(userText)
                    }
                }

                gameContainer.addView(detailsContainer)
                container.addView(gameContainer)
            }
        }
    }
}
