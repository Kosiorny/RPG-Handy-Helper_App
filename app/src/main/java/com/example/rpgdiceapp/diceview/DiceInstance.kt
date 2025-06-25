package com.example.rpgdiceapp.diceview

data class DiceInstance(
    val model: ObjModel,
    var angleX: Float = 0f,
    var angleY: Float = 0f,
    var angleZ: Float = 0f,
    var speedX: Float = 0f,
    var speedY: Float = 0f,
    var speedZ: Float = 0f,
    var isRotating: Boolean = false,
    var targetDamping: Float = 1f,
    var damping: Float = 1f,
    var finished: Boolean = false,
    var result: Int = -1,
    val offsetX: Float = 0f,
    val offsetY: Float = 0f,
    var snapInProgress: Boolean = false,
    var targetAngleX: Float = 0f,
    var targetAngleY: Float = 0f,
    var targetAngleZ: Float = 0f,
    val modelType: String
)
