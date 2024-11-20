package com.bbam.dearmyfriend.data

data class PostResponse(
    val success: Boolean,
    val message: String,
    val posts: List<PostData> // PostData는 기존의 게시물 데이터 클래스
)
