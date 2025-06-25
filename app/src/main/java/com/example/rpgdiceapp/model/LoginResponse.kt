package com.example.rpgdiceapp.model

data class LoginResponse(
    val authResponse: AuthResponse,
    val message: String,
    val error: Int,
    val timestamp: String
)

data class AuthResponse(
    val accessToken: String,
    val expiresIn: Long
)
