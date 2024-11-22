package com.bbam.dearmyfriend.data

import com.google.gson.annotations.SerializedName

data class PostData(
    val id: Int,
    val uid: String,
    val content: String,
    val dateMillis: Long,
    val dateFormatted: String,
    val imageUrls: List<String>?, // 서버에서 전달된 이미지 URL 리스트
    val created_at: String,
    @SerializedName("nickname") val nickname: String?, // 서버의 JSON 필드와 매핑
    @SerializedName("profileImage") val profileImage: String?, // 서버의 JSON 필드와 매핑
    var isLiked: Boolean = false // 좋아요 상태
)

