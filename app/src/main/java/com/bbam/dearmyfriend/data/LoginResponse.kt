package com.bbam.dearmyfriend.data

data class LoginResponse(
    val success: Boolean,
    val uid: String?, // uid 필드 추가
    val message: String
)

data class SessionResponse(
    val logged_in: Boolean,
    val message: String
)
