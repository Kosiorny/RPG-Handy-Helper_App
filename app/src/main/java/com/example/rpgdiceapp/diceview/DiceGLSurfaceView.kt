package com.example.rpgdiceapp.diceview

import android.content.Context
import android.opengl.GLSurfaceView
import android.util.AttributeSet

class DiceGLSurfaceView(context: Context, attrs: AttributeSet? = null) : GLSurfaceView(context, attrs) {
    private val renderer: DiceRenderer

    init {
        setEGLContextClientVersion(2)
        renderer = DiceRenderer(context)
        setRenderer(renderer)
        renderMode = RENDERMODE_CONTINUOUSLY
    }

    fun rollDice(count: Int, diceType: String) {
        renderer.rollDice(count,diceType)
    }

    fun beginSlowStop() {
        renderer.beginSlowStop()
    }

    // W DiceGLSurfaceView
    fun snapToFaceForSelectedFace(selectedFace: Int) {
        renderer.getDiceList().forEach { dice ->
            if (!dice.isRotating && !dice.snapInProgress) {
                renderer.snapToFace(dice, selectedFace)
            }
        }
    }


    fun resetDice() {
        renderer.resetDice()
    }

    fun resetAndReload(count: Int, diceType: String) {
        queueEvent {
            renderer.resetAndReloadModels(count, diceType)
        }
    }


    fun updateDiceCount(count: Int, diceType: String) {
        queueEvent {
            renderer.updateDiceCount(count,diceType)
        }
    }


    fun setOnRollFinishedListener(listener: (List<Int>) -> Unit) {
        renderer.onRollFinishedMulti = listener
    }
}
