package com.bbam.dearmyfriend.data

data class LoginResponse(
    val success: Boolean,
    val message: String
)

data class SessionResponse(
    val logged_in: Boolean,
    val message: String
)
