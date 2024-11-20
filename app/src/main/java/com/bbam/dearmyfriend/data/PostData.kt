package com.bbam.dearmyfriend.data

data class PostData(
    val id: Int,
    val uid: String,
    val content: String,
    val dateMillis: Long,
    val dateFormatted: String,
    val imageUrls: List<String>?, // 서버에서 전달된 이미지 URL 리스트
    val created_at: String,
    val nickname: String?, // 닉네임 추가
    val profileImage: String?, // 프로필 이미지 URL 추가
    var isLiked: Boolean = false // 좋아요 상태
)

