package com.example.rpgdiceapp.diceview

import android.content.Context
import android.opengl.GLES20
import android.opengl.GLSurfaceView
import android.opengl.Matrix
import android.os.Handler
import android.os.Looper
import android.util.Log
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.opengles.GL10
import kotlin.math.abs

class DiceRenderer(private val context: Context) : GLSurfaceView.Renderer {

    private val modelMatrix = FloatArray(16)
    private val viewMatrix = FloatArray(16)
    private val projectionMatrix = FloatArray(16)
    private val mvpMatrix = FloatArray(16)
    private val diceList = mutableListOf<DiceInstance>()
    val rollResults = mutableListOf<Int>()

    private var hasDispatchedResults = false

    var onRollFinishedMulti: ((List<Int>) -> Unit)? = null

    private val faceRotationAngles = mapOf(
        "d6" to mapOf(
            1 to Triple(0f, 0f, 0f),
            2 to Triple(90f, 0f, 0f),
            3 to Triple(0f, -90f, 0f),
            4 to Triple(0f, 90f, 0f),
            5 to Triple(-90f, 0f, 0f),
            6 to Triple(180f, 0f, 0f)
        ),
        "d10" to mapOf(
            0 to Triple(32f, -88f, 0f),
            1 to Triple(32f, -160f, 180f),
            2 to Triple(32f, -160f, 0f),
            3 to Triple(32f, -304f, 180f),
            4 to Triple(32f, -304f, 0f),
            5 to Triple(32f, -232f, 180f),
            6 to Triple(32f, -232f, 0f),
            7 to Triple(32f, -16f, 180f),
            8 to Triple(32f, -16f, 0f),
            9 to Triple(32f, -88f, 180f)
        ),
        "d100" to mapOf(
            0 to Triple(32f, -88f, 0f),
            1 to Triple(32f, -160f, 180f),
            2 to Triple(32f, -160f, 0f),
            3 to Triple(32f, -304f, 180f),
            4 to Triple(32f, -304f, 0f),
            5 to Triple(32f, -232f, 180f),
            6 to Triple(32f, -232f, 0f),
            7 to Triple(32f, -16f, 180f),
            8 to Triple(32f, -16f, 0f),
            9 to Triple(32f, -88f, 180f)
        )
    )

    private fun getFaceFacingCamera(x: Float, y: Float, z: Float, diceType: String): Int {
        val rotationMatrix = FloatArray(16)

        Log.d("Rotation", "Initial Angles - X: $x, Y: $y, Z: $z")
        Matrix.setIdentityM(rotationMatrix, 0)
        Matrix.rotateM(rotationMatrix, 0, x, 1f, 0f, 0f)
        Matrix.rotateM(rotationMatrix, 0, y, 0f, 1f, 0f)
        Matrix.rotateM(rotationMatrix, 0, z, 0f, 0f, 1f)
        Log.d("RotationMatrix", "Rotation Matrix: ${rotationMatrix.joinToString(", ")}")

        val faceNormals = when (diceType) {
            "d6" -> mapOf(
                1 to floatArrayOf(0f, 0f, -1f),
                6 to floatArrayOf(0f, 0f, 1f),
                2 to floatArrayOf(0f, -1f, 0f),
                5 to floatArrayOf(0f, 1f, 0f),
                3 to floatArrayOf(-1f, 0f, 0f),
                4 to floatArrayOf(1f, 0f, 0f)
            )
            "d10", "d100" -> mapOf(
                0 to floatArrayOf(-0.84753149f, -0.52991926f, 0.02959645f),
                1 to floatArrayOf(0.23375373f, 0.52991926f, 0.81519615f),
                2 to floatArrayOf(-0.23375373f, -0.52991926f, 0.81519615f),
                3 to floatArrayOf(-0.66827102f, 0.52991926f, -0.52211054f),
                4 to floatArrayOf(0.66827102f, -0.52991926f, -0.52211054f),
                5 to floatArrayOf(-0.70306374f, 0.52991926f, 0.47422248f),
                6 to floatArrayOf(0.70306374f, -0.52991926f, 0.47422248f),
                7 to floatArrayOf(0.29004953f, 0.52991926f, -0.79690454f),
                8 to floatArrayOf(-0.29004953f, -0.52991926f, -0.79690454f),
                9 to floatArrayOf(0.84753149f, 0.52991926f, 0.02959645f)
            )
            else -> throw IllegalArgumentException("Zły typ kości")
        }

        var maxDot = -Float.MAX_VALUE
        var result = 0

        Log.d("DiceRotation", "Initial rotation angles (x: $x, y: $y, z: $z)")

        for ((face, normal) in faceNormals) {
            val transformed = FloatArray(4)
            Matrix.multiplyMV(transformed, 0, rotationMatrix, 0, floatArrayOf(normal[0], normal[1], normal[2], 0f), 0)
            val dot = -transformed[2]

            Log.d("FaceNormals", "Face $face - Normal: ${normal.joinToString(", ")} -> Transformed Normal: ${transformed.joinToString(", ")} | Dot: $dot")

            if (dot > maxDot) {
                maxDot = dot
                result = face
            }
        }

        Log.d("DiceRotation", "Selected face: $result")

        return when (diceType) {
            "d10" -> result
            "d100" -> result * 10
            else -> result
        }
    }

    fun getDiceList(): List<DiceInstance> {
        return diceList
    }

    override fun onSurfaceCreated(unused: GL10?, config: EGLConfig?) {
        GLES20.glClearColor(0f, 0f, 0f, 1f)
        GLES20.glEnable(GLES20.GL_DEPTH_TEST)

        updateDiceCount(1,"d6")
    }

    fun updateDiceCount(count: Int, diceType: String) {
        val layout = when (count) {
            1 -> listOf(0f to 0f)
            2 -> listOf(-1f to 0f, 1f to 0f)
            3 -> listOf(-1f to 1f, 1f to 1f, 0f to -1.5f)
            4 -> listOf(-1f to 1f, 1f to 1f, -1f to -1.5f, 1f to -1.5f)
            else -> emptyList()
        }

        diceList.clear()
        rollResults.clear()

        val modelType = when (diceType) {
            "d6", "d10", "d100" -> diceType
            else -> throw IllegalArgumentException("Zły typ kości")
        }

        layout.forEach { (x, y) ->
            val model = when (diceType) {
                "d6" -> ObjModel(context, "d6.obj", "M_d6.png", "d6normal.png", "d6metal.png")
                "d10" -> ObjModel(context, "d10.obj", "M_d10.png", "d10normal.png", "d10metal.png")
                else -> throw IllegalArgumentException("Zły typ kości")
            }

            diceList.add(DiceInstance(model, offsetX = x, offsetY = y, modelType = modelType))
        }
    }

    override fun onSurfaceChanged(unused: GL10?, width: Int, height: Int) {
        GLES20.glViewport(0, 0, width, height)
        val ratio = width.toFloat() / height
        Matrix.frustumM(projectionMatrix, 0, -ratio, ratio, -1f, 1f, 3f, 7f)
    }




    override fun onDrawFrame(unused: GL10?) {
        GLES20.glClearColor(0.117f, 0.117f, 0.117f, 1.0f)
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT or GLES20.GL_DEPTH_BUFFER_BIT)
        Matrix.setLookAtM(viewMatrix, 0, 0f, 0f, 6f, 0f, 0f, 0f, 0f, 1f, 0f)

        for ((index, dice) in diceList.withIndex()) {
            dice.damping += (dice.targetDamping - dice.damping) * 0.05f

            if (dice.isRotating) {
                dice.angleX += dice.speedX
                dice.angleY += dice.speedY
                dice.angleZ += dice.speedZ

                dice.speedX *= dice.damping
                dice.speedY *= dice.damping
                dice.speedZ *= dice.damping

                if (isStopped(dice)) {
                    dice.isRotating = false
                    val result = getFaceFacingCamera(dice.angleX, dice.angleY, dice.angleZ, dice.modelType)
                    dice.result = result
                    snapToFace(dice, result)

                    if (index < rollResults.size) {
                        rollResults[index] = result
                    }

                    Log.d("DiceAutoResult", "Kość $index → wynik=$result (po zatrzymaniu)")
                }
            }

            if (dice.snapInProgress) {
                normalizeAngles(dice)
                dice.angleX = approachAngle(dice.angleX, dice.targetAngleX, 0.04f)
                dice.angleY = approachAngle(dice.angleY, dice.targetAngleY, 0.04f)
                dice.angleZ = approachAngle(dice.angleZ, dice.targetAngleZ, 0.04f)

                val close = { a: Float, b: Float -> abs((a - b + 360f) % 360f) < 0.5f }
                if (close(dice.angleX, dice.targetAngleX) &&
                    close(dice.angleY, dice.targetAngleY) &&
                    close(dice.angleZ, dice.targetAngleZ)) {
                    dice.angleX = dice.targetAngleX
                    dice.angleY = dice.targetAngleY
                    dice.angleZ = dice.targetAngleZ
                    dice.snapInProgress = false
                    dice.finished = true
                }
            }

            if (!dice.isRotating && !dice.snapInProgress && dice.result == -1) {

                val result = getFaceFacingCamera(dice.angleX, dice.angleY, dice.angleZ, dice.modelType)
                dice.result = result
                if (index < rollResults.size) {
                    rollResults[index] = result
                }
                Log.d("DiceAutoDetect", "Kość $index → wynik=$result (bez obrotu)")
            }

            Matrix.setIdentityM(modelMatrix, 0)
            Matrix.scaleM(modelMatrix, 0, 0.6f, 0.6f, 0.6f)
            Matrix.translateM(modelMatrix, 0, dice.offsetX, dice.offsetY, 0f)
            Matrix.rotateM(modelMatrix, 0, dice.angleX, 1f, 0f, 0f)
            Matrix.rotateM(modelMatrix, 0, dice.angleY, 0f, 1f, 0f)
            Matrix.rotateM(modelMatrix, 0, dice.angleZ, 0f, 0f, 1f)

            Matrix.multiplyMM(mvpMatrix, 0, viewMatrix, 0, modelMatrix, 0)
            Matrix.multiplyMM(mvpMatrix, 0, projectionMatrix, 0, mvpMatrix, 0)

            dice.model.draw(mvpMatrix)
        }

        Handler(Looper.getMainLooper()).post {
            onRollFinishedMulti?.invoke(rollResults.toList())
        }
    }

    fun rollDice(count: Int, diceType: String) {
        rollResults.clear()
        repeat(count) { rollResults.add(-1) }

        diceList.take(count).forEach { dice ->
            dice.speedX = randomSpeed()
            dice.speedY = randomSpeed()
            dice.speedZ = randomSpeed()
            dice.damping = 1f
            dice.targetDamping = 1f
            dice.isRotating = true
            dice.snapInProgress = false
            dice.finished = false
        }
    }

    fun beginSlowStop() {
        diceList.forEach { it.targetDamping = 0.95f }
    }

    fun resetDice() {
        for (dice in diceList) {
            dice.angleX = 0f
            dice.angleY = 0f
            dice.angleZ = 0f
            dice.speedX = 0f
            dice.speedY = 0f
            dice.speedZ = 0f
            dice.result = -1
            dice.finished = false
            dice.snapInProgress = false
            dice.isRotating = false
        }
        hasDispatchedResults = false

    }

    fun resetAndReloadModels(count: Int, diceType: String) {
        resetDice()
        updateDiceCount(count, diceType)
    }

    fun snapToFace(dice: DiceInstance, face: Int) {
        val rotationAngles = faceRotationAngles[dice.modelType] ?: throw IllegalArgumentException("Zły typ kości")

        rotationAngles[face]?.let { (tx, ty, tz) ->
            normalizeAngles(dice)

            val close = { a: Float, b: Float -> abs((a - b + 360f) % 360f) < 0.5f }

            if (close(dice.angleX, tx) && close(dice.angleY, ty) && close(dice.angleZ, tz)) {
                dice.angleX = tx
                dice.angleY = ty
                dice.angleZ = tz
                dice.snapInProgress = false
                dice.finished = true
            } else {
                dice.targetAngleX = tx
                dice.targetAngleY = ty
                dice.targetAngleZ = tz
                dice.snapInProgress = true
            }
        }
    }


    private fun isStopped(d: DiceInstance): Boolean =
        abs(d.speedX) < 0.085f && abs(d.speedY) < 0.085f && abs(d.speedZ) < 0.085f

    private fun approachAngle(current: Float, target: Float, factor: Float): Float {
        val delta = (target - current + 540f) % 360f - 180f
        return current + delta * factor
    }

    private fun normalizeAngles(d: DiceInstance) {
        d.angleX %= 360f; if (d.angleX < 0) d.angleX += 360f
        d.angleY %= 360f; if (d.angleY < 0) d.angleY += 360f
        d.angleZ %= 360f; if (d.angleZ < 0) d.angleZ += 360f
    }

    private fun randomSpeed(): Float = ((5..15).random() * if ((0..1).random() == 0) -1 else 1).toFloat()
}
