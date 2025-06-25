package com.example.rpgdiceapp

import android.annotation.SuppressLint
import android.content.Intent
import android.graphics.Typeface
import android.hardware.*
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.res.ResourcesCompat
import com.example.rpgdiceapp.diceview.DiceGLSurfaceView
import kotlin.math.sqrt
import androidx.core.graphics.toColorInt

class DiceActivity : AppCompatActivity(), SensorEventListener {

    private lateinit var sensorManager: SensorManager
    private lateinit var diceView: DiceGLSurfaceView
    private lateinit var spinner: Spinner
    private var lastMotionTime = 0L
    private var isShaking = false
    private var diceCount = 1
    val resultViews = mutableListOf<TextView>()

    @SuppressLint("SetTextI18n")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_dice)

        spinner = findViewById(R.id.diceTypeSpinner)

        val diceTypes = arrayOf("d6", "d10")

        val typeAdapter = ArrayAdapter(this, R.layout.spinner_item_with_arrow, diceTypes)
        typeAdapter.setDropDownViewResource(R.layout.spinner_item)
        spinner.adapter = typeAdapter

        spinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parentView: AdapterView<*>, view: View?, position: Int, id: Long) {
                val selectedDice = diceTypes[position]
                resultViews.forEach { it.visibility = View.GONE }
                diceView.resetAndReload(diceCount, selectedDice)
            }

            override fun onNothingSelected(parentView: AdapterView<*>) {}
        }

        val faceSpinner = findViewById<Spinner>(R.id.diceFaceSpinner)
        val faceAdapter = ArrayAdapter.createFromResource(
            this,
            R.array.dice_faces,
            R.layout.spinner_item_with_arrow
        )
        faceAdapter.setDropDownViewResource(R.layout.spinner_item)
        faceSpinner.adapter = faceAdapter

        val snapButton = findViewById<Button>(R.id.snapButton)
        snapButton.setOnClickListener {
            val selectedFace = faceSpinner.selectedItemPosition
            diceView.snapToFaceForSelectedFace(selectedFace)
        }


        diceView = findViewById(R.id.diceSurfaceView)
        diceView.updateDiceCount(diceCount, "d6")

        findViewById<Button>(R.id.resetButton).setOnClickListener {
            resultViews.forEach { it.visibility = View.GONE }
            diceView.resetDice()
        }

        val seekBar = findViewById<SeekBar>(R.id.diceCountSeekBar)
        val diceLabel = findViewById<TextView>(R.id.diceCountLabel)
        seekBar.max = 3
        seekBar.progress = 0
        diceLabel.text = "Liczba kości: 1"
        updateDiceTextOverlays(1)

        seekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                diceCount = progress + 1
                diceLabel.text = "Liczba kości: $diceCount"

                diceView.updateDiceCount(diceCount, spinner.selectedItem.toString())
                updateDiceTextOverlays(diceCount)
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })

        diceView.setOnRollFinishedListener { results ->
            resultViews.forEach { it.visibility = View.GONE }
            results.forEachIndexed { index, value ->
                if (index < resultViews.size) {
                    resultViews[index].text = value.toString()
                    if (resultViews[index].text != "-1") {
                        resultViews[index].visibility = View.VISIBLE
                    }
                }
            }
        }

        sensorManager = getSystemService(SENSOR_SERVICE) as SensorManager
    }

    private fun updateDiceTextOverlays(diceCount: Int) {
        val diceContainer = findViewById<FrameLayout>(R.id.gl_container)

        resultViews.forEach { diceContainer.removeView(it) }
        resultViews.clear()

        val positions = when (diceCount) {
            1 -> listOf(0.46f to 0.25f)
            2 -> listOf(0.22f to 0.1f, 0.70f to 0.1f)
            3 -> listOf(0.22f to 0.1f, 0.70f to 0.1f, 0.46f to 0.5f)
            4 -> listOf(0.22f to 0.1f, 0.70f to 0.1f, 0.22f to 0.5f, 0.70f to 0.5f)
            else -> emptyList()
        }

        positions.forEach { (px, py) ->
            val tv = TextView(this).apply {
                textSize = 40f
                setTextColor("#12E1B9".toColorInt())
                typeface = ResourcesCompat.getFont(this@DiceActivity, R.font.jetbrains_mono)
                visibility = View.GONE
                setTypeface(null, Typeface.BOLD)
                textAlignment = View.TEXT_ALIGNMENT_CENTER

                val params = FrameLayout.LayoutParams(
                    FrameLayout.LayoutParams.WRAP_CONTENT,
                    FrameLayout.LayoutParams.WRAP_CONTENT
                )
                params.gravity = Gravity.TOP or Gravity.START
                layoutParams = params

                post {
                    val x = diceContainer.width * px
                    val y = diceContainer.height * py
                    translationX = x
                    translationY = y
                }
            }

            diceContainer.addView(tv)
            resultViews.add(tv)
        }
    }

    override fun onResume() {
        super.onResume()
        sensorManager.registerListener(
            this,
            sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER),
            SensorManager.SENSOR_DELAY_GAME
        )
    }

    override fun onPause() {
        super.onPause()
        sensorManager.unregisterListener(this)
    }

    override fun onSensorChanged(event: SensorEvent) {
        if (event.sensor.type == Sensor.TYPE_ACCELEROMETER) {
            val x = event.values[0]
            val y = event.values[1]
            val z = event.values[2]

            val magnitude = sqrt(x * x + y * y + z * z)
            val delta = magnitude - SensorManager.GRAVITY_EARTH
            val now = System.currentTimeMillis()

            if (delta > 2f) {
                if (!isShaking) {
                    isShaking = true
                    resultViews.forEach { it.visibility = View.GONE }
                    diceView.rollDice(diceCount, spinner.selectedItem.toString())
                }
                lastMotionTime = now
            } else if (isShaking && now - lastMotionTime > 1000) {
                isShaking = false
                diceView.beginSlowStop()
            }
        }
    }

    fun onLogoClick(view: View) {
        val intent = Intent(this, MainActivity::class.java)
        startActivity(intent)
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
}
