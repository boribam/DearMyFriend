package com.bbam.dearmyfriend.data

data class RegisterResponse(
    val success: Boolean,
    val message: String, // 오류 메시지 등을 포함
    val uid: String?,
    val nickname: String?,
    val profileImage: String?,
)
