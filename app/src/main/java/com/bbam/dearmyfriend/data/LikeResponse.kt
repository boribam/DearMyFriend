package com.bbam.dearmyfriend.data

data class LikeResponse(
    val success: Boolean,
    val message: String?,
    val liked: Boolean // 좋아요 상태 (true: 좋아요, false: 취소)
)